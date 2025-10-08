package org.haven.shared.security;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Regression tests for KeycloakTokenVerificationService
 * Tests token expiration, tampering, and missing claims scenarios
 */
@ExtendWith(MockitoExtension.class)
class KeycloakTokenVerificationServiceTest {

    @Mock
    private JwtDecoder jwtDecoder;

    private KeycloakTokenVerificationService verificationService;
    private MeterRegistry meterRegistry;

    private static final String VALID_ISSUER = "http://localhost:8081/realms/haven";
    private static final String VALID_AUDIENCE = "haven-backend";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        verificationService = new KeycloakTokenVerificationService(
                jwtDecoder,
                VALID_ISSUER,
                VALID_AUDIENCE,
                meterRegistry
        );
    }

    @Test
    @DisplayName("Valid token passes all verification checks")
    void testValidTokenPassesVerification() {
        // Given: Valid JWT token
        String token = "valid.jwt.token";
        Jwt jwt = createValidJwt();
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification succeeds
        assertTrue(result.isValid());
        assertNotNull(result.getJwt());
        assertEquals(200, result.getHttpStatusCode());
    }

    @Test
    @DisplayName("Missing token returns 401 with appropriate error")
    void testMissingTokenReturns401() {
        // Given: Null token
        String token = null;

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification fails with 401
        assertFalse(result.isValid());
        assertEquals(401, result.getHttpStatusCode());
        assertEquals(KeycloakTokenVerificationService.FailureReason.MISSING_TOKEN, result.getFailureReason());
        assertTrue(result.getErrorMessage().contains("missing"));
    }

    @Test
    @DisplayName("Empty token returns 401")
    void testEmptyTokenReturns401() {
        // Given: Empty token
        String token = "   ";

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification fails
        assertFalse(result.isValid());
        assertEquals(401, result.getHttpStatusCode());
    }

    @Test
    @DisplayName("Expired token returns 401 with EXPIRED reason")
    void testExpiredTokenReturns401() {
        // Given: Expired JWT token
        String token = "expired.jwt.token";
        Jwt expiredJwt = createExpiredJwt();
        when(jwtDecoder.decode(token)).thenReturn(expiredJwt);

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification fails with EXPIRED
        assertFalse(result.isValid());
        assertEquals(401, result.getHttpStatusCode());
        assertEquals(KeycloakTokenVerificationService.FailureReason.EXPIRED, result.getFailureReason());
        assertTrue(result.getErrorMessage().toLowerCase().contains("expired"));
    }

    @Test
    @DisplayName("Token not yet valid returns 401 with NOT_YET_VALID reason")
    void testNotYetValidTokenReturns401() {
        // Given: Token with future notBefore time
        String token = "future.jwt.token";
        Jwt futureJwt = createFutureJwt();
        when(jwtDecoder.decode(token)).thenReturn(futureJwt);

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification fails
        assertFalse(result.isValid());
        assertEquals(401, result.getHttpStatusCode());
        assertEquals(KeycloakTokenVerificationService.FailureReason.NOT_YET_VALID, result.getFailureReason());
    }

    @Test
    @DisplayName("Tampered token (invalid signature) returns 401")
    void testTamperedTokenReturns401() {
        // Given: Token that fails signature validation
        String token = "tampered.jwt.token";
        when(jwtDecoder.decode(token)).thenThrow(new JwtValidationException("Invalid signature", Collections.emptyList()));

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification fails with SIGNATURE_INVALID
        assertFalse(result.isValid());
        assertEquals(401, result.getHttpStatusCode());
        assertEquals(KeycloakTokenVerificationService.FailureReason.SIGNATURE_INVALID, result.getFailureReason());
        assertTrue(result.getErrorMessage().contains("signature"));
    }

    @Test
    @DisplayName("Invalid issuer returns 401")
    void testInvalidIssuerReturns401() {
        // Given: Token with wrong issuer
        String token = "wrong.issuer.token";
        Jwt jwt = createJwtWithInvalidIssuer();
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification fails
        assertFalse(result.isValid());
        assertEquals(401, result.getHttpStatusCode());
        assertEquals(KeycloakTokenVerificationService.FailureReason.INVALID_ISSUER, result.getFailureReason());
        assertTrue(result.getErrorMessage().contains("issuer"));
    }

    @Test
    @DisplayName("Invalid audience returns 401")
    void testInvalidAudienceReturns401() {
        // Given: Token with wrong audience
        String token = "wrong.audience.token";
        Jwt jwt = createJwtWithInvalidAudience();
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification fails
        assertFalse(result.isValid());
        assertEquals(401, result.getHttpStatusCode());
        assertEquals(KeycloakTokenVerificationService.FailureReason.INVALID_AUDIENCE, result.getFailureReason());
        assertTrue(result.getErrorMessage().contains("audience"));
    }

    @Test
    @DisplayName("Token with missing required claims fails verification")
    void testMissingClaimsFailsVerification() {
        // Given: Valid token
        String token = "valid.jwt.token";
        Jwt jwt = createValidJwt();
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When: Verify required claims that don't exist
        boolean result = verificationService.verifyRequiredClaims(jwt, "non_existent_claim");

        // Then: Verification fails
        assertFalse(result);
    }

    @Test
    @DisplayName("Decode error returns 401")
    void testDecodeErrorReturns401() {
        // Given: Token that can't be decoded
        String token = "malformed.token";
        when(jwtDecoder.decode(token)).thenThrow(new BadJwtException("Malformed JWT"));

        // When: Verify token
        KeycloakTokenVerificationService.TokenVerificationResult result = verificationService.verifyToken(token);

        // Then: Verification fails
        assertFalse(result.isValid());
        assertEquals(401, result.getHttpStatusCode());
        assertEquals(KeycloakTokenVerificationService.FailureReason.DECODE_ERROR, result.getFailureReason());
    }

    @Test
    @DisplayName("Extract roles from realm_access claim")
    void testExtractRolesFromRealmAccess() {
        // Given: JWT with realm_access roles
        Jwt jwt = createJwtWithRoles(List.of("DV_COUNSELOR", "CASE_MANAGER"), null);

        // When: Extract roles
        List<String> roles = verificationService.extractRoleClaims(jwt);

        // Then: Roles are extracted
        assertEquals(2, roles.size());
        assertTrue(roles.contains("DV_COUNSELOR"));
        assertTrue(roles.contains("CASE_MANAGER"));
    }

    @Test
    @DisplayName("Extract roles from resource_access claim")
    void testExtractRolesFromResourceAccess() {
        // Given: JWT with resource_access roles
        Map<String, Object> resourceRoles = new HashMap<>();
        Map<String, Object> clientRoles = new HashMap<>();
        clientRoles.put("roles", List.of("ADMINISTRATOR", "SUPERVISOR"));
        resourceRoles.put("haven-backend", clientRoles);

        Jwt jwt = createJwtWithRoles(null, resourceRoles);

        // When: Extract roles
        List<String> roles = verificationService.extractRoleClaims(jwt);

        // Then: Roles are extracted
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ADMINISTRATOR"));
        assertTrue(roles.contains("SUPERVISOR"));
    }

    @Test
    @DisplayName("Extract consent scopes from consent_scopes claim")
    void testExtractConsentScopes() {
        // Given: JWT with consent_scopes
        Jwt jwt = createJwtWithConsentScopes(List.of("dv_view", "legal_view"));

        // When: Extract consent scopes
        List<String> scopes = verificationService.extractConsentScopes(jwt);

        // Then: Scopes are extracted
        assertEquals(2, scopes.size());
        assertTrue(scopes.contains("dv_view"));
        assertTrue(scopes.contains("legal_view"));
    }

    @Test
    @DisplayName("Extract consent scopes from standard scope claim as fallback")
    void testExtractConsentScopesFromStandardClaim() {
        // Given: JWT with standard OAuth2 scope claim
        Jwt jwt = createJwtWithStandardScope("openid profile dv_view");

        // When: Extract consent scopes
        List<String> scopes = verificationService.extractConsentScopes(jwt);

        // Then: Scopes are extracted
        assertEquals(3, scopes.size());
        assertTrue(scopes.contains("dv_view"));
    }

    @Test
    @DisplayName("Extract user ID from subject claim")
    void testExtractUserId() {
        // Given: JWT with UUID subject
        UUID userId = UUID.randomUUID();
        Jwt jwt = createJwtWithSubject(userId.toString());

        // When: Extract user ID
        UUID extractedId = verificationService.extractUserId(jwt);

        // Then: User ID matches
        assertEquals(userId, extractedId);
    }

    @Test
    @DisplayName("Extract username from preferred_username claim")
    void testExtractUsername() {
        // Given: JWT with preferred_username
        Jwt jwt = createJwtWithUsername("john.doe");

        // When: Extract username
        String username = verificationService.extractUsername(jwt);

        // Then: Username matches
        assertEquals("john.doe", username);
    }

    @Test
    @DisplayName("Telemetry increments success counter on valid token")
    void testTelemetryIncrementSuccessCounter() {
        // Given: Valid token
        String token = "valid.jwt.token";
        Jwt jwt = createValidJwt();
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When: Verify token
        verificationService.verifyToken(token);

        // Then: Success counter incremented
        double count = meterRegistry.counter("token.verification.success").count();
        assertEquals(1.0, count);
    }

    @Test
    @DisplayName("Telemetry increments failure counter on invalid token")
    void testTelemetryIncrementFailureCounter() {
        // Given: Invalid token (expired)
        String token = "expired.jwt.token";
        Jwt expiredJwt = createExpiredJwt();
        when(jwtDecoder.decode(token)).thenReturn(expiredJwt);

        // When: Verify token
        verificationService.verifyToken(token);

        // Then: Failure counter incremented
        assertTrue(meterRegistry.counter("token.verification.failure", "reason", "expired").count() > 0);
    }

    // Helper methods to create test JWTs

    private Jwt createValidJwt() {
        return createJwt(
                VALID_ISSUER,
                List.of(VALID_AUDIENCE),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(60)
        );
    }

    private Jwt createExpiredJwt() {
        return createJwt(
                VALID_ISSUER,
                List.of(VALID_AUDIENCE),
                Instant.now().minusSeconds(3600), // Expired 1 hour ago
                Instant.now().minusSeconds(7200)
        );
    }

    private Jwt createFutureJwt() {
        return createJwt(
                VALID_ISSUER,
                List.of(VALID_AUDIENCE),
                Instant.now().plusSeconds(7200),
                Instant.now().plusSeconds(3600) // Not valid for another hour
        );
    }

    private Jwt createJwtWithInvalidIssuer() {
        return createJwt(
                "http://evil.com/realms/fake",
                List.of(VALID_AUDIENCE),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(60)
        );
    }

    private Jwt createJwtWithInvalidAudience() {
        return createJwt(
                VALID_ISSUER,
                List.of("wrong-audience"),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(60)
        );
    }

    private Jwt createJwt(String issuer, List<String> audience, Instant expiresAt, Instant notBefore) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("iss", issuer);
        claims.put("aud", audience);
        claims.put("exp", expiresAt);
        claims.put("nbf", notBefore);
        claims.put("iat", Instant.now());
        claims.put("preferred_username", "test-user");

        return new Jwt("token", Instant.now(), expiresAt, headers, claims);
    }

    private Jwt createJwtWithRoles(List<String> realmRoles, Map<String, Object> resourceAccess) {
        Jwt baseJwt = createValidJwt();
        Map<String, Object> claims = new HashMap<>(baseJwt.getClaims());

        if (realmRoles != null) {
            Map<String, Object> realmAccess = new HashMap<>();
            realmAccess.put("roles", realmRoles);
            claims.put("realm_access", realmAccess);
        }

        if (resourceAccess != null) {
            claims.put("resource_access", resourceAccess);
        }

        return new Jwt(baseJwt.getTokenValue(), baseJwt.getIssuedAt(), baseJwt.getExpiresAt(),
                baseJwt.getHeaders(), claims);
    }

    private Jwt createJwtWithConsentScopes(List<String> consentScopes) {
        Jwt baseJwt = createValidJwt();
        Map<String, Object> claims = new HashMap<>(baseJwt.getClaims());
        claims.put("consent_scopes", consentScopes);

        return new Jwt(baseJwt.getTokenValue(), baseJwt.getIssuedAt(), baseJwt.getExpiresAt(),
                baseJwt.getHeaders(), claims);
    }

    private Jwt createJwtWithStandardScope(String scope) {
        Jwt baseJwt = createValidJwt();
        Map<String, Object> claims = new HashMap<>(baseJwt.getClaims());
        claims.put("scope", scope);

        return new Jwt(baseJwt.getTokenValue(), baseJwt.getIssuedAt(), baseJwt.getExpiresAt(),
                baseJwt.getHeaders(), claims);
    }

    private Jwt createJwtWithSubject(String subject) {
        Jwt baseJwt = createValidJwt();
        Map<String, Object> claims = new HashMap<>(baseJwt.getClaims());
        claims.put("sub", subject);

        return new Jwt(baseJwt.getTokenValue(), baseJwt.getIssuedAt(), baseJwt.getExpiresAt(),
                baseJwt.getHeaders(), claims);
    }

    private Jwt createJwtWithUsername(String username) {
        Jwt baseJwt = createValidJwt();
        Map<String, Object> claims = new HashMap<>(baseJwt.getClaims());
        claims.put("preferred_username", username);

        return new Jwt(baseJwt.getTokenValue(), baseJwt.getIssuedAt(), baseJwt.getExpiresAt(),
                baseJwt.getHeaders(), claims);
    }
}
