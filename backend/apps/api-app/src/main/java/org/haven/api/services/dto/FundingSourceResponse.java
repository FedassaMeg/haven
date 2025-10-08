package org.haven.api.services.dto;

import org.haven.shared.vo.services.FundingSource;

public record FundingSourceResponse(
    String funderId,
    String funderName,
    String grantNumber,
    FundingSource.FunderType funderType,
    String programName,
    boolean requiresOutcomeTracking,
    boolean allowsConfidentialServices,
    FundingSource.BillingRateCategory billingRateCategory
) {
    
    public static FundingSourceResponse fromDomain(FundingSource fundingSource) {
        return new FundingSourceResponse(
            fundingSource.getFunderId(),
            fundingSource.getFunderName(),
            fundingSource.getGrantNumber(),
            fundingSource.getFunderType(),
            fundingSource.getProgramName(),
            fundingSource.requiresOutcomeTracking(),
            fundingSource.allowsConfidentialServices(),
            fundingSource.getBillingRateCategory()
        );
    }
}