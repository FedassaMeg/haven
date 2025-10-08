# Keycloak Role Alignment Inventory

**Generated:** 2025-10-07
**Purpose:** Inventory current Keycloak setup and compare to KEYCLOAK_SETUP.md expectations

## Current State Analysis

### Existing Realm Roles (from scripts)
Based on `setup-keycloak-complete.ps1` and `configure-keycloak.ps1`:

1. **admin** - System Administrator with full access
2. **case-manager** - Case Manager with case management access
3. **supervisor** - Supervisor with team oversight access
4. **social-worker** - Social Worker with client access
5. **viewer** - Read-only access
6. **case_manager** (variant)
7. **social_worker** (variant)

### Database Roles (from V1 migration)
SQL enum `user_role`:
1. **ADMIN**
2. **SUPERVISOR**
3. **CASE_MANAGER**
4. **INTAKE_SPECIALIST**
5. **REPORT_VIEWER**
6. **EXTERNAL_PARTNER**

### Frontend Role Enums (TypeScript)
From `frontend/packages/auth/src/types.ts`:
1. **admin**
2. **case-manager**
3. **social-worker**
4. **supervisor**
5. **viewer**

## Gap Analysis

### Missing Required Roles
Per the task requirements, we need to add:
1. ✗ **ce_intake** - Community Engagement & Intake Specialist (NEW)
2. ✗ **dv_advocate** - Domestic Violence Advocate (NEW)
3. ✗ **compliance_auditor** - Compliance & Audit role (NEW)
4. ✗ **exec** - Executive role with composite permissions (NEW)

### Naming Inconsistencies
- Keycloak uses kebab-case: `case-manager`, `social-worker`
- Database uses SCREAMING_SNAKE_CASE: `CASE_MANAGER`, `INTAKE_SPECIALIST`
- Need standardization strategy

### Client Configuration Gaps
Current clients:
- **haven-frontend** (public, PKCE-enabled)
- **haven-backend** (confidential, service account)

**Missing:**
- ✗ Client scopes for role-based access
- ✗ Protocol mappers for custom claims
- ✗ Dedicated client scopes: `portal-api`, `export-service`

### Role Mapper Configuration
**Status:** NOT CONFIGURED
- No ID token mappers detected
- No client role mappers for custom claim names
- Tokens likely use default Keycloak claim structure

## Recommended Actions

### 1. Role Standardization
**Decision:** Use kebab-case for Keycloak, map to SCREAMING_SNAKE_CASE in application

| Keycloak Role | Database Enum | Description |
|---------------|---------------|-------------|
| admin | ADMIN | System administrator |
| supervisor | SUPERVISOR | Program supervisor |
| case-manager | CASE_MANAGER | Case management |
| intake-specialist | INTAKE_SPECIALIST | Intake coordination |
| social-worker | (deprecated) | Legacy role |
| viewer | REPORT_VIEWER | Read-only reporting |
| ce-intake | CE_INTAKE | **NEW** Community engagement intake |
| dv-advocate | DV_ADVOCATE | **NEW** DV advocacy specialist |
| compliance-auditor | COMPLIANCE_AUDITOR | **NEW** Compliance & audit |
| exec | EXEC | **NEW** Executive (composite) |

### 2. Composite Role Design
**exec** role should include:
- ✓ All permissions from `supervisor`
- ✓ Read access from `compliance-auditor`
- ✓ Strategic reporting from `report-viewer`
- ✓ Program-level access (not case-level details)

### 3. Client Scope Mapping
Create dedicated client scopes:
- **portal-api** - Main application API access
- **export-service** - HMIS export & reporting
- **admin-console** - Administrative functions

### 4. Token Claim Mapping
Configure mappers to emit:
```json
{
  "realm_access": {
    "roles": ["ce-intake", "dv-advocate"]
  },
  "resource_access": {
    "portal-api": {
      "roles": ["case:read", "case:write"]
    }
  },
  "custom_claims": {
    "haven_roles": ["CE_INTAKE"],
    "haven_permissions": ["CLIENT_VIEW", "INTAKE_CREATE"]
  }
}
```

## Keycloak Setup Expectations vs Current State

### From KEYCLOAK_SETUP.md

| Expected | Current | Status |
|----------|---------|--------|
| Haven realm | ✓ Created | ✓ PASS |
| haven-backend client | ✓ Created | ✓ PASS |
| haven-frontend client | ✓ Created | ✓ PASS |
| ADMIN role | ✓ Created | ✓ PASS |
| CASE_WORKER role | ✓ Created (as case-manager) | ⚠️ INCONSISTENT |
| COUNSELOR role | ✗ Missing | ✗ FAIL |
| VOLUNTEER role | ✗ Missing | ✗ FAIL |
| VIEWER role | ✓ Created | ✓ PASS |
| Test users | ✓ Created (admin.test, case.manager) | ✓ PASS |
| Token configuration | ✗ Not configured | ✗ FAIL |
| Client scopes | ✗ Not configured | ✗ FAIL |

## Implementation Checklist

- [ ] Create new roles: ce-intake, dv-advocate, compliance-auditor, exec
- [ ] Create composite role definition for exec
- [ ] Create client scopes: portal-api, export-service, admin-console
- [ ] Configure ID token mappers for custom claims
- [ ] Configure client role mappers
- [ ] Export realm configuration to JSON
- [ ] Create database migration for new roles
- [ ] Update application role mapping logic
- [ ] Create sync job for Keycloak → RBAC reconciliation
- [ ] Add integration tests for role propagation
- [ ] Document role permission matrix

## Security Considerations

1. **Least Privilege:** Ensure exec role doesn't grant case-level PII access
2. **Audit Trail:** Log all role assignments and changes
3. **Separation of Duties:** Compliance auditor cannot modify data they audit
4. **Token Expiry:** Configure appropriate token lifespans per role sensitivity
5. **MFA Requirements:** Consider requiring MFA for exec and compliance-auditor roles

## Next Steps

1. Start Keycloak container if not running
2. Execute role creation script
3. Configure client scopes and mappers
4. Export realm JSON
5. Create database migration
6. Implement sync job
7. Run end-to-end validation
