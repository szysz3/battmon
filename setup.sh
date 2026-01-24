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
    if docker info >/dev/null 2>&1; then
        print_success "Docker daemon is running"
    else
        print_error "Docker daemon is not running"
        MISSING_DEPS+=("docker-daemon")
    fi
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

if command_exists curl; then
    print_success "curl is installed"
else
    print_error "curl is not installed"
    MISSING_DEPS+=("curl")
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

# Step 2: apcupsd NIS configuration reminder (multi-UPS)
echo -e "${BLUE}Step 2: apcupsd NIS configuration reminder...${NC}"
echo ""
print_info "BattMon connects to UPS devices over the apcupsd Network Information Server (NIS)."
print_info "Ensure each UPS host has apcupsd running with NIS enabled on port 3551."
print_info "Test from the backend host: apcaccess -h <host>:3551 status"
echo ""

# Step 3: Setup environment file
echo -e "${BLUE}Step 3: Setting up environment configuration...${NC}"
echo ""

if [ ! -f ".env.example" ]; then
    print_error ".env.example not found"
    print_info "Please ensure .env.example exists in the repo root and run this script again."
    exit 1
fi

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

# Step 4.5: Email notification setup
echo -e "${BLUE}Step 4.5: Setting up email notifications...${NC}"
echo ""

# Function to update .env value
update_env_value() {
    local key=$1
    local value=$2
    local env_file=".env"

    if grep -q "^${key}=" "$env_file"; then
        # Update existing value (compatible with BSD sed on macOS)
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s|^${key}=.*|${key}=${value}|" "$env_file"
        else
            sed -i "s|^${key}=.*|${key}=${value}|" "$env_file"
        fi
    else
        # Append if not exists
        echo "${key}=${value}" >> "$env_file"
    fi
}

# Check if email is already configured
EMAIL_ALREADY_CONFIGURED=false
if [ -f ".env" ] && grep -q "^EMAIL_ENABLED=true" .env 2>/dev/null; then
    EMAIL_ALREADY_CONFIGURED=true
    EXISTING_EMAIL_FROM=$(grep "^EMAIL_FROM=" .env 2>/dev/null | cut -d= -f2)
    EXISTING_EMAIL_TO=$(grep "^EMAIL_TO=" .env 2>/dev/null | cut -d= -f2)

    print_success "Email notifications are already configured"
    print_info "From: $EXISTING_EMAIL_FROM"
    print_info "To: $EXISTING_EMAIL_TO"
    echo ""
    read -p "Do you want to reconfigure email settings? (y/N): " -n 1 -r
    echo
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Keeping existing email configuration"
        echo ""
        # Skip to next step
        EMAIL_ALREADY_CONFIGURED=true
    else
        EMAIL_ALREADY_CONFIGURED=false
    fi
fi

if [ "$EMAIL_ALREADY_CONFIGURED" = false ]; then
    print_info "BattMon can send email notifications with full UPS diagnostics when:"
    echo "  • UPS status changes (battery, power loss, etc.)"
    echo "  • UPS power is restored"
    echo "  • Connection to apcupsd is lost/restored"
    echo ""

    read -p "Do you want to enable email notifications? (y/N): " -n 1 -r
    echo
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_info "Setting up Gmail email notifications..."
    echo ""

    print_warning "For Gmail with 2FA (recommended):"
    echo "  1. Enable 2FA on your Gmail account"
    echo "  2. Create an App Password at: https://myaccount.google.com/apppasswords"
    echo "  3. Use the 16-character App Password (not your regular Gmail password)"
    echo ""
    print_info "See README.md for detailed instructions"
    echo ""

    # Sender email
    read -p "Enter sender email address (e.g., notifications@gmail.com): " SENDER_EMAIL
    if [ -z "$SENDER_EMAIL" ]; then
        print_error "Sender email is required"
        exit 1
    fi

    # SMTP username (usually same as sender)
    read -p "Enter SMTP username [${SENDER_EMAIL}]: " SMTP_USER
    SMTP_USER=${SMTP_USER:-$SENDER_EMAIL}

    # SMTP password
    echo ""
    print_warning "IMPORTANT: Use Gmail App Password, NOT your regular password!"
    read -sp "Enter SMTP password (App Password): " SMTP_PASS
    echo ""

    if [ -z "$SMTP_PASS" ]; then
        print_error "SMTP password is required"
        exit 1
    fi

    # Recipient email
    echo ""
    read -p "Enter recipient email address (who receives alerts): " RECIPIENT_EMAIL
    if [ -z "$RECIPIENT_EMAIL" ]; then
        print_error "Recipient email is required"
        exit 1
    fi

    # SMTP server (default Gmail)
    echo ""
    read -p "Enter SMTP host [smtp.gmail.com]: " SMTP_HOST
    SMTP_HOST=${SMTP_HOST:-smtp.gmail.com}

    read -p "Enter SMTP port [587]: " SMTP_PORT
    SMTP_PORT=${SMTP_PORT:-587}

    # Update .env file
    update_env_value "EMAIL_ENABLED" "true"
    update_env_value "SMTP_HOST" "$SMTP_HOST"
    update_env_value "SMTP_PORT" "$SMTP_PORT"
    update_env_value "SMTP_USERNAME" "$SMTP_USER"
    update_env_value "SMTP_PASSWORD" "$SMTP_PASS"
    update_env_value "EMAIL_FROM" "$SENDER_EMAIL"
    update_env_value "EMAIL_TO" "$RECIPIENT_EMAIL"

    echo ""
    print_success "Email notifications configured"
    print_info "From: $SENDER_EMAIL"
    print_info "To: $RECIPIENT_EMAIL"
    print_warning "Your credentials are stored in .env (gitignored)"

    else
        update_env_value "EMAIL_ENABLED" "false"
        print_info "Email notifications disabled"
        print_info "You can enable them later by editing .env file"
    fi
fi

echo ""

# Step 5: Device configuration reminder
echo -e "${BLUE}Step 5: Configure UPS devices...${NC}"
echo ""
print_info "After the backend is running, add devices via the mobile app or the /devices API."
print_info "Each device should use the apcupsd NIS host:port (default 3551)."
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

# Show email notification status
if grep -q "^EMAIL_ENABLED=true" .env 2>/dev/null; then
    echo -e "${GREEN}Email Notifications: ENABLED${NC}"
    RECIPIENT=$(grep "^EMAIL_TO=" .env | cut -d= -f2)
    echo "  • Recipient: $RECIPIENT"
    echo "  • For setup help: see README.md"
    echo ""
fi

echo "For more information, see README.md"
echo ""
