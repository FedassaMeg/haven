# Haven DV Case Management Platform (Monorepo)

Cloud-native, modular monolith for DV & human services case management.

## Stack
- **Backend:** Spring Boot, PostgreSQL, Hibernate, OAuth2, CQRS + Event Sourcing hooks.
- **Frontend:** Next.js (TypeScript) w/ bun workspaces.
- **Infra:** Pulumi (multi-cloud; AWS default), Docker, Kubernetes.

## Quick Start (Dev)
```bash
# 1. copy env
cp .env.example .env

# 2. start infra (postgres, keycloak, localstack)
docker compose -f docker-compose.dev.yml up -d

# 3. backend build & run (monolith API)
( cd backend && ./gradlew :apps:api-app:bootRun )

# 4. frontend dev (case manager portal)
( cd frontend && bun install && bun run dev -w cm-portal )