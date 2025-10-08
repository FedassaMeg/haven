# Export Job Orchestration & Compliance Runbook

## Overview

Operational runbook for Haven HMIS export job orchestration with integrated compliance guardrails.

**Effective Date**: 2025-10-07
**Owner**: Platform Engineering & Data Compliance Teams
**Review Cycle**: Quarterly

---

## Architecture Overview

### Orchestration Pipeline

```
Request Export
    ↓
1. Queue Job → ExportJobAggregate created
    ↓
2. Materialize Views → CSV data generation (VAWA-filtered)
    ↓
3. Validate → CSV validation guardrails
    ↓
4. Package → ZIP bundle with manifest & signature
    ↓
5. Encrypt → KMS-managed AES-256-GCM encryption
    ↓
6. Store → Encrypted bundle → secure storage location
    ↓
7. Consent Ledger → Emit ledger entry via compliance API
    ↓
8. Notify → Email compliance administrators
    ↓
9. Complete → Audit metadata persisted
```

### Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `ExportJobOrchestrationService` | Main orchestration coordinator | `backend/modules/reporting/application/services/` |
| `KmsEncryptionService` | Envelope encryption with KMS | `backend/modules/reporting/infrastructure/security/` |
| `ConsentLedgerService` | Consent tracking API client | `backend/modules/reporting/application/services/` |
| `ExportNotificationService` | Multi-channel notification delivery | `backend/modules/reporting/application/services/` |
| `CsvValidationUtilities` | CSV validation guardrails | `backend/modules/reporting/application/validation/` |

---

## Encryption Storage Locations

### Development/Testing

**Path**: `./data/exports/encrypted/{exportJobId}.enc`
**Format**: Envelope encryption storage format (IV + encrypted DEK + ciphertext)
**Permissions**: `0600` (owner read/write only)

### Staging

**Path**: S3 bucket `s3://haven-staging-exports/{year}/{month}/{exportJobId}.enc`
**Encryption**: Server-side encryption with AWS KMS
**KMS Key**: `arn:aws:kms:us-west-2:ACCOUNT:key/staging-export-key`
**Lifecycle**: 90-day retention, automatic purge

### Production

**Path**: S3 bucket `s3://haven-prod-exports/{year}/{month}/{exportJobId}.enc`
**Encryption**: Server-side encryption + client-side envelope encryption
**KMS Key**: `arn:aws:kms:us-west-2:ACCOUNT:key/prod-export-key`
**Lifecycle**: 90-day retention (HUD guidance), automatic purge
**Replication**: Cross-region replication to `us-east-1` for disaster recovery
**Access Logging**: CloudTrail logs all access events

### Access Control

**IAM Policy** (minimum required):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::haven-prod-exports/*",
      "Condition": {
        "StringEquals": {
          "s3:x-amz-server-side-encryption": "aws:kms"
        }
      }
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:GenerateDataKey"
      ],
      "Resource": "arn:aws:kms:us-west-2:ACCOUNT:key/prod-export-key"
    }
  ]
}
```

---

## Consent Ledger Integration

### Compliance API Endpoints

**Base URL** (production): `https://compliance.haven.internal/api/v1`

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/ledger/entries` | POST | Emit consent ledger entry |
| `/ledger/entries/data-subject/{id}` | GET | Query by data subject |
| `/ledger/entries/purge` | POST | Mark export as purged |

### Ledger Entry Schema

```json
{
  "exportJobId": "uuid",
  "dataSubjects": ["hashed-client-id-1", "hashed-client-id-2"],
  "dataSubjectCount": 150,
  "consentScope": "FULL_EXPORT | LIMITED_DATA_ELEMENTS | VAWA_PROTECTED",
  "consentScopeDescription": "Full HUD export with all UDEs",
  "exportHashMode": "HASH_ALL_SSN | HASH_NONE | HASH_VAWA_ONLY",
  "exportHashDescription": "All SSNs hashed per HUD 2024 specs",
  "retentionWindow": "2025-04-07T00:00:00Z",
  "retentionDays": 90,
  "exportPeriodStart": "2024-01-01",
  "exportPeriodEnd": "2024-12-31",
  "exportType": "HUD_HMIS",
  "exportReason": "Annual HUD APR reporting",
  "requestedBy": "user@haven.org",
  "exportedAt": "2025-01-07T14:30:00Z",
  "storageLocation": "s3://haven-prod-exports/2025/01/export-uuid.enc",
  "exportSha256Hash": "abc123def456...",
  "encrypted": true,
  "kmsKeyId": "arn:aws:kms:...",
  "vawaProtected": true,
  "vawaSuppressedRecords": 15
}
```

### API Authentication

**Header**: `X-API-Key: {compliance-api-key}`
**Key Rotation**: Every 90 days
**Key Storage**: AWS Secrets Manager `haven/compliance/api-key`

### Ledger Expectations

1. **Data Subject Anonymization**: All `PersonalID` values must be hashed using SHA-256 before inclusion in ledger
2. **Retention Enforcement**: Ledger entry must include `retentionWindow` matching storage lifecycle
3. **Purge Notification**: When export is auto-deleted, call `/ledger/entries/purge` to update ledger
4. **Audit Trail**: All ledger API calls logged to compliance audit log

---

## Monitoring Dashboards

### CloudWatch Metrics

| Metric | Namespace | Dimensions | Threshold |
|--------|-----------|------------|-----------|
| `ExportJobCompletionRate` | Haven/Reporting | ExportType | >95% success |
| `EncryptionDurationMs` | Haven/Reporting | KmsKeyId | <5000ms |
| `ValidationErrorRate` | Haven/Reporting | Section | <2% |
| `ConsentLedgerLatencyMs` | Haven/Reporting | - | <1000ms |
| `NotificationDeliveryRate` | Haven/Reporting | Channel | >99% |

### Datadog Dashboard

**Dashboard URL**: `https://datadog.haven.internal/dashboard/export-orchestration`

**Panels**:
- Export job throughput (jobs/hour)
- Validation error breakdown by error code
- Encryption performance (time, size delta)
- Consent ledger API latency percentiles (p50, p95, p99)
- Storage I/O metrics (write throughput, storage size)
- Notification delivery success rate

### Alerts

| Alert | Condition | Severity | Notification |
|-------|-----------|----------|--------------|
| Export Job Failure Rate >5% | >5% failures in 15 min window | Critical | PagerDuty + Email |
| KMS Encryption Failures | Any KMS errors | Critical | PagerDuty |
| Consent Ledger API Unavailable | 3 consecutive failures | High | Email |
| Storage Quota Exceeded | >80% of bucket quota | High | Email |
| Validation Error Spike | >10% error rate | Medium | Email |

---

## Standard Operating Procedures (SOPs)

### SOP-001: Request Export Job

**Frequency**: As needed (typically monthly/quarterly for HUD reporting)

**Steps**:

1. **Verify Prerequisites**
   - User has `ROLE_EXPORT_ADMIN` permission
   - Export period dates are valid
   - Project IDs exist and user has access
   - CoC code is valid

2. **Submit Export Request**
   ```bash
   POST /api/exports/jobs
   {
     "exportType": "HUD_HMIS",
     "reportingPeriodStart": "2024-01-01",
     "reportingPeriodEnd": "2024-12-31",
     "projectIds": ["uuid1", "uuid2"],
     "cocCode": "CA-600",
     "exportReason": "Annual HUD APR reporting",
     "consentScope": "FULL_EXPORT",
     "hashMode": "HASH_ALL_SSN",
     "encryptAtRest": true
   }
   ```

3. **Track Export Status**
   - Monitor export job state: `QUEUED` → `MATERIALIZING` → `VALIDATING` → `COMPLETED`
   - Check CloudWatch logs for progress: `/aws/lambda/export-orchestration`

4. **Await Notification**
   - Compliance administrators receive email when export completes
   - Email contains secure download link, consent ledger ID, validation summary

5. **Download & Verify**
   - Use secure download link (expires in 48 hours)
   - Verify SHA-256 hash matches notification
   - Decrypt if accessing outside Haven environment

---

### SOP-002: Validate Export Data Quality

**Frequency**: Every export before HUD submission

**Steps**:

1. **Review Validation Summary**
   - Check notification email for validation error/warning counts
   - Zero errors required for HUD submission
   - Review warnings for data quality concerns

2. **Download Validation Report**
   ```bash
   GET /api/exports/{exportJobId}/validation-report
   ```

3. **Analyze Error Codes**
   - `PICKLIST_INVALID_CODE`: Invalid HUD code list values → Correct in source system
   - `REQUIRED_FIELD_NULL`: Missing required fields → Complete data entry
   - `DATE_BEFORE_HMIS_EPOCH`: Historical data errors → Verify dates
   - `DATE_SEQUENCE_VIOLATION`: Exit before entry → Correct chronology

4. **Remediate Data Quality Issues**
   - Update source records in HMIS
   - Re-run export job after corrections
   - Verify validation errors resolved

5. **Document Quality Review**
   - Create data quality report
   - Archive in compliance folder
   - Update tracking spreadsheet

---

### SOP-003: Submit Export to HUD HDX Portal

**Frequency**: Annually (APR), as required for special reports

**Steps**:

1. **Download Encrypted Export**
   - Use secure link from notification email
   - Save to encrypted workstation

2. **Decrypt Export Bundle**
   ```bash
   # If decrypting outside Haven environment
   openssl enc -d -aes-256-gcm -in export-{id}.enc -out export-{id}.zip \
     -K {hex-key} -iv {hex-iv}
   ```

3. **Extract CSV Files**
   ```bash
   unzip export-{id}.zip -d export-{id}/
   ```

4. **Verify Manifest Integrity**
   ```bash
   sha256sum -c manifest.sha256
   ```

5. **Upload to HUD HDX**
   - Log in to HDX portal: https://hudhdx.info
   - Navigate to: HMIS → Uploads → New Upload
   - Select CSV files (Client.csv, Enrollment.csv, etc.)
   - Submit for validation

6. **Review HUD Validation Results**
   - Address any HUD-specific validation errors
   - Download HUD validation report
   - If errors: Correct in Haven, re-export, re-submit

7. **Confirm Submission**
   - Wait for HUD acceptance email
   - Download submission confirmation PDF
   - Archive confirmation in compliance folder

8. **Update Consent Ledger**
   ```bash
   POST /api/compliance/ledger/entries/{ledgerEntryId}/hud-submission
   {
     "submittedAt": "2025-01-15T10:30:00Z",
     "hudSubmissionId": "HDX-2025-001",
     "confirmationUrl": "https://hudhdx.info/submissions/..."
   }
   ```

---

### SOP-004: Respond to Data Subject Access Request (DSAR)

**Frequency**: As needed (GDPR/CCPA compliance)

**Steps**:

1. **Identify Exports Containing Data Subject**
   ```bash
   GET /api/compliance/ledger/entries/data-subject/{hashedPersonalId}
   ```

2. **Review Consent Ledger Entries**
   - List all exports containing the data subject
   - Note export dates, purposes, retention windows

3. **Retrieve Export Details**
   ```bash
   GET /api/exports/{exportJobId}/metadata
   ```

4. **Prepare DSAR Response**
   - Document all exports containing subject's data
   - List export purposes and legal bases
   - Provide retention timelines

5. **Purge Data if Requested**
   - If data subject requests deletion:
     ```bash
     POST /api/exports/{exportJobId}/purge
     {
       "reason": "DATA_SUBJECT_REQUEST",
       "requestedBy": "privacy@haven.org",
       "ticketId": "PRIVACY-123"
     }
     ```
   - Update consent ledger:
     ```bash
     POST /api/compliance/ledger/entries/{ledgerEntryId}/purge
     ```

6. **Confirm Completion**
   - Send DSAR response to data subject
   - Document in privacy compliance log

---

### SOP-005: Key Rotation (KMS Master Key)

**Frequency**: Annually (or if key compromised)

**Steps**:

1. **Generate New KMS Key**
   ```bash
   aws kms create-key \
     --description "Haven Export Master Key 2026" \
     --key-policy file://key-policy.json
   ```

2. **Update Key Alias**
   ```bash
   aws kms update-alias \
     --alias-name alias/haven-export-master-key \
     --target-key-id {new-key-id}
   ```

3. **Update Application Configuration**
   ```yaml
   haven:
     kms:
       key-id: arn:aws:kms:us-west-2:ACCOUNT:key/{new-key-id}
   ```

4. **Re-Encrypt Existing DEKs** (Optional)
   - Run batch job to re-encrypt all stored encrypted DEKs
   - Uses `KmsEncryptionService.rotateMasterKey()`

5. **Verify Encryption/Decryption**
   - Test export job with new key
   - Verify existing exports can still be decrypted

6. **Document Key Rotation**
   - Update key rotation log
   - Archive old key ARN for audit trail
   - Schedule old key deletion (30-day waiting period)

---

### SOP-006: Incident Response - Unauthorized Access Attempt

**Frequency**: As needed (security incident)

**Steps**:

1. **Detect Unauthorized Access**
   - CloudTrail alerts on S3 bucket access
   - Failed decryption attempts logged

2. **Isolate Affected Resources**
   ```bash
   # Revoke S3 bucket access temporarily
   aws s3api put-bucket-policy \
     --bucket haven-prod-exports \
     --policy file://deny-all-policy.json
   ```

3. **Review Access Logs**
   ```bash
   # Query CloudTrail for access events
   aws cloudtrail lookup-events \
     --lookup-attributes AttributeKey=ResourceName,AttributeValue=haven-prod-exports \
     --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
     --max-results 100
   ```

4. **Identify Compromised Exports**
   - List all exports accessed during incident window
   - Query consent ledger for affected data subjects

5. **Rotate Encryption Keys**
   - Follow SOP-005 to rotate KMS master key
   - Re-encrypt affected exports if needed

6. **Notify Affected Parties**
   - If PII accessed: Notify compliance team
   - Determine if breach notification required (HIPAA/HITECH)
   - Prepare incident report for stakeholders

7. **Restore Normal Operations**
   - Restore S3 bucket policy
   - Verify exports can be created/accessed
   - Monitor for continued unauthorized activity

8. **Post-Incident Review**
   - Document timeline of events
   - Identify root cause
   - Implement corrective actions
   - Update security policies

---

## Troubleshooting Guide

### Export Job Stuck in MATERIALIZING State

**Symptoms**: Job remains in MATERIALIZING for >30 minutes

**Diagnosis**:
```bash
# Check async executor thread pool
GET /actuator/metrics/executor.active
GET /actuator/metrics/executor.queued

# Review application logs
tail -f /var/log/haven/application.log | grep "ExportJobOrchestration"
```

**Resolution**:
1. Check database connectivity
2. Verify view generator queries complete
3. Increase thread pool size if needed:
   ```yaml
   haven:
     async:
       core-pool-size: 10
       max-pool-size: 20
   ```

---

### Encryption Failures (KMS Errors)

**Symptoms**: Export fails with `KMS_ENCRYPTION_ERROR`

**Diagnosis**:
```bash
# Check KMS key status
aws kms describe-key --key-id {key-arn}

# Review IAM permissions
aws iam get-role-policy --role-name HavenExportRole --policy-name KmsAccess
```

**Resolution**:
1. Verify KMS key is enabled
2. Check IAM role has `kms:Decrypt` and `kms:GenerateDataKey` permissions
3. Review KMS key policy for trust relationships
4. Increase KMS request quota if throttled

---

### Consent Ledger API Unavailable

**Symptoms**: Export completes but ledger entry fails

**Diagnosis**:
```bash
# Test compliance API connectivity
curl -H "X-API-Key: {key}" https://compliance.haven.internal/health

# Check API logs
kubectl logs -n compliance svc/compliance-api --tail=100
```

**Resolution**:
1. Verify network connectivity to compliance service
2. Check API key validity (rotate if expired)
3. Review compliance service health dashboard
4. If API down: Export still completes, ledger entry queued for retry

---

### Notification Delivery Failures

**Symptoms**: Export completes but no email received

**Diagnosis**:
```bash
# Check mail sender metrics
GET /actuator/metrics/mail.sent

# Review SMTP logs
tail -f /var/log/haven/smtp.log
```

**Resolution**:
1. Verify SMTP server connectivity
2. Check email addresses in configuration
3. Review spam filters (whitelist noreply@haven.example.com)
4. Test email delivery:
   ```bash
   POST /api/admin/notifications/test
   {
     "recipients": ["admin@haven.org"],
     "subject": "Test notification"
   }
   ```

---

## Configuration Reference

### Application Properties

```yaml
haven:
  # KMS Configuration
  kms:
    key-id: arn:aws:kms:us-west-2:ACCOUNT:key/export-master-key
    provider: aws  # aws | azure | local
    master-key: ${KMS_MASTER_KEY_HEX}  # For local provider only

  # Compliance API
  compliance:
    api:
      url: https://compliance.haven.internal/api/v1
      key: ${COMPLIANCE_API_KEY}  # From Secrets Manager
    ledger:
      enabled: true

  # Notification
  notification:
    enabled: true
    from-email: noreply@haven.org
    compliance-admins:
      - compliance@haven.org
      - datagovernance@haven.org

  # Storage
  reporting:
    storage:
      base-path: s3://haven-prod-exports
      retention-days: 90

  # Async Processing
  async:
    core-pool-size: 5
    max-pool-size: 10
    queue-capacity: 100
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-01-07 | Initial runbook - Orchestration, encryption, consent ledger, notifications |

---

## References

- [CSV Validation Guardrails](CSV_VALIDATION_GUARDRAILS.md)
- [HUD HMIS Data Standards 2024](https://hudexchange.info)
- [AWS KMS Best Practices](https://docs.aws.amazon.com/kms/latest/developerguide/best-practices.html)
- [HIPAA Security Rule](https://www.hhs.gov/hipaa/for-professionals/security/index.html)
