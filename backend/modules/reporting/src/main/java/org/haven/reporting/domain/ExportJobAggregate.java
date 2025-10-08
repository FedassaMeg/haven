package org.haven.reporting.domain;

import org.haven.reporting.domain.events.*;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Event-sourced aggregate for HUD export job tracking
 * State machine: QUEUED → MATERIALIZING → VALIDATING → COMPLETE/FAILED
 *
 * Coordinates export pipeline:
 * - Queues export request with reporting period and project filters
 * - Materializes views applying VAWA consent checks via PolicyDecisionLog
 * - Validates HUD data quality and compliance rules
 * - Stores CSV artifacts in blob storage with SHA-256 hash
 * - Emits audit events for compliance tracking
 */
public class ExportJobAggregate extends AggregateRoot<ExportJobId> {

    private String exportType; // HMIS_CSV, CoC_APR, ESG_CAPER, etc.
    private LocalDate reportingPeriodStart;
    private LocalDate reportingPeriodEnd;
    private List<UUID> includedProjectIds = new ArrayList<>();
    private String requestedByUserId;
    private String requestedByUserName;
    private String cocCode;
    private String exportReason;

    private ExportJobState state;
    private Long recordsProcessed;
    private Long vawaSupressedRecords;

    // Completion metadata
    private String blobStorageUrl;
    private String sha256Hash;
    private Long totalRecords;
    private List<String> generatedCsvFiles = new ArrayList<>();

    // Failure metadata
    private String errorMessage;
    private String errorCode;
    private List<String> validationErrors = new ArrayList<>();

    private Instant createdAt;
    private Instant completedAt;
    private Instant failedAt;

    /**
     * Queue a new export job
     */
    public static ExportJobAggregate queueExport(
            String exportType,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            List<UUID> includedProjectIds,
            String requestedByUserId,
            String requestedByUserName,
            String cocCode,
            String exportReason) {

        ExportJobId exportJobId = ExportJobId.generate();
        ExportJobAggregate aggregate = new ExportJobAggregate();

        aggregate.apply(new ExportJobQueued(
            exportJobId.value(),
            exportType,
            reportingPeriodStart,
            reportingPeriodEnd,
            includedProjectIds,
            requestedByUserId,
            requestedByUserName,
            cocCode,
            exportReason,
            Instant.now()
        ));

        return aggregate;
    }

    /**
     * Reconstruct aggregate from event history
     */
    public static ExportJobAggregate reconstruct(UUID exportJobId, List<DomainEvent> events) {
        ExportJobAggregate aggregate = new ExportJobAggregate();
        aggregate.id = ExportJobId.of(exportJobId);
        for (int i = 0; i < events.size(); i++) {
            aggregate.replay(events.get(i), i + 1);
        }
        return aggregate;
    }

    /**
     * Start materialization phase
     */
    public void startMaterialization() {
        if (!state.canTransitionTo(ExportJobState.MATERIALIZING)) {
            throw new IllegalStateException(
                "Cannot transition from " + state + " to MATERIALIZING");
        }

        apply(new ExportJobStateChanged(
            id.value(),
            state,
            ExportJobState.MATERIALIZING,
            "Starting view materialization with VAWA consent checks",
            0L,
            Instant.now()
        ));
    }

    /**
     * Start validation phase
     */
    public void startValidation(long recordsProcessed) {
        if (!state.canTransitionTo(ExportJobState.VALIDATING)) {
            throw new IllegalStateException(
                "Cannot transition from " + state + " to VALIDATING");
        }

        apply(new ExportJobStateChanged(
            id.value(),
            state,
            ExportJobState.VALIDATING,
            "Starting HUD compliance validation",
            recordsProcessed,
            Instant.now()
        ));
    }

    /**
     * Mark export as complete
     */
    public void complete(
            String blobStorageUrl,
            String sha256Hash,
            Long totalRecords,
            Long vawaSupressedRecords,
            List<String> generatedCsvFiles) {

        if (!state.canTransitionTo(ExportJobState.COMPLETE)) {
            throw new IllegalStateException(
                "Cannot transition from " + state + " to COMPLETE");
        }

        apply(new ExportJobCompleted(
            id.value(),
            blobStorageUrl,
            sha256Hash,
            totalRecords,
            vawaSupressedRecords,
            generatedCsvFiles,
            Instant.now()
        ));
    }

    /**
     * Mark export as failed
     */
    public void fail(String errorMessage, String errorCode, List<String> validationErrors) {
        // Can fail from any non-terminal state
        if (state.isTerminal()) {
            throw new IllegalStateException(
                "Cannot fail export job in terminal state: " + state);
        }

        apply(new ExportJobFailed(
            id.value(),
            errorMessage,
            errorCode,
            validationErrors,
            Instant.now()
        ));
    }

    // Event handlers - apply business logic changes

    @Override
    protected void when(DomainEvent event) {
        if (event instanceof ExportJobQueued) {
            handle((ExportJobQueued) event);
        } else if (event instanceof ExportJobStateChanged) {
            handle((ExportJobStateChanged) event);
        } else if (event instanceof ExportJobCompleted) {
            handle((ExportJobCompleted) event);
        } else if (event instanceof ExportJobFailed) {
            handle((ExportJobFailed) event);
        }
    }

    private void handle(ExportJobQueued event) {
        this.id = ExportJobId.of(event.getAggregateId());
        this.exportType = event.getExportType();
        this.reportingPeriodStart = event.getReportingPeriodStart();
        this.reportingPeriodEnd = event.getReportingPeriodEnd();
        this.includedProjectIds = new ArrayList<>(event.getIncludedProjectIds());
        this.requestedByUserId = event.getRequestedByUserId();
        this.requestedByUserName = event.getRequestedByUserName();
        this.cocCode = event.getCocCode();
        this.exportReason = event.getExportReason();
        this.state = ExportJobState.QUEUED;
        this.createdAt = event.getOccurredOn();
        this.recordsProcessed = 0L;
        this.vawaSupressedRecords = 0L;
    }

    private void handle(ExportJobStateChanged event) {
        this.state = event.getNewState();
        this.recordsProcessed = event.getRecordsProcessed();
    }

    private void handle(ExportJobCompleted event) {
        this.state = ExportJobState.COMPLETE;
        this.blobStorageUrl = event.getBlobStorageUrl();
        this.sha256Hash = event.getSha256Hash();
        this.totalRecords = event.getTotalRecords();
        this.vawaSupressedRecords = event.getVawaSupressedRecords();
        this.generatedCsvFiles = new ArrayList<>(event.getGeneratedCsvFiles());
        this.completedAt = event.getCompletedAt();
    }

    private void handle(ExportJobFailed event) {
        this.state = ExportJobState.FAILED;
        this.errorMessage = event.getErrorMessage();
        this.errorCode = event.getErrorCode();
        this.validationErrors = new ArrayList<>(event.getValidationErrors());
        this.failedAt = event.getFailedAt();
    }

    // Getters

    public ExportJobState getState() {
        return state;
    }

    public String getExportType() {
        return exportType;
    }

    public LocalDate getReportingPeriodStart() {
        return reportingPeriodStart;
    }

    public LocalDate getReportingPeriodEnd() {
        return reportingPeriodEnd;
    }

    public List<UUID> getIncludedProjectIds() {
        return new ArrayList<>(includedProjectIds);
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
    }

    public String getRequestedByUserName() {
        return requestedByUserName;
    }

    public String getCocCode() {
        return cocCode;
    }

    public String getBlobStorageUrl() {
        return blobStorageUrl;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public Long getVawaSupressedRecords() {
        return vawaSupressedRecords;
    }

    public List<String> getGeneratedCsvFiles() {
        return new ArrayList<>(generatedCsvFiles);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getExportReason() {
        return exportReason;
    }

    public String getRequestedBy() {
        return requestedByUserName;
    }

    public List<String> getStoredFiles() {
        return new ArrayList<>(generatedCsvFiles);
    }

    public Instant getQueuedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        // Started when transitioned to MATERIALIZING or PROCESSING
        return (state == ExportJobState.MATERIALIZING ||
                state == ExportJobState.VALIDATING ||
                state == ExportJobState.COMPLETE ||
                state == ExportJobState.FAILED) ? createdAt : null;
    }

    public String getDownloadUrl() {
        return blobStorageUrl;
    }
}
