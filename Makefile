# Haven Case Management Platform - Makefile
# ============================================================================

# Default shell
SHELL := /bin/bash

# Variables
BACKEND_DIR := backend
FRONTEND_DIR := frontend
INFRA_DIR := infra
SCRIPTS_DIR := scripts

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[0;33m
BLUE := \033[0;36m
NC := \033[0m # No Color

# ============================================================================
# Help
# ============================================================================
.PHONY: help
help:
	@echo "$(BLUE)============================================$(NC)"
	@echo "$(BLUE)Haven Case Management Platform - Commands$(NC)"
	@echo "$(BLUE)============================================$(NC)"
	@echo ""
	@echo "$(GREEN)Setup & Bootstrap:$(NC)"
	@echo "  make bootstrap     - Complete initial setup"
	@echo "  make install       - Install all dependencies"
	@echo "  make env           - Create .env from .env.example"
	@echo ""
	@echo "$(GREEN)Development:$(NC)"
	@echo "  make dev           - Start all services in dev mode"
	@echo "  make dev-backend   - Start backend only"
	@echo "  make dev-frontend  - Start frontend only"
	@echo "  make docker-up     - Start Docker services"
	@echo "  make docker-down   - Stop Docker services"
	@echo ""
	@echo "$(GREEN)Building:$(NC)"
	@echo "  make build         - Build all applications"
	@echo "  make backend       - Build backend only"
	@echo "  make frontend      - Build frontend only"
	@echo "  make build-docker  - Build Docker images"
	@echo ""
	@echo "$(GREEN)Testing:$(NC)"
	@echo "  make test          - Run all tests"
	@echo "  make test-backend  - Run backend tests"
	@echo "  make test-frontend - Run frontend tests"
	@echo "  make test-e2e      - Run E2E tests"
	@echo "  make coverage      - Generate test coverage"
	@echo ""
	@echo "$(GREEN)Code Quality:$(NC)"
	@echo "  make lint          - Run linters"
	@echo "  make format        - Format code"
	@echo "  make check         - Run all checks"
	@echo ""
	@echo "$(GREEN)Database:$(NC)"
	@echo "  make db-migrate    - Run database migrations"
	@echo "  make db-rollback   - Rollback last migration"
	@echo "  make db-reset      - Reset database"
	@echo ""
	@echo "$(GREEN)Infrastructure:$(NC)"
	@echo "  make infra         - Preview infrastructure"
	@echo "  make infra-up      - Deploy infrastructure"
	@echo "  make infra-destroy - Destroy infrastructure"
	@echo ""
	@echo "$(GREEN)Utilities:$(NC)"
	@echo "  make clean         - Clean all build artifacts"
	@echo "  make logs          - Show application logs"
	@echo "  make status        - Check service status"
	@echo ""

# ============================================================================
# Setup & Bootstrap
# ============================================================================
.PHONY: bootstrap install env

bootstrap:
	@echo "$(BLUE)Bootstrapping development environment...$(NC)"
	@$(SCRIPTS_DIR)/bootstrap.sh

install: install-backend install-frontend

install-backend:
	@echo "$(BLUE)Installing backend dependencies...$(NC)"
	@cd $(BACKEND_DIR) && ./gradlew dependencies

install-frontend:
	@echo "$(BLUE)Installing frontend dependencies...$(NC)"
	@cd $(FRONTEND_DIR) && bun install || npm install

env:
	@if [ ! -f .env ]; then \
		echo "$(BLUE)Creating .env file...$(NC)"; \
		cp .env.example .env; \
		echo "$(GREEN)✓ .env file created. Please update with your configuration.$(NC)"; \
	else \
		echo "$(YELLOW).env file already exists$(NC)"; \
	fi

# ============================================================================
# Development
# ============================================================================
.PHONY: dev dev-backend dev-frontend docker-up docker-down

dev: docker-up
	@echo "$(BLUE)Starting development environment...$(NC)"
	@make -j2 dev-backend dev-frontend

dev-backend:
	@echo "$(BLUE)Starting backend...$(NC)"
	@$(SCRIPTS_DIR)/start-backend.sh

dev-frontend:
	@echo "$(BLUE)Starting frontend...$(NC)"
	@$(SCRIPTS_DIR)/start-frontend.sh

docker-up:
	@echo "$(BLUE)Starting Docker services...$(NC)"
	@docker compose -f docker-compose.dev.yaml up -d

docker-down:
	@echo "$(BLUE)Stopping Docker services...$(NC)"
	@docker compose -f docker-compose.dev.yaml down

docker-restart: docker-down docker-up

# ============================================================================
# Building
# ============================================================================
.PHONY: build backend frontend build-docker

build:
	@echo "$(BLUE)Building all applications...$(NC)"
	@$(SCRIPTS_DIR)/build-all.sh

backend:
	@echo "$(BLUE)Building backend...$(NC)"
	@cd $(BACKEND_DIR) && ./gradlew :apps:api-app:clean :apps:api-app:build

frontend:
	@echo "$(BLUE)Building frontend...$(NC)"
	@cd $(FRONTEND_DIR) && bun run build || npm run build

build-docker:
	@echo "$(BLUE)Building Docker images...$(NC)"
	@docker compose -f docker-compose.dev.yaml build

# ============================================================================
# Testing
# ============================================================================
.PHONY: test test-backend test-frontend test-e2e coverage

test:
	@echo "$(BLUE)Running all tests...$(NC)"
	@$(SCRIPTS_DIR)/test.sh

test-backend:
	@echo "$(BLUE)Running backend tests...$(NC)"
	@$(SCRIPTS_DIR)/test.sh --backend-only

test-frontend:
	@echo "$(BLUE)Running frontend tests...$(NC)"
	@$(SCRIPTS_DIR)/test.sh --frontend-only

test-e2e:
	@echo "$(BLUE)Running E2E tests...$(NC)"
	@$(SCRIPTS_DIR)/test.sh --integration

coverage:
	@echo "$(BLUE)Generating test coverage...$(NC)"
	@$(SCRIPTS_DIR)/test.sh --coverage

# ============================================================================
# Code Quality
# ============================================================================
.PHONY: lint format check

lint:
	@echo "$(BLUE)Running linters...$(NC)"
	@$(SCRIPTS_DIR)/lint.sh

format:
	@echo "$(BLUE)Formatting code...$(NC)"
	@cd $(BACKEND_DIR) && ./gradlew spotlessApply || true
	@cd $(FRONTEND_DIR) && npx prettier --write "**/*.{js,jsx,ts,tsx,json,css,md}"

check: lint test
	@echo "$(GREEN)✓ All checks passed!$(NC)"

# ============================================================================
# Database
# ============================================================================
.PHONY: db-migrate db-rollback db-reset db-seed

db-migrate:
	@echo "$(BLUE)Running database migrations...$(NC)"
	@cd $(BACKEND_DIR) && ./gradlew :apps:api-app:flywayMigrate

db-rollback:
	@echo "$(BLUE)Rolling back last migration...$(NC)"
	@cd $(BACKEND_DIR) && ./gradlew :apps:api-app:flywayUndo

db-reset:
	@echo "$(RED)Resetting database...$(NC)"
	@docker exec -it haven_db psql -U haven -c "DROP SCHEMA IF EXISTS haven CASCADE; DROP SCHEMA IF EXISTS audit CASCADE; DROP SCHEMA IF EXISTS event_store CASCADE;"
	@make db-migrate

db-seed:
	@echo "$(BLUE)Seeding database...$(NC)"
	@docker exec -i haven_db psql -U haven < $(BACKEND_DIR)/src/main/resources/db/seed/test-data.sql || echo "Seed file not found"

# ============================================================================
# Infrastructure
# ============================================================================
.PHONY: infra infra-up infra-destroy

infra:
	@echo "$(BLUE)Previewing infrastructure...$(NC)"
	@cd $(INFRA_DIR) && npm install && npx pulumi preview

infra-up:
	@echo "$(BLUE)Deploying infrastructure...$(NC)"
	@cd $(INFRA_DIR) && npx pulumi up

infra-destroy:
	@echo "$(RED)Destroying infrastructure...$(NC)"
	@cd $(INFRA_DIR) && npx pulumi destroy

# ============================================================================
# Utilities
# ============================================================================
.PHONY: clean logs status ps

clean:
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	@cd $(BACKEND_DIR) && ./gradlew clean
	@rm -rf $(FRONTEND_DIR)/node_modules $(FRONTEND_DIR)/.next $(FRONTEND_DIR)/dist
	@rm -rf dist/ logs/ uploads/
	@echo "$(GREEN)✓ Clean complete$(NC)"

logs:
	@docker compose -f docker-compose.dev.yaml logs -f

logs-backend:
	@tail -f logs/haven.log || echo "No backend logs found"

logs-frontend:
	@cd $(FRONTEND_DIR) && npm run dev 2>&1 | tee -a ../logs/frontend.log

status:
	@echo "$(BLUE)Checking service status...$(NC)"
	@echo ""
	@echo "Docker Services:"
	@docker compose -f docker-compose.dev.yaml ps
	@echo ""
	@echo "Backend Health:"
	@curl -s http://localhost:8080/api/actuator/health | jq '.' || echo "Backend not running"
	@echo ""
	@echo "Frontend Status:"
	@curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:3000 || echo "Frontend not running"

ps:
	@docker compose -f docker-compose.dev.yaml ps

# ============================================================================
# Git Hooks
# ============================================================================
.PHONY: install-hooks

install-hooks:
	@echo "$(BLUE)Installing git hooks...$(NC)"
	@echo "#!/bin/bash" > .git/hooks/pre-commit
	@echo "make lint" >> .git/hooks/pre-commit
	@chmod +x .git/hooks/pre-commit
	@echo "$(GREEN)✓ Git hooks installed$(NC)"

# ============================================================================
# Default target
# ============================================================================
.DEFAULT_GOAL := help

all: clean install build test