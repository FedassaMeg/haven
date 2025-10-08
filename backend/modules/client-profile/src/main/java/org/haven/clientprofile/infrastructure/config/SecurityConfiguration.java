package org.haven.clientprofile.infrastructure.config;

import org.haven.clientprofile.domain.ClientDomainService;
import org.haven.clientprofile.domain.ClientRepository;
import org.haven.clientprofile.domain.pii.PIIAccessRepository;
import org.haven.clientprofile.domain.pii.PIIAccessService;
import org.haven.clientprofile.domain.pii.PIIAuditRepository;
import org.haven.clientprofile.domain.pii.PIIAuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Security configuration for PII protection and encryption
 * Configures encryption keys and security policies
 */
@Configuration
@EnableAspectJAutoProxy
public class SecurityConfiguration {
    
    /**
     * Configuration for PII encryption key
     * In production, this should be loaded from secure key management service
     */
    @Bean
    public String piiEncryptionKey(@Value("${haven.security.pii.encryption.key:}") String configuredKey) {
        if (configuredKey.trim().isEmpty()) {
            // Generate a key for development/testing
            // In production, this should fail and require proper configuration
            org.haven.clientprofile.infrastructure.security.PIIEncryptionService.PIIEncryptionException ex = 
                new org.haven.clientprofile.infrastructure.security.PIIEncryptionService.PIIEncryptionException(
                    "PII encryption key not configured. Set haven.security.pii.encryption.key property."
                );
            
            // For development, we'll allow this but log a warning
            System.err.println("WARNING: " + ex.getMessage());
            System.err.println("Generating temporary key for development. DO NOT USE IN PRODUCTION.");
            
            var key = org.haven.clientprofile.infrastructure.security.PIIEncryptionService.generateKey();
            String base64Key = org.haven.clientprofile.infrastructure.security.PIIEncryptionService.keyToBase64(key);
            System.err.println("Generated key (add to config): haven.security.pii.encryption.key=" + base64Key);
            
            return base64Key;
        }
        
        return configuredKey;
    }
    
    /**
     * Configuration for audit logging
     */
    @Bean
    public SecurityAuditConfig securityAuditConfig() {
        return new SecurityAuditConfig(
            true,  // enablePIIAccessLogging
            true,  // enableSafeAtHomeAccessLogging
            true,  // enableVSPAccessLogging
            true,  // enableExportAuditLogging
            30     // auditRetentionDays
        );
    }
    
    /**
     * Security audit configuration
     */
    public record SecurityAuditConfig(
        boolean enablePIIAccessLogging,
        boolean enableSafeAtHomeAccessLogging,
        boolean enableVSPAccessLogging,
        boolean enableExportAuditLogging,
        int auditRetentionDays
    ) {}

    /**
     * Wire ClientDomainService with its dependencies.
     * This keeps the domain layer free of framework annotations.
     */
    @Bean
    public ClientDomainService clientDomainService(ClientRepository clientRepository) {
        return new ClientDomainService(clientRepository);
    }

    /**
     * Wire PIIAuditService with its dependencies.
     * This keeps the domain layer free of framework annotations.
     */
    @Bean
    public PIIAuditService piiAuditService(PIIAuditRepository auditRepository) {
        return new PIIAuditService(auditRepository);
    }

    /**
     * Wire PIIAccessService with its dependencies.
     * This keeps the domain layer free of framework annotations.
     */
    @Bean
    public PIIAccessService piiAccessService(
            PIIAccessRepository accessRepository,
            PIIAuditService auditService) {
        return new PIIAccessService(accessRepository, auditService);
    }
}