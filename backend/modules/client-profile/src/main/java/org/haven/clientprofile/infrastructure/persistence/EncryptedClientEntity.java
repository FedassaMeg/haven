package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.AddressConfidentiality;
import org.haven.clientprofile.domain.Client;
import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.DataSystem;
import org.haven.clientprofile.infrastructure.security.PIIEncryptionService;
import org.haven.clientprofile.infrastructure.security.SafeAtHomeEncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.haven.shared.vo.Address;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Enhanced client entity with PII encryption support
 * Implements Safe at Home address protection and field-level encryption
 */
@Entity
@Table(name = "clients_encrypted", schema = "haven")
public class EncryptedClientEntity {
    
    @Id
    private UUID id;
    
    // Non-PII service data (unencrypted)
    @Column(name = "client_number", nullable = false, unique = true)
    private String clientNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "data_system")
    private DataSystem dataSystem;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Version
    private Long version;
    
    // Encrypted PII fields
    @Column(name = "encrypted_first_name", length = 500)
    private String encryptedFirstName;
    
    @Column(name = "encrypted_last_name", length = 500)
    private String encryptedLastName;
    
    @Column(name = "encrypted_alias_name", length = 500)
    private String encryptedAliasName;
    
    @Column(name = "encrypted_ssn", length = 500)
    private String encryptedSSN;
    
    // Safe at Home encrypted address
    @Column(name = "encrypted_true_address", length = 1000)
    private String encryptedTrueAddress;
    
    // Substitute address (unencrypted for normal operations)
    @Column(name = "substitute_address_line1", length = 255)
    private String substituteAddressLine1;
    
    @Column(name = "substitute_city", length = 100)
    private String substituteCity;
    
    @Column(name = "substitute_state", length = 50)
    private String substituteState;
    
    @Column(name = "substitute_postal_code", length = 20)
    private String substitutePostalCode;
    
    // Confidentiality flags
    @Column(name = "is_safe_at_home")
    private Boolean isSafeAtHome = false;
    
    @Column(name = "is_confidential_location")
    private Boolean isConfidentialLocation = false;
    
    @Column(name = "is_comparable_db_only")
    private Boolean isComparableDbOnly = false;
    
    // Encrypted contact information
    @Column(name = "encrypted_phone", length = 500)
    private String encryptedPhone;
    
    @Column(name = "encrypted_email", length = 500)
    private String encryptedEmail;
    
    @Column(name = "encrypted_emergency_contact", length = 1000)
    private String encryptedEmergencyContact;
    
    // Service fields (unencrypted)
    @Column(name = "case_manager_id")
    private UUID caseManagerId;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "intake_date")
    private Instant intakeDate;
    
    // Transient encryption service (injected, not persisted)
    @Transient
    private PIIEncryptionService encryptionService;
    
    @Transient
    private SafeAtHomeEncryptionService safeAtHomeService;
    
    // Constructors
    protected EncryptedClientEntity() {
        // JPA requires default constructor
    }
    
    public EncryptedClientEntity(UUID id, String clientNumber, DataSystem dataSystem, Instant createdAt) {
        this.id = id;
        this.clientNumber = clientNumber;
        this.dataSystem = dataSystem;
        this.createdAt = createdAt;
    }
    
    /**
     * Sets encryption services (called by Spring after entity creation)
     */
    @PostLoad
    @PostPersist
    @PostUpdate
    public void initializeEncryptionServices() {
        // These would be injected via Spring's @Configurable or EntityListeners
        // For now, we'll handle this in the repository layer
    }
    
    public void setEncryptionServices(PIIEncryptionService encryptionService, 
                                    SafeAtHomeEncryptionService safeAtHomeService) {
        this.encryptionService = encryptionService;
        this.safeAtHomeService = safeAtHomeService;
    }
    
    /**
     * Factory method from domain object with encryption
     */
    public static EncryptedClientEntity fromDomainWithEncryption(Client client, 
                                                               PIIEncryptionService encryptionService,
                                                               SafeAtHomeEncryptionService safeAtHomeService) {
        
        EncryptedClientEntity entity = new EncryptedClientEntity(
            client.getId().value(),
            generateClientNumber(),
            client.getDataSystem(),
            client.getCreatedAt()
        );
        
        entity.setEncryptionServices(encryptionService, safeAtHomeService);
        
        // Encrypt PII fields
        if (client.getPrimaryName() != null) {
            entity.encryptedFirstName = encryptionService.encrypt(client.getPrimaryName().getFirstName());
            entity.encryptedLastName = encryptionService.encrypt(client.getPrimaryName().getLastName());
        }
        
        if (client.getAliasName() != null) {
            entity.encryptedAliasName = encryptionService.encrypt(client.getAliasName());
        }
        
        // Handle Safe at Home address encryption
        if (client.getAddressConfidentiality() != null) {
            var addressConf = client.getAddressConfidentiality();
            entity.isConfidentialLocation = addressConf.isConfidentialLocation();
            entity.isSafeAtHome = addressConf.isSafeAtHomeParticipant();
            
            if (addressConf.trueLocation() != null && addressConf.isConfidentialLocation()) {
                // Encrypt true location for Safe at Home participants
                var encryptedConf = safeAtHomeService.encryptTrueLocation(addressConf);
                entity.encryptedTrueAddress = encryptedConf.encryptedTrueLocation();
            }
            
            // Store substitute address in plain text for normal operations
            if (addressConf.mailingSubstituteAddress() != null) {
                var addr = addressConf.mailingSubstituteAddress();
                entity.substituteAddressLine1 = addr.line1();
                entity.substituteCity = addr.city();
                entity.substituteState = addr.state();
                entity.substitutePostalCode = addr.postalCode();
            }
        }
        
        // Set data system restrictions
        entity.isComparableDbOnly = (client.getDataSystem() == DataSystem.COMPARABLE_DB);
        
        return entity;
    }
    
    /**
     * Converts to domain object with decryption (for authorized users only)
     */
    public Client toDomainWithDecryption(UUID requestingUserId, String accessJustification) {
        if (encryptionService == null || safeAtHomeService == null) {
            throw new IllegalStateException("Encryption services not initialized");
        }
        
        // Decrypt basic PII
        String firstName = encryptionService.decrypt(encryptedFirstName);
        String lastName = encryptionService.decrypt(encryptedLastName);
        
        Client client = Client.create(firstName, lastName);
        
        // Set non-PII fields
        client.setDataSystem(dataSystem);
        
        // Decrypt alias if present
        if (encryptedAliasName != null) {
            String aliasName = encryptionService.decrypt(encryptedAliasName);
            client.setAliasName(aliasName);
        }
        
        // Handle address decryption for Safe at Home
        if (isConfidentialLocation && encryptedTrueAddress != null) {
            // This requires special authorization and logging
            var encryptedConf = new SafeAtHomeEncryptionService.EncryptedAddressConfidentiality(
                encryptedTrueAddress,
                createSubstituteAddress(),
                isConfidentialLocation,
                isSafeAtHome ? AddressConfidentiality.SafeAtHomeStatus.ENROLLED : 
                              AddressConfidentiality.SafeAtHomeStatus.NOT_ENROLLED
            );
            
            // Decrypt true location (with audit logging)
            var addressConf = safeAtHomeService.decryptTrueLocation(
                encryptedConf, requestingUserId, accessJustification
            );
            
            client.updateAddressConfidentiality(addressConf);
            
        } else {
            // For non-confidential locations, use substitute address as true address
            AddressConfidentiality addressConf = AddressConfidentiality.regular(createSubstituteAddress());
            client.updateAddressConfidentiality(addressConf);
        }
        
        return client;
    }
    
    /**
     * Creates redacted view for unauthorized users (VSPs, limited access, etc.)
     */
    public Client toRedactedDomain() {
        // Return minimal client with no PII
        Client client = Client.create("[NAME REDACTED]", "[NAME REDACTED]");
        client.setDataSystem(dataSystem);
        
        // Only substitute address for location (never true address)
        if (!isConfidentialLocation) {
            AddressConfidentiality addressConf = AddressConfidentiality.regular(createSubstituteAddress());
            client.updateAddressConfidentiality(addressConf);
        } else {
            // For confidential locations, provide minimal geographic info only
            org.haven.shared.vo.Address redactedAddr = new org.haven.shared.vo.Address(
                "[PROTECTED LOCATION]", null, substituteCity, substituteState, 
                substitutePostalCode != null ? substitutePostalCode.substring(0, 3) + "XX" : null,
                "US", org.haven.shared.vo.Address.AddressType.PHYSICAL, org.haven.shared.vo.Address.AddressUse.HOME
            );
            AddressConfidentiality addressConf = AddressConfidentiality.regular(redactedAddr);
            client.updateAddressConfidentiality(addressConf);
        }
        
        return client;
    }
    
    /**
     * Gets public address (never exposes true location)
     */
    public org.haven.shared.vo.Address getPublicAddress() {
        return createSubstituteAddress();
    }
    
    /**
     * Checks if user can access encrypted PII
     */
    public boolean canAccessPII(java.util.List<String> userRoles) {
        // VSPs cannot access HMIS PII
        if (userRoles.contains("VSP") && !isComparableDbOnly) {
            return false;
        }
        
        // Check minimum role requirements
        return userRoles.contains("CASE_MANAGER") || 
               userRoles.contains("SUPERVISOR") ||
               userRoles.contains("ADMINISTRATOR");
    }
    
    private org.haven.shared.vo.Address createSubstituteAddress() {
        return new org.haven.shared.vo.Address(
            substituteAddressLine1, null, substituteCity, 
            substituteState, substitutePostalCode, "US",
            org.haven.shared.vo.Address.AddressType.PHYSICAL, org.haven.shared.vo.Address.AddressUse.HOME
        );
    }
    
    private static String generateClientNumber() {
        return "CL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getClientNumber() { return clientNumber; }
    public void setClientNumber(String clientNumber) { this.clientNumber = clientNumber; }
    
    public DataSystem getDataSystem() { return dataSystem; }
    public void setDataSystem(DataSystem dataSystem) { this.dataSystem = dataSystem; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public Boolean getIsSafeAtHome() { return isSafeAtHome; }
    public void setIsSafeAtHome(Boolean isSafeAtHome) { this.isSafeAtHome = isSafeAtHome; }
    
    public Boolean getIsConfidentialLocation() { return isConfidentialLocation; }
    public void setIsConfidentialLocation(Boolean isConfidentialLocation) { this.isConfidentialLocation = isConfidentialLocation; }
    
    public Boolean getIsComparableDbOnly() { return isComparableDbOnly; }
    public void setIsComparableDbOnly(Boolean isComparableDbOnly) { this.isComparableDbOnly = isComparableDbOnly; }
    
    // Encrypted field accessors (for debugging/admin only)
    public String getEncryptedFirstName() { return encryptedFirstName; }
    public String getEncryptedLastName() { return encryptedLastName; }
    public String getEncryptedTrueAddress() { return encryptedTrueAddress; }
}