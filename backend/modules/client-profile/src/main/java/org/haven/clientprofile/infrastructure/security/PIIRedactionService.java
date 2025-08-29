package org.haven.clientprofile.infrastructure.security;

import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.clientprofile.domain.pii.PIIAccessLevel;
import org.haven.clientprofile.domain.pii.PIICategory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for applying "minimum necessary" redaction to PII data
 * Implements HIPAA-style data minimization principles
 */
@Service
public class PIIRedactionService {
    
    /**
     * Applies redaction to any object based on user's access context
     * Default behavior follows "minimum necessary" principle
     */
    @SuppressWarnings("unchecked")
    public <T> T applyRedaction(T data, PIIAccessContext context, UUID clientId) {
        if (data == null) return null;
        
        try {
            // Create a copy to avoid modifying original
            T redactedData = (T) cloneObject(data);
            
            Class<?> clazz = redactedData.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                
                PIICategory category = classifyField(field.getName());
                PIIAccessLevel requiredLevel = getMinimumRequiredLevel(category);
                
                if (!context.hasAccess(category, requiredLevel)) {
                    Object redactedValue = getRedactedValue(field, field.get(redactedData));
                    field.set(redactedData, redactedValue);
                }
            }
            
            return redactedData;
            
        } catch (Exception e) {
            throw new RedactionException("Failed to apply redaction", e);
        }
    }
    
    /**
     * Creates redacted projection for export/sharing
     * Applies strictest "minimum necessary" rules
     */
    public Map<String, Object> createExportProjection(Object data, PIIAccessContext context, 
                                                     ExportType exportType) {
        Map<String, Object> projection = new HashMap<>();
        
        if (data == null) return projection;
        
        try {
            Class<?> clazz = data.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(data);
                
                PIICategory category = classifyField(field.getName());
                PIIAccessLevel exportLevel = getExportAccessLevel(exportType, category);
                
                if (context.hasAccess(category, exportLevel)) {
                    projection.put(field.getName(), value);
                } else {
                    // Apply export-specific redaction
                    Object redactedValue = getExportRedactedValue(field.getName(), value, exportType);
                    if (redactedValue != null) {
                        projection.put(field.getName(), redactedValue);
                    }
                    // Omit field entirely if not allowed for export
                }
            }
            
        } catch (Exception e) {
            throw new RedactionException("Failed to create export projection", e);
        }
        
        return projection;
    }
    
    /**
     * Classifies field based on name to determine PII category
     */
    private PIICategory classifyField(String fieldName) {
        String lowerField = fieldName.toLowerCase();
        
        // Direct identifiers
        if (lowerField.contains("ssn") || lowerField.contains("socialsecurity") ||
            lowerField.contains("firstname") || lowerField.contains("lastname") ||
            lowerField.contains("fullname") || lowerField.contains("legalname")) {
            return PIICategory.DIRECT_IDENTIFIER;
        }
        
        // Quasi-identifiers (including race and ethnicity)
        if (lowerField.contains("birth") || lowerField.contains("dob") ||
            lowerField.contains("age") || lowerField.contains("address") ||
            lowerField.contains("phone") || lowerField.contains("zip") ||
            lowerField.contains("race") || lowerField.contains("ethnicity")) {
            return PIICategory.QUASI_IDENTIFIER;
        }
        
        // Contact information
        if (lowerField.contains("email") || lowerField.contains("contact") ||
            lowerField.contains("mobile") || lowerField.contains("telephone")) {
            return PIICategory.CONTACT_INFO;
        }
        
        // Sensitive attributes
        if (lowerField.contains("medical") || lowerField.contains("financial") ||
            lowerField.contains("income") || lowerField.contains("disability") ||
            lowerField.contains("diagnosis") || lowerField.contains("treatment")) {
            return PIICategory.SENSITIVE_ATTRIBUTE;
        }
        
        // Household information
        if (lowerField.contains("household") || lowerField.contains("family") ||
            lowerField.contains("dependent") || lowerField.contains("children")) {
            return PIICategory.HOUSEHOLD_INFO;
        }
        
        // Default to service data
        return PIICategory.SERVICE_DATA;
    }
    
    /**
     * Gets minimum required access level for a PII category
     * Implements "minimum necessary" defaults
     */
    private PIIAccessLevel getMinimumRequiredLevel(PIICategory category) {
        return switch (category) {
            case DIRECT_IDENTIFIER -> PIIAccessLevel.HIGHLY_CONFIDENTIAL;
            case SENSITIVE_ATTRIBUTE -> PIIAccessLevel.CONFIDENTIAL;
            case QUASI_IDENTIFIER, CONTACT_INFO, HOUSEHOLD_INFO -> PIIAccessLevel.RESTRICTED;
            case SERVICE_DATA -> PIIAccessLevel.INTERNAL;
        };
    }
    
    /**
     * Gets required access level for different export types
     */
    private PIIAccessLevel getExportAccessLevel(ExportType exportType, PIICategory category) {
        return switch (exportType) {
            case HMIS_EXPORT -> switch (category) {
                case DIRECT_IDENTIFIER -> PIIAccessLevel.CONFIDENTIAL;
                case SENSITIVE_ATTRIBUTE -> PIIAccessLevel.RESTRICTED;
                case QUASI_IDENTIFIER, CONTACT_INFO -> PIIAccessLevel.RESTRICTED;
                case HOUSEHOLD_INFO -> PIIAccessLevel.INTERNAL;
                case SERVICE_DATA -> PIIAccessLevel.INTERNAL;
            };
            case VSP_SHARING -> switch (category) {
                case DIRECT_IDENTIFIER, SENSITIVE_ATTRIBUTE -> PIIAccessLevel.HIGHLY_CONFIDENTIAL;
                case QUASI_IDENTIFIER, CONTACT_INFO, HOUSEHOLD_INFO -> PIIAccessLevel.CONFIDENTIAL;
                case SERVICE_DATA -> PIIAccessLevel.RESTRICTED;
            };
            case RESEARCH_DATASET -> switch (category) {
                case DIRECT_IDENTIFIER -> PIIAccessLevel.HIGHLY_CONFIDENTIAL; // Usually excluded
                case SENSITIVE_ATTRIBUTE -> PIIAccessLevel.CONFIDENTIAL;
                case QUASI_IDENTIFIER -> PIIAccessLevel.RESTRICTED;
                case CONTACT_INFO, HOUSEHOLD_INFO -> PIIAccessLevel.INTERNAL;
                case SERVICE_DATA -> PIIAccessLevel.PUBLIC;
            };
            case COURT_REPORTING -> switch (category) {
                case DIRECT_IDENTIFIER -> PIIAccessLevel.RESTRICTED;
                case SENSITIVE_ATTRIBUTE -> PIIAccessLevel.CONFIDENTIAL;
                case QUASI_IDENTIFIER, CONTACT_INFO -> PIIAccessLevel.INTERNAL;
                case HOUSEHOLD_INFO, SERVICE_DATA -> PIIAccessLevel.INTERNAL;
            };
        };
    }
    
    /**
     * Gets redacted value for unauthorized fields
     */
    private Object getRedactedValue(Field field, Object originalValue) {
        if (originalValue == null) return null;
        
        Class<?> fieldType = field.getType();
        
        if (fieldType == String.class) {
            String str = (String) originalValue;
            return getRedactedString(field.getName(), str);
        } else if (fieldType == java.time.LocalDate.class || fieldType == java.time.Instant.class) {
            return null; // Remove dates entirely
        } else if (fieldType.isPrimitive() || Number.class.isAssignableFrom(fieldType)) {
            return getRedactedNumber(fieldType);
        } else if (fieldType == UUID.class) {
            return null; // Remove UUIDs
        }
        
        return "[REDACTED]";
    }
    
    /**
     * Gets export-specific redacted value
     */
    private Object getExportRedactedValue(String fieldName, Object value, ExportType exportType) {
        if (value == null) return null;
        
        return switch (exportType) {
            case HMIS_EXPORT -> getHMISRedactedValue(fieldName, value);
            case VSP_SHARING -> null; // VSP sharing excludes unauthorized fields entirely
            case RESEARCH_DATASET -> getResearchRedactedValue(fieldName, value);
            case COURT_REPORTING -> getCourtRedactedValue(fieldName, value);
        };
    }
    
    private String getRedactedString(String fieldName, String originalValue) {
        String lowerField = fieldName.toLowerCase();
        
        if (lowerField.contains("ssn")) {
            return "***-**-****";
        } else if (lowerField.contains("phone")) {
            return "***-***-****";
        } else if (lowerField.contains("email")) {
            return "***@***.***";
        } else if (lowerField.contains("address")) {
            return "[ADDRESS REDACTED]";
        } else if (lowerField.contains("name")) {
            return "[NAME REDACTED]";
        }
        
        // Generic redaction based on length
        if (originalValue.length() <= 3) {
            return "*".repeat(originalValue.length());
        } else {
            return "*".repeat(originalValue.length() - 1) + originalValue.charAt(originalValue.length() - 1);
        }
    }
    
    private Object getRedactedNumber(Class<?> fieldType) {
        if (fieldType == int.class || fieldType == Integer.class) return 0;
        if (fieldType == long.class || fieldType == Long.class) return 0L;
        if (fieldType == double.class || fieldType == Double.class) return 0.0;
        if (fieldType == float.class || fieldType == Float.class) return 0.0f;
        return null;
    }
    
    private Object getHMISRedactedValue(String fieldName, Object value) {
        // HMIS allows some aggregate data
        if (value instanceof String str) {
            return getRedactedString(fieldName, str);
        }
        return null;
    }
    
    private Object getResearchRedactedValue(String fieldName, Object value) {
        // Research datasets may include generalized data
        if (fieldName.toLowerCase().contains("age")) {
            return "[AGE_RANGE]";
        }
        if (fieldName.toLowerCase().contains("zip")) {
            return "[ZIP_PREFIX]";
        }
        return null;
    }
    
    private Object getCourtRedactedValue(String fieldName, Object value) {
        // Court reporting may include partial information
        if (value instanceof String str && fieldName.toLowerCase().contains("name")) {
            return str.charAt(0) + ".";
        }
        return getRedactedString(fieldName, (String) value);
    }
    
    /**
     * Creates a shallow copy of an object
     */
    private Object cloneObject(Object original) throws Exception {
        Class<?> clazz = original.getClass();
        Object copy = clazz.getDeclaredConstructor().newInstance();
        
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            field.set(copy, field.get(original));
        }
        
        return copy;
    }
    
    /**
     * Export types requiring different redaction levels
     */
    public enum ExportType {
        HMIS_EXPORT,        // Standard HMIS data sharing
        VSP_SHARING,        // Victim Service Provider sharing (most restrictive)
        RESEARCH_DATASET,   // De-identified research data
        COURT_REPORTING     // Court-ordered reporting
    }
    
    /**
     * Exception for redaction errors
     */
    public static class RedactionException extends RuntimeException {
        public RedactionException(String message) {
            super(message);
        }
        
        public RedactionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}