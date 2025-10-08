# Keycloak Role Alignment Implementation Guide

**Version:** 2.0.0
**Date:** 2025-10-07
**Status:** Implementation Complete

## Overview

This document describes the implementation of standardized role alignment between Keycloak and the Haven application, including new roles for community engagement, DV advocacy, compliance, and executive functions.

## Table of Contents

1. [Architecture](#architecture)
2. [New Roles](#new-roles)
3. [Setup Instructions](#setup-instructions)
4. [Database Schema](#database-schema)
5. [Synchronization](#synchronization)
6. [Validation](#validation)
7. [API Reference](#api-reference)
8. [Troubleshooting](#troubleshooting)

---

## Architecture

### Components

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│   Keycloak      │ ◄─────► │  Sync Service    │ ◄─────► │   Database      │
│  (Realm Roles)  │  REST   │  (Java Service)  │  JDBC   │  (RBAC Tables)  │
└─────────────────┘         └──────────────────┘         └─────────────────┘
         │                           │                            │
         │ JWT Tokens                │ Scheduled/Manual           │
         ▼                           ▼                            ▼
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│   Frontend      │         │  CLI Command     │         │  Audit Log      │
│  (React/TS)     │         │  (Gradle Task)   │         │  (rbac_sync_log)│
└─────────────────┘         └──────────────────┘         └─────────────────┘
```

### Data Flow

1. **Keycloak** manages realm roles and user assignments
2. **JWT Tokens** carry role claims to frontend/API
3. **Sync Service** reconciles Keycloak roles → Database
4. **Database** stores role metadata and permissions
5. **Application** enforces RBAC using database permissions

---

## New Roles

### Role Definitions

| Role Name | Keycloak ID | Database Enum | Description | Composite | MFA Required |
|-----------|-------------|---------------|-------------|-----------|--------------|
| **ce-intake** | Auto-assigned | CE_INTAKE | Community Engagement & Intake Specialist | No | No |
| **dv-advocate** | Auto-assigned | DV_ADVOCATE | Domestic Violence Advocate | No | No |
| **compliance-auditor** | Auto-assigned | COMPLIANCE_AUDITOR | Compliance & Audit Officer | No | Yes |
| **exec** | Auto-assigned | EXEC | Executive Director/Leadership | Yes* | Yes |

*Composite of: supervisor + compliance-auditor + report-viewer

### Role Permission Matrix

| Permission | Admin | Supervisor | Case Mgr | CE Intake | DV Advocate | Compliance | Exec | Report Viewer |
|------------|-------|------------|----------|-----------|-------------|------------|------|---------------|
| CLIENT:CREATE | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✗ |
| CLIENT:READ (All) | ✓ | ✓ | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ |
| CLIENT:READ (Own) | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✗ |
| CASE:UPDATE | ✓ | ✓ | ✓ (own) | ✗ | ✓ (own) | ✗ | ✓ | ✗ |
| CASE:AUDIT | ✓ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| REPORT:READ | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ |
| REPORT:EXPORT | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| SYSTEM:ADMIN | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |

---

## Setup Instructions

### Prerequisites

- Docker running with Keycloak container
- PostgreSQL database accessible
- Gradle 8.x or higher
- PowerShell 5.1+ (Windows) or Bash (Linux/Mac)

### Step 1: Configure Keycloak Roles

Run the role alignment script:

```powershell
# Windows
.\scripts\setup-keycloak-roles-aligned.ps1

# Linux/Mac
./scripts/setup-keycloak-roles-aligned.sh
```

This script will:
- Create/update all realm roles
- Configure composite role `exec`
- Create client scopes: `portal-api`, `export-service`, `admin-console`
- Configure protocol mappers for `haven_roles` claim

### Step 2: Run Database Migration

Execute the Flyway migration:

```bash
# Via Gradle
./gradlew flywayMigrate

# Or start the application (auto-migration)
./gradlew bootRun
```

Migration `V40__rbac_role_alignment.sql` will:
- Extend `user_role` enum with new roles
- Create RBAC metadata tables
- Seed role definitions and permissions
- Configure composite role hierarchy

### Step 3: Export Realm Configuration

Generate versioned realm export:

```powershell
.\scripts\export-keycloak-realm.ps1
```

This creates:
- `infrastructure/keycloak/haven-realm-v2.json` - Full realm export
- `infrastructure/keycloak/haven-roles-v2.json` - Roles only
- `infrastructure/keycloak/RELEASE_NOTES_v2.md` - Version documentation

Commit these files to version control.

### Step 4: Create Test Users

Generate test users for each new role:

```powershell
.\scripts\create-test-users-aligned.ps1
```

Test credentials:
- **ce.intake.test** / intake123
- **dv.advocate.test** / advocate123
- **compliance.test** / audit123
- **exec.test** / exec123

### Step 5: Validate Role Propagation

Run end-to-end validation:

```powershell
.\scripts\validate-role-propagation.ps1
```

This script validates:
- ✓ Users can authenticate with Keycloak
- ✓ JWT tokens contain expected roles
- ✓ Custom `haven_roles` claim is present
- ✓ Token expiration and issuer are correct

---

## Database Schema

### Tables

#### `rbac_roles`
Core role definitions synchronized with Keycloak.

```sql
CREATE TABLE rbac_roles (
    id UUID PRIMARY KEY,
    keycloak_role_id VARCHAR(255) UNIQUE,  -- Keycloak UUID
    role_name VARCHAR(100) UNIQUE,         -- kebab-case
    role_enum user_role UNIQUE,            -- SCREAMING_SNAKE_CASE
    display_name VARCHAR(150),
    description TEXT,
    is_composite BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    requires_mfa BOOLEAN DEFAULT false,
    session_timeout_minutes INTEGER DEFAULT 480,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);
```

#### `rbac_role_composites`
Composite role hierarchy (e.g., exec includes supervisor).

```sql
CREATE TABLE rbac_role_composites (
    parent_role_id UUID REFERENCES rbac_roles(id),
    child_role_id UUID REFERENCES rbac_roles(id),
    PRIMARY KEY (parent_role_id, child_role_id)
);
```

#### `rbac_permissions`
Fine-grained permission definitions.

```sql
CREATE TABLE rbac_permissions (
    id UUID PRIMARY KEY,
    resource_type VARCHAR(100),  -- CLIENT, CASE, REPORT, etc.
    action VARCHAR(50),          -- CREATE, READ, UPDATE, DELETE
    scope VARCHAR(50),           -- ALL, OWN, TEAM, DEPARTMENT
    description TEXT
);
```

#### `rbac_role_permissions`
Many-to-many role → permission mapping.

#### `rbac_sync_log`
Audit log for synchronization operations.

---

## Synchronization

### Automatic Sync (Scheduled)

Configure scheduled sync in `application.properties`:

```properties
# Enable scheduled sync (every 6 hours)
keycloak.sync.enabled=true
keycloak.sync.cron=0 0 */6 * * *
```

### Manual Sync via CLI

```bash
# Full synchronization
./gradlew bootRun --args="--keycloak.sync.mode=full"

# Incremental synchronization
./gradlew bootRun --args="--keycloak.sync.mode=incremental"
```

### Manual Sync via API

```bash
# Full sync
curl -X POST http://localhost:8080/api/admin/rbac-sync/sync/full \
  -H "Authorization: Bearer $TOKEN"

# Incremental sync
curl -X POST http://localhost:8080/api/admin/rbac-sync/sync/incremental \
  -H "Authorization: Bearer $TOKEN"

# Get sync status
curl http://localhost:8080/api/admin/rbac-sync/sync-status \
  -H "Authorization: Bearer $TOKEN"
```

### Drift Detection

The sync service detects and logs:
- Roles in Keycloak but not in database
- Roles in database but not in Keycloak
- Role metadata mismatches (name, description)

View drift reports:

```bash
curl http://localhost:8080/api/admin/rbac-sync/drift-detections \
  -H "Authorization: Bearer $TOKEN"
```

---

## Validation

### Token Validation

Decode JWT to verify role claims:

```javascript
// Decode JWT payload (base64)
const [header, payload, signature] = token.split('.');
const decoded = JSON.parse(atob(payload));

// Check realm roles
console.log(decoded.realm_access.roles);
// Expected: ["ce-intake", "dv-advocate", ...]

// Check custom claim
console.log(decoded.haven_roles);
// Expected: ["CE_INTAKE", "DV_ADVOCATE", ...]
```

### Frontend Role Check

```typescript
import { useAuth } from '@haven/auth';

function MyComponent() {
  const { hasRole } = useAuth();

  if (hasRole('ce-intake')) {
    return <IntakeWorkflow />;
  }

  if (hasRole('exec')) {
    return <ExecutiveDashboard />;
  }

  return <Unauthorized />;
}
```

### Backend Authorization

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class IntakeController {

    @GetMapping("/api/intake/cases")
    @PreAuthorize("hasAnyRole('CE_INTAKE', 'ADMIN')")
    public ResponseEntity<List<Case>> getIntakeCases() {
        // Only accessible by CE_INTAKE or ADMIN roles
    }
}
```

---

## API Reference

### RbacSyncController

Base URL: `/api/admin/rbac-sync`

#### POST /sync/full
Perform full synchronization from Keycloak.

**Response:**
```json
{
  "syncType": "FULL",
  "startTime": "2025-10-07T10:00:00Z",
  "endTime": "2025-10-07T10:00:05Z",
  "rolesAdded": 0,
  "rolesUpdated": 4,
  "rolesRemoved": 0,
  "driftDetected": false,
  "driftDetails": [],
  "status": "SUCCESS"
}
```

#### GET /roles
List all RBAC roles with sync status.

**Response:**
```json
[
  {
    "id": "uuid",
    "keycloakRoleId": "keycloak-uuid",
    "roleName": "ce-intake",
    "roleEnum": "CE_INTAKE",
    "displayName": "Community Engagement & Intake",
    "isComposite": false,
    "requiresMfa": false,
    "syncStatus": "SYNCED"
  }
]
```

#### GET /sync-history
Get sync operation history (last 30 days).

#### GET /drift-detections
Get recent drift detections.

---

## Troubleshooting

### Issue: Roles not appearing in tokens

**Symptoms:** JWT doesn't contain expected roles
**Cause:** Role not assigned to user or client scope not configured
**Fix:**
1. Verify role assignment in Keycloak Admin Console
2. Check client scopes are assigned to `haven-frontend` client
3. Ensure protocol mappers are configured correctly

```powershell
.\scripts\setup-keycloak-roles-aligned.ps1
```

### Issue: Sync job reports drift

**Symptoms:** `driftDetected: true` in sync results
**Cause:** Mismatch between Keycloak and database
**Fix:**
1. Review drift details in sync log
2. Manually reconcile missing roles
3. Re-run full sync

```bash
./gradlew bootRun --args="--keycloak.sync.mode=full"
```

### Issue: Composite role not working

**Symptoms:** `exec` role doesn't inherit child role permissions
**Cause:** Composite relationships not configured
**Fix:**
1. Verify composite setup in Keycloak Admin Console
2. Check `rbac_role_composites` table has correct entries

```sql
SELECT * FROM haven.rbac_role_composites
WHERE parent_role_id = (SELECT id FROM haven.rbac_roles WHERE role_name = 'exec');
```

### Issue: Database migration fails

**Symptoms:** `V40__rbac_role_alignment.sql` migration error
**Cause:** Enum value conflicts or missing prerequisites
**Fix:**
1. Ensure V1 migration ran successfully
2. Check for existing conflicting enum values
3. Manually add enum values if needed

```sql
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'CE_INTAKE';
```

---

## Next Steps

1. ✅ Configure role-based UI components
2. ✅ Implement permission checks in backend services
3. ✅ Add integration tests for RBAC enforcement
4. ✅ Configure MFA for sensitive roles (exec, compliance-auditor)
5. ✅ Set up monitoring/alerts for sync failures
6. ✅ Document role assignment workflows for admins

---

## References

- [KEYCLOAK_SETUP.md](./KEYCLOAK_SETUP.md) - Initial Keycloak setup
- [KEYCLOAK_ROLE_INVENTORY.md](./KEYCLOAK_ROLE_INVENTORY.md) - Role gap analysis
- [V40__rbac_role_alignment.sql](./backend/apps/api-app/src/main/resources/db/migration/V40__rbac_role_alignment.sql) - Database schema
- [KeycloakRoleSyncService.java](./backend/shared-kernel/src/main/java/org/haven/shared/rbac/KeycloakRoleSyncService.java) - Sync service implementation

---

**Version History:**
- v2.0.0 (2025-10-07): Role alignment implementation with new roles
- v1.0.0 (2024-XX-XX): Initial Keycloak setup
