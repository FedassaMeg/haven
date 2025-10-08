# Privileged Action Auditing - SIEM Configuration Guide

## Overview

This guide documents the privileged action auditing system for Haven HMIS, including SIEM integration, log routing, alerting configuration, and compliance retention requirements.

## System Architecture

### Components

1. **PrivilegedAuditService** - Core service for audit event emission
2. **Logback Configuration** - Structured JSON logging with SIEM tags
3. **Log Collectors** - File-based sidecar collection (Filebeat, Fluentd, etc.)
4. **SIEM Platform** - Security monitoring and alerting (Splunk, ELK, etc.)
5. **Database Persistence** - Long-term audit trail storage (PostgreSQL)

### Data Flow

```
Application Layer
    ↓
PrivilegedAuditService
    ↓
├─→ Structured JSON Log (Logback)
│   ├─→ privileged-audit.log (long-term storage)
│   └─→ siem/pii-audit.jsonl (sidecar collection)
│       ↓
│       Log Collector (Filebeat/Fluentd)
│       ↓
│       SIEM Platform (Splunk/ELK/etc.)
│
└─→ Database Audit Log Table (PostgreSQL)
    ↓
    Compliance Queries & Reporting
```

## Privileged Actions Instrumented

### DV Note Operations (CRITICAL)
- `DV_NOTE_READ` - Reading restricted/DV notes
- `DV_NOTE_WRITE` - Creating/updating restricted/DV notes
- `DV_NOTE_SEAL` - Sealing notes (court orders, etc.)
- `DV_NOTE_UNSEAL` - Unsealing previously sealed notes
- `DV_NOTE_DELETE` - Deleting restricted notes
- `DV_NOTE_ACCESS_LIST_MODIFIED` - Changing note ACLs

**SIEM Tag**: `pii_audit:dv_note`

### Export Operations (HIGH/CRITICAL)
- `EXPORT_INITIATED` - Export job initiated
- `EXPORT_COMPLETED` - Export job completed successfully
- `EXPORT_FAILED` - Export job failed
- `EXPORT_DOWNLOADED` - Export file downloaded by user
- `EXPORT_PURGED` - Export file purged (retention)

**SIEM Tag**: `pii_audit:export`

### Consent Ledger Operations (HIGH/CRITICAL)
- `CONSENT_LEDGER_ENTRY_CREATED` - New consent ledger entry
- `CONSENT_LEDGER_ENTRY_MODIFIED` - Ledger entry modification
- `CONSENT_OVERRIDE_ATTEMPTED` - Consent override attempted
- `CONSENT_OVERRIDE_GRANTED` - Consent override granted
- `CONSENT_OVERRIDE_DENIED` - Consent override denied

**SIEM Tag**: `pii_audit:consent`

### Ledger Adjustment Operations (HIGH/CRITICAL)
- `LEDGER_ADJUSTMENT_CREATED` - New ledger adjustment
- `LEDGER_ADJUSTMENT_APPROVED` - Adjustment approved
- `LEDGER_ADJUSTMENT_REJECTED` - Adjustment rejected
- `LEDGER_ADJUSTMENT_REVERTED` - Adjustment reverted

**SIEM Tag**: `pii_audit:ledger`

### Policy Decision Operations (CRITICAL)
- `POLICY_DECISION_OVERRIDE` - Security policy overridden
- `POLICY_DECISION_ESCALATION` - Policy decision escalated

**SIEM Tag**: `pii_audit:policy`

### PII/VSP Operations (CRITICAL)
- `PII_EXPORT_FULL_SSN` - PII export with full SSN
- `PII_REDACTION_OVERRIDE` - PII redaction override
- `VSP_AUDIT_LOG_ACCESSED` - VSP audit log accessed

**SIEM Tag**: `pii_audit:pii`

### Administrative Operations (HIGH)
- `ADMIN_ROLE_ASSIGNED` - Admin role assignment
- `ADMIN_ROLE_REVOKED` - Admin role revocation
- `SECURITY_CONFIG_MODIFIED` - Security config change
- `AUDIT_LOG_ACCESSED` - Audit log query
- `AUDIT_LOG_EXPORT_REQUESTED` - Audit log export

**SIEM Tag**: `pii_audit:admin`

## Audit Event Schema

### Standard Fields

```json
{
  "eventId": "uuid",
  "eventType": "PRIVILEGED_ACTION_TYPE",
  "outcome": "SUCCESS|DENIED_*|ERROR_*",
  "timestamp": "ISO-8601 UTC timestamp",
  "actorId": "uuid",
  "actorUsername": "string",
  "actorRoles": ["role1", "role2"],
  "resourceType": "string",
  "resourceId": "uuid",
  "resourceDescription": "string",
  "consentLedgerId": "string (optional)",
  "justification": "string (required for privileged actions)",
  "hashFingerprint": "SHA-256 hash (for exports)",
  "denialReason": "string (for denials)",
  "denialDetails": "string (for denials)",
  "requestId": "correlation ID",
  "sessionId": "session identifier",
  "ipAddress": "IPv4/IPv6 address",
  "userAgent": "client user agent",
  "metadata": {
    "key": "value"
  }
}
```

### Example: DV Note Read Event

```json
{
  "@timestamp": "2024-10-07T14:32:15.123Z",
  "eventId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
  "eventType": "DV_NOTE_READ",
  "outcome": "SUCCESS",
  "actorId": "12345678-1234-1234-1234-123456789012",
  "actorUsername": "jane.advocate",
  "actorRoles": ["DV_ADVOCATE", "CASE_MANAGER"],
  "resourceType": "RestrictedNote",
  "resourceId": "98765432-9876-9876-9876-987654321098",
  "resourceDescription": "Safety Planning Note",
  "justification": "Case review for weekly safety check-in",
  "requestId": "req-abc123",
  "sessionId": "session-xyz789",
  "ipAddress": "192.168.1.100",
  "metadata": {
    "noteType": "SAFETY_PLAN",
    "clientId": "11111111-1111-1111-1111-111111111111",
    "isSealed": false
  },
  "application": "haven",
  "log_type": "privileged_audit",
  "siem_routing": "pii_audit",
  "siemTag": "pii_audit:dv_note"
}
```

## Logback Configuration

### File Locations

1. **Long-term Storage**: `logs/privileged-audit.log`
   - Daily rollover
   - 7-year retention (SOX compliance)
   - JSON format
   - 100GB total cap

2. **SIEM Sidecar Collection**: `logs/siem/pii-audit.jsonl`
   - Hourly rollover
   - 30-day retention (for collection window)
   - JSON Lines format
   - 50GB total cap

### Configuration File

Include in `logback-spring.xml`:

```xml
<include resource="logback-privileged-audit.xml"/>
```

### Environment Variables

- `LOG_PATH` - Base directory for logs (default: `logs`)
- `ENVIRONMENT` - Deployment environment (dev/staging/prod)

## SIEM Platform Configuration

### Splunk Configuration

#### 1. Create Index

```spl
[haven_privileged_audit]
coldPath = $SPLUNK_DB/haven_privileged_audit/colddb
homePath = $SPLUNK_DB/haven_privileged_audit/db
thawedPath = $SPLUNK_DB/haven_privileged_audit/thaweddb
maxDataSize = auto_high_volume
frozenTimePeriodInSecs = 220752000
# 7 years retention for SOX compliance
```

#### 2. Configure Input

```conf
[monitor:///opt/haven/logs/siem/pii-audit*.jsonl]
disabled = false
index = haven_privileged_audit
sourcetype = haven:privileged_audit:json
```

#### 3. Define Source Type

```conf
[haven:privileged_audit:json]
INDEXED_EXTRACTIONS = json
KV_MODE = json
TIME_PREFIX = "@timestamp"\s*:\s*"
TIME_FORMAT = %Y-%m-%dT%H:%M:%S.%3NZ
MAX_TIMESTAMP_LOOKAHEAD = 32
SHOULD_LINEMERGE = false
LINE_BREAKER = ([\r\n]+)
TRUNCATE = 100000
```

### ELK Stack Configuration

#### 1. Filebeat Configuration

```yaml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /opt/haven/logs/siem/pii-audit-*.jsonl
    json.keys_under_root: true
    json.add_error_key: true
    fields:
      log_source: haven_privileged_audit
    tags: ["pii_audit", "privileged_action"]

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "haven-privileged-audit-%{+yyyy.MM.dd}"
  ilm.enabled: false
```

#### 2. Elasticsearch Index Template

```json
{
  "index_patterns": ["haven-privileged-audit-*"],
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2,
    "index.lifecycle.name": "haven-audit-retention",
    "index.lifecycle.rollover_alias": "haven-privileged-audit"
  },
  "mappings": {
    "properties": {
      "@timestamp": { "type": "date" },
      "eventId": { "type": "keyword" },
      "eventType": { "type": "keyword" },
      "outcome": { "type": "keyword" },
      "actorId": { "type": "keyword" },
      "actorUsername": { "type": "keyword" },
      "actorRoles": { "type": "keyword" },
      "resourceType": { "type": "keyword" },
      "resourceId": { "type": "keyword" },
      "consentLedgerId": { "type": "keyword" },
      "hashFingerprint": { "type": "keyword" },
      "denialReason": { "type": "keyword" },
      "siemTag": { "type": "keyword" }
    }
  }
}
```

#### 3. ILM Policy (7-year retention)

```json
{
  "policy": "haven-audit-retention",
  "phases": {
    "hot": {
      "actions": {
        "rollover": {
          "max_size": "50GB",
          "max_age": "30d"
        }
      }
    },
    "warm": {
      "min_age": "90d",
      "actions": {
        "shrink": {
          "number_of_shards": 1
        },
        "forcemerge": {
          "max_num_segments": 1
        }
      }
    },
    "cold": {
      "min_age": "365d",
      "actions": {
        "freeze": {}
      }
    },
    "delete": {
      "min_age": "2555d",
      "actions": {
        "delete": {}
      }
    }
  }
}
```

## SIEM Dashboards

### 1. Privileged Action Overview Dashboard

**Panels**:
- Total privileged actions (24h)
- Privileged actions by type (pie chart)
- Privileged actions timeline (line chart)
- Top 10 actors by action count
- Denial rate (gauge)
- Failed export attempts (table)

**Splunk Query**:
```spl
index=haven_privileged_audit
| stats count by eventType outcome
| sort -count
```

### 2. DV Note Access Monitoring

**Panels**:
- DV note access timeline
- Access by user role
- Sealed note access attempts
- Denied access reasons

**Splunk Query**:
```spl
index=haven_privileged_audit siemTag="pii_audit:dv_note"
| timechart count by eventType
```

### 3. Export Audit Trail

**Panels**:
- Export initiations vs completions
- Export hash fingerprints (integrity)
- Consent ledger cross-references
- Failed exports with reasons

**Splunk Query**:
```spl
index=haven_privileged_audit siemTag="pii_audit:export"
| transaction exportJobId
| table _time actorUsername eventType outcome hashFingerprint consentLedgerId
```

## SIEM Alerts

### Critical Alerts (Immediate Response)

#### 1. Repeated Access Denials (Brute Force Detection)

```spl
index=haven_privileged_audit outcome=DENIED_*
| stats count by actorId actorUsername resourceId
| where count > 5
```

**Trigger**: More than 5 denials for same resource by same user in 15 minutes
**Severity**: High
**Action**: Alert SOC, suspend account, investigate

#### 2. Unauthorized DV Note Access Attempt

```spl
index=haven_privileged_audit
  siemTag="pii_audit:dv_note"
  outcome=DENIED_*
| table _time actorUsername resourceId denialReason
```

**Trigger**: Any denied DV note access
**Severity**: Critical
**Action**: Alert compliance officer, investigate immediately

#### 3. Export Download from Unusual Location

```spl
index=haven_privileged_audit
  eventType=EXPORT_DOWNLOADED
| iplocation ipAddress
| where Country!="United States"
```

**Trigger**: Export download from non-US IP
**Severity**: Critical
**Action**: Alert SOC, investigate data exfiltration

#### 4. Consent Override Granted

```spl
index=haven_privileged_audit
  eventType=CONSENT_OVERRIDE_GRANTED
```

**Trigger**: Any consent override
**Severity**: High
**Action**: Alert compliance officer, verify justification

### Warning Alerts (Review Required)

#### 5. Multiple Failed Exports

```spl
index=haven_privileged_audit
  eventType=EXPORT_FAILED
| stats count by actorId
| where count > 3
```

**Trigger**: More than 3 failed exports by same user in 24 hours
**Severity**: Medium
**Action**: Alert data steward, check for system issues

#### 6. After-Hours DV Note Access

```spl
index=haven_privileged_audit
  siemTag="pii_audit:dv_note"
| eval hour=strftime(_time, "%H")
| where hour < 6 OR hour > 22
```

**Trigger**: DV note access between 10 PM - 6 AM
**Severity**: Medium
**Action**: Review next business day, verify legitimacy

#### 7. Sealed Note Access

```spl
index=haven_privileged_audit
  eventType=DV_NOTE_READ
  metadata.isSealed=true
```

**Trigger**: Any sealed note access
**Severity**: Medium
**Action**: Alert compliance officer, verify court authorization

## Compliance Retention

### SOX Compliance (Financial Data)
- **Requirement**: 7 years
- **Applies To**: Export operations, ledger adjustments
- **Storage**: Database + archival logs
- **Verification**: Annual attestation

### HIPAA Compliance (Health Data)
- **Requirement**: 6 years
- **Applies To**: DV note access, PII operations
- **Storage**: Database + SIEM
- **Verification**: Quarterly audit

### VAWA Compliance (Victim Protection)
- **Requirement**: Indefinite (best practice: 10 years)
- **Applies To**: DV note operations, consent overrides
- **Storage**: Database + immutable archival
- **Verification**: Continuous monitoring

## Security Operations Runbook

### Incident Response: Unauthorized Access Attempt

1. **Detection**: SIEM alert fires for denied access
2. **Initial Triage**:
   - Query audit logs for user's recent activity
   - Check if pattern indicates credential compromise
   - Verify user's current role assignments
3. **Containment**:
   - Suspend user account if suspicious
   - Revoke active sessions
   - Alert user's supervisor
4. **Investigation**:
   - Interview user about access attempt
   - Review resource access logs
   - Check for lateral movement
5. **Resolution**:
   - Re-enable account if legitimate
   - Update access policies if needed
   - Document incident

### Incident Response: Data Exfiltration Suspected

1. **Detection**: Export download from unusual location
2. **Immediate Actions**:
   - Suspend user account
   - Block IP address at firewall
   - Alert CISO and legal team
3. **Forensics**:
   - Retrieve export file hash from audit log
   - Check consent ledger for data subject list
   - Review all recent exports by user
   - Collect network logs for correlation
4. **Notification**:
   - Determine if breach notification required
   - Notify affected data subjects per GDPR/HIPAA
   - Report to HUD if CoC data compromised
5. **Recovery**:
   - Rotate all credentials
   - Review and update security policies
   - Conduct security awareness training

## Testing and Validation

### Pre-Deployment Checklist

- [ ] Verify all privileged endpoints emit audit events
- [ ] Confirm JSON logs are well-formed and parseable
- [ ] Test log rotation and retention policies
- [ ] Validate SIEM ingestion pipeline
- [ ] Configure all critical alerts
- [ ] Set up dashboards for monitoring
- [ ] Document incident response procedures
- [ ] Train security ops team on new audit system

### Audit Emission Test Cases

1. **DV Note Read - Success**
   - Actor with valid permissions reads DV note
   - Verify audit event logged with justification
   - Confirm SIEM tag `pii_audit:dv_note`

2. **DV Note Read - Denied**
   - Actor without permissions attempts read
   - Verify denial event with reason code
   - Confirm alert fires for unauthorized access

3. **Export Completed - With Hash**
   - Export job completes successfully
   - Verify audit event includes SHA-256 hash
   - Confirm consent ledger ID cross-reference

4. **Consent Override - Granted**
   - Administrator overrides consent restriction
   - Verify critical severity event logged
   - Confirm alert fires for compliance review

### SIEM Query Validation

```spl
# Test 1: Verify all event types ingested
index=haven_privileged_audit
| stats count by eventType
| where count=0

# Test 2: Verify required fields present
index=haven_privileged_audit
| search NOT eventId=* OR NOT actorId=* OR NOT outcome=*

# Test 3: Verify SIEM tags correct
index=haven_privileged_audit
| stats count by siemTag
| search NOT siemTag=pii_audit:*
```

## Performance Considerations

### Logging Throughput
- **Expected Load**: 100-500 events/minute
- **Peak Load**: 2000 events/minute (export operations)
- **Async Queue Size**: 512 events
- **Discarding Threshold**: 0 (never discard audit events)

### Database Impact
- **Write Load**: Moderate (append-only inserts)
- **Indexing**: Required on audit_id, user_id, resource_id, timestamp
- **Partitioning**: Recommended by month for tables > 10M rows
- **Archival**: Cold storage for data > 2 years old

### SIEM Ingestion
- **Log Volume**: ~1-5 MB/hour (normal operations)
- **Peak Volume**: ~50 MB/hour (during bulk exports)
- **Retention**: 7 years = ~300 GB total
- **Compression**: JSON log compression recommended

## Troubleshooting

### Issue: Audit events not appearing in SIEM

**Diagnosis**:
```bash
# Check log files exist and are being written
tail -f /opt/haven/logs/siem/pii-audit.jsonl

# Verify log rotation
ls -lh /opt/haven/logs/siem/

# Check Filebeat is running and collecting
systemctl status filebeat
tail -f /var/log/filebeat/filebeat
```

**Resolution**:
- Verify log path in Filebeat configuration
- Check file permissions (Filebeat user needs read access)
- Ensure JSON format is valid (`jq . < pii-audit.jsonl`)

### Issue: High audit logging latency

**Diagnosis**:
```spl
index=haven_privileged_audit
| eval latency=_indextime-_time
| stats avg(latency) max(latency) by host
```

**Resolution**:
- Increase async queue size in logback config
- Check database connection pool saturation
- Review SIEM ingestion pipeline bottlenecks

### Issue: Missing justification fields

**Diagnosis**:
```spl
index=haven_privileged_audit
  eventType IN (DV_NOTE_SEAL, EXPORT_INITIATED, CONSENT_OVERRIDE_GRANTED)
| search NOT justification=*
```

**Resolution**:
- Review code instrumentation for mandatory justification
- Update validation rules in PrivilegedAuditEvent.Builder
- Add integration tests for justification enforcement

## Contact Information

- **Security Operations**: soc@haven-hmis.org
- **Compliance Officer**: compliance@haven-hmis.org
- **SIEM Administrator**: siem-admin@haven-hmis.org
- **On-Call Engineer**: PagerDuty escalation

## References

- [NIST SP 800-92: Guide to Computer Security Log Management](https://csrc.nist.gov/publications/detail/sp/800-92/final)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [HUD HMIS Data Standards](https://www.hudexchange.info/programs/hmis/hmis-data-standards/)
- [VAWA Confidentiality Requirements](https://www.hudexchange.info/programs/vawa/)
