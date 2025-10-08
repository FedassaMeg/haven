# HUD Export Validation & QA

## Overview

Comprehensive validation and quality assurance framework for HUD HMIS exports. Ensures data quality, compliance, and regression prevention through automated testing and monitoring.

## Components

### 1. HUDExportValidationService

**Location:** `src/main/java/.../HUDExportValidationService.java`

Validates exports against HUD Data Quality Framework:

#### Universal Data Element Checks
- **SSN Validation**: 9-digit format, no all-zeros or sequential patterns
- **DOB Validation**: Logical age ranges (0-120 years)
- **Name Completeness**: First or last name required
- **Race/Ethnicity**: At least one category selected
- **Gender**: At least one category selected

#### Data Quality Thresholds
- Required completeness: **95%** for Universal DEs
- SSN valid patterns: No `000000000`, `111111111`, `123456789`
- Age limits: 0-120 years from DOB

#### Referential Integrity
- All `PersonalID` in Enrollment exist in Client
- All `ProjectID` in Enrollment exist in Project
- All `EnrollmentID` in Exit exist in Enrollment
- All `EnrollmentID` in Services exist in Enrollment

#### Date Sequencing
- ExitDate ≥ EntryDate
- MoveInDate ≥ EntryDate
- ServiceDate within enrollment period

### 2. Test Fixtures

**Location:** `src/test/java/.../fixtures/HUDSyntheticDataFixtures.java`

Synthetic dataset matching HUD FY2024 structure:

#### Standard Cases (8 clients)
- Complete demographic data
- Standard enrollments across project types
- Valid exits to permanent housing

#### Edge Cases
1. **VAWA-Protected Client** (C004)
   - Domestic violence survivor
   - Service-level restrictions apply

2. **Overlapping Enrollments** (C003)
   - Simultaneous ES + SO enrollments
   - Valid for different project types

3. **Missing Consent** (C007)
   - Partial name, unknown SSN/DOB
   - Data quality warnings expected

4. **Invalid SSN** (test fixture)
   - `000000000` pattern
   - Should trigger validation error

5. **Date Sequencing Error** (test fixture)
   - Move-in before entry
   - Should trigger validation error

6. **Orphan Exit** (test fixture)
   - References non-existent enrollment
   - Should trigger referential integrity error

### 3. Regression Testing

**Location:** `src/test/java/.../ExportRegressionTest.java`

#### Tolerance-Based Comparisons

**Client Counts by Project Type** (±2% tolerance)
```java
@Test
void testClientCounts_ByProjectType_WithinTolerance() {
    // Expected: ES=2, TH=2, PSH=2, SO=1, Safe Haven=1
    // Actual counts must be within ±2%
}
```

**Completeness Rates** (≥95% threshold)
```java
@Test
void testDataElementCompleteness_UniversalDEs_MeetsThreshold() {
    // SSN, DOB, Name completeness > 95%
}
```

**APR Question Responses** (exact match)
```java
@Test
void testAPRQ6_HouseholdTypes_MatchesGoldenOutput() {
    // Adults Only: 5
    // Adults & Children: 1
    // Children Only: 0
}
```

### 4. Test Harness

**Location:** `src/test/java/.../harness/ExportTestHarness.java`

Automated test suite with:
- **8 comprehensive tests**
- **HTML report generation**
- **CI/CD integration**
- **Exit codes for build gates**

#### Test Suite
1. Standard Dataset Validation
2. VAWA-Protected Data
3. Overlapping Enrollments
4. Missing Consent Edge Case
5. Invalid SSN Detection
6. Referential Integrity
7. Date Sequencing
8. Completeness Rates

#### Running Tests

**Command Line:**
```bash
cd backend/modules/reporting
./gradlew test
```

**Test Harness:**
```bash
./gradlew exportValidationHarness
```

**Regression Only:**
```bash
./gradlew regressionTest
```

**Data Quality Checks:**
```bash
./gradlew dataQualityCheck
```

#### CI/CD Integration

**Automatic Triggers:**
- Push to `master` or `develop`
- Pull requests to `master`
- Nightly at 2 AM UTC

**Build Gates:**
- Tests must pass (exit code 0)
- Variance within ±2% tolerance
- Completeness rates > 95%

**Failure Actions:**
- PR comment with error details
- Slack notification (scheduled runs)
- Test report artifacts uploaded

### 5. Export Alerts

**Location:** `src/main/java/.../ExportAlertService.java`

Monitoring and alerting for:

#### SLA Violations
```java
exportAlertService.checkSLAViolation(
    exportJobId, exportType, startTime, endTime
);
// Alert if duration > 5 minutes
```

#### Data Quality Failures
```java
exportAlertService.alertDataQualityFailure(
    exportJobId, exportType, errorCount, errors
);
// Alert on mandatory element threshold failures
```

#### Excessive Consent Restrictions
```java
exportAlertService.checkExcessiveConsentRestrictions(
    exportJobId, exportType, totalRecords, excludedRecords
);
// Alert if >10% records excluded
```

#### Validation Warnings
```java
exportAlertService.checkValidationWarnings(
    exportJobId, exportType, warningCount, warnings
);
// Alert if >10 warnings
```

## Usage

### Basic Validation

```java
HUDExportValidationService validator = new HUDExportValidationService();

Map<String, List<Map<String, Object>>> export = generateExport();
ValidationResult result = validator.validateExport(export);

if (result.hasErrors()) {
    logger.error("Validation failed with {} errors", result.getErrorCount());
    result.getErrors().forEach(error -> logger.error("{}", error));

    // Alert data quality team
    exportAlertService.alertDataQualityFailure(
        exportJobId,
        exportType,
        result.getErrorCount(),
        result.getErrors().stream()
            .map(ValidationIssue::message)
            .toList()
    );
}
```

### Regression Testing

```java
@Test
void testExport_ClientCounts_WithinTolerance() {
    Map<String, Integer> actual = countClientsByProjectType(export);
    Map<String, Integer> expected = goldenOutputs.getClientCountsByProjectType();

    for (String projectType : expected.keySet()) {
        int exp = expected.get(projectType);
        int act = actual.getOrDefault(projectType, 0);

        double variance = Math.abs((act - exp) * 100.0 / exp);

        assertThat(variance)
            .as("Variance for %s", projectType)
            .isLessThanOrEqualTo(2.0); // ±2% tolerance
    }
}
```

### Alerting Integration

```java
// In ExportJobApplicationService

Instant startTime = Instant.now();

// Generate export...

Instant endTime = Instant.now();

// Check SLA
exportAlertService.checkSLAViolation(exportJobId, exportType, startTime, endTime);

// Validate
ValidationResult result = validator.validateExport(sections);

// Alert on issues
if (result.hasErrors()) {
    exportAlertService.alertDataQualityFailure(
        exportJobId, exportType, result.getErrorCount(), errorList
    );
}

if (result.getWarningCount() > 10) {
    exportAlertService.checkValidationWarnings(
        exportJobId, exportType, result.getWarningCount(), warningList
    );
}

// Check consent restrictions
long excluded = vawaSupressedRecords + consentDeniedRecords;
exportAlertService.checkExcessiveConsentRestrictions(
    exportJobId, exportType, totalRecords, excluded
);
```

## Golden Outputs

### Client Counts by Project Type

| Project Type | Expected Count |
|--------------|----------------|
| ES           | 2              |
| TH           | 2              |
| PSH          | 2              |
| RRH          | 0              |
| SO           | 1              |
| Safe Haven   | 1              |

**Tolerance:** ±2%

### APR Q6: Household Types

| Household Type      | Expected |
|---------------------|----------|
| Adults Only         | 5        |
| Adults & Children   | 1        |
| Children Only       | 0        |

### APR Q7: Veteran Status

| Status      | Expected |
|-------------|----------|
| Veterans    | 1        |
| Non-Veterans| 5        |

### Completeness Rates

| Data Element | Expected Rate |
|--------------|---------------|
| SSN          | 87.5%         |
| DOB          | 100%          |
| Name         | 100%          |

**Threshold:** 95% (warnings below threshold)

## Test Reports

### HTML Report Format

Generated at: `target/test-reports/test-report-{timestamp}.html`

**Includes:**
- Test summary (passed/failed counts)
- Success rate percentage
- Detailed results table
- Error messages for failures
- Duration metrics

### CI/CD Artifacts

**Uploaded on every run:**
- Test reports (30-day retention)
- Regression metrics (90-day retention)
- JUnit XML results

**Download:**
```bash
# GitHub Actions
gh run download <run-id> -n export-test-reports
```

## Monitoring Dashboard

### Key Metrics

1. **Export Generation Time**
   - Target: < 5 minutes
   - Alert: > 5 minutes

2. **Data Quality Score**
   - Completeness rates
   - Validation error counts
   - Warning trends

3. **Regression Status**
   - Client count variance
   - APR response accuracy
   - Completeness rate changes

4. **Consent Restriction Rate**
   - Target: < 10%
   - Alert: > 10%

### Integration Points

**Grafana Dashboard:**
```promql
# Export duration
export_generation_duration_seconds{job="haven-reporting"}

# Validation errors
export_validation_errors_total{severity="error"}

# Consent restrictions
export_consent_restricted_records_ratio
```

**DataDog Monitors:**
- SLA violation detection
- Data quality threshold alerts
- Regression baseline comparison

## Troubleshooting

### "SSN contains invalid pattern"

**Cause:** SSN is all zeros, all same digit, or sequential

**Fix:** Verify SSN data quality or mark as Unknown (DataQuality=9)

### "EnrollmentID not found in Enrollment.csv"

**Cause:** Orphan exit or service record

**Fix:** Check enrollment materialization query; ensure exits join correctly

### "ExitDate before EntryDate"

**Cause:** Date logic error in source data

**Fix:** Audit enrollment dates; implement data quality check at intake

### "Completeness rate below threshold"

**Cause:** Missing required data elements

**Fix:**
1. Check intake forms for required fields
2. Verify data import process
3. Review staff training on Universal DEs

### "Excessive consent restrictions (>10%)"

**Cause:** Possible VSP flag misconfiguration

**Fix:**
1. Review VSP project flags
2. Verify consent collection process
3. Check consent filter logic in export query

## Best Practices

### 1. Run Tests Before Submission

Always run full validation suite before HUD submission:

```bash
./gradlew test exportValidationHarness
```

### 2. Review Warnings

Even if errors pass, review warnings for data quality issues:

```java
if (result.getWarningCount() > 0) {
    result.getWarnings().forEach(w -> logger.warn("{}", w));
}
```

### 3. Track Regression Metrics

Monitor trends over time:
- Client count variance
- Completeness rate changes
- Validation error patterns

### 4. Update Golden Outputs

When system changes legitimately affect outputs:

```java
// Update in ExportRegressionTest.GoldenOutputs
Map<String, Integer> getClientCountsByProjectType() {
    counts.put("ES", 3);  // Updated from 2
    // Document reason in commit message
}
```

### 5. Test Edge Cases

Always include edge cases in test data:
- VAWA-protected clients
- Overlapping enrollments
- Missing consent
- Data quality issues

## Future Enhancements

- [ ] XML schema validation against `HUD_HMIS.xsd`
- [ ] Automated baseline update on approved changes
- [ ] Historical trend analysis dashboard
- [ ] Machine learning anomaly detection
- [ ] Real-time validation websocket updates
