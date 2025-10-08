package org.haven.clientprofile.domain.pii;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Verifies qualified domestic violence counselors under Evidence Code § 1037.1
 * California Evidence Code Section 1037.1 - Privilege for domestic violence counselor
 */
public class QualifiedDVCounselorVerification {
    
    /**
     * Verifies if a user is a qualified DV counselor under EVID § 1037.1
     * 
     * Per Evidence Code 1037.1:
     * - Licensed professional clinical counselor
     * - Licensed clinical social worker  
     * - Licensed marriage and family therapist
     * - Other qualified professional providing confidential DV counseling
     * - Working under supervision of above professionals
     * - 40 hours of specialized DV training
     */
    public static boolean isQualifiedDVCounselor(UUID userId, List<String> userRoles, 
                                                List<UserCredential> credentials) {
        
        // Check for required professional licenses
        boolean hasQualifyingLicense = hasQualifyingProfessionalLicense(credentials);
        
        // Check for required DV-specific training
        boolean hasQualifyingTraining = hasQualifyingDVTraining(credentials);
        
        // Check for appropriate role assignment
        boolean hasQualifyingRole = userRoles.contains("DV_COUNSELOR") || 
                                   userRoles.contains("QUALIFIED_DV_COUNSELOR") ||
                                   userRoles.contains("CLINICAL_SUPERVISOR");
        
        // Must meet all three criteria for EVID 1037.1 qualification
        return hasQualifyingLicense && hasQualifyingTraining && hasQualifyingRole;
    }
    
    /**
     * Checks for qualifying professional licenses under EVID § 1037.1
     */
    private static boolean hasQualifyingProfessionalLicense(List<UserCredential> credentials) {
        for (UserCredential credential : credentials) {
            if (!credential.isActive()) continue;
            
            switch (credential.getCredentialType()) {
                case "LPCC":    // Licensed Professional Clinical Counselor
                case "LCSW":    // Licensed Clinical Social Worker
                case "LMFT":    // Licensed Marriage and Family Therapist
                case "LMHC":    // Licensed Mental Health Counselor
                case "PSY":     // Licensed Psychologist
                case "MD":      // Medical Doctor (if practicing psychiatry)
                    return true;
                    
                // Supervised practice under qualified professional
                case "SUPERVISED_PRACTICE":
                    if (credential.getSupervisorLicense() != null && 
                        isQualifyingLicense(credential.getSupervisorLicense())) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }
    
    /**
     * Checks for qualifying DV-specific training (minimum 40 hours)
     */
    private static boolean hasQualifyingDVTraining(List<UserCredential> credentials) {
        int totalDVTrainingHours = 0;
        
        for (UserCredential credential : credentials) {
            if (!credential.isActive()) continue;
            
            if (credential.getCredentialType().equals("DV_TRAINING")) {
                totalDVTrainingHours += credential.getTrainingHours();
            }
        }
        
        // EVID 1037.1 requires specialized DV counseling training
        return totalDVTrainingHours >= 40;
    }
    
    /**
     * Checks if a license type qualifies under EVID § 1037.1
     */
    private static boolean isQualifyingLicense(String licenseType) {
        return List.of("LPCC", "LCSW", "LMFT", "LMHC", "PSY").contains(licenseType);
    }
    
    /**
     * Verifies privileged counseling note access under EVID § 1037.1
     */
    public static boolean canAccessPrivilegedCounselingNotes(UUID userId, List<String> userRoles,
                                                           List<UserCredential> credentials,
                                                           UUID noteAuthorId) {
        
        // Author can always access their own notes
        if (userId.equals(noteAuthorId)) {
            return true;
        }
        
        // Must be qualified DV counselor
        if (!isQualifiedDVCounselor(userId, userRoles, credentials)) {
            return false;
        }
        
        // Additional restrictions for privileged communications
        // Clinical supervisor can access notes of supervisees
        if (userRoles.contains("CLINICAL_SUPERVISOR")) {
            return isSupervisingCounselor(userId, noteAuthorId);
        }
        
        // Same case team qualified counselors may access in emergency
        if (userRoles.contains("DV_COUNSELOR") && hasEmergencyJustification(userId)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Creates audit log entry for privileged note access
     */
    public static void logPrivilegedNoteAccess(UUID userId, UUID noteId, UUID clientId, 
                                             String accessJustification, boolean granted) {
        
        PrivilegedAccessLog logEntry = new PrivilegedAccessLog(
            userId, noteId, clientId, "EVID_1037_1_PRIVILEGED_NOTE", 
            accessJustification, granted, Instant.now()
        );
        
        // This would be persisted to a high-security audit table
        System.out.println("PRIVILEGED ACCESS LOG: " + logEntry);
    }
    
    /**
     * Checks if counselor is supervising the note author
     */
    private static boolean isSupervisingCounselor(UUID supervisorId, UUID superviseeId) {
        // This would query the supervision relationships table
        // Placeholder implementation
        return false;
    }
    
    /**
     * Checks for valid emergency justification
     */
    private static boolean hasEmergencyJustification(UUID userId) {
        // This would check for active emergency situations
        // Such as imminent safety concerns, court orders, etc.
        return false;
    }
    
    /**
     * User credential for professional verification
     */
    public static class UserCredential {
        private final String credentialType;
        private final String licenseNumber;
        private final String issuingState;
        private final LocalDate issuedDate;
        private final LocalDate expirationDate;
        private final boolean isActive;
        private final int trainingHours;
        private final String supervisorLicense;
        
        public UserCredential(String credentialType, String licenseNumber, String issuingState,
                            LocalDate issuedDate, LocalDate expirationDate, boolean isActive,
                            int trainingHours, String supervisorLicense) {
            this.credentialType = credentialType;
            this.licenseNumber = licenseNumber;
            this.issuingState = issuingState;
            this.issuedDate = issuedDate;
            this.expirationDate = expirationDate;
            this.isActive = isActive;
            this.trainingHours = trainingHours;
            this.supervisorLicense = supervisorLicense;
        }
        
        public String getCredentialType() { return credentialType; }
        public String getLicenseNumber() { return licenseNumber; }
        public String getIssuingState() { return issuingState; }
        public LocalDate getIssuedDate() { return issuedDate; }
        public LocalDate getExpirationDate() { return expirationDate; }
        public boolean isActive() { return isActive && (expirationDate == null || expirationDate.isAfter(LocalDate.now())); }
        public int getTrainingHours() { return trainingHours; }
        public String getSupervisorLicense() { return supervisorLicense; }
    }
    
    /**
     * Privileged access audit log
     */
    public static class PrivilegedAccessLog {
        private final UUID userId;
        private final UUID noteId;
        private final UUID clientId;
        private final String privilegeType;
        private final String justification;
        private final boolean granted;
        private final Instant accessTime;
        
        public PrivilegedAccessLog(UUID userId, UUID noteId, UUID clientId, String privilegeType,
                                 String justification, boolean granted, Instant accessTime) {
            this.userId = userId;
            this.noteId = noteId;
            this.clientId = clientId;
            this.privilegeType = privilegeType;
            this.justification = justification;
            this.granted = granted;
            this.accessTime = accessTime;
        }
        
        @Override
        public String toString() {
            return String.format("PrivilegedAccess[user=%s, note=%s, client=%s, privilege=%s, granted=%s, time=%s]",
                userId, noteId, clientId, privilegeType, granted, accessTime);
        }
    }
}