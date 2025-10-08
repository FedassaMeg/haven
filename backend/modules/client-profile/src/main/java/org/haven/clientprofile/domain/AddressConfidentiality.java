package org.haven.clientprofile.domain;

import org.haven.shared.vo.Address;

public record AddressConfidentiality(
    Address trueLocation,
    Address mailingSubstituteAddress,
    boolean isConfidentialLocation,
    SafeAtHomeStatus safeAtHomeStatus
) {
    
    public enum SafeAtHomeStatus {
        NOT_ENROLLED,
        ENROLLED,
        PENDING,
        EXPIRED
    }
    
    public static AddressConfidentiality regular(Address address) {
        return new AddressConfidentiality(
            address, 
            address, 
            false, 
            SafeAtHomeStatus.NOT_ENROLLED
        );
    }
    
    public static AddressConfidentiality confidential(Address trueLocation, Address substituteAddress) {
        return new AddressConfidentiality(
            trueLocation, 
            substituteAddress, 
            true, 
            SafeAtHomeStatus.ENROLLED
        );
    }
    
    public Address getPublicAddress() {
        return isConfidentialLocation ? mailingSubstituteAddress : trueLocation;
    }
    
    public Address getTrueLocationForAuthorizedAccess() {
        return trueLocation;
    }
    
    public boolean isSafeAtHomeParticipant() {
        return safeAtHomeStatus == SafeAtHomeStatus.ENROLLED;
    }
}