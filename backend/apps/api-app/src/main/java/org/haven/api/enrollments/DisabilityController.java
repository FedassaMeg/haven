package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.programenrollment.application.services.DisabilityLifecycleService;
import org.haven.shared.vo.hmis.DisabilityKind;
import org.haven.programenrollment.domain.DisabilityRecord;
import org.haven.shared.vo.hmis.HmisFivePoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Lazy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API for Disability lifecycle management (all 6 types)
 * Supports HMIS FY2024 UDE 3.09-3.13 Disability data collection
 */
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/disabilities")
@Tag(name = "Disabilities", description = "HMIS Disability (UDE 3.09-3.13) lifecycle management for all 6 disability types")
public class DisabilityController {
    
    private final DisabilityLifecycleService disabilityService;
    
    public DisabilityController(@Lazy DisabilityLifecycleService disabilityService) {
        this.disabilityService = disabilityService;
    }
    
    @Operation(
        summary = "Create all disability records at project start",
        description = "Creates PROJECT_START records for all 6 disability types (HMIS required)"
    )
    @PostMapping("/start/all")
    public ResponseEntity<BulkDisabilityResponse> createAllProjectStartRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateAllDisabilityStartRequest request) {
        
        try {
            disabilityService.createAllProjectStartRecords(
                enrollmentId,
                request.physicalDisability(),
                request.developmentalDisability(),
                request.chronicHealthCondition(),
                request.hivAids(),
                request.mentalHealthDisorder(),
                request.substanceUseDisorder(),
                request.collectedBy()
            );
            
            // Get all created records
            List<DisabilityRecord> allRecords = disabilityService
                .getDisabilityRecordsForEnrollment(enrollmentId);
            
            List<DisabilityResponse> responses = allRecords.stream()
                .map(DisabilityResponse::from)
                .toList();
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BulkDisabilityResponse(responses, "All disability records created successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create all disability records at project exit",
        description = "Creates PROJECT_EXIT records for all 6 disability types (HMIS required)"
    )
    @PostMapping("/exit/all")
    public ResponseEntity<BulkDisabilityResponse> createAllProjectExitRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateAllDisabilityExitRequest request) {
        
        try {
            disabilityService.createAllProjectExitRecords(
                enrollmentId,
                request.exitDate(),
                request.physicalDisability(),
                request.developmentalDisability(),
                request.chronicHealthCondition(),
                request.hivAids(),
                request.mentalHealthDisorder(),
                request.substanceUseDisorder(),
                request.collectedBy()
            );
            
            // Get all created records
            List<DisabilityRecord> allRecords = disabilityService
                .getDisabilityRecordsForEnrollment(enrollmentId);
            
            List<DisabilityResponse> responses = allRecords.stream()
                .filter(record -> record.getStage().name().equals("PROJECT_EXIT"))
                .map(DisabilityResponse::from)
                .toList();
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BulkDisabilityResponse(responses, "All exit disability records created successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create disability record at project start",
        description = "Creates PROJECT_START disability record for specific disability type"
    )
    @PostMapping("/{disabilityKind}/start")
    public ResponseEntity<DisabilityResponse> createProjectStartRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Disability kind")
            @PathVariable DisabilityKind disabilityKind,
            @Valid @RequestBody CreateDisabilityRequest request) {
        
        try {
            DisabilityRecord record = disabilityService.createProjectStartRecord(
                enrollmentId,
                disabilityKind,
                request.hasDisability(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisabilityResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create disability update record",
        description = "Creates UPDATE disability record due to change in circumstances"
    )
    @PostMapping("/{disabilityKind}/update")
    public ResponseEntity<DisabilityResponse> createUpdateRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Disability kind")
            @PathVariable DisabilityKind disabilityKind,
            @Valid @RequestBody CreateDisabilityUpdateRequest request) {
        
        try {
            DisabilityRecord record = disabilityService.createUpdateRecord(
                enrollmentId,
                disabilityKind,
                request.changeDate(),
                request.hasDisability(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisabilityResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create disability record at project exit",
        description = "Creates PROJECT_EXIT disability record for specific disability type"
    )
    @PostMapping("/{disabilityKind}/exit")
    public ResponseEntity<DisabilityResponse> createProjectExitRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Disability kind")
            @PathVariable DisabilityKind disabilityKind,
            @Valid @RequestBody CreateDisabilityExitRequest request) {
        
        try {
            DisabilityRecord record = disabilityService.createProjectExitRecord(
                enrollmentId,
                disabilityKind,
                request.exitDate(),
                request.hasDisability(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisabilityResponse.from(record));
            
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
        description = "Creates a correction record for an existing disability record"
    )
    @PostMapping("/correct/{recordId}")
    public ResponseEntity<DisabilityResponse> createCorrectionRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Original record ID to correct")
            @PathVariable UUID recordId,
            @Valid @RequestBody CreateDisabilityCorrectionRequest request) {
        
        try {
            DisabilityRecord record = disabilityService.createCorrectionRecord(
                recordId,
                request.hasDisability(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisabilityResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get latest disability record",
        description = "Retrieves the latest effective disability record for specific disability type"
    )
    @GetMapping("/{disabilityKind}/latest")
    public ResponseEntity<DisabilityResponse> getLatestDisabilityRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Disability kind")
            @PathVariable DisabilityKind disabilityKind) {
        
        try {
            DisabilityRecord record = disabilityService
                .getLatestEffectiveDisabilityRecord(enrollmentId, disabilityKind);
            
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(DisabilityResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get all disability records for specific type",
        description = "Retrieves all disability records for the enrollment and disability type"
    )
    @GetMapping("/{disabilityKind}")
    public ResponseEntity<List<DisabilityResponse>> getDisabilityRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Disability kind")
            @PathVariable DisabilityKind disabilityKind) {
        
        try {
            List<DisabilityRecord> records = disabilityService
                .getDisabilityRecordsForEnrollment(enrollmentId, disabilityKind);
            
            List<DisabilityResponse> response = records.stream()
                .map(DisabilityResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get all disability records",
        description = "Retrieves all disability records for the enrollment (all 6 types)"
    )
    @GetMapping
    public ResponseEntity<List<DisabilityResponse>> getAllDisabilityRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            List<DisabilityRecord> records = disabilityService
                .getDisabilityRecordsForEnrollment(enrollmentId);
            
            List<DisabilityResponse> response = records.stream()
                .map(DisabilityResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get current disabling conditions",
        description = "Retrieves all current disabling conditions for the enrollment"
    )
    @GetMapping("/disabling-conditions")
    public ResponseEntity<DisablingConditionsResponse> getCurrentDisablingConditions(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            List<DisabilityKind> disablingConditions = disabilityService
                .getCurrentDisablingConditions(enrollmentId);
            
            boolean hasAnyDisability = disabilityService.hasAnyDisablingCondition(enrollmentId);
            boolean hasBehavioralHealth = disabilityService.hasBehavioralHealthDisabilities(enrollmentId);
            boolean hasMedical = disabilityService.hasMedicalDisabilities(enrollmentId);
            
            DisablingConditionsResponse response = new DisablingConditionsResponse(
                disablingConditions,
                hasAnyDisability,
                hasBehavioralHealth,
                hasMedical
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Check compliance status",
        description = "Checks if enrollment meets HMIS disability data compliance requirements"
    )
    @GetMapping("/compliance")
    public ResponseEntity<DisabilityComplianceResponse> checkComplianceStatus(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            boolean meetsCompliance = disabilityService.meetsDisabilityDataCompliance(enrollmentId);
            
            // Check each disability kind
            List<DisabilityKindComplianceStatus> kindStatuses = List.of(DisabilityKind.values())
                .stream()
                .map(kind -> new DisabilityKindComplianceStatus(
                    kind,
                    disabilityService.hasProjectStartRecord(enrollmentId, kind),
                    disabilityService.hasProjectExitRecord(enrollmentId, kind)
                ))
                .toList();
            
            DisabilityComplianceResponse response = new DisabilityComplianceResponse(
                meetsCompliance,
                kindStatuses
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request/Response DTOs
    
    public record CreateAllDisabilityStartRequest(
        @NotNull HmisFivePoint physicalDisability,
        @NotNull HmisFivePoint developmentalDisability,
        @NotNull HmisFivePoint chronicHealthCondition,
        @NotNull HmisFivePoint hivAids,
        @NotNull HmisFivePoint mentalHealthDisorder,
        @NotNull HmisFivePoint substanceUseDisorder,
        @NotNull String collectedBy
    ) {}
    
    public record CreateAllDisabilityExitRequest(
        @NotNull LocalDate exitDate,
        @NotNull HmisFivePoint physicalDisability,
        @NotNull HmisFivePoint developmentalDisability,
        @NotNull HmisFivePoint chronicHealthCondition,
        @NotNull HmisFivePoint hivAids,
        @NotNull HmisFivePoint mentalHealthDisorder,
        @NotNull HmisFivePoint substanceUseDisorder,
        @NotNull String collectedBy
    ) {}
    
    public record CreateDisabilityRequest(
        @NotNull HmisFivePoint hasDisability,
        @NotNull String collectedBy
    ) {}
    
    public record CreateDisabilityUpdateRequest(
        @NotNull LocalDate changeDate,
        @NotNull HmisFivePoint hasDisability,
        @NotNull String collectedBy
    ) {}
    
    public record CreateDisabilityExitRequest(
        @NotNull LocalDate exitDate,
        @NotNull HmisFivePoint hasDisability,
        @NotNull String collectedBy
    ) {}
    
    public record CreateDisabilityCorrectionRequest(
        @NotNull HmisFivePoint hasDisability,
        @NotNull String collectedBy
    ) {}
    
    public record DisabilityResponse(
        UUID recordId,
        UUID enrollmentId,
        UUID clientId,
        LocalDate informationDate,
        String stage,
        DisabilityKind disabilityKind,
        HmisFivePoint hasDisability,
        boolean isCorrection,
        UUID correctsRecordId,
        String collectedBy,
        Instant createdAt,
        Instant updatedAt,
        boolean meetsDataQuality
    ) {
        public static DisabilityResponse from(DisabilityRecord record) {
            return new DisabilityResponse(
                record.getRecordId(),
                record.getEnrollmentId().value(),
                record.getClientId().value(),
                record.getInformationDate(),
                record.getStage().getDisplayName(),
                record.getDisabilityKind(),
                record.getHasDisability(),
                record.isCorrection(),
                record.getCorrectsRecordId(),
                record.getCollectedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.meetsDataQuality()
            );
        }
    }
    
    public record BulkDisabilityResponse(
        List<DisabilityResponse> records,
        String message
    ) {}
    
    public record DisablingConditionsResponse(
        List<DisabilityKind> currentDisablingConditions,
        boolean hasAnyDisablingCondition,
        boolean hasBehavioralHealthDisabilities,
        boolean hasMedicalDisabilities
    ) {}
    
    public record DisabilityKindComplianceStatus(
        DisabilityKind disabilityKind,
        boolean hasProjectStartRecord,
        boolean hasProjectExitRecord
    ) {}
    
    public record DisabilityComplianceResponse(
        boolean meetsCompliance,
        List<DisabilityKindComplianceStatus> disabilityKindStatuses
    ) {}
}
