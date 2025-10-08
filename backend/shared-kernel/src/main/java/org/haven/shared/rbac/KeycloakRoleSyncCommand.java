package org.haven.shared.rbac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CLI command to manually trigger Keycloak role synchronization.
 *
 * Usage:
 *   ./gradlew bootRun --args="--keycloak.sync.mode=full"
 *   ./gradlew bootRun --args="--keycloak.sync.mode=incremental"
 *
 * Or via Java:
 *   java -jar app.jar --keycloak.sync.mode=full
 */
@Component
@ConditionalOnProperty(name = "keycloak.sync.mode")
public class KeycloakRoleSyncCommand implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakRoleSyncCommand.class);

    private final KeycloakRoleSyncService syncService;
    private final String syncMode;

    public KeycloakRoleSyncCommand(
            KeycloakRoleSyncService syncService,
            @org.springframework.beans.factory.annotation.Value("${keycloak.sync.mode:}")
            String syncMode) {
        this.syncService = syncService;
        this.syncMode = syncMode;
    }

    @Override
    public void run(String... args) {
        logger.info("=".repeat(70));
        logger.info("Keycloak Role Synchronization CLI");
        logger.info("=".repeat(70));

        try {
            KeycloakRoleSyncService.SyncResult result;

            switch (syncMode.toLowerCase()) {
                case "full":
                    logger.info("Performing FULL synchronization...");
                    result = syncService.performFullSync(null);
                    break;

                case "incremental":
                    logger.info("Performing INCREMENTAL synchronization...");
                    result = syncService.performIncrementalSync(null);
                    break;

                default:
                    logger.error("Invalid sync mode: {}. Use 'full' or 'incremental'", syncMode);
                    System.exit(1);
                    return;
            }

            // Print results
            logger.info("");
            logger.info("Sync Results:");
            logger.info("  Type: {}", result.syncType);
            logger.info("  Status: {}", result.status);
            logger.info("  Duration: {} ms", result.endTime.toEpochMilli() - result.startTime.toEpochMilli());
            logger.info("  Roles Added: {}", result.rolesAdded);
            logger.info("  Roles Updated: {}", result.rolesUpdated);
            logger.info("  Roles Removed: {}", result.rolesRemoved);
            logger.info("  Drift Detected: {}", result.driftDetected);

            if (result.driftDetected && !result.driftDetails.isEmpty()) {
                logger.warn("");
                logger.warn("Drift Details:");
                result.driftDetails.forEach(detail -> logger.warn("  - {}", detail));
            }

            if (result.status == KeycloakRoleSyncService.SyncStatus.FAILED) {
                logger.error("");
                logger.error("Sync failed: {}", result.errorMessage);
                System.exit(1);
            } else if (result.driftDetected) {
                logger.warn("");
                logger.warn("Sync completed with drift detected. Manual review recommended.");
                System.exit(2);
            } else {
                logger.info("");
                logger.info("Sync completed successfully!");
                System.exit(0);
            }

        } catch (Exception e) {
            logger.error("Sync command failed", e);
            System.exit(1);
        }
    }
}
