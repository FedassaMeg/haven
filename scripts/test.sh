#!/bin/bash

# ============================================================================
# Test Script
# Runs all tests for both backend and frontend
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Load environment variables
if [ -f "$ROOT_DIR/.env" ]; then
    export $(cat "$ROOT_DIR/.env" | grep -v '^#' | xargs)
fi

echo "============================================"
echo ">ê Running Haven Test Suite"
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

# Parse arguments
RUN_BACKEND=true
RUN_FRONTEND=true
RUN_INTEGRATION=false
GENERATE_COVERAGE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --backend-only)
            RUN_FRONTEND=false
            shift
            ;;
        --frontend-only)
            RUN_BACKEND=false
            shift
            ;;
        --integration)
            RUN_INTEGRATION=true
            shift
            ;;
        --coverage)
            GENERATE_COVERAGE=true
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --backend-only    Run only backend tests"
            echo "  --frontend-only   Run only frontend tests"
            echo "  --integration     Include integration tests"
            echo "  --coverage        Generate coverage reports"
            echo "  --help           Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

TOTAL_FAILURES=0

# Backend Tests
if [ "$RUN_BACKEND" = true ]; then
    echo ""
    echo "============================================"
    echo "<¯ Backend Tests"
    echo "============================================"
    
    cd "$ROOT_DIR/backend"
    
    if [ ! -f "./gradlew" ]; then
        print_error "Gradle wrapper not found"
        exit 1
    fi
    
    chmod +x ./gradlew
    
    # Start test containers if needed
    if [ "$RUN_INTEGRATION" = true ]; then
        print_info "Starting test containers for integration tests..."
        docker run -d --name test-postgres \
            -e POSTGRES_USER=test \
            -e POSTGRES_PASSWORD=test \
            -e POSTGRES_DB=haven_test \
            -p 5433:5432 \
            postgres:16-alpine || true
        
        # Wait for test database
        sleep 5
    fi
    
    # Run unit tests
    print_info "Running backend unit tests..."
    if [ "$GENERATE_COVERAGE" = true ]; then
        ./gradlew test jacocoTestReport
    else
        ./gradlew test
    fi
    
    if [ $? -eq 0 ]; then
        print_success "Backend unit tests passed"
    else
        print_error "Backend unit tests failed"
        TOTAL_FAILURES=$((TOTAL_FAILURES + 1))
    fi
    
    # Run integration tests if requested
    if [ "$RUN_INTEGRATION" = true ]; then
        print_info "Running backend integration tests..."
        ./gradlew integrationTest || {
            print_warning "Integration tests failed or not configured"
            TOTAL_FAILURES=$((TOTAL_FAILURES + 1))
        }
    fi
    
    # Clean up test containers
    if [ "$RUN_INTEGRATION" = true ]; then
        docker stop test-postgres >/dev/null 2>&1
        docker rm test-postgres >/dev/null 2>&1
    fi
    
    # Display coverage report location
    if [ "$GENERATE_COVERAGE" = true ] && [ -d "build/reports/jacoco" ]; then
        print_info "Backend coverage report: file://$ROOT_DIR/backend/build/reports/jacoco/test/html/index.html"
    fi
fi

# Frontend Tests
if [ "$RUN_FRONTEND" = true ]; then
    echo ""
    echo "============================================"
    echo "<¯ Frontend Tests"
    echo "============================================"
    
    cd "$ROOT_DIR/frontend"
    
    # Detect package manager
    if [ -f "bun.lockb" ] || command -v bun >/dev/null 2>&1; then
        PACKAGE_MANAGER="bun"
    elif [ -f "package-lock.json" ]; then
        PACKAGE_MANAGER="npm"
    elif [ -f "yarn.lock" ]; then
        PACKAGE_MANAGER="yarn"
    else
        print_error "No package manager detected"
        exit 1
    fi
    
    print_info "Using $PACKAGE_MANAGER for frontend tests"
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        print_info "Installing frontend dependencies..."
        $PACKAGE_MANAGER install
    fi
    
    # Create test script if it doesn't exist
    if ! grep -q '"test"' package.json; then
        print_warning "No test script found in package.json"
        print_info "Adding vitest configuration..."
        
        # Install vitest if not present
        if [ "$PACKAGE_MANAGER" = "bun" ]; then
            bun add -D vitest @vitest/ui happy-dom
        else
            npm install -D vitest @vitest/ui happy-dom
        fi
    fi
    
    # Run tests
    print_info "Running frontend tests..."
    if [ "$GENERATE_COVERAGE" = true ]; then
        if [ "$PACKAGE_MANAGER" = "bun" ]; then
            bun test --coverage
        else
            npx vitest run --coverage
        fi
    else
        if [ "$PACKAGE_MANAGER" = "bun" ]; then
            bun test
        elif grep -q '"test"' package.json; then
            $PACKAGE_MANAGER test
        else
            npx vitest run
        fi
    fi
    
    if [ $? -eq 0 ]; then
        print_success "Frontend tests passed"
    else
        print_error "Frontend tests failed"
        TOTAL_FAILURES=$((TOTAL_FAILURES + 1))
    fi
    
    # Display coverage report location
    if [ "$GENERATE_COVERAGE" = true ] && [ -d "coverage" ]; then
        print_info "Frontend coverage report: file://$ROOT_DIR/frontend/coverage/index.html"
    fi
fi

# End-to-end tests (optional)
if [ "$RUN_INTEGRATION" = true ] && [ -d "$ROOT_DIR/e2e" ]; then
    echo ""
    echo "============================================"
    echo "<­ End-to-End Tests"
    echo "============================================"
    
    print_info "Running E2E tests..."
    cd "$ROOT_DIR/e2e"
    
    # Start services if needed
    print_info "E2E tests would run here if configured"
    # npm run e2e || TOTAL_FAILURES=$((TOTAL_FAILURES + 1))
fi

# Summary
echo ""
echo "============================================"
if [ $TOTAL_FAILURES -eq 0 ]; then
    echo " All tests passed!"
    echo "============================================"
    
    if [ "$GENERATE_COVERAGE" = true ]; then
        echo ""
        echo "Coverage reports generated:"
        [ -d "$ROOT_DIR/backend/build/reports/jacoco" ] && echo "  - Backend: backend/build/reports/jacoco/test/html/index.html"
        [ -d "$ROOT_DIR/frontend/coverage" ] && echo "  - Frontend: frontend/coverage/index.html"
    fi
    
    exit 0
else
    echo "L $TOTAL_FAILURES test suite(s) failed"
    echo "============================================"
    echo ""
    echo "Please review the test failures above."
    exit 1
fi