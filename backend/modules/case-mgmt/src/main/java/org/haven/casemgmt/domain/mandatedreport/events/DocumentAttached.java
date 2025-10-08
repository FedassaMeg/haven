package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a document is attached to a mandated report
 */

public class DocumentAttached extends DomainEvent {
    private final UUID documentId;
    private final String fileName;
    private final String documentType;
    private final boolean isRequired;
    private final UUID attachedByUserId;

    public DocumentAttached(UUID reportId, UUID documentId, String fileName, String documentType, boolean isRequired, UUID attachedByUserId, Instant attachedAt) {
        super(reportId, attachedAt);
        this.documentId = documentId;
        this.fileName = fileName;
        this.documentType = documentType;
        this.isRequired = isRequired;
        this.attachedByUserId = attachedByUserId;
    }

    public UUID documentId() {
        return documentId;
    }

    public String fileName() {
        return fileName;
    }

    public String documentType() {
        return documentType;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public UUID attachedByUserId() {
        return attachedByUserId;
    }


    // JavaBean-style getters
    public UUID getDocumentId() { return documentId; }
    public String getFileName() { return fileName; }
    public String getDocumentType() { return documentType; }
    public boolean IsRequired() { return isRequired; }
    public UUID getAttachedByUserId() { return attachedByUserId; }
}