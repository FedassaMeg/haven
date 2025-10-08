package org.haven.readmodels.domain;

import java.time.Instant;
import java.util.UUID;

public class ConfidentialityGuardrails {
    private UUID clientId;
    private String clientName;
    private Boolean isSafeAtHome;
    private Boolean isComparableDbOnly;
    private Boolean hasConfidentialLocation;
    private Boolean hasRestrictedData;
    private String dataSystem;
    private VisibilityLevel visibilityLevel;
    private Instant lastUpdated;
    
    public enum VisibilityLevel {
        PUBLIC,           // Standard data sharing
        RESTRICTED,       // Limited data sharing
        CONFIDENTIAL,     // Restricted to authorized personnel only
        PRIVILEGED        // Attorney-client privilege or similar
    }
    
    public ConfidentialityGuardrails() {}
    
    public ConfidentialityGuardrails(UUID clientId, String clientName) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.isSafeAtHome = false;
        this.isComparableDbOnly = false;
        this.hasConfidentialLocation = false;
        this.hasRestrictedData = false;
        this.dataSystem = "HMIS";
        this.visibilityLevel = VisibilityLevel.PUBLIC;
        this.lastUpdated = Instant.now();
    }
    
    public boolean requiresBannerWarning() {
        return isSafeAtHome || isComparableDbOnly || hasConfidentialLocation || hasRestrictedData;
    }
    
    public String getBannerWarningText() {
        if (!requiresBannerWarning()) {
            return null;
        }
        
        StringBuilder warning = new StringBuilder("⚠️ CONFIDENTIAL CLIENT: ");
        
        if (isSafeAtHome) {
            warning.append("Safe at Home participant. ");
        }
        
        if (isComparableDbOnly) {
            warning.append("Comparable DB only - no HMIS sharing. ");
        }
        
        if (hasConfidentialLocation) {
            warning.append("Confidential location protection. ");
        }
        
        if (hasRestrictedData) {
            warning.append("Contains privileged/restricted information. ");
        }
        
        return warning.toString().trim();
    }
    
    public String getBannerSeverity() {
        if (isSafeAtHome || hasRestrictedData) {
            return "CRITICAL";
        } else if (isComparableDbOnly || hasConfidentialLocation) {
            return "HIGH";
        }
        return "MEDIUM";
    }
    
    public boolean canShare() {
        return !isComparableDbOnly && visibilityLevel != VisibilityLevel.PRIVILEGED;
    }
    
    public boolean requiresSpecialHandling() {
        return isSafeAtHome || hasConfidentialLocation || visibilityLevel == VisibilityLevel.PRIVILEGED;
    }
    
    // Getters and Setters
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public Boolean getIsSafeAtHome() { return isSafeAtHome; }
    public void setIsSafeAtHome(Boolean isSafeAtHome) { this.isSafeAtHome = isSafeAtHome; }
    
    public Boolean getIsComparableDbOnly() { return isComparableDbOnly; }
    public void setIsComparableDbOnly(Boolean isComparableDbOnly) { this.isComparableDbOnly = isComparableDbOnly; }
    
    public Boolean getHasConfidentialLocation() { return hasConfidentialLocation; }
    public void setHasConfidentialLocation(Boolean hasConfidentialLocation) { this.hasConfidentialLocation = hasConfidentialLocation; }
    
    public Boolean getHasRestrictedData() { return hasRestrictedData; }
    public void setHasRestrictedData(Boolean hasRestrictedData) { this.hasRestrictedData = hasRestrictedData; }
    
    public String getDataSystem() { return dataSystem; }
    public void setDataSystem(String dataSystem) { this.dataSystem = dataSystem; }
    
    public VisibilityLevel getVisibilityLevel() { return visibilityLevel; }
    public void setVisibilityLevel(VisibilityLevel visibilityLevel) { this.visibilityLevel = visibilityLevel; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}