# HUD Export Pipeline Service

Implementation of event-sourced CQRS export pipeline for HUD HMIS compliance reporting.

## Architecture Overview

### Domain Model

**ExportJobAggregate** - Event-sourced aggregate with state machine:
- States: `QUEUED → MATERIALIZING → VALIDATING → COMPLETE/FAILED`
- Events: `ExportJobQueued`, `ExportJobStateChanged`, `ExportJobCompleted`, `ExportJobFailed`
- Repository: `EventSourcedExportJobRepository` using event store pattern

**ExportAuditMetadata** - Compliance tracking entity:
- Captures: AccessContext (user, IP, session), reporting period, SHA-256 hash
- Retention: 90-day default per HUD guidance
- Repository: `ExportAuditMetadataRepository` (JPA)

### Application Services

**ExportJobApplicationService** - Main orchestration service:
- Queues export requests
- Coordinates async processing pipeline
- Manages state transitions
- Creates audit trail

**HUDExportViewGenerator** - View materialization service:
- Generates parameterized views for each CSV table (Client, Enrollment, Exit, Services, CurrentLivingSituation)
- Applies operating year filters (Oct 1 - Sep 30 for CoC APR)
- Joins to PolicyDecisionLog to exclude VAWA-denied records
- Supports suppression behaviors: SUPPRESS, REDACT, AGGREGATE_ONLY

**CsvBlobStorageService** - Artifact storage service:
- File-based storage (extensible to S3/Azure Blob)
- ZIP compression for multi-file exports
- SHA-256 hash generation for integrity verification
- Automatic cleanup based on retention policy

### Infrastructure

**ReportingAsyncConfiguration** - Thread pool configuration:
- `reportGenerationExecutor`: 2-8 threads for materialization
- `reportValidationExecutor`: 2-4 threads for validation
- Separate from transaction pool to prevent blocking

**ExportJobEventHandler** - Event listener:
- Integrates with AuditService
- Logs export lifecycle events
- Creates ExportAuditMetadata records

## HUD Compliance Features

### VAWA Protection
- **PolicyDecisionLog Integration**: Joins to policy decisions to exclude records denied by VAWA consent checks
- **Field-Level Suppression**:
  - `SUPPRESS`: Omit entire record
  - `REDACT`: Mask field value with `***REDACTED***`
  - `AGGREGATE_ONLY`: Allow in aggregate reports only
- **Protected Entities**:
  - CurrentLivingSituation with DV indicators
  - ServiceEpisode with type 14 (Health & DV services)

### HUD Data Standards 2024
- **ReportingMetadataRepository**: Version-locked field mappings per HDX 2024 v1.0
- **Code Lists**: Preserves official codes (RaceNone=8, GenderNone=99)
- **Universal Data Elements**: 3.01-3.917 seeded via migrations
- **CSV Format**: Client, Enrollment, Exit, Services, CurrentLivingSituation tables

### 24 CFR 578 (CoC Program)
- **Operating Year Filtering**: Oct 1 - Sep 30 CoC APR period
- **Project Type Restrictions**: Enforced per report specification
- **CoC Code Filtering**: Ensures only CoC-funded projects in reports

## Usage Example

```java
@Autowired
private ExportJobApplicationService exportService;

// Request CoC APR export
UUID exportJobId = exportService.requestExport(
    "HMIS_CSV",                              // Export type
    LocalDate.of(2024, 10, 1),               // Period start
    LocalDate.of(2025, 9, 30),               // Period end
    List.of(projectId1, projectId2),         // Project IDs
    "CA-500",                                // CoC code
    "Annual Performance Report",             // Reason
    accessContext                            // User context
);

// Monitor status
ExportJobAggregate job = exportJobRepository.findById(exportJobId).get();
ExportJobState state = job.getState(); // QUEUED, MATERIALIZING, VALIDATING, COMPLETE, FAILED
```

## Database Schema

### Migration Scripts
- `V34__reporting_metadata_schema.sql` - Reporting field mapping tables
- `V35__seed_hud_2024_universal_data_elements.sql` - UDE 3.01-3.917
- `V36__seed_enrollment_services_segments.sql` - Client/Enrollment/Exit/Services CSV mappings
- `V37__seed_project_descriptor_mappings.sql` - Project/Organization/Funder descriptors
- `V38__export_audit_metadata_schema.sql` - Export audit tables

### Key Tables
- `reporting_field_mapping` - HUD field mappings with VAWA flags
- `export_audit_metadata` - Compliance audit trail
- `policy_decision_log` - VAWA consent decisions (from read-models module)

## Configuration

```yaml
haven:
  reporting:
    storage:
      base-path: ./data/exports       # CSV storage location
      retention-days: 90              # Retention policy (HUD guidance)
    async:
      generation:
        core-pool-size: 2
        max-pool-size: 8
        queue-capacity: 50
      validation:
        core-pool-size: 2
        max-pool-size: 4
        queue-capacity: 20
```

## Dependencies

- **shared-kernel**: AccessContext, AuditService, ReportingMetadataRepository
- **event-store**: Event sourcing infrastructure
- **read-models**: PolicyDecisionLog for VAWA filtering
- **client-profile**: Client demographic data
- **program-enrollment**: Enrollment/Exit data
- **service-delivery**: Service episode data
- **case-mgmt**: Case coordination (optional linkage)

## Security Considerations

1. **Access Control**: All exports logged with AccessContext (user, IP, session)
2. **VAWA Compliance**: Automatic suppression/redaction per PolicyDecisionLog
3. **Audit Trail**: SHA-256 hash for tamper detection
4. **Data Retention**: Automatic cleanup after 90 days
5. **Rate Limiting**: Track export requests per user via ExportAuditMetadataRepository

## Future Enhancements

- [ ] S3/Azure Blob Storage adapter
- [ ] CoC APR aggregate report generation
- [ ] ESG CAPER report format
- [ ] System Performance Measures (SPM)
- [ ] PIT/HIC count reports
- [ ] CSV validation against HUD XSD schemas
- [ ] Export scheduling (cron jobs for recurring reports)
- [ ] Email notification on completion
