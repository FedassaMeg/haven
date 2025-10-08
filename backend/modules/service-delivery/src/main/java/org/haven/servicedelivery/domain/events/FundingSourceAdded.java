package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.services.FundingSource;
import java.time.Instant;
import java.util.UUID;

public class FundingSourceAdded extends DomainEvent {
    private final FundingSource fundingSource;
    private final double allocationPercentage;

    public FundingSourceAdded(UUID episodeId, FundingSource fundingSource, double allocationPercentage, Instant occurredAt) {
        super(episodeId, occurredAt);
        this.fundingSource = fundingSource;
        this.allocationPercentage = allocationPercentage;
    }

    public FundingSource fundingSource() {
        return fundingSource;
    }

    public double allocationPercentage() {
        return allocationPercentage;
    }


    // JavaBean-style getters
    public FundingSource getFundingSource() { return fundingSource; }
    public double getAllocationPercentage() { return allocationPercentage; }
}