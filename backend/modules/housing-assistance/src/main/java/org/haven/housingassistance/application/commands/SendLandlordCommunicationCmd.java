package org.haven.housingassistance.application.commands;

import org.haven.housingassistance.application.services.LandlordCommunicationService.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record SendLandlordCommunicationCmd(
    @NotNull(message = "Client ID is required")
    UUID clientId,
    
    @NotNull(message = "Landlord ID is required")
    UUID landlordId,
    
    UUID housingAssistanceId,
    
    @NotNull(message = "Communication channel is required")
    Channel channel,
    
    @NotBlank(message = "Subject is required")
    String subject,
    
    @NotBlank(message = "Body is required")
    String body,
    
    Map<String, Object> requestedFields,
    
    String recipientEmail,
    String recipientPhone,
    String recipientFax,
    String recipientPortalId,
    
    @NotNull(message = "User ID is required")
    UUID userId
) {}