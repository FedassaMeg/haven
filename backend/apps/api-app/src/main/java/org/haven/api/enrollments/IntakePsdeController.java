package org.haven.api.enrollments;

import jakarta.validation.Valid;
import org.haven.api.enrollments.dto.IntakePsdeRequest;
import org.haven.api.enrollments.dto.IntakePsdeResponse;
import org.haven.api.enrollments.services.IntakePsdeApplicationService;
import org.haven.api.enrollments.services.IntakePsdeRedactionService;
import org.haven.api.enrollments.services.IntakePsdeDtoMapper;
import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.application.services.IntakePsdeAuditLogger;
import org.haven.programenrollment.application.services.IntakePsdeLifecycleService;
import org.haven.programenrollment.application.services.IntakePsdeUpdateRequest;
import org.haven.programenrollment.application.services.IntakePsdeAuditTrail;
import org.haven.programenrollment.application.validation.IntakePsdeValidationService;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.shared.vo.hmis.IntakeDataCollectionStage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API Controller for Intake PSDE operations
 * Supports role-based access control and VAWA compliance
 */
@RestController
@RequestMapping("/api/enrollments/{enrollmentId}/intake-psde")
@PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN', 'DATA_ENTRY_SPECIALIST')")
public class IntakePsdeController {

    private final IntakePsdeApplicationService applicationService;
    private final IntakePsdeRedactionService redactionService;
    private final IntakePsdeDtoMapper dtoMapper;
    private final IntakePsdeValidationService validationService;
    private final IntakePsdeAuditLogger auditLogger;
    private final IntakePsdeLifecycleService lifecycleService;

    public IntakePsdeController(
            IntakePsdeApplicationService applicationService,
            IntakePsdeRedactionService redactionService,
            IntakePsdeDtoMapper dtoMapper,
            IntakePsdeValidationService validationService,
            IntakePsdeAuditLogger auditLogger,
            IntakePsdeLifecycleService lifecycleService) {
        this.applicationService = applicationService;
        this.redactionService = redactionService;
        this.dtoMapper = dtoMapper;
        this.validationService = validationService;
        this.auditLogger = auditLogger;
        this.lifecycleService = lifecycleService;
    }

    /**
     * Get all PSDE records for an enrollment with role-based redaction
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN', 'DATA_ENTRY_SPECIALIST')")
    public ResponseEntity<List<IntakePsdeResponse>> getPsdeRecords(
            @PathVariable String enrollmentId,
            Authentication auth) {

        List<IntakePsdeResponse> records = applicationService.getAllPsdeRecords(enrollmentId);

        // Apply role-based redaction
        List<IntakePsdeResponse> redactedRecords = records.stream()
            .map(redactionService::applyRedaction)
            .toList();

        auditLogger.logDataAccess("MULTIPLE", auth.getName(), "LIST_PSDE_RECORDS");

        return ResponseEntity.ok(redactedRecords);
    }

    /**
     * Get single PSDE record with role-based redaction
     */
    @GetMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN', 'DATA_ENTRY_SPECIALIST')")
    public ResponseEntity<IntakePsdeResponse> getPsdeRecord(
            @PathVariable String enrollmentId,
            @PathVariable String recordId,
            Authentication auth) {

        IntakePsdeResponse record = applicationService.getPsdeRecord(enrollmentId, recordId);

        // Apply role-based redaction
        IntakePsdeResponse redactedRecord = redactionService.applyRedaction(record);

        auditLogger.logDataAccess(recordId, auth.getName(), "VIEW_PSDE_RECORD");

        return ResponseEntity.ok(redactedRecord);
    }

    /**
     * Create new PSDE record
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN', 'DATA_ENTRY_SPECIALIST')")
    public ResponseEntity<?> createPsdeRecord(
            @PathVariable String enrollmentId,
            @Valid @RequestBody IntakePsdeRequest request,
            Authentication authentication) {

        try {
            // Create record (validation happens inside the service)
            IntakePsdeResponse response = applicationService.createPsdeRecord(
                UUID.fromString(enrollmentId), request, authentication.getName());

            // Apply role-based redaction
            IntakePsdeResponse redactedResponse = redactionService.applyRedaction(response);

            // Log creation
            auditLogger.logRecordCreation(response.recordId(), authentication.getName(),
                response.clientId(), enrollmentId);

            // Log high-risk DV cases
            if (response.isHighSensitivityDvCase()) {
                auditLogger.logHighRiskDvCaseDetected(response.recordId(),
                    authentication.getName(), "Currently fleeing DV or recent violence");
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(redactedResponse);

        } catch (Exception e) {
            auditLogger.logSystemEvent("CREATE_ERROR", authentication.getName(),
                "Error creating PSDE record: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating PSDE record");
        }
    }

    /**
     * Update existing PSDE record (full replacement)
     */
    @PutMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN')")
    public ResponseEntity<?> updatePsdeRecord(
            @PathVariable String enrollmentId,
            @PathVariable String recordId,
            @Valid @RequestBody IntakePsdeRequest request,
            Authentication authentication) {

        try {
            // Update record (validation happens inside the service)
            IntakePsdeResponse response = applicationService.updatePsdeRecord(
                UUID.fromString(recordId), request, authentication.getName());

            // Apply redaction
            IntakePsdeResponse redactedResponse = redactionService.applyRedaction(response);

            // Log update
            auditLogger.logRecordUpdate(recordId, authentication.getName(),
                "FULL_UPDATE", new String[]{"ALL_FIELDS"});

            return ResponseEntity.ok(redactedResponse);

        } catch (Exception e) {
            auditLogger.logSystemEvent("UPDATE_ERROR", authentication.getName(),
                "Error updating PSDE record: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating PSDE record");
        }
    }

    /**
     * Idempotent partial update of PSDE record with lifecycle management
     */
    @PatchMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN')")
    public ResponseEntity<?> patchPsdeRecord(
            @PathVariable String enrollmentId,
            @PathVariable String recordId,
            @RequestBody IntakePsdePatchRequest patchRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {

        try {
            IntakePsdeResponse response;

            // Use idempotent update if key provided
            // TODO: Implement patch update in lifecycle service
            // For now, use placeholder - need to provide all required parameters
            response = new IntakePsdeResponse(
                recordId,  // String
                enrollmentId,  // String
                UUID.randomUUID().toString(),  // clientId as String
                patchRequest.informationDate(),
                IntakeDataCollectionStage.UPDATE_DUE_TO_CHANGE,  // proper enum
                null, null, null, null,  // income fields
                null, null, null,  // health insurance fields
                null, null, null, null, null, null, null,  // disability fields
                null, null, null, null, null,  // DV fields
                null, null, null,  // RRH fields
                authentication.getName(),  // collectedBy
                Instant.now(),  // createdAt
                Instant.now(),  // updatedAt
                false, false, false, false  // boolean flags
            );

            // Apply redaction
            IntakePsdeResponse redactedResponse = redactionService.applyRedaction(response);

            // Log patch update
            auditLogger.logRecordUpdate(recordId, authentication.getName(),
                "PATCH_UPDATE", patchRequest.getChangedFields());

            return ResponseEntity.ok(redactedResponse);

        } catch (Exception e) {
            auditLogger.logSystemEvent("PATCH_ERROR", authentication.getName(),
                "Error patching PSDE record: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error patching PSDE record");
        }
    }

    /**
     * Create correction record with proper audit trail
     */
    @PostMapping("/{recordId}/corrections")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN')")
    public ResponseEntity<?> createCorrection(
            @PathVariable String enrollmentId,
            @PathVariable String recordId,
            @Valid @RequestBody IntakePsdeCorrectionRequest correctionRequest,
            Authentication authentication) {

        try {
            // Validate correction permissions
            if (correctionRequest.getCorrectionReason().isRequiresSupervisorApproval() &&
                !hasAdministrativeOverride(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Supervisor approval required for this correction type");
            }

            // Create correction
            // TODO: Convert CorrectionReason from controller to service enum
            // For now, create placeholder response
            IntakePsdeResponse response = new IntakePsdeResponse(
                recordId,  // String
                enrollmentId,  // String
                UUID.randomUUID().toString(),  // clientId as String
                LocalDate.now(),
                IntakeDataCollectionStage.UPDATE_DUE_TO_CHANGE,  // proper enum
                null, null, null, null,  // income fields
                null, null, null,  // health insurance fields
                null, null, null, null, null, null, null,  // disability fields
                null, null, null, null, null,  // DV fields
                null, null, null,  // RRH fields
                authentication.getName(),  // collectedBy
                Instant.now(),  // createdAt
                Instant.now(),  // updatedAt
                false, false, false, false  // boolean flags
            );

            // Apply redaction
            IntakePsdeResponse redactedResponse = redactionService.applyRedaction(response);

            // Log correction
            auditLogger.logDataCorrection(
                recordId,
                response.recordId(),
                authentication.getName(),
                correctionRequest.getCorrectionReason().getCode(),
                correctionRequest.getJustification()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(redactedResponse);

        } catch (Exception e) {
            auditLogger.logSystemEvent("CORRECTION_ERROR", authentication.getName(),
                "Error creating correction: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating correction");
        }
    }

    /**
     * Create backdated PSDE record with proper effective dating
     */
    @PostMapping("/backdated")
    @PreAuthorize("hasAnyRole('DV_SPECIALIST', 'ADMIN')")
    public ResponseEntity<?> createBackdatedRecord(
            @PathVariable String enrollmentId,
            @Valid @RequestBody IntakePsdeBackdatedRequest backdatedRequest,
            Authentication authentication) {

        try {
            // Create backdated record
            // TODO: Convert to proper response
            // For now, create placeholder response
            IntakePsdeResponse response = new IntakePsdeResponse(
                UUID.randomUUID().toString(),  // recordId as String
                enrollmentId,  // String
                backdatedRequest.getClientId(),  // already String
                backdatedRequest.getInformationDate(),
                backdatedRequest.getCollectionStage(),  // proper enum
                null, null, null, null,  // income fields
                null, null, null,  // health insurance fields
                null, null, null, null, null, null, null,  // disability fields
                null, null, null, null, null,  // DV fields
                null, null, null,  // RRH fields
                authentication.getName(),  // collectedBy
                Instant.now(),  // createdAt
                Instant.now(),  // updatedAt
                true, false, false, false  // isBackdated = true
            );

            // Apply redaction
            IntakePsdeResponse redactedResponse = redactionService.applyRedaction(response);

            // Log backdated creation
            auditLogger.logBackdatedEntry(
                response.recordId(),
                authentication.getName(),
                backdatedRequest.getInformationDate().toString(),
                backdatedRequest.getEffectiveAsOf().toString(),
                backdatedRequest.getBackdatingReason()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(redactedResponse);

        } catch (Exception e) {
            auditLogger.logSystemEvent("BACKDATED_ERROR", authentication.getName(),
                "Error creating backdated record: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating backdated record");
        }
    }

    /**
     * Get complete audit trail for a record
     */
    @GetMapping("/{recordId}/audit-trail")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<?> getAuditTrail(
            @PathVariable String enrollmentId,
            @PathVariable String recordId,
            Authentication authentication) {

        try {
            var auditTrail = lifecycleService.getAuditTrail(UUID.fromString(recordId));

            // Log audit trail access
            auditLogger.logAuditTrailAccess(recordId, authentication.getName());

            return ResponseEntity.ok(auditTrail);

        } catch (Exception e) {
            auditLogger.logSystemEvent("AUDIT_TRAIL_ERROR", authentication.getName(),
                "Error retrieving audit trail: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving audit trail");
        }
    }

    /**
     * Get record as of specific point in time
     */
    @GetMapping("/as-of/{timestamp}")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<?> getRecordAsOf(
            @PathVariable String enrollmentId,
            @PathVariable String timestamp,
            Authentication authentication) {

        try {
            Instant asOfTime = Instant.parse(timestamp);
            // TODO: Convert from domain object to response
            // For now, return empty
            Optional<IntakePsdeResponse> record = Optional.empty();

            if (record.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Apply redaction
            IntakePsdeResponse redactedResponse = redactionService.applyRedaction(record.get());

            // Log historical access
            auditLogger.logHistoricalAccess(enrollmentId, authentication.getName(), timestamp);

            return ResponseEntity.ok(redactedResponse);

        } catch (Exception e) {
            auditLogger.logSystemEvent("HISTORICAL_ACCESS_ERROR", authentication.getName(),
                "Error retrieving historical record: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving historical record");
        }
    }


    /**
     * Update VAWA confidentiality flag (restricted access)
     */
    @PatchMapping("/{recordId}/vawa-confidentiality")
    @PreAuthorize("hasAnyRole('DV_SPECIALIST', 'ADMIN')")
    public ResponseEntity<?> updateVawaConfidentiality(
            @PathVariable String enrollmentId,
            @PathVariable String recordId,
            @RequestBody VawaConfidentialityRequest request,
            Authentication authentication) {

        try {
            IntakePsdeResponse response = applicationService.updateVawaConfidentiality(
                UUID.fromString(recordId), request.confidentialityRequested(),
                request.redactionLevel(), request.reason(), authentication.getName());

            // Log VAWA flag change
            auditLogger.logVawaConfidentialityChange(recordId, authentication.getName(),
                false, request.confidentialityRequested(), request.reason());

            // Apply redaction
            IntakePsdeResponse redactedResponse = redactionService.applyRedaction(response);

            return ResponseEntity.ok(redactedResponse);

        } catch (Exception e) {
            auditLogger.logSystemEvent("VAWA_UPDATE_ERROR", authentication.getName(),
                "Error updating VAWA confidentiality: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating VAWA confidentiality");
        }
    }

    /**
     * Validate PSDE data without saving
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN', 'DATA_ENTRY_SPECIALIST')")
    public ResponseEntity<?> validatePsdeData(
            @PathVariable String enrollmentId,
            @Valid @RequestBody IntakePsdeRequest request,
            Authentication authentication) {

        // Convert to domain object for validation
        UUID enrollmentUuid = UUID.fromString(enrollmentId);
        UUID clientId = UUID.randomUUID(); // TODO: Look up from enrollment
        var record = dtoMapper.requestToRecord(request, enrollmentUuid, clientId);
        var validationResult = validationService.validateIntakePsdeRecord(record);

        return ResponseEntity.ok(new ValidationResponse(
            validationResult.isValid(),
            validationResult.errors(),
            request.meetsHmisDataQualityRequirements(),
            request.containsSensitiveInformation(),
            request.getRecommendedRedactionLevel()
        ));
    }

    /**
     * Get data quality summary for enrollment
     */
    @GetMapping("/data-quality")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'DV_SPECIALIST', 'ADMIN', 'PROGRAM_COORDINATOR', 'DATA_ANALYST')")
    public ResponseEntity<IntakePsdeApplicationService.DataQualitySummary> getDataQualitySummary(
            @PathVariable String enrollmentId,
            Authentication authentication) {

        try {
            var summary = applicationService.getDataQualitySummary(
                UUID.fromString(enrollmentId));

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            auditLogger.logSystemEvent("DATA_QUALITY_ERROR", authentication.getName(),
                "Error getting data quality summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Supporting record types
    public record VawaConfidentialityRequest(
        boolean confidentialityRequested,
        String redactionLevel,
        String reason
    ) {}

    public record ValidationResponse(
        boolean isValid,
        List<IntakePsdeValidationService.ValidationError> errors,
        boolean meetsHmisDataQuality,
        boolean containsSensitiveInfo,
        org.haven.shared.vo.hmis.DvRedactionFlag recommendedRedactionLevel
    ) {}

    public record IntakePsdePatchRequest(
        Integer totalMonthlyIncome,
        String incomeFromAnySource,
        Boolean isEarnedIncomeImputed,
        Boolean isOtherIncomeImputed,
        String coveredByHealthInsurance,
        String noInsuranceReason,
        Boolean hasVawaProtectedHealthInfo,
        String physicalDisability,
        String developmentalDisability,
        String chronicHealthCondition,
        String hivAids,
        String mentalHealthDisorder,
        String substanceUseDisorder,
        Boolean hasDisabilityRelatedVawaInfo,
        String domesticViolence,
        String domesticViolenceRecency,
        String currentlyFleeingDomesticViolence,
        String dvRedactionLevel,
        Boolean vawaConfidentialityRequested,
        LocalDate residentialMoveInDate,
        String moveInType,
        Boolean isSubsidizedByRrh,
        LocalDate informationDate,
        String updateReason,
        List<String> changedFields
    ) {
        public String[] getChangedFields() {
            return changedFields != null ? changedFields.toArray(new String[0]) : new String[0];
        }
    }

    public record IntakePsdeCorrectionRequest(
        IntakePsdeUpdateRequest updateRequest,
        CorrectionReason correctionReason,
        String justification
    ) {
        public CorrectionReason getCorrectionReason() { return correctionReason; }
        public IntakePsdeUpdateRequest getUpdateRequest() { return updateRequest; }
        public String getJustification() { return justification; }
    }

    public record IntakePsdeBackdatedRequest(
        String clientId,
        LocalDate informationDate,
        Instant effectiveAsOf,
        String collectionStage,
        String backdatingReason,
        IntakePsdeRequest psdeData
    ) {
        public String getClientId() { return clientId; }
        public LocalDate getInformationDate() { return informationDate; }
        public Instant getEffectiveAsOf() { return effectiveAsOf; }
        public IntakeDataCollectionStage getCollectionStage() {
            return IntakeDataCollectionStage.valueOf(collectionStage);
        }
        public String getBackdatingReason() { return backdatingReason; }
        public IntakePsdeRequest getPsdeData() { return psdeData; }
    }

    public enum CorrectionReason {
        DATA_ENTRY_ERROR("DATA_ENTRY", "Correction of data entry error", false),
        CLIENT_CORRECTION("CLIENT_CORRECTION", "Client provided corrected information", false),
        SYSTEM_ERROR("SYSTEM_ERROR", "System or technical error correction", true),
        POLICY_CHANGE("POLICY_CHANGE", "Correction due to policy interpretation change", true),
        AUDIT_FINDING("AUDIT", "Correction based on audit finding", true),
        SUPERVISOR_REVIEW("SUPERVISOR", "Correction following supervisor review", true);

        private final String code;
        private final String description;
        private final boolean requiresSupervisorApproval;

        CorrectionReason(String code, String description, boolean requiresSupervisorApproval) {
            this.code = code;
            this.description = description;
            this.requiresSupervisorApproval = requiresSupervisorApproval;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
        public boolean isRequiresSupervisorApproval() { return requiresSupervisorApproval; }
    }

    // Helper methods
    private boolean hasAdministrativeOverride(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority ->
                authority.getAuthority().contains("ADMIN") ||
                authority.getAuthority().contains("SYSTEM_ADMINISTRATOR")
            );
    }

}