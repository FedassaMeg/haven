package org.haven.servicedelivery.infrastructure.persistence;

import org.haven.servicedelivery.domain.ServiceEpisode;
import org.haven.shared.vo.services.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "ServiceDeliveryEpisodeEntity")
@Table(name = "service_episodes")
public class JpaServiceEpisodeEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "enrollment_id", nullable = false)
    private String enrollmentId;
    
    @Column(name = "program_id", nullable = false)
    private String programId;
    
    @Column(name = "program_name", nullable = false)
    private String programName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "service_category")
    private ServiceCategory serviceCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false)
    private ServiceDeliveryMode deliveryMode;
    
    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "planned_duration_minutes")
    private Integer plannedDurationMinutes;
    
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    @Column(name = "primary_provider_id", nullable = false)
    private String primaryProviderId;
    
    @Column(name = "primary_provider_name", nullable = false)
    private String primaryProviderName;
    
    @ElementCollection
    @CollectionTable(name = "service_episode_additional_providers", 
                    joinColumns = @JoinColumn(name = "service_episode_id"))
    @Column(name = "provider_id")
    private List<String> additionalProviderIds = new ArrayList<>();
    
    @Column(name = "service_description", length = 1000)
    private String serviceDescription;
    
    @Column(name = "service_goals", length = 1000)
    private String serviceGoals;
    
    @Column(name = "service_outcome", length = 1000)
    private String serviceOutcome;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status")
    private ServiceEpisode.ServiceCompletionStatus completionStatus;
    
    @Column(name = "follow_up_required", length = 500)
    private String followUpRequired;
    
    @Column(name = "follow_up_date")
    private LocalDate followUpDate;
    
    @Column(name = "notes", length = 2000)
    private String notes;
    
    @Column(name = "is_confidential", nullable = false)
    private boolean isConfidential;
    
    @Column(name = "confidentiality_reason", length = 500)
    private String confidentialityReason;
    
    @Column(name = "is_restricted_access", nullable = false)
    private boolean isRestrictedAccess;
    
    @ElementCollection
    @CollectionTable(name = "service_episode_authorized_viewers", 
                    joinColumns = @JoinColumn(name = "service_episode_id"))
    @Column(name = "viewer_id")
    private List<String> authorizedViewerIds = new ArrayList<>();
    
    @Column(name = "service_location", length = 255)
    private String serviceLocation;
    
    @Column(name = "service_location_address", length = 500)
    private String serviceLocationAddress;
    
    @Column(name = "is_offsite", nullable = false)
    private boolean isOffSite;
    
    @Column(name = "context_notes", length = 1000)
    private String contextNotes;
    
    @Column(name = "is_court_ordered", nullable = false)
    private boolean isCourtOrdered;
    
    @Column(name = "court_order_number", length = 100)
    private String courtOrderNumber;
    
    @Column(name = "requires_documentation", nullable = false)
    private boolean requiresDocumentation;
    
    @ElementCollection
    @CollectionTable(name = "service_episode_attached_documents", 
                    joinColumns = @JoinColumn(name = "service_episode_id"))
    @Column(name = "document_id")
    private List<String> attachedDocumentIds = new ArrayList<>();
    
    @Column(name = "quality_assurance_notes", length = 1000)
    private String qualityAssuranceNotes;
    
    @Column(name = "is_billable", nullable = false)
    private boolean isBillable;
    
    @Column(name = "billing_code", length = 50)
    private String billingCode;
    
    @Column(name = "billing_rate")
    private Double billingRate;
    
    @Column(name = "total_billable_amount")
    private Double totalBillableAmount;
    
    @Column(name = "on_behalf_of_organization", length = 255)
    private String onBehalfOfOrganization;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "last_modified_by", nullable = false)
    private String lastModifiedBy;

    // Constructors
    public JpaServiceEpisodeEntity() {}

    public JpaServiceEpisodeEntity(ServiceEpisode serviceEpisode) {
        this.id = serviceEpisode.getId().value();
        this.clientId = serviceEpisode.getClientId().value();
        this.enrollmentId = serviceEpisode.getEnrollmentId();
        this.programId = serviceEpisode.getProgramId();
        this.programName = serviceEpisode.getProgramName();
        this.serviceType = serviceEpisode.getServiceType();
        this.serviceCategory = serviceEpisode.getServiceCategory();
        this.deliveryMode = serviceEpisode.getDeliveryMode();
        this.serviceDate = serviceEpisode.getServiceDate();
        this.startTime = serviceEpisode.getStartTime();
        this.endTime = serviceEpisode.getEndTime();
        this.plannedDurationMinutes = serviceEpisode.getPlannedDurationMinutes();
        this.actualDurationMinutes = serviceEpisode.getActualDurationMinutes();
        this.primaryProviderId = serviceEpisode.getPrimaryProviderId();
        this.primaryProviderName = serviceEpisode.getPrimaryProviderName();
        this.additionalProviderIds = new ArrayList<>(serviceEpisode.getAdditionalProviderIds());
        this.serviceDescription = serviceEpisode.getServiceDescription();
        this.serviceGoals = serviceEpisode.getServiceGoals();
        this.serviceOutcome = serviceEpisode.getServiceOutcome();
        this.completionStatus = serviceEpisode.getCompletionStatus();
        this.followUpRequired = serviceEpisode.getFollowUpRequired();
        this.followUpDate = serviceEpisode.getFollowUpDate();
        this.notes = serviceEpisode.getNotes();
        this.isConfidential = serviceEpisode.isConfidential();
        this.confidentialityReason = serviceEpisode.getConfidentialityReason();
        this.isRestrictedAccess = serviceEpisode.isRestrictedAccess();
        this.authorizedViewerIds = new ArrayList<>(serviceEpisode.getAuthorizedViewerIds());
        this.serviceLocation = serviceEpisode.getServiceLocation();
        this.serviceLocationAddress = serviceEpisode.getServiceLocationAddress();
        this.isOffSite = serviceEpisode.isOffSite();
        this.contextNotes = serviceEpisode.getContextNotes();
        this.isCourtOrdered = serviceEpisode.isCourtOrdered();
        this.courtOrderNumber = serviceEpisode.getCourtOrderNumber();
        this.requiresDocumentation = serviceEpisode.requiresDocumentation();
        this.attachedDocumentIds = new ArrayList<>(serviceEpisode.getAttachedDocumentIds());
        this.qualityAssuranceNotes = serviceEpisode.getQualityAssuranceNotes();
        this.isBillable = serviceEpisode.isBillable();
        this.billingCode = serviceEpisode.getBillingCode();
        this.billingRate = serviceEpisode.getBillingRate();
        this.totalBillableAmount = serviceEpisode.getTotalBillableAmount();
        this.onBehalfOfOrganization = serviceEpisode.getOnBehalfOfOrganization();
        this.createdAt = serviceEpisode.getCreatedAt();
        this.lastModifiedAt = serviceEpisode.getLastModifiedAt();
        this.createdBy = serviceEpisode.getCreatedBy();
        this.lastModifiedBy = serviceEpisode.getLastModifiedBy();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }
    
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    
    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }
    
    public ServiceCategory getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(ServiceCategory serviceCategory) { this.serviceCategory = serviceCategory; }
    
    public ServiceDeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(ServiceDeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }
    
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public Integer getPlannedDurationMinutes() { return plannedDurationMinutes; }
    public void setPlannedDurationMinutes(Integer plannedDurationMinutes) { this.plannedDurationMinutes = plannedDurationMinutes; }
    
    public Integer getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(Integer actualDurationMinutes) { this.actualDurationMinutes = actualDurationMinutes; }
    
    public String getPrimaryProviderId() { return primaryProviderId; }
    public void setPrimaryProviderId(String primaryProviderId) { this.primaryProviderId = primaryProviderId; }
    
    public String getPrimaryProviderName() { return primaryProviderName; }
    public void setPrimaryProviderName(String primaryProviderName) { this.primaryProviderName = primaryProviderName; }
    
    public List<String> getAdditionalProviderIds() { return additionalProviderIds; }
    public void setAdditionalProviderIds(List<String> additionalProviderIds) { this.additionalProviderIds = additionalProviderIds; }
    
    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }
    
    public String getServiceGoals() { return serviceGoals; }
    public void setServiceGoals(String serviceGoals) { this.serviceGoals = serviceGoals; }
    
    public String getServiceOutcome() { return serviceOutcome; }
    public void setServiceOutcome(String serviceOutcome) { this.serviceOutcome = serviceOutcome; }
    
    public ServiceEpisode.ServiceCompletionStatus getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(ServiceEpisode.ServiceCompletionStatus completionStatus) { this.completionStatus = completionStatus; }
    
    public String getFollowUpRequired() { return followUpRequired; }
    public void setFollowUpRequired(String followUpRequired) { this.followUpRequired = followUpRequired; }
    
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public boolean isConfidential() { return isConfidential; }
    public void setConfidential(boolean confidential) { isConfidential = confidential; }
    
    public String getConfidentialityReason() { return confidentialityReason; }
    public void setConfidentialityReason(String confidentialityReason) { this.confidentialityReason = confidentialityReason; }
    
    public boolean isRestrictedAccess() { return isRestrictedAccess; }
    public void setRestrictedAccess(boolean restrictedAccess) { isRestrictedAccess = restrictedAccess; }
    
    public List<String> getAuthorizedViewerIds() { return authorizedViewerIds; }
    public void setAuthorizedViewerIds(List<String> authorizedViewerIds) { this.authorizedViewerIds = authorizedViewerIds; }
    
    public String getServiceLocation() { return serviceLocation; }
    public void setServiceLocation(String serviceLocation) { this.serviceLocation = serviceLocation; }
    
    public String getServiceLocationAddress() { return serviceLocationAddress; }
    public void setServiceLocationAddress(String serviceLocationAddress) { this.serviceLocationAddress = serviceLocationAddress; }
    
    public boolean isOffSite() { return isOffSite; }
    public void setOffSite(boolean offSite) { isOffSite = offSite; }
    
    public String getContextNotes() { return contextNotes; }
    public void setContextNotes(String contextNotes) { this.contextNotes = contextNotes; }
    
    public boolean isCourtOrdered() { return isCourtOrdered; }
    public void setCourtOrdered(boolean courtOrdered) { isCourtOrdered = courtOrdered; }
    
    public String getCourtOrderNumber() { return courtOrderNumber; }
    public void setCourtOrderNumber(String courtOrderNumber) { this.courtOrderNumber = courtOrderNumber; }
    
    public boolean requiresDocumentation() { return requiresDocumentation; }
    public void setRequiresDocumentation(boolean requiresDocumentation) { this.requiresDocumentation = requiresDocumentation; }
    
    public List<String> getAttachedDocumentIds() { return attachedDocumentIds; }
    public void setAttachedDocumentIds(List<String> attachedDocumentIds) { this.attachedDocumentIds = attachedDocumentIds; }
    
    public String getQualityAssuranceNotes() { return qualityAssuranceNotes; }
    public void setQualityAssuranceNotes(String qualityAssuranceNotes) { this.qualityAssuranceNotes = qualityAssuranceNotes; }
    
    public boolean isBillable() { return isBillable; }
    public void setBillable(boolean billable) { isBillable = billable; }
    
    public String getBillingCode() { return billingCode; }
    public void setBillingCode(String billingCode) { this.billingCode = billingCode; }
    
    public Double getBillingRate() { return billingRate; }
    public void setBillingRate(Double billingRate) { this.billingRate = billingRate; }
    
    public Double getTotalBillableAmount() { return totalBillableAmount; }
    public void setTotalBillableAmount(Double totalBillableAmount) { this.totalBillableAmount = totalBillableAmount; }
    
    public String getOnBehalfOfOrganization() { return onBehalfOfOrganization; }
    public void setOnBehalfOfOrganization(String onBehalfOfOrganization) { this.onBehalfOfOrganization = onBehalfOfOrganization; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(Instant lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
}