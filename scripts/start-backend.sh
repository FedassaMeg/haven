#!/bin/bash

# ============================================================================
# Start Backend Script
# Starts the Haven backend application with proper configuration
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
echo "=� Starting Haven Backend"
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

# Check if Docker services are running
print_info "Checking Docker services..."

if ! docker ps | grep -q haven_db; then
    print_warning "PostgreSQL container is not running"
    print_info "Starting Docker services..."
    cd "$ROOT_DIR"
    if command -v docker-compose >/dev/null 2>&1; then
        docker-compose -f docker-compose.dev.yaml up -d
    else
        docker compose -f docker-compose.dev.yaml up -d
    fi
    
    # Wait for PostgreSQL
    print_info "Waiting for PostgreSQL to be ready..."
    sleep 5
    max_attempts=30
    attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if docker exec haven_db pg_isready -U haven >/dev/null 2>&1; then
            print_success "PostgreSQL is ready!"
            break
        fi
        attempt=$((attempt + 1))
        if [ $attempt -eq $max_attempts ]; then
            print_error "PostgreSQL failed to start"
            exit 1
        fi
        sleep 2
    done
else
    print_success "PostgreSQL is running"
fi

# Check Keycloak (required for OAuth2)
print_info "Checking Keycloak availability..."
if ! curl -s http://localhost:8081/health/ready >/dev/null 2>&1; then
    print_warning "Keycloak is not accessible at http://localhost:8081"
    print_info "Attempting to start Keycloak with Docker..."
    
    # Try to start Keycloak if it's not running
    if docker ps | grep -q keycloak; then
        print_info "Keycloak container exists but may not be ready"
    elif docker ps -a | grep -q keycloak; then
        print_info "Starting existing Keycloak container..."
        docker start keycloak
    else
        print_info "Creating and starting Keycloak container..."
        docker run -d --name keycloak \
            -p 8081:8080 \
            -e KEYCLOAK_ADMIN=admin \
            -e KEYCLOAK_ADMIN_PASSWORD=admin \
            quay.io/keycloak/keycloak:latest start-dev
    fi
    
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
            print_error "Keycloak failed to start within timeout"
            print_info "Please start Keycloak manually and try again"
            exit 1
        fi
        sleep 2
    done
else
    print_success "Keycloak is running"
fi

# Navigate to backend directory
cd "$ROOT_DIR/backend"

# Check if Gradle wrapper exists
if [ ! -f "./gradlew" ]; then
    print_error "Gradle wrapper not found in backend directory"
    exit 1
fi

chmod +x ./gradlew

# Build if needed
if [ ! -d "apps/api-app/build" ]; then
    print_info "Building backend application..."
    ./gradlew :apps:api-app:clean :apps:api-app:build -x test
fi

# Set JVM options
export JAVA_OPTS="-Xmx2g -Xms512m"

# Set active profile
if [ -z "$APP_ENV" ]; then
    export APP_ENV="dev"
fi

print_info "Starting backend with profile: $APP_ENV"

# Display service URLs
echo ""
echo "============================================"
echo "< Service URLs:"
echo "============================================"
echo "API Base URL:        http://localhost:${APP_PORT:-8080}/api"
echo "Swagger UI:          http://localhost:${APP_PORT:-8080}/api/swagger-ui.html"
echo "Actuator Health:     http://localhost:${APP_PORT:-8080}/api/actuator/health"
echo "Actuator Metrics:    http://localhost:${APP_PORT:-8080}/api/actuator/metrics"
echo ""
echo "External Services:"
echo "PostgreSQL:          localhost:5432"
echo "Keycloak:           http://localhost:8081"
echo "Keycloak Admin:     http://localhost:8081/admin (admin/admin)"
echo "============================================"
echo ""

# Run the application
print_info "Starting Spring Boot application..."
print_info "Press Ctrl+C to stop"
echo ""

# Run with Spring Boot DevTools for hot reload in dev mode
if [ "$APP_ENV" = "dev" ] || [ "$APP_ENV" = "development" ]; then
    ./gradlew :apps:api-app:bootRun \
        --args="--spring.profiles.active=$APP_ENV" \
        -Dspring-boot.run.jvmArguments="$JAVA_OPTS -Dspring.devtools.restart.enabled=true"
else
    ./gradlew :apps:api-app:bootRun \
        --args="--spring.profiles.active=$APP_ENV" \
        -Dspring-boot.run.jvmArguments="$JAVA_OPTS"
fi