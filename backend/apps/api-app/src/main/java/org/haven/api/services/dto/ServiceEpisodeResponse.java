package org.haven.api.services.dto;

import org.haven.servicedelivery.domain.ServiceEpisode;
import org.haven.shared.vo.services.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceEpisodeResponse(
    UUID id,
    UUID clientId,
    String enrollmentId,
    String programId,
    String programName,
    ServiceType serviceType,
    ServiceCategory serviceCategory,
    ServiceDeliveryMode deliveryMode,
    LocalDate serviceDate,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer plannedDurationMinutes,
    Integer actualDurationMinutes,
    String primaryProviderId,
    String primaryProviderName,
    List<String> additionalProviderIds,
    FundingSourceResponse primaryFundingSource,
    List<FundingSourceResponse> additionalFundingSources,
    String onBehalfOfOrganization,
    boolean isBillable,
    String billingCode,
    Double billingRate,
    Double totalBillableAmount,
    String serviceDescription,
    String serviceGoals,
    String serviceOutcome,
    ServiceEpisode.ServiceCompletionStatus completionStatus,
    String followUpRequired,
    LocalDate followUpDate,
    String notes,
    boolean isConfidential,
    String confidentialityReason,
    boolean isRestrictedAccess,
    List<String> authorizedViewerIds,
    String serviceLocation,
    String serviceLocationAddress,
    boolean isOffSite,
    String contextNotes,
    boolean isCourtOrdered,
    String courtOrderNumber,
    boolean requiresDocumentation,
    List<String> attachedDocumentIds,
    String qualityAssuranceNotes,
    Instant createdAt,
    Instant lastModifiedAt,
    String createdBy,
    String lastModifiedBy
) {
    
    public static ServiceEpisodeResponse fromDomain(ServiceEpisode episode) {
        return new ServiceEpisodeResponse(
            episode.getId().value(),
            episode.getClientId().value(),
            episode.getEnrollmentId(),
            episode.getProgramId(),
            episode.getProgramName(),
            episode.getServiceType(),
            episode.getServiceCategory(),
            episode.getDeliveryMode(),
            episode.getServiceDate(),
            episode.getStartTime(),
            episode.getEndTime(),
            episode.getPlannedDurationMinutes(),
            episode.getActualDurationMinutes(),
            episode.getPrimaryProviderId(),
            episode.getPrimaryProviderName(),
            episode.getAdditionalProviderIds(),
            FundingSourceResponse.fromDomain(episode.getPrimaryFundingSource()),
            episode.getAdditionalFundingSources().stream()
                .map(FundingSourceResponse::fromDomain)
                .toList(),
            episode.getOnBehalfOfOrganization(),
            episode.isBillable(),
            episode.getBillingCode(),
            episode.getBillingRate(),
            episode.getTotalBillableAmount(),
            episode.getServiceDescription(),
            episode.getServiceGoals(),
            episode.getServiceOutcome(),
            episode.getCompletionStatus(),
            episode.getFollowUpRequired(),
            episode.getFollowUpDate(),
            episode.getNotes(),
            episode.isConfidential(),
            episode.getConfidentialityReason(),
            episode.isRestrictedAccess(),
            episode.getAuthorizedViewerIds(),
            episode.getServiceLocation(),
            episode.getServiceLocationAddress(),
            episode.isOffSite(),
            episode.getContextNotes(),
            episode.isCourtOrdered(),
            episode.getCourtOrderNumber(),
            episode.requiresDocumentation(),
            episode.getAttachedDocumentIds(),
            episode.getQualityAssuranceNotes(),
            episode.getCreatedAt(),
            episode.getLastModifiedAt(),
            episode.getCreatedBy(),
            episode.getLastModifiedBy()
        );
    }
    
    public boolean isCompleted() {
        return completionStatus == ServiceEpisode.ServiceCompletionStatus.COMPLETED;
    }
    
    public boolean isInProgress() {
        return completionStatus == ServiceEpisode.ServiceCompletionStatus.IN_PROGRESS;
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
}