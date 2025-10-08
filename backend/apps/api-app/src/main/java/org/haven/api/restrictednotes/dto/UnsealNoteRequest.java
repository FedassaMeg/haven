package org.haven.api.restrictednotes.dto;

public class UnsealNoteRequest {
    private String unsealReason;
    private String legalBasis;
    
    public String getUnsealReason() { return unsealReason; }
    public void setUnsealReason(String unsealReason) { this.unsealReason = unsealReason; }
    
    public String getLegalBasis() { return legalBasis; }
    public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
}