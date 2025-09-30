package org.haven.clientprofile.application.dto;

/**
 * Response DTO for consent statistics dashboard
 */
public record ConsentStatisticsResponse(
    long totalConsents,
    long activeConsents,
    long revokedConsents,
    long expiredConsents,
    long expiringSoonCount,
    long vAWAProtectedCount,
    ConsentTypeStatistics[] typeBreakdown,
    RecentActivitySummary recentActivity
) {
    
    public record ConsentTypeStatistics(
        String consentType,
        long totalCount,
        long activeCount
    ) {}
    
    public record RecentActivitySummary(
        long consentsGrantedLast30Days,
        long consentsRevokedLast30Days,
        long consentsExpiredLast30Days
    ) {}
}