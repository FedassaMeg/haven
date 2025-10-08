package org.haven.shared.security;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Context for access control decisions
 * Encapsulates user identity, roles, and request metadata
 */
public class AccessContext {

    private final UUID userId;
    private final String userName;
    private final List<UserRole> userRoles;
    private final String accessReason;
    private final String ipAddress;
    private final String sessionId;
    private final String userAgent;

    private AccessContext(UUID userId, String userName, List<UserRole> userRoles,
                         String accessReason, String ipAddress, String sessionId, String userAgent) {
        this.userId = userId;
        this.userName = userName;
        this.userRoles = userRoles;
        this.accessReason = accessReason;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
        this.userAgent = userAgent;
    }

    public static AccessContext fromRoleStrings(UUID userId, String userName, List<String> roleStrings,
                                               String accessReason, String ipAddress, String sessionId, String userAgent) {
        List<UserRole> roles = roleStrings.stream()
                .map(UserRole::fromString)
                .collect(Collectors.toList());
        return new AccessContext(userId, userName, roles, accessReason, ipAddress, sessionId, userAgent);
    }

    public static AccessContext fromRoles(UUID userId, String userName, List<UserRole> userRoles,
                                         String accessReason, String ipAddress, String sessionId, String userAgent) {
        return new AccessContext(userId, userName, userRoles, accessReason, ipAddress, sessionId, userAgent);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public boolean hasRole(UserRole role) {
        return userRoles.contains(role);
    }

    public boolean hasAnyRole(UserRole... roles) {
        for (UserRole role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasClinicalRole() {
        return userRoles.stream().anyMatch(UserRole::isClinical);
    }

    public boolean hasLegalRole() {
        return userRoles.stream().anyMatch(UserRole::isLegal);
    }

    public boolean hasMedicalRole() {
        return userRoles.stream().anyMatch(UserRole::isMedical);
    }

    public boolean hasAdministrativeRole() {
        return userRoles.stream().anyMatch(UserRole::isAdministrative);
    }

    public boolean isExternalPartner() {
        return userRoles.stream().anyMatch(UserRole::isExternalPartner);
    }

    public String getAccessReason() {
        return accessReason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public List<String> getRoleStrings() {
        return userRoles.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public List<String> getRoles() {
        return getRoleStrings();
    }

    @Override
    public String toString() {
        return String.format("AccessContext[user=%s, roles=%s, reason=%s]",
                userId, userRoles, accessReason);
    }
}
