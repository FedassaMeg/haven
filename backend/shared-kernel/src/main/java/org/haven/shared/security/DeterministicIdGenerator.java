package org.haven.shared.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Deterministic ID Generator for PII-hardened exports
 * Generates consistent hashed identifiers from ClientIds using HMAC-SHA256
 * with a configurable salt for enhanced security while maintaining determinism.
 */
public class DeterministicIdGenerator {
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DEFAULT_SALT = "haven-hmis-personal-id-salt-2024";
    
    private final String salt;
    private final SecretKeySpec secretKey;
    
    /**
     * Create generator with default salt
     * WARNING: This should only be used for development/testing
     */
    public DeterministicIdGenerator() {
        this(DEFAULT_SALT);
    }
    
    /**
     * Create generator with custom salt
     * @param salt The salt to use for HMAC generation - should be unique per deployment
     */
    public DeterministicIdGenerator(String salt) {
        if (salt == null || salt.trim().isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be null or empty");
        }
        this.salt = salt;
        this.secretKey = new SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }
    
    /**
     * Generate deterministic hashed PersonalID from ClientId
     * Uses HMAC-SHA256 with configured salt to ensure:
     * - Same ClientId always produces same PersonalID
     * - Different deployments with different salts produce different PersonalIDs
     * - Original ClientId cannot be reverse-engineered from PersonalID
     * 
     * @param clientId The source ClientId to hash
     * @return Deterministic hashed identifier suitable for HMIS PersonalID
     */
    public String generateHashedPersonalId(UUID clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("ClientId cannot be null");
        }
        
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            
            // Hash the ClientId string representation
            byte[] hashBytes = mac.doFinal(clientId.toString().getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string and truncate to reasonable length for HMIS compatibility
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Return first 32 characters (128 bits) as deterministic PersonalID
            // This provides sufficient uniqueness while being HMIS-compatible
            return hexString.substring(0, Math.min(32, hexString.length())).toUpperCase();
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to generate hashed PersonalID", e);
        }
    }
    
    /**
     * Generate UUID-formatted deterministic PersonalID from ClientId
     * Creates a UUID-like structure from the hash for better compatibility
     * 
     * @param clientId The source ClientId to hash
     * @return Deterministic UUID-formatted identifier
     */
    public String generateUuidFormattedPersonalId(UUID clientId) {
        String hashedId = generateHashedPersonalId(clientId);
        
        // Ensure we have at least 32 characters by padding if necessary
        if (hashedId.length() < 32) {
            hashedId = String.format("%-32s", hashedId).replace(' ', '0');
        }
        
        // Format as UUID: 8-4-4-4-12
        return String.format("%s-%s-%s-%s-%s",
            hashedId.substring(0, 8),
            hashedId.substring(8, 12),
            hashedId.substring(12, 16),
            hashedId.substring(16, 20),
            hashedId.substring(20, 32)
        );
    }
    
    /**
     * Verify that a PersonalID was generated from a given ClientId
     * Useful for auditing and debugging
     * 
     * @param clientId The original ClientId
     * @param personalId The PersonalID to verify
     * @return true if the PersonalID matches what would be generated from the ClientId
     */
    public boolean verifyPersonalId(UUID clientId, String personalId) {
        if (clientId == null || personalId == null) {
            return false;
        }
        
        String expectedPersonalId = generateHashedPersonalId(clientId);
        String expectedUuidFormatted = generateUuidFormattedPersonalId(clientId);
        
        return personalId.equals(expectedPersonalId) || personalId.equals(expectedUuidFormatted);
    }
    
    /**
     * Get configuration info for this generator (excluding sensitive salt data)
     */
    public String getConfigurationInfo() {
        return String.format("DeterministicIdGenerator{algorithm=%s, saltLength=%d}", 
            HMAC_ALGORITHM, salt.length());
    }
}