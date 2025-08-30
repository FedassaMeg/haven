package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.programenrollment.application.services.CurrentLivingSituationService;
import org.haven.programenrollment.application.services.CurrentLivingSituationService.*;
import org.haven.programenrollment.domain.CurrentLivingSituation;
import org.haven.programenrollment.domain.CurrentLivingSituationStatus;
import org.haven.programenrollment.domain.ChronicallyHomelessDetermination;
import org.haven.programenrollment.domain.PriorLivingSituation;
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
 * REST API for Current Living Situation management
 * Handles street outreach contact tracking and living situation assessment
 * Critical for coordinated entry and unsheltered client engagement
 */
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/current-living-situation")
@Tag(name = "Current Living Situation", description = "Street outreach contact tracking and living situation assessment")
public class CurrentLivingSituationController {
    
    private final CurrentLivingSituationService clsService;
    
    public CurrentLivingSituationController(CurrentLivingSituationService clsService) {
        this.clsService = clsService;
    }
    
    @Operation(
        summary = "Record current living situation contact",
        description = "Records a current living situation contact for street outreach"
    )
    @PostMapping("/contacts")
    public ResponseEntity<CurrentLivingSituationResponse> recordContact(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody CreateCurrentLivingSituationRequest request) {
        
        try {
            CurrentLivingSituation record = clsService.recordCurrentLivingSituation(
                enrollmentId,
                request.contactDate(),
                request.livingSituation(),
                request.lengthOfStayDays(),
                request.lengthOfStayAtTimeOfContact(),
                request.verifiedBy(),
                request.createdBy()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(CurrentLivingSituationResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Update living situation contact",
        description = "Updates an existing living situation contact with additional details"
    )
    @PutMapping("/contacts/{recordId}")
    public ResponseEntity<CurrentLivingSituationResponse> updateContact(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Contact record ID")
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateCurrentLivingSituationRequest request) {
        
        try {
            CurrentLivingSituation record = clsService.updateCurrentLivingSituation(
                recordId,
                request.livingSituation(),
                request.lengthOfStayDays(),
                request.lengthOfStayAtTimeOfContact(),
                request.verifiedBy()
            );
            
            return ResponseEntity.ok(CurrentLivingSituationResponse.from(record));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Record bulk street contacts",
        description = "Records multiple street contacts efficiently for data import/bulk operations"
    )
    @PostMapping("/contacts/bulk")
    public ResponseEntity<BulkContactResponse> recordBulkContacts(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody BulkStreetContactRequest request) {
        
        try {
            List<StreetContactRequest> contactRequests = request.contacts().stream()
                .map(contact -> new StreetContactRequest(
                    enrollmentId,
                    contact.contactDate(),
                    contact.livingSituation(),
                    contact.lengthOfStayDays(),
                    contact.lengthOfStayAtTimeOfContact(),
                    contact.verifiedBy(),
                    contact.createdBy()
                ))
                .toList();
            
            List<CurrentLivingSituation> createdRecords = clsService.recordBulkStreetContacts(contactRequests);
            
            List<CurrentLivingSituationResponse> responses = createdRecords.stream()
                .map(CurrentLivingSituationResponse::from)
                .toList();
            
            BulkContactResponse response = new BulkContactResponse(
                responses,
                String.format("Successfully created %d contact records", responses.size())
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get all contacts for enrollment",
        description = "Retrieves all living situation contacts for the enrollment"
    )
    @GetMapping("/contacts")
    public ResponseEntity<List<CurrentLivingSituationResponse>> getAllContacts(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            List<CurrentLivingSituation> records = clsService
                .getCurrentLivingSituationsForEnrollment(enrollmentId);
            
            List<CurrentLivingSituationResponse> response = records.stream()
                .map(CurrentLivingSituationResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get contacts within date range",
        description = "Retrieves living situation contacts within specified date range"
    )
    @GetMapping("/contacts/date-range")
    public ResponseEntity<List<CurrentLivingSituationResponse>> getContactsInDateRange(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<CurrentLivingSituation> records = clsService
                .getCurrentLivingSituationsForEnrollmentInDateRange(enrollmentId, startDate, endDate);
            
            List<CurrentLivingSituationResponse> response = records.stream()
                .map(CurrentLivingSituationResponse::from)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get most recent contact",
        description = "Retrieves the most recent living situation contact for the enrollment"
    )
    @GetMapping("/contacts/latest")
    public ResponseEntity<CurrentLivingSituationResponse> getMostRecentContact(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            CurrentLivingSituation record = clsService.getMostRecentCurrentLivingSituation(enrollmentId);
            
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(CurrentLivingSituationResponse.from(record));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get current living situation status",
        description = "Retrieves current living situation status and unsheltered determination"
    )
    @GetMapping("/status")
    public ResponseEntity<LivingSituationStatusResponse> getLivingSituationStatus(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            CurrentLivingSituationStatus status = clsService.getCurrentLivingSituationStatus(enrollmentId);
            boolean isCurrentlyUnsheltered = clsService.isCurrentlyUnsheltered(enrollmentId);
            boolean hasRecentContact = clsService.hasRecentStreetContact(enrollmentId, 30);
            
            LivingSituationStatusResponse response = new LivingSituationStatusResponse(
                status.getCurrentSituation(),
                status.getLastContactDate(),
                status.getDaysSinceLastContact(),
                isCurrentlyUnsheltered,
                hasRecentContact
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get chronically homeless determination",
        description = "Retrieves chronically homeless determination based on CLS history"
    )
    @GetMapping("/chronically-homeless")
    public ResponseEntity<ChronicallyHomelessResponse> getChronicallyHomelessDetermination(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId) {
        
        try {
            ChronicallyHomelessDetermination determination = clsService
                .getChronicallyHomelessDetermination(enrollmentId);
            
            ChronicallyHomelessResponse response = ChronicallyHomelessResponse.from(determination);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Delete contact record",
        description = "Deletes a living situation contact record"
    )
    @DeleteMapping("/contacts/{recordId}")
    public ResponseEntity<Void> deleteContact(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Contact record ID")
            @PathVariable UUID recordId) {
        
        try {
            clsService.deleteCurrentLivingSituation(recordId);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Reporting and Analytics Endpoints
    
    @Operation(
        summary = "Get street outreach metrics",
        description = "Retrieves street outreach engagement metrics for date range"
    )
    @GetMapping("/metrics")
    public ResponseEntity<StreetOutreachMetricsResponse> getStreetOutreachMetrics(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            StreetOutreachMetrics metrics = clsService.getStreetOutreachMetrics(startDate, endDate);
            
            StreetOutreachMetricsResponse response = StreetOutreachMetricsResponse.from(metrics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get contact counts by living situation",
        description = "Retrieves contact counts grouped by living situation type"
    )
    @GetMapping("/analytics/by-situation")
    public ResponseEntity<List<LivingSituationSummaryResponse>> getContactCountsBySituation(
            @Parameter(description = "Enrollment ID")
            @PathVariable UUID enrollmentId,
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<LivingSituationSummary> summaries = clsService
                .getContactCountsByLivingSituation(startDate, endDate);
            
            List<LivingSituationSummaryResponse> response = summaries.stream()
                .map(summary -> new LivingSituationSummaryResponse(
                    summary.livingSituation(),
                    summary.contactCount()
                ))
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request/Response DTOs
    
    public record CreateCurrentLivingSituationRequest(
        @NotNull LocalDate contactDate,
        @NotNull PriorLivingSituation livingSituation,
        Integer lengthOfStayDays,
        String lengthOfStayAtTimeOfContact,
        Boolean verifiedBy,
        @NotNull String createdBy
    ) {}
    
    public record UpdateCurrentLivingSituationRequest(
        @NotNull PriorLivingSituation livingSituation,
        Integer lengthOfStayDays,
        String lengthOfStayAtTimeOfContact,
        Boolean verifiedBy
    ) {}
    
    public record BulkStreetContactItemRequest(
        @NotNull LocalDate contactDate,
        @NotNull PriorLivingSituation livingSituation,
        Integer lengthOfStayDays,
        String lengthOfStayAtTimeOfContact,
        Boolean verifiedBy,
        @NotNull String createdBy
    ) {}
    
    public record BulkStreetContactRequest(
        @NotNull List<BulkStreetContactItemRequest> contacts
    ) {}
    
    public record CurrentLivingSituationResponse(
        UUID recordId,
        UUID enrollmentId,
        UUID clientId,
        LocalDate contactDate,
        PriorLivingSituation livingSituation,
        Integer lengthOfStayDays,
        String lengthOfStayAtTimeOfContact,
        Boolean verifiedBy,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        boolean isUnsheltered
    ) {
        public static CurrentLivingSituationResponse from(CurrentLivingSituation record) {
            return new CurrentLivingSituationResponse(
                record.getRecordId(),
                record.getEnrollmentId().value(),
                record.getClientId().value(),
                record.getContactDate(),
                record.getLivingSituation(),
                record.getLengthOfStayDays(),
                record.getLengthOfStayAtTimeOfContact(),
                record.getVerifiedBy(),
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.isUnsheltered()
            );
        }
    }
    
    public record BulkContactResponse(
        List<CurrentLivingSituationResponse> contacts,
        String message
    ) {}
    
    public record LivingSituationStatusResponse(
        PriorLivingSituation currentSituation,
        LocalDate lastContactDate,
        Integer daysSinceLastContact,
        boolean isCurrentlyUnsheltered,
        boolean hasRecentContact
    ) {}
    
    public record ChronicallyHomelessResponse(
        boolean meetsChronicallyHomelessCriteria,
        Integer totalUnshelteredDays,
        Integer continuousUnshelteredDays,
        LocalDate firstUnshelteredDate,
        LocalDate lastUnshelteredDate,
        String determination
    ) {
        public static ChronicallyHomelessResponse from(ChronicallyHomelessDetermination determination) {
            return new ChronicallyHomelessResponse(
                determination.meetsChronicallyHomelessCriteria(),
                determination.getTotalUnshelteredDays(),
                determination.getContinuousUnshelteredDays(),
                determination.getFirstUnshelteredDate(),
                determination.getLastUnshelteredDate(),
                determination.getDetermination()
            );
        }
    }
    
    public record StreetOutreachMetricsResponse(
        int totalContacts,
        int unshelteredContacts,
        int uniqueClients,
        LocalDate startDate,
        LocalDate endDate,
        double unshelteredContactRate
    ) {
        public static StreetOutreachMetricsResponse from(StreetOutreachMetrics metrics) {
            double unshelteredRate = metrics.totalContacts() > 0 ? 
                (double) metrics.unshelteredContacts() / metrics.totalContacts() : 0.0;
            
            return new StreetOutreachMetricsResponse(
                metrics.totalContacts(),
                metrics.unshelteredContacts(),
                metrics.uniqueClients(),
                metrics.startDate(),
                metrics.endDate(),
                unshelteredRate
            );
        }
    }
    
    public record LivingSituationSummaryResponse(
        PriorLivingSituation livingSituation,
        int contactCount
    ) {}
}