package org.haven.reportingmetadata.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Links ReportSection to ReportingFieldMapping
 * Represents which fields contribute to which report sections
 */
@Entity
@Table(name = "section_field_mapping", indexes = {
    @Index(name = "idx_section_field_section", columnList = "section_id"),
    @Index(name = "idx_section_field_mapping", columnList = "field_mapping_id")
})
public class SectionFieldMapping {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Parent report section
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private ReportSection reportSection;

    /**
     * Field mapping used in this section
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_mapping_id", nullable = false)
    private ReportingFieldMapping fieldMapping;

    /**
     * Display order within section
     */
    @Column
    private Integer displayOrder;

    /**
     * Whether this field is required for this specific section
     */
    @NotNull
    @Column(nullable = false)
    private boolean requiredForSection;

    protected SectionFieldMapping() {
        // JPA constructor
    }

    public SectionFieldMapping(ReportingFieldMapping fieldMapping, Integer displayOrder, boolean requiredForSection) {
        this.fieldMapping = fieldMapping;
        this.displayOrder = displayOrder;
        this.requiredForSection = requiredForSection;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public ReportSection getReportSection() {
        return reportSection;
    }

    public ReportingFieldMapping getFieldMapping() {
        return fieldMapping;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public boolean isRequiredForSection() {
        return requiredForSection;
    }

    // Package-private setter for bidirectional relationship
    void setReportSection(ReportSection reportSection) {
        this.reportSection = reportSection;
    }
}
