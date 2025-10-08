package org.haven.api.admin;

import org.haven.shared.rbac.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * REST API for Keycloak role synchronization management.
 *
 * Requires ADMIN role for all operations.
 */
@RestController
@RequestMapping("/api/admin/rbac-sync")
@PreAuthorize("hasRole('ADMIN')")
public class RbacSyncController {

    private static final Logger logger = LoggerFactory.getLogger(RbacSyncController.class);

    private final KeycloakRoleSyncService syncService;
    private final RbacRoleRepository roleRepository;
    private final RbacSyncLogRepository syncLogRepository;

    public RbacSyncController(
            KeycloakRoleSyncService syncService,
            RbacRoleRepository roleRepository,
            RbacSyncLogRepository syncLogRepository) {
        this.syncService = syncService;
        this.roleRepository = roleRepository;
        this.syncLogRepository = syncLogRepository;
    }

    /**
     * Trigger full synchronization.
     */
    @PostMapping("/sync/full")
    public ResponseEntity<SyncResultDto> performFullSync(Authentication authentication) {
        logger.info("Full sync requested by: {}", authentication.getName());

        UUID userId = extractUserId(authentication);
        KeycloakRoleSyncService.SyncResult result = syncService.performFullSync(userId);

        return ResponseEntity.ok(toDto(result));
    }

    /**
     * Trigger incremental synchronization.
     */
    @PostMapping("/sync/incremental")
    public ResponseEntity<SyncResultDto> performIncrementalSync(Authentication authentication) {
        logger.info("Incremental sync requested by: {}", authentication.getName());

        UUID userId = extractUserId(authentication);
        KeycloakRoleSyncService.SyncResult result = syncService.performIncrementalSync(userId);

        return ResponseEntity.ok(toDto(result));
    }

    /**
     * Get all roles with sync status.
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RbacRoleDto>> getAllRoles() {
        List<RbacRole> roles = roleRepository.findAll();
        List<RbacRoleDto> dtos = roles.stream()
                .map(this::toRoleDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get sync history (last 30 days).
     */
    @GetMapping("/sync-history")
    public ResponseEntity<List<RbacSyncLogDto>> getSyncHistory() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<RbacSyncLog> logs = syncLogRepository.findBySyncTimestampAfterOrderBySyncTimestampDesc(thirtyDaysAgo);

        List<RbacSyncLogDto> dtos = logs.stream()
                .map(this::toSyncLogDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get recent drift detections.
     */
    @GetMapping("/drift-detections")
    public ResponseEntity<List<RbacSyncLogDto>> getDriftDetections() {
        List<RbacSyncLog> logs = syncLogRepository.findByDriftDetectedTrueOrderBySyncTimestampDesc();

        List<RbacSyncLogDto> dtos = logs.stream()
                .limit(50)
                .map(this::toSyncLogDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get latest sync status.
     */
    @GetMapping("/sync-status")
    public ResponseEntity<RbacSyncLogDto> getLatestSyncStatus() {
        List<RbacSyncLog> logs = syncLogRepository.findTop10ByOrderBySyncTimestampDesc();

        if (logs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(toSyncLogDto(logs.get(0)));
    }

    // Helper methods

    private UUID extractUserId(Authentication authentication) {
        // TODO: Extract user ID from authentication context
        // For now, return null (will be logged as system sync)
        return null;
    }

    private SyncResultDto toDto(KeycloakRoleSyncService.SyncResult result) {
        SyncResultDto dto = new SyncResultDto();
        dto.syncType = result.syncType.name();
        dto.startTime = result.startTime;
        dto.endTime = result.endTime;
        dto.rolesAdded = result.rolesAdded;
        dto.rolesUpdated = result.rolesUpdated;
        dto.rolesRemoved = result.rolesRemoved;
        dto.driftDetected = result.driftDetected;
        dto.driftDetails = result.driftDetails;
        dto.status = result.status.name();
        dto.errorMessage = result.errorMessage;
        return dto;
    }

    private RbacRoleDto toRoleDto(RbacRole role) {
        RbacRoleDto dto = new RbacRoleDto();
        dto.id = role.getId();
        dto.keycloakRoleId = role.getKeycloakRoleId();
        dto.roleName = role.getRoleName();
        dto.roleEnum = role.getRoleEnum().name();
        dto.displayName = role.getDisplayName();
        dto.description = role.getDescription();
        dto.isComposite = role.getIsComposite();
        dto.isActive = role.getIsActive();
        dto.requiresMfa = role.getRequiresMfa();
        dto.sessionTimeoutMinutes = role.getSessionTimeoutMinutes();
        dto.syncStatus = role.getKeycloakRoleId() != null ? "SYNCED" : "NOT_SYNCED";
        return dto;
    }

    private RbacSyncLogDto toSyncLogDto(RbacSyncLog log) {
        RbacSyncLogDto dto = new RbacSyncLogDto();
        dto.id = log.getId();
        dto.syncTimestamp = log.getSyncTimestamp();
        dto.syncType = log.getSyncType();
        dto.rolesAdded = log.getRolesAdded();
        dto.rolesUpdated = log.getRolesUpdated();
        dto.rolesRemoved = log.getRolesRemoved();
        dto.driftDetected = log.getDriftDetected();
        dto.driftDetails = log.getDriftDetails();
        dto.syncStatus = log.getSyncStatus();
        dto.errorMessage = log.getErrorMessage();
        dto.performedBy = log.getPerformedBy();
        return dto;
    }

    // DTOs

    public static class SyncResultDto {
        public String syncType;
        public Instant startTime;
        public Instant endTime;
        public int rolesAdded;
        public int rolesUpdated;
        public int rolesRemoved;
        public boolean driftDetected;
        public List<String> driftDetails;
        public String status;
        public String errorMessage;
    }

    public static class RbacRoleDto {
        public UUID id;
        public String keycloakRoleId;
        public String roleName;
        public String roleEnum;
        public String displayName;
        public String description;
        public Boolean isComposite;
        public Boolean isActive;
        public Boolean requiresMfa;
        public Integer sessionTimeoutMinutes;
        public String syncStatus;
    }

    public static class RbacSyncLogDto {
        public UUID id;
        public Instant syncTimestamp;
        public String syncType;
        public Integer rolesAdded;
        public Integer rolesUpdated;
        public Integer rolesRemoved;
        public Boolean driftDetected;
        public Object driftDetails;
        public String syncStatus;
        public String errorMessage;
        public UUID performedBy;
    }
}
