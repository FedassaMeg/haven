package org.haven.reportingmetadata.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object encoding common HUD transformations
 *
 * Provides reusable transformation rules for:
 * - Age calculations respecting HUD Universal Data Element specs
 * - Project type filtering per 24 CFR 578
 * - Household relationship flattening
 * - Race/ethnicity coding per HUD Data Standards
 * - Service type categorizations
 *
 * Examples:
 * - Calculate age at enrollment: TIMESTAMPDIFF(YEAR, dob, enrollmentDate)
 * - ES project types: projectType IN (1, 2, 3, 8)
 * - HoH identification: relationshipToHoH = 1
 * - None codes: COALESCE(race, 8), COALESCE(gender, 99)
 */
@Entity
@Table(name = "transformation_rule", indexes = {
    @Index(name = "idx_transform_name", columnList = "rule_name", unique = true),
    @Index(name = "idx_transform_category", columnList = "category")
})
public class TransformationRule {

    @Id
    @GeneratedValue
    private UUID ruleId;

    /**
     * Unique rule name
     * Examples: "AGE_AT_ENROLLMENT", "ES_PROJECT_FILTER", "HOH_RELATIONSHIP", "RACE_NONE_DEFAULT"
     */
    @NotNull
    @Column(nullable = false, unique = true, length = 100)
    private String ruleName;

    /**
     * Rule category for organization
     * Values: AGE_CALCULATION, PROJECT_TYPE_FILTER, HOUSEHOLD_LOGIC, CODE_LIST_MAPPING, DATE_CALCULATION
     */
    @NotNull
    @Column(nullable = false, length = 50)
    private String category;

    /**
     * Human-readable description
     */
    @NotNull
    @Column(nullable = false, length = 1000)
    private String description;

    /**
     * Transformation expression template
     * Supports placeholders: ${fieldName}, ${enrollmentDate}, ${reportStartDate}, ${reportEndDate}
     *
     * Examples:
     * - "TIMESTAMPDIFF(YEAR, ${dateOfBirth}, ${enrollmentDate})"
     * - "${projectType} IN (1, 2, 3, 8)"
     * - "CASE WHEN ${relationshipToHoH} = 1 THEN true ELSE false END"
     * - "COALESCE(${race}, 8)"
     */
    @NotNull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String expressionTemplate;

    /**
     * Transformation language
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransformLanguage transformLanguage;

    /**
     * Expected return data type
     */
    @NotNull
    @Column(nullable = false, length = 50)
    private String returnDataType;

    /**
     * HUD specification reference
     * Examples: "HUD Universal Data Element 3.03", "24 CFR 578.37", "HDX 2024 Data Dictionary"
     */
    @Column(length = 500)
    private String hudReference;

    /**
     * Example input and output for documentation
     */
    @Column(length = 1000)
    private String exampleUsage;

    /**
     * Whether this rule produces VAWA-sensitive output
     */
    @NotNull
    @Column(nullable = false)
    private boolean vawaRelevant;

    protected TransformationRule() {
        // JPA constructor
    }

    private TransformationRule(Builder builder) {
        this.ruleName = builder.ruleName;
        this.category = builder.category;
        this.description = builder.description;
        this.expressionTemplate = builder.expressionTemplate;
        this.transformLanguage = builder.transformLanguage;
        this.returnDataType = builder.returnDataType;
        this.hudReference = builder.hudReference;
        this.exampleUsage = builder.exampleUsage;
        this.vawaRelevant = builder.vawaRelevant;
    }

    // Getters
    public UUID getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getExpressionTemplate() {
        return expressionTemplate;
    }

    public TransformLanguage getTransformLanguage() {
        return transformLanguage;
    }

    public String getReturnDataType() {
        return returnDataType;
    }

    public String getHudReference() {
        return hudReference;
    }

    public String getExampleUsage() {
        return exampleUsage;
    }

    public boolean isVawaRelevant() {
        return vawaRelevant;
    }

    /**
     * Apply transformation rule with parameter substitution
     */
    public String apply(java.util.Map<String, String> parameters) {
        String result = expressionTemplate;
        for (java.util.Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransformationRule that = (TransformationRule) o;
        return Objects.equals(ruleName, that.ruleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String ruleName;
        private String category;
        private String description;
        private String expressionTemplate;
        private TransformLanguage transformLanguage = TransformLanguage.SQL;
        private String returnDataType;
        private String hudReference;
        private String exampleUsage;
        private boolean vawaRelevant = false;

        public Builder ruleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder expressionTemplate(String expressionTemplate) {
            this.expressionTemplate = expressionTemplate;
            return this;
        }

        public Builder transformLanguage(TransformLanguage transformLanguage) {
            this.transformLanguage = transformLanguage;
            return this;
        }

        public Builder returnDataType(String returnDataType) {
            this.returnDataType = returnDataType;
            return this;
        }

        public Builder hudReference(String hudReference) {
            this.hudReference = hudReference;
            return this;
        }

        public Builder exampleUsage(String exampleUsage) {
            this.exampleUsage = exampleUsage;
            return this;
        }

        public Builder vawaRelevant(boolean vawaRelevant) {
            this.vawaRelevant = vawaRelevant;
            return this;
        }

        public TransformationRule build() {
            return new TransformationRule(this);
        }
    }

    // Common HUD transformation rule factory methods

    /**
     * Age at enrollment calculation per HUD Universal Data Element 3.03
     */
    public static TransformationRule ageAtEnrollment() {
        return builder()
                .ruleName("AGE_AT_ENROLLMENT")
                .category("AGE_CALCULATION")
                .description("Calculate client age at enrollment date per HUD Universal Data Element 3.03")
                .expressionTemplate("TIMESTAMPDIFF(YEAR, ${dateOfBirth}, ${enrollmentDate})")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Integer")
                .hudReference("HUD Universal Data Element 3.03 - Date of Birth")
                .exampleUsage("DOB: 1990-05-15, Enrollment: 2024-01-10 → Age: 33")
                .vawaRelevant(false)
                .build();
    }

    /**
     * Age at report date calculation
     */
    public static TransformationRule ageAtReportDate() {
        return builder()
                .ruleName("AGE_AT_REPORT_DATE")
                .category("AGE_CALCULATION")
                .description("Calculate client age at report generation date")
                .expressionTemplate("TIMESTAMPDIFF(YEAR, ${dateOfBirth}, ${reportDate})")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Integer")
                .hudReference("HUD Universal Data Element 3.03 - Date of Birth")
                .exampleUsage("DOB: 1990-05-15, Report Date: 2024-10-07 → Age: 34")
                .vawaRelevant(false)
                .build();
    }

    /**
     * Emergency Shelter project type filter per 24 CFR 578
     */
    public static TransformationRule emergencyShelterFilter() {
        return builder()
                .ruleName("ES_PROJECT_TYPE_FILTER")
                .category("PROJECT_TYPE_FILTER")
                .description("Filter for Emergency Shelter project types per 24 CFR 578")
                .expressionTemplate("${projectType} IN (1, 2, 3, 8)")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Boolean")
                .hudReference("24 CFR 578.37 - Project Types: 1=ES-Entry/Exit, 2=ES-Night-by-Night, 3=ES, 8=Safe Haven")
                .exampleUsage("Project Type 1 → true, Project Type 13 → false")
                .vawaRelevant(false)
                .build();
    }

    /**
     * CoC-funded project filter per 24 CFR 578
     */
    public static TransformationRule cocFundedProjectFilter() {
        return builder()
                .ruleName("COC_FUNDED_PROJECT_FILTER")
                .category("PROJECT_TYPE_FILTER")
                .description("Filter for CoC-funded projects only per 24 CFR 578")
                .expressionTemplate("${fundingSource} = 'CoC' OR ${fundingSource} LIKE '%CoC%'")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Boolean")
                .hudReference("24 CFR 578.103 - APR applies to CoC-funded projects")
                .exampleUsage("Funding: 'CoC' → true, Funding: 'ESG' → false")
                .vawaRelevant(false)
                .build();
    }

    /**
     * Head of Household identification
     */
    public static TransformationRule headOfHouseholdCheck() {
        return builder()
                .ruleName("HEAD_OF_HOUSEHOLD_CHECK")
                .category("HOUSEHOLD_LOGIC")
                .description("Identify if client is Head of Household")
                .expressionTemplate("${relationshipToHoH} = 1")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Boolean")
                .hudReference("HUD Universal Data Element 4.02.3 - Relationship to Head of Household: 1=Self (Head of Household)")
                .exampleUsage("RelationshipToHoH: 1 → true, RelationshipToHoH: 2 → false")
                .vawaRelevant(false)
                .build();
    }

    /**
     * Race code with None default per HUD Data Standards
     */
    public static TransformationRule raceWithNoneDefault() {
        return builder()
                .ruleName("RACE_NONE_DEFAULT")
                .category("CODE_LIST_MAPPING")
                .description("Default null race to 8 (Client doesn't know) per HUD Data Standards")
                .expressionTemplate("COALESCE(${race}, 8)")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Integer")
                .hudReference("HUD Data Standards Race Code List 1.6: 8=Client doesn't know")
                .exampleUsage("Race: null → 8, Race: 1 → 1")
                .vawaRelevant(false)
                .build();
    }

    /**
     * Gender code with None default per HUD Data Standards
     */
    public static TransformationRule genderWithNoneDefault() {
        return builder()
                .ruleName("GENDER_NONE_DEFAULT")
                .category("CODE_LIST_MAPPING")
                .description("Default null gender to 99 (Data not collected) per HUD Data Standards")
                .expressionTemplate("COALESCE(${gender}, 99)")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Integer")
                .hudReference("HUD Data Standards Gender Code List 1.3: 99=Data not collected")
                .exampleUsage("Gender: null → 99, Gender: 1 → 1")
                .vawaRelevant(false)
                .build();
    }

    /**
     * Length of stay calculation
     */
    public static TransformationRule lengthOfStay() {
        return builder()
                .ruleName("LENGTH_OF_STAY_DAYS")
                .category("DATE_CALCULATION")
                .description("Calculate length of stay in days")
                .expressionTemplate("DATEDIFF(COALESCE(${exitDate}, ${reportEndDate}), ${enrollmentDate})")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Integer")
                .hudReference("HUD APR Programming Specifications - Length of Stay calculation")
                .exampleUsage("Entry: 2024-01-01, Exit: 2024-03-01 → 59 days")
                .vawaRelevant(false)
                .build();
    }

    /**
     * DV victim field redaction per VAWA
     */
    public static TransformationRule vawaRedaction() {
        return builder()
                .ruleName("VAWA_DV_VICTIM_REDACTION")
                .category("CODE_LIST_MAPPING")
                .description("Redact DV victim status when consent not given per VAWA")
                .expressionTemplate("CASE WHEN ${consentGiven} = true THEN ${dvVictim} ELSE NULL END")
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("Boolean")
                .hudReference("VAWA Confidentiality Requirements - Client consent required for DV-related data disclosure")
                .exampleUsage("DV Victim: true, Consent: false → NULL")
                .vawaRelevant(true)
                .build();
    }

    /**
     * Project type grouping for APR
     */
    public static TransformationRule projectTypeGrouping() {
        return builder()
                .ruleName("PROJECT_TYPE_GROUPING")
                .category("PROJECT_TYPE_FILTER")
                .description("Group project types into ES, TH, PSH, RRH, SSO categories")
                .expressionTemplate(
                    "CASE " +
                    "WHEN ${projectType} IN (1, 2, 3, 8) THEN 'ES' " +
                    "WHEN ${projectType} = 13 THEN 'RRH' " +
                    "WHEN ${projectType} = 3 THEN 'PSH' " +
                    "WHEN ${projectType} = 2 THEN 'TH' " +
                    "WHEN ${projectType} = 6 THEN 'SSO' " +
                    "ELSE 'Other' END"
                )
                .transformLanguage(TransformLanguage.SQL)
                .returnDataType("String")
                .hudReference("HUD Project Type Code List 2.02: ES, TH, PSH, RRH, SSO")
                .exampleUsage("Project Type 1 → 'ES', Project Type 13 → 'RRH'")
                .vawaRelevant(false)
                .build();
    }
}
