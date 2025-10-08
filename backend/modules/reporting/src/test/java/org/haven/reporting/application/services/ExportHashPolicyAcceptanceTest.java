package org.haven.reporting.application.services;

import org.haven.reporting.domain.*;
import org.haven.shared.audit.AuditService;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.PolicyDecision;
import org.haven.shared.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Acceptance tests for export hash policy enforcement
 *
 * Validates:
 * - Hashed export flows (always permitted)
 * - Unhashed export flows with valid authorization
 * - Unhashed export refusal paths
 * - Policy escalations and alerts
 * - Audit logging completeness
 */
@DisplayName("Export Hash Policy - Acceptance Tests")
class ExportHashPolicyAcceptanceTest {

    private ExportSecurityPolicyService policyService;
    private TenantExportConfigurationRepository configRepository;
    private AuditService auditService;
    private ExportSecurityMonitoringService monitoringService;

    private UUID testTenantId;
    private AccessContext adminContext;
    private AccessContext dataAnalystContext;

    @BeforeEach
    void setUp() {
        configRepository = mock(TenantExportConfigurationRepository.class);
        auditService = mock(AuditService.class);
        monitoringService = mock(ExportSecurityMonitoringService.class);
        policyService = new ExportSecurityPolicyService(
                auditService, configRepository, monitoringService);

        testTenantId = UUID.randomUUID();

        adminContext = AccessContext.fromRoles(
                UUID.randomUUID(),
                "admin@example.org",
                List.of(UserRole.ADMINISTRATOR),
                "Export for HUD compliance",
                "192.168.1.100",
                "session-123",
                "Mozilla/5.0"
        );

        dataAnalystContext = AccessContext.fromRoles(
                UUID.randomUUID(),
                "analyst@example.org",
                List.of(UserRole.DATA_ANALYST),
                "Annual reporting",
                "192.168.1.101",
                "session-456",
                "Mozilla/5.0"
        );
    }

    @Nested
    @DisplayName("Hashed Export Flows")
    class HashedExportFlows {

        @Test
        @DisplayName("Should always permit hashed exports with ALWAYS_HASH policy")
        void testHashedExport_AlwaysHashPolicy_Permitted() {
            // Arrange
            TenantExportConfiguration config = TenantExportConfiguration.defaultConfiguration(
                    testTenantId, "Test Organization");
            config.setHashBehavior(ExportHashBehavior.ALWAYS_HASH);
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    true, // requesting hashed export
                    null, // no consent scopes needed
                    null, // no clearance needed
                    adminContext
            );

            // Assert
            assertTrue(decision.isPermitted(), "Hashed export should be permitted");
            assertEquals("Hashed export requested - complies with all security policies",
                    decision.getReason());
            verify(auditService).logAction(eq("EXPORT_UNHASHED_ATTEMPT"), any());
        }

        @Test
        @DisplayName("Should permit hashed exports with CONSENT_BASED policy")
        void testHashedExport_ConsentBasedPolicy_Permitted() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Consent Organization",
                    ExportHashBehavior.CONSENT_BASED,
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    false,
                    false,
                    24,
                    true,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    true, // requesting hashed export
                    null,
                    null,
                    dataAnalystContext
            );

            // Assert
            assertTrue(decision.isPermitted());
            verify(auditService).logAction(eq("EXPORT_UNHASHED_ATTEMPT"), any());
        }

        @Test
        @DisplayName("Should permit hashed exports even with NEVER_HASH policy")
        void testHashedExport_NeverHashPolicy_Permitted() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Open Organization",
                    ExportHashBehavior.NEVER_HASH,
                    Set.of(),
                    false,
                    false,
                    24,
                    false,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    true,
                    null,
                    null,
                    adminContext
            );

            // Assert
            assertTrue(decision.isPermitted());
        }
    }

    @Nested
    @DisplayName("Unhashed Export - Permitted Flows")
    class UnhashedExportPermittedFlows {

        @Test
        @DisplayName("Should permit unhashed export with valid consent and clearance")
        void testUnhashedExport_ValidConsentAndClearance_Permitted() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Consent Organization",
                    ExportHashBehavior.CONSENT_BASED,
                    Set.of(ExportConsentScope.PII_DISCLOSURE, ExportConsentScope.HUD_REPORTING),
                    false,
                    false,
                    24,
                    true,
                    Set.of("security@example.org")
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            Set<ExportConsentScope> providedScopes = Set.of(
                    ExportConsentScope.PII_DISCLOSURE,
                    ExportConsentScope.HUD_REPORTING
            );

            ExportSecurityClearance clearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    "supervisor@example.org",
                    "HUD annual reporting requirement",
                    24
            );

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false, // requesting unhashed export
                    providedScopes,
                    clearance,
                    adminContext
            );

            // Assert
            assertTrue(decision.isPermitted(),
                    "Unhashed export should be permitted with valid consent and clearance");
            assertEquals("Unhashed export authorized with valid consent and clearance",
                    decision.getReason());
            verify(monitoringService).logUnhashedExportAttempt(
                    eq(testTenantId), any(), eq(providedScopes), eq(clearance), eq(adminContext));
        }

        @Test
        @DisplayName("Should permit unhashed export with NEVER_HASH policy")
        void testUnhashedExport_NeverHashPolicy_Permitted() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Test Organization",
                    ExportHashBehavior.NEVER_HASH,
                    Set.of(),
                    false,
                    false,
                    24,
                    false,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false, // requesting unhashed
                    null,
                    null,
                    dataAnalystContext
            );

            // Assert
            assertTrue(decision.isPermitted());
            verify(monitoringService).logUnhashedExportAttempt(
                    eq(testTenantId), any(), any(), any(), eq(dataAnalystContext));
        }
    }

    @Nested
    @DisplayName("Unhashed Export - Refusal Paths")
    class UnhashedExportRefusalPaths {

        @Test
        @DisplayName("Should deny unhashed export with ALWAYS_HASH policy")
        void testUnhashedExport_AlwaysHashPolicy_Denied() {
            // Arrange
            TenantExportConfiguration config = TenantExportConfiguration.defaultConfiguration(
                    testTenantId, "Secure Organization");
            config.setHashBehavior(ExportHashBehavior.ALWAYS_HASH);
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            Set<ExportConsentScope> scopes = Set.of(ExportConsentScope.PII_DISCLOSURE);
            ExportSecurityClearance clearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    "supervisor@example.org",
                    "Attempting unhashed export",
                    24
            );

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false, // requesting unhashed
                    scopes,
                    clearance,
                    adminContext
            );

            // Assert
            assertFalse(decision.isPermitted(), "Unhashed export should be denied");
            assertEquals("Organization policy prohibits unhashed exports (ALWAYS_HASH mode)",
                    decision.getReason());
            assertTrue(decision.getMetadata().containsKey("error_code"));
            assertEquals("POLICY_PROHIBITS_UNHASHED", decision.getMetadata().get("error_code"));
            verify(monitoringService).logUnhashedExportAttempt(
                    eq(testTenantId), any(), eq(scopes), eq(clearance), eq(adminContext));
        }

        @Test
        @DisplayName("Should deny unhashed export with missing consent scopes")
        void testUnhashedExport_MissingConsentScopes_Denied() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Consent Organization",
                    ExportHashBehavior.CONSENT_BASED,
                    Set.of(ExportConsentScope.PII_DISCLOSURE, ExportConsentScope.HUD_REPORTING),
                    false,
                    false,
                    24,
                    true,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            // Only provide one of two required scopes
            Set<ExportConsentScope> providedScopes = Set.of(ExportConsentScope.PII_DISCLOSURE);

            ExportSecurityClearance clearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    "supervisor@example.org",
                    "Partial consent",
                    24
            );

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false,
                    providedScopes,
                    clearance,
                    adminContext
            );

            // Assert
            assertFalse(decision.isPermitted());
            assertTrue(decision.getReason().contains("Missing required consent scopes"));
            assertEquals("INSUFFICIENT_CONSENT_SCOPES", decision.getMetadata().get("error_code"));
        }

        @Test
        @DisplayName("Should deny unhashed export with no consent scopes")
        void testUnhashedExport_NoConsentScopes_Denied() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Consent Organization",
                    ExportHashBehavior.CONSENT_BASED,
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    false,
                    false,
                    24,
                    true,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false,
                    null, // no consent scopes
                    null,
                    dataAnalystContext
            );

            // Assert
            assertFalse(decision.isPermitted());
            assertEquals("No consent scopes provided for unhashed export request",
                    decision.getReason());
            assertEquals("MISSING_CONSENT_SCOPES", decision.getMetadata().get("error_code"));
        }

        @Test
        @DisplayName("Should deny unhashed export with missing clearance")
        void testUnhashedExport_MissingClearance_Denied() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Consent Organization",
                    ExportHashBehavior.CONSENT_BASED,
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    false,
                    false,
                    24,
                    true,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            Set<ExportConsentScope> providedScopes = Set.of(ExportConsentScope.PII_DISCLOSURE);

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false,
                    providedScopes,
                    null, // no clearance provided
                    adminContext
            );

            // Assert
            assertFalse(decision.isPermitted());
            assertEquals("No security clearance provided for unhashed export",
                    decision.getReason());
            assertEquals("MISSING_CLEARANCE", decision.getMetadata().get("error_code"));
        }

        @Test
        @DisplayName("Should deny unhashed export with expired clearance")
        void testUnhashedExport_ExpiredClearance_Denied() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Consent Organization",
                    ExportHashBehavior.CONSENT_BASED,
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    false,
                    false,
                    24,
                    true,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            Set<ExportConsentScope> providedScopes = Set.of(ExportConsentScope.PII_DISCLOSURE);

            // Create clearance with -1 hour validity (already expired)
            ExportSecurityClearance expiredClearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    "supervisor@example.org",
                    "Expired clearance test",
                    -1 // expired
            );

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false,
                    providedScopes,
                    expiredClearance,
                    adminContext
            );

            // Assert
            assertFalse(decision.isPermitted());
            assertTrue(decision.getReason().contains("Security clearance expired"));
            assertEquals("CLEARANCE_EXPIRED", decision.getMetadata().get("error_code"));
        }

        @Test
        @DisplayName("Should deny unhashed export with insufficient clearance scopes")
        void testUnhashedExport_InsufficientClearanceScopes_Denied() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Consent Organization",
                    ExportHashBehavior.CONSENT_BASED,
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    false,
                    false,
                    24,
                    true,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            Set<ExportConsentScope> providedScopes = Set.of(ExportConsentScope.PII_DISCLOSURE);

            // Clearance does NOT include PII_DISCLOSURE scope
            ExportSecurityClearance insufficientClearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.HUD_REPORTING), // different scope
                    "supervisor@example.org",
                    "Insufficient scope test",
                    24
            );

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false,
                    providedScopes,
                    insufficientClearance,
                    adminContext
            );

            // Assert
            assertFalse(decision.isPermitted());
            assertTrue(decision.getReason().contains("does not authorize unhashed exports"));
            assertEquals("CLEARANCE_INSUFFICIENT", decision.getMetadata().get("error_code"));
        }
    }

    @Nested
    @DisplayName("Audit and Monitoring")
    class AuditAndMonitoring {

        @Test
        @DisplayName("Should audit all decisions with full context")
        void testAuditLogging_AllDecisions_Logged() {
            // Arrange
            TenantExportConfiguration config = TenantExportConfiguration.defaultConfiguration(
                    testTenantId, "Test Organization");
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            // Act
            policyService.evaluateExportHashPolicy(
                    testTenantId,
                    true, // hashed export
                    null,
                    null,
                    adminContext
            );

            // Assert
            verify(auditService).logAction(eq("EXPORT_UNHASHED_ATTEMPT"), any());
        }

        @Test
        @DisplayName("Should send monitoring events for unhashed attempts")
        void testMonitoring_UnhashedAttempts_Logged() {
            // Arrange
            TenantExportConfiguration config = new TenantExportConfiguration(
                    testTenantId,
                    "Test Organization",
                    ExportHashBehavior.CONSENT_BASED,
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    false,
                    false,
                    24,
                    true,
                    Set.of()
            );
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            Set<ExportConsentScope> scopes = Set.of(ExportConsentScope.PII_DISCLOSURE);
            ExportSecurityClearance clearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    "supervisor@example.org",
                    "Test",
                    24
            );

            // Act
            policyService.evaluateExportHashPolicy(
                    testTenantId,
                    false, // unhashed
                    scopes,
                    clearance,
                    adminContext
            );

            // Assert
            verify(monitoringService).logUnhashedExportAttempt(
                    eq(testTenantId), any(), eq(scopes), eq(clearance), eq(adminContext));
        }

        @Test
        @DisplayName("Should include policy metadata in decisions")
        void testPolicyMetadata_IncludedInDecisions() {
            // Arrange
            TenantExportConfiguration config = TenantExportConfiguration.defaultConfiguration(
                    testTenantId, "Test Organization");
            when(configRepository.findByTenantId(testTenantId)).thenReturn(Optional.of(config));

            // Act
            PolicyDecision decision = policyService.evaluateExportHashPolicy(
                    testTenantId,
                    true,
                    null,
                    null,
                    adminContext
            );

            // Assert
            assertNotNull(decision.getMetadata());
            assertTrue(decision.getMetadata().containsKey("tenant_id"));
            assertTrue(decision.getMetadata().containsKey("user_id"));
            assertTrue(decision.getMetadata().containsKey("hash_behavior"));
            assertTrue(decision.getMetadata().containsKey("evaluation_time_ms"));
        }
    }
}
