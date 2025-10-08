# 🔐 Test User Credentials - Haven HMIS

This document contains login credentials for all test users in the Haven system. These accounts have been pre-configured with appropriate role-based permissions for testing purposes.

---

## 📋 Quick Reference Table

| Role | Username | Password | Email | Primary Use Case |
|------|----------|----------|-------|-----------------|
| **Admin** | `admin.test` | `Admin123!` | admin@haven.test | System administration & configuration |
| **Supervisor** | `supervisor.test` | `Super123!` | supervisor@haven.test | Team oversight & case management |
| **Case Manager** | `case.manager.test` | `Case123!` | case.manager@haven.test | Day-to-day case management |
| **Intake Specialist** | `intake.specialist.test` | `Intake123!` | intake@haven.test | New client intake & assessment |
| **CE Intake** | `ce.intake.test` | `CE123!` | ce.intake@haven.test | Coordinated Entry assessments |
| **DV Advocate** | `dv.advocate.test` | `DV123!` | dv.advocate@haven.test | Domestic violence services |
| **Compliance Auditor** | `compliance.test` | `Comply123!` | compliance@haven.test | Compliance & auditing |
| **Executive** | `exec.test` | `Exec123!` | exec@haven.test | Executive reporting & oversight |
| **Report Viewer** | `report.viewer.test` | `Report123!` | reports@haven.test | Read-only report access |
| **External Partner** | `external.partner.test` | `Partner123!` | external@haven.test | Limited external access |
| **Counselor** | `counselor.test` | `Counsel123!` | counselor@haven.test | Therapeutic counseling services |
| **Advocate** | `advocate.test` | `Advocate123!` | advocate@haven.test | Client advocacy & support |
| **Data Analyst** | `data.analyst.test` | `Data123!` | data.analyst@haven.test | HMIS exports & data analysis |

---

## 🔧 Setup Instructions

### Prerequisites
- Keycloak running on `http://localhost:8081`
- Haven realm configured
- Admin credentials: `admin` / `admin`

### Create Test Users

Run the PowerShell script to create all test accounts:

```powershell
cd scripts
.\create-all-role-test-users.ps1
```

This will:
1. Authenticate with Keycloak admin
2. Create all 13 test user accounts
3. Assign appropriate roles to each user
4. Display summary and credentials

---

## 👤 Detailed User Profiles

### 1. **Admin User**
- **Username:** `admin.test`
- **Password:** `Admin123!`
- **Role:** `admin`
- **Permissions:** Full system access
- **Use Cases:**
  - System configuration
  - User management
  - Role assignment
  - All CRUD operations

---

### 2. **Supervisor**
- **Username:** `supervisor.test`
- **Password:** `Super123!`
- **Role:** `supervisor`
- **Permissions:** Team oversight, case assignment, reporting
- **Use Cases:**
  - View team caseloads
  - Assign cases to case managers
  - Review team performance
  - Access all reports
  - Approve financial assistance requests

---

### 3. **Case Manager**
- **Username:** `case.manager.test`
- **Password:** `Case123!`
- **Role:** `case-manager`
- **Permissions:** Case management, client services
- **Use Cases:**
  - Manage assigned cases
  - Document service episodes
  - Update client information
  - Create restricted notes
  - Request financial assistance

---

### 4. **Intake Specialist**
- **Username:** `intake.specialist.test`
- **Password:** `Intake123!`
- **Role:** `intake-specialist`
- **Permissions:** Client intake, initial assessment
- **Use Cases:**
  - Create new client profiles
  - Conduct intake assessments
  - Open initial cases
  - Record preliminary information

---

### 5. **Coordinated Entry Intake**
- **Username:** `ce.intake.test`
- **Password:** `CE123!`
- **Role:** `ce-intake`
- **Permissions:** CE assessments and referrals
- **Use Cases:**
  - Conduct CE assessments
  - Create CE events (referrals)
  - Manage prioritization status
  - Link to housing resources

---

### 6. **DV Advocate**
- **Username:** `dv.advocate.test`
- **Password:** `DV123!`
- **Role:** `dv-advocate`
- **Permissions:** DV-specific services, safety planning
- **Use Cases:**
  - Create safety plans
  - Document DV services
  - Access VAWA-protected data
  - Provide crisis intervention

---

### 7. **Compliance Auditor**
- **Username:** `compliance.test`
- **Password:** `Comply123!`
- **Role:** `compliance-auditor`
- **Permissions:** Audit trails, compliance reports
- **Use Cases:**
  - Review audit logs
  - Access compliance reports
  - Monitor HUD data quality
  - Review consent ledger

---

### 8. **Executive Director**
- **Username:** `exec.test`
- **Password:** `Exec123!`
- **Role:** `exec`
- **Permissions:** High-level reporting, dashboards
- **Use Cases:**
  - View executive dashboards
  - Access aggregated reports
  - Monitor program outcomes
  - Review system statistics

---

### 9. **Report Viewer**
- **Username:** `report.viewer.test`
- **Password:** `Report123!`
- **Role:** `report-viewer`
- **Permissions:** Read-only report access
- **Use Cases:**
  - View standard reports
  - Access dashboards (read-only)
  - Export report data
  - No client data modification

---

### 10. **External Partner**
- **Username:** `external.partner.test`
- **Password:** `Partner123!`
- **Role:** `external-partner`
- **Permissions:** Limited external access
- **Use Cases:**
  - View shared client information (with consent)
  - Access referral information
  - Limited reporting access
  - No sensitive data access

---

### 11. **Counselor**
- **Username:** `counselor.test`
- **Password:** `Counsel123!`
- **Role:** `counselor`
- **Permissions:** Service delivery, counseling sessions
- **Use Cases:**
  - Document counseling sessions
  - Create service episodes
  - Access client clinical information
  - Create privileged counseling notes

---

### 12. **Advocate**
- **Username:** `advocate.test`
- **Password:** `Advocate123!`
- **Role:** `advocate`
- **Permissions:** Client advocacy, service coordination
- **Use Cases:**
  - Coordinate services
  - Document advocacy efforts
  - Legal accompaniment services
  - Resource navigation

---

### 13. **Data Analyst**
- **Username:** `data.analyst.test`
- **Password:** `Data123!`
- **Role:** `data-analyst`
- **Permissions:** HMIS exports, data analysis, reporting
- **Use Cases:**
  - Create HMIS exports
  - Run data quality reports
  - Access system-wide statistics
  - Generate compliance reports
  - Create custom reports

---

## 🧪 Testing Scenarios

### Role-Based Access Control (RBAC) Testing

#### **Test 1: Client Data Access**
1. Login as `case.manager.test`
2. Navigate to client list
3. ✅ Should see assigned clients
4. ❌ Should NOT see unassigned confidential clients

#### **Test 2: Service Episode Creation**
1. Login as `counselor.test`
2. Navigate to service episodes
3. ✅ Should be able to create counseling sessions
4. ✅ Should mark sessions as confidential

#### **Test 3: Export Functionality**
1. Login as `data.analyst.test`
2. Navigate to exports page
3. ✅ Should see export configuration options
4. ✅ Should be able to create HMIS exports

#### **Test 4: Consent Management**
1. Login as `compliance.test`
2. Navigate to consent ledger
3. ✅ Should see consent audit trails
4. ✅ Should access VAWA-protected consent records

#### **Test 5: Report Access**
1. Login as `report.viewer.test`
2. Navigate to reports
3. ✅ Should see all standard reports
4. ❌ Should NOT be able to modify data

---

## 🔒 Security Notes

### Password Policy
- All test passwords follow format: `[Role]123!`
- Contains uppercase, lowercase, number, and special character
- **NOT FOR PRODUCTION USE**

### Account Management
- All accounts are pre-verified (emailVerified: true)
- Passwords are non-temporary (no forced reset on first login)
- Accounts are enabled by default

### Reset Instructions
To reset all test users, run:
```powershell
# Delete all test users
.\scripts\delete-test-users.ps1

# Recreate them
.\scripts\create-all-role-test-users.ps1
```

---

## 🚀 Quick Start Testing Guide

### 1. **Start the System**
```bash
# Start backend
cd backend
./gradlew bootRun

# Start Keycloak (if not running)
docker-compose up keycloak

# Start frontend
cd frontend/apps/cm-portal
npm run dev
```

### 2. **Create Test Users**
```powershell
cd scripts
.\create-all-role-test-users.ps1
```

### 3. **Test Login**
1. Navigate to: `http://localhost:3000/login`
2. Choose any test user from the table above
3. Enter username and password
4. Verify role-specific dashboard appears

### 4. **Test Permissions**
- Try accessing restricted features
- Verify RBAC enforcement
- Test consent requirements for VAWA data
- Check audit trail creation

---

## 📞 Support

If you encounter issues:
1. Verify Keycloak is running: `http://localhost:8081`
2. Check realm configuration: Realm should be `haven`
3. Verify roles exist in Keycloak admin console
4. Review backend logs for authentication errors

---

## ⚠️ Important Warnings

**DO NOT USE IN PRODUCTION**
- These are test credentials only
- Passwords are intentionally simple
- No MFA or advanced security enabled
- Suitable for development/testing only

**Credential Rotation**
- Change all test passwords before UAT
- Never commit production credentials to Git
- Use proper secret management for production

---

*Last Updated: 2025-10-07*
*Generated by: Haven HMIS Test User Setup Script*
