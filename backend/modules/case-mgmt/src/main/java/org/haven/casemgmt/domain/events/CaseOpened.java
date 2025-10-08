package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.UUID;

public class CaseOpened extends DomainEvent {
    private final UUID caseId;
    private final UUID clientId;
    private final CodeableConcept caseType;
    private final CodeableConcept priority;
    private final String description;

    public CaseOpened(UUID caseId, UUID clientId, CodeableConcept caseType, CodeableConcept priority, String description, Instant occurredAt) {
        super(caseId, occurredAt);
        this.caseId = caseId;
        this.clientId = clientId;
        this.caseType = caseType;
        this.priority = priority;
        this.description = description;
    }

    @Override
    public String eventType() {
        return "CaseOpened";
    }

    public UUID caseId() {
        return caseId;
    }

    public UUID clientId() {
        return clientId;
    }

    public CodeableConcept caseType() {
        return caseType;
    }

    public CodeableConcept priority() {
        return priority;
    }

    public String description() {
        return description;
    }

    // JavaBean-style getters
    public UUID getCaseId() { return caseId; }
    public UUID getClientId() { return clientId; }
    public CodeableConcept getCaseType() { return caseType; }
    public CodeableConcept getPriority() { return priority; }
    public String getDescription() { return description; }
}