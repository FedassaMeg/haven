# CSV Writer Validation Guardrails

## Overview

Comprehensive validation guardrails for HUD HMIS CSV exports implementing centralized validation utilities, structured diagnostic logging, and PII-safe error reporting.

**Effective Date**: 2025-10-07
**HUD Compliance**: 2024 HMIS Data Standards v1.0

---

## Architecture

### Components

1. **CsvValidationUtilities** - Centralized validation functions
2. **CsvValidationLogger** - Structured diagnostic log channel
3. **ValidationDiagnostic** - PII-safe error formatting
4. **HudPicklistCodes** - 2024 picklist code definitions
5. **CSVExportStrategy** - Instrumented CSV writer with validation hooks

### Validation Flow

```
CSV Data Rows
    ↓
validateRow() - Invoke validation prior to row emission
    ↓
CsvValidationUtilities
    ├─ validateDateInRange()
    ├─ validateNullableField()
    ├─ validatePicklistCode()
    └─ validateDateSequence()
    ↓
ValidationDiagnostic (PII-safe)
    ↓
CsvValidationLogger - Propagate to structured log channel
    ↓
Decision: ERROR → Reject Row | WARNING → Emit Row | SUCCESS → Emit Row
    ↓
Export Completion → logSummary()
```

---

## Validation Semantics

### 1. Date Range Enforcement

**Validator**: `CsvValidationUtilities.validateDateInRange()`

**Rules**:
- **HMIS Epoch Boundary**: Dates must be ≥ 1998-10-01 (HUD HMIS inception)
- **Future Tolerance**: Dates must be ≤ (today + 30 days)
- **Export Period Boundaries**:
  - Dates before export start → **WARNING** (data quality concern)
  - Dates after export end → **WARNING** (data quality concern)
  - Dates outside HMIS epoch or future tolerance → **ERROR** (reject row)

**Error Codes**:
- `DATE_NULL` - Required date field is null
- `DATE_PARSE_FAILURE` - Invalid date format
- `DATE_BEFORE_HMIS_EPOCH` - Date precedes 1998-10-01
- `DATE_TOO_FAR_FUTURE` - Date exceeds 30-day future tolerance
- `DATE_BEFORE_EXPORT_PERIOD` - Date before export start (warning)
- `DATE_AFTER_EXPORT_PERIOD` - Date after export end (warning)

**Example**:
```java
ValidationDiagnostic result = CsvValidationUtilities.validateDateInRange(
    "EntryDate",
    entryDate,
    exportStartDate,
    exportEndDate,
    "Enrollment row 42"
);
```

---

### 2. Nullable Field Handling

**Validator**: `CsvValidationUtilities.validateNullableField()`

**HUD Requirement Flags**:
- **R** (Required) - Null/empty → **ERROR**
- **C** (Conditional) - Null/empty → **WARNING** (verify business rules)
- **O** (Optional) - Null/empty → **SUCCESS**

**Rules**:
- Empty strings (`""`, `"   "`) treated as null
- Conditional fields require additional business logic verification

**Error Codes**:
- `REQUIRED_FIELD_NULL` - Required field is null/empty
- `CONDITIONAL_FIELD_NULL` - Conditional field is null/empty (warning)

**Example**:
```java
ValidationDiagnostic result = CsvValidationUtilities.validateNullableField(
    "RelationshipToHoH",
    value,
    "R",  // Required flag from HUD metadata
    "Enrollment row 42"
);
```

---

### 3. HUD Picklist Code Validation

**Validator**: `CsvValidationUtilities.validatePicklistCode()`

**Picklists** (HUD 2024 Data Standards):
- 1.4 Name Data Quality (1, 2, 8, 9, 99)
- 1.5 SSN Data Quality (1, 2, 8, 9, 99)
- 1.6 DOB Data Quality (1, 2, 8, 9, 99)
- 1.7 Disabling Condition (0, 1, 8, 9, 99)
- 1.27 Relationship to HoH (1-5)
- 3.12 Destination (1-436, see `HudPicklistCodes`)
- 3.917 Living Situation (1-37, 99)
- 4.05 Data Collection Stage (1, 2, 3, 5)
- 4.10 Domestic Violence (0, 1, 8, 9, 99)
- 4.12 Disability Type (5-10)

**Rules**:
- Codes must match HUD 2024 specifications exactly
- Invalid codes → **ERROR** (reject row)
- Null codes → **SUCCESS** (handled separately via `validateNullableField()`)

**Error Codes**:
- `PICKLIST_PARSE_FAILURE` - Value is not an integer
- `PICKLIST_INVALID_CODE` - Code not in HUD picklist

**Example**:
```java
ValidationDiagnostic result = CsvValidationUtilities.validatePicklistCode(
    "RelationshipToHoH",
    relationshipCode,
    HudPicklistCodes.RELATIONSHIP_TO_HOH,
    "1.27 Relationship to HoH",
    "Enrollment row 42"
);
```

---

### 4. PII-Safe Error Formatting

**Component**: `ValidationDiagnostic`

**Sanitization Rules**:
- **SSN Patterns**: 9-digit sequences → `[SSN-REDACTED]`
- **DOB Patterns**: Dates before 2005 → `[DOB-REDACTED]`
- **Long Values**: Truncate to 50 characters
- **Row Context**: Use anonymized identifiers (`Enrollment row 42`, not `EnrollmentID=ENR-12345`)

**Diagnostic Output Formats**:

**Log Format**:
```
[ERROR] Enrollment row 42 | RelationshipToHoH | PICKLIST_INVALID_CODE | RelationshipToHoH has invalid code 999 for picklist 1.27 Relationship to HoH (valid: [1, 2, 3, 4, 5])
```

**JSON Format** (for monitoring dashboards):
```json
{
  "rowContext": "Enrollment row 42",
  "fieldName": "RelationshipToHoH",
  "severity": "ERROR",
  "errorCode": "PICKLIST_INVALID_CODE",
  "message": "RelationshipToHoH has invalid code 999 for picklist 1.27 Relationship to HoH (valid: [1, 2, 3, 4, 5])",
  "timestamp": "2024-10-07T14:32:15.123Z"
}
```

---

## Instrumentation

### CSVExportStrategy Integration

**Method**: `formatWithValidation(sections, exportPeriod, exportJobId, validationLogger)`

**Workflow**:
1. Initialize `CsvValidationLogger` with export job ID
2. For each CSV section (Client, Enrollment, Exit, Services, etc.):
   - For each row:
     - **Invoke validation** via `validateRow()`
     - **Propagate diagnostics** to logger via `logger.logBatch()`
     - **Decision**:
       - If `hasErrors()` → Skip row (do not emit)
       - Else → Emit row to CSV
3. **Log summary** via `logger.logSummary()`
4. **Fail export** if `logger.hasErrors()` → Throw `CsvValidationException`

**Section-Specific Validators**:
- `validateClientRow()` - Name, SSN, DOB data quality
- `validateEnrollmentRow()` - Entry date, relationship, living situation, disabling condition
- `validateExitRow()` - Exit date, destination
- `validateServicesRow()` - Date provided, record type
- `validateIncomeBenefitsRow()` - Information date, data collection stage
- `validateHealthAndDVRow()` - DV victim status, when occurred
- `validateDisabilitiesRow()` - Disability type, disability response

---

## Diagnostic Logging

### CsvValidationLogger

**Initialization**:
```java
CsvValidationLogger logger = new CsvValidationLogger("export-job-2024-10-07-001");
```

**Logging Diagnostics**:
```java
ValidationDiagnostic diagnostic = CsvValidationUtilities.validateDateInRange(...);
logger.log(diagnostic);

// Or batch:
List<ValidationDiagnostic> diagnostics = validateRow(...);
logger.logBatch(diagnostics);
```

**Summary Output**:
```java
logger.logSummary();
```

**Example Log Output**:
```
INFO  - CSV Validation Summary for Export Job: export-job-2024-10-07-001
INFO  - Total Validations: 1250
INFO  - Success: 1180 (94.4%)
INFO  - Warnings: 35 (2.8%)
INFO  - Errors: 35 (2.8%)
INFO  - Top Error Codes:
INFO  -   PICKLIST_INVALID_CODE - 20 occurrences
INFO  -   REQUIRED_FIELD_NULL - 10 occurrences
INFO  -   DATE_BEFORE_HMIS_EPOCH - 5 occurrences
ERROR - CSV validation FAILED for export job export-job-2024-10-07-001 with 35 errors
```

**JSON Summary** (for monitoring):
```java
String summaryJson = logger.getSummaryJson();
```

**Example JSON**:
```json
{
  "exportJobId": "export-job-2024-10-07-001",
  "totalValidations": 1250,
  "successCount": 1180,
  "warningCount": 35,
  "errorCount": 35,
  "successRate": 0.944,
  "warningRate": 0.028,
  "errorRate": 0.028,
  "passed": false,
  "errorCodeFrequency": {
    "PICKLIST_INVALID_CODE": 20,
    "REQUIRED_FIELD_NULL": 10,
    "DATE_BEFORE_HMIS_EPOCH": 5
  },
  "topErrors": [
    {
      "rowContext": "Enrollment row 42",
      "fieldName": "RelationshipToHoH",
      "severity": "ERROR",
      "errorCode": "PICKLIST_INVALID_CODE",
      "message": "RelationshipToHoH has invalid code 999...",
      "timestamp": "2024-10-07T14:32:15.123Z"
    }
  ],
  "topWarnings": [...]
}
```

---

## Support Procedures for Remediation

### Error Code Resolution Guide

| Error Code | Severity | Remediation |
|------------|----------|-------------|
| `DATE_NULL` | ERROR | Required date field missing - verify data collection workflow |
| `DATE_PARSE_FAILURE` | ERROR | Invalid date format - check data type mappings |
| `DATE_BEFORE_HMIS_EPOCH` | ERROR | Date before 1998-10-01 - correct historical data entry |
| `DATE_TOO_FAR_FUTURE` | ERROR | Date >30 days in future - verify system clock or data entry |
| `DATE_BEFORE_EXPORT_PERIOD` | WARNING | Date outside export period - acceptable for ongoing enrollments |
| `DATE_AFTER_EXPORT_PERIOD` | WARNING | Date outside export period - verify export date range |
| `REQUIRED_FIELD_NULL` | ERROR | Required field missing - review HUD requirements and data collection |
| `CONDITIONAL_FIELD_NULL` | WARNING | Conditional field missing - verify business rules apply |
| `PICKLIST_PARSE_FAILURE` | ERROR | Non-integer value in picklist field - check data type conversion |
| `PICKLIST_INVALID_CODE` | ERROR | Code not in HUD 2024 picklist - update to valid HUD code |
| `DATE_SEQUENCE_VIOLATION` | ERROR | Exit before entry - correct date logic |
| `DATE_SEQUENCE_EQUAL` | WARNING | Same-day entry/exit - verify if allowed per project type |

### Data Quality Dashboard Integration

**Metrics to Monitor**:
- Error rate by section (Client, Enrollment, Exit, Services)
- Error code frequency distribution
- Validation success rate trends over time
- Rows rejected vs. rows exported

**Alert Thresholds**:
- Error rate >5% → High priority alert
- Specific error code >10 occurrences → Investigation required
- Any `DATE_BEFORE_HMIS_EPOCH` errors → Critical data quality issue

### Remediation Workflow

1. **Export Fails with Errors**:
   - Retrieve validation summary JSON from `CsvValidationLogger`
   - Identify top error codes and affected row contexts
   - Query source data using anonymized row numbers (e.g., "Enrollment row 42")
   - Correct data quality issues in source system
   - Re-run export

2. **Export Succeeds with Warnings**:
   - Review warning diagnostics
   - Assess if warnings are acceptable (e.g., dates outside export period)
   - Document acceptable warnings in export audit metadata
   - Proceed with export submission

3. **Persistent Validation Failures**:
   - Review HUD picklist code mappings in `HudPicklistCodes.java`
   - Verify against latest HUD Data Standards (2024 v1.0)
   - Check for mapping errors in `reporting_field_mapping` table
   - Validate data collection workflows against HUD specifications

---

## Testing

### Unit Tests

**Location**: `backend/modules/reporting/src/test/java/org/haven/reporting/application/validation/`

**Coverage**:
- `CsvValidationUtilitiesTest.java` - Comprehensive edge-case testing
  - Invalid picklist codes
  - Null value handling
  - Out-of-range dates
  - PII sanitization
  - Boundary value testing

### Integration Tests

**Location**: `backend/modules/reporting/src/test/java/org/haven/reporting/application/services/`

**Coverage**:
- `CSVExportStrategyValidationTest.java` - End-to-end validation testing
  - Valid data export
  - Invalid code rejection
  - Null field handling
  - Mixed valid/invalid rows
  - Anonymized diagnostic output

### Test Fixtures

**Edge Cases Tested**:
- RelationshipToHoH: 999 (invalid code)
- EntryDate: 1997-01-01 (before HMIS epoch)
- EntryDate: 2025-12-01 (too far future)
- Destination: null (required field missing)
- DataCollectionStage: 99 (invalid code)
- NameDataQuality: "invalid" (non-integer)

**Run Tests**:
```bash
./gradlew :backend:modules:reporting:test --tests "*Validation*"
```

---

## Configuration

### Enable Validation (Default)

Validation is **enabled by default** for all CSV exports. Use `formatWithValidation()` method.

### Disable Validation (Legacy Mode)

For backward compatibility, legacy `format()` method bypasses validation:
```java
byte[] csv = csvExportStrategy.format(sections);  // No validation
```

**⚠️ Not recommended for production exports**

### Custom Validation Logger

Provide custom logger for specialized monitoring:
```java
CsvValidationLogger customLogger = new CsvValidationLogger("custom-job-id");
byte[] csv = csvExportStrategy.formatWithValidation(
    sections,
    exportPeriod,
    "custom-job-id",
    customLogger
);

// Retrieve diagnostics
CsvValidationLogger.ValidationSummary summary = customLogger.getSummary();
```

---

## Compliance Notes

### HUD Data Standards Conformance

- **Picklist Codes**: HUD HMIS Data Standards 2024 v1.0 (effective 2024-10-01)
- **Universal Data Elements**: 3.01 - 3.917 validated per HDX 2024 v1.0
- **Date Boundaries**: HMIS epoch (1998-10-01) per HUD system inception
- **Data Quality Framework**: Aligned with HUD Data Quality Framework thresholds

### VAWA Compliance

PII-safe error formatting ensures:
- No SSN values in logs
- No DOB values in logs
- No client names in logs
- Anonymized row identifiers only

### Audit Trail

All validation diagnostics logged to structured channel suitable for:
- Export audit metadata storage
- Data quality monitoring dashboards
- HUD APR reporting requirements
- System audit compliance

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-10-07 | Initial implementation - Centralized validation utilities, structured logging, PII-safe diagnostics |

---

## References

- HUD HMIS Data Standards 2024 v1.0: https://hudexchange.info
- HUD Data Quality Framework: https://hudexchange.info/data-quality
- RFC 4180 CSV Specification: https://tools.ietf.org/html/rfc4180
- Migration V35: HUD 2024 Universal Data Elements seed data
