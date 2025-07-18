# Convenience top-level targets
.PHONY: all backend frontend infra build test clean docker-up docker-down

all: build

backend:
	cd backend && ./gradlew build

frontend:
	cd frontend && bun install && bun run build --filter=./apps/cm-portal

infra:
	cd infra && npm install && npx pulumi preview

build: backend frontend

test:
	cd backend && ./gradlew test
	cd frontend && bun run test --if-present

clean:
	cd backend && ./gradlew clean
	rm -rf frontend/node_modules frontend/.bun

docker-up:
	docker compose -f docker-compose.dev.yml up -d --build

docker-down:
	docker compose -f docker-compose.dev.yml down -v