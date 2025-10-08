package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.programenrollment.application.services.BedNightService;
import org.haven.programenrollment.application.services.BedNightService.*;
import org.haven.programenrollment.domain.BedNight;
import org.springframework.format.annotation.DateTimeFormat;
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
 * REST API for Bed Night management
 * Handles Emergency Shelter Night-by-Night (ES-NbN) bed tracking
 * Critical for bed utilization reporting and client service tracking
 */
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/bed-nights")
@Tag(name = "Bed Nights", description = "Emergency Shelter Night-by-Night (ES-NbN) bed tracking")
public class BedNightController {
    
    private final BedNightService bedNightService;
    
    public BedNightController(@Lazy BedNightService bedNightService) {
        this.bedNightService = bedNightService;
    }
    
    @Operation(
        summary = "Record single bed night",
        description = "Records a single bed night for the enrollment"
    )
    @PostMapping
    public ResponseEntity<BedNightResponse> recordBedNight(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateBedNightRequest request) {
        
        try {
            BedNight record = bedNightService.recordBedNight(
                enrollmentId,
                request.bedNightDate(),
                request.createdBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(BedNightResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Record bed night range",
        description = "Records consecutive bed nights for a date range"
    )
    @PostMapping("/range")
    public ResponseEntity<BedNightRangeResponse> recordBedNightRange(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateBedNightRangeRequest request) {
        
        try {
            List<BedNight> records = bedNightService.recordBedNightRange(
                enrollmentId,
                request.startDate(),
                request.endDate(),
                request.createdBy()
            );
            
            List<BedNightResponse> responses = records.stream()
                .map(BedNightResponse::from)
                .toList();
            
            BedNightRangeResponse response = new BedNightRangeResponse(
                responses,
                String.format("Successfully recorded %d bed nights from %s to %s", 
                    responses.size(), request.startDate(), request.endDate())
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Record bulk bed nights",
        description = "Records multiple bed nights efficiently for data import/bulk operations"
    )
    @PostMapping("/bulk")
    public ResponseEntity<BulkBedNightResponse> recordBulkBedNights(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody BulkBedNightRequest request) {
        
        try {
            List<BedNightRequest> bedNightRequests = request.bedNights().stream()
                .map(item -> new BedNightRequest(
                    enrollmentId,
                    item.bedNightDate(),
                    item.createdBy()
                ))
                .toList();
            
            List<BedNight> createdRecords = bedNightService.recordBulkBedNights(bedNightRequests);
            
            List<BedNightResponse> responses = createdRecords.stream()
                .map(BedNightResponse::from)
                .toList();
            
            BulkBedNightResponse response = new BulkBedNightResponse(
                responses,
                String.format("Successfully created %d bed night records", responses.size()),
                request.bedNights().size() - responses.size() // failed count
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get all bed nights for enrollment",
        description = "Retrieves all bed nights for the enrollment"
    )
    @GetMapping
    public ResponseEntity<List<BedNightResponse>> getAllBedNights(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            List<BedNight> records = bedNightService.getBedNightsForEnrollment(enrollmentId);
            
            List<BedNightResponse> response = records.stream()
                .map(BedNightResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get bed nights within date range",
        description = "Retrieves bed nights within specified date range"
    )
    @GetMapping("/date-range")
    public ResponseEntity<List<BedNightResponse>> getBedNightsInDateRange(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<BedNight> records = bedNightService
                .getBedNightsForEnrollmentInDateRange(enrollmentId, startDate, endDate);
            
            List<BedNightResponse> response = records.stream()
                .map(BedNightResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get specific bed night",
        description = "Retrieves bed night for specific date"
    )
    @GetMapping("/date/{bedNightDate}")
    public ResponseEntity<BedNightResponse> getBedNight(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Bed night date")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bedNightDate) {
        
        try {
            BedNight record = bedNightService.getBedNight(enrollmentId, bedNightDate);
            
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(BedNightResponse.from(record));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Check if bed night exists",
        description = "Checks if bed night exists for specific date"
    )
    @GetMapping("/exists/{bedNightDate}")
    public ResponseEntity<BedNightExistsResponse> checkBedNightExists(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Bed night date")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bedNightDate) {
        
        try {
            boolean exists = bedNightService.hasBedNight(enrollmentId, bedNightDate);
            
            BedNightExistsResponse response = new BedNightExistsResponse(exists, bedNightDate);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Count bed nights",
        description = "Counts total bed nights for the enrollment"
    )
    @GetMapping("/count")
    public ResponseEntity<BedNightCountResponse> countBedNights(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            long count = bedNightService.countBedNightsForEnrollment(enrollmentId);
            
            BedNightCountResponse response = new BedNightCountResponse(count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Remove bed night",
        description = "Removes a bed night record for specific date"
    )
    @DeleteMapping("/date/{bedNightDate}")
    public ResponseEntity<Void> removeBedNight(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Bed night date")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bedNightDate) {
        
        try {
            bedNightService.removeBedNight(enrollmentId, bedNightDate);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Analytics and Reporting Endpoints
    
    @Operation(
        summary = "Get bed utilization metrics",
        description = "Calculates bed utilization metrics for the enrollment or system-wide"
    )
    @GetMapping("/metrics/utilization")
    public ResponseEntity<BedUtilizationMetricsResponse> getBedUtilizationMetrics(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            BedUtilizationMetrics metrics = bedNightService
                .calculateBedUtilizationMetrics(startDate, endDate);
            
            BedUtilizationMetricsResponse response = BedUtilizationMetricsResponse.from(metrics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get consecutive nights analysis",
        description = "Analyzes consecutive bed night patterns for the enrollment"
    )
    @GetMapping("/analytics/consecutive-nights")
    public ResponseEntity<ConsecutiveNightsAnalysisResponse> getConsecutiveNightsAnalysis(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            ConsecutiveNightsAnalysis analysis = bedNightService
                .getConsecutiveNightsAnalysis(enrollmentId);
            
            ConsecutiveNightsAnalysisResponse response = ConsecutiveNightsAnalysisResponse.from(analysis);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get bed night pattern",
        description = "Identifies bed night usage pattern for the enrollment"
    )
    @GetMapping("/analytics/pattern")
    public ResponseEntity<BedNightPatternResponse> getBedNightPattern(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            BedNightPattern pattern = bedNightService.getBedNightPattern(enrollmentId);
            
            BedNightPatternResponse response = new BedNightPatternResponse(pattern);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get bed availability report",
        description = "Generates bed availability report for specific date"
    )
    @GetMapping("/reports/availability")
    public ResponseEntity<BedAvailabilityReportResponse> getBedAvailabilityReport(
            @Parameter(description = "Report date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @Parameter(description = "Total bed capacity")
            @RequestParam int totalBedCapacity) {
        
        try {
            BedAvailabilityReport report = bedNightService
                .getBedAvailabilityReport(reportDate, totalBedCapacity);
            
            BedAvailabilityReportResponse response = BedAvailabilityReportResponse.from(report);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // System-wide endpoints
    
    @Operation(
        summary = "Find enrollments with bed night gaps",
        description = "Identifies enrollments with significant gaps in bed night tracking"
    )
    @GetMapping("/system/gaps")
    public ResponseEntity<List<UUID>> findEnrollmentsWithBedNightGaps(
            @Parameter(description = "Minimum gap in days")
            @RequestParam(defaultValue = "7") int minGapDays) {
        
        try {
            List<UUID> enrollments = bedNightService.findEnrollmentsWithBedNightGaps(minGapDays);
            return ResponseEntity.ok(enrollments);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request/Response DTOs
    
    public record CreateBedNightRequest(
        @NotNull LocalDate bedNightDate,
        @NotNull String createdBy
    ) {}
    
    public record CreateBedNightRangeRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull String createdBy
    ) {}
    
    public record BulkBedNightItemRequest(
        @NotNull LocalDate bedNightDate,
        @NotNull String createdBy
    ) {}
    
    public record BulkBedNightRequest(
        @NotNull List<BulkBedNightItemRequest> bedNights
    ) {}
    
    public record BedNightResponse(
        UUID recordId,
        UUID enrollmentId,
        UUID clientId,
        LocalDate bedNightDate,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static BedNightResponse from(BedNight record) {
            return new BedNightResponse(
                record.getRecordId(),
                record.getEnrollmentId().value(),
                record.getClientId().value(),
                record.getBedNightDate(),
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt()
            );
        }
    }
    
    public record BedNightRangeResponse(
        List<BedNightResponse> bedNights,
        String message
    ) {}
    
    public record BulkBedNightResponse(
        List<BedNightResponse> bedNights,
        String message,
        int failedCount
    ) {}
    
    public record BedNightExistsResponse(
        boolean exists,
        LocalDate bedNightDate
    ) {}
    
    public record BedNightCountResponse(
        long totalBedNights
    ) {}
    
    public record BedUtilizationMetricsResponse(
        long totalBedNights,
        int uniqueClients,
        int uniqueEnrollments,
        double averageDailyUtilization,
        LocalDate startDate,
        LocalDate endDate
    ) {
        public static BedUtilizationMetricsResponse from(BedUtilizationMetrics metrics) {
            return new BedUtilizationMetricsResponse(
                metrics.totalBedNights(),
                metrics.uniqueClients(),
                metrics.uniqueEnrollments(),
                metrics.averageDailyUtilization(),
                metrics.startDate(),
                metrics.endDate()
            );
        }
    }
    
    public record ConsecutiveNightsAnalysisResponse(
        int totalNights,
        int longestStreak,
        LocalDate longestStreakStart,
        LocalDate longestStreakEnd,
        int totalGaps,
        double continuityRatio
    ) {
        public static ConsecutiveNightsAnalysisResponse from(ConsecutiveNightsAnalysis analysis) {
            double continuityRatio = analysis.totalNights() > 0 ? 
                (double) analysis.longestStreak() / analysis.totalNights() : 0.0;
            
            return new ConsecutiveNightsAnalysisResponse(
                analysis.totalNights(),
                analysis.longestStreak(),
                analysis.longestStreakStart(),
                analysis.longestStreakEnd(),
                analysis.totalGaps(),
                continuityRatio
            );
        }
    }
    
    public record BedNightPatternResponse(
        BedNightPattern pattern
    ) {}
    
    public record BedAvailabilityReportResponse(
        LocalDate reportDate,
        long totalBedCapacity,
        long bedsUsed,
        long bedsAvailable,
        double occupancyRate
    ) {
        public static BedAvailabilityReportResponse from(BedAvailabilityReport report) {
            return new BedAvailabilityReportResponse(
                report.reportDate(),
                report.totalBedCapacity(),
                report.bedsUsed(),
                report.bedsAvailable(),
                report.occupancyRate()
            );
        }
    }
}
