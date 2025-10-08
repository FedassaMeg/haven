package org.haven.clientprofile.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryption service for PII data protection
 * Implements NIST-approved encryption standards for Safe at Home compliance
 */
@Service
public class PIIEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom;
    
    public PIIEncryptionService(@Value("${haven.security.pii.encryption.key:}") String base64Key) {
        this.secureRandom = new SecureRandom();
        
        if (base64Key != null && !base64Key.trim().isEmpty()) {
            // Use provided key from configuration
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            this.encryptionKey = new SecretKeySpec(keyBytes, ALGORITHM);
        } else {
            // Generate new key for development (should be configured in production)
            this.encryptionKey = generateKey();
        }
    }
    
    /**
     * Encrypts PII data using AES-GCM
     * @param plaintext The data to encrypt
     * @return Base64-encoded encrypted data with IV prepended
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return plaintext;
        }
        
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);
            
            // Encrypt data
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            throw new PIIEncryptionException("Failed to encrypt PII data", e);
        }
    }
    
    /**
     * Decrypts PII data using AES-GCM
     * @param encryptedData Base64-encoded encrypted data with IV prepended
     * @return Decrypted plaintext
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            return encryptedData;
        }
        
        try {
            // Decode from Base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedData);
            
            if (encryptedWithIv.length < GCM_IV_LENGTH) {
                throw new PIIEncryptionException("Invalid encrypted data format");
            }
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);
            
            // Decrypt data
            byte[] decryptedData = cipher.doFinal(encrypted);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new PIIEncryptionException("Failed to decrypt PII data", e);
        }
    }
    
    /**
     * Encrypts address data for Safe at Home compliance
     * @param address Full address string
     * @return Encrypted address
     */
    public String encryptAddress(String address) {
        if (address == null) return null;
        return encrypt(address);
    }
    
    /**
     * Decrypts address data for authorized access
     * @param encryptedAddress Encrypted address string
     * @return Decrypted address
     */
    public String decryptAddress(String encryptedAddress) {
        if (encryptedAddress == null) return null;
        return decrypt(encryptedAddress);
    }
    
    /**
     * Encrypts SSN with additional validation
     * @param ssn Social Security Number
     * @return Encrypted SSN
     */
    public String encryptSSN(String ssn) {
        if (ssn == null) return null;
        
        // Validate SSN format (basic check)
        String cleanSSN = ssn.replaceAll("[^0-9]", "");
        if (cleanSSN.length() != 9) {
            throw new IllegalArgumentException("Invalid SSN format");
        }
        
        return encrypt(cleanSSN);
    }
    
    /**
     * Decrypts SSN for authorized access
     * @param encryptedSSN Encrypted SSN
     * @return Decrypted SSN
     */
    public String decryptSSN(String encryptedSSN) {
        if (encryptedSSN == null) return null;
        return decrypt(encryptedSSN);
    }
    
    /**
     * Creates a masked version of encrypted data for display purposes
     * @param encryptedData Encrypted data
     * @param visibleChars Number of characters to show at end
     * @return Masked string like "***-**-1234"
     */
    public String createMaskedDisplay(String encryptedData, int visibleChars) {
        if (encryptedData == null) return null;
        
        try {
            String decrypted = decrypt(encryptedData);
            if (decrypted.length() <= visibleChars) {
                return "*".repeat(decrypted.length());
            }
            
            String visible = decrypted.substring(decrypted.length() - visibleChars);
            String masked = "*".repeat(decrypted.length() - visibleChars);
            return masked + visible;
            
        } catch (Exception e) {
            return "***ENCRYPTED***";
        }
    }
    
    /**
     * Generates a new AES-256 key
     * @return Generated secret key
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256); // AES-256
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new PIIEncryptionException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * Converts a SecretKey to Base64 string for configuration
     * @param key Secret key
     * @return Base64-encoded key
     */
    public static String keyToBase64(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Exception for encryption/decryption errors
     */
    public static class PIIEncryptionException extends RuntimeException {
        public PIIEncryptionException(String message) {
            super(message);
        }
        
        public PIIEncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}