package org.haven.shared.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for synchronizing Keycloak realm roles with application RBAC tables.
 *
 * Performs bidirectional sync:
 * - Keycloak → Database: Update role metadata, detect drift
 * - Database → Keycloak: Create missing roles (admin-initiated)
 *
 * Logs all sync operations to rbac_sync_log for audit trail.
 */
@Service
public class KeycloakRoleSyncService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakRoleSyncService.class);

    private final RbacRoleRepository rbacRoleRepository;
    private final RbacSyncLogRepository syncLogRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:haven}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    public KeycloakRoleSyncService(
            RbacRoleRepository rbacRoleRepository,
            RbacSyncLogRepository syncLogRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.rbacRoleRepository = rbacRoleRepository;
        this.syncLogRepository = syncLogRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Perform full synchronization from Keycloak to database.
     *
     * @param performedBy UUID of user initiating sync (null for scheduled jobs)
     * @return SyncResult with statistics
     */
    @Transactional
    public SyncResult performFullSync(UUID performedBy) {
        logger.info("Starting full Keycloak role sync for realm: {}", realm);

        SyncResult result = new SyncResult();
        result.syncType = SyncType.FULL;
        result.startTime = Instant.now();

        try {
            // Get admin token
            String adminToken = getAdminToken();

            // Fetch all roles from Keycloak
            List<KeycloakRoleDto> keycloakRoles = fetchKeycloakRoles(adminToken);
            logger.info("Fetched {} roles from Keycloak", keycloakRoles.size());

            // Fetch all roles from database
            Map<String, RbacRole> dbRoles = rbacRoleRepository.findAll().stream()
                    .collect(Collectors.toMap(RbacRole::getRoleName, r -> r));

            // Process each Keycloak role
            for (KeycloakRoleDto kcRole : keycloakRoles) {
                // Only sync Haven-specific roles (skip default Keycloak roles)
                if (!isHavenRole(kcRole.getName())) {
                    continue;
                }

                RbacRole dbRole = dbRoles.get(kcRole.getName());

                if (dbRole == null) {
                    // Role exists in Keycloak but not in DB - potential drift
                    logger.warn("Drift detected: Role '{}' exists in Keycloak but not in database", kcRole.getName());
                    result.driftDetected = true;
                    result.driftDetails.add(String.format("Missing DB role: %s", kcRole.getName()));
                } else {
                    // Update Keycloak role ID if changed
                    if (!kcRole.getId().equals(dbRole.getKeycloakRoleId())) {
                        logger.info("Updating Keycloak role ID for '{}': {} -> {}",
                                kcRole.getName(), dbRole.getKeycloakRoleId(), kcRole.getId());
                        dbRole.setKeycloakRoleId(kcRole.getId());
                        dbRole.setUpdatedAt(Instant.now());
                        rbacRoleRepository.save(dbRole);
                        result.rolesUpdated++;
                    }

                    // Update description if changed
                    if (kcRole.getDescription() != null &&
                            !kcRole.getDescription().equals(dbRole.getDescription())) {
                        logger.debug("Updating description for role '{}'", kcRole.getName());
                        dbRole.setDescription(kcRole.getDescription());
                        dbRole.setUpdatedAt(Instant.now());
                        rbacRoleRepository.save(dbRole);
                        result.rolesUpdated++;
                    }

                    dbRoles.remove(kcRole.getName());
                }
            }

            // Check for roles in DB but not in Keycloak
            for (RbacRole orphanRole : dbRoles.values()) {
                logger.warn("Drift detected: Role '{}' exists in database but not in Keycloak", orphanRole.getRoleName());
                result.driftDetected = true;
                result.driftDetails.add(String.format("Missing Keycloak role: %s", orphanRole.getRoleName()));
            }

            result.status = SyncStatus.SUCCESS;
            logger.info("Full sync completed successfully. Updated: {}, Drift: {}",
                    result.rolesUpdated, result.driftDetected);

        } catch (Exception e) {
            result.status = SyncStatus.FAILED;
            result.errorMessage = e.getMessage();
            logger.error("Full sync failed", e);
        } finally {
            result.endTime = Instant.now();
            saveSyncLog(result, performedBy);
        }

        return result;
    }

    /**
     * Perform incremental sync - only update roles that have changed.
     */
    @Transactional
    public SyncResult performIncrementalSync(UUID performedBy) {
        logger.info("Starting incremental Keycloak role sync");

        SyncResult result = new SyncResult();
        result.syncType = SyncType.INCREMENTAL;
        result.startTime = Instant.now();

        try {
            String adminToken = getAdminToken();
            List<KeycloakRoleDto> keycloakRoles = fetchKeycloakRoles(adminToken);

            // Only check roles that have Keycloak IDs already
            List<RbacRole> trackedRoles = rbacRoleRepository.findByKeycloakRoleIdNotNull();

            for (RbacRole dbRole : trackedRoles) {
                Optional<KeycloakRoleDto> kcRoleOpt = keycloakRoles.stream()
                        .filter(kr -> kr.getId().equals(dbRole.getKeycloakRoleId()))
                        .findFirst();

                if (kcRoleOpt.isEmpty()) {
                    logger.warn("Role '{}' (ID: {}) not found in Keycloak",
                            dbRole.getRoleName(), dbRole.getKeycloakRoleId());
                    result.driftDetected = true;
                    result.driftDetails.add(String.format("Missing: %s", dbRole.getRoleName()));
                } else {
                    KeycloakRoleDto kcRole = kcRoleOpt.get();
                    boolean updated = false;

                    if (!kcRole.getName().equals(dbRole.getRoleName())) {
                        logger.info("Role name changed in Keycloak: {} -> {}", dbRole.getRoleName(), kcRole.getName());
                        // Don't auto-update role name - log as drift
                        result.driftDetected = true;
                        result.driftDetails.add(String.format("Name mismatch: %s vs %s",
                                dbRole.getRoleName(), kcRole.getName()));
                    }

                    if (kcRole.getDescription() != null &&
                            !kcRole.getDescription().equals(dbRole.getDescription())) {
                        dbRole.setDescription(kcRole.getDescription());
                        updated = true;
                    }

                    if (updated) {
                        dbRole.setUpdatedAt(Instant.now());
                        rbacRoleRepository.save(dbRole);
                        result.rolesUpdated++;
                    }
                }
            }

            result.status = SyncStatus.SUCCESS;
            logger.info("Incremental sync completed. Updated: {}, Drift: {}",
                    result.rolesUpdated, result.driftDetected);

        } catch (Exception e) {
            result.status = SyncStatus.FAILED;
            result.errorMessage = e.getMessage();
            logger.error("Incremental sync failed", e);
        } finally {
            result.endTime = Instant.now();
            saveSyncLog(result, performedBy);
        }

        return result;
    }

    /**
     * Get admin access token from Keycloak.
     */
    private String getAdminToken() throws Exception {
        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        String body = String.format("grant_type=password&client_id=%s&username=%s&password=%s",
                adminClientId, adminUsername, adminPassword);

        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(body, headers), String.class);

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        return jsonNode.get("access_token").asText();
    }

    /**
     * Fetch all realm roles from Keycloak.
     */
    private List<KeycloakRoleDto> fetchKeycloakRoles(String adminToken) throws Exception {
        String rolesUrl = String.format("%s/admin/realms/%s/roles", keycloakUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);

        ResponseEntity<String> response = restTemplate.exchange(rolesUrl, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        List<KeycloakRoleDto> roles = new ArrayList<>();

        for (JsonNode roleNode : jsonNode) {
            KeycloakRoleDto role = new KeycloakRoleDto();
            role.setId(roleNode.get("id").asText());
            role.setName(roleNode.get("name").asText());
            role.setDescription(roleNode.has("description") ? roleNode.get("description").asText() : null);
            role.setComposite(roleNode.has("composite") && roleNode.get("composite").asBoolean());
            roles.add(role);
        }

        return roles;
    }

    /**
     * Check if role is a Haven application role (vs default Keycloak role).
     */
    private boolean isHavenRole(String roleName) {
        Set<String> havenRoles = Set.of(
                "admin", "supervisor", "case-manager", "intake-specialist",
                "ce-intake", "dv-advocate", "compliance-auditor", "exec",
                "report-viewer", "external-partner"
        );
        return havenRoles.contains(roleName);
    }

    /**
     * Save sync result to audit log.
     */
    private void saveSyncLog(SyncResult result, UUID performedBy) {
        RbacSyncLog log = new RbacSyncLog();
        log.setId(UUID.randomUUID());
        log.setSyncTimestamp(result.startTime);
        log.setSyncType(result.syncType.name());
        log.setRolesAdded(result.rolesAdded);
        log.setRolesUpdated(result.rolesUpdated);
        log.setRolesRemoved(result.rolesRemoved);
        log.setDriftDetected(result.driftDetected);
        log.setDriftDetails(result.driftDetails.isEmpty() ? null :
                objectMapper.valueToTree(result.driftDetails));
        log.setSyncStatus(result.status.name());
        log.setErrorMessage(result.errorMessage);
        log.setPerformedBy(performedBy);

        syncLogRepository.save(log);
    }

    // DTOs and Enums

    public static class KeycloakRoleDto {
        private String id;
        private String name;
        private String description;
        private boolean composite;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isComposite() { return composite; }
        public void setComposite(boolean composite) { this.composite = composite; }
    }

    public static class SyncResult {
        public SyncType syncType;
        public Instant startTime;
        public Instant endTime;
        public int rolesAdded = 0;
        public int rolesUpdated = 0;
        public int rolesRemoved = 0;
        public boolean driftDetected = false;
        public List<String> driftDetails = new ArrayList<>();
        public SyncStatus status;
        public String errorMessage;
    }

    public enum SyncType {
        FULL, INCREMENTAL, MANUAL
    }

    public enum SyncStatus {
        SUCCESS, FAILED, PARTIAL
    }
}
