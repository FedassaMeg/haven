#!/bin/bash

# ============================================================================
# Haven Bootstrap Script
# Sets up the development environment for first-time setup
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "=� Haven Development Environment Bootstrap"
echo "============================================"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

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

# Check prerequisites
print_info "Checking prerequisites..."

# Check Docker
if ! command_exists docker; then
    print_error "Docker is not installed. Please install Docker Desktop first."
    print_info "Download from: https://www.docker.com/products/docker-desktop/"
    exit 1
fi

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    print_error "Docker is installed but not running."
    print_info "Please start Docker Desktop and try again."
    print_info "On Windows: Open Docker Desktop from the Start menu"
    exit 1
fi

# Check Docker Compose
if ! command_exists docker-compose && ! docker compose version >/dev/null 2>&1; then
    print_error "Docker Compose is not available."
    print_info "Docker Compose should be included with Docker Desktop."
    exit 1
fi

# Check Java
if ! command_exists java; then
    print_error "Java is not installed. Please install Java 21 or later."
    exit 1
fi

# Check Node.js
if ! command_exists node; then
    print_error "Node.js is not installed. Please install Node.js 18 or later."
    exit 1
fi

# Check Bun (preferred) or npm
if command_exists bun; then
    PACKAGE_MANAGER="bun"
    print_success "Found Bun package manager"
elif command_exists npm; then
    PACKAGE_MANAGER="npm"
    print_warning "Bun not found, using npm instead"
else
    print_error "No package manager found. Please install Bun or npm."
    exit 1
fi

print_success "All prerequisites met!"

# Create .env file if it doesn't exist
if [ ! -f "$ROOT_DIR/.env" ]; then
    print_info "Creating .env file from .env.example..."
    cp "$ROOT_DIR/.env.example" "$ROOT_DIR/.env"
    print_success ".env file created. Please update it with your configuration."
else
    print_info ".env file already exists"
fi

# Create necessary directories
print_info "Creating necessary directories..."
mkdir -p "$ROOT_DIR/logs"
mkdir -p "$ROOT_DIR/uploads"
mkdir -p "$ROOT_DIR/backend/build"
mkdir -p "$ROOT_DIR/frontend/build"

# Start Docker services
print_info "Starting Docker services..."
cd "$ROOT_DIR"
if command_exists docker-compose; then
    docker-compose -f docker-compose.dev.yaml up -d
else
    docker compose -f docker-compose.dev.yaml up -d
fi

# Wait for PostgreSQL to be ready
print_info "Waiting for PostgreSQL to be ready..."
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if docker exec haven_db pg_isready -U haven >/dev/null 2>&1; then
        print_success "PostgreSQL is ready!"
        break
    fi
    attempt=$((attempt + 1))
    if [ $attempt -eq $max_attempts ]; then
        print_error "PostgreSQL failed to start in time"
        exit 1
    fi
    sleep 2
done

# Wait for Keycloak to be ready
print_info "Waiting for Keycloak to be ready..."
max_attempts=60
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -s http://localhost:8081/health/ready >/dev/null 2>&1; then
        print_success "Keycloak is ready!"
        break
    fi
    attempt=$((attempt + 1))
    if [ $attempt -eq $max_attempts ]; then
        print_warning "Keycloak is taking longer than expected to start. You may need to wait a bit more."
    fi
    sleep 2
done

# Build backend
print_info "Building backend..."
cd "$ROOT_DIR/backend"
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    # Build only the api-app project specifically
    ./gradlew :apps:api-app:clean :apps:api-app:build -x test
else
    print_error "Gradle wrapper not found"
    exit 1
fi
print_success "Backend built successfully!"

# Install frontend dependencies
print_info "Installing frontend dependencies..."
cd "$ROOT_DIR/frontend"
if [ "$PACKAGE_MANAGER" = "bun" ]; then
    bun install
else
    npm install
fi
print_success "Frontend dependencies installed!"

# Run database migrations
print_info "Running database migrations..."
cd "$ROOT_DIR/backend"
./gradlew :apps:api-app:flywayMigrate || print_warning "Flyway migration will run on application startup"

# Setup Keycloak realm (if needed)
print_info "Setting up Keycloak realm..."
# This would normally import a realm configuration
# For now, we'll just provide instructions
cat << EOF

============================================
=� Manual Keycloak Setup Required:
============================================
1. Access Keycloak at: http://localhost:8081
2. Login with: admin / admin
3. Create a new realm: 'haven'
4. Create clients:
   - haven-backend (confidential)
   - haven-frontend (public)
5. Create roles:
   - ADMIN
   - SUPERVISOR
   - CASE_MANAGER
6. Create test users with appropriate roles
============================================

EOF

# Final instructions
print_success "Bootstrap complete!"
echo ""
echo "============================================"
echo "<� Development environment is ready!"
echo "============================================"
echo ""
echo "Next steps:"
echo "1. Update .env file with your configuration"
echo "2. Configure Keycloak realm and clients"
echo "3. Run the backend: cd backend && ./gradlew :apps:api-app:bootRun"
echo "4. Run the frontend: cd frontend && $PACKAGE_MANAGER run dev"
echo ""
echo "Services running:"
echo "  - PostgreSQL: localhost:5432"
echo "  - Keycloak: http://localhost:8081"
echo "  - Kafka: localhost:9092 (if enabled)"
echo ""
echo "Run './scripts/start-backend.sh' to start the backend"
echo "Run './scripts/start-frontend.sh' to start the frontend"
echo ""