package org.haven.servicedelivery.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.services.*;
import org.haven.servicedelivery.domain.events.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced ServiceEpisode aggregate for comprehensive service tracking
 * Replaces CaseNote-centric flow with proper service delivery tracking
 * Includes duration, funding, program association, and outcome tracking
 */
public class ServiceEpisode extends AggregateRoot<ServiceEpisodeId> {
    
    private ClientId clientId;
    private String enrollmentId; // Links to ProgramEnrollment
    private String programId;
    private String programName;
    
    // Service Details
    private ServiceType serviceType;
    private ServiceCategory serviceCategory;
    private ServiceDeliveryMode deliveryMode;
    private LocalDate serviceDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer plannedDurationMinutes;
    private Integer actualDurationMinutes;
    
    // Provider Information
    private String primaryProviderId;
    private String primaryProviderName;
    private List<String> additionalProviderIds = new ArrayList<>();
    private String supervisorId;
    
    // Funding & Billing
    private FundingSource primaryFundingSource;
    private List<FundingSource> additionalFundingSources = new ArrayList<>();
    private String onBehalfOfOrganization; // For contracted services
    private boolean isBillable;
    private String billingCode;
    private Double billingRate;
    private Double totalBillableAmount;
    
    // Service Content & Outcomes
    private String serviceDescription;
    private String serviceGoals;
    private String serviceOutcome;
    private ServiceCompletionStatus completionStatus;
    private String followUpRequired;
    private LocalDate followUpDate;
    private String notes;
    
    // Privacy & Confidentiality
    private boolean isConfidential;
    private String confidentialityReason;
    private boolean isRestrictedAccess;
    private List<String> authorizedViewerIds = new ArrayList<>();
    
    // Location & Context
    private String serviceLocation;
    private String serviceLocationAddress;
    private boolean isOffSite;
    private String contextNotes;
    
    // Quality & Compliance
    private boolean isCourtOrdered;
    private String courtOrderNumber;
    private boolean requiresDocumentation;
    private List<String> attachedDocumentIds = new ArrayList<>();
    private String qualityAssuranceNotes;
    
    // Tracking & Metadata
    private Instant createdAt;
    private Instant lastModifiedAt;
    private String createdBy;
    private String lastModifiedBy;

    public static ServiceEpisode create(
            ClientId clientId,
            String enrollmentId,
            String programId,
            String programName,
            ServiceType serviceType,
            ServiceDeliveryMode deliveryMode,
            LocalDate serviceDate,
            Integer plannedDurationMinutes,
            String primaryProviderId,
            String primaryProviderName,
            FundingSource fundingSource,
            String serviceDescription,
            boolean isConfidential,
            String createdBy) {
        
        ServiceEpisodeId episodeId = ServiceEpisodeId.generate();
        ServiceEpisode episode = new ServiceEpisode();
        
        episode.apply(new ServiceEpisodeCreated(
            episodeId.value(),
            clientId.value(),
            enrollmentId,
            programId,
            programName,
            serviceType,
            deliveryMode,
            serviceDate,
            plannedDurationMinutes,
            primaryProviderId,
            primaryProviderName,
            fundingSource,
            serviceDescription,
            isConfidential,
            createdBy,
            Instant.now()
        ));
        
        return episode;
    }

    public void startService(LocalDateTime startTime, String location) {
        if (this.startTime != null) {
            throw new IllegalStateException("Service has already been started");
        }
        
        apply(new ServiceStarted(
            id.value(),
            startTime,
            location,
            Instant.now()
        ));
    }

    public void completeService(LocalDateTime endTime, String outcome, 
                               ServiceCompletionStatus status, String notes) {
        if (this.startTime == null) {
            throw new IllegalStateException("Cannot complete service that hasn't been started");
        }
        if (this.endTime != null) {
            throw new IllegalStateException("Service has already been completed");
        }
        
        Integer actualDuration = calculateDuration(startTime, endTime);
        Double billableAmount = calculateBillableAmount(actualDuration);
        
        apply(new ServiceCompleted(
            id.value(),
            endTime,
            actualDuration,
            outcome,
            status,
            notes,
            billableAmount,
            Instant.now()
        ));
    }

    public void addProvider(String providerId, String providerName, String role) {
        apply(new ProviderAdded(
            id.value(),
            providerId,
            providerName,
            role,
            Instant.now()
        ));
    }

    public void addFundingSource(FundingSource fundingSource, double allocationPercentage) {
        if (allocationPercentage <= 0 || allocationPercentage > 100) {
            throw new IllegalArgumentException("Allocation percentage must be between 0 and 100");
        }
        
        apply(new FundingSourceAdded(
            id.value(),
            fundingSource,
            allocationPercentage,
            Instant.now()
        ));
    }

    public void updateOutcome(String outcome, String followUpRequired, LocalDate followUpDate) {
        apply(new ServiceOutcomeUpdated(
            id.value(),
            outcome,
            followUpRequired,
            followUpDate,
            lastModifiedBy,
            Instant.now()
        ));
    }

    public void attachDocument(String documentId, String documentType, String description) {
        apply(new DocumentAttached(
            id.value(),
            documentId,
            documentType,
            description,
            Instant.now()
        ));
    }

    public void markAsCourtOrdered(String courtOrderNumber) {
        apply(new ServiceMarkedCourtOrdered(
            id.value(),
            courtOrderNumber,
            Instant.now()
        ));
    }

    private Integer calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return null;
        return (int) java.time.Duration.between(start, end).toMinutes();
    }

    private Double calculateBillableAmount(Integer durationMinutes) {
        if (!isBillable || billingRate == null || durationMinutes == null) {
            return 0.0;
        }
        
        double hours = durationMinutes / 60.0;
        double modeMultiplier = deliveryMode.getBillingMultiplier();
        return billingRate * hours * modeMultiplier;
    }

    @Override
    protected void when(DomainEvent event) {
        if (event instanceof ServiceEpisodeCreated e) {
            this.id = new ServiceEpisodeId(e.episodeId());
            this.clientId = new ClientId(e.clientId());
            this.enrollmentId = e.enrollmentId();
            this.programId = e.programId();
            this.programName = e.programName();
            this.serviceType = e.serviceType();
            this.serviceCategory = e.serviceType().getCategory();
            this.deliveryMode = e.deliveryMode();
            this.serviceDate = e.serviceDate();
            this.plannedDurationMinutes = e.plannedDurationMinutes();
            this.primaryProviderId = e.primaryProviderId();
            this.primaryProviderName = e.primaryProviderName();
            this.primaryFundingSource = e.fundingSource();
            this.serviceDescription = e.serviceDescription();
            this.isConfidential = e.isConfidential();
            this.isBillable = e.serviceType().isBillableService();
            this.completionStatus = ServiceCompletionStatus.SCHEDULED;
            this.createdBy = e.createdBy();
            this.createdAt = e.occurredAt();
            this.lastModifiedAt = e.occurredAt();
            this.lastModifiedBy = e.createdBy();
        } else if (event instanceof ServiceStarted e) {
            this.startTime = e.startTime();
            this.serviceLocation = e.location();
            this.completionStatus = ServiceCompletionStatus.IN_PROGRESS;
            this.lastModifiedAt = e.occurredAt();
        } else if (event instanceof ServiceCompleted e) {
            this.endTime = e.endTime();
            this.actualDurationMinutes = e.actualDurationMinutes();
            this.serviceOutcome = e.outcome();
            this.completionStatus = e.status();
            this.notes = e.notes();
            this.totalBillableAmount = e.billableAmount();
            this.lastModifiedAt = e.occurredAt();
        } else if (event instanceof ProviderAdded e) {
            this.additionalProviderIds.add(e.providerId());
            this.lastModifiedAt = e.occurredAt();
        } else if (event instanceof FundingSourceAdded e) {
            this.additionalFundingSources.add(e.fundingSource());
            this.lastModifiedAt = e.occurredAt();
        } else if (event instanceof ServiceOutcomeUpdated e) {
            this.serviceOutcome = e.outcome();
            this.followUpRequired = e.followUpRequired();
            this.followUpDate = e.followUpDate();
            this.lastModifiedBy = e.updatedBy();
            this.lastModifiedAt = e.occurredAt();
        } else if (event instanceof DocumentAttached e) {
            this.attachedDocumentIds.add(e.documentId());
            this.lastModifiedAt = e.occurredAt();
        } else if (event instanceof ServiceMarkedCourtOrdered e) {
            this.isCourtOrdered = true;
            this.courtOrderNumber = e.courtOrderNumber();
            this.lastModifiedAt = e.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }

    public enum ServiceCompletionStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        PARTIALLY_COMPLETED,
        CANCELLED,
        NO_SHOW,
        POSTPONED
    }

    // Getters
    public ClientId getClientId() { return clientId; }
    public String getEnrollmentId() { return enrollmentId; }
    public String getProgramId() { return programId; }
    public String getProgramName() { return programName; }
    public ServiceType getServiceType() { return serviceType; }
    public ServiceCategory getServiceCategory() { return serviceCategory; }
    public ServiceDeliveryMode getDeliveryMode() { return deliveryMode; }
    public LocalDate getServiceDate() { return serviceDate; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Integer getPlannedDurationMinutes() { return plannedDurationMinutes; }
    public Integer getActualDurationMinutes() { return actualDurationMinutes; }
    public String getPrimaryProviderId() { return primaryProviderId; }
    public String getPrimaryProviderName() { return primaryProviderName; }
    public List<String> getAdditionalProviderIds() { return List.copyOf(additionalProviderIds); }
    public FundingSource getPrimaryFundingSource() { return primaryFundingSource; }
    public List<FundingSource> getAdditionalFundingSources() { return List.copyOf(additionalFundingSources); }
    public String getOnBehalfOfOrganization() { return onBehalfOfOrganization; }
    public boolean isBillable() { return isBillable; }
    public String getBillingCode() { return billingCode; }
    public Double getBillingRate() { return billingRate; }
    public Double getTotalBillableAmount() { return totalBillableAmount; }
    public String getServiceDescription() { return serviceDescription; }
    public String getServiceGoals() { return serviceGoals; }
    public String getServiceOutcome() { return serviceOutcome; }
    public ServiceCompletionStatus getCompletionStatus() { return completionStatus; }
    public String getFollowUpRequired() { return followUpRequired; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public String getNotes() { return notes; }
    public boolean isConfidential() { return isConfidential; }
    public String getConfidentialityReason() { return confidentialityReason; }
    public boolean isRestrictedAccess() { return isRestrictedAccess; }
    public List<String> getAuthorizedViewerIds() { return List.copyOf(authorizedViewerIds); }
    public String getServiceLocation() { return serviceLocation; }
    public String getServiceLocationAddress() { return serviceLocationAddress; }
    public boolean isOffSite() { return isOffSite; }
    public String getContextNotes() { return contextNotes; }
    public boolean isCourtOrdered() { return isCourtOrdered; }
    public String getCourtOrderNumber() { return courtOrderNumber; }
    public boolean requiresDocumentation() { return requiresDocumentation; }
    public List<String> getAttachedDocumentIds() { return List.copyOf(attachedDocumentIds); }
    public String getQualityAssuranceNotes() { return qualityAssuranceNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getLastModifiedBy() { return lastModifiedBy; }

    // Business logic methods
    public boolean isCompleted() {
        return completionStatus == ServiceCompletionStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return completionStatus == ServiceCompletionStatus.IN_PROGRESS;
    }

    public boolean requiresFollowUp() {
        return followUpRequired != null && !followUpRequired.trim().isEmpty();
    }

    public boolean isOverdue() {
        if (followUpDate == null) return false;
        return followUpDate.isBefore(LocalDate.now());
    }

    public Double getActualBillableHours() {
        if (actualDurationMinutes == null) return null;
        return actualDurationMinutes / 60.0;
    }

    public boolean meetsMinimumDuration() {
        if (actualDurationMinutes == null || serviceType == null) return false;
        var typicalRange = serviceType.getTypicalDuration();
        return actualDurationMinutes >= typicalRange.minMinutes();
    }

    public boolean exceedsMaximumDuration() {
        if (actualDurationMinutes == null || serviceType == null) return false;
        var typicalRange = serviceType.getTypicalDuration();
        return actualDurationMinutes > typicalRange.maxMinutes();
    }
}