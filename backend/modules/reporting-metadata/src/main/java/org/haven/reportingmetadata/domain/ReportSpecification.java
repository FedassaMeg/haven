package org.haven.reportingmetadata.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.*;

/**
 * Aggregate root for HUD report specifications
 *
 * Captures complete specification for generating HUD-compliant reports:
 * - CoC APR sections (Q4a-Q27c)
 * - ESG CAPER sections
 * - SPM measures (1a-7b2)
 * - PIT/HIC tables
 *
 * Encodes:
 * - Report scope (CoC, project type, funding source filters)
 * - Inclusion/exclusion logic
 * - Aggregate rules and calculations
 * - Filing deadlines
 * - Versioning tied to HUD notices
 *
 * Compliance references:
 * - 24 CFR 578.103 (CoC APR requirements)
 * - Notice CPD-17-01 (SPM specifications)
 * - HUD HMIS Programming Specifications
 */
@Entity
@Table(name = "report_specification", indexes = {
    @Index(name = "idx_report_type", columnList = "report_type"),
    @Index(name = "idx_report_effective", columnList = "effective_from, effective_to"),
    @Index(name = "idx_report_version", columnList = "version_identifier"),
    @Index(name = "idx_report_section", columnList = "section_identifier")
})
public class ReportSpecification {

    @Id
    @GeneratedValue
    private UUID specificationId;

    /**
     * Report type: CoC_APR, ESG_CAPER, SPM, PIT, HIC, LSA
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HudSpecificationType reportType;

    /**
     * Section or measure identifier
     * Examples: "Q04a", "Q23c", "Metric1a", "S1", "CAPER-4.1"
     */
    @NotNull
    @Column(nullable = false, length = 100)
    private String sectionIdentifier;

    /**
     * Human-readable section name
     * Examples: "Clients Served", "Permanent Housing Destinations", "Length of Time Homeless"
     */
    @NotNull
    @Column(nullable = false, length = 500)
    private String sectionName;

    /**
     * Section description and purpose
     */
    @Column(length = 2000)
    private String description;

    /**
     * Version identifier tied to HUD specification release
     * Examples: "HDX-2024", "APR-FY2024", "SPM-v2.0"
     */
    @NotNull
    @Column(nullable = false, length = 100)
    private String versionIdentifier;

    /**
     * HUD notice or specification reference
     * Examples: "Notice CPD-23-08", "HMIS Programming Specs v2024"
     */
    @Column(length = 200)
    private String hudNoticeReference;

    /**
     * Effective start date for this specification version
     */
    @NotNull
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Effective end date (null = currently active)
     */
    @Column
    private LocalDate effectiveTo;

    /**
     * Report filing deadline (for APR/CAPER/SPM)
     * Examples: "90 days after fiscal year end", "February 28 annually"
     */
    @Column(length = 200)
    private String filingDeadline;

    /**
     * Scope filters encoded as JSON
     * Structure: {
     *   "cocCodes": ["CA-600", "CA-601"],
     *   "projectTypes": [1, 2, 3, 8],
     *   "fundingSources": ["CoC", "ESG"],
     *   "reportingPeriod": {"start": "2023-10-01", "end": "2024-09-30"}
     * }
     */
    @Column(columnDefinition = "TEXT")
    private String scopeFiltersJson;

    /**
     * Inclusion logic encoded as JSON
     * Structure: {
     *   "includeOnlyActiveEnrollments": true,
     *   "requireProjectStart": true,
     *   "excludeTestData": true,
     *   "minimumStayDays": 1
     * }
     */
    @Column(columnDefinition = "TEXT")
    private String inclusionLogicJson;

    /**
     * Exclusion logic encoded as JSON
     * Structure: {
     *   "excludeProjectTypes": [14],
     *   "excludeNonCoC": true,
     *   "excludeOutOfCoC": true
     * }
     */
    @Column(columnDefinition = "TEXT")
    private String exclusionLogicJson;

    /**
     * Aggregate calculation rules encoded as JSON
     * Structure: {
     *   "aggregateBy": ["ProjectType", "FundingSource"],
     *   "metrics": [
     *     {"name": "TotalClients", "expression": "COUNT(DISTINCT ClientID)"},
     *     {"name": "AvgLOS", "expression": "AVG(LengthOfStay)"}
     *   ]
     * }
     */
    @Column(columnDefinition = "TEXT")
    private String aggregateRulesJson;

    /**
     * SQL query template for generating report data
     * Parameterized with placeholders: ${cocCode}, ${startDate}, ${endDate}
     */
    @Column(columnDefinition = "TEXT")
    private String sqlQueryTemplate;

    /**
     * Whether this specification requires VAWA consent checks
     */
    @NotNull
    @Column(nullable = false)
    private boolean requiresVawaConsentCheck;

    /**
     * Whether aggregates are permitted without consent (VAWA)
     * If true, can show counts even when individual records suppressed
     */
    @NotNull
    @Column(nullable = false)
    private boolean allowAggregatesWithoutConsent;

    /**
     * Data quality threshold requirements (if any)
     * Examples: "Less than 5% null values in critical fields"
     */
    @Column(length = 1000)
    private String dataQualityRequirements;

    /**
     * Field mappings associated with this specification
     * Lazy-loaded collection
     */
    @OneToMany(mappedBy = "reportSpecification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportSection> sections = new ArrayList<>();

    protected ReportSpecification() {
        // JPA constructor
    }

    private ReportSpecification(Builder builder) {
        this.reportType = builder.reportType;
        this.sectionIdentifier = builder.sectionIdentifier;
        this.sectionName = builder.sectionName;
        this.description = builder.description;
        this.versionIdentifier = builder.versionIdentifier;
        this.hudNoticeReference = builder.hudNoticeReference;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveTo = builder.effectiveTo;
        this.filingDeadline = builder.filingDeadline;
        this.scopeFiltersJson = builder.scopeFiltersJson;
        this.inclusionLogicJson = builder.inclusionLogicJson;
        this.exclusionLogicJson = builder.exclusionLogicJson;
        this.aggregateRulesJson = builder.aggregateRulesJson;
        this.sqlQueryTemplate = builder.sqlQueryTemplate;
        this.requiresVawaConsentCheck = builder.requiresVawaConsentCheck;
        this.allowAggregatesWithoutConsent = builder.allowAggregatesWithoutConsent;
        this.dataQualityRequirements = builder.dataQualityRequirements;
    }

    // Getters
    public UUID getSpecificationId() {
        return specificationId;
    }

    public HudSpecificationType getReportType() {
        return reportType;
    }

    public String getSectionIdentifier() {
        return sectionIdentifier;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getDescription() {
        return description;
    }

    public String getVersionIdentifier() {
        return versionIdentifier;
    }

    public String getHudNoticeReference() {
        return hudNoticeReference;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public String getFilingDeadline() {
        return filingDeadline;
    }

    public String getScopeFiltersJson() {
        return scopeFiltersJson;
    }

    public String getInclusionLogicJson() {
        return inclusionLogicJson;
    }

    public String getExclusionLogicJson() {
        return exclusionLogicJson;
    }

    public String getAggregateRulesJson() {
        return aggregateRulesJson;
    }

    public String getSqlQueryTemplate() {
        return sqlQueryTemplate;
    }

    public boolean isRequiresVawaConsentCheck() {
        return requiresVawaConsentCheck;
    }

    public boolean isAllowAggregatesWithoutConsent() {
        return allowAggregatesWithoutConsent;
    }

    public String getDataQualityRequirements() {
        return dataQualityRequirements;
    }

    public List<ReportSection> getSections() {
        return Collections.unmodifiableList(sections);
    }

    /**
     * Add section to specification
     */
    public void addSection(ReportSection section) {
        sections.add(section);
        section.setReportSpecification(this);
    }

    /**
     * Check if specification is active on given date
     */
    public boolean isActiveOn(LocalDate date) {
        boolean afterStart = !date.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);
        return afterStart && beforeEnd;
    }

    /**
     * Check if specification is currently active
     */
    public boolean isCurrentlyActive() {
        return isActiveOn(LocalDate.now());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private HudSpecificationType reportType;
        private String sectionIdentifier;
        private String sectionName;
        private String description;
        private String versionIdentifier;
        private String hudNoticeReference;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String filingDeadline;
        private String scopeFiltersJson;
        private String inclusionLogicJson;
        private String exclusionLogicJson;
        private String aggregateRulesJson;
        private String sqlQueryTemplate;
        private boolean requiresVawaConsentCheck = false;
        private boolean allowAggregatesWithoutConsent = true;
        private String dataQualityRequirements;

        public Builder reportType(HudSpecificationType reportType) {
            this.reportType = reportType;
            return this;
        }

        public Builder sectionIdentifier(String sectionIdentifier) {
            this.sectionIdentifier = sectionIdentifier;
            return this;
        }

        public Builder sectionName(String sectionName) {
            this.sectionName = sectionName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder versionIdentifier(String versionIdentifier) {
            this.versionIdentifier = versionIdentifier;
            return this;
        }

        public Builder hudNoticeReference(String hudNoticeReference) {
            this.hudNoticeReference = hudNoticeReference;
            return this;
        }

        public Builder effectiveFrom(LocalDate effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(LocalDate effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public Builder filingDeadline(String filingDeadline) {
            this.filingDeadline = filingDeadline;
            return this;
        }

        public Builder scopeFiltersJson(String scopeFiltersJson) {
            this.scopeFiltersJson = scopeFiltersJson;
            return this;
        }

        public Builder inclusionLogicJson(String inclusionLogicJson) {
            this.inclusionLogicJson = inclusionLogicJson;
            return this;
        }

        public Builder exclusionLogicJson(String exclusionLogicJson) {
            this.exclusionLogicJson = exclusionLogicJson;
            return this;
        }

        public Builder aggregateRulesJson(String aggregateRulesJson) {
            this.aggregateRulesJson = aggregateRulesJson;
            return this;
        }

        public Builder sqlQueryTemplate(String sqlQueryTemplate) {
            this.sqlQueryTemplate = sqlQueryTemplate;
            return this;
        }

        public Builder requiresVawaConsentCheck(boolean requiresVawaConsentCheck) {
            this.requiresVawaConsentCheck = requiresVawaConsentCheck;
            return this;
        }

        public Builder allowAggregatesWithoutConsent(boolean allowAggregatesWithoutConsent) {
            this.allowAggregatesWithoutConsent = allowAggregatesWithoutConsent;
            return this;
        }

        public Builder dataQualityRequirements(String dataQualityRequirements) {
            this.dataQualityRequirements = dataQualityRequirements;
            return this;
        }

        public ReportSpecification build() {
            return new ReportSpecification(this);
        }
    }
}
