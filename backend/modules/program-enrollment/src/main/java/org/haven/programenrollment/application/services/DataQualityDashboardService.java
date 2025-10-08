package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.ProjectLinkageRepository;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing data quality dashboard widgets and metrics
 */
@Service
@Transactional
public class DataQualityDashboardService {

    private final DataQualityAlertService alertService;
    private final ProjectLinkageRepository linkageRepository;
    private final ProgramEnrollmentRepository enrollmentRepository;

    public DataQualityDashboardService(DataQualityAlertService alertService,
                                      ProjectLinkageRepository linkageRepository,
                                      ProgramEnrollmentRepository enrollmentRepository) {
        this.alertService = alertService;
        this.linkageRepository = linkageRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Get comprehensive dashboard overview
     */
    @Transactional(readOnly = true)
    public DashboardOverview getDashboardOverview() {
        // Get alert statistics
        DataQualityAlertService.AlertStatistics alertStats = alertService.getAlertStatistics();

        // Get linkage statistics
        LinkageStatistics linkageStats = getLinkageStatistics();

        // Get violation trends
        List<ViolationTrend> trends = getViolationTrends();

        // Get top violations by type
        Map<DataQualityAlert.AlertType, Long> violationsByType = getViolationsByType();

        return new DashboardOverview(
            alertStats,
            linkageStats,
            trends,
            violationsByType,
            LocalDate.now()
        );
    }

    /**
     * Update dashboard metrics after data quality job execution
     */
    public void updateDataQualityMetrics(ThRrhDataQualityJob.DataQualityJobResult jobResult) {
        // In a real implementation, this would update a metrics store or cache
        // For now, we'll just log the update
        System.out.printf("Updated dashboard metrics: %d total violations, %d new alerts%n",
            jobResult.getTotalViolations(), jobResult.getNewAlerts().size());
    }

    /**
     * Get widget data for alerts summary
     */
    @Transactional(readOnly = true)
    public AlertsSummaryWidget getAlertsSummaryWidget() {
        List<DataQualityAlert> unresolvedAlerts = alertService.getUnresolvedAlerts();

        long highSeverityCount = unresolvedAlerts.stream()
            .filter(alert -> alert.getSeverity() == DataQualityAlert.Severity.HIGH)
            .count();

        long mediumSeverityCount = unresolvedAlerts.stream()
            .filter(alert -> alert.getSeverity() == DataQualityAlert.Severity.MEDIUM)
            .count();

        long lowSeverityCount = unresolvedAlerts.stream()
            .filter(alert -> alert.getSeverity() == DataQualityAlert.Severity.LOW)
            .count();

        return new AlertsSummaryWidget(
            unresolvedAlerts.size(),
            highSeverityCount,
            mediumSeverityCount,
            lowSeverityCount
        );
    }

    /**
     * Get widget data for violation trends
     */
    @Transactional(readOnly = true)
    public ViolationTrendsWidget getViolationTrendsWidget() {
        List<ViolationTrend> trends = getViolationTrends();
        return new ViolationTrendsWidget(trends);
    }

    /**
     * Get widget data for linkage health
     */
    @Transactional(readOnly = true)
    public LinkageHealthWidget getLinkageHealthWidget() {
        LinkageStatistics stats = getLinkageStatistics();

        // Calculate health score (0-100)
        double healthScore = calculateLinkageHealthScore(stats);

        return new LinkageHealthWidget(
            stats.getActiveLinkages(),
            stats.getTotalLinkages(),
            healthScore,
            getHealthStatus(healthScore)
        );
    }

    private LinkageStatistics getLinkageStatistics() {
        long activeLinkages = linkageRepository.countActiveLinkages();
        // In a real implementation, you'd have methods to count total, expired, etc.
        long totalLinkages = activeLinkages; // Simplified for now

        return new LinkageStatistics(
            activeLinkages,
            totalLinkages,
            0L, // expiredLinkages
            0L  // revokedLinkages
        );
    }

    private List<ViolationTrend> getViolationTrends() {
        // In a real implementation, this would query historical violation data
        // For now, return empty list
        return List.of();
    }

    private Map<DataQualityAlert.AlertType, Long> getViolationsByType() {
        List<DataQualityAlert> unresolvedAlerts = alertService.getUnresolvedAlerts();

        return unresolvedAlerts.stream()
            .collect(Collectors.groupingBy(
                DataQualityAlert::getAlertType,
                Collectors.counting()
            ));
    }

    private double calculateLinkageHealthScore(LinkageStatistics stats) {
        if (stats.getTotalLinkages() == 0) {
            return 100.0; // No linkages = no problems
        }

        // Simple health calculation based on active vs total ratio
        double activeRatio = (double) stats.getActiveLinkages() / stats.getTotalLinkages();
        return activeRatio * 100.0;
    }

    private String getHealthStatus(double healthScore) {
        if (healthScore >= 90) return "Excellent";
        if (healthScore >= 75) return "Good";
        if (healthScore >= 60) return "Fair";
        if (healthScore >= 40) return "Poor";
        return "Critical";
    }

    // Dashboard data classes
    public static class DashboardOverview {
        private DataQualityAlertService.AlertStatistics alertStatistics;
        private LinkageStatistics linkageStatistics;
        private List<ViolationTrend> violationTrends;
        private Map<DataQualityAlert.AlertType, Long> violationsByType;
        private LocalDate asOfDate;

        public DashboardOverview(DataQualityAlertService.AlertStatistics alertStatistics,
                                LinkageStatistics linkageStatistics,
                                List<ViolationTrend> violationTrends,
                                Map<DataQualityAlert.AlertType, Long> violationsByType,
                                LocalDate asOfDate) {
            this.alertStatistics = alertStatistics;
            this.linkageStatistics = linkageStatistics;
            this.violationTrends = violationTrends;
            this.violationsByType = violationsByType;
            this.asOfDate = asOfDate;
        }

        // Getters
        public DataQualityAlertService.AlertStatistics getAlertStatistics() { return alertStatistics; }
        public LinkageStatistics getLinkageStatistics() { return linkageStatistics; }
        public List<ViolationTrend> getViolationTrends() { return violationTrends; }
        public Map<DataQualityAlert.AlertType, Long> getViolationsByType() { return violationsByType; }
        public LocalDate getAsOfDate() { return asOfDate; }
    }

    public static class LinkageStatistics {
        private long activeLinkages;
        private long totalLinkages;
        private long expiredLinkages;
        private long revokedLinkages;

        public LinkageStatistics(long activeLinkages, long totalLinkages,
                                long expiredLinkages, long revokedLinkages) {
            this.activeLinkages = activeLinkages;
            this.totalLinkages = totalLinkages;
            this.expiredLinkages = expiredLinkages;
            this.revokedLinkages = revokedLinkages;
        }

        // Getters
        public long getActiveLinkages() { return activeLinkages; }
        public long getTotalLinkages() { return totalLinkages; }
        public long getExpiredLinkages() { return expiredLinkages; }
        public long getRevokedLinkages() { return revokedLinkages; }
    }

    public static class ViolationTrend {
        private LocalDate date;
        private DataQualityAlert.AlertType alertType;
        private long count;

        public ViolationTrend(LocalDate date, DataQualityAlert.AlertType alertType, long count) {
            this.date = date;
            this.alertType = alertType;
            this.count = count;
        }

        // Getters
        public LocalDate getDate() { return date; }
        public DataQualityAlert.AlertType getAlertType() { return alertType; }
        public long getCount() { return count; }
    }

    public static class AlertsSummaryWidget {
        private long totalUnresolvedAlerts;
        private long highSeverityCount;
        private long mediumSeverityCount;
        private long lowSeverityCount;

        public AlertsSummaryWidget(long totalUnresolvedAlerts, long highSeverityCount,
                                  long mediumSeverityCount, long lowSeverityCount) {
            this.totalUnresolvedAlerts = totalUnresolvedAlerts;
            this.highSeverityCount = highSeverityCount;
            this.mediumSeverityCount = mediumSeverityCount;
            this.lowSeverityCount = lowSeverityCount;
        }

        // Getters
        public long getTotalUnresolvedAlerts() { return totalUnresolvedAlerts; }
        public long getHighSeverityCount() { return highSeverityCount; }
        public long getMediumSeverityCount() { return mediumSeverityCount; }
        public long getLowSeverityCount() { return lowSeverityCount; }
    }

    public static class ViolationTrendsWidget {
        private List<ViolationTrend> trends;

        public ViolationTrendsWidget(List<ViolationTrend> trends) {
            this.trends = trends;
        }

        public List<ViolationTrend> getTrends() { return trends; }
    }

    public static class LinkageHealthWidget {
        private long activeLinkages;
        private long totalLinkages;
        private double healthScore;
        private String healthStatus;

        public LinkageHealthWidget(long activeLinkages, long totalLinkages,
                                  double healthScore, String healthStatus) {
            this.activeLinkages = activeLinkages;
            this.totalLinkages = totalLinkages;
            this.healthScore = healthScore;
            this.healthStatus = healthStatus;
        }

        // Getters
        public long getActiveLinkages() { return activeLinkages; }
        public long getTotalLinkages() { return totalLinkages; }
        public double getHealthScore() { return healthScore; }
        public String getHealthStatus() { return healthStatus; }
    }
}