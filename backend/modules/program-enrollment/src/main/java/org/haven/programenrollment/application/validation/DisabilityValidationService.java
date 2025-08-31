package org.haven.programenrollment.application.validation;

import org.haven.programenrollment.domain.*;
import org.haven.shared.vo.hmis.DisabilityKind;
import org.haven.shared.vo.hmis.DataCollectionStage;
import org.haven.shared.validation.BusinessRuleValidator;
import org.haven.shared.validation.BusinessRuleValidator.*;
import org.haven.shared.vo.hmis.HmisFivePoint;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Validation service for HMIS Disability data collection
 * Enforces HMIS FY2024 business rules and data quality standards
 */
@Service
public class DisabilityValidationService {
    
    private final BusinessRuleValidator validator;
    
    public DisabilityValidationService(BusinessRuleValidator validator) {
        this.validator = validator;
    }
    
    /**
     * Validate disability record creation
     */
    public ValidationResult validateDisabilityRecordCreation(
            DisabilityRecordCreationRequest request) {
        
        return validator.validate(request,
            // Basic field validation
            BusinessRuleValidator.rule(
                req -> ((DisabilityRecordCreationRequest) req).disabilityKind() != null,
                "Disability kind is required"
            ),
            BusinessRuleValidator.rule(
                req -> ((DisabilityRecordCreationRequest) req).hasDisability() != null,
                "Has disability response is required"
            ),
            BusinessRuleValidator.rule(
                req -> ((DisabilityRecordCreationRequest) req).collectedBy() != null && 
                       !((DisabilityRecordCreationRequest) req).collectedBy().trim().isEmpty(),
                "Collected by is required and cannot be empty"
            ),
            BusinessRuleValidator.rule(
                req -> ((DisabilityRecordCreationRequest) req).informationDate() != null,
                "Information date is required"
            ),
            
            // Date validation
            BusinessRuleValidator.rule(
                req -> {
                    LocalDate infoDate = ((DisabilityRecordCreationRequest) req).informationDate();
                    return infoDate == null || !infoDate.isAfter(LocalDate.now());
                },
                "Information date cannot be in the future"
            ),
            BusinessRuleValidator.rule(
                req -> {
                    LocalDate infoDate = ((DisabilityRecordCreationRequest) req).informationDate();
                    LocalDate entryDate = ((DisabilityRecordCreationRequest) req).enrollmentEntryDate();
                    return infoDate == null || entryDate == null || !infoDate.isBefore(entryDate);
                },
                "Information date cannot be before enrollment entry date"
            ),
            
            // HMIS business rules
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint response = ((DisabilityRecordCreationRequest) req).hasDisability();
                    return response == null || isValidHmisFivePointResponse(response);
                },
                "Invalid HMIS five-point response for disability"
            ),
            
            // Stage-specific validation
            BusinessRuleValidator.rule(
                req -> {
                    DataCollectionStage stage = ((DisabilityRecordCreationRequest) req).stage();
                    LocalDate infoDate = ((DisabilityRecordCreationRequest) req).informationDate();
                    LocalDate entryDate = ((DisabilityRecordCreationRequest) req).enrollmentEntryDate();
                    LocalDate exitDate = ((DisabilityRecordCreationRequest) req).enrollmentExitDate();
                    
                    if (stage == DataCollectionStage.PROJECT_START) {
                        return infoDate == null || entryDate == null || infoDate.equals(entryDate);
                    } else if (stage == DataCollectionStage.PROJECT_EXIT) {
                        return infoDate == null || exitDate == null || infoDate.equals(exitDate);
                    }
                    return true;
                },
                "Information date must match enrollment entry/exit date for PROJECT_START/PROJECT_EXIT records"
            )
        );
    }
    
    /**
     * Validate disability record update
     */
    public ValidationResult validateDisabilityRecordUpdate(
            DisabilityRecordUpdateRequest request) {
        
        return validator.validate(request,
            // Basic validation
            BusinessRuleValidator.rule(
                req -> ((DisabilityRecordUpdateRequest) req).recordId() != null,
                "Record ID is required for updates"
            ),
            BusinessRuleValidator.rule(
                req -> ((DisabilityRecordUpdateRequest) req).changeDate() != null,
                "Change date is required for update records"
            ),
            BusinessRuleValidator.rule(
                req -> ((DisabilityRecordUpdateRequest) req).hasDisability() != null,
                "Has disability response is required"
            ),
            
            // Change date validation
            BusinessRuleValidator.rule(
                req -> {
                    LocalDate changeDate = ((DisabilityRecordUpdateRequest) req).changeDate();
                    LocalDate originalDate = ((DisabilityRecordUpdateRequest) req).originalInformationDate();
                    return changeDate == null || originalDate == null || changeDate.isAfter(originalDate);
                },
                "Change date must be after original information date"
            ),
            BusinessRuleValidator.rule(
                req -> {
                    LocalDate changeDate = ((DisabilityRecordUpdateRequest) req).changeDate();
                    return changeDate == null || !changeDate.isAfter(LocalDate.now());
                },
                "Change date cannot be in the future"
            ),
            
            // Update logic validation
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint newResponse = ((DisabilityRecordUpdateRequest) req).hasDisability();
                    HmisFivePoint originalResponse = ((DisabilityRecordUpdateRequest) req).originalHasDisability();
                    return newResponse == null || originalResponse == null || !newResponse.equals(originalResponse);
                },
                "Update record must have different disability response than original"
            )
        );
    }
    
    /**
     * Validate disability correction
     */
    public ValidationResult validateDisabilityCorrection(
            DisabilityCorrectionRequest request) {
        
        return validator.validate(request,
            // Basic validation
            BusinessRuleValidator.rule(
                req -> ((DisabilityCorrectionRequest) req).originalRecordId() != null,
                "Original record ID is required for corrections"
            ),
            BusinessRuleValidator.rule(
                req -> ((DisabilityCorrectionRequest) req).hasDisability() != null,
                "Has disability response is required"
            ),
            BusinessRuleValidator.rule(
                req -> ((DisabilityCorrectionRequest) req).correctionReason() != null &&
                       !((DisabilityCorrectionRequest) req).correctionReason().trim().isEmpty(),
                "Correction reason is required and cannot be empty"
            ),
            
            // Correction logic validation
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint newResponse = ((DisabilityCorrectionRequest) req).hasDisability();
                    HmisFivePoint originalResponse = ((DisabilityCorrectionRequest) req).originalHasDisability();
                    return newResponse == null || originalResponse == null || !newResponse.equals(originalResponse);
                },
                "Correction must have different disability response than original"
            ),
            
            // Temporal validation
            BusinessRuleValidator.rule(
                req -> {
                    LocalDate originalDate = ((DisabilityCorrectionRequest) req).originalInformationDate();
                    return originalDate == null || !originalDate.isAfter(LocalDate.now().minusDays(365));
                },
                "Cannot correct records older than 365 days without special authorization"
            )
        );
    }
    
    /**
     * Validate bulk disability creation
     */
    public ValidationResult validateBulkDisabilityCreation(
            BulkDisabilityCreationRequest request) {
        
        return validator.validate(request,
            // Basic validation
            BusinessRuleValidator.rule(
                req -> ((BulkDisabilityCreationRequest) req).enrollmentId() != null,
                "Enrollment ID is required"
            ),
            BusinessRuleValidator.rule(
                req -> ((BulkDisabilityCreationRequest) req).responses() != null &&
                       ((BulkDisabilityCreationRequest) req).responses().size() == 6,
                "Must provide responses for all 6 disability types"
            ),
            
            // All responses must be valid
            BusinessRuleValidator.rule(
                req -> {
                    var responses = ((BulkDisabilityCreationRequest) req).responses();
                    return responses == null || responses.values().stream()
                        .allMatch(this::isValidHmisFivePointResponse);
                },
                "All disability responses must be valid HMIS five-point responses"
            ),
            
            // No duplicate disability kinds
            BusinessRuleValidator.rule(
                req -> {
                    var responses = ((BulkDisabilityCreationRequest) req).responses();
                    return responses == null || responses.keySet().size() == 6;
                },
                "All 6 disability types must be present exactly once"
            ),
            
            // Data consistency validation
            BusinessRuleValidator.rule(
                req -> {
                    var responses = ((BulkDisabilityCreationRequest) req).responses();
                    if (responses == null) return true;
                    
                    // If any disability is YES, at least one other should be NO/DATA_NOT_COLLECTED
                    // to avoid data quality issues where all disabilities are marked YES
                    long yesCount = responses.values().stream()
                        .mapToLong(response -> response == HmisFivePoint.YES ? 1 : 0)
                        .sum();
                    
                    return yesCount <= 4; // Allow up to 4 disabilities to be YES simultaneously
                },
                "Data quality warning: Having more than 4 disabilities marked as YES may indicate data collection issues"
            )
        );
    }
    
    /**
     * Validate enrollment disability compliance
     */
    public ValidationResult validateDisabilityCompliance(
            DisabilityComplianceRequest request) {
        
        return validator.validate(request,
            // PROJECT_START records required
            BusinessRuleValidator.rule(
                req -> {
                    var missingStart = ((DisabilityComplianceRequest) req).missingProjectStartRecords();
                    return missingStart == null || missingStart.isEmpty();
                },
                () -> {
                    var req = (DisabilityComplianceRequest) null; // This would be properly cast in real validation
                    return "Missing PROJECT_START records for disability types: " + 
                           String.join(", ", List.of("PLACEHOLDER")); // Would show actual missing types
                }
            ),
            
            // PROJECT_EXIT records required if enrollment is exited
            BusinessRuleValidator.rule(
                req -> {
                    boolean isExited = ((DisabilityComplianceRequest) req).isEnrollmentExited();
                    var missingExit = ((DisabilityComplianceRequest) req).missingProjectExitRecords();
                    return !isExited || missingExit == null || missingExit.isEmpty();
                },
                "Missing PROJECT_EXIT disability records for exited enrollment"
            ),
            
            // Data quality validation
            BusinessRuleValidator.rule(
                req -> {
                    var dataQualityIssues = ((DisabilityComplianceRequest) req).dataQualityIssues();
                    return dataQualityIssues == null || dataQualityIssues.isEmpty();
                },
                "Disability records have data quality issues that must be resolved"
            )
        );
    }
    
    private boolean isValidHmisFivePointResponse(HmisFivePoint response) {
        return response != null && List.of(
            HmisFivePoint.YES,
            HmisFivePoint.NO,
            HmisFivePoint.CLIENT_DOESNT_KNOW,
            HmisFivePoint.CLIENT_REFUSED,
            HmisFivePoint.DATA_NOT_COLLECTED
        ).contains(response);
    }
    
    // Request DTOs for validation
    public record DisabilityRecordCreationRequest(
        DisabilityKind disabilityKind,
        HmisFivePoint hasDisability,
        String collectedBy,
        LocalDate informationDate,
        DataCollectionStage stage,
        LocalDate enrollmentEntryDate,
        LocalDate enrollmentExitDate
    ) {}
    
    public record DisabilityRecordUpdateRequest(
        UUID recordId,
        LocalDate changeDate,
        HmisFivePoint hasDisability,
        String collectedBy,
        LocalDate originalInformationDate,
        HmisFivePoint originalHasDisability
    ) {}
    
    public record DisabilityCorrectionRequest(
        UUID originalRecordId,
        HmisFivePoint hasDisability,
        String correctionReason,
        String collectedBy,
        LocalDate originalInformationDate,
        HmisFivePoint originalHasDisability
    ) {}
    
    public record BulkDisabilityCreationRequest(
        UUID enrollmentId,
        java.util.Map<DisabilityKind, HmisFivePoint> responses,
        String collectedBy,
        LocalDate informationDate
    ) {}
    
    public record DisabilityComplianceRequest(
        UUID enrollmentId,
        boolean isEnrollmentExited,
        List<DisabilityKind> missingProjectStartRecords,
        List<DisabilityKind> missingProjectExitRecords,
        List<String> dataQualityIssues
    ) {}
}