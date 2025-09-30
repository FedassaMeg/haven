package org.haven.reporting.application.services;

import org.haven.reporting.domain.pithic.HicInventoryData;
import org.haven.reporting.domain.pithic.PitCensusData;
import org.haven.shared.vo.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight scheduler that delegates to {@link PitHicAggregationService}. The original implementation
 * persisted output through the housing assistance module; with that module removed we simply keep
 * results in memory and expose basic job status so downstream callers remain operational.
 */
@Service
public class PitHicEtlJobService {

    private static final Logger log = LoggerFactory.getLogger(PitHicEtlJobService.class);

    private final PitHicAggregationService aggregationService;

    @Value("${pithic.etl.enabled:true}")
    private boolean etlEnabled;

    @Value("${pithic.etl.pit.census.day:31}")
    private int pitCensusDay;

    @Value("${pithic.etl.pit.census.month:1}")
    private int pitCensusMonth;

    @Value("${pithic.etl.hic.census.day:31}")
    private int hicCensusDay;

    @Value("${pithic.etl.hic.census.month:1}")
    private int hicCensusMonth;

    private List<String> continuumCodes = List.of("DEFAULT-CONTINUUM");
    private List<String> organizationIds = List.of("DEFAULT-ORG");

    private final Map<UUID, JobStatus> activeJobs = new ConcurrentHashMap<>();

    public PitHicEtlJobService(PitHicAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Value("${pithic.etl.continuums:}")
    public void setContinuumsProperty(String configuredContinuums) {
        this.continuumCodes = parseCsv(configuredContinuums, "DEFAULT-CONTINUUM");
    }

    @Value("${pithic.etl.organizations:}")
    public void setOrganizationsProperty(String configuredOrganizations) {
        this.organizationIds = parseCsv(configuredOrganizations, "DEFAULT-ORG");
    }

    @Scheduled(cron = "0 0 0 ${pithic.etl.pit.census.day} ${pithic.etl.pit.census.month} *")
    public void runScheduledPitCensus() {
        if (!etlEnabled) {
            log.debug("Skipping scheduled PIT census ETL – service disabled");
            return;
        }
        LocalDate censusDate = LocalDate.now();
        startPitCensusJob(censusDate, "SCHEDULED", UserId.system());
    }

    @Scheduled(cron = "0 0 0 ${pithic.etl.hic.census.day} ${pithic.etl.hic.census.month} *")
    public void runScheduledHicInventory() {
        if (!etlEnabled) {
            log.debug("Skipping scheduled HIC inventory ETL – service disabled");
            return;
        }
        LocalDate inventoryDate = LocalDate.now();
        startHicInventoryJob(inventoryDate, "SCHEDULED", UserId.system());
    }

    public UUID startPitCensusJob(LocalDate censusDate, String trigger, UserId initiatedBy) {
        UUID jobId = UUID.randomUUID();
        JobStatus status = new JobStatus(jobId, JobType.PIT_CENSUS, censusDate, trigger);
        activeJobs.put(jobId, status);
        log.info("Starting PIT census job {} for date {} (trigger: {})", jobId, censusDate, trigger);

        try {
            status.startProcessing();
            for (String continuum : safeList(continuumCodes)) {
                for (String org : safeList(organizationIds)) {
                    PitCensusData data = aggregationService.generatePitCensusData(censusDate, continuum, org, initiatedBy);
                    status.incrementProcessed();
                    log.debug("Generated PIT census {} for continuum {} org {}", data.getCensusId(), continuum, org);
                }
            }
            status.complete();
        } catch (Exception ex) {
            status.fail();
            status.incrementErrors();
            log.error("PIT census job {} failed", jobId, ex);
        }
        return jobId;
    }

    public UUID startHicInventoryJob(LocalDate inventoryDate, String trigger, UserId initiatedBy) {
        UUID jobId = UUID.randomUUID();
        JobStatus status = new JobStatus(jobId, JobType.HIC_INVENTORY, inventoryDate, trigger);
        activeJobs.put(jobId, status);
        log.info("Starting HIC inventory job {} for date {} (trigger: {})", jobId, inventoryDate, trigger);

        try {
            status.startProcessing();
            for (String continuum : safeList(continuumCodes)) {
                for (String org : safeList(organizationIds)) {
                    HicInventoryData data = aggregationService.generateHicInventoryData(inventoryDate, continuum, org, initiatedBy);
                    status.incrementProcessed();
                    log.debug("Generated HIC inventory {} for continuum {} org {}", data.getInventoryId(), continuum, org);
                }
            }
            status.complete();
        } catch (Exception ex) {
            status.fail();
            status.incrementErrors();
            log.error("HIC inventory job {} failed", jobId, ex);
        }
        return jobId;
    }

    public JobStatus getJobStatus(UUID jobId) {
        return activeJobs.get(jobId);
    }

    public void configureCensusWindows(CensusWindowConfig config) {
        this.pitCensusDay = config.pitCensusDay();
        this.pitCensusMonth = config.pitCensusMonth();
        this.hicCensusDay = config.hicCensusDay();
        this.hicCensusMonth = config.hicCensusMonth();
        log.info("Updated census windows: PIT {}-{}/ HIC {}-{}",
            pitCensusMonth, pitCensusDay, hicCensusMonth, hicCensusDay);
    }

    public AdhocJobResult runAdhocAggregation(LocalDate startDate,
                                              LocalDate endDate,
                                              List<String> specificContinuums,
                                              List<String> specificOrganizations,
                                              UserId initiatedBy) {
        List<PitCensusData> pitResults = new ArrayList<>();
        List<HicInventoryData> hicResults = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            for (String continuum : safeList(specificContinuums)) {
                for (String org : safeList(specificOrganizations)) {
                    pitResults.add(aggregationService.generatePitCensusData(current, continuum, org, initiatedBy));
                    hicResults.add(aggregationService.generateHicInventoryData(current, continuum, org, initiatedBy));
                }
            }
            current = current.plusDays(1);
        }

        return new AdhocJobResult(UUID.randomUUID(), UUID.randomUUID(), pitResults, hicResults);
    }

    private List<String> safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of("DEFAULT");
        }
        return values;
    }

    private List<String> parseCsv(String raw, String defaultValue) {
        if (raw == null || raw.isBlank()) {
            return List.of(defaultValue);
        }
        String[] tokens = raw.split(",");
        List<String> cleaned = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return cleaned.isEmpty() ? List.of(defaultValue) : cleaned;
    }

    public enum JobType {
        PIT_CENSUS,
        HIC_INVENTORY
    }

    public enum JobState {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    public static class JobStatus {
        private final UUID jobId;
        private final JobType type;
        private final LocalDate processDate;
        private final String trigger;
        private JobState state = JobState.PENDING;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int processedCount;
        private int errorCount;

        JobStatus(UUID jobId, JobType type, LocalDate processDate, String trigger) {
            this.jobId = jobId;
            this.type = type;
            this.processDate = processDate;
            this.trigger = trigger;
        }

        void startProcessing() {
            this.state = JobState.RUNNING;
            this.startTime = LocalDateTime.now();
        }

        void complete() {
            this.state = JobState.COMPLETED;
            this.endTime = LocalDateTime.now();
        }

        void fail() {
            this.state = JobState.FAILED;
            this.endTime = LocalDateTime.now();
        }

        void incrementProcessed() {
            this.processedCount++;
        }

        void incrementErrors() {
            this.errorCount++;
        }

        public UUID getJobId() { return jobId; }
        public JobType getType() { return type; }
        public LocalDate getProcessDate() { return processDate; }
        public String getTrigger() { return trigger; }
        public JobState getState() { return state; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getProcessedCount() { return processedCount; }
        public int getErrorCount() { return errorCount; }
    }

    public record CensusWindowConfig(int pitCensusDay,
                                     int pitCensusMonth,
                                     int hicCensusDay,
                                     int hicCensusMonth) {}

    public record AdhocJobResult(UUID pitJobId,
                                 UUID hicJobId,
                                 List<PitCensusData> pitResults,
                                 List<HicInventoryData> hicResults) {}
}
