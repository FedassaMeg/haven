# PIIRedactionService Keycloak Integration - Rollout Guide

## Overview

This guide documents the rollout steps for the enhanced `PIIRedactionService` with Keycloak token verification, role-based redaction policies, and consent-based access control.

## Architecture Summary

### Components Implemented

1. **KeycloakTokenVerificationService** (`shared-kernel`)
   - JWT token verification with JWKS caching
   - Issuer and audience validation
   - Role and consent scope extraction
   - Comprehensive failure telemetry

2. **RedactionPermission** (`shared-kernel`)
   - Internal permission model mapping roles to redaction policies
   - Consent scope-based policy enforcement
   - Field-level redaction level determination

3. **Enhanced PIIRedactionService** (`client-profile`)
   - Token-based redaction with `applyRedactionWithToken()` method
   - Dynamic permission cache invalidation
   - Backward-compatible with existing `applyRedaction()` method

## Configuration

### Required Configuration Properties

Add to your `application.properties`:

```properties
# JWT Verification
haven.security.jwt.expected-audience=${JWT_EXPECTED_AUDIENCE:haven-backend}
haven.security.jwt.jwks-cache-ttl=${JWT_JWKS_CACHE_TTL:3600}
haven.security.jwt.jwks-cache-max-size=${JWT_JWKS_CACHE_MAX_SIZE:100}

# Redaction Permission Caching
spring.cache.type=${CACHE_TYPE:caffeine}
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=600s
spring.cache.cache-names=redactionPermissions,jwksCache

# Role-based Redaction Policies
haven.security.redaction.dv-advocate-unredacted=${REDACTION_DV_ADVOCATE_UNREDACTED:true}
haven.security.redaction.ce-intake-default-redacted=${REDACTION_CE_INTAKE_DEFAULT:true}
haven.security.redaction.external-partner-hash-only=${REDACTION_EXTERNAL_HASH_ONLY:true}
haven.security.redaction.compliance-auditor-hash-only=${REDACTION_COMPLIANCE_HASH:true}
```

### Environment Variables

Set the following in your deployment environment:

- `KEYCLOAK_URL`: Keycloak server URL (e.g., `https://auth.haven.org`)
- `KEYCLOAK_REALM`: Keycloak realm name (e.g., `haven`)
- `JWT_EXPECTED_AUDIENCE`: Expected audience claim (default: `haven-backend`)

## Keycloak Configuration

### 1. Configure Realm Roles

Ensure the following roles exist in your Keycloak realm:

- `DV_COUNSELOR`
- `CASE_MANAGER`
- `LEGAL_ADVOCATE`
- `NURSE`
- `DOCTOR`
- `ADMINISTRATOR`
- `VICTIM_SERVICE_PROVIDER`

### 2. Configure Consent Scopes

Add custom consent scopes to your Keycloak client:

- `dv_view`: Access to confidential DV notes
- `legal_view`: Access to legal case information
- `medical_view`: Access to medical records
- `hmis_export`: Permission to export HMIS data
- `research_view`: Access for research purposes
- `court_testimony`: Access for court testimony

### 3. Client Configuration

Update your `haven-backend` client in Keycloak:

1. **Access Type**: `confidential`
2. **Valid Redirect URIs**: Add your application URIs
3. **Client Scopes**: Include the consent scopes created above
4. **Mappers**: Add token mappers for:
   - Realm roles → `realm_access.roles`
   - Client roles → `resource_access.haven-backend.roles`
   - Consent scopes → `consent_scopes` (custom claim)

### 4. Create Consent Scope Mapper

Add a Protocol Mapper to include consent scopes in JWT:

```
Name: consent-scopes-mapper
Mapper Type: User Session Note
User Session Note: consent_scopes
Token Claim Name: consent_scopes
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
Add to userinfo: ON
```

## Rollout Steps

### Phase 1: Infrastructure Deployment (Week 1)

1. **Deploy Dependencies**
   ```bash
   ./gradlew :backend:shared-kernel:build
   ./gradlew :backend:modules:client-profile:build
   ```

2. **Update Database** (if using cache persistence)
   - No database migrations required for in-memory cache
   - For persistent cache, configure Redis/Hazelcast

3. **Deploy to Staging**
   - Deploy updated JAR files
   - Verify Keycloak connectivity
   - Test JWKS endpoint accessibility

### Phase 2: Keycloak Configuration (Week 1-2)

1. **Configure Keycloak Realm**
   - Create/update roles
   - Add consent scope mappers
   - Test token generation with `consent_scopes` claim

2. **Validate Token Structure**
   ```bash
   # Decode a sample token to verify claims
   echo "YOUR_TOKEN" | cut -d. -f2 | base64 -d | jq
   ```

   Verify presence of:
   - `realm_access.roles` or `resource_access.haven-backend.roles`
   - `consent_scopes`
   - `iss`, `aud`, `exp`, `sub`

### Phase 3: Gradual Migration (Week 2-3)

1. **Update Controllers to Use New Method**

   **Before:**
   ```java
   PIIAccessContext context = buildAccessContext(user);
   ClientData redacted = redactionService.applyRedaction(data, context, clientId);
   ```

   **After:**
   ```java
   String token = extractTokenFromRequest(request);
   ClientData redacted = redactionService.applyRedactionWithToken(data, token, clientId);
   ```

2. **Rollout Order**
   - Start with non-critical endpoints
   - Monitor telemetry for failures
   - Roll back if failure rate > 5%

### Phase 4: Testing & Validation (Week 3-4)

1. **Run Integration Tests**
   ```bash
   ./gradlew :backend:modules:client-profile:test --tests "*PIIRedactionServiceIntegrationTest*"
   ./gradlew :backend:shared-kernel:test --tests "*KeycloakTokenVerificationServiceTest*"
   ```

2. **Manual Testing Scenarios**
   - DV advocate with `dv_view` scope sees unredacted DV notes
   - CE intake worker without consent sees redacted data
   - Compliance auditor sees hashed identifiers only
   - External partner receives maximum redaction
   - Role downgrade removes access immediately
   - Consent withdrawal triggers redaction

3. **Monitor Telemetry**
   ```bash
   # Check token verification metrics
   curl http://localhost:8080/actuator/metrics/token.verification.success
   curl http://localhost:8080/actuator/metrics/token.verification.failure
   ```

### Phase 5: Production Rollout (Week 4-5)

1. **Enable Feature Flag**
   ```properties
   haven.security.token-based-redaction.enabled=true
   ```

2. **Blue-Green Deployment**
   - Deploy to 10% of traffic
   - Monitor for 24 hours
   - Increase to 50% if stable
   - Full rollout after 48 hours

3. **Monitor Key Metrics**
   - Token verification success rate (target: >99%)
   - Token verification latency (target: <50ms)
   - Cache hit rate (target: >80%)
   - 401/403 error rate

## Cache Invalidation Strategy

### When to Invalidate Caches

1. **User Role Change**
   ```java
   // After updating user roles in Keycloak
   redactionService.invalidatePermissionCache(userId);
   ```

2. **Consent Withdrawal**
   ```java
   // When client revokes consent
   redactionService.invalidatePermissionCache(userId);
   ```

3. **Policy Changes**
   ```java
   // When redaction policies are updated
   redactionService.invalidateAllPermissionCaches();
   ```

### Automated Cache Invalidation

Configure event listeners to automatically invalidate caches:

```java
@EventListener
public void onConsentRevoked(ConsentRevokedEvent event) {
    redactionService.invalidatePermissionCache(event.getUserId());
}

@EventListener
public void onRoleChanged(UserRoleChangedEvent event) {
    redactionService.invalidatePermissionCache(event.getUserId());
}
```

## Troubleshooting

### Token Verification Failures

**Symptom**: High `token.verification.failure` metrics

**Diagnosis**:
```bash
# Check failure reasons
curl http://localhost:8080/actuator/metrics/token.verification.failure?tag=reason:expired
curl http://localhost:8080/actuator/metrics/token.verification.failure?tag=reason:signature_invalid
```

**Solutions**:
- `expired`: Check clock skew between services
- `signature_invalid`: Verify JWKS endpoint accessibility
- `invalid_issuer`: Verify `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- `invalid_audience`: Verify `haven.security.jwt.expected-audience`

### Excessive Redaction

**Symptom**: Users with proper roles see redacted data

**Diagnosis**:
1. Verify token contains expected roles:
   ```bash
   echo $TOKEN | cut -d. -f2 | base64 -d | jq '.realm_access.roles'
   ```

2. Verify consent scopes present:
   ```bash
   echo $TOKEN | cut -d. -f2 | base64 -d | jq '.consent_scopes'
   ```

**Solutions**:
- Add missing consent scope mapper in Keycloak
- Grant user appropriate consent scopes
- Verify role name matches `UserRole` enum exactly

### Cache Staleness

**Symptom**: Users retain old permissions after role change

**Solutions**:
1. Reduce cache TTL: `spring.cache.caffeine.spec=expireAfterWrite=300s`
2. Manually invalidate cache for affected users
3. Implement real-time cache invalidation via Keycloak events

## Performance Optimization

### JWKS Caching

JWKS keys are cached automatically by Spring Security's `JwtDecoder`. Configure cache TTL:

```properties
haven.security.jwt.jwks-cache-ttl=3600  # 1 hour
```

### Permission Cache Tuning

Adjust Caffeine cache settings based on load:

```properties
# High-traffic system
spring.cache.caffeine.spec=maximumSize=10000,expireAfterWrite=300s

# Low-traffic system
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=600s
```

## Security Considerations

### Token Storage

- **Never** log full JWT tokens
- Store tokens in HTTP-only cookies or secure storage
- Implement token refresh mechanism

### Audit Logging

All token verification failures are automatically logged. Monitor for:
- Repeated failures from same IP (potential attack)
- Signature validation failures (tampering attempts)
- Expired token usage (client misconfiguration)

### Compliance

This implementation supports:
- **HIPAA**: Minimum necessary principle via role-based redaction
- **VAWA**: DV counselor-specific access controls
- **42 CFR Part 2**: Consent-based disclosure

## Rollback Plan

If issues arise during rollout:

1. **Immediate Rollback** (within 5 minutes)
   ```properties
   # Disable token-based redaction
   haven.security.token-based-redaction.enabled=false
   ```

2. **Code Rollback** (within 30 minutes)
   - Revert to previous deployment
   - Controllers automatically fall back to `applyRedaction()` method

3. **Communication**
   - Notify users of temporary access issues
   - Document root cause for post-mortem

## Post-Rollout Monitoring

### Week 1-2

- Monitor telemetry daily
- Review error logs for auth failures
- Collect user feedback on access issues

### Week 3-4

- Performance baseline establishment
- Cache hit rate analysis
- Identify optimization opportunities

### Ongoing

- Monthly review of redaction policies
- Quarterly security audit
- Annual compliance review

## Success Criteria

- ✅ Token verification success rate >99%
- ✅ Average verification latency <50ms
- ✅ No unauthorized data access incidents
- ✅ Cache hit rate >80%
- ✅ Zero rollbacks required
- ✅ User satisfaction with access controls

## Support Contacts

- **Technical Lead**: [Your Name]
- **Security Team**: security@haven.org
- **Keycloak Admin**: keycloak-admin@haven.org
- **On-call**: [On-call rotation]
