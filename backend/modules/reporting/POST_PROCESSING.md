# HUD Export Post-Processing & Packaging

## Overview

This module implements secure, compliant post-processing for HUD HMIS exports, including:

1. **Format Conversion** - CSV, XML, Excel per HUD specifications
2. **Aggregation & Suppression** - CoC APR and SPM metrics with privacy protections
3. **Secure Packaging** - ZIP archives with tamper-evident manifests
4. **Encryption** - Optional AES-256-GCM for VAWA-protected data

## Architecture

### Components

```
ExportJobApplicationServiceV2
├── HUDExportViewGenerator (materialization)
├── AggregationService (metrics computation)
├── HUDExportFormatter (format conversion)
│   ├── CSVExportStrategy
│   ├── XMLExportStrategy
│   └── ExcelExportStrategy
├── ExportPackagingService (ZIP + manifest + signature)
└── ExportAuditMetadataV2 (integrity tracking)
```

### Pipeline Phases

1. **Materialization** - Generate normalized data sections from event store
2. **Aggregation** - Compute APR/SPM metrics with n<5 suppression
3. **Formatting** - Convert to CSV/XML/Excel per HUD specs
4. **Validation** - Check required files, schema compliance
5. **Packaging** - Create signed ZIP with manifest
6. **Storage** - Persist to blob storage
7. **Audit** - Record signature for integrity verification

## Format Strategies

### CSV (Default)

**Specification:** RFC 4180 + HUD HMIS CSV 2024

- UTF-8 with BOM (`0xEF 0xBB 0xBF`)
- CRLF line endings (`\r\n`)
- HUD-mandated column order (see `CSVExportStrategy.HUD_COLUMN_ORDER`)
- Quoted fields containing `,`, `"`, or newlines
- Double-escaped internal quotes (`"" -> ""`)

**Example:**
```csv
PersonalID,FirstName,LastName,DOB,...
"12345","Jane","Doe","1985-03-15",...
```

### XML

**Specification:** HUD HMIS XSD Schema (HUD_HMIS.xsd)

- Namespace: `https://www.hudhdx.info/Resources/Vendors/HMIS/HUD_HMIS.xsd`
- Schema version: 2024
- Indented formatting (2 spaces)
- UTF-8 encoding

**Example:**
```xml
<HMISExport xmlns="..." version="2024">
  <Clients>
    <Client>
      <PersonalID>12345</PersonalID>
      <FirstName>Jane</FirstName>
      ...
    </Client>
  </Clients>
</HMISExport>
```

### Excel

**Specification:** Apache POI XSSF Workbook

- Separate sheet per section (Client, Enrollment, etc.)
- Formatted headers (bold, gray background, border)
- Date cells with `yyyy-mm-dd` format
- Auto-sized columns

## Aggregation & Suppression

### Cell Suppression Rule

**HUD Privacy Guidance:** Suppress cells where `1 <= n < 5`

```java
private Object suppressIfNeeded(int count) {
    if (count > 0 && count < 5) {
        return "*";  // Masked value
    }
    return count;
}
```

### CoC APR Metrics

#### Q6: Household Type
```sql
SELECT
  household_type,  -- Adults Only, Adults and Children, Children Only
  COUNT(DISTINCT household_id) as count
FROM enrollments
GROUP BY household_type
```

#### Q7: Veteran Status
```sql
SELECT
  veteran_status,
  COUNT(DISTINCT personal_id) as count
WHERE age >= 18
GROUP BY veteran_status
```

#### Q10: Income Sources
Tracks income sources at entry, annual, and exit:
- Earned, Unemployment, SSI, SSDI
- VA Disability (service/non-service)
- TANF, GA, Pension, Child Support, etc.

### SPM Metrics

#### Metric 1: Returns to Homelessness (2-year lookback)

Percentage of persons exiting to permanent housing who return within:
- 6 months
- 12 months
- 24 months

**Return Definition:** Subsequent enrollment in ES, TH, SO, or Safe Haven project

#### Metric 7: Successful Placements (365-day outcomes)

Percentage of persons who:
1. Moved into permanent housing (PSH, RRH, PH)
2. Maintained housing for ≥365 days OR
3. Exited to permanent destination

## Packaging

### Manifest Structure

```json
{
  "exportJobId": "uuid",
  "generatedAt": "2024-01-15T10:30:00Z",
  "files": {
    "Client.csv": "sha256_hash_1",
    "Enrollment.csv": "sha256_hash_2",
    ...
  },
  "format": "CSV",
  "encrypted": false
}
```

### Digital Signature

**Algorithm:** HMAC-SHA256

```java
Mac mac = Mac.getInstance("HmacSHA256");
SecretKeySpec key = new SecretKeySpec(signingKey, "HmacSHA256");
mac.init(key);
byte[] signature = mac.doFinal(manifestJson.getBytes());
```

**Configuration:**
```yaml
haven:
  export:
    signing-key: ${EXPORT_SIGNING_KEY}  # 64-char hex string
```

### Encryption (Optional)

**Algorithm:** AES-256-GCM

- 12-byte random IV per package
- 128-bit authentication tag
- IV prepended to ciphertext
- Key derived from signing key (first 32 bytes)

**When to Encrypt:**
- VAWA-protected exports
- Exports containing SSN, DOB, or PII
- External delivery (email, FTP)

## Usage

### Basic Export (CSV)

```java
UUID jobId = exportService.requestExport(
    "HMIS_CSV",
    LocalDate.of(2024, 1, 1),
    LocalDate.of(2024, 12, 31),
    projectIds,
    "CA-500",
    "Annual HUD submission",
    ExportFormat.CSV,
    false,  // not encrypted
    accessContext
);
```

### Encrypted APR Export

```java
UUID jobId = exportService.requestExport(
    "CoC_APR",
    reportStart,
    reportEnd,
    projectIds,
    cocCode,
    "CoC APR with aggregations",
    ExportFormat.EXCEL,
    true,  // encrypted
    accessContext
);
```

### Verification

```java
// Download package
ExportPackage pkg = downloadExport(jobId);

// Verify manifest hash
boolean valid = pkg.verifyIntegrity(expectedHash);

// Verify signature
ExportPackagingService svc = new ExportPackagingService(...);
boolean signatureValid = svc.verifySignature(
    manifestBytes,
    pkg.digitalSignature()
);

// Decrypt if needed
if (pkg.metadata().encrypted()) {
    byte[] decrypted = svc.decryptData(pkg.zipArchive());
}
```

## Security Considerations

### Key Management

**DO NOT** hardcode signing keys in source code!

**Recommended:**
- Store in secret manager (AWS Secrets Manager, Azure Key Vault)
- Rotate keys quarterly
- Use different keys per environment (dev/staging/prod)

### Audit Trail

Every export records:
- Who requested (user ID, IP, session)
- What data (projects, date range)
- When generated
- Manifest hash + signature
- Encryption status

Query audit logs:
```sql
SELECT
  export_job_id,
  requested_by_user_name,
  digital_signature,
  encrypted,
  generated_at
FROM export_audit_metadata_v2
WHERE reporting_period_start >= '2024-01-01'
ORDER BY generated_at DESC;
```

### Tamper Detection

On download, verify:
1. Manifest hash matches stored value
2. File hashes match manifest
3. Digital signature valid

If any check fails: **REJECT EXPORT** and alert security team.

## HUD Compliance

### Data Standards

- **HMIS CSV 2024 Specification** - Column order, data types, code lists
- **HUD HMIS XML Schema** - XSD validation for XML exports
- **24 CFR Part 578** - CoC Program interim rule requirements

### Privacy (VAWA)

- Cell suppression (n<5) for protected categories
- Service-level redaction for DV providers
- Encrypted delivery for exports containing PII

### Retention

Default: 7 years per HUD guidance

```sql
UPDATE export_audit_metadata_v2
SET expires_at = generated_at + INTERVAL '7 years';
```

## Troubleshooting

### "Missing required file: Client.csv"

Ensure `Client` section is materialized by `HUDExportViewGenerator`.

### "Failed to verify signature"

Check:
1. Signing key matches key used during generation
2. Manifest JSON not modified
3. Encoding is UTF-8 (not UTF-16 or Latin-1)

### "Decryption failed"

- IV length must be 12 bytes
- GCM tag length must be 128 bits
- Ensure same key used for encryption/decryption

## Future Enhancements

- [ ] XML schema validation against HUD_HMIS.xsd
- [ ] Incremental exports (delta since last export)
- [ ] Multi-CoC exports with data segregation
- [ ] SFTP delivery integration
- [ ] Real-time export status websocket
