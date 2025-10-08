package org.haven.reportingmetadata.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Report section or measure within a ReportSpecification
 * Represents individual questions (APR Q04a, Q23c) or metrics (SPM Metric1a)
 */
@Entity
@Table(name = "report_section", indexes = {
    @Index(name = "idx_section_spec", columnList = "specification_id"),
    @Index(name = "idx_section_code", columnList = "section_code")
})
public class ReportSection {

    @Id
    @GeneratedValue
    private UUID sectionId;

    /**
     * Parent specification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specification_id", nullable = false)
    private ReportSpecification reportSpecification;

    /**
     * Section code (Q04a, Metric1a, etc.)
     */
    @NotNull
    @Column(nullable = false, length = 50)
    private String sectionCode;

    /**
     * Section title
     */
    @NotNull
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * Detailed description
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Display order within parent specification
     */
    @Column
    private Integer displayOrder;

    /**
     * Field mappings for this section
     */
    @OneToMany(mappedBy = "reportSection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SectionFieldMapping> fieldMappings = new ArrayList<>();

    protected ReportSection() {
        // JPA constructor
    }

    public ReportSection(String sectionCode, String title, String description, Integer displayOrder) {
        this.sectionCode = sectionCode;
        this.title = title;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    // Getters
    public UUID getSectionId() {
        return sectionId;
    }

    public ReportSpecification getReportSpecification() {
        return reportSpecification;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public List<SectionFieldMapping> getFieldMappings() {
        return Collections.unmodifiableList(fieldMappings);
    }

    // Package-private setter for bidirectional relationship
    void setReportSpecification(ReportSpecification reportSpecification) {
        this.reportSpecification = reportSpecification;
    }

    /**
     * Add field mapping to section
     */
    public void addFieldMapping(SectionFieldMapping mapping) {
        fieldMappings.add(mapping);
        mapping.setReportSection(this);
    }
}
