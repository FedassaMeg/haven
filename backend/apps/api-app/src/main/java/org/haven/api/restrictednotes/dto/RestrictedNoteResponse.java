package org.haven.api.restrictednotes.dto;

import java.util.UUID;

public class RestrictedNoteResponse {
    private UUID noteId;
    private String message;
    private boolean success = true;
    
    public UUID getNoteId() { return noteId; }
    public void setNoteId(UUID noteId) { this.noteId = noteId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}