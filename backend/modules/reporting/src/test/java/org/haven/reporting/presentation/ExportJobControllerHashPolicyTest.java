package org.haven.reporting.presentation;

import org.haven.reporting.application.services.ExportJobApplicationService;
import org.haven.reporting.application.services.ExportSecurityPolicyService;
import org.haven.reporting.domain.*;
import org.haven.reporting.infrastructure.persistence.ExportAuditMetadataRepository;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.PolicyDecision;
import org.haven.shared.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for export controller hash policy enforcement
 *
 * Validates:
 * - Fail-fast behavior on policy violations
 * - HTTP status codes for different refusal scenarios
 * - Error response structure
 * - End-to-end authorization flow
 */
@DisplayName("Export Controller - Hash Policy Integration Tests")
class ExportJobControllerHashPolicyTest {

    private ExportJobController controller;
    private ExportJobApplicationService exportService;
    private ExportJobRepository exportJobRepository;
    private ExportAuditMetadataRepository auditMetadataRepository;
    private ExportSecurityPolicyService securityPolicyService;

    private AccessContext adminContext;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        exportService = mock(ExportJobApplicationService.class);
        exportJobRepository = mock(ExportJobRepository.class);
        auditMetadataRepository = mock(ExportAuditMetadataRepository.class);
        securityPolicyService = mock(ExportSecurityPolicyService.class);

        controller = new ExportJobController(
                exportService,
                exportJobRepository,
                auditMetadataRepository,
                securityPolicyService
        );

        testTenantId = UUID.randomUUID();
        adminContext = AccessContext.fromRoles(
                testTenantId, // Using as tenant ID placeholder
                "admin@example.org",
                List.of(UserRole.ADMINISTRATOR),
                "HUD export request",
                "192.168.1.100",
                "session-123",
                "Mozilla/5.0"
        );
    }

    @Nested
    @DisplayName("Successful Export Flows")
    class SuccessfulExportFlows {

        @Test
        @DisplayName("Should accept hashed export request")
        void testHashedExportRequest_Accepted() {
            // Arrange
            ExportJobController.ExportJobRequest request = new ExportJobController.ExportJobRequest();
            request.setExportType("HMIS_CSV");
            request.setReportingPeriodStart(LocalDate.of(2024, 1, 1));
            request.setReportingPeriodEnd(LocalDate.of(2024, 12, 31));
            request.setProjectIds(List.of(UUID.randomUUID()));
            request.setCocCode("CA-600");
            request.setExportReason("Annual HUD reporting");
            request.setHashedExport(true); // hashed

            PolicyDecision permitDecision = PolicyDecision.permit(
                    "EXPORT_HASH_POLICY",
                    "v1.0",
                    "Hashed export requested - complies with all security policies",
                    Map.of(),
                    adminContext
            );

            when(securityPolicyService.evaluateExportHashPolicy(
                    any(), eq(true), any(), any(), eq(adminContext)))
                    .thenReturn(permitDecision);

            UUID exportJobId = UUID.randomUUID();
            when(exportService.requestExport(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(exportJobId);

            // Act
            ResponseEntity<?> response = controller.requestExport(request, adminContext);

            // Assert
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertTrue(response.getBody() instanceof ExportJobController.ExportJobResponse);
            ExportJobController.ExportJobResponse jobResponse =
                    (ExportJobController.ExportJobResponse) response.getBody();
            assertEquals(exportJobId, jobResponse.getExportJobId());
            assertEquals(ExportJobState.QUEUED, jobResponse.getState());

            verify(securityPolicyService).evaluateExportHashPolicy(
                    any(), eq(true), any(), any(), eq(adminContext));
            verify(exportService).requestExport(any(), any(), any(), any(), any(), any(), eq(adminContext));
        }

        @Test
        @DisplayName("Should accept unhashed export with valid authorization")
        void testUnhashedExportRequest_ValidAuth_Accepted() {
            // Arrange
            ExportJobController.ExportJobRequest request = new ExportJobController.ExportJobRequest();
            request.setExportType("HMIS_CSV");
            request.setReportingPeriodStart(LocalDate.of(2024, 1, 1));
            request.setReportingPeriodEnd(LocalDate.of(2024, 12, 31));
            request.setProjectIds(List.of(UUID.randomUUID()));
            request.setCocCode("CA-600");
            request.setExportReason("Legal subpoena compliance");
            request.setHashedExport(false); // unhashed
            request.setConsentScopes(Set.of(
                    ExportConsentScope.PII_DISCLOSURE,
                    ExportConsentScope.LEGAL_SUBPOENA
            ));

            ExportSecurityClearance clearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.PII_DISCLOSURE, ExportConsentScope.LEGAL_SUBPOENA),
                    "legal@example.org",
                    "Court order #12345",
                    48
            );
            request.setClearance(clearance);

            PolicyDecision permitDecision = PolicyDecision.permit(
                    "EXPORT_HASH_POLICY",
                    "v1.0",
                    "Unhashed export authorized with valid consent and clearance",
                    Map.of(),
                    adminContext
            );

            when(securityPolicyService.evaluateExportHashPolicy(
                    any(), eq(false), any(), eq(clearance), eq(adminContext)))
                    .thenReturn(permitDecision);

            UUID exportJobId = UUID.randomUUID();
            when(exportService.requestExport(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(exportJobId);

            // Act
            ResponseEntity<?> response = controller.requestExport(request, adminContext);

            // Assert
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            verify(securityPolicyService).evaluateExportHashPolicy(
                    any(), eq(false), any(), eq(clearance), eq(adminContext));
            verify(exportService).requestExport(any(), any(), any(), any(), any(), any(), eq(adminContext));
        }
    }

    @Nested
    @DisplayName("Fail-Fast Refusal Paths")
    class FailFastRefusalPaths {

        @Test
        @DisplayName("Should reject unhashed export with ALWAYS_HASH policy")
        void testUnhashedExportRequest_AlwaysHashPolicy_Rejected() {
            // Arrange
            ExportJobController.ExportJobRequest request = new ExportJobController.ExportJobRequest();
            request.setExportType("HMIS_CSV");
            request.setReportingPeriodStart(LocalDate.of(2024, 1, 1));
            request.setReportingPeriodEnd(LocalDate.of(2024, 12, 31));
            request.setProjectIds(List.of(UUID.randomUUID()));
            request.setCocCode("CA-600");
            request.setExportReason("Attempted unhashed export");
            request.setHashedExport(false); // unhashed
            request.setConsentScopes(Set.of(ExportConsentScope.PII_DISCLOSURE));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("error_code", "POLICY_PROHIBITS_UNHASHED");
            metadata.put("tenant_id", testTenantId.toString());

            PolicyDecision denyDecision = PolicyDecision.deny(
                    "EXPORT_HASH_POLICY",
                    "v1.0",
                    "Organization policy prohibits unhashed exports (ALWAYS_HASH mode)",
                    metadata,
                    adminContext
            );

            when(securityPolicyService.evaluateExportHashPolicy(
                    any(), eq(false), any(), any(), eq(adminContext)))
                    .thenReturn(denyDecision);

            // Act
            ResponseEntity<?> response = controller.requestExport(request, adminContext);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertTrue(response.getBody() instanceof ExportJobController.ExportPolicyViolationResponse);

            ExportJobController.ExportPolicyViolationResponse violation =
                    (ExportJobController.ExportPolicyViolationResponse) response.getBody();
            assertEquals("EXPORT_POLICY_VIOLATION", violation.getErrorCode());
            assertTrue(violation.getReason().contains("ALWAYS_HASH mode"));

            verify(securityPolicyService).evaluateExportHashPolicy(
                    any(), eq(false), any(), any(), eq(adminContext));
            verifyNoInteractions(exportService); // Should NOT proceed to export
        }

        @Test
        @DisplayName("Should reject unhashed export with missing consent scopes")
        void testUnhashedExportRequest_MissingConsent_Rejected() {
            // Arrange
            ExportJobController.ExportJobRequest request = new ExportJobController.ExportJobRequest();
            request.setExportType("HMIS_CSV");
            request.setReportingPeriodStart(LocalDate.of(2024, 1, 1));
            request.setReportingPeriodEnd(LocalDate.of(2024, 12, 31));
            request.setProjectIds(List.of(UUID.randomUUID()));
            request.setCocCode("CA-600");
            request.setExportReason("Missing consent test");
            request.setHashedExport(false);
            request.setConsentScopes(null); // missing consent

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("error_code", "MISSING_CONSENT_SCOPES");

            PolicyDecision denyDecision = PolicyDecision.deny(
                    "EXPORT_HASH_POLICY",
                    "v1.0",
                    "No consent scopes provided for unhashed export request",
                    metadata,
                    adminContext
            );

            when(securityPolicyService.evaluateExportHashPolicy(
                    any(), eq(false), eq(null), any(), eq(adminContext)))
                    .thenReturn(denyDecision);

            // Act
            ResponseEntity<?> response = controller.requestExport(request, adminContext);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            ExportJobController.ExportPolicyViolationResponse violation =
                    (ExportJobController.ExportPolicyViolationResponse) response.getBody();
            assertTrue(violation.getReason().contains("No consent scopes"));
            verifyNoInteractions(exportService);
        }

        @Test
        @DisplayName("Should reject unhashed export with expired clearance")
        void testUnhashedExportRequest_ExpiredClearance_Rejected() {
            // Arrange
            ExportJobController.ExportJobRequest request = new ExportJobController.ExportJobRequest();
            request.setExportType("HMIS_CSV");
            request.setReportingPeriodStart(LocalDate.of(2024, 1, 1));
            request.setReportingPeriodEnd(LocalDate.of(2024, 12, 31));
            request.setProjectIds(List.of(UUID.randomUUID()));
            request.setCocCode("CA-600");
            request.setExportReason("Expired clearance test");
            request.setHashedExport(false);
            request.setConsentScopes(Set.of(ExportConsentScope.PII_DISCLOSURE));

            ExportSecurityClearance expiredClearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.PII_DISCLOSURE),
                    "supervisor@example.org",
                    "Test clearance",
                    -1 // expired
            );
            request.setClearance(expiredClearance);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("error_code", "CLEARANCE_EXPIRED");
            metadata.put("clearance_expires_at", expiredClearance.expiresAt().toString());

            PolicyDecision denyDecision = PolicyDecision.deny(
                    "EXPORT_HASH_POLICY",
                    "v1.0",
                    "Security clearance expired at " + expiredClearance.expiresAt(),
                    metadata,
                    adminContext
            );

            when(securityPolicyService.evaluateExportHashPolicy(
                    any(), eq(false), any(), eq(expiredClearance), eq(adminContext)))
                    .thenReturn(denyDecision);

            // Act
            ResponseEntity<?> response = controller.requestExport(request, adminContext);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            ExportJobController.ExportPolicyViolationResponse violation =
                    (ExportJobController.ExportPolicyViolationResponse) response.getBody();
            assertTrue(violation.getReason().contains("expired"));
            verifyNoInteractions(exportService);
        }

        @Test
        @DisplayName("Should reject unhashed export with insufficient clearance scopes")
        void testUnhashedExportRequest_InsufficientClearance_Rejected() {
            // Arrange
            ExportJobController.ExportJobRequest request = new ExportJobController.ExportJobRequest();
            request.setExportType("HMIS_CSV");
            request.setReportingPeriodStart(LocalDate.of(2024, 1, 1));
            request.setReportingPeriodEnd(LocalDate.of(2024, 12, 31));
            request.setProjectIds(List.of(UUID.randomUUID()));
            request.setCocCode("CA-600");
            request.setExportReason("Insufficient clearance test");
            request.setHashedExport(false);
            request.setConsentScopes(Set.of(ExportConsentScope.PII_DISCLOSURE));

            // Clearance doesn't include PII_DISCLOSURE
            ExportSecurityClearance insufficientClearance = ExportSecurityClearance.grant(
                    adminContext.getUserId(),
                    adminContext.getUserName(),
                    Set.of(UserRole.ADMINISTRATOR),
                    Set.of(ExportConsentScope.HUD_REPORTING), // different scope
                    "supervisor@example.org",
                    "Limited clearance",
                    24
            );
            request.setClearance(insufficientClearance);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("error_code", "CLEARANCE_INSUFFICIENT");

            PolicyDecision denyDecision = PolicyDecision.deny(
                    "EXPORT_HASH_POLICY",
                    "v1.0",
                    "Security clearance does not authorize unhashed exports",
                    metadata,
                    adminContext
            );

            when(securityPolicyService.evaluateExportHashPolicy(
                    any(), eq(false), any(), eq(insufficientClearance), eq(adminContext)))
                    .thenReturn(denyDecision);

            // Act
            ResponseEntity<?> response = controller.requestExport(request, adminContext);

            // Assert
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            ExportJobController.ExportPolicyViolationResponse violation =
                    (ExportJobController.ExportPolicyViolationResponse) response.getBody();
            assertTrue(violation.getReason().contains("does not authorize"));
            verifyNoInteractions(exportService);
        }
    }

    @Nested
    @DisplayName("Error Response Structure")
    class ErrorResponseStructure {

        @Test
        @DisplayName("Should include detailed error metadata in violation response")
        void testViolationResponse_IncludesMetadata() {
            // Arrange
            ExportJobController.ExportJobRequest request = new ExportJobController.ExportJobRequest();
            request.setExportType("HMIS_CSV");
            request.setReportingPeriodStart(LocalDate.of(2024, 1, 1));
            request.setReportingPeriodEnd(LocalDate.of(2024, 12, 31));
            request.setProjectIds(List.of(UUID.randomUUID()));
            request.setCocCode("CA-600");
            request.setExportReason("Test");
            request.setHashedExport(false);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("error_code", "POLICY_PROHIBITS_UNHASHED");
            metadata.put("tenant_id", testTenantId.toString());
            metadata.put("user_id", adminContext.getUserId().toString());
            metadata.put("hash_behavior", "ALWAYS_HASH");
            metadata.put("evaluation_time_ms", 5L);

            PolicyDecision denyDecision = PolicyDecision.deny(
                    "EXPORT_HASH_POLICY",
                    "v1.0",
                    "Organization policy prohibits unhashed exports (ALWAYS_HASH mode)",
                    metadata,
                    adminContext
            );

            when(securityPolicyService.evaluateExportHashPolicy(
                    any(), eq(false), any(), any(), eq(adminContext)))
                    .thenReturn(denyDecision);

            // Act
            ResponseEntity<?> response = controller.requestExport(request, adminContext);

            // Assert
            ExportJobController.ExportPolicyViolationResponse violation =
                    (ExportJobController.ExportPolicyViolationResponse) response.getBody();

            assertNotNull(violation.getMetadata());
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMetadata = (Map<String, Object>) violation.getMetadata();

            assertEquals("POLICY_PROHIBITS_UNHASHED", responseMetadata.get("error_code"));
            assertEquals(testTenantId.toString(), responseMetadata.get("tenant_id"));
            assertTrue(responseMetadata.containsKey("evaluation_time_ms"));
        }

        @Test
        @DisplayName("Should provide actionable error messages")
        void testViolationResponse_ActionableMessages() {
            // Arrange
            ExportJobController.ExportJobRequest request = new ExportJobController.ExportJobRequest();
            request.setExportType("HMIS_CSV");
            request.setReportingPeriodStart(LocalDate.of(2024, 1, 1));
            request.setReportingPeriodEnd(LocalDate.of(2024, 12, 31));
            request.setProjectIds(List.of(UUID.randomUUID()));
            request.setCocCode("CA-600");
            request.setExportReason("Test");
            request.setHashedExport(false);
            request.setConsentScopes(Set.of(ExportConsentScope.HUD_REPORTING)); // insufficient

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("error_code", "INSUFFICIENT_CONSENT_SCOPES");
            metadata.put("missing_scopes", List.of("PII_DISCLOSURE"));

            PolicyDecision denyDecision = PolicyDecision.deny(
                    "EXPORT_HASH_POLICY",
                    "v1.0",
                    "Missing required consent scopes: [PII_DISCLOSURE]",
                    metadata,
                    adminContext
            );

            when(securityPolicyService.evaluateExportHashPolicy(
                    any(), eq(false), any(), any(), eq(adminContext)))
                    .thenReturn(denyDecision);

            // Act
            ResponseEntity<?> response = controller.requestExport(request, adminContext);

            // Assert
            ExportJobController.ExportPolicyViolationResponse violation =
                    (ExportJobController.ExportPolicyViolationResponse) response.getBody();

            assertTrue(violation.getReason().contains("Missing required consent scopes"));
            assertTrue(violation.getReason().contains("PII_DISCLOSURE"));
        }
    }
}
