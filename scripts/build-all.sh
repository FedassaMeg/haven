#!/bin/bash

# ============================================================================
# Build All Script
# Builds both backend and frontend applications
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Load environment variables
if [ -f "$ROOT_DIR/.env" ]; then
    export $(cat "$ROOT_DIR/.env" | grep -v '^#' | xargs)
fi

echo "============================================"
echo "=( Building Haven Applications"
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

# Build backend
print_info "Building backend..."
cd "$ROOT_DIR/backend"

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew :apps:api-app:clean :apps:api-app:build
    
    if [ $? -eq 0 ]; then
        print_success "Backend build successful!"
    else
        print_error "Backend build failed!"
        exit 1
    fi
else
    print_error "Gradle wrapper not found"
    exit 1
fi

# Build frontend
print_info "Building frontend..."
cd "$ROOT_DIR/frontend"

# Detect package manager
if [ -f "bun.lockb" ] || command -v bun >/dev/null 2>&1; then
    PACKAGE_MANAGER="bun"
elif [ -f "package-lock.json" ] || [ -f "yarn.lock" ]; then
    if command -v npm >/dev/null 2>&1; then
        PACKAGE_MANAGER="npm"
    elif command -v yarn >/dev/null 2>&1; then
        PACKAGE_MANAGER="yarn"
    fi
else
    print_error "No package manager detected"
    exit 1
fi

print_info "Using $PACKAGE_MANAGER for frontend build"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    print_info "Installing frontend dependencies..."
    $PACKAGE_MANAGER install
fi

# Build frontend
$PACKAGE_MANAGER run build

if [ $? -eq 0 ]; then
    print_success "Frontend build successful!"
else
    print_error "Frontend build failed!"
    exit 1
fi

# Create distribution directory
print_info "Creating distribution directory..."
mkdir -p "$ROOT_DIR/dist"

# Copy backend JAR
print_info "Copying backend artifacts..."
find "$ROOT_DIR/backend/apps/api-app/build/libs" -name "*.jar" -type f -exec cp {} "$ROOT_DIR/dist/" \;

# Copy frontend build
print_info "Copying frontend artifacts..."
if [ -d "$ROOT_DIR/frontend/apps/cm-portal/.next" ]; then
    cp -r "$ROOT_DIR/frontend/apps/cm-portal/.next" "$ROOT_DIR/dist/frontend"
elif [ -d "$ROOT_DIR/frontend/apps/cm-portal/dist" ]; then
    cp -r "$ROOT_DIR/frontend/apps/cm-portal/dist" "$ROOT_DIR/dist/frontend"
fi

print_success "Build complete!"
echo ""
echo "============================================"
echo " All applications built successfully!"
echo "============================================"
echo ""
echo "Build artifacts:"
echo "  - Backend: dist/*.jar"
echo "  - Frontend: dist/frontend/"
echo ""
echo "To run in production mode:"
echo "  - Backend: java -jar dist/*.jar"
echo "  - Frontend: cd dist/frontend && npx serve"
echo ""