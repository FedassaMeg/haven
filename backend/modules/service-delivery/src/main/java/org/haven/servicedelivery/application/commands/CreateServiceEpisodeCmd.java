package org.haven.servicedelivery.application.commands;

import org.haven.shared.vo.services.*;
import java.time.LocalDate;
import java.util.UUID;

public record CreateServiceEpisodeCmd(
    UUID clientId,
    String enrollmentId,
    String programId,
    String programName,
    ServiceType serviceType,
    ServiceDeliveryMode deliveryMode,
    LocalDate serviceDate,
    Integer plannedDurationMinutes,
    String primaryProviderId,
    String primaryProviderName,
    String funderId,
    String funderName,
    String grantNumber,
    String serviceDescription,
    boolean isConfidential,
    String createdBy
) {
    
    public FundingSource getFundingSource() {
        if ("HUD-COC".equals(funderId)) {
            return FundingSource.hudCoc(grantNumber, programName);
        } else if ("DOJ-VAWA".equals(funderId)) {
            return FundingSource.vawa(grantNumber, programName);
        } else if ("CAL-OES".equals(funderId)) {
            return FundingSource.calOes(grantNumber, programName);
        } else if ("FEMA-ESG".equals(funderId)) {
            return FundingSource.fema(grantNumber, programName);
        } else if ("HUD-HOPWA".equals(funderId)) {
            return FundingSource.hopwa(grantNumber, programName);
        } else if ("NONE".equals(funderId)) {
            return FundingSource.noFunding();
        } else {
            return FundingSource.privateFoundation(funderId, funderName, grantNumber, programName);
        }
    }
}