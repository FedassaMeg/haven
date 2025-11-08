# VAWA-Compliant Intake Workflow

Version 2.0

## Overview

This module provides a comprehensive 10-step intake workflow for domestic violence service providers, compliant with VAWA (Violence Against Women Act) confidentiality requirements and HUD HMIS data standards.

## Directory Structure

```
intake/
├── index.ts                    # Public API barrel export
├── lib/                        # Business logic layer
│   ├── businessLogic.ts       # Domain logic & calculations
│   └── validation.ts          # Validation rules & functions
└── utils/                      # Shared utilities
    ├── types.ts               # TypeScript type definitions
    └── constants.ts           # Static configuration & enums
```

## Error Severity Levels

### ERROR (Blocking)
- **Prevents step progression**
- Must be resolved to continue
- Red alert styling in UI (`#ef4444`)
- Examples:
  - Missing required field
  - Invalid data format
  - Consent not provided
  - Entry date in future
  - Invalid email/phone/SSN format
  - File size exceeds limit
  - Required document not uploaded
  - Staff confirmation missing

### WARNING (Non-blocking)
- **Allows step progression**
- Requires staff acknowledgment
- Yellow/orange alert styling (`#f59e0b`)
- Examples:
  - High risk client (safety routing suggested)
  - HMIS participation declined (data sharing restricted)
  - Missing optional field (data quality impact)
  - Data quality concern (reporting readiness affected)
  - No client strengths identified
  - Emergency shelter needed
  - Children may be unsafe (CPS consultation suggested)
  - Criminal record identified (legal advocacy suggested)
  - Photo release not obtained
  - APR/CAPER not ready (missing fields)

## 10-Step Workflow

### Step 1: Initial Contact / Referral Intake
- Client alias (NOT full name)
- Referral source
- Initial safety check
- Worker assignment

### Step 2: Safety & Consent Check
- Safe contact methods
- Emergency contact information
- **REQUIRED CONSENTS** (BLOCKING):
  - Consent to services
  - Consent to data collection
  - HMIS participation status
- Digital signature required

### Step 3: Crisis / Risk Assessment
- Lethality screening (Danger Assessment, ODARA, etc.)
- Risk level: MINIMAL → LOW → MODERATE → HIGH → SEVERE
- Immediate safety assessment
- Police involvement
- Protective order status
- Dependents information

### Step 4: Eligibility & Program Match
- Homeless status (HUD categories 1-4)
- Income assessment
- Household composition
- Program eligibility determination:
  - Transitional Housing (TH)
  - Rapid Re-Housing (RRH)
  - Permanent Supportive Housing (PSH)
  - Emergency Shelter (ES)

### Step 5: Housing Barrier Assessment
- Rental history (evictions, debt)
- Credit history
- Criminal background
- Employment status
- Support network
- Transportation access
- Auto-generated stability plan

### Step 6: Service Plan & Case Assignment
- Case manager assignment
- Client goals (SMART format)
- Client strengths (empowerment focus)
- Follow-up schedule (30/60/90 days)

### Step 7: Documentation Uploads
- **Required documents**:
  - VAWA consent form
  - HMIS consent form
  - Service agreement
- Optional documents:
  - Photo ID
  - Income verification
  - Protective order

### Step 8: Demographics & Outcome Baseline
- Full legal name (collected AFTER consent)
- Date of birth
- SSN (optional, with data quality indicator)
- Gender (HMIS multi-select)
- Race & ethnicity (HUD codes)
- Veteran status
- Disabling condition
- VAWA confidentiality flags

### Step 9: Service Enrollment Confirmation
- Entry date
- Project type & ID
- Funding source
- Relationship to Head of Household
- Cost allocation
- **Staff accuracy confirmation** (BLOCKING)

### Step 10: Follow-Up / Continuous Reassessment
- Reassessment schedule
- Audit protection settings
- HUD reporting readiness (APR/CAPER)
- Next steps for client

## Usage Examples

### Import Types
```typescript
import type {
  MasterIntakeData,
  InitialContactData,
  SafetyAndConsentData,
  ValidationError,
  StepValidationResult,
} from '@/components/intake';
```

### Import Constants
```typescript
import {
  WORKFLOW_STEPS,
  HOMELESS_CATEGORIES,
  RISK_LEVELS,
  ERROR_SEVERITY,
  SEVERITY_CONFIG,
  ERROR_MESSAGES,
  WARNING_MESSAGES,
} from '@/components/intake';
```

### Validate a Step
```typescript
import { validateStep2 } from '@/components/intake';

const result = validateStep2(safetyAndConsentData);

if (!result.isValid) {
  // Handle blocking errors
  result.errors.forEach(error => {
    console.error(`${error.field}: ${error.message}`);
  });
}

// Handle non-blocking warnings
result.warnings.forEach(warning => {
  console.warn(`${warning.field}: ${warning.message}`);
});
```

### Use Business Logic
```typescript
import {
  calculateOverallRiskLevel,
  shouldAutoRouteToSafety,
  determineEligibility,
  calculateBarrierSeverity,
} from '@/components/intake';

// Risk assessment
const overallRisk = calculateOverallRiskLevel(
  lethalityRiskLevel,
  currentlySafe,
  safePlaceToStay,
  needsEmergencyShelter
);

if (shouldAutoRouteToSafety(overallRisk, currentlySafe, needsEmergencyShelter, childrenSafe)) {
  // Route to crisis intervention
}

// Eligibility determination
const eligibility = determineEligibility(eligibilityData);
console.log(`Recommended program: ${eligibility.recommendedProgramId}`);

// Housing barriers
const barrierSeverity = calculateBarrierSeverity(housingBarrierData);
// Returns: 'LOW' | 'MODERATE' | 'HIGH' | 'SEVERE'
```

### Check Reporting Readiness
```typescript
import {
  checkAprReadiness,
  checkCaperReadiness,
  calculateDataQualityScore,
} from '@/components/intake';

const aprCheck = checkAprReadiness(masterIntakeData);
if (!aprCheck.ready) {
  console.log('APR missing fields:', aprCheck.missingFields);
}

const dataQuality = calculateDataQualityScore(masterIntakeData);
console.log(`Data quality: ${dataQuality}%`);
```

### Helper Functions
```typescript
import {
  createStepValidator,
  getFieldError,
  canSubmitIntake,
  calculateIntakeProgress,
  getRecommendedNextAction,
} from '@/components/intake';

// Check if intake can be submitted
const submitCheck = canSubmitIntake(masterData);
if (!submitCheck.canSubmit) {
  console.log('Cannot submit:', submitCheck.reason);
  console.log('Missing steps:', submitCheck.missingSteps);
}

// Calculate progress
const progress = calculateIntakeProgress(masterData);
console.log(`Progress: ${progress.percentage}%`);
console.log(`Completed: ${progress.completedSteps}/${progress.totalSteps}`);
console.log(`Next step: ${progress.nextStep}`);

// Get recommended action
const action = getRecommendedNextAction(masterData);
console.log(`Action: ${action.action} (${action.priority})`);
console.log(`Description: ${action.description}`);
```

## VAWA Compliance

This module implements VAWA (Violence Against Women Act) confidentiality requirements:

1. **Progressive Disclosure**: Client's full name and identifying information collected AFTER consent
2. **Safe Contact Methods**: Configurable quiet hours and code words
3. **HMIS Opt-Out**: Clients can decline HMIS participation
4. **VAWA Exemption**: Separate confidentiality flag for fleeing DV situations
5. **Data Access Controls**: Field-level security based on VAWA status
6. **Audit Trail**: Complete history with version control
7. **Document Retention**: Auto-deletion rules for temporary documents

## HUD/HMIS Compliance

Implements HUD HMIS Data Standards for:

- Homeless definition categories (24 CFR §578.3)
- Prior living situations
- Project types (ES, TH, RRH, PSH, etc.)
- Race/ethnicity codes
- Gender (multi-select)
- Data quality indicators
- Annual Performance Report (APR) readiness
- CAPER reporting

## API Integration

### Backend API Endpoints

The intake workflow integrates with the following backend REST APIs:

#### 1. Pre-Intake Contact (Steps 1-7: Temp Client)
```
POST   /api/v1/intake/pre-contact          # Create temp client
GET    /api/v1/intake/pre-contact/:id      # Retrieve temp client
PUT    /api/v1/intake/pre-contact/:id      # Update temp client
```

**Purpose**: Manage temporary client records before full demographic data is collected.

**Lifecycle**:
- Created on Step 1 completion
- Updated as user progresses through Steps 2-7
- Auto-expires after 30 days
- Deleted after promotion to full client (Step 8)

#### 2. Program Matching (Step 4: Eligibility)
```
POST   /api/v1/intake/programs/match       # Match client to programs
GET    /api/v1/intake/programs/available   # Get available programs
```

**Purpose**: Determine program eligibility based on HUD criteria.

**Matching Logic**:
- HUD homeless category eligibility (24 CFR §578.3)
- Income limits (Area Median Income %)
- Household composition requirements
- Veteran status (for VASH programs)
- Disabling condition (for PSH programs)
- DV survivor status (for VAWA programs)
- Program capacity and availability

#### 3. Client Promotion (Step 8: Demographics)
```
POST   /api/v1/intake/clients/promote      # Convert temp → full client
```

**Purpose**: Create full client record with complete PII after consent obtained.

**Process**:
1. Validate temp client exists
2. Check for duplicate clients (SSN, name+DOB)
3. Create full client record
4. Generate HMIS client ID (pseudonymized if VAWA protected)
5. Merge temp client data
6. Delete temp client record

#### 4. Enrollment Creation (Step 9: Enrollment)
```
POST   /api/v1/intake/enrollments          # Create enrollment
```

**Purpose**: Create official enrollment record and trigger HMIS integration.

**Side Effects**:
- Update program capacity (decrement available)
- Create HMIS enrollment record (async)
- Send notifications to case manager and client (async)
- Create initial case note
- Set up default tasks

#### 5. Recurring Tasks (Step 10: Follow-up)
```
POST   /api/v1/intake/tasks/recurring      # Create follow-up tasks
GET    /api/v1/intake/tasks                # Get tasks
```

**Purpose**: Schedule recurring reassessment tasks.

**Recurrence Patterns**:
- Monthly check-ins: `FREQ=MONTHLY;INTERVAL=1`
- 90-day reviews: `FREQ=DAILY;INTERVAL=90`
- Annual assessments: `FREQ=YEARLY;INTERVAL=1`

#### 6. Document Upload (Step 7: Documentation)
```
POST   /api/v1/intake/documents            # Upload document
GET    /api/v1/intake/documents/:id        # Get document metadata + signed URL
```

**Purpose**: Secure document storage with encryption and access control.

**File Processing**:
1. Validate file type (PDF, JPG, PNG, DOCX) and size (max 10MB)
2. Virus scanning (ClamAV)
3. Encryption (AES-256)
4. Upload to secure storage (S3/Azure Blob)
5. Create metadata record with access logs
6. Set expiration for temporary documents

### API Client Integration

#### Shared Types
All API request/response types are defined in:
```
frontend/packages/api-client/src/intakeTypes.ts
```

Import types in your code:
```typescript
import type {
  CreatePreIntakeContactRequest,
  CreatePreIntakeContactResponse,
  MatchProgramsRequest,
  MatchProgramsResponse,
  CreateEnrollmentRequest,
  CreateEnrollmentResponse,
  PromoteClientRequest,
  PromoteClientResponse,
  CreateRecurringTaskRequest,
  CreateRecurringTaskResponse,
  UploadDocumentRequest,
  UploadDocumentResponse,
} from '@/packages/api-client/src/intakeTypes';
```

#### API Client Methods

Add to `frontend/packages/api-client/src/hooks.ts`:

```typescript
// Pre-Intake Contact
export const useCreatePreIntakeContact = () => {
  return useMutation({
    mutationFn: (data: CreatePreIntakeContactRequest) =>
      apiClient.post('/intake/pre-contact', data),
  });
};

export const useGetPreIntakeContact = (tempClientId: string) => {
  return useQuery({
    queryKey: ['pre-intake-contact', tempClientId],
    queryFn: () => apiClient.get(`/intake/pre-contact/${tempClientId}`),
  });
};

// Program Matching
export const useMatchPrograms = () => {
  return useMutation({
    mutationFn: (data: MatchProgramsRequest) =>
      apiClient.post('/intake/programs/match', data),
  });
};

// Client Promotion
export const usePromoteClient = () => {
  return useMutation({
    mutationFn: (data: PromoteClientRequest) =>
      apiClient.post('/intake/clients/promote', data),
  });
};

// Enrollment Creation
export const useCreateEnrollment = () => {
  return useMutation({
    mutationFn: (data: CreateEnrollmentRequest) =>
      apiClient.post('/intake/enrollments', data),
  });
};

// Recurring Tasks
export const useCreateRecurringTask = () => {
  return useMutation({
    mutationFn: (data: CreateRecurringTaskRequest) =>
      apiClient.post('/intake/tasks/recurring', data),
  });
};

// Document Upload
export const useUploadDocument = () => {
  return useMutation({
    mutationFn: (data: UploadDocumentRequest) =>
      apiClient.post('/intake/documents', data),
  });
};
```

### Integration Checklist

#### Phase 1: Pre-Intake Contact
- [ ] Backend: Create `pre_intake_contacts` table
- [ ] Backend: Implement POST /intake/pre-contact
- [ ] Backend: Implement GET /intake/pre-contact/:id
- [ ] Backend: Implement PUT /intake/pre-contact/:id
- [ ] Backend: Add TTL cleanup job (30 days)
- [ ] Frontend: Add API client methods
- [ ] Frontend: Update Step 1 to call API
- [ ] Frontend: Update orchestrator to store tempClientId
- [ ] Test: End-to-end temp client flow

#### Phase 2: Program Matching
- [ ] Backend: Implement eligibility logic engine
- [ ] Backend: Create POST /intake/programs/match
- [ ] Backend: Create GET /intake/programs/available
- [ ] Backend: Add program capacity tracking
- [ ] Frontend: Add API client methods
- [ ] Frontend: Update Step 4 to call matching API
- [ ] Frontend: Display eligibility results
- [ ] Test: All program types, edge cases, capacity limits

#### Phase 3: Client Promotion
- [ ] Backend: Implement duplicate client detection
- [ ] Backend: Create POST /intake/clients/promote
- [ ] Backend: Add HMIS ID generation (with VAWA pseudonymization)
- [ ] Frontend: Add API client method
- [ ] Frontend: Update Step 8 to call promotion API
- [ ] Frontend: Update orchestrator to handle promoted client ID
- [ ] Test: VAWA protected vs non-protected clients

#### Phase 4: Enrollment Creation
- [ ] Backend: Extend enrollment entity for intake data
- [ ] Backend: Implement POST /intake/enrollments
- [ ] Backend: Add capacity checking and decrement logic
- [ ] Backend: HMIS integration (async)
- [ ] Backend: Notification system (email/SMS)
- [ ] Frontend: Add API client method
- [ ] Frontend: Update Step 9 to call enrollment API
- [ ] Test: Happy path, capacity limits, HMIS sync

#### Phase 5: Recurring Tasks
- [ ] Backend: Task scheduler service
- [ ] Backend: Implement POST /intake/tasks/recurring
- [ ] Backend: Implement GET /intake/tasks
- [ ] Backend: Reminder notification system
- [ ] Backend: RRULE parser for recurrence
- [ ] Frontend: Add API client methods
- [ ] Frontend: Update Step 10 to create tasks
- [ ] Test: Task creation, recurrence patterns, reminders

#### Phase 6: Document Upload
- [ ] Backend: Secure file storage setup (S3/Azure)
- [ ] Backend: File encryption service (AES-256)
- [ ] Backend: Virus scanning integration (ClamAV)
- [ ] Backend: Implement POST /intake/documents
- [ ] Backend: Implement GET /intake/documents/:id (signed URLs)
- [ ] Backend: Document expiration cleanup job
- [ ] Frontend: Add API client methods
- [ ] Frontend: Update Step 7 to upload files
- [ ] Test: File upload, encryption, access control, expiration

### Feature Flags

Enable/disable API integration with feature flags:

```typescript
// frontend/apps/cm-portal/src/config/features.ts
export const FEATURE_FLAGS = {
  INTAKE_USE_REAL_API: process.env.NEXT_PUBLIC_INTAKE_USE_REAL_API === 'true',
  INTAKE_MOCK_MODE: process.env.NEXT_PUBLIC_INTAKE_MOCK_MODE === 'true',
};

// In orchestrator (index-v2.tsx)
import { FEATURE_FLAGS } from '@/config/features';

const createPreIntakeContact = async (data: InitialContactData) => {
  if (FEATURE_FLAGS.INTAKE_USE_REAL_API) {
    // Call real API
    const response = await apiClient.post('/intake/pre-contact', data);
    return response.data.tempClientId;
  } else {
    // Use mock implementation
    return `TEMP-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
};
```

### Environment Variables

Add to `.env.local`:
```bash
# API Integration
NEXT_PUBLIC_INTAKE_USE_REAL_API=false
NEXT_PUBLIC_INTAKE_MOCK_MODE=true

# API Base URL
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
```

## License

Proprietary - Haven Case Management System
