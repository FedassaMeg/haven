package org.haven.reporting.application.services;

import org.haven.casemgmt.application.services.RestrictedNoteService;
import org.haven.casemgmt.domain.RestrictedNote;
import org.haven.reporting.domain.hmis.*;
import org.haven.shared.vo.hmis.HmisPersonalId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for filtering HMIS exports to exclude data from restricted notes
 * Ensures that privileged notes stay suppressed in exports
 */
@Service
public class RestrictedNoteExportFilterService {
    
    private final RestrictedNoteService restrictedNoteService;
    
    @Autowired
    public RestrictedNoteExportFilterService(RestrictedNoteService restrictedNoteService) {
        this.restrictedNoteService = restrictedNoteService;
    }
    
    /**
     * Convert HmisPersonalId to UUID for restricted notes lookup
     * Since HmisPersonalId is a deterministic hash of clientId, we need to reverse-map it
     * In production, this would use a mapping service or database lookup
     */
    private UUID convertPersonalIdToClientId(HmisPersonalId personalId) {
        // TODO: Implement proper personalId to clientId mapping service
        // This is a temporary implementation that assumes personalId can be parsed as UUID
        // In production, you would have a mapping table or service
        try {
            return UUID.fromString(personalId.value());
        } catch (IllegalArgumentException e) {
            // If personalId is not a valid UUID (due to hashing), 
            // return a null UUID - this will result in no restricted notes found
            // which defaults to allowing export (fail-safe)
            return null;
        }
    }
    
    /**
     * Filter client projections to exclude those with restricted visibility
     */
    public List<HmisClientProjection> filterClientProjections(List<HmisClientProjection> clients, 
                                                             UUID exportRequestedBy, 
                                                             List<String> exporterRoles) {
        return clients.stream()
            .filter(client -> {
                UUID clientId = convertPersonalIdToClientId(client.personalId());
                return clientId != null && hasExportPermission(clientId, exportRequestedBy, exporterRoles);
            })
            .map(client -> applyClientRedactions(client, exportRequestedBy, exporterRoles))
            .collect(Collectors.toList());
    }
    
    /**
     * Filter enrollment projections to exclude those with restricted visibility
     */
    public List<HmisEnrollmentProjection> filterEnrollmentProjections(List<HmisEnrollmentProjection> enrollments,
                                                                    UUID exportRequestedBy,
                                                                    List<String> exporterRoles) {
        return enrollments.stream()
            .filter(enrollment -> {
                UUID clientId = convertPersonalIdToClientId(enrollment.personalId());
                return clientId != null && hasExportPermission(clientId, exportRequestedBy, exporterRoles);
            })
            .map(enrollment -> applyEnrollmentRedactions(enrollment, exportRequestedBy, exporterRoles))
            .collect(Collectors.toList());
    }
    
    /**
     * Filter health and DV projections to exclude privileged counseling information
     */
    public List<HmisHealthAndDvProjection> filterHealthAndDvProjections(List<HmisHealthAndDvProjection> healthAndDv,
                                                                       UUID exportRequestedBy,
                                                                       List<String> exporterRoles) {
        return healthAndDv.stream()
            .filter(health -> {
                UUID clientId = convertPersonalIdToClientId(health.personalId());
                return clientId != null && hasExportPermission(clientId, exportRequestedBy, exporterRoles);
            })
            .map(health -> applyHealthDvRedactions(health, exportRequestedBy, exporterRoles))
            .collect(Collectors.toList());
    }
    
    /**
     * Filter income benefits projections
     */
    public List<HmisIncomeBenefitsProjection> filterIncomeBenefitsProjections(List<HmisIncomeBenefitsProjection> incomeBenefits,
                                                                            UUID exportRequestedBy,
                                                                            List<String> exporterRoles) {
        return incomeBenefits.stream()
            .filter(income -> {
                UUID clientId = convertPersonalIdToClientId(income.personalId());
                return clientId != null && hasExportPermission(clientId, exportRequestedBy, exporterRoles);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Check if user has permission to export data for a specific client
     */
    private boolean hasExportPermission(UUID clientId, UUID exportRequestedBy, List<String> exporterRoles) {
        // Get all accessible notes for this client
        List<RestrictedNote> clientNotes = restrictedNoteService.getAccessibleNotesForClient(
            clientId, exportRequestedBy, exporterRoles
        );
        
        // Check if any notes restrict export access
        boolean hasExportRestrictingNotes = clientNotes.stream()
            .anyMatch(note -> isExportRestricted(note, exporterRoles));
        
        if (hasExportRestrictingNotes) {
            return false;
        }
        
        // Check for specific export restrictions by note type
        boolean hasPrivilegedCounselingNotes = clientNotes.stream()
            .anyMatch(note -> note.getNoteType() == RestrictedNote.NoteType.PRIVILEGED_COUNSELING ||
                             note.getNoteType() == RestrictedNote.NoteType.ATTORNEY_CLIENT);
        
        if (hasPrivilegedCounselingNotes && !hasPrivilegedExportRole(exporterRoles)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a note restricts export access
     */
    private boolean isExportRestricted(RestrictedNote note, List<String> exporterRoles) {
        // Sealed notes cannot be exported unless unsealed
        if (note.isSealed()) {
            return true;
        }
        
        // Attorney-client privileged notes require legal role for export
        if (note.getVisibilityScope() == RestrictedNote.VisibilityScope.ATTORNEY_CLIENT &&
            !exporterRoles.contains("ATTORNEY") && !exporterRoles.contains("LEGAL_ADVOCATE")) {
            return true;
        }
        
        // Author-only notes cannot be exported by others
        if (note.getVisibilityScope() == RestrictedNote.VisibilityScope.AUTHOR_ONLY) {
            return true;
        }
        
        // Medical team notes require medical role for export
        if (note.getVisibilityScope() == RestrictedNote.VisibilityScope.MEDICAL_TEAM &&
            !hasPrivilegedExportRole(exporterRoles) &&
            !exporterRoles.contains("DOCTOR") && !exporterRoles.contains("NURSE") && 
            !exporterRoles.contains("MEDICAL_ADVOCATE")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if exporter has privileged export role
     */
    private boolean hasPrivilegedExportRole(List<String> exporterRoles) {
        return exporterRoles.contains("ADMINISTRATOR") ||
               exporterRoles.contains("COMPLIANCE_OFFICER") ||
               exporterRoles.contains("HMIS_LEAD") ||
               exporterRoles.contains("DATA_ANALYST");
    }
    
    /**
     * Apply redactions to client data based on restricted notes
     */
    private HmisClientProjection applyClientRedactions(HmisClientProjection client, 
                                                      UUID exportRequestedBy, 
                                                      List<String> exporterRoles) {
        List<RestrictedNote> clientNotes = restrictedNoteService.getAccessibleNotesForClient(
            convertPersonalIdToClientId(client.personalId()), exportRequestedBy, exporterRoles
        );
        
        // Check for Safe at Home or confidential location protections
        boolean hasLocationRestrictions = clientNotes.stream()
            .anyMatch(note -> note.getContent() != null && 
                (note.getContent().toLowerCase().contains("safe at home") ||
                 note.getContent().toLowerCase().contains("confidential location")));
        
        // Apply redactions if necessary
        if (hasLocationRestrictions) {
            // Create a copy with redacted location information
            return client.withRedactedLocation();
        }
        
        // Check for name confidentiality restrictions
        boolean hasNameRestrictions = clientNotes.stream()
            .anyMatch(note -> note.getNoteType() == RestrictedNote.NoteType.SAFETY_PLAN ||
                             (note.getContent() != null && 
                              note.getContent().toLowerCase().contains("name confidentiality")));
        
        if (hasNameRestrictions && !hasPrivilegedExportRole(exporterRoles)) {
            return client.withRedactedName();
        }
        
        return client;
    }
    
    /**
     * Apply redactions to enrollment data
     */
    private HmisEnrollmentProjection applyEnrollmentRedactions(HmisEnrollmentProjection enrollment,
                                                             UUID exportRequestedBy,
                                                             List<String> exporterRoles) {
        List<RestrictedNote> clientNotes = restrictedNoteService.getAccessibleNotesForClient(
            convertPersonalIdToClientId(enrollment.personalId()), exportRequestedBy, exporterRoles
        );
        
        // Check for prior living situation restrictions
        boolean hasPriorLivingRestrictions = clientNotes.stream()
            .anyMatch(note -> note.getNoteType() == RestrictedNote.NoteType.SAFETY_PLAN ||
                             note.getVisibilityScope() == RestrictedNote.VisibilityScope.AUTHOR_ONLY);
        
        if (hasPriorLivingRestrictions && !hasPrivilegedExportRole(exporterRoles)) {
            return enrollment.withRedactedPriorLiving();
        }
        
        return enrollment;
    }
    
    /**
     * Apply redactions to health and DV data
     */
    private HmisHealthAndDvProjection applyHealthDvRedactions(HmisHealthAndDvProjection healthDv,
                                                            UUID exportRequestedBy,
                                                            List<String> exporterRoles) {
        List<RestrictedNote> clientNotes = restrictedNoteService.getAccessibleNotesForClient(
            convertPersonalIdToClientId(healthDv.personalId()), exportRequestedBy, exporterRoles
        );
        
        // Check for privileged counseling or medical restrictions
        boolean hasPrivilegedHealthInfo = clientNotes.stream()
            .anyMatch(note -> note.getNoteType() == RestrictedNote.NoteType.PRIVILEGED_COUNSELING ||
                             note.getNoteType() == RestrictedNote.NoteType.MEDICAL ||
                             note.getVisibilityScope() == RestrictedNote.VisibilityScope.CLINICAL_ONLY);
        
        if (hasPrivilegedHealthInfo && !hasPrivilegedExportRole(exporterRoles) &&
            !exporterRoles.contains("CLINICIAN") && !exporterRoles.contains("DV_COUNSELOR")) {
            return healthDv.withRedactedHealthInfo();
        }
        
        // Check for DV-specific restrictions
        boolean hasDvRestrictions = clientNotes.stream()
            .anyMatch(note -> note.getContent() != null && 
                (note.getContent().toLowerCase().contains("domestic violence") ||
                 note.getContent().toLowerCase().contains("intimate partner")));
        
        if (hasDvRestrictions && !exporterRoles.contains("DV_COUNSELOR") && 
            !hasPrivilegedExportRole(exporterRoles)) {
            return healthDv.withRedactedDvInfo();
        }
        
        return healthDv;
    }
    
    /**
     * Generate export summary with filtering details
     */
    public ExportFilterSummary generateFilterSummary(int originalClientCount, 
                                                    int filteredClientCount,
                                                    int originalEnrollmentCount,
                                                    int filteredEnrollmentCount,
                                                    UUID exportRequestedBy,
                                                    List<String> exporterRoles) {
        ExportFilterSummary summary = new ExportFilterSummary();
        summary.setExportRequestedBy(exportRequestedBy);
        summary.setExporterRoles(exporterRoles);
        summary.setOriginalClientCount(originalClientCount);
        summary.setFilteredClientCount(filteredClientCount);
        summary.setOriginalEnrollmentCount(originalEnrollmentCount);
        summary.setFilteredEnrollmentCount(filteredEnrollmentCount);
        summary.setClientsFiltered(originalClientCount - filteredClientCount);
        summary.setEnrollmentsFiltered(originalEnrollmentCount - filteredEnrollmentCount);
        summary.setHasPrivilegedExportRole(hasPrivilegedExportRole(exporterRoles));
        
        return summary;
    }
    
    /**
     * Data class for export filter summary
     */
    public static class ExportFilterSummary {
        private UUID exportRequestedBy;
        private List<String> exporterRoles;
        private int originalClientCount;
        private int filteredClientCount;
        private int originalEnrollmentCount;
        private int filteredEnrollmentCount;
        private int clientsFiltered;
        private int enrollmentsFiltered;
        private boolean hasPrivilegedExportRole;
        
        // Getters and setters
        public UUID getExportRequestedBy() { return exportRequestedBy; }
        public void setExportRequestedBy(UUID exportRequestedBy) { this.exportRequestedBy = exportRequestedBy; }
        
        public List<String> getExporterRoles() { return exporterRoles; }
        public void setExporterRoles(List<String> exporterRoles) { this.exporterRoles = exporterRoles; }
        
        public int getOriginalClientCount() { return originalClientCount; }
        public void setOriginalClientCount(int originalClientCount) { this.originalClientCount = originalClientCount; }
        
        public int getFilteredClientCount() { return filteredClientCount; }
        public void setFilteredClientCount(int filteredClientCount) { this.filteredClientCount = filteredClientCount; }
        
        public int getOriginalEnrollmentCount() { return originalEnrollmentCount; }
        public void setOriginalEnrollmentCount(int originalEnrollmentCount) { this.originalEnrollmentCount = originalEnrollmentCount; }
        
        public int getFilteredEnrollmentCount() { return filteredEnrollmentCount; }
        public void setFilteredEnrollmentCount(int filteredEnrollmentCount) { this.filteredEnrollmentCount = filteredEnrollmentCount; }
        
        public int getClientsFiltered() { return clientsFiltered; }
        public void setClientsFiltered(int clientsFiltered) { this.clientsFiltered = clientsFiltered; }
        
        public int getEnrollmentsFiltered() { return enrollmentsFiltered; }
        public void setEnrollmentsFiltered(int enrollmentsFiltered) { this.enrollmentsFiltered = enrollmentsFiltered; }
        
        public boolean isHasPrivilegedExportRole() { return hasPrivilegedExportRole; }
        public void setHasPrivilegedExportRole(boolean hasPrivilegedExportRole) { this.hasPrivilegedExportRole = hasPrivilegedExportRole; }
    }
}