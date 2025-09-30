package org.haven.reporting.application.services;

import org.haven.reporting.domain.pithic.HicInventoryData;
import org.haven.reporting.domain.pithic.PitCensusData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal validation facade that keeps public API compatibility while the housing assistance module
 * is absent. The service returns successful validation results with basic metrics so existing
 * reporting flows remain intact.
 */
@Service
public class PitHicValidationService {

    private static final Logger log = LoggerFactory.getLogger(PitHicValidationService.class);

    private final PitHicAggregationService aggregationService;

    public PitHicValidationService(PitHicAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    public PitValidationResult validatePitCensus(UUID censusId) {
        PitCensusData data = aggregationService.findPitCensus(censusId)
            .orElseThrow(() -> new IllegalArgumentException("PIT census not found: " + censusId));

        log.debug("Validated PIT census {} in placeholder mode", censusId);
        Map<String, Object> metrics = Map.of(
            "totalPersons", data.getTotalPersons(),
            "generatedAt", data.getGeneratedAt()
        );

        return new PitValidationResult(
            censusId,
            data.getCensusDate(),
            ValidationStatus.PASSED,
            List.of(),
            metrics,
            LocalDate.now()
        );
    }

    public HicValidationResult validateHicInventory(UUID inventoryId) {
        HicInventoryData data = aggregationService.findHicInventory(inventoryId)
            .orElseThrow(() -> new IllegalArgumentException("HIC inventory not found: " + inventoryId));

        log.debug("Validated HIC inventory {} in placeholder mode", inventoryId);
        Map<String, Object> metrics = Map.of(
            "totalBeds", data.calculateTotals().totalBeds(),
            "generatedAt", data.getGeneratedAt()
        );

        return new HicValidationResult(
            inventoryId,
            data.getInventoryDate(),
            ValidationStatus.PASSED,
            List.of(),
            metrics,
            LocalDate.now()
        );
    }

    public CrossValidationResult crossValidatePitHic(UUID censusId, UUID inventoryId) {
        PitCensusData pit = aggregationService.findPitCensus(censusId)
            .orElseThrow(() -> new IllegalArgumentException("PIT census not found: " + censusId));
        HicInventoryData hic = aggregationService.findHicInventory(inventoryId)
            .orElseThrow(() -> new IllegalArgumentException("HIC inventory not found: " + inventoryId));

        log.debug("Cross validated PIT {} and HIC {} in placeholder mode", censusId, inventoryId);

        Map<String, Object> metrics = Map.of(
            "pitTotalPersons", pit.getTotalPersons(),
            "hicTotalBeds", hic.calculateTotals().totalBeds()
        );

        return new CrossValidationResult(
            censusId,
            inventoryId,
            ValidationStatus.PASSED,
            List.of(),
            metrics,
            LocalDate.now()
        );
    }

    // Result types -----------------------------------------------------------------------------

    public record PitValidationResult(UUID censusId,
                                      LocalDate censusDate,
                                      ValidationStatus status,
                                      List<ValidationIssue> issues,
                                      Map<String, Object> metrics,
                                      LocalDate validatedOn) {}

    public record HicValidationResult(UUID inventoryId,
                                      LocalDate inventoryDate,
                                      ValidationStatus status,
                                      List<ValidationIssue> issues,
                                      Map<String, Object> metrics,
                                      LocalDate validatedOn) {}

    public record CrossValidationResult(UUID censusId,
                                        UUID inventoryId,
                                        ValidationStatus status,
                                        List<ValidationIssue> issues,
                                        Map<String, Object> metrics,
                                        LocalDate validatedOn) {}

    public record ValidationIssue(ValidationSeverity severity,
                                  String code,
                                  String message) {}

    public enum ValidationSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public enum ValidationStatus {
        PASSED,
        WARNING,
        FAILED
    }
}
