package org.haven.casemgmt.application.commands;

import java.util.UUID;

/**
 * Command to attach a document to a mandated report
 */
public record AttachDocumentCmd(
    UUID reportId,
    UUID documentId,
    String fileName,
    String documentType,
    long fileSize,
    String mimeType,
    UUID attachedByUserId,
    boolean isRequired,
    String description
) {
    
    public AttachDocumentCmd {
        if (reportId == null) {
            throw new IllegalArgumentException("Report ID cannot be null");
        }
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (attachedByUserId == null) {
            throw new IllegalArgumentException("Attached by user ID cannot be null");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
    }
    
    /**
     * Validate command for business rules
     */
    public void validate() {
        // File size limit (10MB)
        if (fileSize > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size cannot exceed 10MB");
        }
        
        // File name length limit
        if (fileName.length() > 255) {
            throw new IllegalArgumentException("File name cannot exceed 255 characters");
        }
        
        // Validate file type for mandated reports
        if (mimeType != null) {
            String mime = mimeType.toLowerCase();
            if (!isAllowedMimeType(mime)) {
                throw new IllegalArgumentException("File type not allowed for mandated reports: " + mimeType);
            }
        }
    }
    
    private boolean isAllowedMimeType(String mimeType) {
        return mimeType.startsWith("image/") ||
               mimeType.equals("application/pdf") ||
               mimeType.startsWith("text/") ||
               mimeType.contains("document") ||
               mimeType.contains("spreadsheet") ||
               mimeType.contains("wordprocessing");
    }
}