package org.haven.reporting.application.services;

import org.haven.reporting.domain.ExportConsentScope;
import org.haven.reporting.domain.ExportHashBehavior;
import org.haven.shared.audit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for emitting consent ledger entries to compliance microservice.
 *
 * The consent ledger tracks:
 * - Data subjects included in export (anonymized client identifiers)
 * - Consent scope (full export vs. limited data elements)
 * - Export hash mode (SSN hashing behavior per HUD specs)
 * - Retention window (when export must be purged)
 * - Export job metadata for audit trail
 *
 * Integrates with:
 * - Compliance microservice REST API
 * - Data governance platform
 * - Audit log aggregation
 *
 * Supports both synchronous and asynchronous emission modes.
 */
@Service
public class ConsentLedgerService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentLedgerService.class);

    private final RestTemplate restTemplate;
    private final String complianceApiUrl;
    private final String complianceApiKey;
    private final boolean enabled;
    private final int defaultRetentionDays;
    private final PrivilegedAuditService privilegedAuditService;

    public ConsentLedgerService(
            RestTemplate restTemplate,
            @Value("${haven.compliance.api.url:http://localhost:8081/compliance}") String complianceApiUrl,
            @Value("${haven.compliance.api.key:}") String complianceApiKey,
            @Value("${haven.compliance.ledger.enabled:true}") boolean enabled,
            @Value("${haven.reporting.storage.retention-days:90}") int defaultRetentionDays,
            PrivilegedAuditService privilegedAuditService) {

        this.restTemplate = restTemplate;
        this.complianceApiUrl = complianceApiUrl;
        this.complianceApiKey = complianceApiKey;
        this.enabled = enabled;
        this.defaultRetentionDays = defaultRetentionDays;
        this.privilegedAuditService = privilegedAuditService;

        logger.info("Consent Ledger Service initialized - Enabled: {}, API: {}, Retention: {} days",
                enabled, complianceApiUrl, defaultRetentionDays);
    }

    /**
     * Emit consent ledger entry for export job.
     *
     * @param entry Consent ledger entry details
     * @return Ledger entry ID from compliance system
     */
    public String emitLedgerEntry(ConsentLedgerEntry entry) {
        if (!enabled) {
            logger.debug("Consent ledger disabled - skipping entry for export job: {}", entry.exportJobId());
            return "DISABLED-" + UUID.randomUUID();
        }

        try {
            logger.info("Emitting consent ledger entry for export job: {} with {} data subjects",
                    entry.exportJobId(), entry.dataSubjects().size());

            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (complianceApiKey != null && !complianceApiKey.isEmpty()) {
                headers.set("X-API-Key", complianceApiKey);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("exportJobId", entry.exportJobId().toString());
            requestBody.put("dataSubjects", entry.dataSubjects());
            requestBody.put("dataSubjectCount", entry.dataSubjects().size());
            requestBody.put("consentScope", entry.consentScope().name());
            requestBody.put("consentScopeDescription", entry.consentScope().getDescription());
            requestBody.put("exportHashMode", entry.exportHashMode().name());
            requestBody.put("exportHashDescription", entry.exportHashMode().getDescription());
            requestBody.put("retentionWindow", entry.retentionWindow().toString());
            requestBody.put("retentionDays", ChronoUnit.DAYS.between(Instant.now(), entry.retentionWindow()));
            requestBody.put("exportPeriodStart", entry.exportPeriodStart().toString());
            requestBody.put("exportPeriodEnd", entry.exportPeriodEnd().toString());
            requestBody.put("exportType", entry.exportType());
            requestBody.put("exportReason", entry.exportReason());
            requestBody.put("requestedBy", entry.requestedBy());
            requestBody.put("exportedAt", entry.exportedAt().toString());
            requestBody.put("storageLocation", entry.storageLocation());
            requestBody.put("exportSha256Hash", entry.exportSha256Hash());
            requestBody.put("encrypted", entry.encrypted());
            requestBody.put("kmsKeyId", entry.kmsKeyId());
            requestBody.put("vawaProtected", entry.vawaProtected());
            requestBody.put("vawaSuppressedRecords", entry.vawaSuppressedRecords());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call compliance API
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    complianceApiUrl + "/ledger/entries",
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                String ledgerEntryId = (String) response.getBody().get("ledgerEntryId");
                logger.info("Consent ledger entry created: {} for export job: {}",
                        ledgerEntryId, entry.exportJobId());

                // Emit privileged audit event for consent ledger entry creation
                privilegedAuditService.logAction(
                    PrivilegedAuditEvent.builder()
                        .eventType(PrivilegedActionType.CONSENT_LEDGER_ENTRY_CREATED)
                        .outcome(AuditOutcome.SUCCESS)
                        .actorId(UUID.randomUUID()) // Would extract from context in production
                        .actorUsername(entry.requestedBy())
                        .actorRoles(List.of("DATA_STEWARD"))
                        .resourceType("ConsentLedger")
                        .resourceId(entry.exportJobId())
                        .resourceDescription("Consent ledger entry for " + entry.exportType() + " export")
                        .consentLedgerId(ledgerEntryId)
                        .hashFingerprint(entry.exportSha256Hash())
                        .justification(entry.exportReason())
                        .addMetadata("dataSubjectCount", entry.dataSubjects().size())
                        .addMetadata("consentScope", entry.consentScope().name())
                        .addMetadata("exportHashMode", entry.exportHashMode().name())
                        .addMetadata("retentionDays", ChronoUnit.DAYS.between(Instant.now(), entry.retentionWindow()))
                        .addMetadata("vawaProtected", entry.vawaProtected())
                        .build()
                );

                return ledgerEntryId;
            } else {
                throw new ConsentLedgerException("Unexpected response from compliance API: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Failed to emit consent ledger entry for export job: {}", entry.exportJobId(), e);
            throw new ConsentLedgerException("Failed to emit consent ledger entry", e);
        }
    }

    /**
     * Emit ledger entry asynchronously.
     */
    public void emitLedgerEntryAsync(ConsentLedgerEntry entry) {
        // In production, use @Async or message queue
        new Thread(() -> {
            try {
                emitLedgerEntry(entry);
            } catch (Exception e) {
                logger.error("Async consent ledger emission failed for export job: {}",
                        entry.exportJobId(), e);
            }
        }).start();
    }

    /**
     * Query ledger entries for a data subject.
     *
     * @param dataSubjectId Anonymized client identifier
     * @return List of ledger entries
     */
    public List<ConsentLedgerEntry> queryByDataSubject(String dataSubjectId) {
        if (!enabled) {
            return List.of();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (complianceApiKey != null && !complianceApiKey.isEmpty()) {
                headers.set("X-API-Key", complianceApiKey);
            }

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    complianceApiUrl + "/ledger/entries/data-subject/" + dataSubjectId,
                    HttpMethod.GET,
                    request,
                    List.class
            );

            logger.info("Retrieved {} ledger entries for data subject: {}",
                    response.getBody().size(), dataSubjectId);

            return (List<ConsentLedgerEntry>) response.getBody();

        } catch (Exception e) {
            logger.error("Failed to query ledger entries for data subject: {}", dataSubjectId, e);
            return List.of();
        }
    }

    /**
     * Mark export as purged in ledger (retention window expired).
     *
     * @param exportJobId Export job identifier
     */
    public void markExportPurged(UUID exportJobId) {
        if (!enabled) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            if (complianceApiKey != null && !complianceApiKey.isEmpty()) {
                headers.set("X-API-Key", complianceApiKey);
            }

            Map<String, Object> requestBody = Map.of(
                    "exportJobId", exportJobId.toString(),
                    "purgedAt", Instant.now().toString(),
                    "purgeReason", "RETENTION_WINDOW_EXPIRED"
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            restTemplate.postForEntity(
                    complianceApiUrl + "/ledger/entries/purge",
                    request,
                    Map.class
            );

            logger.info("Marked export {} as purged in consent ledger", exportJobId);

        } catch (Exception e) {
            logger.error("Failed to mark export as purged: {}", exportJobId, e);
        }
    }

    /**
     * Consent ledger entry record.
     */
    public record ConsentLedgerEntry(
            UUID exportJobId,
            List<String> dataSubjects,        // Anonymized client IDs (hashed or de-identified)
            ExportConsentScope consentScope,  // Full export, limited data elements, etc.
            ExportHashBehavior exportHashMode,// SSN/DOB hashing behavior
            Instant retentionWindow,          // When export must be purged
            LocalDate exportPeriodStart,
            LocalDate exportPeriodEnd,
            String exportType,                // HUD, CalOES, etc.
            String exportReason,              // Annual reporting, data quality review, etc.
            String requestedBy,               // User who requested export
            Instant exportedAt,
            String storageLocation,           // Encrypted storage path
            String exportSha256Hash,          // Export integrity hash
            boolean encrypted,
            String kmsKeyId,                  // KMS key used for encryption
            boolean vawaProtected,            // Contains VAWA-protected data
            long vawaSuppressedRecords        // Count of VAWA-suppressed records
    ) {
        /**
         * Create ledger entry from export job details.
         */
        public static ConsentLedgerEntry fromExportJob(
                UUID exportJobId,
                List<String> dataSubjects,
                ExportConsentScope consentScope,
                ExportHashBehavior exportHashMode,
                int retentionDays,
                LocalDate exportPeriodStart,
                LocalDate exportPeriodEnd,
                String exportType,
                String exportReason,
                String requestedBy,
                String storageLocation,
                String exportSha256Hash,
                boolean encrypted,
                String kmsKeyId,
                boolean vawaProtected,
                long vawaSuppressedRecords) {

            Instant retentionWindow = Instant.now().plus(retentionDays, ChronoUnit.DAYS);

            return new ConsentLedgerEntry(
                    exportJobId,
                    dataSubjects,
                    consentScope,
                    exportHashMode,
                    retentionWindow,
                    exportPeriodStart,
                    exportPeriodEnd,
                    exportType,
                    exportReason,
                    requestedBy,
                    Instant.now(),
                    storageLocation,
                    exportSha256Hash,
                    encrypted,
                    kmsKeyId,
                    vawaProtected,
                    vawaSuppressedRecords
            );
        }
    }

    public static class ConsentLedgerException extends RuntimeException {
        public ConsentLedgerException(String message) {
            super(message);
        }

        public ConsentLedgerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
