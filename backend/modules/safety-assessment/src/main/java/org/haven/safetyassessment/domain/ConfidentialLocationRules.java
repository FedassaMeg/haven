package org.haven.safetyassessment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Confidential Location Rules for protecting client safety through address/location confidentiality
 * Controls which staff can access location information and under what circumstances
 * Maintains audit trail without exposing confidential locations to unauthorized roles
 */
public class ConfidentialLocationRules {
    private UUID rulesId;
    private UUID clientId;
    private LocationConfidentialityLevel confidentialityLevel;
    
    // Protected locations
    private List<ProtectedLocation> confidentialLocations = new ArrayList<>();
    private List<String> safeLocationCategories = new ArrayList<>();
    
    // Access control
    private List<String> authorizedRoles = new ArrayList<>();
    private List<String> authorizedUsers = new ArrayList<>();
    private Map<String, LocationAccessPermission> rolePermissions = new HashMap<>();
    
    // Emergency access
    private boolean allowEmergencyAccess;
    private List<String> emergencyAccessRoles = new ArrayList<>();
    private String emergencyAccessProtocol;
    
    // Geographic restrictions
    private List<String> restrictedZipCodes = new ArrayList<>();
    private List<String> restrictedNeighborhoods = new ArrayList<>();
    private Double restrictedRadius; // Miles from a specific point
    private String restrictedRadiusCenter; // Address or coordinates
    
    // Disclosure rules
    private boolean allowPartialAddressSharing; // Street but not number
    private boolean allowGeneralAreaSharing; // Neighborhood/zip only
    private boolean allowNoAddressSharing; // No location info at all
    private List<String> approvedLocationSharingContexts = new ArrayList<>();
    
    // Audit and compliance
    private List<LocationAccessAudit> accessAuditTrail = new ArrayList<>();
    private String legalBasisForConfidentiality;
    private LocalDate confidentialityExpirationDate;
    private boolean requiresCourtOrderToDisclose;
    
    // Metadata
    private String establishedBy;
    private String establishedByRole;
    private Instant createdAt;
    private Instant lastModified;
    private Integer versionNumber;
    
    public ConfidentialLocationRules(UUID clientId, LocationConfidentialityLevel confidentialityLevel, String establishedBy, String establishedByRole) {
        this.rulesId = UUID.randomUUID();
        this.clientId = clientId;
        this.confidentialityLevel = confidentialityLevel;
        this.establishedBy = establishedBy;
        this.establishedByRole = establishedByRole;
        this.allowEmergencyAccess = true; // Safe default
        this.versionNumber = 1;
        this.createdAt = Instant.now();
        this.lastModified = Instant.now();
        
        // Initialize default permissions based on confidentiality level
        initializeDefaultPermissions();
    }
    
    private void initializeDefaultPermissions() {
        switch (confidentialityLevel) {
            case PUBLIC -> {
                allowPartialAddressSharing = true;
                allowGeneralAreaSharing = true;
                authorizedRoles.addAll(List.of("CASE_MANAGER", "SUPERVISOR", "ADMIN", "SERVICE_PROVIDER"));
            }
            case RESTRICTED -> {
                allowGeneralAreaSharing = true;
                allowPartialAddressSharing = false;
                authorizedRoles.addAll(List.of("CASE_MANAGER", "SUPERVISOR", "ADMIN"));
            }
            case CONFIDENTIAL -> {
                allowGeneralAreaSharing = false;
                allowPartialAddressSharing = false;
                authorizedRoles.addAll(List.of("SUPERVISOR", "ADMIN"));
            }
            case TOP_SECRET -> {
                allowNoAddressSharing = true;
                authorizedRoles.add("ADMIN");
                requiresCourtOrderToDisclose = true;
            }
        }
        
        // Set default emergency access
        emergencyAccessRoles.addAll(List.of("ADMIN", "EMERGENCY_COORDINATOR"));
    }
    
    public void addProtectedLocation(String address, LocationType locationType, String protectionReason) {
        ProtectedLocation location = new ProtectedLocation(address, locationType, protectionReason);
        confidentialLocations.add(location);
        updateModificationTime();
    }
    
    public void removeProtectedLocation(String address) {
        confidentialLocations.removeIf(loc -> loc.getAddress().equals(address));
        updateModificationTime();
    }
    
    public void addRestrictedArea(String zipCode, String reason) {
        String restrictedArea = zipCode + " [REASON: " + reason + "]";
        if (!restrictedZipCodes.contains(restrictedArea)) {
            restrictedZipCodes.add(restrictedArea);
        }
        updateModificationTime();
    }
    
    public void setGeographicRestriction(Double radiusMiles, String centerAddress, String reason) {
        this.restrictedRadius = radiusMiles;
        this.restrictedRadiusCenter = centerAddress + " [REASON: " + reason + "]";
        updateModificationTime();
    }
    
    public void authorizeUser(String userId, String role, LocationAccessPermission permission) {
        if (!authorizedUsers.contains(userId)) {
            authorizedUsers.add(userId);
        }
        rolePermissions.put(userId, permission);
        
        // Audit the authorization
        LocationAccessAudit audit = new LocationAccessAudit(
            userId, role, "USER_AUTHORIZED", permission.toString(), establishedBy
        );
        accessAuditTrail.add(audit);
        updateModificationTime();
    }
    
    public void revokeUserAccess(String userId, String reason) {
        authorizedUsers.remove(userId);
        rolePermissions.remove(userId);
        
        // Audit the revocation
        LocationAccessAudit audit = new LocationAccessAudit(
            userId, "UNKNOWN", "ACCESS_REVOKED", reason, establishedBy
        );
        accessAuditTrail.add(audit);
        updateModificationTime();
    }
    
    public boolean canUserAccessLocation(String userId, String userRole, String requestedAddress) {
        // Check if user is authorized
        if (!isUserAuthorized(userId, userRole)) {
            return false;
        }
        
        // Check if location is protected
        boolean isProtected = confidentialLocations.stream()
            .anyMatch(loc -> loc.getAddress().equals(requestedAddress));
        
        if (!isProtected) {
            return true; // Not a protected location
        }
        
        // Check permission level
        LocationAccessPermission userPermission = getUserPermission(userId, userRole);
        
        // Audit the access attempt
        LocationAccessAudit audit = new LocationAccessAudit(
            userId, userRole, "ACCESS_ATTEMPT", requestedAddress, userId
        );
        accessAuditTrail.add(audit);
        
        return userPermission != LocationAccessPermission.NO_ACCESS;
    }
    
    public LocationAccessLevel getLocationAccessLevel(String userId, String userRole) {
        if (!isUserAuthorized(userId, userRole)) {
            return LocationAccessLevel.NO_ACCESS;
        }
        
        LocationAccessPermission permission = getUserPermission(userId, userRole);
        
        return switch (permission) {
            case FULL_ACCESS -> LocationAccessLevel.FULL_ADDRESS;
            case PARTIAL_ACCESS -> allowPartialAddressSharing ? 
                LocationAccessLevel.PARTIAL_ADDRESS : LocationAccessLevel.GENERAL_AREA;
            case GENERAL_ACCESS -> allowGeneralAreaSharing ? 
                LocationAccessLevel.GENERAL_AREA : LocationAccessLevel.NO_ACCESS;
            case NO_ACCESS -> LocationAccessLevel.NO_ACCESS;
        };
    }
    
    public boolean requiresEmergencyOverride(String userId, String userRole, String requestedLocation) {
        if (!allowEmergencyAccess) {
            return false;
        }
        
        // Check if this is an emergency access role
        if (emergencyAccessRoles.contains(userRole)) {
            return true;
        }
        
        // Check if normal access would be denied
        return !canUserAccessLocation(userId, userRole, requestedLocation);
    }
    
    public String getLocationDisplayValue(String fullAddress, String userId, String userRole) {
        LocationAccessLevel accessLevel = getLocationAccessLevel(userId, userRole);
        
        return switch (accessLevel) {
            case FULL_ADDRESS -> fullAddress;
            case PARTIAL_ADDRESS -> maskAddressPartially(fullAddress);
            case GENERAL_AREA -> extractGeneralArea(fullAddress);
            case NO_ACCESS -> "[CONFIDENTIAL LOCATION]";
        };
    }
    
    private boolean isUserAuthorized(String userId, String userRole) {
        return authorizedUsers.contains(userId) || authorizedRoles.contains(userRole);
    }
    
    private LocationAccessPermission getUserPermission(String userId, String userRole) {
        // Check user-specific permissions first
        if (rolePermissions.containsKey(userId)) {
            return rolePermissions.get(userId);
        }
        
        // Fall back to role-based permissions
        return switch (userRole) {
            case "ADMIN" -> LocationAccessPermission.FULL_ACCESS;
            case "SUPERVISOR" -> LocationAccessPermission.PARTIAL_ACCESS;
            case "CASE_MANAGER" -> LocationAccessPermission.GENERAL_ACCESS;
            default -> LocationAccessPermission.NO_ACCESS;
        };
    }
    
    private String maskAddressPartially(String address) {
        // Simple implementation - could be enhanced
        String[] parts = address.split(" ");
        if (parts.length > 1) {
            return "*** " + String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        }
        return "[ADDRESS MASKED]";
    }
    
    private String extractGeneralArea(String address) {
        // Extract city/zip code - simplified implementation
        String[] parts = address.split(",");
        if (parts.length >= 2) {
            return parts[parts.length - 1].trim(); // Last part usually contains city/zip
        }
        return "[GENERAL AREA]";
    }
    
    private void updateModificationTime() {
        this.lastModified = Instant.now();
        this.versionNumber++;
    }
    
    public enum LocationConfidentialityLevel {
        PUBLIC("Standard location sharing allowed"),
        RESTRICTED("Limited location sharing with authorized staff"),
        CONFIDENTIAL("No location sharing except with supervisory approval"),
        TOP_SECRET("Absolute location confidentiality - court order required");
        
        private final String description;
        
        LocationConfidentialityLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public enum LocationType {
        HOME_ADDRESS("Primary residence"),
        WORK_ADDRESS("Workplace location"),
        SHELTER_ADDRESS("Emergency shelter location"),
        FAMILY_ADDRESS("Family member residence"),
        SAFE_HOUSE("Designated safe house"),
        SERVICE_LOCATION("Service provider location"),
        OTHER("Other confidential location");
        
        private final String description;
        
        LocationType(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public enum LocationAccessPermission {
        FULL_ACCESS("Can view complete address information"),
        PARTIAL_ACCESS("Can view street name but not house number"),
        GENERAL_ACCESS("Can view neighborhood/general area only"),
        NO_ACCESS("Cannot view any location information");
        
        private final String description;
        
        LocationAccessPermission(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public enum LocationAccessLevel {
        FULL_ADDRESS,
        PARTIAL_ADDRESS,
        GENERAL_AREA,
        NO_ACCESS
    }
    
    public static class ProtectedLocation {
        private String address;
        private LocationType locationType;
        private String protectionReason;
        private Instant protectionStartDate;
        private LocalDate protectionExpiryDate;
        private boolean isActive;
        
        public ProtectedLocation(String address, LocationType locationType, String protectionReason) {
            this.address = address;
            this.locationType = locationType;
            this.protectionReason = protectionReason;
            this.protectionStartDate = Instant.now();
            this.isActive = true;
        }
        
        // Getters and setters
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public LocationType getLocationType() { return locationType; }
        public void setLocationType(LocationType locationType) { this.locationType = locationType; }
        public String getProtectionReason() { return protectionReason; }
        public void setProtectionReason(String protectionReason) { this.protectionReason = protectionReason; }
        public Instant getProtectionStartDate() { return protectionStartDate; }
        public LocalDate getProtectionExpiryDate() { return protectionExpiryDate; }
        public void setProtectionExpiryDate(LocalDate protectionExpiryDate) { this.protectionExpiryDate = protectionExpiryDate; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
    }
    
    public static class LocationAccessAudit {
        private UUID auditId;
        private String userId;
        private String userRole;
        private String accessAction; // ACCESS_ATTEMPT, USER_AUTHORIZED, ACCESS_REVOKED
        private String locationOrReason;
        private String performedBy;
        private Instant timestamp;
        
        public LocationAccessAudit(String userId, String userRole, String accessAction, String locationOrReason, String performedBy) {
            this.auditId = UUID.randomUUID();
            this.userId = userId;
            this.userRole = userRole;
            this.accessAction = accessAction;
            this.locationOrReason = locationOrReason;
            this.performedBy = performedBy;
            this.timestamp = Instant.now();
        }
        
        // Getters
        public UUID getAuditId() { return auditId; }
        public String getUserId() { return userId; }
        public String getUserRole() { return userRole; }
        public String getAccessAction() { return accessAction; }
        public String getLocationOrReason() { return locationOrReason; }
        public String getPerformedBy() { return performedBy; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    // Getters
    public UUID getRulesId() { return rulesId; }
    public UUID getClientId() { return clientId; }
    public LocationConfidentialityLevel getConfidentialityLevel() { return confidentialityLevel; }
    public List<ProtectedLocation> getConfidentialLocations() { return List.copyOf(confidentialLocations); }
    public List<String> getSafeLocationCategories() { return List.copyOf(safeLocationCategories); }
    public List<String> getAuthorizedRoles() { return List.copyOf(authorizedRoles); }
    public List<String> getAuthorizedUsers() { return List.copyOf(authorizedUsers); }
    public Map<String, LocationAccessPermission> getRolePermissions() { return new HashMap<>(rolePermissions); }
    public boolean isAllowEmergencyAccess() { return allowEmergencyAccess; }
    public List<String> getEmergencyAccessRoles() { return List.copyOf(emergencyAccessRoles); }
    public String getEmergencyAccessProtocol() { return emergencyAccessProtocol; }
    public List<String> getRestrictedZipCodes() { return List.copyOf(restrictedZipCodes); }
    public List<String> getRestrictedNeighborhoods() { return List.copyOf(restrictedNeighborhoods); }
    public Double getRestrictedRadius() { return restrictedRadius; }
    public String getRestrictedRadiusCenter() { return restrictedRadiusCenter; }
    public boolean isAllowPartialAddressSharing() { return allowPartialAddressSharing; }
    public boolean isAllowGeneralAreaSharing() { return allowGeneralAreaSharing; }
    public boolean isAllowNoAddressSharing() { return allowNoAddressSharing; }
    public List<String> getApprovedLocationSharingContexts() { return List.copyOf(approvedLocationSharingContexts); }
    public List<LocationAccessAudit> getAccessAuditTrail() { return List.copyOf(accessAuditTrail); }
    public String getLegalBasisForConfidentiality() { return legalBasisForConfidentiality; }
    public LocalDate getConfidentialityExpirationDate() { return confidentialityExpirationDate; }
    public boolean isRequiresCourtOrderToDisclose() { return requiresCourtOrderToDisclose; }
    public String getEstablishedBy() { return establishedBy; }
    public String getEstablishedByRole() { return establishedByRole; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    public Integer getVersionNumber() { return versionNumber; }
    
    // Setters with modification tracking
    public void setEmergencyAccessProtocol(String emergencyAccessProtocol) { 
        this.emergencyAccessProtocol = emergencyAccessProtocol; 
        updateModificationTime(); 
    }
    public void setLegalBasisForConfidentiality(String legalBasisForConfidentiality) { 
        this.legalBasisForConfidentiality = legalBasisForConfidentiality; 
        updateModificationTime(); 
    }
    public void setConfidentialityExpirationDate(LocalDate confidentialityExpirationDate) { 
        this.confidentialityExpirationDate = confidentialityExpirationDate; 
        updateModificationTime(); 
    }
    public void setRequiresCourtOrderToDisclose(boolean requiresCourtOrderToDisclose) { 
        this.requiresCourtOrderToDisclose = requiresCourtOrderToDisclose; 
        updateModificationTime(); 
    }
}