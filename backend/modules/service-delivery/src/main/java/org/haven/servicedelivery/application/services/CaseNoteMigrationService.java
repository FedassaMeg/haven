package org.haven.servicedelivery.application.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing the migration of historical case notes to service episodes
 */
@Service
@Transactional
public class CaseNoteMigrationService {

    private final JdbcTemplate jdbcTemplate;

    public CaseNoteMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Analyze all case notes and prepare migration plan
     */
    public MigrationAnalysisResult analyzeCaseNotesForMigration() {
        // Run the analysis function
        Integer processedCount = jdbcTemplate.queryForObject(
            "SELECT analyze_case_notes_for_migration()",
            Integer.class
        );

        // Get the analysis results
        Map<String, Object> report = jdbcTemplate.queryForMap(
            "SELECT * FROM generate_migration_report()"
        );

        return MigrationAnalysisResult.builder()
            .processedCount(processedCount)
            .totalCaseNotes(((Number) report.get("total_case_notes")).longValue())
            .highConfidence(((Number) report.get("high_confidence")).longValue())
            .mediumConfidence(((Number) report.get("medium_confidence")).longValue())
            .lowConfidence(((Number) report.get("low_confidence")).longValue())
            .requiresReview(((Number) report.get("requires_review")).longValue())
            .completedMigrations(((Number) report.get("completed_migrations")).longValue())
            .pendingMigrations(((Number) report.get("pending_migrations")).longValue())
            .failedMigrations(((Number) report.get("failed_migrations")).longValue())
            .serviceTypeBreakdown((String) report.get("service_type_breakdown"))
            .build();
    }

    /**
     * Execute migration for approved records in batches
     */
    public MigrationExecutionResult executeMigrationBatch(int batchSize) {
        Integer createdCount = jdbcTemplate.queryForObject(
            "SELECT execute_case_note_migration(?)",
            Integer.class,
            batchSize
        );

        return MigrationExecutionResult.builder()
            .createdServiceEpisodes(createdCount)
            .batchSize(batchSize)
            .executionTime(LocalDateTime.now())
            .build();
    }

    /**
     * Get migration summary report
     */
    public MigrationReport getMigrationReport() {
        Map<String, Object> report = jdbcTemplate.queryForMap(
            "SELECT * FROM generate_migration_report()"
        );

        return MigrationReport.builder()
            .totalCaseNotes(((Number) report.get("total_case_notes")).longValue())
            .highConfidence(((Number) report.get("high_confidence")).longValue())
            .mediumConfidence(((Number) report.get("medium_confidence")).longValue())
            .lowConfidence(((Number) report.get("low_confidence")).longValue())
            .requiresReview(((Number) report.get("requires_review")).longValue())
            .completedMigrations(((Number) report.get("completed_migrations")).longValue())
            .pendingMigrations(((Number) report.get("pending_migrations")).longValue())
            .failedMigrations(((Number) report.get("failed_migrations")).longValue())
            .serviceTypeBreakdown((String) report.get("service_type_breakdown"))
            .build();
    }

    /**
     * Get records requiring manual review
     */
    public List<ManualReviewItem> getManualReviewItems() {
        return jdbcTemplate.query(
            """
            SELECT id, case_note_id, mapping_confidence, detected_service_type,
                   detected_duration_minutes, manual_review_reason, note_content,
                   note_date, client_id, content_length
            FROM migration_manual_review
            ORDER BY note_date DESC
            LIMIT 100
            """,
            (rs, rowNum) -> ManualReviewItem.builder()
                .id(UUID.fromString(rs.getString("id")))
                .caseNoteId(UUID.fromString(rs.getString("case_note_id")))
                .mappingConfidence(rs.getString("mapping_confidence"))
                .detectedServiceType(rs.getString("detected_service_type"))
                .detectedDurationMinutes(rs.getInt("detected_duration_minutes"))
                .reviewReason(rs.getString("manual_review_reason"))
                .noteContent(rs.getString("note_content"))
                .noteDate(rs.getTimestamp("note_date").toLocalDateTime())
                .clientId(UUID.fromString(rs.getString("client_id")))
                .contentLength(rs.getInt("content_length"))
                .build()
        );
    }

    /**
     * Approve a manual review item for migration
     */
    public void approveManualReviewItem(UUID migrationId, String approvedServiceType,
                                       Integer approvedDuration, String notes) {
        jdbcTemplate.update(
            """
            UPDATE case_note_migration_log
            SET manual_review_required = false,
                detected_service_type = ?,
                detected_duration_minutes = ?,
                migration_notes = COALESCE(migration_notes, '') || ' | Manual approval: ' || ?
            WHERE id = ?
            """,
            approvedServiceType, approvedDuration, notes, migrationId
        );
    }

    /**
     * Reject a manual review item
     */
    public void rejectManualReviewItem(UUID migrationId, String rejectionReason) {
        jdbcTemplate.update(
            """
            UPDATE case_note_migration_log
            SET migration_status = 'rejected',
                migration_notes = COALESCE(migration_notes, '') || ' | Rejected: ' || ?
            WHERE id = ?
            """,
            rejectionReason, migrationId
        );
    }

    /**
     * Get migration statistics by time period
     */
    public List<MigrationStatsByPeriod> getMigrationStatsByPeriod() {
        return jdbcTemplate.query(
            """
            SELECT
                DATE_TRUNC('month', cn.created_at) as period,
                COUNT(*) as total_notes,
                COUNT(CASE WHEN ml.mapping_confidence = 'high' THEN 1 END) as high_confidence,
                COUNT(CASE WHEN ml.mapping_confidence = 'medium' THEN 1 END) as medium_confidence,
                COUNT(CASE WHEN ml.mapping_confidence = 'low' THEN 1 END) as low_confidence,
                COUNT(CASE WHEN ml.migration_status = 'completed' THEN 1 END) as migrated
            FROM case_note_migration_log ml
            JOIN case_notes cn ON ml.case_note_id = cn.id
            GROUP BY DATE_TRUNC('month', cn.created_at)
            ORDER BY period DESC
            LIMIT 24
            """,
            (rs, rowNum) -> MigrationStatsByPeriod.builder()
                .period(rs.getTimestamp("period").toLocalDateTime())
                .totalNotes(rs.getLong("total_notes"))
                .highConfidence(rs.getLong("high_confidence"))
                .mediumConfidence(rs.getLong("medium_confidence"))
                .lowConfidence(rs.getLong("low_confidence"))
                .migrated(rs.getLong("migrated"))
                .build()
        );
    }

    /**
     * Get problematic cases that failed migration
     */
    public List<FailedMigrationItem> getFailedMigrations() {
        return jdbcTemplate.query(
            """
            SELECT ml.id, ml.case_note_id, ml.migration_notes, ml.detected_service_type,
                   cn.content, cn.created_at, cr.client_id
            FROM case_note_migration_log ml
            JOIN case_notes cn ON ml.case_note_id = cn.id
            JOIN case_records cr ON cn.case_record_id = cr.id
            WHERE ml.migration_status = 'failed'
            ORDER BY cn.created_at DESC
            """,
            (rs, rowNum) -> FailedMigrationItem.builder()
                .id(UUID.fromString(rs.getString("id")))
                .caseNoteId(UUID.fromString(rs.getString("case_note_id")))
                .migrationNotes(rs.getString("migration_notes"))
                .detectedServiceType(rs.getString("detected_service_type"))
                .noteContent(rs.getString("content"))
                .noteDate(rs.getTimestamp("created_at").toLocalDateTime())
                .clientId(UUID.fromString(rs.getString("client_id")))
                .build()
        );
    }

    /**
     * Retry failed migrations
     */
    public void retryFailedMigrations() {
        jdbcTemplate.update(
            """
            UPDATE case_note_migration_log
            SET migration_status = 'pending',
                migration_notes = COALESCE(migration_notes, '') || ' | Retry attempted at ' || CURRENT_TIMESTAMP
            WHERE migration_status = 'failed'
            """
        );
    }

    // Record classes for responses
    public static class MigrationAnalysisResult {
        private final Integer processedCount;
        private final Long totalCaseNotes;
        private final Long highConfidence;
        private final Long mediumConfidence;
        private final Long lowConfidence;
        private final Long requiresReview;
        private final Long completedMigrations;
        private final Long pendingMigrations;
        private final Long failedMigrations;
        private final String serviceTypeBreakdown;

        private MigrationAnalysisResult(Builder builder) {
            this.processedCount = builder.processedCount;
            this.totalCaseNotes = builder.totalCaseNotes;
            this.highConfidence = builder.highConfidence;
            this.mediumConfidence = builder.mediumConfidence;
            this.lowConfidence = builder.lowConfidence;
            this.requiresReview = builder.requiresReview;
            this.completedMigrations = builder.completedMigrations;
            this.pendingMigrations = builder.pendingMigrations;
            this.failedMigrations = builder.failedMigrations;
            this.serviceTypeBreakdown = builder.serviceTypeBreakdown;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Integer processedCount;
            private Long totalCaseNotes;
            private Long highConfidence;
            private Long mediumConfidence;
            private Long lowConfidence;
            private Long requiresReview;
            private Long completedMigrations;
            private Long pendingMigrations;
            private Long failedMigrations;
            private String serviceTypeBreakdown;

            public Builder processedCount(Integer processedCount) { this.processedCount = processedCount; return this; }
            public Builder totalCaseNotes(Long totalCaseNotes) { this.totalCaseNotes = totalCaseNotes; return this; }
            public Builder highConfidence(Long highConfidence) { this.highConfidence = highConfidence; return this; }
            public Builder mediumConfidence(Long mediumConfidence) { this.mediumConfidence = mediumConfidence; return this; }
            public Builder lowConfidence(Long lowConfidence) { this.lowConfidence = lowConfidence; return this; }
            public Builder requiresReview(Long requiresReview) { this.requiresReview = requiresReview; return this; }
            public Builder completedMigrations(Long completedMigrations) { this.completedMigrations = completedMigrations; return this; }
            public Builder pendingMigrations(Long pendingMigrations) { this.pendingMigrations = pendingMigrations; return this; }
            public Builder failedMigrations(Long failedMigrations) { this.failedMigrations = failedMigrations; return this; }
            public Builder serviceTypeBreakdown(String serviceTypeBreakdown) { this.serviceTypeBreakdown = serviceTypeBreakdown; return this; }
            public MigrationAnalysisResult build() { return new MigrationAnalysisResult(this); }
        }

        // Getters
        public Integer getProcessedCount() { return processedCount; }
        public Long getTotalCaseNotes() { return totalCaseNotes; }
        public Long getHighConfidence() { return highConfidence; }
        public Long getMediumConfidence() { return mediumConfidence; }
        public Long getLowConfidence() { return lowConfidence; }
        public Long getRequiresReview() { return requiresReview; }
        public Long getCompletedMigrations() { return completedMigrations; }
        public Long getPendingMigrations() { return pendingMigrations; }
        public Long getFailedMigrations() { return failedMigrations; }
        public String getServiceTypeBreakdown() { return serviceTypeBreakdown; }
    }

    public static class MigrationExecutionResult {
        private final Integer createdServiceEpisodes;
        private final Integer batchSize;
        private final LocalDateTime executionTime;

        private MigrationExecutionResult(Builder builder) {
            this.createdServiceEpisodes = builder.createdServiceEpisodes;
            this.batchSize = builder.batchSize;
            this.executionTime = builder.executionTime;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Integer createdServiceEpisodes;
            private Integer batchSize;
            private LocalDateTime executionTime;

            public Builder createdServiceEpisodes(Integer createdServiceEpisodes) { this.createdServiceEpisodes = createdServiceEpisodes; return this; }
            public Builder batchSize(Integer batchSize) { this.batchSize = batchSize; return this; }
            public Builder executionTime(LocalDateTime executionTime) { this.executionTime = executionTime; return this; }
            public MigrationExecutionResult build() { return new MigrationExecutionResult(this); }
        }

        // Getters
        public Integer getCreatedServiceEpisodes() { return createdServiceEpisodes; }
        public Integer getBatchSize() { return batchSize; }
        public LocalDateTime getExecutionTime() { return executionTime; }
    }

    public static class MigrationReport {
        private final Long totalCaseNotes;
        private final Long highConfidence;
        private final Long mediumConfidence;
        private final Long lowConfidence;
        private final Long requiresReview;
        private final Long completedMigrations;
        private final Long pendingMigrations;
        private final Long failedMigrations;
        private final String serviceTypeBreakdown;

        private MigrationReport(Builder builder) {
            this.totalCaseNotes = builder.totalCaseNotes;
            this.highConfidence = builder.highConfidence;
            this.mediumConfidence = builder.mediumConfidence;
            this.lowConfidence = builder.lowConfidence;
            this.requiresReview = builder.requiresReview;
            this.completedMigrations = builder.completedMigrations;
            this.pendingMigrations = builder.pendingMigrations;
            this.failedMigrations = builder.failedMigrations;
            this.serviceTypeBreakdown = builder.serviceTypeBreakdown;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long totalCaseNotes;
            private Long highConfidence;
            private Long mediumConfidence;
            private Long lowConfidence;
            private Long requiresReview;
            private Long completedMigrations;
            private Long pendingMigrations;
            private Long failedMigrations;
            private String serviceTypeBreakdown;

            public Builder totalCaseNotes(Long totalCaseNotes) { this.totalCaseNotes = totalCaseNotes; return this; }
            public Builder highConfidence(Long highConfidence) { this.highConfidence = highConfidence; return this; }
            public Builder mediumConfidence(Long mediumConfidence) { this.mediumConfidence = mediumConfidence; return this; }
            public Builder lowConfidence(Long lowConfidence) { this.lowConfidence = lowConfidence; return this; }
            public Builder requiresReview(Long requiresReview) { this.requiresReview = requiresReview; return this; }
            public Builder completedMigrations(Long completedMigrations) { this.completedMigrations = completedMigrations; return this; }
            public Builder pendingMigrations(Long pendingMigrations) { this.pendingMigrations = pendingMigrations; return this; }
            public Builder failedMigrations(Long failedMigrations) { this.failedMigrations = failedMigrations; return this; }
            public Builder serviceTypeBreakdown(String serviceTypeBreakdown) { this.serviceTypeBreakdown = serviceTypeBreakdown; return this; }
            public MigrationReport build() { return new MigrationReport(this); }
        }

        // Getters
        public Long getTotalCaseNotes() { return totalCaseNotes; }
        public Long getHighConfidence() { return highConfidence; }
        public Long getMediumConfidence() { return mediumConfidence; }
        public Long getLowConfidence() { return lowConfidence; }
        public Long getRequiresReview() { return requiresReview; }
        public Long getCompletedMigrations() { return completedMigrations; }
        public Long getPendingMigrations() { return pendingMigrations; }
        public Long getFailedMigrations() { return failedMigrations; }
        public String getServiceTypeBreakdown() { return serviceTypeBreakdown; }
    }

    public static class ManualReviewItem {
        private final UUID id;
        private final UUID caseNoteId;
        private final String mappingConfidence;
        private final String detectedServiceType;
        private final Integer detectedDurationMinutes;
        private final String reviewReason;
        private final String noteContent;
        private final LocalDateTime noteDate;
        private final UUID clientId;
        private final Integer contentLength;

        private ManualReviewItem(Builder builder) {
            this.id = builder.id;
            this.caseNoteId = builder.caseNoteId;
            this.mappingConfidence = builder.mappingConfidence;
            this.detectedServiceType = builder.detectedServiceType;
            this.detectedDurationMinutes = builder.detectedDurationMinutes;
            this.reviewReason = builder.reviewReason;
            this.noteContent = builder.noteContent;
            this.noteDate = builder.noteDate;
            this.clientId = builder.clientId;
            this.contentLength = builder.contentLength;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private UUID id;
            private UUID caseNoteId;
            private String mappingConfidence;
            private String detectedServiceType;
            private Integer detectedDurationMinutes;
            private String reviewReason;
            private String noteContent;
            private LocalDateTime noteDate;
            private UUID clientId;
            private Integer contentLength;

            public Builder id(UUID id) { this.id = id; return this; }
            public Builder caseNoteId(UUID caseNoteId) { this.caseNoteId = caseNoteId; return this; }
            public Builder mappingConfidence(String mappingConfidence) { this.mappingConfidence = mappingConfidence; return this; }
            public Builder detectedServiceType(String detectedServiceType) { this.detectedServiceType = detectedServiceType; return this; }
            public Builder detectedDurationMinutes(Integer detectedDurationMinutes) { this.detectedDurationMinutes = detectedDurationMinutes; return this; }
            public Builder reviewReason(String reviewReason) { this.reviewReason = reviewReason; return this; }
            public Builder noteContent(String noteContent) { this.noteContent = noteContent; return this; }
            public Builder noteDate(LocalDateTime noteDate) { this.noteDate = noteDate; return this; }
            public Builder clientId(UUID clientId) { this.clientId = clientId; return this; }
            public Builder contentLength(Integer contentLength) { this.contentLength = contentLength; return this; }
            public ManualReviewItem build() { return new ManualReviewItem(this); }
        }

        // Getters
        public UUID getId() { return id; }
        public UUID getCaseNoteId() { return caseNoteId; }
        public String getMappingConfidence() { return mappingConfidence; }
        public String getDetectedServiceType() { return detectedServiceType; }
        public Integer getDetectedDurationMinutes() { return detectedDurationMinutes; }
        public String getReviewReason() { return reviewReason; }
        public String getNoteContent() { return noteContent; }
        public LocalDateTime getNoteDate() { return noteDate; }
        public UUID getClientId() { return clientId; }
        public Integer getContentLength() { return contentLength; }
    }

    public static class MigrationStatsByPeriod {
        private final LocalDateTime period;
        private final Long totalNotes;
        private final Long highConfidence;
        private final Long mediumConfidence;
        private final Long lowConfidence;
        private final Long migrated;

        private MigrationStatsByPeriod(Builder builder) {
            this.period = builder.period;
            this.totalNotes = builder.totalNotes;
            this.highConfidence = builder.highConfidence;
            this.mediumConfidence = builder.mediumConfidence;
            this.lowConfidence = builder.lowConfidence;
            this.migrated = builder.migrated;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private LocalDateTime period;
            private Long totalNotes;
            private Long highConfidence;
            private Long mediumConfidence;
            private Long lowConfidence;
            private Long migrated;

            public Builder period(LocalDateTime period) { this.period = period; return this; }
            public Builder totalNotes(Long totalNotes) { this.totalNotes = totalNotes; return this; }
            public Builder highConfidence(Long highConfidence) { this.highConfidence = highConfidence; return this; }
            public Builder mediumConfidence(Long mediumConfidence) { this.mediumConfidence = mediumConfidence; return this; }
            public Builder lowConfidence(Long lowConfidence) { this.lowConfidence = lowConfidence; return this; }
            public Builder migrated(Long migrated) { this.migrated = migrated; return this; }
            public MigrationStatsByPeriod build() { return new MigrationStatsByPeriod(this); }
        }

        // Getters
        public LocalDateTime getPeriod() { return period; }
        public Long getTotalNotes() { return totalNotes; }
        public Long getHighConfidence() { return highConfidence; }
        public Long getMediumConfidence() { return mediumConfidence; }
        public Long getLowConfidence() { return lowConfidence; }
        public Long getMigrated() { return migrated; }
    }

    public static class FailedMigrationItem {
        private final UUID id;
        private final UUID caseNoteId;
        private final String migrationNotes;
        private final String detectedServiceType;
        private final String noteContent;
        private final LocalDateTime noteDate;
        private final UUID clientId;

        private FailedMigrationItem(Builder builder) {
            this.id = builder.id;
            this.caseNoteId = builder.caseNoteId;
            this.migrationNotes = builder.migrationNotes;
            this.detectedServiceType = builder.detectedServiceType;
            this.noteContent = builder.noteContent;
            this.noteDate = builder.noteDate;
            this.clientId = builder.clientId;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private UUID id;
            private UUID caseNoteId;
            private String migrationNotes;
            private String detectedServiceType;
            private String noteContent;
            private LocalDateTime noteDate;
            private UUID clientId;

            public Builder id(UUID id) { this.id = id; return this; }
            public Builder caseNoteId(UUID caseNoteId) { this.caseNoteId = caseNoteId; return this; }
            public Builder migrationNotes(String migrationNotes) { this.migrationNotes = migrationNotes; return this; }
            public Builder detectedServiceType(String detectedServiceType) { this.detectedServiceType = detectedServiceType; return this; }
            public Builder noteContent(String noteContent) { this.noteContent = noteContent; return this; }
            public Builder noteDate(LocalDateTime noteDate) { this.noteDate = noteDate; return this; }
            public Builder clientId(UUID clientId) { this.clientId = clientId; return this; }
            public FailedMigrationItem build() { return new FailedMigrationItem(this); }
        }

        // Getters
        public UUID getId() { return id; }
        public UUID getCaseNoteId() { return caseNoteId; }
        public String getMigrationNotes() { return migrationNotes; }
        public String getDetectedServiceType() { return detectedServiceType; }
        public String getNoteContent() { return noteContent; }
        public LocalDateTime getNoteDate() { return noteDate; }
        public UUID getClientId() { return clientId; }
    }
}