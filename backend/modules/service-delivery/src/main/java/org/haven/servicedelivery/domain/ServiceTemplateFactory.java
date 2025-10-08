package org.haven.servicedelivery.domain;

import org.haven.servicedelivery.application.commands.CreateServiceEpisodeCmd;
import org.haven.shared.vo.services.ServiceDeliveryMode;
import org.haven.shared.vo.services.ServiceType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service Template Factory
 * Creates pre-configured service episode commands for common scenarios
 * Encapsulates funding rules and service type defaults
 */
public class ServiceTemplateFactory {

    private final FundingRuleProvider fundingRuleProvider;

    public ServiceTemplateFactory(FundingRuleProvider fundingRuleProvider) {
        this.fundingRuleProvider = fundingRuleProvider;
    }

    /**
     * Create crisis intervention service template
     */
    public CreateServiceEpisodeCmd createCrisisInterventionService(
            UUID clientId,
            String enrollmentId,
            String programId,
            String providerId,
            String providerName,
            boolean isConfidential,
            String createdBy) {

        var fundingRule = fundingRuleProvider.getFundingForServiceType(ServiceType.CRISIS_INTERVENTION);

        return new CreateServiceEpisodeCmd(
            clientId,
            enrollmentId,
            programId,
            "Crisis Response Program",
            ServiceType.CRISIS_INTERVENTION,
            ServiceDeliveryMode.IN_PERSON,
            LocalDate.now(),
            60, // 1 hour planned
            providerId,
            providerName,
            fundingRule.funderId(),
            fundingRule.funderName(),
            null,
            "Crisis intervention service",
            isConfidential,
            createdBy
        );
    }

    /**
     * Create counseling session template
     */
    public CreateServiceEpisodeCmd createCounselingSession(
            UUID clientId,
            String enrollmentId,
            String programId,
            ServiceType counselingType,
            String providerId,
            String providerName,
            String createdBy) {

        var fundingRule = fundingRuleProvider.getFundingForServiceType(counselingType);

        return new CreateServiceEpisodeCmd(
            clientId,
            enrollmentId,
            programId,
            "Counseling Program",
            counselingType,
            ServiceDeliveryMode.IN_PERSON,
            LocalDate.now(),
            50, // 50 minutes standard
            providerId,
            providerName,
            fundingRule.funderId(),
            fundingRule.funderName(),
            null,
            counselingType.getDescription() + " session",
            true, // Counseling is confidential
            createdBy
        );
    }

    /**
     * Create case management contact template
     */
    public CreateServiceEpisodeCmd createCaseManagementContact(
            UUID clientId,
            String enrollmentId,
            String programId,
            ServiceDeliveryMode deliveryMode,
            String providerId,
            String providerName,
            String description,
            String createdBy) {

        var fundingRule = fundingRuleProvider.getFundingForServiceType(ServiceType.CASE_MANAGEMENT);

        return new CreateServiceEpisodeCmd(
            clientId,
            enrollmentId,
            programId,
            "Case Management Program",
            ServiceType.CASE_MANAGEMENT,
            deliveryMode,
            LocalDate.now(),
            30, // 30 minutes planned
            providerId,
            providerName,
            fundingRule.funderId(),
            fundingRule.funderName(),
            null,
            description,
            false, // Case management typically not confidential
            createdBy
        );
    }

    /**
     * Funding Rule Provider interface
     * Allows configuration of funding rules per service type
     */
    public interface FundingRuleProvider {
        FundingRule getFundingForServiceType(ServiceType serviceType);
    }

    /**
     * Funding Rule record
     */
    public record FundingRule(
        String funderId,
        String funderName
    ) {
        public static FundingRule vawa() {
            return new FundingRule("DOJ-VAWA", "DOJ Violence Against Women Act");
        }

        public static FundingRule calOes() {
            return new FundingRule("CAL-OES", "California Office of Emergency Services");
        }

        public static FundingRule hudCoc() {
            return new FundingRule("HUD-COC", "HUD Continuum of Care");
        }

        public static FundingRule noFunding() {
            return new FundingRule("NONE", "No Funding");
        }
    }
}
