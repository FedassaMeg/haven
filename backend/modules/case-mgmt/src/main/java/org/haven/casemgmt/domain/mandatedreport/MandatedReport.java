package org.haven.casemgmt.domain.mandatedreport;

import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.CaseId;
import org.haven.casemgmt.domain.mandatedreport.events.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mandated Report aggregate for managing required reporting workflows
 * Handles legal reporting requirements with proper compliance tracking
 */
public class MandatedReport extends AggregateRoot<MandatedReportId> {
    
    private CaseId caseId;
    private UUID clientId;
    private ReportType reportType;
    private ReportStatus status;
    private String reportNumber;
    private String reportingAgency;
    private String agencyContactInfo;
    private String incidentDescription;
    private Instant incidentDateTime;
    private Instant reportCreatedAt;
    private Instant filingDeadline;
    private Instant filedAt;
    private UUID createdByUserId;
    private UUID filedByUserId;
    private String investigationOutcome;
    private String agencyResponse;
    private List<DocumentAttachment> attachments = new ArrayList<>();
    private List<String> followUpActions = new ArrayList<>();
    private boolean isEmergencyReport;
    private String legalJustification;
    
    protected MandatedReport() {
        // Required for event sourcing reconstruction
    }
    
    /**
     * Create new mandated report
     */
    public static MandatedReport create(MandatedReportId reportId, CaseId caseId, UUID clientId,
                                      ReportType reportType, String incidentDescription,
                                      Instant incidentDateTime, UUID createdByUserId,
                                      String legalJustification) {
        
        MandatedReport report = new MandatedReport();
        
        Instant filingDeadline = calculateFilingDeadline(incidentDateTime, reportType);
        String reportNumber = generateReportNumber(reportType);
        
        report.apply(new MandatedReportCreated(
            reportId.value(),
            caseId.value(),
            clientId,
            reportType,
            reportNumber,
            reportType.getReportingAgency(),
            incidentDescription,
            incidentDateTime,
            filingDeadline,
            createdByUserId,
            reportType.isEmergency(),
            legalJustification,
            Instant.now()
        ));
        
        return report;
    }
    
    /**
     * File the report with appropriate agency
     */
    public void file(String agencyContactInfo, UUID filedByUserId) {
        if (status != ReportStatus.DRAFT) {
            throw new MandatedReportException("Can only file reports in DRAFT status");
        }
        
        validateRequiredAttachments();
        
        apply(new MandatedReportFiled(
            id.value(),
            caseId.value(),
            clientId,
            reportType,
            reportNumber,
            reportingAgency,
            agencyContactInfo,
            incidentDescription,
            incidentDateTime,
            filedByUserId,
            isEmergencyReport,
            Instant.now()
        ));
    }
    
    /**
     * Update report status based on agency response
     */
    public void updateStatus(ReportStatus newStatus, String statusReason, UUID updatedByUserId) {
        if (!status.canTransitionTo(newStatus)) {
            throw new MandatedReportException(
                String.format("Cannot transition from %s to %s", status, newStatus));
        }
        
        apply(new MandatedReportStatusUpdated(
            id.value(),
            caseId.value(),
            status,
            newStatus,
            statusReason,
            updatedByUserId,
            Instant.now()
        ));
    }
    
    /**
     * Add document attachment to report
     */
    public void addAttachment(DocumentAttachment attachment) {
        attachment.validate();
        
        // Check for duplicate attachments
        if (attachments.stream().anyMatch(att -> att.getDocumentId().equals(attachment.getDocumentId()))) {
            throw new MandatedReportException("Document already attached to this report");
        }
        
        apply(new DocumentAttached(
            id.value(),
            attachment.getDocumentId(),
            attachment.getFileName(),
            attachment.getDocumentType(),
            attachment.isRequired(),
            attachment.getAttachedByUserId(),
            Instant.now()
        ));
    }
    
    /**
     * Remove document attachment
     */
    public void removeAttachment(UUID documentId, UUID removedByUserId, String reason) {
        DocumentAttachment attachment = attachments.stream()
            .filter(att -> att.getDocumentId().equals(documentId))
            .findFirst()
            .orElseThrow(() -> new MandatedReportException("Document not found in attachments"));
        
        if (attachment.isRequired() && status != ReportStatus.DRAFT) {
            throw new MandatedReportException("Cannot remove required attachment from filed report");
        }
        
        apply(new DocumentDetached(
            id.value(),
            documentId,
            reason,
            removedByUserId,
            Instant.now()
        ));
    }
    
    /**
     * Record agency response or investigation update
     */
    public void recordAgencyResponse(String response, String investigationOutcome, UUID recordedByUserId) {
        apply(new AgencyResponseRecorded(
            id.value(),
            caseId.value(),
            response,
            investigationOutcome,
            recordedByUserId,
            Instant.now()
        ));
    }
    
    /**
     * Add follow-up action required
     */
    public void addFollowUpAction(String action, UUID addedByUserId) {
        if (action == null || action.trim().isEmpty()) {
            throw new MandatedReportException("Follow-up action cannot be empty");
        }
        
        apply(new FollowUpActionAdded(
            id.value(),
            action,
            addedByUserId,
            Instant.now()
        ));
    }
    
    /**
     * Mark report as overdue if past filing deadline
     */
    public void markOverdueIfNeeded() {
        if (status == ReportStatus.DRAFT && 
            filingDeadline != null && 
            filingDeadline.isBefore(Instant.now())) {
            
            apply(new MandatedReportOverdue(
                id.value(),
                caseId.value(),
                filingDeadline,
                Instant.now()
            ));
        }
    }
    
    /**
     * Check if report is overdue
     */
    public boolean isOverdue() {
        return status == ReportStatus.DRAFT && 
               filingDeadline != null && 
               filingDeadline.isBefore(Instant.now());
    }
    
    /**
     * Get time remaining to file report
     */
    public long getHoursUntilDeadline() {
        if (filingDeadline == null) return Long.MAX_VALUE;
        return ChronoUnit.HOURS.between(Instant.now(), filingDeadline);
    }
    
    /**
     * Validate that all required attachments are present
     */
    private void validateRequiredAttachments() {
        boolean hasRequiredAttachments = attachments.stream()
            .anyMatch(DocumentAttachment::isRequired);
        
        if (reportType.requiresFollowUp() && !hasRequiredAttachments) {
            throw new MandatedReportException("Emergency reports require at least one supporting document");
        }
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof MandatedReportCreated e) {
            this.id = new MandatedReportId(e.reportId());
            this.caseId = new CaseId(e.caseId());
            this.clientId = e.clientId();
            this.reportType = e.reportType();
            this.status = ReportStatus.DRAFT;
            this.reportNumber = e.reportNumber();
            this.reportingAgency = e.reportingAgency();
            this.incidentDescription = e.incidentDescription();
            this.incidentDateTime = e.incidentDateTime();
            this.filingDeadline = e.filingDeadline();
            this.reportCreatedAt = e.createdAt();
            this.createdByUserId = e.createdByUserId();
            this.isEmergencyReport = e.isEmergencyReport();
            this.legalJustification = e.legalJustification();
        } else if (event instanceof MandatedReportFiled e) {
            this.status = ReportStatus.FILED;
            this.agencyContactInfo = e.agencyContactInfo();
            this.filedAt = e.filedAt();
            this.filedByUserId = e.filedByUserId();
        } else if (event instanceof MandatedReportStatusUpdated e) {
            this.status = e.newStatus();
        } else if (event instanceof DocumentAttached e) {
            // Would load full attachment details from document service
            // For now, just track the reference
        } else if (event instanceof DocumentDetached e) {
            this.attachments.removeIf(att -> att.getDocumentId().equals(e.documentId()));
        } else if (event instanceof AgencyResponseRecorded e) {
            this.agencyResponse = e.response();
            this.investigationOutcome = e.investigationOutcome();
        } else if (event instanceof FollowUpActionAdded e) {
            this.followUpActions.add(e.action());
        } else if (event instanceof MandatedReportOverdue e) {
            this.status = ReportStatus.OVERDUE;
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    private static Instant calculateFilingDeadline(Instant incidentDateTime, ReportType reportType) {
        if (reportType.getHoursToFile() == 0) {
            return Instant.now(); // Immediate filing required
        }
        return incidentDateTime.plus(reportType.getHoursToFile(), ChronoUnit.HOURS);
    }
    
    private static String generateReportNumber(ReportType reportType) {
        String prefix;
        switch (reportType) {
            case CHILD_ABUSE:
                prefix = "CA";
                break;
            case ELDER_ABUSE:
                prefix = "EA";
                break;
            case DOMESTIC_VIOLENCE:
                prefix = "DV";
                break;
            case SEXUAL_ASSAULT:
                prefix = "SA";
                break;
            case HUMAN_TRAFFICKING:
                prefix = "HT";
                break;
            case MENTAL_HEALTH_HOLD:
                prefix = "MH";
                break;
            case COMMUNICABLE_DISEASE:
                prefix = "CD";
                break;
            case COURT_ORDERED:
                prefix = "CO";
                break;
            case WELFARE_FRAUD:
                prefix = "WF";
                break;
            case SUSPICIOUS_DEATH:
                prefix = "SD";
                break;
            default:
                prefix = "UR"; // Unknown Report
                break;
        }
        
        return prefix + "-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    // Getters
    public CaseId getCaseId() { return caseId; }
    public UUID getClientId() { return clientId; }
    public ReportType getReportType() { return reportType; }
    public ReportStatus getStatus() { return status; }
    public String getReportNumber() { return reportNumber; }
    public String getReportingAgency() { return reportingAgency; }
    public String getAgencyContactInfo() { return agencyContactInfo; }
    public String getIncidentDescription() { return incidentDescription; }
    public Instant getIncidentDateTime() { return incidentDateTime; }
    public Instant getReportCreatedAt() { return reportCreatedAt; }
    public Instant getFilingDeadline() { return filingDeadline; }
    public Instant getFiledAt() { return filedAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public UUID getFiledByUserId() { return filedByUserId; }
    public String getInvestigationOutcome() { return investigationOutcome; }
    public String getAgencyResponse() { return agencyResponse; }
    public List<DocumentAttachment> getAttachments() { return List.copyOf(attachments); }
    public List<String> getFollowUpActions() { return List.copyOf(followUpActions); }
    public boolean isEmergencyReport() { return isEmergencyReport; }
    public String getLegalJustification() { return legalJustification; }
    
    /**
     * Domain exception for mandated report business rule violations
     */
    public static class MandatedReportException extends RuntimeException {
        public MandatedReportException(String message) {
            super(message);
        }
        
        public MandatedReportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}