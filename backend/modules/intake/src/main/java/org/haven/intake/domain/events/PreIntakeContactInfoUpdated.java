package org.haven.intake.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.intake.domain.ReferralSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PreIntakeContactInfoUpdated extends DomainEvent {
    private final UUID tempClientId;
    private final String clientAlias;
    private final LocalDate contactDate;
    private final ReferralSource referralSource;

    public PreIntakeContactInfoUpdated(
            UUID tempClientId,
            String clientAlias,
            LocalDate contactDate,
            ReferralSource referralSource,
            Instant occurredAt) {
        super(tempClientId, occurredAt);
        this.tempClientId = tempClientId;
        this.clientAlias = clientAlias;
        this.contactDate = contactDate;
        this.referralSource = referralSource;
    }

    public UUID tempClientId() { return tempClientId; }
    public String clientAlias() { return clientAlias; }
    public LocalDate contactDate() { return contactDate; }
    public ReferralSource referralSource() { return referralSource; }
}
