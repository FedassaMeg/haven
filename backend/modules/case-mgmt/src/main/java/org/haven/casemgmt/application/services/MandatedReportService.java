package org.haven.casemgmt.application.services;

import org.haven.casemgmt.domain.mandatedreport.*;
import org.haven.casemgmt.application.handlers.MandatedReportCommandHandler;
import org.haven.casemgmt.application.commands.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service for mandated report operations
 * Orchestrates document integration and note creation
 */
@Service
public class MandatedReportService {
    
    private final MandatedReportCommandHandler commandHandler;
    private final DocumentIntegrationService documentService;
    private final RestrictedNoteService restrictedNoteService;
    
    @Autowired
    public MandatedReportService(MandatedReportCommandHandler commandHandler,
                               DocumentIntegrationService documentService,
                               RestrictedNoteService restrictedNoteService) {
        this.commandHandler = commandHandler;
        this.documentService = documentService;
        this.restrictedNoteService = restrictedNoteService;
    }
    
    /**
     * Create mandated report with automatic restricted note
     */
    public MandatedReportId createMandatedReportWithNote(CreateMandatedReportCmd command) {
        // Create the mandated report
        MandatedReportId reportId = commandHandler.handle(command);
        
        // Automatically create restricted note for the report
        createAutomaticRestrictedNote(reportId, command);
        
        return reportId;
    }
    
    /**
     * File report with document validation and note updates
     */
    public void fileReportWithDocuments(FileMandatedReportCmd command, List<UUID> requiredDocumentIds) {
        // Validate all required documents are attached
        validateRequiredDocuments(command.reportId(), requiredDocumentIds);
        
        // File the report
        commandHandler.handle(command);
        
        // Update related notes with filing status
        updateNotesWithFilingStatus(command.reportId(), command.filedByUserId());
    }
    
    /**
     * Attach document with integration validation
     */
    public void attachDocumentWithValidation(AttachDocumentCmd command) {
        // Validate document exists in document management system
        documentService.validateDocumentExists(command.documentId());
        
        // Validate document content is appropriate for mandated reports
        documentService.validateDocumentContent(command.documentId(), command.mimeType());
        
        // Attach the document
        commandHandler.handle(command);
        
        // Create note about document attachment
        createDocumentAttachmentNote(command);
    }
    
    /**
     * Update report status with automatic note creation
     */
    public void updateStatusWithNote(UpdateReportStatusCmd command) {
        // Update the status
        commandHandler.handle(command);
        
        // Create note about status change
        createStatusChangeNote(command);
    }
    
    /**
     * Record agency response with follow-up notes
     */
    public void recordAgencyResponseWithNotes(RecordAgencyResponseCmd command) {
        // Record the response
        commandHandler.handle(command);
        
        // Create follow-up notes based on response
        createAgencyResponseNotes(command);
    }
    
    /**
     * Create automatic restricted note for new mandated report
     */
    private void createAutomaticRestrictedNote(MandatedReportId reportId, CreateMandatedReportCmd command) {
        String noteContent = String.format(
            "MANDATED REPORT CREATED - %s\n\n" +
            "Report Number: [To be assigned]\n" +
            "Report Type: %s\n" +
            "Reporting Agency: %s\n" +
            "Incident Date/Time: %s\n" +
            "Legal Justification: %s\n\n" +
            "CONFIDENTIAL: This note contains information related to a mandated report. " +
            "Access is restricted to authorized personnel only.\n\n" +
            "Description: %s",
            command.reportType().getDisplayName(),
            command.reportType().getDisplayName(),
            command.reportType().getReportingAgency(),
            command.incidentDateTime(),
            command.legalJustification(),
            command.incidentDescription()
        );
        
        restrictedNoteService.createRestrictedNote(
            command.clientId(),
            resolveClientName(command.clientId()),
            command.caseId(),
            resolveCaseNumber(command.caseId()),
            "MANDATED_REPORT",
            noteContent,
            "Mandated Report: " + command.reportType().getDisplayName(),
            command.createdByUserId(),
            resolveUserName(command.createdByUserId()),
            List.of(),
            "LEGAL_TEAM"
        );
    }
    
    /**
     * Validate required documents are attached
     */
    private void validateRequiredDocuments(UUID reportId, List<UUID> requiredDocumentIds) {
        for (UUID documentId : requiredDocumentIds) {
            if (!documentService.isDocumentAttachedToReport(reportId, documentId)) {
                throw new IllegalStateException(
                    "Required document not attached to report: " + documentId
                );
            }
        }
    }
    
    /**
     * Update notes with filing status
     */
    private void updateNotesWithFilingStatus(UUID reportId, UUID filedByUserId) {
        String updateContent = String.format(
            "MANDATED REPORT FILED\n\n" +
            "Filed at: %s\n" +
            "Filed by: %s\n" +
            "Status: Report has been officially submitted to appropriate agency.\n\n" +
            "Next steps: Monitor for agency acknowledgment and investigation progress.",
            Instant.now(),
            filedByUserId
        );
        
        restrictedNoteService.updateRestrictedNote(
            reportId,
            updateContent,
            filedByUserId,
            resolveUserName(filedByUserId),
            "Report filing status update"
        );
    }
    
    /**
     * Create note about document attachment
     */
    private void createDocumentAttachmentNote(AttachDocumentCmd command) {
        String noteContent = String.format(
            "DOCUMENT ATTACHED TO MANDATED REPORT\n\n" +
            "Document: %s\n" +
            "Type: %s\n" +
            "Required: %s\n" +
            "Attached by: %s\n" +
            "Attached at: %s\n\n" +
            "Description: %s",
            command.fileName(),
            command.documentType(),
            command.isRequired() ? "Yes" : "No",
            command.attachedByUserId(),
            Instant.now(),
            command.description()
        );
        
        restrictedNoteService.updateRestrictedNote(
            command.reportId(),
            noteContent,
            command.attachedByUserId(),
            resolveUserName(command.attachedByUserId()),
            "Document attachment"
        );
    }
    
    /**
     * Create note about status change
     */
    private void createStatusChangeNote(UpdateReportStatusCmd command) {
        String noteContent = String.format(
            "MANDATED REPORT STATUS UPDATE\n\n" +
            "New Status: %s\n" +
            "Reason: %s\n" +
            "Updated by: %s\n" +
            "Updated at: %s",
            command.newStatus().getDisplayName(),
            command.statusReason(),
            command.updatedByUserId(),
            Instant.now()
        );
        
        restrictedNoteService.updateRestrictedNote(
            command.reportId(),
            noteContent,
            command.updatedByUserId(),
            resolveUserName(command.updatedByUserId()),
            "Status change: " + command.statusReason()
        );
    }
    
    /**
     * Create notes based on agency response
     */
    private void createAgencyResponseNotes(RecordAgencyResponseCmd command) {
        String noteContent = String.format(
            "AGENCY RESPONSE RECEIVED\n\n" +
            "Response: %s\n" +
            "Investigation Outcome: %s\n" +
            "Recorded by: %s\n" +
            "Recorded at: %s",
            command.response(),
            command.investigationOutcome() != null ? command.investigationOutcome() : "Pending",
            command.recordedByUserId(),
            Instant.now()
        );
        
        restrictedNoteService.updateRestrictedNote(
            command.reportId(),
            noteContent,
            command.recordedByUserId(),
            resolveUserName(command.recordedByUserId()),
            "Agency response recorded"
        );
    }
    
    private String resolveClientName(UUID clientId) {
        return String.format("Client-%s", clientId.toString().substring(0, 8));
    }
    
    private String resolveCaseNumber(UUID caseId) {
        return String.format("CASE-%s", caseId.toString().substring(0, 8).toUpperCase());
    }
    
    private String resolveUserName(UUID userId) {
        return String.format("User-%s", userId.toString().substring(0, 8));
    }
}