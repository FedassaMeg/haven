package org.haven.servicedelivery.infrastructure.persistence;

import org.haven.servicedelivery.application.services.ServiceBillingService.BillingRateRepository;
import org.haven.shared.vo.services.FundingSource;
import org.haven.shared.vo.services.ServiceCategory;
import org.haven.shared.vo.services.ServiceDeliveryMode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class BillingRateRepositoryImpl implements BillingRateRepository {
    
    @Override
    public Optional<BigDecimal> findRate(FundingSource.BillingRateCategory category, 
                                       ServiceCategory serviceCategory, 
                                       ServiceDeliveryMode deliveryMode) {
        // Return default rates for now
        if (category == FundingSource.BillingRateCategory.FEDERAL_RATE) {
            return Optional.of(BigDecimal.valueOf(75.00));
        } else if (category == FundingSource.BillingRateCategory.STATE_RATE) {
            return Optional.of(BigDecimal.valueOf(65.00));
        } else if (category == FundingSource.BillingRateCategory.LOCAL_RATE) {
            return Optional.of(BigDecimal.valueOf(55.00));
        } else if (category == FundingSource.BillingRateCategory.PRIVATE_RATE) {
            return Optional.of(BigDecimal.valueOf(70.00));
        } else if (category == FundingSource.BillingRateCategory.NO_BILLING) {
            return Optional.of(BigDecimal.ZERO);
        } else {
            return Optional.of(BigDecimal.valueOf(50.00)); // Default rate
        }
    }
}