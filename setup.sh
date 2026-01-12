#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}   Battery Monitor (BattMon) - Setup Script${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Step 1: Check prerequisites
echo -e "${BLUE}Step 1: Checking prerequisites...${NC}"
echo ""

MISSING_DEPS=()

if command_exists docker; then
    print_success "Docker is installed ($(docker --version | cut -d' ' -f3 | tr -d ','))"
else
    print_error "Docker is not installed"
    MISSING_DEPS+=("docker")
fi

if command_exists docker-compose || docker compose version >/dev/null 2>&1; then
    if command_exists docker-compose; then
        print_success "Docker Compose is installed ($(docker-compose --version | cut -d' ' -f3 | tr -d ','))"
    else
        print_success "Docker Compose is installed (docker compose plugin)"
    fi
else
    print_error "Docker Compose is not installed"
    MISSING_DEPS+=("docker-compose")
fi

if command_exists python3; then
    print_success "Python 3 is installed ($(python3 --version | cut -d' ' -f2))"
else
    print_error "Python 3 is not installed"
    MISSING_DEPS+=("python3")
fi

if command_exists apcaccess; then
    print_success "apcaccess is installed"
else
    print_error "apcaccess (apcupsd) is not installed"
    MISSING_DEPS+=("apcupsd")
fi

if [ ${#MISSING_DEPS[@]} -ne 0 ]; then
    echo ""
    print_error "Missing dependencies: ${MISSING_DEPS[*]}"
    echo ""
    print_info "Please install missing dependencies and run this script again."
    exit 1
fi

echo ""

# Step 2: Verify apcupsd is working
echo -e "${BLUE}Step 2: Verifying apcupsd configuration...${NC}"
echo ""

if apcaccess status >/dev/null 2>&1; then
    print_success "apcaccess is working correctly"

    # Show brief UPS info
    UPS_STATUS=$(apcaccess status 2>/dev/null | grep "STATUS" | awk '{print $3}')
    UPS_MODEL=$(apcaccess status 2>/dev/null | grep "MODEL" | cut -d: -f2 | xargs)

    if [ -n "$UPS_STATUS" ]; then
        print_info "UPS Status: $UPS_STATUS"
    fi
    if [ -n "$UPS_MODEL" ]; then
        print_info "UPS Model: $UPS_MODEL"
    fi
else
    print_warning "apcaccess command failed - apcupsd might not be configured"
    read -p "Do you want to continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_error "Setup cancelled. Please configure apcupsd first."
        exit 1
    fi
fi

echo ""

# Step 3: Setup environment file
echo -e "${BLUE}Step 3: Setting up environment configuration...${NC}"
echo ""

if [ -f ".env" ]; then
    print_warning ".env file already exists"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cp .env.example .env
        print_success "Created new .env from .env.example"
    else
        print_info "Keeping existing .env file"
    fi
else
    cp .env.example .env
    print_success "Created .env from .env.example"
fi

echo ""

# Step 4: Firebase credentials (required)
echo -e "${BLUE}Step 4: Setting up Firebase credentials...${NC}"
echo ""

if [ -f "backend/firebase-credentials.json" ]; then
    print_success "Firebase credentials found at backend/firebase-credentials.json"
else
    print_error "Firebase credentials not found: backend/firebase-credentials.json"
    echo ""
    read -p "Enter the path to your Firebase service account JSON file: " FIREBASE_PATH

    if [ -f "$FIREBASE_PATH" ]; then
        cp "$FIREBASE_PATH" backend/firebase-credentials.json
        print_success "Firebase credentials copied to backend/firebase-credentials.json"
    else
        print_error "File not found: $FIREBASE_PATH"
        echo ""
        print_error "Cannot continue without Firebase credentials."
        print_info "Please obtain a Firebase service account JSON file and run this script again."
        exit 1
    fi
fi

echo ""

# Step 5: Setup apcaccess-proxy
echo -e "${BLUE}Step 5: Setting up apcaccess HTTP proxy...${NC}"
echo ""

# Check if proxy is already running
if curl -s http://localhost:8081/health >/dev/null 2>&1; then
    print_success "apcaccess-proxy is already running"
    PROXY_ALREADY_RUNNING=true
else
    PROXY_ALREADY_RUNNING=false

    print_info "The apcaccess-proxy needs to run on the host machine"
    echo ""
    echo "Choose how to run it:"
    echo "  1) Systemd service (recommended for production, Linux only)"
    echo "  2) Background process (simple, works on all systems)"
    echo "  3) Skip (I'll run it manually later)"
    echo ""
    read -p "Enter your choice (1-3): " -n 1 -r PROXY_CHOICE
    echo
    echo ""

    case $PROXY_CHOICE in
        1)
            if [[ "$OSTYPE" == "linux-gnu"* ]]; then
                print_info "Setting up systemd service..."

                # Ask for installation path
                read -p "Install to /opt/battmon? (Y/n): " -n 1 -r
                echo

                if [[ ! $REPLY =~ ^[Nn]$ ]]; then
                    INSTALL_PATH="/opt/battmon"
                else
                    read -p "Enter installation path: " INSTALL_PATH
                fi

                echo ""
                print_info "This requires sudo privileges..."

                sudo mkdir -p "$INSTALL_PATH"
                sudo cp -r "$SCRIPT_DIR"/* "$INSTALL_PATH/"
                sudo cp "$INSTALL_PATH/scripts/apcaccess-proxy.service" /etc/systemd/system/

                # Update service file with correct path
                sudo sed -i "s|WorkingDirectory=.*|WorkingDirectory=$INSTALL_PATH|" /etc/systemd/system/apcaccess-proxy.service
                sudo sed -i "s|ExecStart=.*|ExecStart=/usr/bin/python3 $INSTALL_PATH/scripts/apcaccess-proxy.py|" /etc/systemd/system/apcaccess-proxy.service

                sudo systemctl daemon-reload
                sudo systemctl enable apcaccess-proxy
                sudo systemctl start apcaccess-proxy

                sleep 2

                if sudo systemctl is-active --quiet apcaccess-proxy; then
                    print_success "Systemd service installed and started"
                    print_info "Check status: sudo systemctl status apcaccess-proxy"
                    print_info "View logs: sudo journalctl -u apcaccess-proxy -f"
                else
                    print_error "Failed to start systemd service"
                    print_info "Check logs: sudo journalctl -u apcaccess-proxy -n 50"
                    exit 1
                fi
            else
                print_error "Systemd is only available on Linux"
                print_info "Falling back to background process..."
                nohup python3 scripts/apcaccess-proxy.py > apcaccess-proxy.log 2>&1 &
                sleep 2
                print_success "Started apcaccess-proxy in background (PID: $!)"
                print_info "Logs: tail -f apcaccess-proxy.log"
            fi
            ;;
        2)
            nohup python3 scripts/apcaccess-proxy.py > apcaccess-proxy.log 2>&1 &
            sleep 2
            print_success "Started apcaccess-proxy in background (PID: $!)"
            print_info "Logs: tail -f apcaccess-proxy.log"
            ;;
        3)
            print_warning "Skipping proxy setup - you must start it manually before running the backend"
            print_info "Run: python3 scripts/apcaccess-proxy.py &"
            ;;
        *)
            print_error "Invalid choice. Skipping proxy setup."
            ;;
    esac
fi

echo ""

# Verify proxy is running
if curl -s http://localhost:8081/health >/dev/null 2>&1; then
    print_success "apcaccess-proxy is responding on http://localhost:8081"
else
    print_error "apcaccess-proxy is not responding"
    print_warning "The backend won't work until the proxy is running"

    read -p "Do you want to continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""

# Step 6: Build and start Docker containers
echo -e "${BLUE}Step 6: Building and starting Docker containers...${NC}"
echo ""

print_info "This may take 2-5 minutes on first run..."
echo ""

# Check if we should use docker-compose or docker compose
if command_exists docker-compose; then
    DOCKER_COMPOSE="docker-compose"
else
    DOCKER_COMPOSE="docker compose"
fi

$DOCKER_COMPOSE up -d --build

echo ""
print_success "Docker containers started"

echo ""

# Step 7: Verify everything is working
echo -e "${BLUE}Step 7: Verifying installation...${NC}"
echo ""

print_info "Waiting for services to start (30 seconds)..."
sleep 30

# Check container status
print_info "Checking container status..."

if $DOCKER_COMPOSE ps | grep -q "battmon-backend"; then
    print_success "Backend container is running"
else
    print_error "Backend container is not running"
    print_info "Check logs: $DOCKER_COMPOSE logs backend"
fi

if $DOCKER_COMPOSE ps | grep -q "battmon-postgres"; then
    print_success "PostgreSQL container is running"
else
    print_error "PostgreSQL container is not running"
    print_info "Check logs: $DOCKER_COMPOSE logs postgres"
fi

echo ""

# Test backend API
print_info "Testing backend API..."

if curl -s http://localhost:8080/ >/dev/null 2>&1; then
    print_success "Backend API is responding"

    echo ""
    print_info "Testing UPS data collection..."

    LATEST_DATA=$(curl -s http://localhost:8080/status/latest 2>/dev/null)
    if [ -n "$LATEST_DATA" ]; then
        print_success "UPS data is being collected"
        echo ""
        print_info "Latest reading:"
        echo "$LATEST_DATA" | python3 -m json.tool 2>/dev/null || echo "$LATEST_DATA"
    else
        print_warning "No UPS data collected yet (this is normal on first start)"
        print_info "Data collection runs every 5 seconds"
    fi
else
    print_error "Backend API is not responding"
    print_info "Check logs: $DOCKER_COMPOSE logs backend"
fi

echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}   Setup Complete!${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""

print_info "Your Battery Monitor system is running!"
echo ""
echo "Useful commands:"
echo "  • View logs:           $DOCKER_COMPOSE logs -f"
echo "  • Stop services:       $DOCKER_COMPOSE down"
echo "  • Restart services:    $DOCKER_COMPOSE restart"
echo "  • Check status:        $DOCKER_COMPOSE ps"
echo ""
echo "API Endpoints:"
echo "  • Root:                curl http://localhost:8080/"
echo "  • Latest reading:      curl http://localhost:8080/status/latest"
echo "  • Historical data:     curl 'http://localhost:8080/status/history?from=2026-01-12T10:00:00Z&to=2026-01-12T11:00:00Z'"
echo ""
echo "For more information, see README.md"
echo ""
