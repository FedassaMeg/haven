package org.haven.shared.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for verifying Keycloak JWT tokens
 * Implements JWKS caching, issuer/audience validation, and failure telemetry
 */
@Service
public class KeycloakTokenVerificationService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakTokenVerificationService.class);

    private final JwtDecoder jwtDecoder;
    private final String expectedIssuer;
    private final String expectedAudience;
    private final MeterRegistry meterRegistry;

    // Telemetry counters
    private final Counter tokenVerificationSuccess;
    private final Counter tokenVerificationFailure;
    private final Counter tokenExpired;
    private final Counter tokenTampering;
    private final Counter tokenMissingClaims;

    public KeycloakTokenVerificationService(
            JwtDecoder jwtDecoder,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String expectedIssuer,
            @Value("${haven.security.jwt.expected-audience:haven-backend}") String expectedAudience,
            MeterRegistry meterRegistry) {
        this.jwtDecoder = jwtDecoder;
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
        this.meterRegistry = meterRegistry;

        // Initialize telemetry counters
        this.tokenVerificationSuccess = Counter.builder("token.verification.success")
                .description("Number of successful token verifications")
                .register(meterRegistry);
        this.tokenVerificationFailure = Counter.builder("token.verification.failure")
                .description("Number of failed token verifications")
                .tag("reason", "unknown")
                .register(meterRegistry);
        this.tokenExpired = Counter.builder("token.verification.expired")
                .description("Number of expired tokens")
                .register(meterRegistry);
        this.tokenTampering = Counter.builder("token.verification.tampering")
                .description("Number of tampered tokens detected")
                .register(meterRegistry);
        this.tokenMissingClaims = Counter.builder("token.verification.missing_claims")
                .description("Number of tokens with missing required claims")
                .register(meterRegistry);
    }

    /**
     * Verify and decode a JWT token
     * @param token JWT token string
     * @return TokenVerificationResult containing decoded JWT or error
     */
    public TokenVerificationResult verifyToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            recordFailure("missing_token");
            return TokenVerificationResult.failure("Token is missing", FailureReason.MISSING_TOKEN);
        }

        try {
            // Decode and verify signature using JWKS
            Jwt jwt = jwtDecoder.decode(token);

            // Verify issuer
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
            if (!expectedIssuer.equals(issuer)) {
                recordFailure("invalid_issuer");
                return TokenVerificationResult.failure(
                    String.format("Invalid issuer: expected %s but got %s", expectedIssuer, issuer),
                    FailureReason.INVALID_ISSUER
                );
            }

            // Verify audience
            List<String> audiences = jwt.getAudience();
            if (audiences == null || !audiences.contains(expectedAudience)) {
                recordFailure("invalid_audience");
                return TokenVerificationResult.failure(
                    String.format("Invalid audience: expected %s but got %s", expectedAudience, audiences),
                    FailureReason.INVALID_AUDIENCE
                );
            }

            // Verify expiration
            Instant expiresAt = jwt.getExpiresAt();
            if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
                tokenExpired.increment();
                recordFailure("expired");
                return TokenVerificationResult.failure("Token has expired", FailureReason.EXPIRED);
            }

            // Verify not before time
            Instant notBefore = jwt.getNotBefore();
            if (notBefore != null && notBefore.isAfter(Instant.now())) {
                recordFailure("not_yet_valid");
                return TokenVerificationResult.failure("Token is not yet valid", FailureReason.NOT_YET_VALID);
            }

            tokenVerificationSuccess.increment();
            log.debug("Token verification successful for subject: {}", jwt.getSubject());

            return TokenVerificationResult.success(jwt);

        } catch (JwtException e) {
            // Handle JWT decoding/validation failures
            if (e instanceof JwtValidationException) {
                tokenTampering.increment();
                recordFailure("signature_invalid");
                return TokenVerificationResult.failure("Token signature validation failed: " + e.getMessage(), FailureReason.SIGNATURE_INVALID);
            } else if (e.getMessage() != null && e.getMessage().contains("expired")) {
                tokenExpired.increment();
                recordFailure("expired");
                return TokenVerificationResult.failure("Token has expired", FailureReason.EXPIRED);
            } else {
                recordFailure("decode_error");
                return TokenVerificationResult.failure("Failed to decode token: " + e.getMessage(), FailureReason.DECODE_ERROR);
            }
        } catch (Exception e) {
            recordFailure("unexpected_error");
            log.error("Unexpected error during token verification", e);
            return TokenVerificationResult.failure("Unexpected error: " + e.getMessage(), FailureReason.UNEXPECTED_ERROR);
        }
    }

    /**
     * Extract role claims from verified JWT
     * Supports both realm_access and resource_access claim structures
     */
    public List<String> extractRoleClaims(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                roles.addAll(realmRoles);
            }
        }

        // Extract resource-specific roles
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            for (Object clientRoles : resourceAccess.values()) {
                if (clientRoles instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rolesMap = (Map<String, Object>) clientRoles;
                    if (rolesMap.containsKey("roles")) {
                        @SuppressWarnings("unchecked")
                        List<String> clientRoleList = (List<String>) rolesMap.get("roles");
                        if (clientRoleList != null) {
                            roles.addAll(clientRoleList);
                        }
                    }
                }
            }
        }

        log.debug("Extracted roles from token: {}", roles);
        return new ArrayList<>(roles);
    }

    /**
     * Extract consent scopes from verified JWT
     * Looks for custom consent_scopes claim
     */
    public List<String> extractConsentScopes(Jwt jwt) {
        // Check for consent_scopes claim
        Object consentScopesObj = jwt.getClaim("consent_scopes");
        if (consentScopesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> scopes = (List<String>) consentScopesObj;
            log.debug("Extracted consent scopes from token: {}", scopes);
            return new ArrayList<>(scopes);
        }

        // Fallback to standard OAuth2 scope claim
        String scopeClaim = jwt.getClaimAsString("scope");
        if (scopeClaim != null && !scopeClaim.isEmpty()) {
            List<String> scopes = Arrays.asList(scopeClaim.split(" "));
            log.debug("Extracted scopes from standard scope claim: {}", scopes);
            return scopes;
        }

        log.debug("No consent scopes found in token");
        return Collections.emptyList();
    }

    /**
     * Extract user ID from JWT
     */
    public UUID extractUserId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isEmpty()) {
            throw new IllegalArgumentException("Token missing subject claim");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            // If subject is not a UUID, hash it to create a deterministic UUID
            log.warn("Token subject is not a valid UUID, using deterministic UUID generation: {}", subject);
            return UUID.nameUUIDFromBytes(subject.getBytes());
        }
    }

    /**
     * Extract username from JWT
     */
    public String extractUsername(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isEmpty()) {
            return preferredUsername;
        }

        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isEmpty()) {
            return email;
        }

        return jwt.getSubject();
    }

    /**
     * Verify required claims are present in the token
     */
    public boolean verifyRequiredClaims(Jwt jwt, String... requiredClaims) {
        for (String claim : requiredClaims) {
            if (!jwt.hasClaim(claim) || jwt.getClaim(claim) == null) {
                tokenMissingClaims.increment();
                log.warn("Token missing required claim: {}", claim);
                return false;
            }
        }
        return true;
    }

    private void recordFailure(String reason) {
        Counter.builder("token.verification.failure")
                .description("Number of failed token verifications")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Result of token verification
     */
    public static class TokenVerificationResult {
        private final boolean valid;
        private final Jwt jwt;
        private final String errorMessage;
        private final FailureReason failureReason;

        private TokenVerificationResult(boolean valid, Jwt jwt, String errorMessage, FailureReason failureReason) {
            this.valid = valid;
            this.jwt = jwt;
            this.errorMessage = errorMessage;
            this.failureReason = failureReason;
        }

        public static TokenVerificationResult success(Jwt jwt) {
            return new TokenVerificationResult(true, jwt, null, null);
        }

        public static TokenVerificationResult failure(String errorMessage, FailureReason reason) {
            return new TokenVerificationResult(false, null, errorMessage, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public Jwt getJwt() {
            if (!valid) {
                throw new IllegalStateException("Cannot get JWT from failed verification result");
            }
            return jwt;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public FailureReason getFailureReason() {
            return failureReason;
        }

        public int getHttpStatusCode() {
            if (valid) {
                return 200;
            }
            return switch (failureReason) {
                case MISSING_TOKEN, EXPIRED, NOT_YET_VALID -> 401;
                case SIGNATURE_INVALID, INVALID_ISSUER, INVALID_AUDIENCE, MISSING_CLAIMS -> 401;
                default -> 500;
            };
        }
    }

    /**
     * Reasons for token verification failure
     */
    public enum FailureReason {
        MISSING_TOKEN,
        EXPIRED,
        NOT_YET_VALID,
        SIGNATURE_INVALID,
        INVALID_ISSUER,
        INVALID_AUDIENCE,
        MISSING_CLAIMS,
        DECODE_ERROR,
        UNEXPECTED_ERROR
    }
}
