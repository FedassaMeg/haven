package org.haven.clientprofile.infrastructure.security;

import org.haven.clientprofile.domain.DataSystem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for enforcing VSP (Victim Service Provider) data access restrictions
 * Ensures VSPs cannot access HMIS PII data per HUD requirements
 */
@Service
public class VSPDataAccessService {
    
    private final PIIRedactionService redactionService;
    
    public VSPDataAccessService(PIIRedactionService redactionService) {
        this.redactionService = redactionService;
    }
    
    /**
     * Filters client data for VSP access
     * Removes all HMIS PII fields, leaves only ComparableDB data
     */
    public <T> T filterForVSPAccess(T clientData, UUID userId, List<String> userRoles) {
        if (!isVSPUser(userRoles)) {
            return clientData; // Non-VSP users get full access based on normal permissions
        }
        
        // VSP users get heavily redacted view with no HMIS PII
        return applyVSPRedaction(clientData);
    }
    
    /**
     * Creates VSP-safe export projection
     * Removes all identifying information per HUD guidelines
     */
    public Map<String, Object> createVSPExportProjection(Object clientData, UUID userId) {
        VSPRedactionContext context = new VSPRedactionContext(userId);
        
        return redactionService.createExportProjection(
            clientData, 
            context,
            PIIRedactionService.ExportType.VSP_SHARING
        );
    }
    
    /**
     * Validates VSP user can access specific client data
     * Enforces ComparableDB-only restriction
     */
    public boolean canVSPAccessClient(UUID clientId, List<String> userRoles) {
        if (!isVSPUser(userRoles)) {
            return true; // Non-VSP access handled by normal permissions
        }
        
        // Check if client is restricted to ComparableDB only
        ClientDataSystemRestriction restriction = getClientDataSystemRestriction(clientId);
        
        if (restriction.getDataSystem() == DataSystem.HMIS && 
            !restriction.isComparableDbAccessAllowed()) {
            return false; // VSP cannot access HMIS-only clients
        }
        
        return true;
    }
    
    /**
     * Gets list of clients accessible to VSP user
     * Filters out HMIS-only clients
     */
    public List<UUID> getVSPAccessibleClients(UUID vspUserId, List<String> userRoles) {
        if (!isVSPUser(userRoles)) {
            throw new IllegalArgumentException("User is not a VSP");
        }
        
        // This would query the database for clients with ComparableDB access
        // For now, return empty list as placeholder
        return List.of();
    }
    
    /**
     * Logs VSP data access attempt for audit
     */
    public void logVSPDataAccess(UUID userId, UUID clientId, String dataType, 
                               boolean accessGranted, String reason) {
        
        VSPAccessAuditLog logEntry = new VSPAccessAuditLog(
            userId, clientId, dataType, accessGranted, reason, 
            java.time.Instant.now()
        );
        
        // This would be persisted to audit table
        System.out.println("VSP_ACCESS_LOG: " + logEntry);
    }
    
    /**
     * Checks if user has VSP role
     */
    private boolean isVSPUser(List<String> userRoles) {
        return userRoles.contains("VSP") || 
               userRoles.contains("VICTIM_SERVICE_PROVIDER") ||
               userRoles.contains("COMMUNITY_PARTNER");
    }
    
    /**
     * Applies heavy redaction for VSP users
     */
    @SuppressWarnings("unchecked")
    private <T> T applyVSPRedaction(T data) {
        // VSP users get minimal data - mostly service information only
        try {
            T redactedData = (T) cloneObject(data);
            
            Class<?> clazz = redactedData.getClass();
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                
                if (isHMISPIIField(field.getName())) {
                    // Remove HMIS PII fields entirely
                    field.set(redactedData, null);
                } else if (isQuasiIdentifier(field.getName())) {
                    // Heavily redact quasi-identifiers
                    Object value = field.get(redactedData);
                    if (value instanceof String) {
                        field.set(redactedData, "[REDACTED_FOR_VSP]");
                    } else {
                        field.set(redactedData, null);
                    }
                }
                // Service data fields are left intact
            }
            
            return redactedData;
            
        } catch (Exception e) {
            throw new VSPAccessException("Failed to apply VSP redaction", e);
        }
    }
    
    /**
     * Checks if field contains HMIS PII that VSPs cannot access
     */
    private boolean isHMISPIIField(String fieldName) {
        String lowerField = fieldName.toLowerCase();
        return lowerField.contains("ssn") ||
               lowerField.contains("firstname") ||
               lowerField.contains("lastname") ||
               lowerField.contains("fullname") ||
               lowerField.contains("legalname") ||
               lowerField.contains("dob") ||
               lowerField.contains("birth") ||
               lowerField.contains("address") ||
               lowerField.contains("phone") ||
               lowerField.contains("email");
    }
    
    /**
     * Checks if field is a quasi-identifier requiring redaction
     */
    private boolean isQuasiIdentifier(String fieldName) {
        String lowerField = fieldName.toLowerCase();
        return lowerField.contains("age") ||
               lowerField.contains("gender") ||
               lowerField.contains("race") ||
               lowerField.contains("ethnicity") ||
               lowerField.contains("zip");
    }
    
    /**
     * Gets client data system restriction
     */
    private ClientDataSystemRestriction getClientDataSystemRestriction(UUID clientId) {
        // This would query the confidentiality_guardrails table
        // For now, return a default
        return new ClientDataSystemRestriction(clientId, DataSystem.COMPARABLE_DB, true);
    }
    
    /**
     * Creates a shallow copy of an object
     */
    private Object cloneObject(Object original) throws Exception {
        Class<?> clazz = original.getClass();
        Object copy = clazz.getDeclaredConstructor().newInstance();
        
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            field.setAccessible(true);
            field.set(copy, field.get(original));
        }
        
        return copy;
    }
    
    /**
     * VSP redaction context for the redaction service
     */
    private static class VSPRedactionContext extends org.haven.clientprofile.domain.pii.PIIAccessContext {
        
        public VSPRedactionContext(UUID userId) {
            super(userId, List.of("VSP"), "VSP data access", 
                  UUID.randomUUID(), "session-" + System.currentTimeMillis(), 
                  "127.0.0.1");
        }
        
        // Override hasAccess method is already inherited from PIIAccessContext
    }
    
    /**
     * Client data system restriction information
     */
    public static class ClientDataSystemRestriction {
        private final UUID clientId;
        private final DataSystem dataSystem;
        private final boolean comparableDbAccessAllowed;
        
        public ClientDataSystemRestriction(UUID clientId, DataSystem dataSystem, 
                                         boolean comparableDbAccessAllowed) {
            this.clientId = clientId;
            this.dataSystem = dataSystem;
            this.comparableDbAccessAllowed = comparableDbAccessAllowed;
        }
        
        public UUID getClientId() { return clientId; }
        public DataSystem getDataSystem() { return dataSystem; }
        public boolean isComparableDbAccessAllowed() { return comparableDbAccessAllowed; }
    }
    
    /**
     * VSP access audit log entry
     */
    public static class VSPAccessAuditLog {
        private final UUID userId;
        private final UUID clientId;
        private final String dataType;
        private final boolean accessGranted;
        private final String reason;
        private final java.time.Instant accessTime;
        
        public VSPAccessAuditLog(UUID userId, UUID clientId, String dataType, 
                               boolean accessGranted, String reason, java.time.Instant accessTime) {
            this.userId = userId;
            this.clientId = clientId;
            this.dataType = dataType;
            this.accessGranted = accessGranted;
            this.reason = reason;
            this.accessTime = accessTime;
        }
        
        @Override
        public String toString() {
            return String.format("VSPAccess[user=%s, client=%s, type=%s, granted=%s, reason=%s, time=%s]",
                userId, clientId, dataType, accessGranted, reason, accessTime);
        }
    }
    
    /**
     * Exception for VSP access violations
     */
    public static class VSPAccessException extends RuntimeException {
        public VSPAccessException(String message) {
            super(message);
        }
        
        public VSPAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}