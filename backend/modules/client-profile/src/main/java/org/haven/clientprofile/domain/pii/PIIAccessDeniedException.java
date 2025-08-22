package org.haven.clientprofile.domain.pii;

public class PIIAccessDeniedException extends RuntimeException {
    
    public PIIAccessDeniedException(String message) {
        super(message);
    }
    
    public PIIAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}