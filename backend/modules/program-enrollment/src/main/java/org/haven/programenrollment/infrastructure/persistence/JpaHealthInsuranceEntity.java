package org.haven.programenrollment.infrastructure.persistence;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.*;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Health Insurance Records
 * Maps to health_insurance_records table with HMIS-compliant structure
 */
@Entity
@Table(name = "health_insurance_records")
public class JpaHealthInsuranceEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "information_date", nullable = false)
    private LocalDate informationDate;
    
    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;
    
    // Overall insurance status
    @Column(name = "covered_by_health_insurance", nullable = false)
    private Integer coveredByHealthInsurance;
    
    // Individual insurance sources - Boolean flags
    @Column(name = "medicaid", nullable = false)
    private Boolean medicaid = false;
    
    @Column(name = "medicare", nullable = false)
    private Boolean medicare = false;
    
    @Column(name = "schip", nullable = false)
    private Boolean schip = false;
    
    @Column(name = "vha_medical_services", nullable = false)
    private Boolean vhaMedicalServices = false;
    
    @Column(name = "employer_provided", nullable = false)
    private Boolean employerProvided = false;
    
    @Column(name = "cobra", nullable = false)
    private Boolean cobra = false;
    
    @Column(name = "private_pay", nullable = false)
    private Boolean privatePay = false;
    
    @Column(name = "state_adult_health_insurance", nullable = false)
    private Boolean stateAdultHealthInsurance = false;
    
    @Column(name = "indian_health_service", nullable = false)
    private Boolean indianHealthService = false;
    
    @Column(name = "other_insurance", nullable = false)
    private Boolean otherInsurance = false;
    
    @Column(name = "other_insurance_specify")
    private String otherInsuranceSpecify;
    
    // HOPWA-specific field
    @Enumerated(EnumType.STRING)
    @Column(name = "hopwa_no_insurance_reason")
    private String hopwaNoInsuranceReason;
    
    // Audit fields
    @Column(name = "collected_by", nullable = false)
    private String collectedBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected JpaHealthInsuranceEntity() {
        // JPA constructor
    }
    
    public JpaHealthInsuranceEntity(HealthInsuranceRecord record) {
        this.id = record.getRecordId();
        this.enrollmentId = record.getEnrollmentId().value();
        this.clientId = record.getClientId().value();
        this.informationDate = record.getInformationDate();
        this.recordType = record.getRecordType().getCode();
        this.coveredByHealthInsurance = record.getCoveredByHealthInsurance().getHmisValue();
        this.collectedBy = record.getCollectedBy();
        this.createdAt = record.getCreatedAt();
        this.updatedAt = record.getUpdatedAt();
        
        // Map individual insurance sources
        this.medicaid = record.isMedicaid();
        this.medicare = record.isMedicare();
        this.schip = record.isSchip();
        this.vhaMedicalServices = record.isVhaMedicalServices();
        this.employerProvided = record.isEmployerProvided();
        this.cobra = record.isCobra();
        this.privatePay = record.isPrivatePay();
        this.stateAdultHealthInsurance = record.isStateAdultHealthInsurance();
        this.indianHealthService = record.isIndianHealthService();
        this.otherInsurance = record.isOtherInsurance();
        this.otherInsuranceSpecify = record.getOtherInsuranceSpecify();
        
        // Map HOPWA reason
        this.hopwaNoInsuranceReason = record.getHopwaNoInsuranceReason() != null ? 
            record.getHopwaNoInsuranceReason().toDatabaseValue() : null;
    }
    
    public HealthInsuranceRecord toDomainObject() {
        return reconstituteDomainObject();
    }
    
    private HealthInsuranceRecord reconstituteDomainObject() {
        // Create appropriate factory method based on record type
        InformationDate infoDateType = InformationDate.valueOf(recordType);
        ProgramEnrollmentId enrollmentDomainId = ProgramEnrollmentId.of(enrollmentId);
        ClientId clientDomainId = new ClientId(clientId);
        CoveredByHealthInsurance coverageStatus = CoveredByHealthInsurance.fromHmisValue(coveredByHealthInsurance);
        
        HealthInsuranceRecord record;
        
        switch (infoDateType) {
            case START_OF_PROJECT -> record = HealthInsuranceRecord.createAtProjectStart(
                enrollmentDomainId, clientDomainId, informationDate, coverageStatus, collectedBy);
            case UPDATE -> record = HealthInsuranceRecord.createUpdate(
                enrollmentDomainId, clientDomainId, informationDate, coverageStatus, collectedBy);
            case ANNUAL_ASSESSMENT -> record = HealthInsuranceRecord.createAnnualAssessment(
                enrollmentDomainId, clientDomainId, informationDate, coverageStatus, collectedBy);
            case EXIT -> record = HealthInsuranceRecord.createAtProjectExit(
                enrollmentDomainId, clientDomainId, informationDate, coverageStatus, collectedBy);
            case MINOR_TURNING_18 -> record = HealthInsuranceRecord.createMinorTurning18(
                enrollmentDomainId, clientDomainId, informationDate, coverageStatus, collectedBy);
            default -> throw new IllegalArgumentException("Unknown record type: " + recordType);
        }
        
        // Update insurance sources
        updateDomainRecordWithSources(record);
        
        // Update HOPWA reason if present
        if (hopwaNoInsuranceReason != null) {
            record.updateHopwaNoInsuranceReason(
                HopwaNoInsuranceReason.fromDatabaseValue(hopwaNoInsuranceReason));
        }
        
        return record;
    }
    
    private void updateDomainRecordWithSources(HealthInsuranceRecord record) {
        if (medicaid) {
            record.updateInsuranceSource(HealthInsurance.MEDICAID, true);
        }
        if (medicare) {
            record.updateInsuranceSource(HealthInsurance.MEDICARE, true);
        }
        if (schip) {
            record.updateInsuranceSource(HealthInsurance.SCHIP, true);
        }
        if (vhaMedicalServices) {
            record.updateInsuranceSource(HealthInsurance.VA_MEDICAL_SERVICES, true);
        }
        if (employerProvided) {
            record.updateInsuranceSource(HealthInsurance.EMPLOYER_PROVIDED, true);
        }
        if (cobra) {
            record.updateInsuranceSource(HealthInsurance.COBRA, true);
        }
        if (privatePay) {
            record.updateInsuranceSource(HealthInsurance.PRIVATE_PAY, true);
        }
        if (stateAdultHealthInsurance) {
            record.updateInsuranceSource(HealthInsurance.STATE_ADULT_HEALTH_INSURANCE, true);
        }
        if (indianHealthService) {
            record.updateInsuranceSource(HealthInsurance.INDIAN_HEALTH_SERVICE, true);
        }
        if (otherInsurance) {
            record.updateOtherInsuranceSource(true, otherInsuranceSpecify);
        }
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getClientId() { return clientId; }
    public LocalDate getInformationDate() { return informationDate; }
    public String getRecordType() { return recordType; }
    public Integer getCoveredByHealthInsurance() { return coveredByHealthInsurance; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    // Insurance source getters
    public Boolean getMedicaid() { return medicaid; }
    public Boolean getMedicare() { return medicare; }
    public Boolean getSchip() { return schip; }
    public Boolean getVhaMedicalServices() { return vhaMedicalServices; }
    public Boolean getEmployerProvided() { return employerProvided; }
    public Boolean getCobra() { return cobra; }
    public Boolean getPrivatePay() { return privatePay; }
    public Boolean getStateAdultHealthInsurance() { return stateAdultHealthInsurance; }
    public Boolean getIndianHealthService() { return indianHealthService; }
    public Boolean getOtherInsurance() { return otherInsurance; }
    public String getOtherInsuranceSpecify() { return otherInsuranceSpecify; }
    public String getHopwaNoInsuranceReason() { return hopwaNoInsuranceReason; }
}