package org.haven.api.restrictednotes.dto;

import java.time.Instant;

public class SealNoteRequest {
    private String sealReason;
    private String legalBasis;
    private boolean temporary;
    private Instant expiresAt;
    
    public String getSealReason() { return sealReason; }
    public void setSealReason(String sealReason) { this.sealReason = sealReason; }
    
    public String getLegalBasis() { return legalBasis; }
    public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
    
    public boolean isTemporary() { return temporary; }
    public void setTemporary(boolean temporary) { this.temporary = temporary; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}