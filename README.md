# Battery Monitor (BattMon)

Battery monitoring system with a Kotlin/Ktor backend and mobile application. Monitors UPS (Uninterruptible Power Supply) status using APC UPS daemon and provides real-time notifications.

## Features

- Real-time UPS status monitoring via apcupsd
- Historical data storage with PostgreSQL
- REST API for querying UPS status
- Push notifications via Firebase Cloud Messaging
- Automated data retention policies
- Mobile application support

## Project Structure

- `backend/` - Kotlin/Ktor REST API server
- `mobile/` - Mobile application
- `shared/` - Shared Kotlin multiplatform code
- `scripts/` - Utility scripts (apcaccess HTTP proxy)

## Prerequisites

Before running the system, ensure you have:

- Docker and Docker Compose installed
- Python 3 installed
- `apcupsd` installed and configured on the host machine
- Firebase service account JSON file for push notifications

## Quick Setup (Automated)

The simplest way to get started:

```bash
./setup.sh
```

The script will:
1. ✓ Check all prerequisites
2. ✓ Verify apcupsd is working
3. ✓ Setup Firebase credentials
4. ✓ Create `.env` configuration
5. ✓ Configure apcaccess-proxy
6. ✓ Build and start Docker containers
7. ✓ Verify everything is working

---

## Manual Setup (Step by Step)

If you prefer to set things up manually or the automated script doesn't work for your system:

## Running the System (Docker)

### Step 1: Verify apcupsd is Working

```bash
# Check if apcupsd is running
sudo systemctl status apcupsd

# Test the command
apcaccess status
```

You should see UPS status information. If not, configure `apcupsd` before proceeding.

### Step 2: Start the apcaccess HTTP Proxy

The backend needs to access `apcaccess` on the host. Run the HTTP proxy:

**Option A: In a separate terminal (recommended for first run)**

```bash
cd /path/to/battmon
python3 scripts/apcaccess-proxy.py
```

Leave this terminal open.

**Option B: In background**

```bash
python3 scripts/apcaccess-proxy.py &
```

**Verify the proxy:**

```bash
# Health check - should return "OK"
curl http://localhost:8081/health

# Test apcaccess - should return UPS status
curl http://localhost:8081/apcaccess
```

### Step 3: Configure Environment

```bash
# Copy the example environment file
cp .env.example .env

# (Optional) Edit if needed - defaults should work
nano .env
```

### Step 4: Setup Firebase Credentials

Place your Firebase service account JSON file:

```bash
# Copy your Firebase credentials
cp /path/to/your/firebase-credentials.json backend/firebase-credentials.json

# Verify the file exists
ls -la backend/firebase-credentials.json
```

### Step 5: Build and Start Services

```bash
# Build and start everything
docker-compose up -d --build
```

This will:
- Build the backend Docker image (first run takes 2-5 minutes)
- Start PostgreSQL container
- Start backend container

### Step 6: Verify Everything is Running

```bash
# Check container status
docker-compose ps

# Check backend health
curl http://localhost:8080/status

# Get latest UPS reading
curl http://localhost:8080/status/latest
```

If you see UPS data, the system is working!

### Step 7: Monitor Logs

```bash
# Watch all logs
docker-compose logs -f

# Backend only
docker-compose logs -f backend

# PostgreSQL only
docker-compose logs -f postgres
```

## Management Commands

```bash
# Stop services (keeps data)
docker-compose down

# Stop and remove all data (WARNING: deletes database!)
docker-compose down -v

# Restart services
docker-compose restart

# Rebuild after code changes
docker-compose up -d --build

# View logs
docker-compose logs -f
```

## API Endpoints

```bash
# Root endpoint (API info)
curl http://localhost:8080/

# Latest UPS reading
curl http://localhost:8080/status/latest

# Historical data (requires ISO-8601 timestamps)
curl "http://localhost:8080/status/history?from=2026-01-12T10:00:00Z&to=2026-01-12T11:00:00Z"

# Historical data (last hour example using date command)
FROM=$(date -u -v-1H +%Y-%m-%dT%H:%M:%SZ)
TO=$(date -u +%Y-%m-%dT%H:%M:%SZ)
curl "http://localhost:8080/status/history?from=$FROM&to=$TO"

# Register device token for notifications
curl -X POST http://localhost:8080/notifications/register \
  -H "Content-Type: application/json" \
  -d '{"token": "your-fcm-token"}'
```

## Troubleshooting

### Backend can't connect to apcaccess

**Check if proxy is running:**
```bash
curl http://localhost:8081/health
```

**Restart the proxy:**
```bash
python3 scripts/apcaccess-proxy.py &
```

### Backend can't connect to database

```bash
# Check PostgreSQL status
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Backend keeps restarting

```bash
# Check backend logs for errors
docker-compose logs backend

# Common issues:
# - apcaccess-proxy not running
# - Database connection failed
# - Firebase credentials not found (if enabled)
```

### Port already in use

```bash
# Find what's using port 8080
lsof -i :8080

# Stop the conflicting service or change port in docker-compose.yml
```

## Architecture

The Docker setup uses:

1. **Backend Container** - Runs with host network mode to access localhost services
2. **PostgreSQL Container** - Separate container with persistent storage
3. **apcaccess-proxy** - Python HTTP service on host that exposes `apcaccess` command

The backend calls `http://localhost:8081/apcaccess` instead of executing the command directly, allowing the containerized app to query UPS status from the host where `apcupsd` is configured.

## Production Setup

### Run apcaccess-proxy as systemd service (Linux)

```bash
# Copy project to /opt/battmon
sudo cp -r /path/to/battmon /opt/battmon

# Install systemd service
sudo cp scripts/apcaccess-proxy.service /etc/systemd/system/

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable apcaccess-proxy
sudo systemctl start apcaccess-proxy

# Check status
sudo systemctl status apcaccess-proxy
```

### Auto-start Docker services

```bash
# Services already have restart: unless-stopped
# Just ensure Docker starts on boot
sudo systemctl enable docker
```

### Change database password

```bash
# Edit .env and change DATABASE_PASSWORD
nano .env

# Recreate containers
docker-compose down -v
docker-compose up -d
```

## Database Access

```bash
# Connect via docker-compose
docker-compose exec postgres psql -U battmon -d battmon

# Or from host (if psql is installed)
psql -h localhost -U battmon -d battmon
# Password: battmon (or from .env)
```

**Useful queries:**
```sql
-- Count records
SELECT COUNT(*) FROM ups_status;

-- Latest 10 readings
SELECT * FROM ups_status ORDER BY timestamp DESC LIMIT 10;

-- Exit
\q
```

## Local Development (Without Docker)

Requirements:
- JDK 17
- PostgreSQL database
- `apcupsd` installed and configured

```bash
cd backend
./gradlew run
```

See [backend/README.md](backend/README.md) for details.

## Environment Variables

Key variables in `.env`:

```bash
# Database
DATABASE_NAME=battmon
DATABASE_USER=battmon
DATABASE_PASSWORD=battmon

# UPS Monitor
UPS_COMMAND=curl -s http://localhost:8081/apcaccess
UPS_POLL_INTERVAL=5

# Firebase (optional)
HOST_FIREBASE_CREDENTIALS_PATH=./backend/firebase-credentials.json
FIREBASE_ENABLED=true

# JVM
JAVA_OPTS=-Xmx512m -Xms256m
```

## What's Next?

- Configure mobile app to connect to `http://<host-ip>:8080`
- Set up firewall rules if accessing from other devices
- Consider HTTPS/TLS for production
- Set up automated PostgreSQL backups
- Monitor disk usage

## License

Private project.
