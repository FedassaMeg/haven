package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.*;
import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * HMIS-compliant Health Insurance Record
 * Represents a single data collection event for health insurance per HMIS FY2024 standards
 * Supports multiple records per enrollment for lifecycle events (start, update, annual, exit)
 */
public class HealthInsuranceRecord {
    
    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate informationDate;
    private InformationDate recordType;
    
    // Overall insurance status (4.04.1)
    private CoveredByHealthInsurance coveredByHealthInsurance;
    
    // Individual insurance sources (4.04.2-4.04.A) - Boolean flags
    private boolean medicaid;
    private boolean medicare;
    private boolean schip; // State Children's Health Insurance Program
    private boolean vhaMedicalServices; // VA Medical Services
    private boolean employerProvided;
    private boolean cobra; // Consolidated Omnibus Budget Reconciliation Act
    private boolean privatePay; // Purchased directly/Private payment
    private boolean stateAdultHealthInsurance;
    private boolean indianHealthService;
    private boolean otherInsurance;
    private String otherInsuranceSpecify; // Free text field for "Other"
    
    // HOPWA-specific field (only for HOPWA programs when no insurance)
    private HopwaNoInsuranceReason hopwaNoInsuranceReason;
    
    // Audit fields
    private String collectedBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public HealthInsuranceRecord() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Create a new health insurance record at project start
     */
    public static HealthInsuranceRecord createAtProjectStart(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate entryDate,
            CoveredByHealthInsurance coveredByHealthInsurance,
            String collectedBy) {
        
        HealthInsuranceRecord record = new HealthInsuranceRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = entryDate;
        record.recordType = InformationDate.START_OF_PROJECT;
        record.coveredByHealthInsurance = coveredByHealthInsurance;
        record.collectedBy = collectedBy;
        
        // Initialize all sources as false
        record.initializeSourcesAsFalse();
        
        return record;
    }
    
    /**
     * Create an update record due to change in circumstances
     */
    public static HealthInsuranceRecord createUpdate(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate changeDate,
            CoveredByHealthInsurance coveredByHealthInsurance,
            String collectedBy) {
        
        HealthInsuranceRecord record = new HealthInsuranceRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = changeDate;
        record.recordType = InformationDate.UPDATE;
        record.coveredByHealthInsurance = coveredByHealthInsurance;
        record.collectedBy = collectedBy;
        
        record.initializeSourcesAsFalse();
        
        return record;
    }
    
    /**
     * Create annual assessment record
     */
    public static HealthInsuranceRecord createAnnualAssessment(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate assessmentDate,
            CoveredByHealthInsurance coveredByHealthInsurance,
            String collectedBy) {
        
        HealthInsuranceRecord record = new HealthInsuranceRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = assessmentDate;
        record.recordType = InformationDate.ANNUAL_ASSESSMENT;
        record.coveredByHealthInsurance = coveredByHealthInsurance;
        record.collectedBy = collectedBy;
        
        record.initializeSourcesAsFalse();
        
        return record;
    }
    
    /**
     * Create exit record
     */
    public static HealthInsuranceRecord createAtProjectExit(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate exitDate,
            CoveredByHealthInsurance coveredByHealthInsurance,
            String collectedBy) {
        
        HealthInsuranceRecord record = new HealthInsuranceRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = exitDate;
        record.recordType = InformationDate.EXIT;
        record.coveredByHealthInsurance = coveredByHealthInsurance;
        record.collectedBy = collectedBy;
        
        record.initializeSourcesAsFalse();
        
        return record;
    }
    
    /**
     * Create record when minor turns 18
     */
    public static HealthInsuranceRecord createMinorTurning18(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate birthdayDate,
            CoveredByHealthInsurance coveredByHealthInsurance,
            String collectedBy) {
        
        HealthInsuranceRecord record = new HealthInsuranceRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = birthdayDate;
        record.recordType = InformationDate.MINOR_TURNING_18;
        record.coveredByHealthInsurance = coveredByHealthInsurance;
        record.collectedBy = collectedBy;
        
        record.initializeSourcesAsFalse();
        
        return record;
    }
    
    private void initializeSourcesAsFalse() {
        this.medicaid = false;
        this.medicare = false;
        this.schip = false;
        this.vhaMedicalServices = false;
        this.employerProvided = false;
        this.cobra = false;
        this.privatePay = false;
        this.stateAdultHealthInsurance = false;
        this.indianHealthService = false;
        this.otherInsurance = false;
    }
    
    /**
     * Update individual insurance source
     */
    public void updateInsuranceSource(HealthInsurance source, boolean hasSource) {
        this.updatedAt = Instant.now();
        
        switch (source) {
            case MEDICAID -> this.medicaid = hasSource;
            case MEDICARE -> this.medicare = hasSource;
            case SCHIP -> this.schip = hasSource;
            case VA_MEDICAL_SERVICES -> this.vhaMedicalServices = hasSource;
            case EMPLOYER_PROVIDED -> this.employerProvided = hasSource;
            case COBRA -> this.cobra = hasSource;
            case PRIVATE_PAY, HEALTH_INSURANCE_PURCHASED_DIRECTLY -> this.privatePay = hasSource;
            case STATE_ADULT_HEALTH_INSURANCE -> this.stateAdultHealthInsurance = hasSource;
            case INDIAN_HEALTH_SERVICE -> this.indianHealthService = hasSource;
            case OTHER_INSURANCE -> this.otherInsurance = hasSource;
            default -> throw new IllegalArgumentException("Unknown insurance source: " + source);
        }
    }
    
    /**
     * Update other insurance source with specify text
     */
    public void updateOtherInsuranceSource(boolean hasSource, String specify) {
        this.otherInsurance = hasSource;
        this.otherInsuranceSpecify = specify;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update HOPWA no insurance reason
     */
    public void updateHopwaNoInsuranceReason(HopwaNoInsuranceReason reason) {
        this.hopwaNoInsuranceReason = reason;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if record meets HMIS data quality standards
     */
    public boolean meetsDataQuality() {
        // Coverage status validation
        if (coveredByHealthInsurance == null || !coveredByHealthInsurance.isKnownResponse()) {
            return false;
        }
        
        // If has coverage, at least one source should be identified
        if (coveredByHealthInsurance.hasCoverage()) {
            if (!hasAtLeastOneInsuranceSource()) {
                return false;
            }
        }
        
        // If no coverage, no sources should be identified
        if (coveredByHealthInsurance.noCoverage()) {
            if (hasAnyInsuranceSource()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if record meets HOPWA-specific requirements
     * For HOPWA programs, if no insurance, reason must be provided
     */
    public boolean meetsHopwaRequirements(boolean isHopwaProgram) {
        if (!isHopwaProgram) {
            return true; // HOPWA rules don't apply
        }
        
        // If no coverage and no sources, HOPWA reason is required
        if (coveredByHealthInsurance != null && 
            coveredByHealthInsurance.noCoverage() && 
            !hasAnyInsuranceSource()) {
            return hopwaNoInsuranceReason != null && hopwaNoInsuranceReason.isKnownResponse();
        }
        
        return true;
    }
    
    private boolean hasAtLeastOneInsuranceSource() {
        return medicaid || medicare || schip || vhaMedicalServices || 
               employerProvided || cobra || privatePay || stateAdultHealthInsurance ||
               indianHealthService || otherInsurance;
    }
    
    private boolean hasAnyInsuranceSource() {
        return hasAtLeastOneInsuranceSource();
    }
    
    /**
     * Check if client has government insurance
     */
    public boolean hasGovernmentInsurance() {
        return medicaid || medicare || schip || vhaMedicalServices || 
               stateAdultHealthInsurance || indianHealthService;
    }
    
    /**
     * Check if client has private insurance
     */
    public boolean hasPrivateInsurance() {
        return employerProvided || cobra || privatePay || otherInsurance;
    }
    
    // Getters
    public UUID getRecordId() { return recordId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDate getInformationDate() { return informationDate; }
    public InformationDate getRecordType() { return recordType; }
    public CoveredByHealthInsurance getCoveredByHealthInsurance() { return coveredByHealthInsurance; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    // Insurance source getters
    public boolean isMedicaid() { return medicaid; }
    public boolean isMedicare() { return medicare; }
    public boolean isSchip() { return schip; }
    public boolean isVhaMedicalServices() { return vhaMedicalServices; }
    public boolean isEmployerProvided() { return employerProvided; }
    public boolean isCobra() { return cobra; }
    public boolean isPrivatePay() { return privatePay; }
    public boolean isStateAdultHealthInsurance() { return stateAdultHealthInsurance; }
    public boolean isIndianHealthService() { return indianHealthService; }
    public boolean isOtherInsurance() { return otherInsurance; }
    public String getOtherInsuranceSpecify() { return otherInsuranceSpecify; }
    public HopwaNoInsuranceReason getHopwaNoInsuranceReason() { return hopwaNoInsuranceReason; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthInsuranceRecord that = (HealthInsuranceRecord) o;
        return Objects.equals(recordId, that.recordId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}