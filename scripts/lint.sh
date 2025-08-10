#!/bin/bash

# ============================================================================
# Lint Script
# Runs linting and code quality checks for both backend and frontend
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "= Running Code Quality Checks"
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

ERRORS_FOUND=0

# Backend linting
print_info "Running backend checks..."
cd "$ROOT_DIR/backend"

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    
    # Run checkstyle if configured
    if ./gradlew tasks --all | grep -q "checkstyleMain"; then
        print_info "Running Checkstyle..."
        ./gradlew checkstyleMain checkstyleTest || {
            print_warning "Checkstyle found issues"
            ERRORS_FOUND=1
        }
    fi
    
    # Run SpotBugs if configured
    if ./gradlew tasks --all | grep -q "spotbugsMain"; then
        print_info "Running SpotBugs..."
        ./gradlew spotbugsMain spotbugsTest || {
            print_warning "SpotBugs found issues"
            ERRORS_FOUND=1
        }
    fi
    
    # Run PMD if configured
    if ./gradlew tasks --all | grep -q "pmdMain"; then
        print_info "Running PMD..."
        ./gradlew pmdMain pmdTest || {
            print_warning "PMD found issues"
            ERRORS_FOUND=1
        }
    fi
    
    # Compile to check for errors
    print_info "Compiling backend code..."
    ./gradlew compileJava compileTestJava || {
        print_error "Backend compilation failed!"
        exit 1
    }
    
    print_success "Backend checks completed"
else
    print_error "Gradle wrapper not found"
    exit 1
fi

# Frontend linting
print_info "Running frontend checks..."
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

print_info "Using $PACKAGE_MANAGER for frontend"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    print_info "Installing frontend dependencies..."
    $PACKAGE_MANAGER install
fi

# Run ESLint
if [ -f "package.json" ] && grep -q '"lint"' package.json; then
    print_info "Running ESLint..."
    $PACKAGE_MANAGER run lint || {
        print_warning "ESLint found issues"
        ERRORS_FOUND=1
    }
else
    print_warning "No lint script found in package.json"
fi

# Run TypeScript type checking
if [ -f "package.json" ] && grep -q '"typecheck"' package.json; then
    print_info "Running TypeScript type checking..."
    $PACKAGE_MANAGER run typecheck || {
        print_warning "TypeScript found type errors"
        ERRORS_FOUND=1
    }
else
    print_warning "No typecheck script found in package.json"
fi

# Run Prettier check
if command -v npx >/dev/null 2>&1; then
    if [ -f ".prettierrc" ] || [ -f ".prettierrc.json" ] || [ -f ".prettierrc.js" ]; then
        print_info "Running Prettier check..."
        npx prettier --check "**/*.{js,jsx,ts,tsx,json,css,scss,md}" --ignore-path .gitignore || {
            print_warning "Prettier found formatting issues"
            print_info "Run 'npx prettier --write \"**/*.{js,jsx,ts,tsx,json,css,scss,md}\"' to fix"
            ERRORS_FOUND=1
        }
    fi
fi

print_success "Frontend checks completed"

# Summary
echo ""
echo "============================================"
if [ $ERRORS_FOUND -eq 0 ]; then
    echo " All checks passed!"
    echo "============================================"
    exit 0
else
    echo "   Some checks found issues"
    echo "============================================"
    echo ""
    echo "Please review the warnings above and fix any issues."
    echo "This is a non-blocking warning - continuing..."
    exit 0
fi