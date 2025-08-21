package org.haven.safetyassessment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Contact Safety Preferences for client communication security
 * Controls how and when staff can safely contact clients
 * Maintains history without exposing to unauthorized roles
 */
public class ContactSafetyPreferences {
    private UUID preferencesId;
    private UUID clientId;
    private ContactSafetyLevel safetyLevel;
    
    // Safe contact methods
    private List<SafeContactMethod> approvedContactMethods = new ArrayList<>();
    private List<String> safePhoneNumbers = new ArrayList<>();
    private List<String> safeEmailAddresses = new ArrayList<>();
    
    // Unsafe contact restrictions
    private List<String> unsafePhoneNumbers = new ArrayList<>();
    private List<String> unsafeEmailAddresses = new ArrayList<>();
    private List<String> unsafeLocations = new ArrayList<>();
    private boolean noHomeVisits;
    private boolean noWorkplaceContact;
    
    // Time-based restrictions
    private SafeContactTimeWindow safeTimeWindow;
    private List<String> unsafeDaysOfWeek = new ArrayList<>();
    private String specialInstructions;
    
    // Communication content restrictions
    private boolean useCodeWords;
    private String codeWordForSafe;
    private String codeWordForDanger;
    private boolean noVoicemails;
    private boolean noTextMessages;
    private boolean requireEncryptedCommunication;
    
    // Emergency override settings
    private boolean allowEmergencyOverride;
    private List<String> emergencyOverrideRoles = new ArrayList<>();
    
    // Metadata
    private String lastUpdatedBy;
    private String lastUpdatedByRole;
    private Instant createdAt;
    private Instant lastModified;
    private Integer versionNumber;
    
    public ContactSafetyPreferences(UUID clientId, ContactSafetyLevel safetyLevel, String createdBy) {
        this.preferencesId = UUID.randomUUID();
        this.clientId = clientId;
        this.safetyLevel = safetyLevel;
        this.lastUpdatedBy = createdBy;
        this.versionNumber = 1;
        this.createdAt = Instant.now();
        this.lastModified = Instant.now();
        this.allowEmergencyOverride = true; // Safe default
    }
    
    public void addSafeContactMethod(SafeContactMethod method) {
        if (!approvedContactMethods.contains(method)) {
            approvedContactMethods.add(method);
        }
        updateModificationTime();
    }
    
    public void removeSafeContactMethod(SafeContactMethod method) {
        approvedContactMethods.remove(method);
        updateModificationTime();
    }
    
    public void addSafePhoneNumber(String phoneNumber, String label) {
        String safePhone = phoneNumber + (label != null ? " (" + label + ")" : "");
        if (!safePhoneNumbers.contains(safePhone)) {
            safePhoneNumbers.add(safePhone);
        }
        updateModificationTime();
    }
    
    public void addUnsafePhoneNumber(String phoneNumber, String reason) {
        String unsafePhone = phoneNumber + (reason != null ? " [UNSAFE: " + reason + "]" : " [UNSAFE]");
        if (!unsafePhoneNumbers.contains(unsafePhone)) {
            unsafePhoneNumbers.add(unsafePhone);
        }
        updateModificationTime();
    }
    
    public void addSafeEmailAddress(String email, String label) {
        String safeEmail = email + (label != null ? " (" + label + ")" : "");
        if (!safeEmailAddresses.contains(safeEmail)) {
            safeEmailAddresses.add(safeEmail);
        }
        updateModificationTime();
    }
    
    public void addUnsafeLocation(String location, String reason) {
        String unsafeLocation = location + (reason != null ? " [REASON: " + reason + "]" : "");
        if (!unsafeLocations.contains(unsafeLocation)) {
            unsafeLocations.add(unsafeLocation);
        }
        updateModificationTime();
    }
    
    public void setSafeTimeWindow(SafeContactTimeWindow timeWindow) {
        this.safeTimeWindow = timeWindow;
        updateModificationTime();
    }
    
    public void setCodeWords(String safeWord, String dangerWord) {
        this.useCodeWords = true;
        this.codeWordForSafe = safeWord;
        this.codeWordForDanger = dangerWord;
        updateModificationTime();
    }
    
    public void updateSafetyLevel(ContactSafetyLevel newLevel, String updatedBy, String reason) {
        this.safetyLevel = newLevel;
        this.lastUpdatedBy = updatedBy;
        this.specialInstructions = (specialInstructions != null ? specialInstructions + "; " : "") + 
                                   "Safety level changed: " + reason;
        updateModificationTime();
    }
    
    public boolean isContactMethodSafe(String contactInfo, ContactMethod method) {
        switch (method) {
            case PHONE_CALL, SMS -> {
                return safePhoneNumbers.stream().anyMatch(safe -> safe.contains(contactInfo)) &&
                       unsafePhoneNumbers.stream().noneMatch(unsafe -> unsafe.contains(contactInfo));
            }
            case EMAIL -> {
                return safeEmailAddresses.stream().anyMatch(safe -> safe.contains(contactInfo)) &&
                       unsafeEmailAddresses.stream().noneMatch(unsafe -> unsafe.contains(contactInfo));
            }
            case IN_PERSON -> {
                return !noHomeVisits && !noWorkplaceContact &&
                       unsafeLocations.stream().noneMatch(unsafe -> unsafe.toLowerCase().contains("home") || 
                                                                     unsafe.toLowerCase().contains("work"));
            }
            default -> { return false; }
        }
    }
    
    public boolean isTimeWindowSafe(Instant proposedContactTime) {
        if (safeTimeWindow == null) {
            return true; // No restrictions
        }
        
        // Implementation would check if proposedContactTime falls within safe window
        // This is a simplified version
        return true;
    }
    
    public boolean canUserModify(String userId, String userRole) {
        // Case managers and supervisors can modify contact preferences
        return "CASE_MANAGER".equals(userRole) || 
               "SUPERVISOR".equals(userRole) || 
               "ADMIN".equals(userRole) ||
               lastUpdatedBy.equals(userId);
    }
    
    public boolean requiresEmergencyOverride(ContactMethod method, String contactInfo) {
        if (!allowEmergencyOverride) {
            return false;
        }
        
        // Check if contact method violates safety preferences
        return !isContactMethodSafe(contactInfo, method);
    }
    
    private void updateModificationTime() {
        this.lastModified = Instant.now();
        this.versionNumber++;
    }
    
    public enum ContactSafetyLevel {
        STANDARD("Standard safety precautions"),
        ELEVATED("Elevated safety concerns - extra caution required"),
        HIGH_RISK("High risk - limited contact methods only"),
        EMERGENCY_ONLY("Emergency contact only - all routine contact suspended");
        
        private final String description;
        
        ContactSafetyLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public enum SafeContactMethod {
        SECURE_PHONE("Secure/verified phone number"),
        SECURE_EMAIL("Secure/encrypted email"),
        OFFICE_VISIT("Office visit only"),
        DESIGNATED_LOCATION("Pre-designated safe location"),
        INTERMEDIARY_CONTACT("Contact through trusted intermediary"),
        ENCRYPTED_MESSAGE("Encrypted messaging app");
        
        private final String description;
        
        SafeContactMethod(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public enum ContactMethod {
        PHONE_CALL,
        SMS,
        EMAIL,
        IN_PERSON,
        VOICEMAIL,
        ENCRYPTED_MESSAGE
    }
    
    public static class SafeContactTimeWindow {
        private String startTime; // e.g., "09:00"
        private String endTime;   // e.g., "17:00" 
        private List<String> safeDaysOfWeek = new ArrayList<>(); // e.g., ["MONDAY", "TUESDAY"]
        private String timeZone;
        private String notes;
        
        public SafeContactTimeWindow(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        // Getters and setters
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public List<String> getSafeDaysOfWeek() { return List.copyOf(safeDaysOfWeek); }
        public void setSafeDaysOfWeek(List<String> days) { this.safeDaysOfWeek = new ArrayList<>(days); }
        public String getTimeZone() { return timeZone; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    // Getters
    public UUID getPreferencesId() { return preferencesId; }
    public UUID getClientId() { return clientId; }
    public ContactSafetyLevel getSafetyLevel() { return safetyLevel; }
    public List<SafeContactMethod> getApprovedContactMethods() { return List.copyOf(approvedContactMethods); }
    public List<String> getSafePhoneNumbers() { return List.copyOf(safePhoneNumbers); }
    public List<String> getSafeEmailAddresses() { return List.copyOf(safeEmailAddresses); }
    public List<String> getUnsafePhoneNumbers() { return List.copyOf(unsafePhoneNumbers); }
    public List<String> getUnsafeEmailAddresses() { return List.copyOf(unsafeEmailAddresses); }
    public List<String> getUnsafeLocations() { return List.copyOf(unsafeLocations); }
    public boolean isNoHomeVisits() { return noHomeVisits; }
    public boolean isNoWorkplaceContact() { return noWorkplaceContact; }
    public SafeContactTimeWindow getSafeTimeWindow() { return safeTimeWindow; }
    public List<String> getUnsafeDaysOfWeek() { return List.copyOf(unsafeDaysOfWeek); }
    public String getSpecialInstructions() { return specialInstructions; }
    public boolean isUseCodeWords() { return useCodeWords; }
    public String getCodeWordForSafe() { return codeWordForSafe; }
    public String getCodeWordForDanger() { return codeWordForDanger; }
    public boolean isNoVoicemails() { return noVoicemails; }
    public boolean isNoTextMessages() { return noTextMessages; }
    public boolean isRequireEncryptedCommunication() { return requireEncryptedCommunication; }
    public boolean isAllowEmergencyOverride() { return allowEmergencyOverride; }
    public List<String> getEmergencyOverrideRoles() { return List.copyOf(emergencyOverrideRoles); }
    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public String getLastUpdatedByRole() { return lastUpdatedByRole; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    public Integer getVersionNumber() { return versionNumber; }
    
    // Setters with proper modification tracking
    public void setNoHomeVisits(boolean noHomeVisits) { 
        this.noHomeVisits = noHomeVisits; 
        updateModificationTime(); 
    }
    public void setNoWorkplaceContact(boolean noWorkplaceContact) { 
        this.noWorkplaceContact = noWorkplaceContact; 
        updateModificationTime(); 
    }
    public void setNoVoicemails(boolean noVoicemails) { 
        this.noVoicemails = noVoicemails; 
        updateModificationTime(); 
    }
    public void setNoTextMessages(boolean noTextMessages) { 
        this.noTextMessages = noTextMessages; 
        updateModificationTime(); 
    }
    public void setRequireEncryptedCommunication(boolean requireEncryptedCommunication) { 
        this.requireEncryptedCommunication = requireEncryptedCommunication; 
        updateModificationTime(); 
    }
    public void setAllowEmergencyOverride(boolean allowEmergencyOverride) { 
        this.allowEmergencyOverride = allowEmergencyOverride; 
        updateModificationTime(); 
    }
    public void setSpecialInstructions(String specialInstructions) { 
        this.specialInstructions = specialInstructions; 
        updateModificationTime(); 
    }
}