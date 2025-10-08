package org.haven.api.services;

import org.haven.clientprofile.application.services.ClientAppService;
import org.haven.clientprofile.application.commands.CreateClientCmd;
import org.haven.clientprofile.domain.ClientId;
import org.haven.casemgmt.application.services.CaseAppService;
import org.haven.casemgmt.application.commands.OpenCaseCmd;
import org.haven.casemgmt.domain.CaseId;
import org.haven.shared.vo.CodeableConcept;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Orchestrates complex business workflows across multiple bounded contexts
 */
@Service
@Transactional
public class ClientOnboardingService {
    
    private final ClientAppService clientAppService;
    private final CaseAppService caseAppService;
    
    public ClientOnboardingService(ClientAppService clientAppService, CaseAppService caseAppService) {
        this.clientAppService = clientAppService;
        this.caseAppService = caseAppService;
    }
    
    /**
     * Complete client onboarding process
     */
    public OnboardingResult onboardClient(CreateClientCmd clientCmd, boolean requiresInitialAssessment) {
        // Create the client
        ClientId clientId = clientAppService.handle(clientCmd);
        
        CaseId assessmentCaseId = null;
        if (requiresInitialAssessment) {
            // Automatically create initial assessment case
            var assessmentType = createCaseTypeCoding("initial-assessment", "Initial Assessment");
            var openCaseCmd = new OpenCaseCmd(
                clientId,
                assessmentType,
                null, // Priority will be auto-calculated
                "Initial client assessment and intake"
            );
            assessmentCaseId = caseAppService.handle(openCaseCmd);
        }
        
        return new OnboardingResult(clientId, assessmentCaseId);
    }
    
    /**
     * Business rule: Determine if a client needs immediate attention
     */
    public boolean requiresImmediateAttention(CreateClientCmd clientCmd) {
        // Example business logic - in real world would be more sophisticated
        // Could check for risk factors, previous history, etc.
        return false; // Placeholder
    }
    
    private CodeableConcept createCaseTypeCoding(String code, String display) {
        var coding = new CodeableConcept.Coding(
            "http://haven.org/fhir/CodeSystem/case-type",
            null, code, display, null);
        return new CodeableConcept(List.of(coding), display);
    }
    
    public record OnboardingResult(
        ClientId clientId,
        CaseId initialAssessmentCaseId
    ) {}
}