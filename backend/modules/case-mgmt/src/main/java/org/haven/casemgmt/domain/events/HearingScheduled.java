package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class HearingScheduled extends DomainEvent {
    private final UUID clientId;
    private final UUID caseId;
    private final LocalDateTime hearingDateTime;
    private final String courtName;
    private final String hearingType;
    private final String purpose;
    private final String judgeName;
    private final String address;
    private final String scheduledBy;
    private final String notes;

    public HearingScheduled(UUID legalAdvocacyId, UUID clientId, UUID caseId, LocalDateTime hearingDateTime, String courtName, String hearingType, String purpose, String judgeName, String address, String scheduledBy, String notes, Instant occurredAt) {
        super(legalAdvocacyId, occurredAt != null ? occurredAt : Instant.now());
        if (legalAdvocacyId == null) throw new IllegalArgumentException("Legal advocacy ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (hearingDateTime == null) throw new IllegalArgumentException("Hearing date/time cannot be null");
        if (courtName == null || courtName.trim().isEmpty()) throw new IllegalArgumentException("Court name cannot be null or empty");
        if (hearingType == null || hearingType.trim().isEmpty()) throw new IllegalArgumentException("Hearing type cannot be null or empty");
        if (scheduledBy == null || scheduledBy.trim().isEmpty()) throw new IllegalArgumentException("Scheduled by cannot be null or empty");

        this.clientId = clientId;
        this.caseId = caseId;
        this.hearingDateTime = hearingDateTime;
        this.courtName = courtName;
        this.hearingType = hearingType;
        this.purpose = purpose;
        this.judgeName = judgeName;
        this.address = address;
        this.scheduledBy = scheduledBy;
        this.notes = notes;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID caseId() {
        return caseId;
    }

    public LocalDateTime hearingDateTime() {
        return hearingDateTime;
    }

    public String courtName() {
        return courtName;
    }

    public String hearingType() {
        return hearingType;
    }

    public String purpose() {
        return purpose;
    }

    public String judgeName() {
        return judgeName;
    }

    public String address() {
        return address;
    }

    public String scheduledBy() {
        return scheduledBy;
    }

    public String notes() {
        return notes;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getCaseId() { return caseId; }
    public LocalDateTime getHearingDateTime() { return hearingDateTime; }
    public String getCourtName() { return courtName; }
    public String getHearingType() { return hearingType; }
    public String getPurpose() { return purpose; }
    public String getJudgeName() { return judgeName; }
    public String getAddress() { return address; }
    public String getScheduledBy() { return scheduledBy; }
    public String getNotes() { return notes; }
}