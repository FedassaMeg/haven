# HUD Specification Metadata

This directory contains YAML-encoded metadata for HUD HMIS reporting specifications.

## Purpose

These configuration files provide executable metadata for generating HUD-compliant reports:
- Field mappings from Haven domain model to HUD data elements
- Transformation rules and calculations
- VAWA confidentiality requirements
- Data quality standards
- Versioning tied to HUD notices and effective dates

## File Organization

### CoC Annual Performance Report (APR)
- `coc-apr-q04a-spec.yaml` - Q4a: Persons Served
- `coc-apr-q23c-spec.yaml` - Q23c: Exit Destinations to Permanent Housing

### System Performance Measures (SPM)
- `spm-metric1a-spec.yaml` - Metric 1a: Length of Time Homeless

### Point-in-Time Count
- `pit-count-spec.yaml` - Annual PIT count specifications

### HMIS CSV Export
- `hmis-csv-client-spec.yaml` - Client.csv format specification

## Compliance References

### HUD Regulations
- **24 CFR 578.103** - CoC APR requirements
- **24 CFR 578.37** - Project type definitions

### HUD Notices
- **Notice CPD-23-08** - FY2023 CoC Program Competition
- **Notice CPD-17-01** - System Performance Measures
- **HDX 2024** - HMIS Data Standards and CSV Format Specifications

### VAWA Requirements
- **Violence Against Women Act (VAWA)** - Confidentiality protections for DV victims
- Requires explicit client consent before disclosing DV-related data in individual-level reports
- Aggregate counts permitted without individual consent per HUD guidance

## Field Mapping Structure

Each specification includes:

```yaml
reportType: CoC_APR | ESG_CAPER | SYSTEM_PERFORMANCE_MEASURES | PIT_HIC | HMIS_CSV
sectionIdentifier: "Q04a" | "Metric1a" | "PIT-S1" | etc.
versionIdentifier: Version tied to HUD spec release
effectiveFrom: YYYY-MM-DD
effectiveTo: YYYY-MM-DD (null = currently active)

scopeFilters:
  cocCodes: [...]
  projectTypes: [...]
  fundingSources: [...]

inclusionLogic:
  # Rules for what data to include

exclusionLogic:
  # Rules for what data to exclude

fieldMappings:
  - sourceEntity: Domain entity name
    sourceField: Field path
    targetHudElement: "Spec:Element"
    requiredFlag: R | C | O
    vawaSensitiveField: true | false
    vawaSuppressionBehavior: SUPPRESS | AGGREGATE_ONLY | REDACT
    transformationRule: Rule name from TransformationRule entity
```

## VAWA Suppression Behaviors

### SUPPRESS
Complete exclusion from export when consent not given.
Used for: SSN, individual-level DV victim identification

### AGGREGATE_ONLY
Individual records suppressed, but included in aggregate counts.
Used for: DV victim counts in PIT, demographic aggregates

### REDACT
Record included but sensitive field shown as NULL or HUD "not collected" code.
Used for: Names, DOB when DV victim without consent

## Transformation Rules

Common transformations defined in `TransformationRule` entity:
- `AGE_AT_ENROLLMENT` - Calculate age per HUD Universal Data Element 3.03
- `ES_PROJECT_TYPE_FILTER` - Emergency Shelter project types (1, 2, 3, 8)
- `COC_FUNDED_PROJECT_FILTER` - CoC funding source filter
- `HEAD_OF_HOUSEHOLD_CHECK` - HoH identification (relationshipToHoH = 1)
- `RACE_NONE_DEFAULT` - Default null race to code 8
- `GENDER_NONE_DEFAULT` - Default null gender to code 99
- `LENGTH_OF_STAY_DAYS` - Calculate days between entry and exit
- `VAWA_DV_VICTIM_REDACTION` - Consent-based redaction

## Usage in Code

These YAML files are loaded by `ReportingMetadataService` to:
1. Generate SQL queries for report data extraction
2. Apply VAWA consent filtering via `ConfidentialityPolicyService`
3. Transform source data to HUD format
4. Validate data quality before submission
5. Maintain audit trail via `PolicyDecisionLog`

## Versioning

Specifications are version-controlled with effective dates:
- `effectiveFrom` - When this version becomes active (typically HUD fiscal year start: October 1)
- `effectiveTo` - When superseded by new version (null = currently active)
- `hudNoticeReference` - HUD notice or spec document authorizing this version

## Data Lineage

Each mapping provides bidirectional traceability:
- **Source → HUD**: Haven domain field → HUD element ID
- **HUD → Source**: HUD element ID → Haven domain field
- **Transform**: SQL/Java expression applied during conversion
- **Audit**: VAWA decisions logged in `PolicyDecisionLog`

## Adding New Specifications

When HUD publishes new specifications:

1. Create new YAML file in this directory
2. Define `effectiveFrom` date (usually Oct 1 of HUD fiscal year)
3. Map all required (`R`) and conditionally required (`C`) fields
4. Tag VAWA-sensitive fields with appropriate suppression behavior
5. Reference applicable transformation rules
6. Update `ReportingMetadataService` if new transformation logic needed
7. Run validation: `validateMappingConfiguration(specType)`

## Testing

Use `ReportingMetadataService.validateMappingConfiguration()` to check:
- All required fields mapped
- VAWA-sensitive fields have suppression behavior defined
- Transformation expressions reference valid rules
- Effective dates don't overlap for same section
