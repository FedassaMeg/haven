# Client Intake Workflow Implementation Summary

**Date:** November 7, 2025
**Implementation Status:** Phase 1 Complete (Pre-Contact & Client Promotion)

---

## Overview

This document details the implementation of the VAWA-compliant progressive intake workflow for the Haven HMIS system. The implementation follows **Option A**: Full progressive intake workflow as originally specified in [IntakeController.java](backend/apps/api-app/src/main/java/org/haven/api/intake/IntakeController.java:18-33).

## Architecture

### Workflow Design

The intake workflow implements a 10-step progressive disclosure process:

```
Steps 1-7: Pre-Intake Contact Phase (Temporary Client Record)
    ├─ Step 1: Initial Contact
    ├─ Step 2: Needs Assessment
    ├─ Step 3: Safety Assessment
    ├─ Step 4: Program Matching
    ├─ Step 5: Housing History
    ├─ Step 6: Safety Plan
    └─ Step 7: Documentation Upload

Step 8: Client Promotion (Temp → Full Client w/ Demographics)

Step 9: Enrollment Creation

Step 10: Follow-up Task Setup
```

### Module Structure

```
backend/modules/intake/
├── domain/
│   ├── PreIntakeContact.java          (Aggregate Root)
│   ├── PreIntakeContactId.java        (Value Object)
│   ├── ReferralSource.java            (Enum)
│   ├── PreIntakeContactRepository.java (Repository Interface)
│   └── events/
│       ├── PreIntakeContactCreated.java
│       ├── PreIntakeWorkflowUpdated.java
│       ├── PreIntakeContactInfoUpdated.java
│       ├── PreIntakeContactPromoted.java
│       └── PreIntakeContactExpired.java
├── application/
│   ├── commands/
│   │   ├── CreatePreIntakeContactCmd.java
│   │   ├── UpdatePreIntakeContactCmd.java
│   │   ├── UpdateWorkflowDataCmd.java
│   │   └── PromoteClientCmd.java
│   ├── dto/
│   │   └── PreIntakeContactDto.java
│   └── services/
│       └── IntakeAppService.java
├── infrastructure/
│   └── persistence/
│       ├── JpaPreIntakeContactEntity.java
│       ├── JpaPreIntakeContactRepository.java
│       └── PreIntakeContactRepositoryAdapter.java
├── IntakeModuleConfiguration.java
└── build.gradle.kts
```

---

## ✅ Implemented Features

### 1. Pre-Intake Contact Domain Model

**Location:** `backend/modules/intake/src/main/java/org/haven/intake/domain/`

**Features:**
- Event-sourced aggregate with domain events
- 30-day TTL with automatic expiration
- JSONB workflow data storage for flexible step data
- Client alias support for safety/anonymity
- Promotion tracking to prevent duplicate promotions
- Referral source categorization (13 standard sources)

**Key Methods:**
- `create()` - Factory method with validation
- `updateWorkflowData()` - Update step progress (Steps 1-7)
- `updateContactInfo()` - Update basic contact info
- `markPromoted()` - Mark as promoted to full client
- `markExpired()` - TTL cleanup support
- `isExpired()` - Check expiration status

### 2. Persistence Layer

**Database Schema:** [V41__pre_intake_contacts.sql](backend/modules/intake/src/main/resources/db/migration/V41__pre_intake_contacts.sql)

**Table:** `haven.pre_intake_contacts`

**Columns:**
- `id` (UUID, Primary Key)
- `client_alias` (VARCHAR(200), NOT NULL)
- `contact_date` (DATE, NOT NULL)
- `referral_source` (ENUM, NOT NULL)
- `intake_worker_name` (VARCHAR(200), NOT NULL)
- `workflow_data` (JSONB, DEFAULT '{}')
- `current_step` (INTEGER, DEFAULT 1)
- `created_at`, `updated_at`, `expires_at` (TIMESTAMP)
- `expired`, `promoted` (BOOLEAN)
- `promoted_client_id` (UUID, FK to clients)
- `version` (BIGINT, optimistic locking)

**Indexes:**
- `idx_pre_intake_contacts_expires_at` - TTL cleanup
- `idx_pre_intake_contacts_worker` - Worker queries
- `idx_pre_intake_contacts_alias` - Full-text search (GIN)
- `idx_pre_intake_contacts_promoted_client` - Promoted client lookups
- `idx_pre_intake_contacts_active` - Active contacts (partial index)

**Constraints:**
- Contact date cannot be in future
- Expires_at must be after created_at
- Current step range: 1-10
- Promoted client ID required when promoted=true

### 3. Application Service

**Location:** [IntakeAppService.java](backend/modules/intake/src/main/java/org/haven/intake/application/services/IntakeAppService.java)

**Command Handlers:**
- `handle(CreatePreIntakeContactCmd)` - Create temp contact
  - Validates contact date (max 7 days past)
  - Sets 30-day expiration
  - Returns PreIntakeContactId

- `handle(UpdatePreIntakeContactCmd)` - Update basic info
  - Updates alias, contact date, referral source
  - Validates not expired/promoted

- `handle(UpdateWorkflowDataCmd)` - Update step data
  - Stores step data in JSONB `workflow_data` field
  - Tracks current step progress
  - Validates step range (1-10)

- `handle(PromoteClientCmd)` - Promote to full client
  - Checks for duplicates (name similarity)
  - Creates full Client via ClientAppService
  - Applies VAWA pseudonymization (TODO flag)
  - Marks temp contact as promoted
  - Returns ClientId

**Query Handlers:**
- `getPreIntakeContact(id)` - Retrieve by ID

### 4. REST API Implementation

**Controller:** [IntakeControllerImpl.java](backend/apps/api-app/src/main/java/org/haven/api/intake/IntakeControllerImpl.java)

**Implemented Endpoints:**

#### ✅ Endpoint 1: Create Pre-Intake Contact
```http
POST /api/v1/intake/pre-contact
Content-Type: application/json

{
  "clientAlias": "Jane (DV Survivor)",
  "contactDate": "2025-11-07",
  "referralSource": "DOMESTIC_VIOLENCE_HOTLINE",
  "intakeWorkerName": "Sarah Johnson"
}

Response (201 Created):
{
  "tempClientId": "550e8400-e29b-41d4-a716-446655440000",
  "clientAlias": "Jane (DV Survivor)",
  "createdAt": "2025-11-07T10:30:00Z",
  "expiresAt": "2025-12-07T10:30:00Z"
}
```

#### ✅ Endpoint 2: Get Pre-Intake Contact
```http
GET /api/v1/intake/pre-contact/{tempClientId}

Response (200 OK):
{
  "tempClientId": "550e8400-e29b-41d4-a716-446655440000",
  "clientAlias": "Jane (DV Survivor)",
  "contactDate": "2025-11-07",
  "referralSource": "DOMESTIC_VIOLENCE_HOTLINE",
  "intakeWorkerName": "Sarah Johnson",
  "workflowData": {
    "step_1": {...},
    "step_2": {...}
  },
  "currentStep": 2,
  "created_at": "2025-11-07T10:30:00Z",
  "updatedAt": "2025-11-07T11:15:00Z",
  "expiresAt": "2025-12-07T10:30:00Z",
  "expired": false,
  "promoted": false,
  "promotedClientId": null
}

Response (410 Gone) - if expired
```

#### ✅ Endpoint 3: Update Pre-Intake Contact
```http
PUT /api/v1/intake/pre-contact/{tempClientId}
Content-Type: application/json

{
  "clientAlias": "Jane S.",
  "step": 3,
  "stepData": {
    "needsAssessment": {
      "housingStatus": "HOMELESS",
      "immediateSafetyRisk": true
    }
  }
}

Response (200 OK): <PreIntakeContactDto>
```

#### ✅ Endpoint 6: Promote Client (Temp → Full)
```http
POST /api/v1/intake/clients/promote
Content-Type: application/json

{
  "tempClientId": "550e8400-e29b-41d4-a716-446655440000",
  "givenName": "Jane",
  "familyName": "Smith",
  "gender": "FEMALE",
  "birthDate": "1985-03-15",
  "addresses": [
    {
      "line1": "123 Safe House St",
      "city": "Portland",
      "state": "OR",
      "postalCode": "97201"
    }
  ],
  "telecoms": [
    {
      "system": "phone",
      "value": "(503) 555-0100",
      "use": "mobile"
    }
  ],
  "vawaProtected": true,
  "socialSecurityNumber": null
}

Response (201 Created):
{
  "clientId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "tempClientId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Client promoted successfully. Pre-intake contact marked as promoted."
}
```

### 5. DTOs

**Location:** `backend/apps/api-app/src/main/java/org/haven/api/intake/dto/`

- `CreatePreIntakeContactRequest.java` - Endpoint 1 request
- `CreatePreIntakeContactResponse.java` - Endpoint 1 response
- `UpdatePreIntakeContactRequest.java` - Endpoint 3 request
- `PromoteClientRequest.java` - Endpoint 6 request
- `PromoteClientResponse.java` - Endpoint 6 response

### 6. Build Configuration

**Module:** Added to `settings.gradle.kts`
```kotlin
include("modules:intake")
```

**Dependencies:** Added to `apps/api-app/build.gradle.kts`
```kotlin
implementation(project(":modules:intake"))
```

**Module Build:** [build.gradle.kts](backend/modules/intake/build.gradle.kts)
- Depends on: `:shared-kernel`, `:event-store`, `:client-profile`
- Spring Boot starters: data-jpa, validation, web
- PostgreSQL JDBC driver

---

## ❌ Not Yet Implemented

### Endpoint 4: Program Matching Engine
```http
POST /api/v1/intake/programs/match
```

**Requirements:**
- HUD eligibility criteria engine
- Program capacity checking
- Scoring/ranking algorithm
- Recommendation logic

**Implementation Needed:**
- Program eligibility service
- HUD 24 CFR §578.3 compliance rules
- Income/household validation
- Veteran/DV status checks
- Disabling condition verification

### Endpoint 5: Available Programs Query
```http
GET /api/v1/intake/programs/available
```

**Requirements:**
- Query programs by type, funding source
- Capacity filtering
- Waitlist support

**Implementation Needed:**
- Program query service
- Capacity view/projection

### Endpoint 7: Create Enrollment
```http
POST /api/v1/intake/enrollments
```

**Requirements:**
- Create enrollment record
- Update program capacity
- HMIS integration
- Notifications (email, SMS)
- Default task creation

**Implementation Needed:**
- EnrollmentAppService (may already exist)
- Capacity management
- Notification service
- HMIS export integration

### Endpoint 8-9: Recurring Task Scheduling
```http
POST /api/v1/intake/tasks/recurring
GET /api/v1/intake/tasks
```

**Requirements:**
- RRULE parsing (RFC 5545)
- Task series generation
- Reminder scheduling
- Multi-channel notifications

**Implementation Needed:**
- Task domain model
- RRULE library integration
- Task scheduler service
- Reminder infrastructure

### Endpoints 10-11: Document Management
```http
POST /api/v1/intake/documents
GET /api/v1/intake/documents/{id}
```

**Requirements:**
- Multipart file upload
- Virus scanning (ClamAV)
- AES-256 encryption
- Secure storage (S3, Azure Blob)
- Signed URL generation (15-min TTL)
- Document metadata tracking
- VAWA-protected document restrictions

**Implementation Needed:**
- Document domain model
- File upload service
- Encryption service
- Cloud storage integration
- Virus scanning integration
- Access control/audit logging

---

## VAWA Compliance

### Implemented
- ✅ Minimal PII collection before consent (pre-contact uses alias)
- ✅ 30-day TTL for temporary data
- ✅ Progressive disclosure (temp → full client)
- ✅ VAWA protection flag in client promotion
- ✅ Duplicate detection before promotion

### Not Yet Implemented
- ❌ HMIS Personal ID pseudonymization for VAWA clients
- ❌ Data sharing restrictions in HMIS exports
- ❌ Document access restrictions for VAWA clients
- ❌ Address confidentiality flagging during promotion
- ❌ Safe-at-Home integration

**Location for VAWA Logic:**
[IntakeAppService.java:90-99](backend/modules/intake/src/main/java/org/haven/intake/application/services/IntakeAppService.java:90-99)

---

## Testing Requirements

### Unit Tests Needed
- [ ] PreIntakeContact domain logic
- [ ] IntakeAppService command handlers
- [ ] JPA entity mapping (domain ↔ entity)
- [ ] TTL expiration logic
- [ ] Duplicate detection logic

### Integration Tests Needed
- [ ] Full intake workflow (Steps 1-8)
- [ ] Client promotion with VAWA flag
- [ ] Duplicate client scenarios
- [ ] TTL cleanup job
- [ ] Database constraint validation
- [ ] Transaction rollback scenarios

### Test Data Setup
- [ ] Seed referral sources
- [ ] Create test pre-intake contacts
- [ ] Create expired contacts for cleanup testing
- [ ] Create duplicate client scenarios

---

## Deployment Checklist

### Database
- [ ] Run migration V41 (pre_intake_contacts table)
- [ ] Verify indexes created
- [ ] Verify constraints active
- [ ] Verify trigger (updated_at) working

### Application
- [ ] Build intake module
- [ ] Verify module dependency resolution
- [ ] Configure IntakeModuleConfiguration in Spring context
- [ ] Verify JPA entity scanning
- [ ] Verify repository bean creation

### TTL Cleanup Job
- [ ] Implement scheduled job (daily cron)
- [ ] Query expired contacts
- [ ] Mark as expired or delete
- [ ] Log cleanup metrics

### Monitoring
- [ ] Track pre-intake contact creation rate
- [ ] Track promotion rate
- [ ] Track expiration rate
- [ ] Alert on high expiration rate (may indicate UX issues)

---

## Frontend Integration Guide

### State Management

**Recommended Structure:**
```typescript
interface IntakeState {
  tempClientId: string | null;
  currentStep: number;
  stepData: {
    [stepNumber: number]: any;
  };
  expiresAt: Date | null;
}
```

### API Integration

**Step 1: Create Pre-Contact**
```typescript
const createPreContact = async (data: {
  clientAlias: string;
  contactDate: string;
  referralSource: string;
  intakeWorkerName: string;
}) => {
  const response = await fetch('/api/v1/intake/pre-contact', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });

  const result = await response.json();
  // Store tempClientId in state
  return result.tempClientId;
};
```

**Steps 2-7: Update Workflow Data**
```typescript
const updateStepData = async (tempClientId: string, step: number, stepData: any) => {
  await fetch(`/api/v1/intake/pre-contact/${tempClientId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ step, stepData })
  });
};
```

**Step 8: Promote Client**
```typescript
const promoteToClient = async (tempClientId: string, demographics: any) => {
  const response = await fetch('/api/v1/intake/clients/promote', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      tempClientId,
      ...demographics
    })
  });

  const result = await response.json();
  return result.clientId; // Full client ID
};
```

---

## Performance Considerations

### Database
- **JSONB Indexing:** Consider GIN index on `workflow_data` if querying step data
- **Partitioning:** Consider table partitioning by `created_at` for large volumes
- **Archival:** Move expired records to archive table instead of hard delete

### Caching
- Cache program matching results (Step 4)
- Cache available programs query (Step 4)
- TTL: 5-15 minutes

### Batch Operations
- TTL cleanup: Run in batches of 1000
- Avoid long-running transactions

---

## Security Considerations

### Authorization
- Intake workers can only see their own pre-contacts
- Supervisors can see all pre-contacts for their team
- VAWA-protected client promotions require additional permission

### Audit Logging
- Log all pre-contact creation (who, when, what)
- Log all client promotions
- Log duplicate detection warnings
- Log TTL cleanup operations

### Data Encryption
- Encrypt `workflow_data` JSONB at rest (PostgreSQL TDE)
- Use HTTPS for all API calls
- Consider field-level encryption for sensitive step data

---

## Next Steps

### Phase 2: Program Matching & Enrollment
1. Implement program eligibility engine
2. Create program capacity management
3. Implement enrollment creation
4. Wire up IntakeController endpoints 4, 5, 7

### Phase 3: Task Scheduling
1. Implement task domain model
2. Add RRULE library (biweekly, icalendar4j)
3. Create task scheduler service
4. Wire up IntakeController endpoints 8, 9

### Phase 4: Document Management
1. Implement document domain model
2. Integrate virus scanning
3. Set up cloud storage
4. Implement encryption service
5. Wire up IntakeController endpoints 10, 11

### Phase 5: VAWA Enhancements
1. Implement HMIS Personal ID pseudonymization
2. Add data sharing restrictions
3. Integrate Safe-at-Home program
4. Enhance document access controls

---

## File Inventory

### Domain Layer
- ✅ `PreIntakeContact.java` - Aggregate Root
- ✅ `PreIntakeContactId.java` - Value Object
- ✅ `ReferralSource.java` - Enum
- ✅ `PreIntakeContactRepository.java` - Repository Interface
- ✅ `events/PreIntakeContactCreated.java`
- ✅ `events/PreIntakeWorkflowUpdated.java`
- ✅ `events/PreIntakeContactInfoUpdated.java`
- ✅ `events/PreIntakeContactPromoted.java`
- ✅ `events/PreIntakeContactExpired.java`

### Application Layer
- ✅ `commands/CreatePreIntakeContactCmd.java`
- ✅ `commands/UpdatePreIntakeContactCmd.java`
- ✅ `commands/UpdateWorkflowDataCmd.java`
- ✅ `commands/PromoteClientCmd.java`
- ✅ `dto/PreIntakeContactDto.java`
- ✅ `services/IntakeAppService.java`

### Infrastructure Layer
- ✅ `persistence/JpaPreIntakeContactEntity.java`
- ✅ `persistence/JpaPreIntakeContactRepository.java`
- ✅ `persistence/PreIntakeContactRepositoryAdapter.java`

### API Layer
- ✅ `IntakeControllerImpl.java` (4 endpoints implemented, 7 stubbed)
- ✅ `dto/CreatePreIntakeContactRequest.java`
- ✅ `dto/CreatePreIntakeContactResponse.java`
- ✅ `dto/UpdatePreIntakeContactRequest.java`
- ✅ `dto/PromoteClientRequest.java`
- ✅ `dto/PromoteClientResponse.java`

### Configuration
- ✅ `IntakeModuleConfiguration.java`
- ✅ `build.gradle.kts`
- ✅ `settings.gradle.kts` (module included)
- ✅ `apps/api-app/build.gradle.kts` (dependency added)

### Database
- ✅ `V41__pre_intake_contacts.sql`

---

## Summary

**Phase 1 Status:** ✅ **COMPLETE**

**Implemented:**
- Pre-intake contact domain model with event sourcing
- Full persistence layer with JPA/PostgreSQL
- Application service with 4 command handlers
- REST API with 4 working endpoints
- Database migration with comprehensive schema
- Module build configuration
- Complete DDD architecture (domain → application → infrastructure → API)

**Percentage Complete:** ~40% of full intake workflow specification

**Production Ready:** Endpoints 1-3, 6 are ready for testing and deployment after:
1. Running database migration
2. Adding unit/integration tests
3. Implementing TTL cleanup job
4. Adding audit logging
5. Frontend integration

**Next Priority:** Implement program matching engine (Endpoint 4) or document management (Endpoints 10-11) based on business priority.

---

**For questions or clarifications, reference:**
- Original specification: [IntakeController.java](backend/apps/api-app/src/main/java/org/haven/api/intake/IntakeController.java)
- Implementation: [IntakeControllerImpl.java](backend/apps/api-app/src/main/java/org/haven/api/intake/IntakeControllerImpl.java)
- Domain model: [PreIntakeContact.java](backend/modules/intake/src/main/java/org/haven/intake/domain/PreIntakeContact.java)
- Application service: [IntakeAppService.java](backend/modules/intake/src/main/java/org/haven/intake/application/services/IntakeAppService.java)
