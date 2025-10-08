package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a document is removed from a mandated report
 */

public class DocumentDetached extends DomainEvent {
    private final UUID documentId;
    private final String reason;
    private final UUID removedByUserId;

    public DocumentDetached(UUID reportId, UUID documentId, String reason, UUID removedByUserId, Instant removedAt) {
        super(reportId, removedAt);
        this.documentId = documentId;
        this.reason = reason;
        this.removedByUserId = removedByUserId;
    }

    public UUID documentId() {
        return documentId;
    }

    public String reason() {
        return reason;
    }

    public UUID removedByUserId() {
        return removedByUserId;
    }


    // JavaBean-style getters
    public UUID getDocumentId() { return documentId; }
    public String getReason() { return reason; }
    public UUID getRemovedByUserId() { return removedByUserId; }
}