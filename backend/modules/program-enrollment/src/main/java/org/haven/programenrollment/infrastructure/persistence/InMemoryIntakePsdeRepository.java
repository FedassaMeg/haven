package org.haven.programenrollment.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.programenrollment.domain.IntakePsdeRepository;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.shared.vo.hmis.IntakeDataCollectionStage;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link IntakePsdeRepository} used until a persistent adapter is available.
 * Supports the lifecycle and reporting operations exercised by the domain services.
 */
@Repository
public class InMemoryIntakePsdeRepository implements IntakePsdeRepository {

    private static final Comparator<IntakePsdeRecord> HISTORY_COMPARATOR =
        Comparator.comparing(IntakePsdeRecord::getEffectiveStart, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(IntakePsdeRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(IntakePsdeRecord::getVersion, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(IntakePsdeRecord::getRecordId);

    private static final Comparator<IntakePsdeRecord> REVERSE_EFFECTIVE_START =
        Comparator.comparing(IntakePsdeRecord::getEffectiveStart, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(IntakePsdeRecord::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(IntakePsdeRecord::getRecordId);

    private static final Comparator<IntakePsdeRecord> INFORMATION_DATE_COMPARATOR =
        Comparator.comparing(IntakePsdeRecord::getInformationDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(IntakePsdeRecord::getRecordId);

    private final Map<UUID, IntakePsdeRecord> store = new ConcurrentHashMap<>();

    @Override
    public IntakePsdeRecord save(IntakePsdeRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(record.getRecordId(), "recordId must not be null");
        store.put(record.getRecordId(), record);
        return record;
    }

    @Override
    public Optional<IntakePsdeRecord> findByRecordId(UUID recordId) {
        return Optional.ofNullable(store.get(recordId));
    }

    @Override
    public Optional<IntakePsdeRecord> findActiveByRecordId(UUID recordId) {
        return Optional.ofNullable(store.get(recordId))
            .filter(this::isActive);
    }

    @Override
    public Optional<IntakePsdeRecord> findActiveByEnrollmentIdAsOf(ProgramEnrollmentId enrollmentId, Instant asOfTime) {
        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
        Objects.requireNonNull(asOfTime, "asOfTime must not be null");

        return streamByEnrollment(enrollmentId)
            .filter(record -> isActiveAsOf(record, asOfTime))
            .sorted(REVERSE_EFFECTIVE_START)
            .findFirst();
    }

    @Override
    public Optional<IntakePsdeRecord> findActiveByEnrollmentAndDateAndStage(
            ProgramEnrollmentId enrollmentId,
            LocalDate informationDate,
            IntakeDataCollectionStage collectionStage) {

        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
        Objects.requireNonNull(informationDate, "informationDate must not be null");
        Objects.requireNonNull(collectionStage, "collectionStage must not be null");

        return streamByEnrollment(enrollmentId)
            .filter(this::isActive)
            .filter(record -> informationDate.equals(record.getInformationDate()))
            .filter(record -> record.getCollectionStage() == collectionStage)
            .sorted(REVERSE_EFFECTIVE_START)
            .findFirst();
    }

    @Override
    public List<IntakePsdeRecord> findHistoryByEnrollmentId(ProgramEnrollmentId enrollmentId) {
        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");

        return streamByEnrollment(enrollmentId)
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findActiveByEnrollmentId(ProgramEnrollmentId enrollmentId) {
        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");

        return streamByEnrollment(enrollmentId)
            .filter(this::isActive)
            .sorted(REVERSE_EFFECTIVE_START)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findOverlappingRecords(
            ProgramEnrollmentId enrollmentId,
            Instant effectiveStart,
            Instant effectiveEnd) {

        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
        Objects.requireNonNull(effectiveStart, "effectiveStart must not be null");

        Instant targetEnd = effectiveEnd;

        return streamByEnrollment(enrollmentId)
            .filter(record -> overlaps(record, effectiveStart, targetEnd))
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findAuditChain(UUID recordId) {
        if (recordId == null) {
            return Collections.emptyList();
        }

        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        List<IntakePsdeRecord> result = new ArrayList<>();

        stack.push(recordId);

        while (!stack.isEmpty()) {
            UUID currentId = stack.pop();
            if (!visited.add(currentId)) {
                continue;
            }

            IntakePsdeRecord record = store.get(currentId);
            if (record != null) {
                result.add(record);

                if (record.getSupersedes() != null) {
                    stack.push(record.getSupersedes());
                }
                if (record.getCorrectsRecordId() != null) {
                    stack.push(record.getCorrectsRecordId());
                }
            }

            for (IntakePsdeRecord candidate : store.values()) {
                if (candidate.getSupersedes() != null && candidate.getSupersedes().equals(currentId)) {
                    stack.push(candidate.getRecordId());
                }
                if (candidate.getCorrectsRecordId() != null && candidate.getCorrectsRecordId().equals(currentId)) {
                    stack.push(candidate.getRecordId());
                }
            }
        }

        return result.stream()
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public Optional<IntakePsdeRecord> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        return store.values().stream()
            .filter(record -> idempotencyKey.equals(record.getIdempotencyKey()))
            .findFirst();
    }

    @Override
    public List<IntakePsdeRecord> findForHudAuditExport(LocalDate reportingPeriodStart, LocalDate reportingPeriodEnd) {
        Objects.requireNonNull(reportingPeriodStart, "reportingPeriodStart must not be null");
        Objects.requireNonNull(reportingPeriodEnd, "reportingPeriodEnd must not be null");

        return store.values().stream()
            .filter(record -> isWithin(record.getInformationDate(), reportingPeriodStart, reportingPeriodEnd))
            .sorted(INFORMATION_DATE_COMPARATOR)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findByCorrection(boolean isCorrection) {
        return store.values().stream()
            .filter(record -> Boolean.TRUE.equals(record.getIsCorrection()) == isCorrection)
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findCorrectedBetween(Instant startTime, Instant endTime) {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");

        return store.values().stream()
            .filter(record -> isWithin(record.getCorrectedAt(), startTime, endTime))
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findBackdatedBetween(Instant startTime, Instant endTime) {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");

        return store.values().stream()
            .filter(record -> Boolean.TRUE.equals(record.getIsBackdated()))
            .filter(record -> isWithin(record.getEffectiveStart(), startTime, endTime))
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findByLifecycleStatus(String lifecycleStatus) {
        return store.values().stream()
            .filter(record -> statusMatches(record, lifecycleStatus))
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public long countActiveByEnrollmentId(ProgramEnrollmentId enrollmentId) {
        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");

        return streamByEnrollment(enrollmentId)
            .filter(this::isActive)
            .count();
    }

    @Override
    public long countCorrectionsByEnrollmentId(ProgramEnrollmentId enrollmentId) {
        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");

        return streamByEnrollment(enrollmentId)
            .filter(record -> Boolean.TRUE.equals(record.getIsCorrection()))
            .count();
    }

    @Override
    public void deleteRecord(UUID recordId, String deletedBy, String reason) {
        if (recordId == null) {
            return;
        }

        store.computeIfPresent(recordId, (id, record) -> {
            record.setLifecycleStatus("DELETED");
            if (record.getEffectiveEnd() == null) {
                record.setEffectiveEnd(Instant.now());
            }
            record.setUpdatedBy(deletedBy);
            record.setSupersededBy(deletedBy);
            // Reason is currently informational only; IntakePsdeRecord has no dedicated field.
            return record;
        });
    }

    @Override
    public List<IntakePsdeRecord> findRecordsNeedingComplianceReview() {
        return store.values().stream()
            .filter(record -> record.hasDvConditionalLogicErrors() || !record.meetsHmisDataQuality())
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findHighRiskDvCases() {
        return store.values().stream()
            .filter(IntakePsdeRecord::isHighSensitivityDvCase)
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    @Override
    public List<IntakePsdeRecord> findRecordsWithDataQualityIssues() {
        return store.values().stream()
            .filter(record -> !record.meetsHmisDataQuality())
            .sorted(HISTORY_COMPARATOR)
            .toList();
    }

    private Stream<IntakePsdeRecord> streamByEnrollment(ProgramEnrollmentId enrollmentId) {
        return store.values().stream()
            .filter(record -> matchesEnrollment(record, enrollmentId));
    }

    private boolean matchesEnrollment(IntakePsdeRecord record, ProgramEnrollmentId enrollmentId) {
        return record != null && record.getEnrollmentId() != null && record.getEnrollmentId().equals(enrollmentId);
    }

    private boolean isActive(IntakePsdeRecord record) {
        return record != null &&
            statusMatches(record, "ACTIVE") &&
            isActiveAsOf(record, Instant.now());
    }

    private boolean isActiveAsOf(IntakePsdeRecord record, Instant instant) {
        if (record == null || instant == null) {
            return false;
        }
        Instant start = normalizeStart(record.getEffectiveStart());
        Instant end = normalizeEnd(record.getEffectiveEnd());
        return !instant.isBefore(start) && !instant.isAfter(end);
    }

    private boolean overlaps(IntakePsdeRecord record, Instant start, Instant end) {
        if (record == null) {
            return false;
        }
        Instant recordStart = normalizeStart(record.getEffectiveStart());
        Instant recordEnd = normalizeEnd(record.getEffectiveEnd());
        Instant targetStart = normalizeStart(start);
        Instant targetEnd = normalizeEnd(end);
        return !recordEnd.isBefore(targetStart) && !targetEnd.isBefore(recordStart);
    }

    private boolean statusMatches(IntakePsdeRecord record, String lifecycleStatus) {
        if (record == null) {
            return false;
        }
        String currentStatus = record.getLifecycleStatus();
        if (lifecycleStatus == null) {
            return currentStatus == null;
        }
        return lifecycleStatus.equalsIgnoreCase(String.valueOf(currentStatus));
    }

    private Instant normalizeStart(Instant start) {
        return start != null ? start : Instant.MIN;
    }

    private Instant normalizeEnd(Instant end) {
        return end != null ? end : Instant.MAX;
    }

    private boolean isWithin(Instant value, Instant start, Instant end) {
        if (value == null) {
            return false;
        }
        return (start == null || !value.isBefore(start)) &&
               (end == null || !value.isAfter(end));
    }

    private boolean isWithin(LocalDate value, LocalDate start, LocalDate end) {
        if (value == null) {
            return false;
        }
        return (start == null || !value.isBefore(start)) &&
               (end == null || !value.isAfter(end));
    }
}
