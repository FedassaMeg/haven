package org.haven.servicedelivery.application.services;

import org.haven.servicedelivery.domain.ServiceEpisode;
import org.haven.shared.vo.services.FundingSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service Billing Service
 * Domain service - framework independent
 * Handles billing calculation and tracking for service episodes
 */
public class ServiceBillingService {

    private final BillingRateRepository billingRateRepository;

    public ServiceBillingService(BillingRateRepository billingRateRepository) {
        this.billingRateRepository = billingRateRepository;
    }

    /**
     * Generate billing for a completed service episode
     */
    public BillingRecord generateBilling(ServiceEpisode episode) {
        if (!episode.isBillable() || !episode.isCompleted()) {
            throw new IllegalArgumentException("Episode is not billable or not completed");
        }

        Double hours = episode.getActualBillableHours();
        if (hours == null || hours <= 0) {
            throw new IllegalArgumentException("Invalid billable hours");
        }

        FundingSource fundingSource = episode.getPrimaryFundingSource();
        BigDecimal hourlyRate = getHourlyRate(fundingSource, episode.getServiceType(), episode.getDeliveryMode());
        
        BigDecimal totalAmount = hourlyRate
            .multiply(BigDecimal.valueOf(hours))
            .multiply(BigDecimal.valueOf(episode.getDeliveryMode().getBillingMultiplier()))
            .setScale(2, RoundingMode.HALF_UP);

        return new BillingRecord(
            episode.getId().value(),
            episode.getClientId().value(),
            episode.getEnrollmentId(),
            episode.getProgramId(),
            fundingSource,
            episode.getServiceType(),
            episode.getDeliveryMode(),
            episode.getServiceDate(),
            hours,
            hourlyRate,
            totalAmount,
            BillingStatus.PENDING
        );
    }

    /**
     * Generate billing summary for funding source
     */
    public BillingSummary generateBillingSummary(FundingSource fundingSource, 
                                                LocalDate startDate, 
                                                LocalDate endDate,
                                                List<BillingRecord> billingRecords) {
        
        var filteredRecords = billingRecords.stream()
            .filter(record -> record.fundingSource().equals(fundingSource))
            .filter(record -> !record.serviceDate().isBefore(startDate) && 
                             !record.serviceDate().isAfter(endDate))
            .toList();

        BigDecimal totalAmount = filteredRecords.stream()
            .map(BillingRecord::totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double totalHours = filteredRecords.stream()
            .mapToDouble(BillingRecord::billableHours)
            .sum();

        int totalServices = filteredRecords.size();

        Map<org.haven.shared.vo.services.ServiceType, Integer> serviceBreakdown = 
            filteredRecords.stream()
                .collect(Collectors.groupingBy(
                    BillingRecord::serviceType,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));

        return new BillingSummary(
            fundingSource,
            startDate,
            endDate,
            totalServices,
            totalHours,
            totalAmount,
            serviceBreakdown
        );
    }

    /**
     * Validate billing record against funding source requirements
     */
    public BillingValidationResult validateBilling(BillingRecord record) {
        var issues = new java.util.ArrayList<String>();

        // Check if service type is compatible with funding source
        if (!record.fundingSource().isCompatibleWith(record.serviceType())) {
            issues.add("Service type not compatible with funding source");
        }

        // Check if hours are reasonable
        var typicalRange = record.serviceType().getTypicalDuration();
        int actualMinutes = (int) (record.billableHours() * 60);
        if (actualMinutes < typicalRange.minMinutes()) {
            issues.add("Service duration below minimum expected for service type");
        }
        if (actualMinutes > typicalRange.maxMinutes() * 2) { // Allow some flexibility
            issues.add("Service duration significantly exceeds maximum expected for service type");
        }

        // Check delivery mode compatibility
        if (record.serviceType().requiresConfidentialHandling() && 
            !record.deliveryMode().allowsConfidentialServices()) {
            issues.add("Delivery mode not suitable for confidential service type");
        }

        return new BillingValidationResult(
            record.episodeId(),
            issues.isEmpty(),
            issues
        );
    }

    private BigDecimal getHourlyRate(FundingSource fundingSource, 
                                   org.haven.shared.vo.services.ServiceType serviceType,
                                   org.haven.shared.vo.services.ServiceDeliveryMode deliveryMode) {
        return billingRateRepository.findRate(
            fundingSource.getBillingRateCategory(),
            serviceType.getCategory(),
            deliveryMode
        ).orElse(BigDecimal.valueOf(50.00)); // Default rate
    }

    // Supporting records and interfaces
    public record BillingRecord(
        java.util.UUID episodeId,
        java.util.UUID clientId,
        String enrollmentId,
        String programId,
        FundingSource fundingSource,
        org.haven.shared.vo.services.ServiceType serviceType,
        org.haven.shared.vo.services.ServiceDeliveryMode deliveryMode,
        LocalDate serviceDate,
        Double billableHours,
        BigDecimal hourlyRate,
        BigDecimal totalAmount,
        BillingStatus status
    ) {}

    public record BillingSummary(
        FundingSource fundingSource,
        LocalDate startDate,
        LocalDate endDate,
        int totalServices,
        Double totalHours,
        BigDecimal totalAmount,
        Map<org.haven.shared.vo.services.ServiceType, Integer> serviceBreakdown
    ) {}

    public record BillingValidationResult(
        java.util.UUID episodeId,
        boolean isValid,
        List<String> issues
    ) {}

    public enum BillingStatus {
        PENDING,
        APPROVED,
        INVOICED,
        PAID,
        REJECTED
    }

    public interface BillingRateRepository {
        java.util.Optional<BigDecimal> findRate(
            FundingSource.BillingRateCategory category,
            org.haven.shared.vo.services.ServiceCategory serviceCategory,
            org.haven.shared.vo.services.ServiceDeliveryMode deliveryMode
        );
    }
}