package org.haven.api.services.dto;

import org.haven.shared.vo.services.ServiceCategory;
import org.haven.shared.vo.services.ServiceType;

public record ServiceTypeResponse(
    String name,
    String description,
    ServiceCategory category,
    boolean requiresConfidentialHandling,
    boolean isBillableService,
    int typicalMinDuration,
    int typicalMaxDuration
) {
    
    public static ServiceTypeResponse fromDomain(ServiceType serviceType) {
        var durationRange = serviceType.getTypicalDuration();
        return new ServiceTypeResponse(
            serviceType.name(),
            serviceType.getDescription(),
            serviceType.getCategory(),
            serviceType.requiresConfidentialHandling(),
            serviceType.isBillableService(),
            durationRange.minMinutes(),
            durationRange.maxMinutes()
        );
    }
}