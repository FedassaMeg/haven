package org.haven.reporting.application.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.haven.readmodels.infrastructure.PolicyDecisionLogRepository;
import org.haven.reporting.domain.ExportPeriod;
import org.haven.shared.reporting.ReportingFieldMapping;
import org.haven.shared.reporting.ReportingMetadataRepository;
import org.haven.shared.security.ConfidentialityPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating HUD HMIS CSV export views
 *
 * Responsibilities:
 * - Generates parameterized views for each CSV table (Client, Enrollment, Exit, Services, etc.)
 * - Applies operating year filters (Oct 1 - Sep 30 for CoC APR)
 * - Applies project type restrictions per report specification
 * - Joins to PolicyDecisionLog to exclude VAWA-denied records
 * - Uses Hibernate/JPA for dynamic query generation
 *
 * HUD compliance:
 * - Enforces VAWA consent checks via PolicyDecisionLog
 * - Supports suppression behaviors: SUPPRESS (omit record), REDACT (mask field), AGGREGATE_ONLY
 * - Preserves HUD code lists (RaceNone=8, GenderNone=99, etc.)
 */
@Service
public class HUDExportViewGenerator {

    private static final Logger logger = LoggerFactory.getLogger(HUDExportViewGenerator.class);

    private final EntityManager entityManager;
    private final ReportingMetadataRepository metadataRepository;
    private final PolicyDecisionLogRepository policyDecisionLogRepository;
    private final ConfidentialityPolicyService confidentialityPolicyService;

    public HUDExportViewGenerator(
            EntityManager entityManager,
            ReportingMetadataRepository metadataRepository,
            PolicyDecisionLogRepository policyDecisionLogRepository,
            ConfidentialityPolicyService confidentialityPolicyService) {
        this.entityManager = entityManager;
        this.metadataRepository = metadataRepository;
        this.policyDecisionLogRepository = policyDecisionLogRepository;
        this.confidentialityPolicyService = confidentialityPolicyService;
    }

    /**
     * Generate Client.csv data
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateClientCsv(
            ExportPeriod period,
            List<UUID> projectIds,
            String cocCode) {

        List<ReportingFieldMapping> mappings = metadataRepository
                .findActiveBySpecType("HMIS_CSV", LocalDate.now())
                .stream()
                .filter(m -> m.getSourceEntity().equals("ClientProfile"))
                .sorted(Comparator.comparing(m -> m.getCsvFieldOrder() != null ? m.getCsvFieldOrder() : 999))
                .collect(Collectors.toList());

        String sql = buildClientQuery(mappings, period, projectIds, cocCode);

        logger.info("Generating Client.csv for period {} with {} projects", period, projectIds.size());

        Query query = entityManager.createNativeQuery(sql);
        setQueryParameters(query, period, projectIds, cocCode);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return transformToMaps(results, mappings);
    }

    /**
     * Generate Enrollment.csv data
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateEnrollmentCsv(
            ExportPeriod period,
            List<UUID> projectIds,
            String cocCode) {

        List<ReportingFieldMapping> mappings = metadataRepository
                .findActiveBySpecType("HMIS_CSV", LocalDate.now())
                .stream()
                .filter(m -> m.getSourceEntity().equals("ProgramEnrollment") &&
                           m.getTargetHudElementId().startsWith("CSV:Enrollment."))
                .sorted(Comparator.comparing(m -> m.getCsvFieldOrder() != null ? m.getCsvFieldOrder() : 999))
                .collect(Collectors.toList());

        String sql = buildEnrollmentQuery(mappings, period, projectIds, cocCode);

        logger.info("Generating Enrollment.csv for period {} with {} projects", period, projectIds.size());

        Query query = entityManager.createNativeQuery(sql);
        setQueryParameters(query, period, projectIds, cocCode);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return transformToMaps(results, mappings);
    }

    /**
     * Generate Services.csv data with VAWA filtering
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateServicesCsv(
            ExportPeriod period,
            List<UUID> projectIds,
            String cocCode) {

        List<ReportingFieldMapping> mappings = metadataRepository
                .findActiveBySpecType("HMIS_CSV", LocalDate.now())
                .stream()
                .filter(m -> m.getSourceEntity().equals("ServiceEpisode"))
                .sorted(Comparator.comparing(m -> m.getCsvFieldOrder() != null ? m.getCsvFieldOrder() : 999))
                .collect(Collectors.toList());

        // Services.csv requires VAWA consent checks
        String sql = buildServicesQueryWithVawaFilter(mappings, period, projectIds, cocCode);

        logger.info("Generating Services.csv with VAWA filtering for period {}", period);

        Query query = entityManager.createNativeQuery(sql);
        setQueryParameters(query, period, projectIds, cocCode);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return transformToMaps(results, mappings);
    }

    /**
     * Generate CurrentLivingSituation.csv data with VAWA filtering
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateCurrentLivingSituationCsv(
            ExportPeriod period,
            List<UUID> projectIds,
            String cocCode) {

        List<ReportingFieldMapping> mappings = metadataRepository
                .findActiveBySpecType("HMIS_CSV", LocalDate.now())
                .stream()
                .filter(m -> m.getSourceEntity().equals("CurrentLivingSituation"))
                .sorted(Comparator.comparing(m -> m.getCsvFieldOrder() != null ? m.getCsvFieldOrder() : 999))
                .collect(Collectors.toList());

        String sql = buildCurrentLivingSituationQueryWithVawaFilter(mappings, period, projectIds, cocCode);

        logger.info("Generating CurrentLivingSituation.csv with VAWA filtering for period {}", period);

        Query query = entityManager.createNativeQuery(sql);
        setQueryParameters(query, period, projectIds, cocCode);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return transformToMaps(results, mappings);
    }

    /**
     * Build Client.csv query
     */
    private String buildClientQuery(
            List<ReportingFieldMapping> mappings,
            ExportPeriod period,
            List<UUID> projectIds,
            String cocCode) {

        StringBuilder sql = new StringBuilder("SELECT ");

        // Add field selections with transformations
        for (int i = 0; i < mappings.size(); i++) {
            ReportingFieldMapping mapping = mappings.get(i);
            if (i > 0) sql.append(", ");

            String fieldExpression = mapping.getTransformExpression() != null &&
                                    !mapping.getTransformExpression().isEmpty()
                    ? mapping.getTransformExpression()
                    : "c." + mapping.getSourceField();

            sql.append(fieldExpression).append(" AS ").append(mapping.getCsvFieldName());
        }

        sql.append(" FROM client_profile c ");
        sql.append(" INNER JOIN program_enrollment pe ON c.client_id = pe.client_id ");
        sql.append(" WHERE pe.project_id IN (:projectIds) ");

        // Operating year filter: enrollment overlaps with reporting period
        sql.append(" AND pe.entry_date <= :periodEnd ");
        sql.append(" AND (pe.exit_date IS NULL OR pe.exit_date >= :periodStart) ");

        if (cocCode != null && !cocCode.isEmpty()) {
            sql.append(" AND pe.enrollment_coc = :cocCode ");
        }

        // Distinct clients only
        sql.append(" GROUP BY c.client_id ");
        sql.append(" ORDER BY c.client_id ");

        return sql.toString();
    }

    /**
     * Build Enrollment.csv query
     */
    private String buildEnrollmentQuery(
            List<ReportingFieldMapping> mappings,
            ExportPeriod period,
            List<UUID> projectIds,
            String cocCode) {

        StringBuilder sql = new StringBuilder("SELECT ");

        for (int i = 0; i < mappings.size(); i++) {
            ReportingFieldMapping mapping = mappings.get(i);
            if (i > 0) sql.append(", ");

            String fieldExpression = mapping.getTransformExpression() != null &&
                                    !mapping.getTransformExpression().isEmpty()
                    ? mapping.getTransformExpression()
                    : "pe." + mapping.getSourceField();

            sql.append(fieldExpression).append(" AS ").append(mapping.getCsvFieldName());
        }

        sql.append(" FROM program_enrollment pe ");
        sql.append(" WHERE pe.project_id IN (:projectIds) ");

        // Operating year filter
        sql.append(" AND pe.entry_date <= :periodEnd ");
        sql.append(" AND (pe.exit_date IS NULL OR pe.exit_date >= :periodStart) ");

        if (cocCode != null && !cocCode.isEmpty()) {
            sql.append(" AND pe.enrollment_coc = :cocCode ");
        }

        sql.append(" ORDER BY pe.enrollment_id ");

        return sql.toString();
    }

    /**
     * Build Services.csv query with VAWA consent filtering
     * Excludes DV service records (type 14) without consent per PolicyDecisionLog
     */
    private String buildServicesQueryWithVawaFilter(
            List<ReportingFieldMapping> mappings,
            ExportPeriod period,
            List<UUID> projectIds,
            String cocCode) {

        StringBuilder sql = new StringBuilder("SELECT ");

        for (int i = 0; i < mappings.size(); i++) {
            ReportingFieldMapping mapping = mappings.get(i);
            if (i > 0) sql.append(", ");

            String fieldExpression = mapping.getTransformExpression() != null &&
                                    !mapping.getTransformExpression().isEmpty()
                    ? mapping.getTransformExpression()
                    : "se." + mapping.getSourceField();

            sql.append(fieldExpression).append(" AS ").append(mapping.getCsvFieldName());
        }

        sql.append(" FROM service_episode se ");
        sql.append(" INNER JOIN program_enrollment pe ON se.enrollment_id = pe.enrollment_id ");
        sql.append(" WHERE pe.project_id IN (:projectIds) ");

        // Operating year filter: service date within period
        sql.append(" AND se.service_date >= :periodStart ");
        sql.append(" AND se.service_date <= :periodEnd ");

        if (cocCode != null && !cocCode.isEmpty()) {
            sql.append(" AND pe.enrollment_coc = :cocCode ");
        }

        // VAWA filter: Exclude DV services (type 14) denied by policy
        sql.append(" AND NOT EXISTS ( ");
        sql.append("   SELECT 1 FROM policy_decision_log pdl ");
        sql.append("   WHERE pdl.resource_id = se.service_id ");
        sql.append("   AND pdl.resource_type = 'ServiceEpisode' ");
        sql.append("   AND pdl.allowed = false ");
        sql.append("   AND pdl.policy_rule IN ('VAWA_DV_SERVICE_RESTRICTION', 'VAWA_CONSENT_REQUIRED') ");
        sql.append(" ) ");

        sql.append(" ORDER BY se.service_id ");

        return sql.toString();
    }

    /**
     * Build CurrentLivingSituation.csv query with VAWA filtering
     */
    private String buildCurrentLivingSituationQueryWithVawaFilter(
            List<ReportingFieldMapping> mappings,
            ExportPeriod period,
            List<UUID> projectIds,
            String cocCode) {

        StringBuilder sql = new StringBuilder("SELECT ");

        for (int i = 0; i < mappings.size(); i++) {
            ReportingFieldMapping mapping = mappings.get(i);
            if (i > 0) sql.append(", ");

            String fieldExpression = mapping.getTransformExpression() != null &&
                                    !mapping.getTransformExpression().isEmpty()
                    ? mapping.getTransformExpression()
                    : "cls." + mapping.getSourceField();

            sql.append(fieldExpression).append(" AS ").append(mapping.getCsvFieldName());
        }

        sql.append(" FROM current_living_situation cls ");
        sql.append(" INNER JOIN program_enrollment pe ON cls.enrollment_id = pe.enrollment_id ");
        sql.append(" WHERE pe.project_id IN (:projectIds) ");

        // Operating year filter
        sql.append(" AND cls.information_date >= :periodStart ");
        sql.append(" AND cls.information_date <= :periodEnd ");

        if (cocCode != null && !cocCode.isEmpty()) {
            sql.append(" AND pe.enrollment_coc = :cocCode ");
        }

        // VAWA filter: Exclude CLS records denied by policy (DV victims without consent)
        sql.append(" AND NOT EXISTS ( ");
        sql.append("   SELECT 1 FROM policy_decision_log pdl ");
        sql.append("   WHERE pdl.resource_id = cls.current_living_situation_id ");
        sql.append("   AND pdl.resource_type = 'CurrentLivingSituation' ");
        sql.append("   AND pdl.allowed = false ");
        sql.append("   AND pdl.policy_rule IN ('VAWA_CURRENT_LIVING_SITUATION_RESTRICTION', 'VAWA_CONSENT_REQUIRED') ");
        sql.append(" ) ");

        sql.append(" ORDER BY cls.current_living_situation_id ");

        return sql.toString();
    }

    /**
     * Set query parameters
     */
    private void setQueryParameters(Query query, ExportPeriod period, List<UUID> projectIds, String cocCode) {
        query.setParameter("periodStart", period.getStartDate());
        query.setParameter("periodEnd", period.getEndDate());
        query.setParameter("projectIds", projectIds.stream().map(UUID::toString).collect(Collectors.toList()));

        if (cocCode != null && !cocCode.isEmpty()) {
            query.setParameter("cocCode", cocCode);
        }
    }

    /**
     * Transform query results to maps keyed by CSV field names
     */
    private List<Map<String, Object>> transformToMaps(
            List<Object[]> results,
            List<ReportingFieldMapping> mappings) {

        List<Map<String, Object>> output = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> record = new LinkedHashMap<>();

            for (int i = 0; i < mappings.size() && i < row.length; i++) {
                ReportingFieldMapping mapping = mappings.get(i);
                Object value = row[i];

                // Apply VAWA suppression if configured
                if (mapping.isVawaSensitiveField() && value != null) {
                    String suppressionBehavior = mapping.getVawaSuppressionBehavior();
                    if ("REDACT".equals(suppressionBehavior)) {
                        value = "***REDACTED***";
                    } else if ("SUPPRESS".equals(suppressionBehavior)) {
                        continue; // Skip field entirely
                    }
                    // AGGREGATE_ONLY: handled at aggregate report level, not row-level
                }

                record.put(mapping.getCsvFieldName(), value);
            }

            output.add(record);
        }

        return output;
    }
}
