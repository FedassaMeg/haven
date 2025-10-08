package org.haven.financialassistance.domain.ledger.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class DocumentAttached extends DomainEvent {
    private final String documentId;
    private final String documentName;
    private final String documentType;
    private final byte[] documentContent;
    private final String uploadedBy;

    public DocumentAttached(
        UUID ledgerId,
        String documentId,
        String documentName,
        String documentType,
        byte[] documentContent,
        String uploadedBy,
        Instant occurredAt
    ) {
        super(ledgerId, occurredAt);
        this.documentId = documentId;
        this.documentName = documentName;
        this.documentType = documentType;
        this.documentContent = documentContent;
        this.uploadedBy = uploadedBy;
    }

    // JavaBean style getters
    public UUID getLedgerId() { return getAggregateId(); }
    public String getDocumentId() { return documentId; }
    public String getDocumentName() { return documentName; }
    public String getDocumentType() { return documentType; }
    public byte[] getDocumentContent() { return documentContent; }
    public String getUploadedBy() { return uploadedBy; }

    // Record style getters
    public UUID ledgerId() { return getAggregateId(); }
    public String documentId() { return documentId; }
    public String documentName() { return documentName; }
    public String documentType() { return documentType; }
    public byte[] documentContent() { return documentContent; }
    public String uploadedBy() { return uploadedBy; }

    public static DocumentAttached create(UUID ledgerId, String documentId, String documentName,
                                        String documentType, byte[] documentContent, String uploadedBy) {
        return new DocumentAttached(
            ledgerId, documentId, documentName, documentType, documentContent, uploadedBy, Instant.now()
        );
    }
}