package org.haven.clientprofile.infrastructure.security;

import org.haven.clientprofile.domain.AddressConfidentiality;
import org.haven.shared.vo.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Specialized encryption service for Safe at Home address protection
 * Ensures true locations are never exposed in plain text
 */
@Service
public class SafeAtHomeEncryptionService {
    
    private final PIIEncryptionService encryptionService;
    
    @Autowired
    public SafeAtHomeEncryptionService(PIIEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }
    
    /**
     * Encrypts true location for Safe at Home participants
     * Returns encrypted AddressConfidentiality with substitute address visible
     */
    public EncryptedAddressConfidentiality encryptTrueLocation(AddressConfidentiality addressConf) {
        if (addressConf == null) {
            return null;
        }
        
        // Always encrypt the true location for security
        String encryptedTrueLocation = null;
        if (addressConf.trueLocation() != null) {
            String fullTrueAddress = formatAddressForEncryption(addressConf.trueLocation());
            encryptedTrueLocation = encryptionService.encryptAddress(fullTrueAddress);
        }
        
        // Substitute address remains in plain text for normal operations
        Address substituteAddress = addressConf.mailingSubstituteAddress();
        
        return new EncryptedAddressConfidentiality(
            encryptedTrueLocation,
            substituteAddress,
            addressConf.isConfidentialLocation(),
            addressConf.safeAtHomeStatus()
        );
    }
    
    /**
     * Decrypts true location for authorized personnel only
     * Must log all access attempts for audit
     */
    public AddressConfidentiality decryptTrueLocation(EncryptedAddressConfidentiality encryptedConf,
                                                     UUID userId, String accessJustification) {
        if (encryptedConf == null) {
            return null;
        }
        
        // Log access attempt for audit
        logTrueLocationAccess(userId, encryptedConf, accessJustification);
        
        Address trueLocation = null;
        if (encryptedConf.encryptedTrueLocation() != null) {
            String decryptedAddress = encryptionService.decryptAddress(encryptedConf.encryptedTrueLocation());
            trueLocation = parseAddressFromDecryption(decryptedAddress);
        }
        
        return new AddressConfidentiality(
            trueLocation,
            encryptedConf.mailingSubstituteAddress(),
            encryptedConf.isConfidentialLocation(),
            encryptedConf.safeAtHomeStatus()
        );
    }
    
    /**
     * Gets public-facing address for display/sharing
     * Never exposes true location
     */
    public Address getPublicAddress(EncryptedAddressConfidentiality encryptedConf) {
        if (encryptedConf == null) {
            return null;
        }
        
        // Always return substitute address for public use
        return encryptedConf.mailingSubstituteAddress();
    }
    
    /**
     * Creates redacted address for reports/exports
     * Removes all location-identifying information
     */
    public Address createRedactedAddress(EncryptedAddressConfidentiality encryptedConf) {
        if (encryptedConf == null || !encryptedConf.isConfidentialLocation()) {
            return getPublicAddress(encryptedConf);
        }
        
        // For confidential locations, return minimal geographic info
        Address substitute = encryptedConf.mailingSubstituteAddress();
        if (substitute != null) {
            return new Address(
                "[ADDRESS PROTECTED]",
                null,
                substitute.city(),
                substitute.state(),
                substitute.postalCode() != null ? substitute.postalCode().substring(0, 3) + "XX" : null,
                substitute.country(), substitute.type(), substitute.use()
            );
        }
        
        return new Address("[PROTECTED LOCATION]", null, "[PROTECTED]", "[PROTECTED]", "XXXXX", "US", Address.AddressType.PHYSICAL, Address.AddressUse.HOME);
    }
    
    /**
     * Validates that an address is a legitimate substitute address
     * Must not be the same as true location
     */
    public boolean isValidSubstituteAddress(Address substituteAddress, Address trueLocation) {
        if (substituteAddress == null || trueLocation == null) {
            return false;
        }
        
        // Substitute address must be different from true location
        if (addressesMatch(substituteAddress, trueLocation)) {
            return false;
        }
        
        // Substitute address should be in a different geographic area
        if (substituteAddress.postalCode() != null && trueLocation.postalCode() != null) {
            String subZip3 = substituteAddress.postalCode().substring(0, Math.min(3, substituteAddress.postalCode().length()));
            String trueZip3 = trueLocation.postalCode().substring(0, Math.min(3, trueLocation.postalCode().length()));
            
            // Same ZIP+3 area is not sufficient protection
            if (subZip3.equals(trueZip3)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Formats address for encryption (full address string)
     */
    private String formatAddressForEncryption(Address address) {
        if (address == null) return null;
        
        StringBuilder formatted = new StringBuilder();
        if (address.line1() != null) formatted.append(address.line1());
        if (address.line2() != null) formatted.append("|").append(address.line2());
        if (address.city() != null) formatted.append("|").append(address.city());
        if (address.state() != null) formatted.append("|").append(address.state());
        if (address.postalCode() != null) formatted.append("|").append(address.postalCode());
        
        return formatted.toString();
    }
    
    /**
     * Parses address from decrypted string
     */
    private Address parseAddressFromDecryption(String decryptedAddress) {
        if (decryptedAddress == null) return null;
        
        String[] parts = decryptedAddress.split("\\|");
        if (parts.length < 1) return null;
        
        return new Address(
            parts.length > 0 ? parts[0] : null,
            parts.length > 1 ? parts[1] : null,
            parts.length > 2 ? parts[2] : null,
            parts.length > 3 ? parts[3] : null,
            parts.length > 4 ? parts[4] : null,
            "US", Address.AddressType.PHYSICAL, Address.AddressUse.HOME
        );
    }
    
    /**
     * Checks if two addresses are the same
     */
    private boolean addressesMatch(Address addr1, Address addr2) {
        if (addr1 == null || addr2 == null) return false;
        
        return equals(addr1.line1(), addr2.line1()) &&
               equals(addr1.line2(), addr2.line2()) &&
               equals(addr1.city(), addr2.city()) &&
               equals(addr1.state(), addr2.state()) &&
               equals(addr1.postalCode(), addr2.postalCode());
    }
    
    private boolean equals(String s1, String s2) {
        return (s1 == null && s2 == null) || (s1 != null && s1.equals(s2));
    }
    
    /**
     * Logs access to true location for audit trail
     */
    private void logTrueLocationAccess(UUID userId, EncryptedAddressConfidentiality encryptedConf, 
                                     String justification) {
        // This would integrate with the PIIAuditService
        System.out.println(String.format(
            "SAFE_AT_HOME_ACCESS: User=%s, Justification=%s, Time=%s",
            userId, justification, java.time.Instant.now()
        ));
    }
    
    /**
     * Encrypted version of AddressConfidentiality where true location is always encrypted
     */
    public record EncryptedAddressConfidentiality(
        String encryptedTrueLocation,          // Always encrypted
        Address mailingSubstituteAddress,       // Plain text for normal operations
        boolean isConfidentialLocation,
        AddressConfidentiality.SafeAtHomeStatus safeAtHomeStatus
    ) {
        
        /**
         * Gets the public-facing address (never the true location)
         */
        public Address getPublicAddress() {
            return mailingSubstituteAddress;
        }
        
        /**
         * Check if this is a Safe at Home participant
         */
        public boolean isSafeAtHomeParticipant() {
            return safeAtHomeStatus == AddressConfidentiality.SafeAtHomeStatus.ENROLLED;
        }
        
        /**
         * Check if true location is encrypted
         */
        public boolean hasTrueLocationEncrypted() {
            return encryptedTrueLocation != null && !encryptedTrueLocation.trim().isEmpty();
        }
    }
}