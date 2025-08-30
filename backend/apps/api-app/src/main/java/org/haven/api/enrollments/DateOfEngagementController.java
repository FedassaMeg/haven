package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.programenrollment.application.services.DateOfEngagementService;
import org.haven.programenrollment.application.services.DateOfEngagementService.*;
import org.haven.programenrollment.domain.DateOfEngagement;
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
 * REST API for Date of Engagement management
 * Handles engagement date tracking with correction support
 * Critical for program effectiveness measurement and coordinated entry
 */
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/date-of-engagement")
@Tag(name = "Date of Engagement", description = "Engagement date tracking with correction support")
public class DateOfEngagementController {
    
    private final DateOfEngagementService engagementService;
    
    public DateOfEngagementController(DateOfEngagementService engagementService) {
        this.engagementService = engagementService;
    }
    
    @Operation(
        summary = "Set date of engagement",
        description = "Sets the initial date of engagement for the enrollment"
    )
    @PostMapping
    public ResponseEntity<DateOfEngagementResponse> setDateOfEngagement(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody SetDateOfEngagementRequest request) {
        
        try {
            DateOfEngagement record = engagementService.setDateOfEngagement(
                enrollmentId,
                request.engagementDate(),
                request.createdBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DateOfEngagementResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Correct date of engagement",
        description = "Creates a correction record for the existing date of engagement"
    )
    @PostMapping("/correct")
    public ResponseEntity<DateOfEngagementResponse> correctDateOfEngagement(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CorrectDateOfEngagementRequest request) {
        
        try {
            DateOfEngagement record = engagementService.correctDateOfEngagement(
                enrollmentId,
                request.newEngagementDate(),
                request.createdBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(DateOfEngagementResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get current date of engagement",
        description = "Retrieves the current effective date of engagement for the enrollment"
    )
    @GetMapping
    public ResponseEntity<DateOfEngagementResponse> getDateOfEngagement(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            DateOfEngagement record = engagementService.getDateOfEngagement(enrollmentId);
            
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(DateOfEngagementResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get effective engagement date",
        description = "Retrieves just the effective engagement date (LocalDate) for the enrollment"
    )
    @GetMapping("/effective-date")
    public ResponseEntity<EffectiveDateResponse> getEffectiveEngagementDate(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            LocalDate effectiveDate = engagementService.getEffectiveEngagementDate(enrollmentId);
            
            if (effectiveDate == null) {
                return ResponseEntity.notFound().build();
            }
            
            EffectiveDateResponse response = new EffectiveDateResponse(effectiveDate);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get all engagement records",
        description = "Retrieves all engagement records for the enrollment (including corrections)"
    )
    @GetMapping("/all-records")
    public ResponseEntity<List<DateOfEngagementResponse>> getAllEngagementRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            List<DateOfEngagement> records = engagementService
                .getAllEngagementRecordsForEnrollment(enrollmentId);
            
            List<DateOfEngagementResponse> response = records.stream()
                .map(DateOfEngagementResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Check engagement status",
        description = "Checks if enrollment has date of engagement set and compliance status"
    )
    @GetMapping("/status")
    public ResponseEntity<EngagementStatusResponse> getEngagementStatus(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            boolean hasEngagement = engagementService.hasDateOfEngagement(enrollmentId);
            boolean meetsCompliance = engagementService.meetsEngagementCompliance(enrollmentId);
            LocalDate effectiveDate = engagementService.getEffectiveEngagementDate(enrollmentId);
            
            EngagementStatusResponse response = new EngagementStatusResponse(
                hasEngagement,
                effectiveDate,
                meetsCompliance,
                !hasEngagement ? "No engagement date set" : null
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Delete engagement record",
        description = "Deletes an engagement record (use with caution - prefer corrections)"
    )
    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteEngagementRecord(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Engagement record ID")
            @PathVariable UUID recordId) {
        
        try {
            engagementService.deleteDateOfEngagement(recordId);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get correction records",
        description = "Retrieves all correction records for a specific original engagement record"
    )
    @GetMapping("/records/{recordId}/corrections")
    public ResponseEntity<List<DateOfEngagementResponse>> getCorrectionRecords(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Original record ID")
            @PathVariable UUID recordId) {
        
        try {
            List<DateOfEngagement> corrections = engagementService.getCorrectionRecords(recordId);
            
            List<DateOfEngagementResponse> response = corrections.stream()
                .map(DateOfEngagementResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Analytics and Reporting Endpoints
    
    @Operation(
        summary = "Get engagement metrics",
        description = "Calculates engagement metrics for program effectiveness measurement"
    )
    @GetMapping("/metrics")
    public ResponseEntity<EngagementMetricsResponse> getEngagementMetrics(
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            EngagementMetrics metrics = engagementService.calculateEngagementMetrics(startDate, endDate);
            
            EngagementMetricsResponse response = EngagementMetricsResponse.from(metrics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get rapid re-housing engagement analysis",
        description = "Provides analysis of engagement timing patterns for rapid re-housing programs"
    )
    @GetMapping("/analytics/rapid-rehousing")
    public ResponseEntity<RapidReHousingEngagementAnalysisResponse> getRapidReHousingAnalysis(
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            RapidReHousingEngagementAnalysis analysis = engagementService
                .getRapidReHousingEngagementAnalysis(startDate, endDate);
            
            RapidReHousingEngagementAnalysisResponse response = 
                RapidReHousingEngagementAnalysisResponse.from(analysis);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // System-wide endpoints (not enrollment-specific)
    
    @Operation(
        summary = "Find enrollments missing engagement date",
        description = "Identifies enrollments that are missing date of engagement"
    )
    @GetMapping("/system/missing-engagement")
    public ResponseEntity<List<UUID>> findEnrollmentsMissingEngagementDate() {
        
        try {
            List<UUID> enrollments = engagementService.findEnrollmentsMissingEngagementDate();
            return ResponseEntity.ok(enrollments);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request/Response DTOs
    
    public record SetDateOfEngagementRequest(
        @NotNull LocalDate engagementDate,
        @NotNull String createdBy
    ) {}
    
    public record CorrectDateOfEngagementRequest(
        @NotNull LocalDate newEngagementDate,
        @NotNull String createdBy
    ) {}
    
    public record DateOfEngagementResponse(
        UUID recordId,
        UUID enrollmentId,
        UUID clientId,
        LocalDate engagementDate,
        boolean isCorrection,
        UUID correctsRecordId,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static DateOfEngagementResponse from(DateOfEngagement record) {
            return new DateOfEngagementResponse(
                record.getRecordId(),
                record.getEnrollmentId().value(),
                record.getClientId().value(),
                record.getEngagementDate(),
                record.isCorrection(),
                record.getCorrectsRecordId(),
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt()
            );
        }
    }
    
    public record EffectiveDateResponse(
        LocalDate effectiveEngagementDate
    ) {}
    
    public record EngagementStatusResponse(
        boolean hasDateOfEngagement,
        LocalDate effectiveEngagementDate,
        boolean meetsEngagementCompliance,
        String issueDescription
    ) {}
    
    public record EngagementMetricsResponse(
        int totalEnrollments,
        int enrollmentsWithEngagement,
        double engagementRate,
        double averageDaysToEngagement,
        LocalDate startDate,
        LocalDate endDate
    ) {
        public static EngagementMetricsResponse from(EngagementMetrics metrics) {
            return new EngagementMetricsResponse(
                metrics.totalEnrollments(),
                metrics.enrollmentsWithEngagement(),
                metrics.engagementRate(),
                metrics.averageDaysToEngagement(),
                metrics.startDate(),
                metrics.endDate()
            );
        }
    }
    
    public record RapidReHousingEngagementAnalysisResponse(
        int totalEngagements,
        long rapidEngagement,      // <= 7 days
        long standardEngagement,   // 8-30 days  
        long delayedEngagement,    // > 30 days
        double rapidEngagementRate,
        double standardEngagementRate,
        double delayedEngagementRate,
        LocalDate startDate,
        LocalDate endDate
    ) {
        public static RapidReHousingEngagementAnalysisResponse from(RapidReHousingEngagementAnalysis analysis) {
            int total = analysis.totalEngagements();
            double rapidRate = total > 0 ? (double) analysis.rapidEngagement() / total : 0.0;
            double standardRate = total > 0 ? (double) analysis.standardEngagement() / total : 0.0;
            double delayedRate = total > 0 ? (double) analysis.delayedEngagement() / total : 0.0;
            
            return new RapidReHousingEngagementAnalysisResponse(
                analysis.totalEngagements(),
                analysis.rapidEngagement(),
                analysis.standardEngagement(),
                analysis.delayedEngagement(),
                rapidRate,
                standardRate,
                delayedRate,
                analysis.startDate(),
                analysis.endDate()
            );
        }
    }
}