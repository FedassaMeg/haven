package org.haven.api.services.dto;

import org.haven.shared.vo.services.ServiceDeliveryMode;
import org.haven.shared.vo.services.ServiceType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class QuickServiceRequests {
    
    public record QuickCrisisServiceRequest(
        @NotNull UUID clientId,
        @NotNull @Size(min = 1) String enrollmentId,
        @NotNull @Size(min = 1) String programId,
        @NotNull @Size(min = 1) String providerId,
        @NotNull @Size(min = 1) String providerName,
        boolean isConfidential
    ) {}
    
    public record QuickCounselingServiceRequest(
        @NotNull UUID clientId,
        @NotNull @Size(min = 1) String enrollmentId,
        @NotNull @Size(min = 1) String programId,
        @NotNull ServiceType serviceType,
        @NotNull @Size(min = 1) String providerId,
        @NotNull @Size(min = 1) String providerName
    ) {}
    
    public record QuickCaseManagementServiceRequest(
        @NotNull UUID clientId,
        @NotNull @Size(min = 1) String enrollmentId,
        @NotNull @Size(min = 1) String programId,
        @NotNull ServiceDeliveryMode deliveryMode,
        @NotNull @Size(min = 1) String providerId,
        @NotNull @Size(min = 1) String providerName,
        @Size(max = 500) String description
    ) {}
    
    public record UpdateOutcomeRequest(
        @Size(max = 1000) String outcome,
        @Size(max = 500) String followUpRequired,
        java.time.LocalDate followUpDate
    ) {}
    
    public record AddProviderRequest(
        @NotNull @Size(min = 1) String providerId,
        @NotNull @Size(min = 1) String providerName,
        @Size(max = 100) String role
    ) {}
    
    public record AddFundingSourceRequest(
        @NotNull @Size(min = 1) String funderId,
        @Size(max = 255) String funderName,
        @Size(max = 100) String grantNumber,
        @Size(max = 255) String programName,
        @NotNull @jakarta.validation.constraints.DecimalMin("0.01") 
        @jakarta.validation.constraints.DecimalMax("100.00")
        Double allocationPercentage
    ) {}
    
    public record CourtOrderRequest(
        @NotNull @Size(min = 1, max = 100) String courtOrderNumber
    ) {}
}