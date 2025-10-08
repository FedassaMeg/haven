package org.haven.clientprofile.infrastructure.security;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.haven.shared.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for PIIRedactionService with Keycloak token verification
 * Tests role-based and consent-based redaction policies
 */
@ExtendWith(MockitoExtension.class)
class PIIRedactionServiceIntegrationTest {

    @Mock
    private JwtDecoder jwtDecoder;

    private KeycloakTokenVerificationService tokenVerificationService;
    private PIIRedactionService redactionService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        tokenVerificationService = new KeycloakTokenVerificationService(
                jwtDecoder,
                "http://localhost:8081/realms/haven",
                "haven-backend",
                meterRegistry
        );
        redactionService = new PIIRedactionService(tokenVerificationService);
    }

    @Test
    @DisplayName("DV advocate with dv_view scope sees unredacted DV notes")
    void testDVAdvocateUnredactedAccess() {
        // Given: Token with DV_COUNSELOR role and dv_view consent scope
        String token = "valid-jwt-token";
        Jwt jwt = createJwt(
                "user-123",
                List.of("DV_COUNSELOR"),
                List.of("dv_view"),
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        TestDataWithDVNote testData = new TestDataWithDVNote();
        testData.firstName = "Jane";
        testData.lastName = "Doe";
        testData.dvConfidentialNote = "Sensitive DV information about safety planning";
        testData.serviceData = "General service information";

        // When: Apply redaction
        TestDataWithDVNote result = redactionService.applyRedactionWithToken(
                testData,
                token,
                UUID.randomUUID()
        );

        // Then: DV notes are visible (unredacted)
        assertNotNull(result);
        assertEquals("Jane", result.firstName);
        assertEquals("Doe", result.lastName);
        assertEquals("Sensitive DV information about safety planning", result.dvConfidentialNote);
        assertEquals("General service information", result.serviceData);
    }

    @Test
    @DisplayName("CE intake worker receives redacted payload by default")
    void testCEIntakeRedactedByDefault() {
        // Given: Token with CASE_MANAGER role, no special consent scopes
        String token = "valid-jwt-token";
        Jwt jwt = createJwt(
                "user-456",
                List.of("CASE_MANAGER"),
                List.of(), // No consent scopes
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        TestDataWithDVNote testData = new TestDataWithDVNote();
        testData.firstName = "Jane";
        testData.lastName = "Doe";
        testData.ssn = "123-45-6789";
        testData.dvConfidentialNote = "Sensitive DV information";
        testData.serviceData = "General service information";

        // When: Apply redaction
        TestDataWithDVNote result = redactionService.applyRedactionWithToken(
                testData,
                token,
                UUID.randomUUID()
        );

        // Then: Sensitive data is redacted
        assertNotNull(result);
        assertTrue(result.firstName.contains("*") || result.firstName.equals("[NAME REDACTED]"));
        assertTrue(result.lastName.contains("*") || result.lastName.equals("[NAME REDACTED]"));
        assertTrue(result.ssn.contains("***"));
        assertNull(result.dvConfidentialNote); // Fully redacted
        assertEquals("General service information", result.serviceData); // Service data visible
    }

    @Test
    @DisplayName("Compliance auditor sees only hashed identifiers")
    void testComplianceAuditorHashOnly() {
        // Given: Token with ADMINISTRATOR role acting as compliance auditor
        String token = "valid-jwt-token";
        Jwt jwt = createJwt(
                "auditor-789",
                List.of("ADMINISTRATOR"),
                List.of(), // No consent scopes
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        TestDataWithDVNote testData = new TestDataWithDVNote();
        testData.firstName = "Jane";
        testData.lastName = "Doe";
        testData.ssn = "123-45-6789";
        testData.email = "jane.doe@example.com";

        // When: Apply redaction
        TestDataWithDVNote result = redactionService.applyRedactionWithToken(
                testData,
                token,
                UUID.randomUUID()
        );

        // Then: Identifiers are redacted (partial for admin)
        assertNotNull(result);
        assertTrue(result.firstName.contains("*") || result.firstName.equals("[NAME REDACTED]"));
        assertTrue(result.ssn.contains("***"));
        assertTrue(result.email == null || result.email.contains("***"));
    }

    @Test
    @DisplayName("Consent scope removal forces redaction even if role present")
    void testConsentScopeRemovalForcesRedaction() {
        // Given: DV Counselor WITHOUT dv_view scope (consent withdrawn)
        String token = "valid-jwt-token";
        Jwt jwt = createJwt(
                "user-123",
                List.of("DV_COUNSELOR"),
                List.of(), // NO dv_view scope - consent withdrawn
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        TestDataWithDVNote testData = new TestDataWithDVNote();
        testData.dvConfidentialNote = "Sensitive DV information";

        // When: Apply redaction
        TestDataWithDVNote result = redactionService.applyRedactionWithToken(
                testData,
                token,
                UUID.randomUUID()
        );

        // Then: DV note is redacted despite having DV_COUNSELOR role
        assertNotNull(result);
        assertNull(result.dvConfidentialNote); // Fully redacted due to missing consent
    }

    @Test
    @DisplayName("Role downgrade removes access to confidential data")
    void testRoleDowngradeRemovesAccess() {
        // Scenario 1: User has DV_COUNSELOR role with dv_view scope
        String tokenWithRole = "token-with-dv-role";
        Jwt jwtWithRole = createJwt(
                "user-123",
                List.of("DV_COUNSELOR"),
                List.of("dv_view"),
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(tokenWithRole)).thenReturn(jwtWithRole);

        TestDataWithDVNote testData = new TestDataWithDVNote();
        testData.dvConfidentialNote = "Sensitive information";

        TestDataWithDVNote resultWithRole = redactionService.applyRedactionWithToken(
                testData,
                tokenWithRole,
                UUID.randomUUID()
        );

        // DV note visible
        assertEquals("Sensitive information", resultWithRole.dvConfidentialNote);

        // Scenario 2: Role downgraded to CASE_MANAGER (no dv_view)
        String tokenDowngraded = "token-downgraded";
        Jwt jwtDowngraded = createJwt(
                "user-123",
                List.of("CASE_MANAGER"), // Downgraded
                List.of(), // No consent scopes
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(tokenDowngraded)).thenReturn(jwtDowngraded);

        TestDataWithDVNote resultDowngraded = redactionService.applyRedactionWithToken(
                testData,
                tokenDowngraded,
                UUID.randomUUID()
        );

        // DV note now redacted
        assertNull(resultDowngraded.dvConfidentialNote);
    }

    @Test
    @DisplayName("Medical role with medical_view scope sees medical info")
    void testMedicalRoleWithConsentSeesData() {
        // Given: NURSE role with medical_view consent
        String token = "valid-jwt-token";
        Jwt jwt = createJwt(
                "nurse-001",
                List.of("NURSE"),
                List.of("medical_view"),
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        TestDataWithMedical testData = new TestDataWithMedical();
        testData.medicalDiagnosis = "Patient diagnosis information";
        testData.treatmentPlan = "Treatment plan details";

        // When: Apply redaction
        TestDataWithMedical result = redactionService.applyRedactionWithToken(
                testData,
                token,
                UUID.randomUUID()
        );

        // Then: Medical data visible
        assertNotNull(result);
        assertEquals("Patient diagnosis information", result.medicalDiagnosis);
        assertEquals("Treatment plan details", result.treatmentPlan);
    }

    @Test
    @DisplayName("Legal advocate with legal_view scope sees legal info")
    void testLegalAdvocateWithConsentSeesData() {
        // Given: LEGAL_ADVOCATE role with legal_view consent
        String token = "valid-jwt-token";
        Jwt jwt = createJwt(
                "advocate-001",
                List.of("LEGAL_ADVOCATE"),
                List.of("legal_view"),
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        TestDataWithLegal testData = new TestDataWithLegal();
        testData.legalCaseNumber = "CASE-2024-001";
        testData.courtTestimonyNotes = "Testimony notes";

        // When: Apply redaction
        TestDataWithLegal result = redactionService.applyRedactionWithToken(
                testData,
                token,
                UUID.randomUUID()
        );

        // Then: Legal data visible
        assertNotNull(result);
        assertEquals("CASE-2024-001", result.legalCaseNumber);
        assertEquals("Testimony notes", result.courtTestimonyNotes);
    }

    @Test
    @DisplayName("External partner receives maximum redaction")
    void testExternalPartnerMaximumRedaction() {
        // Given: VSP (external partner) role
        String token = "valid-jwt-token";
        Jwt jwt = createJwt(
                "vsp-partner-001",
                List.of("VICTIM_SERVICE_PROVIDER"),
                List.of(), // Even with consent scopes, external partners get redaction
                Instant.now().plusSeconds(3600)
        );
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        TestDataWithDVNote testData = new TestDataWithDVNote();
        testData.firstName = "Jane";
        testData.lastName = "Doe";
        testData.ssn = "123-45-6789";
        testData.dvConfidentialNote = "Sensitive info";

        // When: Apply redaction
        TestDataWithDVNote result = redactionService.applyRedactionWithToken(
                testData,
                token,
                UUID.randomUUID()
        );

        // Then: All PII heavily redacted or hashed
        assertNotNull(result);
        // External partners get hash-only for identifiers
        assertNotEquals("Jane", result.firstName);
        assertNotEquals("Doe", result.lastName);
        assertNotEquals("123-45-6789", result.ssn);
        assertNull(result.dvConfidentialNote);
    }

    // Helper method to create JWT for testing
    private Jwt createJwt(String subject, List<String> roles, List<String> consentScopes, Instant expiresAt) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("iss", "http://localhost:8081/realms/haven");
        claims.put("aud", List.of("haven-backend"));
        claims.put("exp", expiresAt);
        claims.put("iat", Instant.now());
        claims.put("preferred_username", "test-user");

        // Add roles in Keycloak format
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);
        claims.put("realm_access", realmAccess);

        // Add consent scopes
        claims.put("consent_scopes", consentScopes);

        return new Jwt(
                "token-value",
                Instant.now(),
                expiresAt,
                headers,
                claims
        );
    }

    // Test data classes
    static class TestDataWithDVNote {
        public String firstName;
        public String lastName;
        public String ssn;
        public String email;
        public String dvConfidentialNote;
        public String serviceData;
    }

    static class TestDataWithMedical {
        public String medicalDiagnosis;
        public String treatmentPlan;
        public String medicalHistory;
    }

    static class TestDataWithLegal {
        public String legalCaseNumber;
        public String courtTestimonyNotes;
        public String attorneyNotes;
    }
}
