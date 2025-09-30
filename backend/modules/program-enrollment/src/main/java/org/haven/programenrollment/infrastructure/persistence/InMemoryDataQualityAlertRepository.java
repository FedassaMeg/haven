package org.haven.programenrollment.infrastructure.persistence;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.haven.programenrollment.application.services.DataQualityAlert;
import org.haven.programenrollment.application.services.DataQualityAlertRepository;
import org.springframework.stereotype.Repository;

/**
 * Simple in-memory repository for {@link DataQualityAlert} instances. Provides predictable
 * behavior for application services until a persistent implementation is available.
 */
@Repository
public class InMemoryDataQualityAlertRepository implements DataQualityAlertRepository {

    private final Map<UUID, DataQualityAlert> store = new ConcurrentHashMap<>();

    @Override
    public void save(DataQualityAlert alert) {
        Objects.requireNonNull(alert, "alert must not be null");
        Objects.requireNonNull(alert.getId(), "alert id must not be null");
        store.put(alert.getId(), alert);
    }

    @Override
    public Optional<DataQualityAlert> findById(UUID alertId) {
        if (alertId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(alertId));
    }

    @Override
    public List<DataQualityAlert> findUnresolved() {
        return store.values().stream()
            .filter(alert -> !alert.isResolved())
            .collect(Collectors.toList());
    }

    @Override
    public List<DataQualityAlert> findByEnrollmentId(UUID enrollmentId) {
        if (enrollmentId == null) {
            return List.of();
        }
        return store.values().stream()
            .filter(alert -> Objects.equals(alert.getEnrollmentId(), enrollmentId))
            .collect(Collectors.toList());
    }

    @Override
    public List<DataQualityAlert> findByAlertType(DataQualityAlert.AlertType alertType) {
        if (alertType == null) {
            return List.of();
        }
        return store.values().stream()
            .filter(alert -> alertType == alert.getAlertType())
            .collect(Collectors.toList());
    }

    @Override
    public List<DataQualityAlert> findBySeverity(DataQualityAlert.Severity severity) {
        if (severity == null) {
            return List.of();
        }
        return store.values().stream()
            .filter(alert -> severity == alert.getSeverity())
            .collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return store.size();
    }

    @Override
    public long countUnresolved() {
        return store.values().stream()
            .filter(alert -> !alert.isResolved())
            .count();
    }

    @Override
    public long countBySeverity(DataQualityAlert.Severity severity) {
        if (severity == null) {
            return 0;
        }
        return store.values().stream()
            .filter(alert -> severity == alert.getSeverity())
            .count();
    }

    @Override
    public long countCreatedToday() {
        LocalDate today = LocalDate.now();
        return store.values().stream()
            .filter(alert -> today.equals(alert.getCreatedDate()))
            .count();
    }

    @Override
    public List<DataQualityAlert> findAlertsRequiringEscalation(int daysThreshold) {
        LocalDate thresholdDate = LocalDate.now().minusDays(Math.max(daysThreshold, 0));
        return store.values().stream()
            .filter(alert -> alert.getSeverity() == DataQualityAlert.Severity.HIGH
                || alert.getSeverity() == DataQualityAlert.Severity.CRITICAL)
            .filter(alert -> !alert.isResolved())
            .filter(alert -> isCreatedBefore(alert, thresholdDate))
            .collect(Collectors.toList());
    }

    private boolean isCreatedBefore(DataQualityAlert alert, LocalDate thresholdDate) {
        LocalDate createdDate = alert.getCreatedDate();
        if (createdDate == null) {
            return false;
        }
        return createdDate.isBefore(thresholdDate);
    }
}
