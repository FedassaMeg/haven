package org.haven.reporting.application.services;

import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.programenrollment.domain.IntakePsdeRepository;
import org.haven.programenrollment.application.services.IntakePsdeAuditLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating HUD audit export reports for PSDE data
 * Implements comprehensive reporting requirements for federal compliance
 */
@Service
@Transactional(readOnly = true)
public class PsdeHudAuditExportService {

    private final IntakePsdeRepository psdeRepository;
    private final IntakePsdeAuditLogger auditLogger;

    public PsdeHudAuditExportService(
            IntakePsdeRepository psdeRepository,
            IntakePsdeAuditLogger auditLogger) {
        this.psdeRepository = psdeRepository;
        this.auditLogger = auditLogger;
    }

    /**
     * Generate comprehensive HUD audit export for specified reporting period
     */
    public HudAuditExportReport generateAuditExport(
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            String exportedBy) {

        // Log the export request
        auditLogger.logSystemEvent(
            "HUD_EXPORT_REQUEST",
            exportedBy,
            String.format("reportingPeriodStart=%s, reportingPeriodEnd=%s",
                reportingPeriodStart, reportingPeriodEnd)
        );

        // Get all PSDE records for the period
        List<IntakePsdeRecord> records = psdeRepository.findForHudAuditExport(
            reportingPeriodStart, reportingPeriodEnd);

        // Generate export data
        HudAuditExportReport report = new HudAuditExportReport(
            reportingPeriodStart,
            reportingPeriodEnd,
            Instant.now(),
            exportedBy,
            generateDataQualityMatrix(records),
            generateComplianceMatrix(records),
            generateCorrectionSummary(records),
            generateVawaComplianceSummary(records),
            generateRecordStatistics(records),
            generateHistoricalSnapshotSummary(records)
        );

        // Log the export completion
        boolean includesDvData = report.vawaCompliance().totalDvCases() > 0;
        auditLogger.logDataExport(
            exportedBy,
            "HUD_AUDIT_EXPORT",
            report.getTotalRecords(),
            includesDvData
        );
        auditLogger.logSystemEvent(
            "HUD_EXPORT_COMPLETED",
            exportedBy,
            String.format("records=%d, dataQuality=%.2f, compliance=%.2f",
                report.getTotalRecords(), report.getDataQualityScore(), report.getComplianceScore())
        );

        return report;
    }

    /**
     * Generate data quality matrix for HUD requirements
     */
    private DataQualityMatrix generateDataQualityMatrix(List<IntakePsdeRecord> records) {
        int totalRecords = records.size();
        int compliantRecords = 0;
        int incomeCompliant = 0;
        int disabilityCompliant = 0;
        int dvCompliant = 0;
        int healthInsuranceCompliant = 0;

        for (IntakePsdeRecord record : records) {
            boolean isCompliant = record.meetsHmisDataQuality();
            if (isCompliant) compliantRecords++;

            // Check individual data element compliance
            if (hasCompleteIncomeData(record)) incomeCompliant++;
            if (hasCompleteDisabilityData(record)) disabilityCompliant++;
            if (hasCompleteDvData(record)) dvCompliant++;
            if (hasCompleteHealthInsuranceData(record)) healthInsuranceCompliant++;
        }

        return new DataQualityMatrix(
            totalRecords,
            compliantRecords,
            calculatePercentage(compliantRecords, totalRecords),
            incomeCompliant,
            calculatePercentage(incomeCompliant, totalRecords),
            disabilityCompliant,
            calculatePercentage(disabilityCompliant, totalRecords),
            dvCompliant,
            calculatePercentage(dvCompliant, totalRecords),
            healthInsuranceCompliant,
            calculatePercentage(healthInsuranceCompliant, totalRecords)
        );
    }

    /**
     * Generate VAWA compliance matrix
     */
    private VawaComplianceMatrix generateVawaComplianceSummary(List<IntakePsdeRecord> records) {
        int totalDvCases = 0;
        int vawaProtectedCases = 0;
        int highSensitivityCases = 0;
        int properlyRedactedCases = 0;
        int confidentialityRequestedCases = 0;

        for (IntakePsdeRecord record : records) {
            if (hasDomesticViolenceData(record)) {
                totalDvCases++;

                if (record.isHighSensitivityDvCase()) {
                    highSensitivityCases++;
                }

                if (record.getVawaConfidentialityRequested() != null && record.getVawaConfidentialityRequested()) {
                    confidentialityRequestedCases++;
                }

                if (record.requiresDvRedaction()) {
                    vawaProtectedCases++;

                    if (isProperlyRedacted(record)) {
                        properlyRedactedCases++;
                    }
                }
            }
        }

        return new VawaComplianceMatrix(
            totalDvCases,
            vawaProtectedCases,
            highSensitivityCases,
            confidentialityRequestedCases,
            properlyRedactedCases,
            calculatePercentage(properlyRedactedCases, vawaProtectedCases)
        );
    }

    /**
     * Generate correction summary for audit trail
     */
    private CorrectionSummary generateCorrectionSummary(List<IntakePsdeRecord> records) {
        int totalCorrections = 0;
        int dataEntryCorrections = 0;
        int clientCorrections = 0;
        int auditCorrections = 0;
        int systemErrorCorrections = 0;

        Map<String, Integer> correctionsByReason = records.stream()
            .filter(r -> r.getIsCorrection() != null && r.getIsCorrection())
            .collect(Collectors.groupingBy(
                r -> r.getCorrectionReason() != null ? r.getCorrectionReason() : "UNKNOWN",
                Collectors.summingInt(r -> 1)
            ));

        totalCorrections = correctionsByReason.values().stream().mapToInt(Integer::intValue).sum();
        dataEntryCorrections = correctionsByReason.getOrDefault("DATA_ENTRY_ERROR", 0);
        clientCorrections = correctionsByReason.getOrDefault("CLIENT_CORRECTION", 0);
        auditCorrections = correctionsByReason.getOrDefault("AUDIT_FINDING", 0);
        systemErrorCorrections = correctionsByReason.getOrDefault("SYSTEM_ERROR", 0);

        return new CorrectionSummary(
            totalCorrections,
            dataEntryCorrections,
            clientCorrections,
            auditCorrections,
            systemErrorCorrections,
            calculatePercentage(totalCorrections, records.size())
        );
    }

    /**
     * Generate overall compliance matrix
     */
    private ComplianceMatrix generateComplianceMatrix(List<IntakePsdeRecord> records) {
        int totalRecords = records.size();
        int fullyCompliant = 0;
        int partiallyCompliant = 0;
        int nonCompliant = 0;

        for (IntakePsdeRecord record : records) {
            int complianceScore = calculateRecordComplianceScore(record);

            if (complianceScore >= 95) {
                fullyCompliant++;
            } else if (complianceScore >= 75) {
                partiallyCompliant++;
            } else {
                nonCompliant++;
            }
        }

        return new ComplianceMatrix(
            totalRecords,
            fullyCompliant,
            calculatePercentage(fullyCompliant, totalRecords),
            partiallyCompliant,
            calculatePercentage(partiallyCompliant, totalRecords),
            nonCompliant,
            calculatePercentage(nonCompliant, totalRecords),
            calculateOverallComplianceScore(records)
        );
    }

    /**
     * Generate record statistics summary
     */
    private RecordStatistics generateRecordStatistics(List<IntakePsdeRecord> records) {
        Map<String, Long> recordsByStage = records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getCollectionStage().toString(),
                Collectors.counting()
            ));

        Map<String, Long> recordsByStatus = records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getLifecycleStatus(),
                Collectors.counting()
            ));

        int activeRecords = recordsByStatus.getOrDefault("ACTIVE", 0L).intValue();
        int supersededRecords = recordsByStatus.getOrDefault("SUPERSEDED", 0L).intValue();
        int correctedRecords = recordsByStatus.getOrDefault("CORRECTED", 0L).intValue();

        return new RecordStatistics(
            records.size(),
            activeRecords,
            supersededRecords,
            correctedRecords,
            recordsByStage.getOrDefault("INITIAL_INTAKE", 0L).intValue(),
            recordsByStage.getOrDefault("COMPREHENSIVE_ASSESSMENT", 0L).intValue(),
            calculateAverageVersions(records)
        );
    }

    /**
     * Generate historical snapshot summary
     */
    private HistoricalSnapshotSummary generateHistoricalSnapshotSummary(List<IntakePsdeRecord> records) {
        int backdatedRecords = (int) records.stream()
            .filter(r -> r.getIsBackdated() != null && r.getIsBackdated())
            .count();

        List<IntakePsdeRecord> versionsChain = records.stream()
            .filter(r -> r.getVersion() != null && r.getVersion() > 1)
            .collect(Collectors.toList());

        int maxVersionsForSingleRecord = records.stream()
            .mapToInt(r -> r.getVersion() != null ? r.getVersion() : 1)
            .max()
            .orElse(1);

        return new HistoricalSnapshotSummary(
            backdatedRecords,
            versionsChain.size(),
            maxVersionsForSingleRecord,
            calculateAverageTimeBetweenVersions(records)
        );
    }

    // Helper methods for compliance checking
    private boolean hasCompleteIncomeData(IntakePsdeRecord record) {
        return record.getIncomeFromAnySource() != null &&
               !record.getIncomeFromAnySource().toString().contains("DATA_NOT_COLLECTED");
    }

    private boolean hasCompleteDisabilityData(IntakePsdeRecord record) {
        return record.getPhysicalDisability() != null &&
               record.getDevelopmentalDisability() != null &&
               record.getChronicHealthCondition() != null &&
               record.getHivAids() != null &&
               record.getMentalHealthDisorder() != null &&
               record.getSubstanceUseDisorder() != null;
    }

    private boolean hasCompleteDvData(IntakePsdeRecord record) {
        if (record.getDomesticViolence() == null) return false;

        // If DV = Yes, then recency should be collected
        if (record.getDomesticViolence().toString().contains("YES")) {
            return record.getDomesticViolenceRecency() != null &&
                   !record.getDomesticViolenceRecency().toString().contains("DATA_NOT_COLLECTED");
        }

        return true; // If DV = No, then it's complete
    }

    private boolean hasCompleteHealthInsuranceData(IntakePsdeRecord record) {
        return record.getCoveredByHealthInsurance() != null &&
               !record.getCoveredByHealthInsurance().toString().contains("DATA_NOT_COLLECTED");
    }

    private boolean hasDomesticViolenceData(IntakePsdeRecord record) {
        return record.getDomesticViolence() != null &&
               !record.getDomesticViolence().toString().contains("DATA_NOT_COLLECTED");
    }

    private boolean isProperlyRedacted(IntakePsdeRecord record) {
        return record.getDvRedactionLevel() != null &&
               !record.getDvRedactionLevel().toString().equals("NO_REDACTION");
    }

    private int calculateRecordComplianceScore(IntakePsdeRecord record) {
        int score = 0;
        int maxScore = 0;

        // HUD data quality (40 points)
        maxScore += 40;
        if (record.meetsHmisDataQuality()) score += 40;

        // VAWA compliance (30 points)
        maxScore += 30;
        if (!record.requiresDvRedaction() || isProperlyRedacted(record)) {
            score += 30;
        }

        // Data completeness (20 points)
        maxScore += 20;
        if (hasCompleteIncomeData(record)) score += 5;
        if (hasCompleteDisabilityData(record)) score += 5;
        if (hasCompleteDvData(record)) score += 5;
        if (hasCompleteHealthInsuranceData(record)) score += 5;

        // Lifecycle integrity (10 points)
        maxScore += 10;
        if (record.getLifecycleStatus() != null) {
            if (record.getLifecycleStatus().equals("ACTIVE")) score += 10;
            else if (record.getLifecycleStatus().equals("SUPERSEDED")) score += 8;
            else if (record.getLifecycleStatus().equals("CORRECTED")) score += 6;
        }

        return (int) Math.round((double) score / maxScore * 100);
    }

    private double calculateOverallComplianceScore(List<IntakePsdeRecord> records) {
        if (records.isEmpty()) return 0.0;

        double totalScore = records.stream()
            .mapToInt(this::calculateRecordComplianceScore)
            .average()
            .orElse(0.0);

        return Math.round(totalScore * 100.0) / 100.0;
    }

    private double calculateAverageVersions(List<IntakePsdeRecord> records) {
        if (records.isEmpty()) return 0.0;

        double average = records.stream()
            .mapToInt(r -> r.getVersion() != null ? r.getVersion() : 1)
            .average()
            .orElse(1.0);

        return Math.round(average * 100.0) / 100.0;
    }

    private double calculateAverageTimeBetweenVersions(List<IntakePsdeRecord> records) {
        // TODO: Implement calculation of average time between versions
        // This would require analyzing supersession chains and calculating time differences
        return 0.0;
    }

    private double calculatePercentage(int numerator, int denominator) {
        if (denominator == 0) return 0.0;
        return Math.round((double) numerator / denominator * 10000.0) / 100.0;
    }

    // Report data classes
    public record HudAuditExportReport(
        LocalDate reportingPeriodStart,
        LocalDate reportingPeriodEnd,
        Instant exportedAt,
        String exportedBy,
        DataQualityMatrix dataQuality,
        ComplianceMatrix compliance,
        CorrectionSummary corrections,
        VawaComplianceMatrix vawaCompliance,
        RecordStatistics statistics,
        HistoricalSnapshotSummary historicalSnapshots
    ) {
        public int getTotalRecords() {
            return statistics.totalRecords();
        }

        public double getDataQualityScore() {
            return dataQuality.overallQualityPercentage();
        }

        public double getComplianceScore() {
            return compliance.overallComplianceScore();
        }
    }

    public record DataQualityMatrix(
        int totalRecords,
        int compliantRecords,
        double overallQualityPercentage,
        int incomeCompliant,
        double incomeCompliancePercentage,
        int disabilityCompliant,
        double disabilityCompliancePercentage,
        int dvCompliant,
        double dvCompliancePercentage,
        int healthInsuranceCompliant,
        double healthInsuranceCompliancePercentage
    ) {}

    public record ComplianceMatrix(
        int totalRecords,
        int fullyCompliant,
        double fullyCompliantPercentage,
        int partiallyCompliant,
        double partiallyCompliantPercentage,
        int nonCompliant,
        double nonCompliantPercentage,
        double overallComplianceScore
    ) {}

    public record CorrectionSummary(
        int totalCorrections,
        int dataEntryCorrections,
        int clientCorrections,
        int auditCorrections,
        int systemErrorCorrections,
        double correctionRate
    ) {}

    public record VawaComplianceMatrix(
        int totalDvCases,
        int vawaProtectedCases,
        int highSensitivityCases,
        int confidentialityRequestedCases,
        int properlyRedactedCases,
        double vawaCompliancePercentage
    ) {}

    public record RecordStatistics(
        int totalRecords,
        int activeRecords,
        int supersededRecords,
        int correctedRecords,
        int initialIntakeRecords,
        int comprehensiveAssessmentRecords,
        double averageVersionsPerRecord
    ) {}

    public record HistoricalSnapshotSummary(
        int backdatedRecords,
        int recordsWithMultipleVersions,
        int maxVersionsForSingleRecord,
        double averageTimeBetweenVersionsHours
    ) {}
}