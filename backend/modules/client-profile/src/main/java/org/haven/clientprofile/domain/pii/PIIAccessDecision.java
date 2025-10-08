package org.haven.clientprofile.domain.pii;

public class PIIAccessDecision {
    private final boolean allowed;
    private final String reason;
    private final PIIAccessPermission permission;
    
    private PIIAccessDecision(boolean allowed, String reason, PIIAccessPermission permission) {
        this.allowed = allowed;
        this.reason = reason;
        this.permission = permission;
    }
    
    public static PIIAccessDecision allowed(PIIAccessPermission permission) {
        return new PIIAccessDecision(true, "Access granted", permission);
    }
    
    public static PIIAccessDecision denied(String reason) {
        return new PIIAccessDecision(false, reason, null);
    }
    
    public boolean isAllowed() {
        return allowed;
    }
    
    public String getReason() {
        return reason;
    }
    
    public PIIAccessPermission getPermission() {
        return permission;
    }
    
    public void throwIfDenied() {
        if (!allowed) {
            throw new PIIAccessDeniedException(reason);
        }
    }
}