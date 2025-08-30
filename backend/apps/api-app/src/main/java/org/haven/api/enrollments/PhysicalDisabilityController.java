package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.programenrollment.application.services.PhysicalDisabilityLifecycleService;
import org.haven.programenrollment.domain.PhysicalDisabilityRecord;
import org.haven.shared.vo.hmis.HmisFivePointResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API for Physical Disability lifecycle management
 * Supports HMIS FY2024 UDE 3.08 Physical Disability data collection
 */
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/disabilities/physical")
@Tag(name = "Physical Disabilities", description = "HMIS Physical Disability (UDE 3.08) lifecycle management")
public class PhysicalDisabilityController {
    
    private final PhysicalDisabilityLifecycleService physicalDisabilityService;
    
    public PhysicalDisabilityController(PhysicalDisabilityLifecycleService physicalDisabilityService) {
        this.physicalDisabilityService = physicalDisabilityService;
    }
    
    @Operation(
        summary = "Create physical disability record at project start",
        description = "Creates PROJECT_START physical disability record (HMIS required)"
    )
    @PostMapping("/start")
    public ResponseEntity<PhysicalDisabilityResponse> createProjectStartRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreatePhysicalDisabilityRequest request) {
        
        try {
            PhysicalDisabilityRecord record = physicalDisabilityService.createProjectStartRecord(
                enrollmentId,
                request.physicalDisability(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(PhysicalDisabilityResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create physical disability update record",
        description = "Creates UPDATE physical disability record due to change in circumstances"
    )
    @PostMapping("/update")
    public ResponseEntity<PhysicalDisabilityResponse> createUpdateRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreatePhysicalDisabilityUpdateRequest request) {
        
        try {
            PhysicalDisabilityRecord record = physicalDisabilityService.createUpdateRecord(
                enrollmentId,
                request.changeDate(),
                request.physicalDisability(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(PhysicalDisabilityResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Create physical disability record at project exit",
        description = "Creates PROJECT_EXIT physical disability record (HMIS required)"
    )
    @PostMapping("/exit")
    public ResponseEntity<PhysicalDisabilityResponse> createProjectExitRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreatePhysicalDisabilityExitRequest request) {
        
        try {
            PhysicalDisabilityRecord record = physicalDisabilityService.createProjectExitRecord(
                enrollmentId,
                request.exitDate(),
                request.physicalDisability(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(PhysicalDisabilityResponse.from(record));
            
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
        description = "Creates a correction record for an existing physical disability record"
    )
    @PostMapping("/correct/{recordId}")
    public ResponseEntity<PhysicalDisabilityResponse> createCorrectionRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Original record ID to correct")
            @PathVariable UUID recordId,
            @Valid @RequestBody CreatePhysicalDisabilityCorrectionRequest request) {
        
        try {
            PhysicalDisabilityRecord record = physicalDisabilityService.createCorrectionRecord(
                recordId,
                request.physicalDisability(),
                request.collectedBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(PhysicalDisabilityResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Update physical expected long-term response",
        description = "Updates the physical expected long-term response for an existing record"
    )
    @PatchMapping("/records/{recordId}/expected-long-term")
    public ResponseEntity<Void> updatePhysicalExpectedLongTerm(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Physical disability record ID")
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateExpectedLongTermRequest request) {
        
        try {
            physicalDisabilityService.updatePhysicalExpectedLongTerm(
                recordId,
                request.expectedLongTerm()
            );
            
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Update physical disability status",
        description = "Updates both physical disability and expected long-term responses for an existing record"
    )
    @PatchMapping("/records/{recordId}/status")
    public ResponseEntity<Void> updatePhysicalDisabilityStatus(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Physical disability record ID")
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdatePhysicalDisabilityStatusRequest request) {
        
        try {
            physicalDisabilityService.updatePhysicalDisabilityStatus(
                recordId,
                request.physicalDisability(),
                request.expectedLongTerm()
            );
            
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get latest physical disability record",
        description = "Retrieves the latest effective physical disability record for the enrollment"
    )
    @GetMapping("/latest")
    public ResponseEntity<PhysicalDisabilityResponse> getLatestPhysicalDisabilityRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            PhysicalDisabilityRecord record = physicalDisabilityService
                .getLatestEffectivePhysicalDisabilityRecord(enrollmentId);
            
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(PhysicalDisabilityResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get all physical disability records",
        description = "Retrieves all physical disability records for the enrollment, ordered by date descending"
    )
    @GetMapping
    public ResponseEntity<List<PhysicalDisabilityResponse>> getAllPhysicalDisabilityRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            List<PhysicalDisabilityRecord> records = physicalDisabilityService
                .getPhysicalDisabilityRecordsForEnrollment(enrollmentId);
            
            List<PhysicalDisabilityResponse> response = records.stream()
                .map(PhysicalDisabilityResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get correction records",
        description = "Retrieves all correction records for a specific original record"
    )
    @GetMapping("/records/{recordId}/corrections")
    public ResponseEntity<List<PhysicalDisabilityResponse>> getCorrectionRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Original record ID")
            @PathVariable UUID recordId) {
        
        try {
            List<PhysicalDisabilityRecord> corrections = physicalDisabilityService
                .getCorrectionRecords(recordId);
            
            List<PhysicalDisabilityResponse> response = corrections.stream()
                .map(PhysicalDisabilityResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Check compliance status",
        description = "Checks if enrollment meets HMIS physical disability data compliance requirements"
    )
    @GetMapping("/compliance")
    public ResponseEntity<ComplianceStatusResponse> checkComplianceStatus(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            boolean meetsCompliance = physicalDisabilityService
                .meetsPhysicalDisabilityDataCompliance(enrollmentId);
            
            boolean hasStart = physicalDisabilityService.hasProjectStartRecord(enrollmentId);
            boolean hasExit = physicalDisabilityService.hasProjectExitRecord(enrollmentId);
            
            ComplianceStatusResponse response = new ComplianceStatusResponse(
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
    
    // Request/Response DTOs
    
    public record CreatePhysicalDisabilityRequest(
        @NotNull HmisFivePointResponse physicalDisability,
        @NotNull String collectedBy
    ) {}
    
    public record CreatePhysicalDisabilityUpdateRequest(
        @NotNull LocalDate changeDate,
        @NotNull HmisFivePointResponse physicalDisability,
        @NotNull String collectedBy
    ) {}
    
    public record CreatePhysicalDisabilityExitRequest(
        @NotNull LocalDate exitDate,
        @NotNull HmisFivePointResponse physicalDisability,
        @NotNull String collectedBy
    ) {}
    
    public record CreatePhysicalDisabilityCorrectionRequest(
        @NotNull HmisFivePointResponse physicalDisability,
        @NotNull String collectedBy
    ) {}
    
    public record UpdateExpectedLongTermRequest(
        @NotNull HmisFivePointResponse expectedLongTerm
    ) {}
    
    public record UpdatePhysicalDisabilityStatusRequest(
        @NotNull HmisFivePointResponse physicalDisability,
        HmisFivePointResponse expectedLongTerm
    ) {}
    
    public record PhysicalDisabilityResponse(
        UUID recordId,
        UUID enrollmentId,
        UUID clientId,
        LocalDate informationDate,
        String stage,
        HmisFivePointResponse physicalDisability,
        HmisFivePointResponse physicalExpectedLongTerm,
        boolean isCorrection,
        UUID correctsRecordId,
        String collectedBy,
        Instant createdAt,
        Instant updatedAt,
        boolean meetsDataQuality,
        boolean indicatesDisablingCondition
    ) {
        public static PhysicalDisabilityResponse from(PhysicalDisabilityRecord record) {
            return new PhysicalDisabilityResponse(
                record.getRecordId(),
                record.getEnrollmentId().value(),
                record.getClientId().value(),
                record.getInformationDate(),
                record.getStage().getDisplayName(),
                record.getPhysicalDisability(),
                record.getPhysicalExpectedLongTerm(),
                record.isCorrection(),
                record.getCorrectsRecordId(),
                record.getCollectedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.meetsDataQuality(),
                record.indicatesDisablingCondition()
            );
        }
    }
    
    public record ComplianceStatusResponse(
        boolean meetsCompliance,
        boolean hasProjectStartRecord,
        boolean hasProjectExitRecord,
        String issueDescription
    ) {}
}