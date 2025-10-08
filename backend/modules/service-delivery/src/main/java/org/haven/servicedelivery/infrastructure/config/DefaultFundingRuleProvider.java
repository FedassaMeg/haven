package org.haven.servicedelivery.infrastructure.config;

import org.haven.servicedelivery.domain.ServiceTemplateFactory;
import org.haven.shared.vo.services.ServiceCategory;
import org.haven.shared.vo.services.ServiceType;
import org.springframework.stereotype.Component;

/**
 * Default Funding Rule Provider
 * Maps service types to appropriate funding sources
 * Configurable via application properties in future
 */
@Component
public class DefaultFundingRuleProvider implements ServiceTemplateFactory.FundingRuleProvider {

    @Override
    public ServiceTemplateFactory.FundingRule getFundingForServiceType(ServiceType serviceType) {
        // Map service categories to funding sources
        ServiceCategory category = serviceType.getCategory();

        if (category == ServiceCategory.CRISIS_RESPONSE) {
            return ServiceTemplateFactory.FundingRule.vawa();
        } else if (category == ServiceCategory.COUNSELING) {
            return ServiceTemplateFactory.FundingRule.calOes();
        } else if (category == ServiceCategory.CASE_MANAGEMENT || category == ServiceCategory.HOUSING) {
            return ServiceTemplateFactory.FundingRule.hudCoc();
        } else {
            return ServiceTemplateFactory.FundingRule.noFunding();
        }
    }
}
