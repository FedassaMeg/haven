package org.haven.api.services.dto;

import org.haven.shared.vo.services.ServiceDeliveryMode;

public record ServiceDeliveryModeResponse(
    String name,
    String description,
    boolean allowsConfidentialServices,
    boolean hasReducedBillingRate,
    boolean isRemoteDelivery,
    double billingMultiplier
) {
    
    public static ServiceDeliveryModeResponse fromDomain(ServiceDeliveryMode mode) {
        return new ServiceDeliveryModeResponse(
            mode.name(),
            mode.getDescription(),
            mode.allowsConfidentialServices(),
            mode.hasReducedBillingRate(),
            mode.isRemoteDelivery(),
            mode.getBillingMultiplier()
        );
    }
}