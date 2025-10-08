package org.haven.casemgmt.infrastructure.persistence;

import org.haven.casemgmt.domain.mandatedreport.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for MandatedReport persistence
 */
@Entity
@Table(name = "mandated_reports", schema = "haven")
public class JpaMandatedReportEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "case_id", nullable = false)
    private UUID caseId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status;
    
    @Column(name = "report_number", unique = true, length = 100)
    private String reportNumber;
    
    @Column(name = "reporting_agency", length = 200)
    private String reportingAgency;
    
    @Column(name = "agency_contact_info", length = 500)
    private String agencyContactInfo;
    
    @Column(name = "incident_description", columnDefinition = "TEXT")
    private String incidentDescription;
    
    @Column(name = "incident_date_time")
    private Instant incidentDateTime;
    
    @Column(name = "report_created_at", nullable = false)
    private Instant reportCreatedAt;
    
    @Column(name = "filing_deadline")
    private Instant filingDeadline;
    
    @Column(name = "filed_at")
    private Instant filedAt;
    
    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;
    
    @Column(name = "filed_by_user_id")
    private UUID filedByUserId;
    
    @Column(name = "investigation_outcome", columnDefinition = "TEXT")
    private String investigationOutcome;
    
    @Column(name = "agency_response", columnDefinition = "TEXT")
    private String agencyResponse;
    
    @Column(name = "is_emergency_report")
    private Boolean isEmergencyReport = false;
    
    @Column(name = "legal_justification", columnDefinition = "TEXT")
    private String legalJustification;
    
    @Version
    private Long version;
    
    // Constructors
    protected JpaMandatedReportEntity() {
        // JPA requires default constructor
    }
    
    public JpaMandatedReportEntity(UUID id, UUID caseId, UUID clientId, ReportType reportType,
                                 ReportStatus status, String reportNumber, String reportingAgency,
                                 String incidentDescription, Instant incidentDateTime,
                                 Instant reportCreatedAt, Instant filingDeadline,
                                 UUID createdByUserId, Boolean isEmergencyReport,
                                 String legalJustification) {
        this.id = id;
        this.caseId = caseId;
        this.clientId = clientId;
        this.reportType = reportType;
        this.status = status;
        this.reportNumber = reportNumber;
        this.reportingAgency = reportingAgency;
        this.incidentDescription = incidentDescription;
        this.incidentDateTime = incidentDateTime;
        this.reportCreatedAt = reportCreatedAt;
        this.filingDeadline = filingDeadline;
        this.createdByUserId = createdByUserId;
        this.isEmergencyReport = isEmergencyReport;
        this.legalJustification = legalJustification;
    }
    
    // Factory method from domain
    public static JpaMandatedReportEntity fromDomain(MandatedReport report) {
        return new JpaMandatedReportEntity(
            report.getId().value(),
            report.getCaseId().value(),
            report.getClientId(),
            report.getReportType(),
            report.getStatus(),
            report.getReportNumber(),
            report.getReportingAgency(),
            report.getIncidentDescription(),
            report.getIncidentDateTime(),
            report.getReportCreatedAt(),
            report.getFilingDeadline(),
            report.getCreatedByUserId(),
            report.isEmergencyReport(),
            report.getLegalJustification()
        );
    }
    
    // Convert to domain (simplified - would need event sourcing in full implementation)
    public MandatedReport toDomain() {
        // Note: This is a simplified reconstruction
        // In a full event-sourced implementation, you would replay events
        throw new UnsupportedOperationException("Domain reconstruction not implemented - use event sourcing");
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }
    
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    
    public String getReportNumber() { return reportNumber; }
    public void setReportNumber(String reportNumber) { this.reportNumber = reportNumber; }
    
    public String getReportingAgency() { return reportingAgency; }
    public void setReportingAgency(String reportingAgency) { this.reportingAgency = reportingAgency; }
    
    public String getAgencyContactInfo() { return agencyContactInfo; }
    public void setAgencyContactInfo(String agencyContactInfo) { this.agencyContactInfo = agencyContactInfo; }
    
    public String getIncidentDescription() { return incidentDescription; }
    public void setIncidentDescription(String incidentDescription) { this.incidentDescription = incidentDescription; }
    
    public Instant getIncidentDateTime() { return incidentDateTime; }
    public void setIncidentDateTime(Instant incidentDateTime) { this.incidentDateTime = incidentDateTime; }
    
    public Instant getReportCreatedAt() { return reportCreatedAt; }
    public void setReportCreatedAt(Instant reportCreatedAt) { this.reportCreatedAt = reportCreatedAt; }
    
    public Instant getFilingDeadline() { return filingDeadline; }
    public void setFilingDeadline(Instant filingDeadline) { this.filingDeadline = filingDeadline; }
    
    public Instant getFiledAt() { return filedAt; }
    public void setFiledAt(Instant filedAt) { this.filedAt = filedAt; }
    
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    
    public UUID getFiledByUserId() { return filedByUserId; }
    public void setFiledByUserId(UUID filedByUserId) { this.filedByUserId = filedByUserId; }
    
    public String getInvestigationOutcome() { return investigationOutcome; }
    public void setInvestigationOutcome(String investigationOutcome) { this.investigationOutcome = investigationOutcome; }
    
    public String getAgencyResponse() { return agencyResponse; }
    public void setAgencyResponse(String agencyResponse) { this.agencyResponse = agencyResponse; }
    
    public Boolean getIsEmergencyReport() { return isEmergencyReport; }
    public void setIsEmergencyReport(Boolean isEmergencyReport) { this.isEmergencyReport = isEmergencyReport; }
    
    public String getLegalJustification() { return legalJustification; }
    public void setLegalJustification(String legalJustification) { this.legalJustification = legalJustification; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}