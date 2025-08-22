package org.haven.casemgmt.application.handlers;

import org.haven.casemgmt.application.commands.*;
import org.haven.casemgmt.domain.mandatedreport.*;
import org.haven.casemgmt.domain.CaseId;
import org.haven.shared.domain.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handlers for mandated report operations
 * Ensures proper validation and enforcement of reporting business rules
 */
@Service
@Transactional
public class MandatedReportCommandHandler {
    
    private final Repository<MandatedReport, MandatedReportId> mandatedReportRepository;
    
    @Autowired
    public MandatedReportCommandHandler(Repository<MandatedReport, MandatedReportId> mandatedReportRepository) {
        this.mandatedReportRepository = mandatedReportRepository;
    }
    
    /**
     * Handle create mandated report command
     */
    public MandatedReportId handle(CreateMandatedReportCmd command) {
        command.validate();
        
        MandatedReportId reportId = MandatedReportId.newId();
        CaseId caseId = new CaseId(command.caseId());
        
        MandatedReport report = MandatedReport.create(
            reportId,
            caseId,
            command.clientId(),
            command.reportType(),
            command.incidentDescription(),
            command.incidentDateTime(),
            command.createdByUserId(),
            command.legalJustification()
        );
        
        mandatedReportRepository.save(report);
        
        return reportId;
    }
    
    /**
     * Handle file mandated report command
     */
    public void handle(FileMandatedReportCmd command) {
        command.validate();
        
        MandatedReport report = mandatedReportRepository.findById(new MandatedReportId(command.reportId()))
            .orElseThrow(() -> new MandatedReportNotFoundException("Mandated report not found: " + command.reportId()));
        
        report.file(command.agencyContactInfo(), command.filedByUserId());
        
        mandatedReportRepository.save(report);
    }
    
    /**
     * Handle update report status command
     */
    public void handle(UpdateReportStatusCmd command) {
        command.validate();
        
        MandatedReport report = mandatedReportRepository.findById(new MandatedReportId(command.reportId()))
            .orElseThrow(() -> new MandatedReportNotFoundException("Mandated report not found: " + command.reportId()));
        
        report.updateStatus(command.newStatus(), command.statusReason(), command.updatedByUserId());
        
        mandatedReportRepository.save(report);
    }
    
    /**
     * Handle attach document command
     */
    public void handle(AttachDocumentCmd command) {
        command.validate();
        
        MandatedReport report = mandatedReportRepository.findById(new MandatedReportId(command.reportId()))
            .orElseThrow(() -> new MandatedReportNotFoundException("Mandated report not found: " + command.reportId()));
        
        DocumentAttachment attachment = new DocumentAttachment(
            command.documentId(),
            command.fileName(),
            command.documentType(),
            command.fileSize(),
            command.mimeType(),
            command.attachedByUserId(),
            command.isRequired(),
            command.description()
        );
        
        report.addAttachment(attachment);
        
        mandatedReportRepository.save(report);
    }
    
    /**
     * Handle record agency response command
     */
    public void handle(RecordAgencyResponseCmd command) {
        command.validate();
        
        MandatedReport report = mandatedReportRepository.findById(new MandatedReportId(command.reportId()))
            .orElseThrow(() -> new MandatedReportNotFoundException("Mandated report not found: " + command.reportId()));
        
        report.recordAgencyResponse(
            command.response(),
            command.investigationOutcome(),
            command.recordedByUserId()
        );
        
        mandatedReportRepository.save(report);
    }
    
    /**
     * Mark overdue reports (typically called by scheduled job)
     */
    public void markOverdueReports() {
        // This would typically use a query to find all draft reports past deadline
        // For now, we'll implement as a placeholder
        System.out.println("Checking for overdue mandated reports at: " + java.time.Instant.now());
    }
    
    /**
     * Exception for mandated report not found scenarios
     */
    public static class MandatedReportNotFoundException extends RuntimeException {
        public MandatedReportNotFoundException(String message) {
            super(message);
        }
    }
}