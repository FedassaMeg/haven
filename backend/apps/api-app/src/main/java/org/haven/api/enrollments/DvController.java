package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.programenrollment.application.services.DvLifecycleService;
import org.haven.programenrollment.domain.DomesticViolenceRecency;
import org.haven.programenrollment.domain.DvRecord;
import org.haven.programenrollment.domain.DvSafetyAssessment;
import org.haven.shared.vo.hmis.HmisFivePoint;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API for Domestic Violence lifecycle management
 * Supports HMIS FY2024 UDE 4.11 Domestic Violence data collection
 * Enhanced security and privacy considerations for sensitive DV data
 */
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/domestic-violence")
@Tag(name = "Domestic Violence", description = "HMIS Domestic Violence (UDE 4.11) lifecycle management with enhanced security")
@PreAuthorize("hasRole('CASE_MANAGER') or hasRole('ADMIN') or hasRole('DV_SPECIALIST')")
public class DvController {
    
    private final DvLifecycleService dvService;
    
    public DvController(DvLifecycleService dvService) {
        this.dvService = dvService;
    }
    
    @Operation(
        summary = "Create DV record at project start",
        description = "Creates PROJECT_START domestic violence record (HMIS required). Requires DV_SPECIALIST or ADMIN role for sensitive data."
    )
    @PostMapping("/start")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<DvResponse> createProjectStartRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateDvRequest request) {
        
        try {
            DvRecord record = dvService.createProjectStartRecord(
                enrollmentId,
                request.dvHistory(),
                request.whenExperienced(),
                request.currentlyFleeing(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DvResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create DV update record",
        description = "Creates UPDATE domestic violence record due to change in circumstances"
    )
    @PostMapping("/update")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<DvResponse> createUpdateRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateDvUpdateRequest request) {
        
        try {
            DvRecord record = dvService.createUpdateRecord(
                enrollmentId,
                request.changeDate(),
                request.dvHistory(),
                request.whenExperienced(),
                request.currentlyFleeing(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DvResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create DV record at project exit",
        description = "Creates PROJECT_EXIT domestic violence record (HMIS required)"
    )
    @PostMapping("/exit")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<DvResponse> createProjectExitRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateDvExitRequest request) {
        
        try {
            DvRecord record = dvService.createProjectExitRecord(
                enrollmentId,
                request.exitDate(),
                request.dvHistory(),
                request.whenExperienced(),
                request.currentlyFleeing(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DvResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create correction record",
        description = "Creates a correction record for an existing DV record"
    )
    @PostMapping("/correct/{recordId}")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<DvResponse> createCorrectionRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Original record ID to correct")
            @PathVariable UUID recordId,
            @Valid @RequestBody CreateDvCorrectionRequest request) {
        
        try {
            DvRecord record = dvService.createCorrectionRecord(
                recordId,
                request.dvHistory(),
                request.whenExperienced(),
                request.currentlyFleeing(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DvResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get latest DV record",
        description = "Retrieves the latest effective DV record for the enrollment"
    )
    @GetMapping("/latest")
    public ResponseEntity<DvResponse> getLatestDvRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            DvRecord record = dvService.getLatestEffectiveDvRecord(enrollmentId);
            
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(DvResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get all DV records",
        description = "Retrieves all DV records for the enrollment, ordered by date descending"
    )
    @GetMapping
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<List<DvResponse>> getAllDvRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            List<DvRecord> records = dvService.getDvRecordsForEnrollment(enrollmentId);
            
            List<DvResponse> response = records.stream()
                .map(DvResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get DV safety assessment",
        description = "Retrieves comprehensive safety assessment for the enrollment"
    )
    @GetMapping("/safety-assessment")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<DvSafetyAssessmentResponse> getDvSafetyAssessment(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            DvSafetyAssessment assessment = dvService.getDvSafetyAssessment(enrollmentId);
            
            DvSafetyAssessmentResponse response = DvSafetyAssessmentResponse.from(assessment);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Check DV status",
        description = "Checks DV history and current fleeing status (limited access for case managers)"
    )
    @GetMapping("/status")
    public ResponseEntity<DvStatusResponse> getDvStatus(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            boolean hasDvHistory = dvService.hasDvHistory(enrollmentId);
            boolean isCurrentlyFleeing = dvService.isCurrentlyFleeingDv(enrollmentId);
            boolean requiresEnhancedSafety = dvService.requiresEnhancedSafety(enrollmentId);
            
            DvStatusResponse response = new DvStatusResponse(
                hasDvHistory,
                isCurrentlyFleeing,
                requiresEnhancedSafety
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create safety alert",
        description = "Creates safety alert for high-risk clients requiring enhanced protocols"
    )
    @PostMapping("/safety-alert")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<SafetyAlertResponse> createSafetyAlert(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateSafetyAlertRequest request) {
        
        try {
            dvService.createSafetyAlert(enrollmentId, request.alertDetails(), request.createdBy());
            
            SafetyAlertResponse response = new SafetyAlertResponse(
                enrollmentId,
                "Safety alert created successfully",
                Instant.now()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get correction records",
        description = "Retrieves all correction records for a specific original record"
    )
    @GetMapping("/records/{recordId}/corrections")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<List<DvResponse>> getCorrectionRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Original record ID")
            @PathVariable UUID recordId) {
        
        try {
            List<DvRecord> corrections = dvService.getCorrectionRecords(recordId);
            
            List<DvResponse> response = corrections.stream()
                .map(DvResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Check compliance status",
        description = "Checks if enrollment meets HMIS DV data compliance requirements"
    )
    @GetMapping("/compliance")
    public ResponseEntity<DvComplianceResponse> checkComplianceStatus(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            boolean meetsCompliance = dvService.meetsDvDataCompliance(enrollmentId);
            boolean hasStart = dvService.hasProjectStartRecord(enrollmentId);
            boolean hasExit = dvService.hasProjectExitRecord(enrollmentId);
            
            DvComplianceResponse response = new DvComplianceResponse(
                meetsCompliance,
                hasStart,
                hasExit,
                !hasStart ? "Missing required PROJECT_START record" : null
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // High-Risk Management Endpoints
    
    @Operation(
        summary = "Get high-risk clients requiring intervention",
        description = "Retrieves list of high-risk clients currently fleeing DV"
    )
    @GetMapping("/high-risk-clients")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<List<UUID>> getHighRiskClients() {
        
        try {
            List<UUID> highRiskClients = dvService.getHighRiskClientsRequiringIntervention();
            return ResponseEntity.ok(highRiskClients);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get enrollments requiring safety protocols",
        description = "Retrieves enrollments that require enhanced safety protocol activation"
    )
    @GetMapping("/safety-protocols-required")
    @PreAuthorize("hasRole('DV_SPECIALIST') or hasRole('ADMIN')")
    public ResponseEntity<List<UUID>> getEnrollmentsRequiringSafetyProtocols() {
        
        try {
            List<UUID> enrollments = dvService.getEnrollmentsRequiringSafetyProtocols();
            return ResponseEntity.ok(enrollments);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request/Response DTOs
    
    public record CreateDvRequest(
        @NotNull HmisFivePoint dvHistory,
        DomesticViolenceRecency whenExperienced,
        @NotNull HmisFivePoint currentlyFleeing,
        @NotNull String collectedBy
    ) {}
    
    public record CreateDvUpdateRequest(
        @NotNull LocalDate changeDate,
        @NotNull HmisFivePoint dvHistory,
        DomesticViolenceRecency whenExperienced,
        @NotNull HmisFivePoint currentlyFleeing,
        @NotNull String collectedBy
    ) {}
    
    public record CreateDvExitRequest(
        @NotNull LocalDate exitDate,
        @NotNull HmisFivePoint dvHistory,
        DomesticViolenceRecency whenExperienced,
        @NotNull HmisFivePoint currentlyFleeing,
        @NotNull String collectedBy
    ) {}
    
    public record CreateDvCorrectionRequest(
        @NotNull HmisFivePoint dvHistory,
        DomesticViolenceRecency whenExperienced,
        @NotNull HmisFivePoint currentlyFleeing,
        @NotNull String collectedBy
    ) {}
    
    public record CreateSafetyAlertRequest(
        @NotNull String alertDetails,
        @NotNull String createdBy
    ) {}
    
    public record DvResponse(
        UUID recordId,
        UUID enrollmentId,
        UUID clientId,
        LocalDate informationDate,
        String stage,
        HmisFivePoint dvHistory,
        DomesticViolenceRecency whenExperienced,
        HmisFivePoint currentlyFleeing,
        boolean isCorrection,
        UUID correctsRecordId,
        String collectedBy,
        Instant createdAt,
        Instant updatedAt,
        boolean meetsDataQuality,
        boolean requiresEnhancedSafety
    ) {
        public static DvResponse from(DvRecord record) {
            return new DvResponse(
                record.getRecordId(),
                record.getEnrollmentId().value(),
                record.getClientId().value(),
                record.getInformationDate(),
                record.getStage().getDisplayName(),
                record.getDvHistory(),
                record.getWhenExperienced(),
                record.getCurrentlyFleeing(),
                record.isCorrection(),
                record.getCorrectsRecordId(),
                record.getCollectedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.meetsDataQuality(),
                record.requiresEnhancedSafety()
            );
        }
    }
    
    public record DvSafetyAssessmentResponse(
        boolean hasDvHistory,
        boolean isCurrentlyFleeing,
        boolean isDvRecent,
        boolean isDvVeryRecent,
        boolean requiresEnhancedSafety,
        String riskLevel,
        String recommendedActions
    ) {
        public static DvSafetyAssessmentResponse from(DvSafetyAssessment assessment) {
            return new DvSafetyAssessmentResponse(
                assessment.hasDvHistory(),
                assessment.isCurrentlyFleeing(),
                assessment.isDvRecent(),
                assessment.isDvVeryRecent(),
                assessment.requiresEnhancedSafety(),
                assessment.getRiskLevel().name(),
                assessment.getRecommendedActions()
            );
        }
    }
    
    public record DvStatusResponse(
        boolean hasDvHistory,
        boolean isCurrentlyFleeing,
        boolean requiresEnhancedSafety
    ) {}
    
    public record SafetyAlertResponse(
        UUID enrollmentId,
        String message,
        Instant createdAt
    ) {}
    
    public record DvComplianceResponse(
        boolean meetsCompliance,
        boolean hasProjectStartRecord,
        boolean hasProjectExitRecord,
        String issueDescription
    ) {}
}