package org.haven.programenrollment.application.validation;

import org.haven.programenrollment.domain.*;
import org.haven.shared.vo.hmis.DomesticViolenceRecency;
import org.haven.shared.validation.BusinessRuleValidator;
import org.haven.shared.validation.BusinessRuleValidator.*;
import org.haven.shared.vo.hmis.HmisFivePoint;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Validation service for HMIS Domestic Violence data collection
 * Enforces HMIS FY2024 business rules with enhanced security and privacy considerations
 */
@Service
public class DvValidationService {
    
    private final BusinessRuleValidator validator;
    
    public DvValidationService(BusinessRuleValidator validator) {
        this.validator = validator;
    }
    
    /**
     * Validate DV record creation with enhanced security checks
     */
    public ValidationResult validateDvRecordCreation(
            DvRecordCreationRequest request) {
        
        return validator.validate(request,
            // Basic field validation
            BusinessRuleValidator.rule(
                req -> ((DvRecordCreationRequest) req).dvHistory() != null,
                "DV history response is required"
            ),
            BusinessRuleValidator.rule(
                req -> ((DvRecordCreationRequest) req).currentlyFleeing() != null,
                "Currently fleeing response is required"
            ),
            BusinessRuleValidator.rule(
                req -> ((DvRecordCreationRequest) req).collectedBy() != null && 
                       !((DvRecordCreationRequest) req).collectedBy().trim().isEmpty(),
                "Collected by is required and cannot be empty"
            ),
            BusinessRuleValidator.rule(
                req -> ((DvRecordCreationRequest) req).informationDate() != null,
                "Information date is required"
            ),
            
            // Date validation
            BusinessRuleValidator.rule(
                req -> {
                    LocalDate infoDate = ((DvRecordCreationRequest) req).informationDate();
                    return infoDate == null || !infoDate.isAfter(LocalDate.now());
                },
                "Information date cannot be in the future"
            ),
            BusinessRuleValidator.rule(
                req -> {
                    LocalDate infoDate = ((DvRecordCreationRequest) req).informationDate();
                    LocalDate entryDate = ((DvRecordCreationRequest) req).enrollmentEntryDate();
                    return infoDate == null || entryDate == null || !infoDate.isBefore(entryDate);
                },
                "Information date cannot be before enrollment entry date"
            ),
            
            // HMIS business rules
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint dvHistory = ((DvRecordCreationRequest) req).dvHistory();
                    return dvHistory == null || isValidHmisFivePointResponse(dvHistory);
                },
                "Invalid HMIS five-point response for DV history"
            ),
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint currentlyFleeing = ((DvRecordCreationRequest) req).currentlyFleeing();
                    return currentlyFleeing == null || isValidHmisFivePointResponse(currentlyFleeing);
                },
                "Invalid HMIS five-point response for currently fleeing"
            ),
            
            // DV-specific business rules
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint dvHistory = ((DvRecordCreationRequest) req).dvHistory();
                    DomesticViolenceRecency whenExperienced = ((DvRecordCreationRequest) req).whenExperienced();
                    
                    if (dvHistory == HmisFivePoint.YES) {
                        return whenExperienced != null;
                    } else if (dvHistory == HmisFivePoint.NO) {
                        return whenExperienced == null;
                    }
                    return true; // For other responses, whenExperienced is optional
                },
                "When DV history is YES, 'when experienced' must be specified. When NO, it must be null."
            ),
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint dvHistory = ((DvRecordCreationRequest) req).dvHistory();
                    HmisFivePoint currentlyFleeing = ((DvRecordCreationRequest) req).currentlyFleeing();
                    
                    // If currently fleeing is YES, DV history must be YES or DATA_NOT_COLLECTED
                    if (currentlyFleeing == HmisFivePoint.YES) {
                        return dvHistory == HmisFivePoint.YES || dvHistory == HmisFivePoint.DATA_NOT_COLLECTED;
                    }
                    return true;
                },
                "If currently fleeing DV, must have DV history or DV history must be DATA_NOT_COLLECTED"
            ),
            
            // Security and privacy validation
            BusinessRuleValidator.rule(
                req -> {
                    String collectedBy = ((DvRecordCreationRequest) req).collectedBy();
                    List<String> authorizedRoles = ((DvRecordCreationRequest) req).collectorAuthorizedRoles();
                    return collectedBy == null || authorizedRoles == null || 
                           authorizedRoles.contains("DV_SPECIALIST") || authorizedRoles.contains("ADMIN");
                },
                "DV data can only be collected by authorized DV specialists or administrators"
            ),
            BusinessRuleValidator.rule(
                req -> {
                    boolean hasConsent = ((DvRecordCreationRequest) req).hasClientConsent();
                    HmisFivePoint dvHistory = ((DvRecordCreationRequest) req).dvHistory();
                    
                    // Explicit consent required when collecting sensitive DV data
                    return dvHistory != HmisFivePoint.YES || hasConsent;
                },
                "Explicit client consent required when recording DV history as YES"
            ),
            
            // High-risk safety validation
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint currentlyFleeing = ((DvRecordCreationRequest) req).currentlyFleeing();
                    boolean safetyProtocolsActivated = ((DvRecordCreationRequest) req).safetyProtocolsActivated();
                    
                    // Safety protocols must be activated for high-risk clients
                    return currentlyFleeing != HmisFivePoint.YES || safetyProtocolsActivated;
                },
                "Safety protocols must be activated when client is currently fleeing DV"
            )
        );
    }
    
    /**
     * Validate DV record update with security considerations
     */
    public ValidationResult validateDvRecordUpdate(
            DvRecordUpdateRequest request) {
        
        return validator.validate(request,
            // Basic validation
            BusinessRuleValidator.rule(
                req -> ((DvRecordUpdateRequest) req).recordId() != null,
                "Record ID is required for updates"
            ),
            BusinessRuleValidator.rule(
                req -> ((DvRecordUpdateRequest) req).changeDate() != null,
                "Change date is required for update records"
            ),
            
            // Security validation for updates
            BusinessRuleValidator.rule(
                req -> {
                    String updatedBy = ((DvRecordUpdateRequest) req).updatedBy();
                    List<String> authorizedRoles = ((DvRecordUpdateRequest) req).updaterAuthorizedRoles();
                    return updatedBy == null || authorizedRoles == null || 
                           authorizedRoles.contains("DV_SPECIALIST") || authorizedRoles.contains("ADMIN");
                },
                "DV records can only be updated by authorized DV specialists or administrators"
            ),
            
            // Change date validation
            BusinessRuleValidator.rule(
                req -> {
                    LocalDate changeDate = ((DvRecordUpdateRequest) req).changeDate();
                    LocalDate originalDate = ((DvRecordUpdateRequest) req).originalInformationDate();
                    return changeDate == null || originalDate == null || changeDate.isAfter(originalDate);
                },
                "Change date must be after original information date"
            ),
            
            // Safety escalation validation
            BusinessRuleValidator.rule(
                req -> {
                    HmisFivePoint newCurrentlyFleeing = ((DvRecordUpdateRequest) req).currentlyFleeing();
                    HmisFivePoint originalCurrentlyFleeing = ((DvRecordUpdateRequest) req).originalCurrentlyFleeing();
                    boolean safetyEscalated = ((DvRecordUpdateRequest) req).safetyEscalationTriggered();
                    
                    // Safety escalation required when fleeing status changes to YES
                    if (originalCurrentlyFleeing != HmisFivePoint.YES && newCurrentlyFleeing == HmisFivePoint.YES) {
                        return safetyEscalated;
                    }
                    return true;
                },
                "Safety escalation must be triggered when fleeing status changes to YES"
            )
        );
    }
    
    /**
     * Validate DV correction with enhanced audit trail
     */
    public ValidationResult validateDvCorrection(
            DvCorrectionRequest request) {
        
        return validator.validate(request,
            // Basic validation
            BusinessRuleValidator.rule(
                req -> ((DvCorrectionRequest) req).originalRecordId() != null,
                "Original record ID is required for corrections"
            ),
            BusinessRuleValidator.rule(
                req -> ((DvCorrectionRequest) req).correctionReason() != null &&
                       !((DvCorrectionRequest) req).correctionReason().trim().isEmpty(),
                "Detailed correction reason is required for DV record corrections"
            ),
            
            // Security validation for corrections
            BusinessRuleValidator.rule(
                req -> {
                    String correctedBy = ((DvCorrectionRequest) req).correctedBy();
                    List<String> authorizedRoles = ((DvCorrectionRequest) req).correctorAuthorizedRoles();
                    return correctedBy == null || authorizedRoles == null || 
                           authorizedRoles.contains("DV_SPECIALIST") || authorizedRoles.contains("ADMIN");
                },
                "DV records can only be corrected by authorized DV specialists or administrators"
            ),
            
            // Audit trail validation
            BusinessRuleValidator.rule(
                req -> {
                    String supervisorApproval = ((DvCorrectionRequest) req).supervisorApproval();
                    LocalDate originalDate = ((DvCorrectionRequest) req).originalInformationDate();
                    
                    // Supervisor approval required for corrections of records older than 30 days
                    return originalDate == null || originalDate.isAfter(LocalDate.now().minusDays(30)) ||
                           (supervisorApproval != null && !supervisorApproval.trim().isEmpty());
                },
                "Supervisor approval required for corrections of DV records older than 30 days"
            ),
            
            // High-risk client validation
            BusinessRuleValidator.rule(
                req -> {
                    boolean isHighRisk = ((DvCorrectionRequest) req).isHighRiskClient();
                    boolean additionalSafetyReview = ((DvCorrectionRequest) req).additionalSafetyReviewCompleted();
                    
                    // Additional safety review required for high-risk clients
                    return !isHighRisk || additionalSafetyReview;
                },
                "Additional safety review required when correcting records for high-risk clients"
            )
        );
    }
    
    /**
     * Validate safety assessment access
     */
    public ValidationResult validateSafetyAssessmentAccess(
            SafetyAssessmentAccessRequest request) {
        
        return validator.validate(request,
            // Role-based access validation
            BusinessRuleValidator.rule(
                req -> {
                    List<String> userRoles = ((SafetyAssessmentAccessRequest) req).userRoles();
                    return userRoles != null && (
                        userRoles.contains("DV_SPECIALIST") || 
                        userRoles.contains("ADMIN") ||
                        userRoles.contains("SAFETY_COORDINATOR")
                    );
                },
                "Safety assessment access requires DV_SPECIALIST, ADMIN, or SAFETY_COORDINATOR role"
            ),
            
            // Access context validation
            BusinessRuleValidator.rule(
                req -> {
                    String accessReason = ((SafetyAssessmentAccessRequest) req).accessReason();
                    return accessReason != null && !accessReason.trim().isEmpty();
                },
                "Access reason must be documented for safety assessment access"
            ),
            
            // Time-based access validation
            BusinessRuleValidator.rule(
                req -> {
                    boolean isBusinessHours = ((SafetyAssessmentAccessRequest) req).isBusinessHours();
                    boolean isEmergency = ((SafetyAssessmentAccessRequest) req).isEmergencyAccess();
                    
                    // After-hours access requires emergency justification
                    return isBusinessHours || isEmergency;
                },
                "After-hours safety assessment access requires emergency justification"
            )
        );
    }
    
    /**
     * Validate high-risk client identification
     */
    public ValidationResult validateHighRiskIdentification(
            HighRiskIdentificationRequest request) {
        
        return validator.validate(request,
            // Risk factors validation
            BusinessRuleValidator.rule(
                req -> {
                    List<String> riskFactors = ((HighRiskIdentificationRequest) req).identifiedRiskFactors();
                    return riskFactors != null && !riskFactors.isEmpty();
                },
                "At least one risk factor must be identified for high-risk classification"
            ),
            
            // Safety plan validation
            BusinessRuleValidator.rule(
                req -> {
                    boolean hasActiveSafetyPlan = ((HighRiskIdentificationRequest) req).hasActiveSafetyPlan();
                    return hasActiveSafetyPlan;
                },
                "Active safety plan required for all high-risk clients"
            ),
            
            // Notification validation
            BusinessRuleValidator.rule(
                req -> {
                    boolean supervisorNotified = ((HighRiskIdentificationRequest) req).supervisorNotified();
                    boolean safetyTeamNotified = ((HighRiskIdentificationRequest) req).safetyTeamNotified();
                    
                    return supervisorNotified && safetyTeamNotified;
                },
                "Both supervisor and safety team must be notified of high-risk client identification"
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
    public record DvRecordCreationRequest(
        HmisFivePoint dvHistory,
        DomesticViolenceRecency whenExperienced,
        HmisFivePoint currentlyFleeing,
        String collectedBy,
        List<String> collectorAuthorizedRoles,
        LocalDate informationDate,
        LocalDate enrollmentEntryDate,
        boolean hasClientConsent,
        boolean safetyProtocolsActivated
    ) {}
    
    public record DvRecordUpdateRequest(
        UUID recordId,
        LocalDate changeDate,
        HmisFivePoint dvHistory,
        DomesticViolenceRecency whenExperienced,
        HmisFivePoint currentlyFleeing,
        String updatedBy,
        List<String> updaterAuthorizedRoles,
        LocalDate originalInformationDate,
        HmisFivePoint originalCurrentlyFleeing,
        boolean safetyEscalationTriggered
    ) {}
    
    public record DvCorrectionRequest(
        UUID originalRecordId,
        String correctionReason,
        String correctedBy,
        List<String> correctorAuthorizedRoles,
        LocalDate originalInformationDate,
        String supervisorApproval,
        boolean isHighRiskClient,
        boolean additionalSafetyReviewCompleted
    ) {}
    
    public record SafetyAssessmentAccessRequest(
        List<String> userRoles,
        String accessReason,
        boolean isBusinessHours,
        boolean isEmergencyAccess
    ) {}
    
    public record HighRiskIdentificationRequest(
        List<String> identifiedRiskFactors,
        boolean hasActiveSafetyPlan,
        boolean supervisorNotified,
        boolean safetyTeamNotified
    ) {}
}