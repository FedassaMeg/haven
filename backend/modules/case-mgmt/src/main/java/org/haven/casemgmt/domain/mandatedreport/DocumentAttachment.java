package org.haven.casemgmt.domain.mandatedreport;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Document attachment for mandated reports
 * Links to document management system
 */
public class DocumentAttachment {
    
    private final UUID documentId;
    private final String fileName;
    private final String documentType;
    private final long fileSize;
    private final String mimeType;
    private final Instant attachedAt;
    private final UUID attachedByUserId;
    private final boolean isRequired;
    private final String description;
    
    public DocumentAttachment(UUID documentId, String fileName, String documentType,
                            long fileSize, String mimeType, UUID attachedByUserId,
                            boolean isRequired, String description) {
        this.documentId = Objects.requireNonNull(documentId, "Document ID cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "File name cannot be null");
        this.documentType = documentType;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.attachedAt = Instant.now();
        this.attachedByUserId = Objects.requireNonNull(attachedByUserId, "Attached by user ID cannot be null");
        this.isRequired = isRequired;
        this.description = description;
    }
    
    /**
     * Create required document attachment
     */
    public static DocumentAttachment required(UUID documentId, String fileName, String documentType,
                                            long fileSize, String mimeType, UUID attachedByUserId,
                                            String description) {
        return new DocumentAttachment(documentId, fileName, documentType, fileSize, mimeType,
                                    attachedByUserId, true, description);
    }
    
    /**
     * Create optional document attachment
     */
    public static DocumentAttachment optional(UUID documentId, String fileName, String documentType,
                                            long fileSize, String mimeType, UUID attachedByUserId,
                                            String description) {
        return new DocumentAttachment(documentId, fileName, documentType, fileSize, mimeType,
                                    attachedByUserId, false, description);
    }
    
    /**
     * Check if document is a supported type for mandated reports
     */
    public boolean isSupportedType() {
        return mimeType != null && (
            mimeType.startsWith("image/") ||
            mimeType.equals("application/pdf") ||
            mimeType.startsWith("text/") ||
            mimeType.contains("document") ||
            mimeType.contains("spreadsheet")
        );
    }
    
    /**
     * Check if file size is within limits (10MB max for reports)
     */
    public boolean isWithinSizeLimit() {
        return fileSize <= 10 * 1024 * 1024; // 10MB
    }
    
    /**
     * Validate attachment meets requirements
     */
    public void validate() {
        if (!isSupportedType()) {
            throw new IllegalArgumentException("Unsupported document type: " + mimeType);
        }
        
        if (!isWithinSizeLimit()) {
            throw new IllegalArgumentException("Document size exceeds 10MB limit");
        }
        
        if (fileName.length() > 255) {
            throw new IllegalArgumentException("File name too long (max 255 characters)");
        }
    }
    
    // Getters
    public UUID getDocumentId() { return documentId; }
    public String getFileName() { return fileName; }
    public String getDocumentType() { return documentType; }
    public long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public Instant getAttachedAt() { return attachedAt; }
    public UUID getAttachedByUserId() { return attachedByUserId; }
    public boolean isRequired() { return isRequired; }
    public String getDescription() { return description; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentAttachment that = (DocumentAttachment) o;
        return Objects.equals(documentId, that.documentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(documentId);
    }
}