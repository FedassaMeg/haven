package org.haven.reporting.application.services;

import org.haven.reporting.domain.AggregationResult;
import org.haven.reporting.domain.ExportPeriod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Computes aggregated metrics for CoC APR and SPM reports.
 * Applies cell suppression rules for protected categories (n<5).
 */
@Service
public class AggregationService {

    private static final int SUPPRESSION_THRESHOLD = 5;
    private static final String SUPPRESSED_VALUE = "*";

    private final JdbcTemplate jdbcTemplate;

    public AggregationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * CoC APR Q6: Household Type by Household
     * Counts households by composition (adults only, adults & children, children only)
     */
    public Map<String, Object> computeCoCAPRQ6(Set<String> projectIds, ExportPeriod period) {
        String sql = """
            SELECT
                CASE
                    WHEN COUNT(CASE WHEN adult = 1 THEN 1 END) > 0
                         AND COUNT(CASE WHEN adult = 0 THEN 1 END) > 0 THEN 'Adults and Children'
                    WHEN COUNT(CASE WHEN adult = 1 THEN 1 END) > 0 THEN 'Adults Only'
                    WHEN COUNT(CASE WHEN adult = 0 THEN 1 END) > 0 THEN 'Children Only'
                END as household_type,
                COUNT(DISTINCT household_id) as household_count
            FROM (
                SELECT
                    e.household_id,
                    CASE WHEN TIMESTAMPDIFF(YEAR, c.dob, e.entry_date) >= 18 THEN 1 ELSE 0 END as adult
                FROM enrollments e
                JOIN clients c ON e.personal_id = c.personal_id
                WHERE e.project_id IN (?)
                  AND e.entry_date BETWEEN ? AND ?
                  AND (e.exit_date IS NULL OR e.exit_date >= ?)
            ) household_members
            GROUP BY household_id
            HAVING household_type IS NOT NULL
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                sql,
                String.join(",", projectIds),
                period.startDate(),
                period.endDate(),
                period.startDate()
        );

        Map<String, Object> aggregated = new HashMap<>();
        aggregated.put("adultsOnly", suppressIfNeeded(getCount(results, "Adults Only")));
        aggregated.put("adultsAndChildren", suppressIfNeeded(getCount(results, "Adults and Children")));
        aggregated.put("childrenOnly", suppressIfNeeded(getCount(results, "Children Only")));

        return aggregated;
    }

    /**
     * CoC APR Q7: Veteran Status
     * Counts adults by veteran status
     */
    public Map<String, Object> computeCoCAPRQ7(Set<String> projectIds, ExportPeriod period) {
        String sql = """
            SELECT
                COALESCE(c.veteran_status, 0) as is_veteran,
                COUNT(DISTINCT e.personal_id) as client_count
            FROM enrollments e
            JOIN clients c ON e.personal_id = c.personal_id
            WHERE e.project_id IN (?)
              AND e.entry_date BETWEEN ? AND ?
              AND (e.exit_date IS NULL OR e.exit_date >= ?)
              AND TIMESTAMPDIFF(YEAR, c.dob, e.entry_date) >= 18
            GROUP BY is_veteran
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                sql,
                String.join(",", projectIds),
                period.startDate(),
                period.endDate(),
                period.startDate()
        );

        Map<String, Object> aggregated = new HashMap<>();
        aggregated.put("veterans", suppressIfNeeded(getCount(results, 1)));
        aggregated.put("nonVeterans", suppressIfNeeded(getCount(results, 0)));

        return aggregated;
    }

    /**
     * CoC APR Q10: Income Sources at Start and Annual Assessment/Exit
     * Tracks changes in income sources over enrollment
     */
    public Map<String, Object> computeCoCAPRQ10(Set<String> projectIds, ExportPeriod period) {
        // Income sources: earned, unemployment, SSI, SSDI, VA, etc.
        String[] incomeSources = {
                "earned", "unemployment", "ssi", "ssdi",
                "va_disability_service", "va_disability_non_service",
                "private_disability", "workers_comp", "tanf", "ga",
                "soc_sec_retirement", "pension", "child_support", "alimony", "other_income"
        };

        Map<String, Object> results = new HashMap<>();

        for (String source : incomeSources) {
            String sql = String.format("""
                SELECT
                    stage,
                    COUNT(DISTINCT personal_id) as count_with_source
                FROM (
                    SELECT
                        ib.personal_id,
                        CASE
                            WHEN ib.data_collection_stage = 1 THEN 'entry'
                            WHEN ib.data_collection_stage = 3 THEN 'exit'
                            WHEN ib.data_collection_stage = 5 THEN 'annual'
                        END as stage
                    FROM income_benefits ib
                    JOIN enrollments e ON ib.enrollment_id = e.enrollment_id
                    WHERE e.project_id IN (?)
                      AND ib.information_date BETWEEN ? AND ?
                      AND ib.%s = 1
                ) income_by_stage
                WHERE stage IS NOT NULL
                GROUP BY stage
                """, source);

            List<Map<String, Object>> sourceResults = jdbcTemplate.queryForList(
                    sql,
                    String.join(",", projectIds),
                    period.startDate(),
                    period.endDate()
            );

            Map<String, Object> sourceData = new HashMap<>();
            sourceData.put("atEntry", suppressIfNeeded(getCount(sourceResults, "entry")));
            sourceData.put("atExit", suppressIfNeeded(getCount(sourceResults, "exit")));
            sourceData.put("atAnnual", suppressIfNeeded(getCount(sourceResults, "annual")));

            results.put(source, sourceData);
        }

        return results;
    }

    /**
     * SPM Metric 1: Returns to Homelessness (2-year lookback)
     * Percentage of persons who exit to permanent housing and return within 6, 12, 24 months
     */
    public Map<String, Object> computeSPMMetric1(Set<String> projectIds, ExportPeriod period) {
        // Find exits to permanent housing destinations
        String exitsSql = """
            SELECT
                e.personal_id,
                ex.exit_date,
                ex.destination
            FROM enrollments e
            JOIN exits ex ON e.enrollment_id = ex.enrollment_id
            WHERE e.project_id IN (?)
              AND ex.exit_date BETWEEN ? AND ?
              AND ex.destination IN (
                  410, 411, 421, 422, 423, 426  -- Permanent housing destinations
              )
            """;

        List<Map<String, Object>> exits = jdbcTemplate.queryForList(
                exitsSql,
                String.join(",", projectIds),
                period.startDate().minusYears(2),
                period.endDate()
        );

        int total = exits.size();
        int returnedIn6Months = 0;
        int returnedIn12Months = 0;
        int returnedIn24Months = 0;

        // For each exit, check if person returned to homelessness
        for (Map<String, Object> exit : exits) {
            String personalId = (String) exit.get("personal_id");
            LocalDate exitDate = ((java.sql.Date) exit.get("exit_date")).toLocalDate();

            // Check for subsequent enrollments in homeless projects
            String returnSql = """
                SELECT MIN(e.entry_date) as return_date
                FROM enrollments e
                JOIN projects p ON e.project_id = p.project_id
                WHERE e.personal_id = ?
                  AND e.entry_date > ?
                  AND p.project_type IN (1, 2, 4, 8)  -- ES, TH, SO, Safe Haven
                """;

            LocalDate returnDate = jdbcTemplate.query(
                    returnSql,
                    rs -> rs.next() ? rs.getDate("return_date").toLocalDate() : null,
                    personalId,
                    java.sql.Date.valueOf(exitDate)
            );

            if (returnDate != null) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(exitDate, returnDate);
                if (daysBetween <= 180) returnedIn6Months++;
                if (daysBetween <= 365) returnedIn12Months++;
                if (daysBetween <= 730) returnedIn24Months++;
            }
        }

        Map<String, Object> results = new HashMap<>();
        results.put("totalExits", suppressIfNeeded(total));
        results.put("returnedIn6Months", suppressIfNeeded(returnedIn6Months));
        results.put("returnedIn12Months", suppressIfNeeded(returnedIn12Months));
        results.put("returnedIn24Months", suppressIfNeeded(returnedIn24Months));
        results.put("percentReturnedIn6Months", calculatePercentage(returnedIn6Months, total));
        results.put("percentReturnedIn12Months", calculatePercentage(returnedIn12Months, total));
        results.put("percentReturnedIn24Months", calculatePercentage(returnedIn24Months, total));

        return results;
    }

    /**
     * SPM Metric 7: Successful Placement (365-day outcomes)
     * Percentage of persons who maintain permanent housing for at least 1 year
     */
    public Map<String, Object> computeSPMMetric7(Set<String> projectIds, ExportPeriod period) {
        // Find move-ins to permanent housing
        String moveInSql = """
            SELECT
                e.personal_id,
                e.enrollment_id,
                e.move_in_date
            FROM enrollments e
            JOIN projects p ON e.project_id = p.project_id
            WHERE e.project_id IN (?)
              AND e.move_in_date BETWEEN ? AND ?
              AND p.project_type IN (3, 9, 13)  -- PSH, RRH, PH
              AND e.move_in_date IS NOT NULL
            """;

        List<Map<String, Object>> moveIns = jdbcTemplate.queryForList(
                moveInSql,
                String.join(",", projectIds),
                period.startDate().minusYears(1),  // Need 1-year lookback
                period.endDate().minusYears(1)     // Must have 1 year to measure
        );

        int total = moveIns.size();
        int successfulPlacements = 0;

        // For each move-in, check if person stayed housed for 365 days
        for (Map<String, Object> moveIn : moveIns) {
            String enrollmentId = (String) moveIn.get("enrollment_id");
            LocalDate moveInDate = ((java.sql.Date) moveIn.get("move_in_date")).toLocalDate();
            LocalDate oneYearLater = moveInDate.plusDays(365);

            // Check if exited before 1 year or still enrolled
            String exitSql = """
                SELECT exit_date, destination
                FROM exits
                WHERE enrollment_id = ?
                """;

            Map<String, Object> exitData = jdbcTemplate.query(
                    exitSql,
                    rs -> {
                        if (rs.next()) {
                            java.sql.Date exitDate = rs.getDate("exit_date");
                            Integer destination = rs.getInt("destination");
                            Map<String, Object> data = new HashMap<>();
                            data.put("exit_date", exitDate != null ? exitDate.toLocalDate() : null);
                            data.put("destination", destination);
                            return data;
                        }
                        return null;
                    },
                    enrollmentId
            );

            boolean successful = false;
            if (exitData == null) {
                // Still enrolled = success
                successful = true;
            } else {
                LocalDate exitDate = (LocalDate) exitData.get("exit_date");
                Integer destination = (Integer) exitData.get("destination");

                // Stayed at least 1 year, or exited to permanent housing
                if (exitDate.isAfter(oneYearLater) ||
                        List.of(410, 411, 421, 422, 423, 426).contains(destination)) {
                    successful = true;
                }
            }

            if (successful) {
                successfulPlacements++;
            }
        }

        Map<String, Object> results = new HashMap<>();
        results.put("totalPlacements", suppressIfNeeded(total));
        results.put("successfulPlacements", suppressIfNeeded(successfulPlacements));
        results.put("successRate", calculatePercentage(successfulPlacements, total));

        return results;
    }

    /**
     * Apply cell suppression: if count < threshold, return "*"
     */
    private Object suppressIfNeeded(int count) {
        if (count > 0 && count < SUPPRESSION_THRESHOLD) {
            return SUPPRESSED_VALUE;
        }
        return count;
    }

    private String calculatePercentage(int numerator, int denominator) {
        if (denominator == 0) {
            return "N/A";
        }
        if (numerator < SUPPRESSION_THRESHOLD && numerator > 0) {
            return SUPPRESSED_VALUE;
        }
        double percentage = (numerator * 100.0) / denominator;
        return String.format("%.1f%%", percentage);
    }

    private int getCount(List<Map<String, Object>> results, Object key) {
        return results.stream()
                .filter(row -> key.equals(row.get(getKeyColumn(row))))
                .findFirst()
                .map(row -> ((Number) row.get("count")).intValue())
                .orElse(0);
    }

    private String getKeyColumn(Map<String, Object> row) {
        if (row.containsKey("household_type")) return "household_type";
        if (row.containsKey("is_veteran")) return "is_veteran";
        if (row.containsKey("stage")) return "stage";
        return "key";
    }
}
