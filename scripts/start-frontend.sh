#!/bin/bash

# ============================================================================
# Start Frontend Script
# Starts the Haven frontend application in development mode
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Load environment variables
if [ -f "$ROOT_DIR/.env" ]; then
    echo "Loading environment variables from .env file..."
    export $(cat "$ROOT_DIR/.env" | grep -v '^#' | xargs)
fi

echo "============================================"
echo "< Starting Haven Frontend"
echo "============================================"

# Function to print colored output
print_info() {
    echo -e "\033[0;36m[INFO]\033[0m $1"
}

print_success() {
    echo -e "\033[0;32m[SUCCESS]\033[0m $1"
}

print_error() {
    echo -e "\033[0;31m[ERROR]\033[0m $1"
}

print_warning() {
    echo -e "\033[0;33m[WARNING]\033[0m $1"
}

# Check if backend is running
print_info "Checking backend availability..."
BACKEND_URL="http://localhost:${APP_PORT:-8080}/api/actuator/health"

if curl -s -f "$BACKEND_URL" >/dev/null 2>&1; then
    print_success "Backend is running"
else
    print_warning "Backend is not accessible at $BACKEND_URL"
    print_info "Frontend will start but API calls may fail"
    print_info "Run './scripts/start-backend.sh' in another terminal to start the backend"
    echo ""
fi

# Navigate to frontend directory
cd "$ROOT_DIR/frontend"

# Detect package manager
if [ -f "bun.lockb" ] || command -v bun >/dev/null 2>&1; then
    PACKAGE_MANAGER="bun"
    print_info "Using Bun package manager"
elif [ -f "package-lock.json" ]; then
    PACKAGE_MANAGER="npm"
    print_info "Using npm package manager"
elif [ -f "yarn.lock" ]; then
    PACKAGE_MANAGER="yarn"
    print_info "Using Yarn package manager"
else
    print_error "No package manager detected"
    print_info "Please install Bun, npm, or Yarn"
    exit 1
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    print_info "Installing frontend dependencies..."
    $PACKAGE_MANAGER install
    
    if [ $? -ne 0 ]; then
        print_error "Failed to install dependencies"
        exit 1
    fi
    print_success "Dependencies installed"
fi

# Check for outdated dependencies
print_info "Checking for outdated dependencies..."
if [ "$PACKAGE_MANAGER" = "npm" ]; then
    npm outdated || true
elif [ "$PACKAGE_MANAGER" = "bun" ]; then
    bun outdated || true
fi

# Set environment variables for Next.js
export NEXT_PUBLIC_API_URL="http://localhost:${APP_PORT:-8080}/api"
export NEXT_PUBLIC_KEYCLOAK_URL="http://localhost:8081"
export NEXT_PUBLIC_KEYCLOAK_REALM="haven"
export NEXT_PUBLIC_KEYCLOAK_CLIENT_ID="haven-frontend"

# Display service information
echo ""
echo "============================================"
echo "< Frontend Configuration:"
echo "============================================"
echo "Frontend URL:        http://localhost:3000"
echo "API URL:            $NEXT_PUBLIC_API_URL"
echo "Keycloak URL:       $NEXT_PUBLIC_KEYCLOAK_URL"
echo "Keycloak Realm:     $NEXT_PUBLIC_KEYCLOAK_REALM"
echo "============================================"
echo ""

# Choose which app to run
if [ -z "$1" ]; then
    APP="cm-portal"
else
    APP="$1"
fi

print_info "Starting $APP application..."
print_info "Press Ctrl+C to stop"
echo ""

# Start the development server
if [ "$PACKAGE_MANAGER" = "bun" ]; then
    # Bun workspaces use --filter differently
    cd "apps/$APP" && bun run dev
elif [ "$PACKAGE_MANAGER" = "npm" ]; then
    npm run dev -w $APP
elif [ "$PACKAGE_MANAGER" = "yarn" ]; then
    yarn workspace $APP dev
fi

# Handle exit
if [ $? -ne 0 ]; then
    print_error "Frontend failed to start"
    exit 1
fi