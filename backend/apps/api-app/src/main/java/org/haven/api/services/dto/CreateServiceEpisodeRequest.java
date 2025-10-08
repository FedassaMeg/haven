package org.haven.api.services.dto;

import org.haven.shared.vo.services.ServiceDeliveryMode;
import org.haven.shared.vo.services.ServiceType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateServiceEpisodeRequest(
    @NotNull
    UUID clientId,
    
    @NotNull
    @Size(min = 1, max = 255)
    String enrollmentId,
    
    @NotNull
    @Size(min = 1, max = 255)
    String programId,
    
    @NotNull
    @Size(min = 1, max = 255)
    String programName,
    
    @NotNull
    ServiceType serviceType,
    
    @NotNull
    ServiceDeliveryMode deliveryMode,
    
    @NotNull
    LocalDate serviceDate,
    
    @Positive
    Integer plannedDurationMinutes,
    
    @NotNull
    @Size(min = 1, max = 255)
    String primaryProviderId,
    
    @NotNull
    @Size(min = 1, max = 255)
    String primaryProviderName,
    
    @NotNull
    @Size(min = 1, max = 100)
    String funderId,
    
    @Size(max = 255)
    String funderName,
    
    @Size(max = 100)
    String grantNumber,
    
    @Size(max = 1000)
    String serviceDescription,
    
    boolean isConfidential
) {
}