package org.haven.intake.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.intake.domain.ReferralSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PreIntakeContactCreated extends DomainEvent {
    private final UUID tempClientId;
    private final String clientAlias;
    private final LocalDate contactDate;
    private final ReferralSource referralSource;
    private final String intakeWorkerName;
    private final Instant expiresAt;

    public PreIntakeContactCreated(
            UUID tempClientId,
            String clientAlias,
            LocalDate contactDate,
            ReferralSource referralSource,
            String intakeWorkerName,
            Instant occurredAt,
            Instant expiresAt) {
        super(tempClientId, occurredAt);
        this.tempClientId = tempClientId;
        this.clientAlias = clientAlias;
        this.contactDate = contactDate;
        this.referralSource = referralSource;
        this.intakeWorkerName = intakeWorkerName;
        this.expiresAt = expiresAt;
    }

    public UUID tempClientId() { return tempClientId; }
    public String clientAlias() { return clientAlias; }
    public LocalDate contactDate() { return contactDate; }
    public ReferralSource referralSource() { return referralSource; }
    public String intakeWorkerName() { return intakeWorkerName; }
    public Instant expiresAt() { return expiresAt; }
}
