package org.haven.programenrollment.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.haven.programenrollment.domain.ce.CePacket;
import org.haven.programenrollment.domain.ce.CeShareScope;
import org.haven.programenrollment.domain.vsp.VspExportMetadata;
import org.haven.programenrollment.domain.vsp.VspExportMetadataRepository;
import org.haven.shared.audit.AuditService;
import org.haven.shared.vo.hmis.VawaRecipientCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced VSP Export Service with CE-specific anonymization
 * Implements comprehensive export metadata tracking and VAWA compliance
 */
@Service
@Transactional
public class VspExportService {

    private static final Logger logger = LoggerFactory.getLogger(VspExportService.class);
    private static final int DEFAULT_EXPIRY_DAYS = 90;

    private final VspExportMetadataRepository metadataRepository;
    private final CeExportService ceExportService;
    private final ConsentLedgerUpdatePublisher consentPublisher;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public VspExportService(
            VspExportMetadataRepository metadataRepository,
            CeExportService ceExportService,
            ConsentLedgerUpdatePublisher consentPublisher,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.metadataRepository = metadataRepository;
        this.ceExportService = ceExportService;
        this.consentPublisher = consentPublisher;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Export data for VSP with CE-specific anonymization
     */
    public VspExportResult exportForVsp(VspExportRequest request) {
        logger.info("Starting VSP export for recipient: {}", request.recipient());

        // Validate VAWA compliance
        validateVawaCompliance(request);

        // Generate CE hash key
        String ceHashKey = generateCeHashKey(request);

        // Create anonymization rules based on recipient category
        VspExportMetadata.AnonymizationRules rules = createAnonymizationRules(request);

        // Prepare export metadata
        VspExportMetadata metadata = new VspExportMetadata(
            UUID.randomUUID(),
            request.recipient(),
            request.recipientCategory(),
            request.consentBasis(),
            generatePacketHash(request),
            ceHashKey,
            Instant.now(),
            calculateExpiryDate(request.expiryDays()),
            request.shareScopes(),
            rules,
            buildMetadataMap(request),
            request.initiatedBy()
        );

        // Perform the export with anonymization
        Map<String, Object> exportData = performExport(request, metadata);

        // Save metadata to repository
        metadata = metadataRepository.save(metadata);

        // Audit the export
        auditExport(metadata, exportData.size());

        // Update consent ledger
        updateConsentLedger(metadata);

        return new VspExportResult(
            metadata.getExportId(),
            metadata.getRecipient(),
            metadata.getCeHashKey(),
            exportData,
            metadata.getExportTimestamp(),
            metadata.getExpiryDate(),
            metadata.getStatus()
        );
    }

    /**
     * Get share history for a specific recipient
     */
    @Transactional(readOnly = true)
    public RecipientShareHistory getShareHistory(String recipient) {
        List<VspExportMetadata> exports = metadataRepository.findByRecipient(recipient);
        VspExportMetadataRepository.ExportStatistics stats =
            metadataRepository.getStatisticsForRecipient(recipient);

        List<ShareHistoryEntry> entries = exports.stream()
            .map(this::toShareHistoryEntry)
            .collect(Collectors.toList());

        return new RecipientShareHistory(
            recipient,
            entries,
            stats.totalExports(),
            stats.activeExports(),
            stats.revokedExports(),
            stats.expiredExports(),
            stats.firstExportDate(),
            stats.lastExportDate()
        );
    }

    /**
     * Revoke an export with audit trail
     */
    public void revokeExport(UUID exportId, String revokedBy, String reason) {
        VspExportMetadata metadata = metadataRepository.findById(exportId)
            .orElseThrow(() -> new IllegalArgumentException("Export not found: " + exportId));

        metadata.revoke(revokedBy, reason);
        metadataRepository.save(metadata);

        // Audit the revocation
        auditService.logAction("VSP_EXPORT_REVOKED", Map.of(
            "exportId", exportId,
            "recipient", metadata.getRecipient(),
            "revokedBy", revokedBy,
            "reason", reason,
            "timestamp", Instant.now()
        ));

        // Notify recipient of revocation
        notifyRecipientOfRevocation(metadata, reason);
    }

    /**
     * Check and update expired exports
     */
    public void processExpiredExports() {
        LocalDateTime now = LocalDateTime.now();
        List<VspExportMetadata> expiring = metadataRepository.findExpiringBefore(now);

        for (VspExportMetadata export : expiring) {
            export.checkAndUpdateExpiry();
            metadataRepository.updateStatus(export.getExportId(), export.getStatus());

            logger.info("Export {} expired for recipient {}",
                export.getExportId(), export.getRecipient());
        }

        // Clean up old expired exports
        Instant retentionCutoff = Instant.now().minus(365, java.time.temporal.ChronoUnit.DAYS);
        int deleted = metadataRepository.deleteExpiredOlderThan(retentionCutoff);
        logger.info("Deleted {} expired exports older than retention period", deleted);
    }

    private void validateVawaCompliance(VspExportRequest request) {
        if (!request.recipientCategory().isAuthorizedForVictimData()) {
            throw new IllegalArgumentException(
                "Recipient category " + request.recipientCategory() +
                " is not authorized for victim data"
            );
        }

        // Additional validation for sensitive data
        if (request.shareScopes().contains(CeShareScope.DV_DATA) &&
            !request.recipientCategory().hasFullVawaCompliance()) {
            throw new IllegalArgumentException(
                "Recipient does not have full VAWA compliance for DV data"
            );
        }
    }

    private String generateCeHashKey(VspExportRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = request.recipient() + "_" +
                          request.enrollmentIds().stream()
                              .map(UUID::toString)
                              .collect(Collectors.joining("_")) +
                          "_" + System.currentTimeMillis();

            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return "CE_" + Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CE hash key", e);
        }
    }

    private VspExportMetadata.AnonymizationRules createAnonymizationRules(VspExportRequest request) {
        VawaRecipientCategory.AnonymizationLevel level =
            request.recipientCategory().getRequiredAnonymizationLevel();

        VspExportMetadata.AnonymizationRules.Builder builder =
            VspExportMetadata.AnonymizationRules.builder();

        switch (level) {
            case MINIMAL:
                // VSPs with full compliance - minimal anonymization
                builder.suppressLocationMetadata(false)
                      .replaceHouseholdIds(false)
                      .redactDvIndicators(false)
                      .anonymizeDates(false);
                break;

            case STANDARD:
                // Authorized but limited recipients
                builder.suppressLocationMetadata(true)
                      .replaceHouseholdIds(true)
                      .redactDvIndicators(false)
                      .anonymizeDates(true)
                      .addFieldToRedact("ssn")
                      .addFieldToRedact("phoneNumber")
                      .addFieldToRedact("email");
                break;

            case FULL:
                // Maximum redaction for unauthorized recipients
                builder.suppressLocationMetadata(true)
                      .replaceHouseholdIds(true)
                      .redactDvIndicators(true)
                      .anonymizeDates(true)
                      .addFieldToRedact("ssn")
                      .addFieldToRedact("firstName")
                      .addFieldToRedact("lastName")
                      .addFieldToRedact("dateOfBirth")
                      .addFieldToRedact("phoneNumber")
                      .addFieldToRedact("email")
                      .addFieldToRedact("address")
                      .addFieldMapping("clientId", "anonymousId");
                break;
        }

        // Add any custom rules from request
        if (request.additionalRedactions() != null) {
            for (String field : request.additionalRedactions()) {
                builder.addFieldToRedact(field);
            }
        }

        return builder.build();
    }

    private String generatePacketHash(VspExportRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String packetData = objectMapper.writeValueAsString(request);
            byte[] hash = digest.digest(packetData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate packet hash", e);
        }
    }

    private LocalDateTime calculateExpiryDate(Integer expiryDays) {
        int days = expiryDays != null ? expiryDays : DEFAULT_EXPIRY_DAYS;
        return LocalDateTime.now().plusDays(days);
    }

    private Map<String, Object> buildMetadataMap(VspExportRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("requestId", request.requestId());
        metadata.put("exportFormat", request.exportFormat());
        metadata.put("recordCount", request.enrollmentIds().size());
        metadata.put("includeAssessments", request.includeAssessments());
        metadata.put("includeEvents", request.includeEvents());
        metadata.put("includeReferrals", request.includeReferrals());
        metadata.put("exportReason", request.exportReason());
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> performExport(VspExportRequest request, VspExportMetadata metadata) {
        // Get base export data from CE export service
        CeExportService.ExportRequest ceRequest = new CeExportService.ExportRequest(
            request.cocId(),
            request.enrollmentIds(),
            request.startDate(),
            request.endDate(),
            CeExportService.ExportType.ALL_RECORDS,
            request.shareScopes(),
            request.encryptionKeyId(),
            request.initiatedBy()
        );

        CeExportService.CeExportResult ceResult;
        switch (request.exportFormat()) {
            case "JSON":
                ceResult = ceExportService.exportVendorJson(ceRequest);
                break;
            case "XML":
                ceResult = ceExportService.exportHudXml(ceRequest);
                break;
            default:
                ceResult = ceExportService.exportHudCsv(ceRequest);
                break;
        }

        // Apply anonymization rules
        Map<String, Object> rawData = Map.of(
            "fileName", ceResult.fileName(),
            "recordCount", ceResult.recordCount(),
            "format", ceResult.format(),
            "data", new String(ceResult.encryptedData(), StandardCharsets.UTF_8)
        );

        return metadata.getAnonymizationRules().apply(rawData, metadata.getCeHashKey());
    }

    private void auditExport(VspExportMetadata metadata, int dataSize) {
        auditService.logAction("VSP_EXPORT_CREATED", Map.of(
            "exportId", metadata.getExportId(),
            "recipient", metadata.getRecipient(),
            "recipientCategory", metadata.getRecipientCategory(),
            "ceHashKey", metadata.getCeHashKey(),
            "shareScopes", metadata.getShareScopes(),
            "dataSize", dataSize,
            "expiryDate", metadata.getExpiryDate(),
            "initiatedBy", metadata.getInitiatedBy()
        ));
    }

    private void updateConsentLedger(VspExportMetadata metadata) {
        Map<String, Object> ledgerUpdate = Map.of(
            "eventType", "VSP_EXPORT",
            "exportId", metadata.getExportId(),
            "recipient", metadata.getRecipient(),
            "consentBasis", metadata.getConsentBasis(),
            "shareScopes", metadata.getShareScopes(),
            "ceHashKey", metadata.getCeHashKey(),
            "timestamp", metadata.getExportTimestamp()
        );

        consentPublisher.publishUpdate(ledgerUpdate);
    }

    private void notifyRecipientOfRevocation(VspExportMetadata metadata, String reason) {
        // Implementation would send notification to recipient
        logger.info("Notifying {} of export revocation: {}", metadata.getRecipient(), reason);
    }

    private ShareHistoryEntry toShareHistoryEntry(VspExportMetadata metadata) {
        return new ShareHistoryEntry(
            metadata.getExportId(),
            metadata.getExportTimestamp(),
            metadata.getExpiryDate(),
            metadata.getStatus(),
            metadata.getShareScopes(),
            metadata.getConsentBasis(),
            metadata.getCeHashKey(),
            metadata.getRevokedAt(),
            metadata.getRevokedBy(),
            metadata.getRevocationReason()
        );
    }

    // Request and response records
    public record VspExportRequest(
        UUID requestId,
        String recipient,
        VawaRecipientCategory recipientCategory,
        String consentBasis,
        String cocId,
        List<UUID> enrollmentIds,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Set<CeShareScope> shareScopes,
        String exportFormat,
        String encryptionKeyId,
        Integer expiryDays,
        String initiatedBy,
        boolean includeAssessments,
        boolean includeEvents,
        boolean includeReferrals,
        String exportReason,
        Set<String> additionalRedactions
    ) {}

    public record VspExportResult(
        UUID exportId,
        String recipient,
        String ceHashKey,
        Map<String, Object> anonymizedData,
        Instant exportTimestamp,
        LocalDateTime expiryDate,
        VspExportMetadata.ExportStatus status
    ) {}

    public record RecipientShareHistory(
        String recipient,
        List<ShareHistoryEntry> exports,
        long totalExports,
        long activeExports,
        long revokedExports,
        long expiredExports,
        Instant firstExportDate,
        Instant lastExportDate
    ) {}

    public record ShareHistoryEntry(
        UUID exportId,
        Instant exportTimestamp,
        LocalDateTime expiryDate,
        VspExportMetadata.ExportStatus status,
        Set<CeShareScope> shareScopes,
        String consentBasis,
        String ceHashKey,
        Instant revokedAt,
        String revokedBy,
        String revocationReason
    ) {}
}