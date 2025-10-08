package org.haven.clientprofile.domain.pii;

public enum PIICategory {
    DIRECT_IDENTIFIER("Name, SSN, Photo", PIIAccessLevel.HIGHLY_CONFIDENTIAL),
    QUASI_IDENTIFIER("DOB, Address, Phone", PIIAccessLevel.RESTRICTED),
    SENSITIVE_ATTRIBUTE("Medical, Financial, Legal", PIIAccessLevel.CONFIDENTIAL),
    CONTACT_INFO("Email, Phone, Address", PIIAccessLevel.RESTRICTED),
    HOUSEHOLD_INFO("Family composition", PIIAccessLevel.RESTRICTED),
    SERVICE_DATA("Non-PII service information", PIIAccessLevel.INTERNAL);
    
    private final String description;
    private final PIIAccessLevel defaultAccessLevel;
    
    PIICategory(String description, PIIAccessLevel defaultAccessLevel) {
        this.description = description;
        this.defaultAccessLevel = defaultAccessLevel;
    }
    
    public String getDescription() { return description; }
    public PIIAccessLevel getDefaultAccessLevel() { return defaultAccessLevel; }
    
    public boolean requiresSpecialHandling() {
        return this == DIRECT_IDENTIFIER || this == SENSITIVE_ATTRIBUTE;
    }
    
    public boolean isHighRisk() {
        return defaultAccessLevel.getLevel() >= PIIAccessLevel.CONFIDENTIAL.getLevel();
    }
}