package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class DocumentAttached extends DomainEvent {
    private final String documentId;
    private final String documentType;
    private final String description;

    public DocumentAttached(UUID episodeId, String documentId, String documentType, String description, Instant occurredAt) {
        super(episodeId, occurredAt);
        this.documentId = documentId;
        this.documentType = documentType;
        this.description = description;
    }

    public String documentId() {
        return documentId;
    }

    public String documentType() {
        return documentType;
    }

    public String description() {
        return description;
    }


    // JavaBean-style getters
    public String getDocumentId() { return documentId; }
    public String getDocumentType() { return documentType; }
    public String getDescription() { return description; }
}