package org.haven.documentmgmt.domain;

import org.haven.documentmgmt.domain.events.*;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class DocumentLifecycle extends AggregateRoot<DocumentId> {
    
    private UUID clientId;
    private UUID caseId;
    private String documentName;
    private DocumentType documentType;
    private DocumentStatus status;
    private LocalDate requiredDate;
    private LocalDate receivedDate;
    private LocalDate verifiedDate;
    private LocalDate expirationDate;
    private String description;
    private String submittedBy;
    private String verifiedBy;
    private String notes;
    private Instant createdAt;
    private Instant lastModified;
    
    public enum DocumentType {
        RECEIPT("Receipt", false),
        ROI_RELEASE_OF_INFORMATION("Release of Information", true),
        LEASE_AGREEMENT("Lease Agreement", true),
        BIRTH_CERTIFICATE("Birth Certificate", false),
        IDENTIFICATION("Identification Document", true),
        INCOME_VERIFICATION("Income Verification", true),
        INSURANCE_CARD("Insurance Card", true),
        MEDICAL_RECORDS("Medical Records", false),
        COURT_ORDER("Court Order", true),
        CONSENT_FORM("Consent Form", true),
        SAFETY_PLAN("Safety Plan", false),
        SERVICE_AGREEMENT("Service Agreement", true),
        OTHER("Other Document", false);
        
        private final String description;
        private final boolean hasExpiration;
        
        DocumentType(String description, boolean hasExpiration) {
            this.description = description;
            this.hasExpiration = hasExpiration;
        }
        
        public String getDescription() { return description; }
        public boolean hasExpiration() { return hasExpiration; }
    }
    
    public enum DocumentStatus {
        REQUIRED("Required - not yet received"),
        RECEIVED("Received - pending verification"),
        VERIFIED("Verified - document is valid"),
        EXPIRED("Expired - needs renewal"),
        REJECTED("Rejected - document invalid"),
        WAIVED("Waived - not required for this case");
        
        private final String description;
        
        DocumentStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public static DocumentLifecycle require(UUID clientId, UUID caseId, String documentName, 
                                          DocumentType documentType, LocalDate requiredDate, 
                                          LocalDate expirationDate, String requiredBy) {
        DocumentId documentId = DocumentId.generate();
        DocumentLifecycle document = new DocumentLifecycle();
        document.apply(new DocumentRequired(
            documentId.getValue(), clientId, caseId, documentName, documentType,
            requiredDate, expirationDate, requiredBy, Instant.now()
        ));
        return document;
    }
    
    public void markReceived(String submittedBy, String notes) {
        if (status != DocumentStatus.REQUIRED) {
            throw new IllegalStateException("Can only mark REQUIRED documents as received");
        }
        apply(new DocumentReceived(id.getValue(), submittedBy, notes, LocalDate.now(), Instant.now()));
    }
    
    public void markVerified(String verifiedBy, String notes) {
        if (status != DocumentStatus.RECEIVED) {
            throw new IllegalStateException("Can only verify RECEIVED documents");
        }
        apply(new DocumentVerified(id.getValue(), verifiedBy, notes, LocalDate.now(), Instant.now()));
    }
    
    public void markExpired(String reason) {
        if (status != DocumentStatus.VERIFIED) {
            throw new IllegalStateException("Only VERIFIED documents can expire");
        }
        apply(new DocumentExpired(id.getValue(), reason, LocalDate.now(), Instant.now()));
    }
    
    public void reject(String reason, String rejectedBy) {
        if (status == DocumentStatus.VERIFIED || status == DocumentStatus.EXPIRED) {
            throw new IllegalStateException("Cannot reject verified or expired documents");
        }
        apply(new DocumentRejected(id.getValue(), reason, rejectedBy, Instant.now()));
    }
    
    public void waive(String reason, String waivedBy) {
        if (status == DocumentStatus.VERIFIED) {
            throw new IllegalStateException("Cannot waive verified documents");
        }
        apply(new DocumentWaived(id.getValue(), reason, waivedBy, Instant.now()));
    }
    
    public void updateExpiration(LocalDate newExpirationDate, String updatedBy, String reason) {
        apply(new DocumentExpirationUpdated(id.getValue(), this.expirationDate, newExpirationDate, 
                                          updatedBy, reason, Instant.now()));
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof DocumentRequired e) {
            this.id = DocumentId.from(e.documentId());
            this.clientId = e.clientId();
            this.caseId = e.caseId();
            this.documentName = e.documentName();
            this.documentType = e.documentType();
            this.status = DocumentStatus.REQUIRED;
            this.requiredDate = e.requiredDate();
            this.expirationDate = e.expirationDate();
            this.createdAt = e.occurredAt();
            this.lastModified = e.occurredAt();
        } else if (event instanceof DocumentReceived e) {
            this.status = DocumentStatus.RECEIVED;
            this.receivedDate = e.receivedDate();
            this.submittedBy = e.submittedBy();
            this.notes = e.notes();
            this.lastModified = e.occurredAt();
        } else if (event instanceof DocumentVerified e) {
            this.status = DocumentStatus.VERIFIED;
            this.verifiedDate = e.verifiedDate();
            this.verifiedBy = e.verifiedBy();
            this.notes = e.notes();
            this.lastModified = e.occurredAt();
        } else if (event instanceof DocumentExpired e) {
            this.status = DocumentStatus.EXPIRED;
            this.lastModified = e.occurredAt();
        } else if (event instanceof DocumentRejected e) {
            this.status = DocumentStatus.REJECTED;
            this.lastModified = e.occurredAt();
        } else if (event instanceof DocumentWaived e) {
            this.status = DocumentStatus.WAIVED;
            this.lastModified = e.occurredAt();
        } else if (event instanceof DocumentExpirationUpdated e) {
            this.expirationDate = e.newExpirationDate();
            this.lastModified = e.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public boolean isExpiringWithin(int days) {
        if (expirationDate == null || status != DocumentStatus.VERIFIED) {
            return false;
        }
        return expirationDate.isBefore(LocalDate.now().plusDays(days + 1));
    }
    
    public boolean isOverdue() {
        if (status == DocumentStatus.REQUIRED && requiredDate != null) {
            return requiredDate.isBefore(LocalDate.now());
        }
        if (status == DocumentStatus.VERIFIED && expirationDate != null) {
            return expirationDate.isBefore(LocalDate.now());
        }
        return false;
    }
    
    public boolean requiresAction() {
        return status == DocumentStatus.REQUIRED || 
               status == DocumentStatus.RECEIVED ||
               isOverdue() ||
               isExpiringWithin(30);
    }
    
    // Getters
    public UUID getClientId() { return clientId; }
    public UUID getCaseId() { return caseId; }
    public String getDocumentName() { return documentName; }
    public DocumentType getDocumentType() { return documentType; }
    public DocumentStatus getStatus() { return status; }
    public LocalDate getRequiredDate() { return requiredDate; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public LocalDate getVerifiedDate() { return verifiedDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getDescription() { return description; }
    public String getSubmittedBy() { return submittedBy; }
    public String getVerifiedBy() { return verifiedBy; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
}