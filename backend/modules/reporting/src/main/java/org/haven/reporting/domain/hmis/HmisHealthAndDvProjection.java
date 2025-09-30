package org.haven.reporting.domain.hmis;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.ProgramSpecificDataElements;
import org.haven.programenrollment.domain.PhysicalDisabilityRecord;
import org.haven.programenrollment.domain.HealthInsuranceRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HMIS HealthAndDV.csv projection  
 * Represents health insurance and domestic violence data for HMIS CSV export per FY2024 Data Standards
 * Aligns with HMIS Data Elements 4.04 Health Insurance, 4.05-4.10 Disabilities, 4.11 Domestic Violence
 */
public record HmisHealthAndDvProjection(
    String healthAndDvId,
    String enrollmentId,
    HmisPersonalId personalId,
    LocalDate informationDate,
    
    // Health Insurance (4.04)
    DisabilityType medicaid,
    DisabilityType medicare,
    DisabilityType schip,
    DisabilityType vaMedicalServices,
    DisabilityType employerProvided,
    DisabilityType cobra,
    DisabilityType privatePayment,
    DisabilityType stateHealthInsurance,
    DisabilityType indianHealthService,
    DisabilityType otherInsurance,
    DisabilityType noInsurance,
    
    // Disability Information (4.05-4.10)
    DisabilityType physicalDisability,
    DisabilityType developmentalDisability,
    DisabilityType chronicHealthCondition,
    DisabilityType hivAids,
    DisabilityType mentalHealthDisorder,
    DisabilityType substanceUseDisorder,
    
    // Domestic Violence (4.11)
    DomesticViolence domesticViolence,
    
    LocalDate dateCreated,
    LocalDate dateUpdated,
    String userId,
    LocalDateTime dateDeleted,
    String exportId
) {

    /**
     * Create projection from real domain records (preferred method)
     * Uses actual PhysicalDisabilityRecord and HealthInsuranceRecord data
     */
    public static HmisHealthAndDvProjection fromDomainRecords(
            String enrollmentId,
            HmisPersonalId personalId,
            PhysicalDisabilityRecord physicalDisabilityRecord,
            HealthInsuranceRecord healthInsuranceRecord,
            ProgramSpecificDataElements psde, // For other disabilities and DV
            String userId,
            String exportId) {
        
        LocalDate informationDate = getLatestInformationDateFromRecords(
            physicalDisabilityRecord, healthInsuranceRecord, psde);
        
        return new HmisHealthAndDvProjection(
            generateHealthAndDvId(enrollmentId),
            enrollmentId,
            personalId,
            informationDate,
            
            // Health Insurance from HealthInsuranceRecord
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isMedicaid()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isMedicare()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isSchip()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isVhaMedicalServices()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isEmployerProvided()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isCobra()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isPrivatePay()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isStateAdultHealthInsurance()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isIndianHealthService()),
            mapFromHealthInsuranceRecord(healthInsuranceRecord, r -> r.isOtherInsurance()),
            mapNoInsuranceFromHealthRecord(healthInsuranceRecord),
            
            // Physical Disability from PhysicalDisabilityRecord
            mapFromPhysicalDisabilityRecord(physicalDisabilityRecord),
            
            // Other disabilities from PSDE (until they get their own record types)
            psde != null && psde.getDevelopmentalDisability() != null ? psde.getDevelopmentalDisability() : DisabilityType.DATA_NOT_COLLECTED,
            psde != null && psde.getChronicHealthCondition() != null ? psde.getChronicHealthCondition() : DisabilityType.DATA_NOT_COLLECTED,
            psde != null && psde.getHivAids() != null ? psde.getHivAids() : DisabilityType.DATA_NOT_COLLECTED,
            psde != null && psde.getMentalHealthDisorder() != null ? psde.getMentalHealthDisorder() : DisabilityType.DATA_NOT_COLLECTED,
            psde != null && psde.getSubstanceUseDisorder() != null ? psde.getSubstanceUseDisorder() : DisabilityType.DATA_NOT_COLLECTED,
            
            // Domestic Violence from PSDE (until it gets its own record type)
            psde != null && psde.getDomesticViolence() != null ? psde.getDomesticViolence() : DomesticViolence.DATA_NOT_COLLECTED,
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId
        );
    }
    
    /**
     * Create projection from ProgramSpecificDataElements (legacy method)
     * @deprecated Use fromDomainRecords for better data fidelity
     */
    @Deprecated
    public static HmisHealthAndDvProjection fromProgramSpecificData(
            String enrollmentId,
            HmisPersonalId personalId,
            ProgramSpecificDataElements psde,
            String userId,
            String exportId) {
        
        if (psde == null) {
            return createEmptyProjection(enrollmentId, personalId, userId, exportId);
        }
        
        return new HmisHealthAndDvProjection(
            generateHealthAndDvId(enrollmentId),
            enrollmentId,
            personalId,
            getLatestInformationDate(psde),
            
            // Health Insurance (legacy mapping)
            mapHealthInsurance(psde.getHealthInsurances(), HealthInsurance.MEDICAID),
            mapHealthInsurance(psde.getHealthInsurances(), HealthInsurance.MEDICARE),
            mapHealthInsurance(psde.getHealthInsurances(), HealthInsurance.SCHIP),
            mapHealthInsurance(psde.getHealthInsurances(), HealthInsurance.VA_MEDICAL_SERVICES),
            mapHealthInsurance(psde.getHealthInsurances(), HealthInsurance.EMPLOYER_PROVIDED),
            DisabilityType.DATA_NOT_COLLECTED, // COBRA not in legacy enum
            mapHealthInsurance(psde.getHealthInsurances(), HealthInsurance.HEALTH_INSURANCE_PURCHASED_DIRECTLY),
            DisabilityType.DATA_NOT_COLLECTED, // State health insurance not in legacy enum
            DisabilityType.DATA_NOT_COLLECTED, // Indian Health Service not in legacy enum  
            mapHealthInsurance(psde.getHealthInsurances(), HealthInsurance.OTHER_INSURANCE),
            psde.hasHealthInsurance() ? DisabilityType.NO : DisabilityType.YES,
            
            // Disability Information (legacy mapping)
            psde.getPhysicalDisability() != null ? psde.getPhysicalDisability() : DisabilityType.DATA_NOT_COLLECTED,
            psde.getDevelopmentalDisability() != null ? psde.getDevelopmentalDisability() : DisabilityType.DATA_NOT_COLLECTED,
            psde.getChronicHealthCondition() != null ? psde.getChronicHealthCondition() : DisabilityType.DATA_NOT_COLLECTED,
            psde.getHivAids() != null ? psde.getHivAids() : DisabilityType.DATA_NOT_COLLECTED,
            psde.getMentalHealthDisorder() != null ? psde.getMentalHealthDisorder() : DisabilityType.DATA_NOT_COLLECTED,
            psde.getSubstanceUseDisorder() != null ? psde.getSubstanceUseDisorder() : DisabilityType.DATA_NOT_COLLECTED,
            
            // Domestic Violence (legacy mapping)
            psde.getDomesticViolence() != null ? psde.getDomesticViolence() : DomesticViolence.DATA_NOT_COLLECTED,
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId
        );
    }
    
    private static HmisHealthAndDvProjection createEmptyProjection(
            String enrollmentId,
            HmisPersonalId personalId,
            String userId,
            String exportId) {
        
        return new HmisHealthAndDvProjection(
            generateHealthAndDvId(enrollmentId),
            enrollmentId,
            personalId,
            LocalDate.now(),
            
            // All health insurance as data not collected
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            
            // All disabilities as data not collected
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            
            // Domestic violence as data not collected
            DomesticViolence.DATA_NOT_COLLECTED,
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId
        );
    }
    
    private static DisabilityType mapHealthInsurance(List<HealthInsurance> insurances, HealthInsurance targetInsurance) {
        return insurances.contains(targetInsurance) ? DisabilityType.YES : DisabilityType.NO;
    }
    
    /**
     * Map from HealthInsuranceRecord using functional interface
     */
    private static DisabilityType mapFromHealthInsuranceRecord(
            HealthInsuranceRecord record, 
            java.util.function.Function<HealthInsuranceRecord, Boolean> mapper) {
        if (record == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }
        
        // Map HmisFivePointResponse coverage status
        if (record.getCoveredByHealthInsurance() != null) {
            switch (record.getCoveredByHealthInsurance()) {
                case YES:
                    return mapper.apply(record) ? DisabilityType.YES : DisabilityType.NO;
                case NO:
                    return DisabilityType.NO;
                case CLIENT_DOESNT_KNOW:
                    return DisabilityType.CLIENT_DOESNT_KNOW;
                case CLIENT_REFUSED:
                    return DisabilityType.CLIENT_REFUSED;
                case DATA_NOT_COLLECTED:
                default:
                    return DisabilityType.DATA_NOT_COLLECTED;
            }
        }
        
        return DisabilityType.DATA_NOT_COLLECTED;
    }
    
    /**
     * Map "no insurance" from HealthInsuranceRecord
     */
    private static DisabilityType mapNoInsuranceFromHealthRecord(HealthInsuranceRecord record) {
        if (record == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }
        
        if (record.getCoveredByHealthInsurance() != null) {
            switch (record.getCoveredByHealthInsurance()) {
                case YES:
                    return DisabilityType.NO; // Has insurance = NO to "no insurance"
                case NO:
                    return DisabilityType.YES; // No insurance = YES to "no insurance"
                case CLIENT_DOESNT_KNOW:
                    return DisabilityType.CLIENT_DOESNT_KNOW;
                case CLIENT_REFUSED:
                    return DisabilityType.CLIENT_REFUSED;
                case DATA_NOT_COLLECTED:
                default:
                    return DisabilityType.DATA_NOT_COLLECTED;
            }
        }
        
        return DisabilityType.DATA_NOT_COLLECTED;
    }
    
    /**
     * Map from PhysicalDisabilityRecord to DisabilityType for CSV export
     */
    private static DisabilityType mapFromPhysicalDisabilityRecord(PhysicalDisabilityRecord record) {
        if (record == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }
        
        HmisFivePointResponse response = record.getPhysicalDisability();
        if (response == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }
        
        // Map HmisFivePointResponse to DisabilityType
        switch (response) {
            case YES:
                return DisabilityType.YES;
            case NO:
                return DisabilityType.NO;
            case CLIENT_DOESNT_KNOW:
                return DisabilityType.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED:
                return DisabilityType.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED:
            default:
                return DisabilityType.DATA_NOT_COLLECTED;
        }
    }
    
    /**
     * Get latest information date from domain records
     */
    private static LocalDate getLatestInformationDateFromRecords(
            PhysicalDisabilityRecord physicalRecord,
            HealthInsuranceRecord healthRecord,
            ProgramSpecificDataElements psde) {
        
        LocalDate latest = null;
        
        // Check physical disability record
        if (physicalRecord != null && physicalRecord.getInformationDate() != null) {
            latest = physicalRecord.getInformationDate();
        }
        
        // Check health insurance record
        if (healthRecord != null && healthRecord.getInformationDate() != null) {
            if (latest == null || healthRecord.getInformationDate().isAfter(latest)) {
                latest = healthRecord.getInformationDate();
            }
        }
        
        // Check PSDE dates
        if (psde != null) {
            if (psde.getDisabilityInformationDate() != null && 
                (latest == null || psde.getDisabilityInformationDate().isAfter(latest))) {
                latest = psde.getDisabilityInformationDate();
            }
            
            if (psde.getDomesticViolenceInformationDate() != null && 
                (latest == null || psde.getDomesticViolenceInformationDate().isAfter(latest))) {
                latest = psde.getDomesticViolenceInformationDate();
            }
        }
        
        return latest != null ? latest : LocalDate.now();
    }
    
    private static LocalDate getLatestInformationDate(ProgramSpecificDataElements psde) {
        LocalDate latest = psde.getHealthInsuranceInformationDate();
        
        if (psde.getDisabilityInformationDate() != null && 
            (latest == null || psde.getDisabilityInformationDate().isAfter(latest))) {
            latest = psde.getDisabilityInformationDate();
        }
        
        if (psde.getDomesticViolenceInformationDate() != null && 
            (latest == null || psde.getDomesticViolenceInformationDate().isAfter(latest))) {
            latest = psde.getDomesticViolenceInformationDate();
        }
        
        return latest != null ? latest : LocalDate.now();
    }
    
    private static String generateHealthAndDvId(String enrollmentId) {
        return "HD_" + enrollmentId;
    }
    
    /**
     * Convert to CSV row format
     */
    public String toCsvRow() {
        return String.join(",",
            quote(healthAndDvId),
            quote(enrollmentId),
            quote(personalId.value()),
            quote(formatDate(informationDate)),
            String.valueOf(medicaid.getHmisValue()),
            String.valueOf(medicare.getHmisValue()),
            String.valueOf(schip.getHmisValue()),
            String.valueOf(vaMedicalServices.getHmisValue()),
            String.valueOf(employerProvided.getHmisValue()),
            String.valueOf(cobra.getHmisValue()),
            String.valueOf(privatePayment.getHmisValue()),
            String.valueOf(stateHealthInsurance.getHmisValue()),
            String.valueOf(indianHealthService.getHmisValue()),
            String.valueOf(otherInsurance.getHmisValue()),
            String.valueOf(noInsurance.getHmisValue()),
            String.valueOf(physicalDisability.getHmisValue()),
            String.valueOf(developmentalDisability.getHmisValue()),
            String.valueOf(chronicHealthCondition.getHmisValue()),
            String.valueOf(hivAids.getHmisValue()),
            String.valueOf(mentalHealthDisorder.getHmisValue()),
            String.valueOf(substanceUseDisorder.getHmisValue()),
            String.valueOf(domesticViolence.getHmisValue()),
            quote(formatDate(dateCreated)),
            quote(formatDate(dateUpdated)),
            quote(userId),
            quote(formatDateTime(dateDeleted)),
            quote(exportId)
        );
    }
    
    private String quote(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    
    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : "";
    }
    
    /**
     * Create a copy with redacted health information for privacy protection
     */
    public HmisHealthAndDvProjection withRedactedHealthInfo() {
        return new HmisHealthAndDvProjection(
            healthAndDvId,
            enrollmentId,
            personalId,
            informationDate,
            
            // Redact all health insurance information
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            
            // Redact all disability information
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            
            // Keep DV info - not health related
            domesticViolence,
            
            dateCreated,
            dateUpdated,
            userId,
            dateDeleted,
            exportId
        );
    }
    
    /**
     * Create a copy with redacted domestic violence information for privacy protection
     */
    public HmisHealthAndDvProjection withRedactedDvInfo() {
        return new HmisHealthAndDvProjection(
            healthAndDvId,
            enrollmentId,
            personalId,
            informationDate,
            
            // Keep health insurance information
            medicaid,
            medicare,
            schip,
            vaMedicalServices,
            employerProvided,
            cobra,
            privatePayment,
            stateHealthInsurance,
            indianHealthService,
            otherInsurance,
            noInsurance,
            
            // Keep disability information
            physicalDisability,
            developmentalDisability,
            chronicHealthCondition,
            hivAids,
            mentalHealthDisorder,
            substanceUseDisorder,
            
            // Redact domestic violence information
            DomesticViolence.DATA_NOT_COLLECTED,
            
            dateCreated,
            dateUpdated,
            userId,
            dateDeleted,
            exportId
        );
    }
}