package org.haven.clientprofile.application.services;

import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.clientprofile.infrastructure.security.PIIRedactionService;
import org.haven.clientprofile.infrastructure.security.VSPDataAccessService;
import org.haven.clientprofile.infrastructure.security.DataSystemBoundaryEnforcer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for creating secure exports with proper redaction and access controls
 * Implements "minimum necessary" principle for all data sharing
 */
@Service
public class SecureExportService {
    
    private final PIIRedactionService redactionService;
    private final VSPDataAccessService vspDataAccessService;
    
    @Autowired
    public SecureExportService(PIIRedactionService redactionService, 
                              VSPDataAccessService vspDataAccessService) {
        this.redactionService = redactionService;
        this.vspDataAccessService = vspDataAccessService;
    }
    
    /**
     * Creates HMIS export with appropriate redaction for user's access level
     */
    @DataSystemBoundaryEnforcer.EnforceDataSystemBoundary(
        allowedSystems = {org.haven.clientprofile.domain.DataSystem.HMIS},
        requiresPIIAccess = true
    )
    public Map<String, Object> createHMISExport(Object clientData, PIIAccessContext context) {
        
        // Validate user can access HMIS data
        if (isVSPUser(context.getUserRoles())) {
            throw new ExportAccessException("VSP users cannot create HMIS exports containing PII");
        }
        
        Map<String, Object> exportData = redactionService.createExportProjection(
            clientData, context, PIIRedactionService.ExportType.HMIS_EXPORT
        );
        
        // Add export metadata
        exportData.put("exportType", "HMIS");
        exportData.put("exportedBy", context.getUserId());
        exportData.put("exportTimestamp", Instant.now());
        exportData.put("redactionLevel", "HMIS_STANDARD");
        exportData.put("privacyNotice", "This data contains PII and must be handled according to HMIS standards");
        
        logExportActivity(context.getUserId(), "HMIS_EXPORT", exportData.size());
        
        return exportData;
    }
    
    /**
     * Creates VSP-safe export with maximum redaction
     */
    @DataSystemBoundaryEnforcer.EnforceDataSystemBoundary(
        allowedSystems = {org.haven.clientprofile.domain.DataSystem.COMPARABLE_DB},
        requiresPIIAccess = false
    )
    public Map<String, Object> createVSPExport(Object clientData, PIIAccessContext context) {
        
        if (!isVSPUser(context.getUserRoles())) {
            throw new ExportAccessException("VSP export type only available to Victim Service Providers");
        }
        
        Map<String, Object> exportData = vspDataAccessService.createVSPExportProjection(
            clientData, context.getUserId()
        );
        
        // Add VSP-specific metadata
        exportData.put("exportType", "VSP_SHARING");
        exportData.put("exportedBy", context.getUserId());
        exportData.put("exportTimestamp", Instant.now());
        exportData.put("redactionLevel", "VSP_MAXIMUM");
        exportData.put("privacyNotice", "This data has been de-identified for victim service provider use");
        exportData.put("dataSource", "COMPARABLE_DB");
        
        logExportActivity(context.getUserId(), "VSP_EXPORT", exportData.size());
        
        return exportData;
    }
    
    /**
     * Creates research dataset with de-identified data
     */
    public Map<String, Object> createResearchExport(Object clientData, PIIAccessContext context,
                                                   String researchPurpose, String approvalNumber) {
        
        // Validate research authorization
        if (!hasResearchAuthorization(context.getUserId(), approvalNumber)) {
            throw new ExportAccessException("Invalid research authorization");
        }
        
        Map<String, Object> exportData = redactionService.createExportProjection(
            clientData, context, PIIRedactionService.ExportType.RESEARCH_DATASET
        );
        
        // Add research metadata
        exportData.put("exportType", "RESEARCH");
        exportData.put("researchPurpose", researchPurpose);
        exportData.put("approvalNumber", approvalNumber);
        exportData.put("exportedBy", context.getUserId());
        exportData.put("exportTimestamp", Instant.now());
        exportData.put("redactionLevel", "RESEARCH_DEIDENTIFIED");
        exportData.put("privacyNotice", "This dataset has been de-identified for research purposes");
        
        logExportActivity(context.getUserId(), "RESEARCH_EXPORT", exportData.size());
        
        return exportData;
    }
    
    /**
     * Creates court reporting export with legal protections
     */
    public Map<String, Object> createCourtReport(Object clientData, PIIAccessContext context,
                                                String courtOrder, String caseNumber) {
        
        // Validate court order
        if (!isValidCourtOrder(courtOrder)) {
            throw new ExportAccessException("Invalid court order for data export");
        }
        
        Map<String, Object> exportData = redactionService.createExportProjection(
            clientData, context, PIIRedactionService.ExportType.COURT_REPORTING
        );
        
        // Add court reporting metadata
        exportData.put("exportType", "COURT_REPORTING");
        exportData.put("courtOrder", courtOrder);
        exportData.put("caseNumber", caseNumber);
        exportData.put("exportedBy", context.getUserId());
        exportData.put("exportTimestamp", Instant.now());
        exportData.put("redactionLevel", "COURT_ORDERED");
        exportData.put("privacyNotice", "This data is provided pursuant to court order");
        
        logExportActivity(context.getUserId(), "COURT_REPORT", exportData.size());
        
        return exportData;
    }
    
    /**
     * Creates bulk export for multiple clients with consistent redaction
     */
    public List<Map<String, Object>> createBulkExport(List<Object> clientDataList, 
                                                     PIIAccessContext context,
                                                     PIIRedactionService.ExportType exportType) {
        
        // Validate bulk export authorization
        if (!hasBulkExportAuthorization(context.getUserId(), exportType)) {
            throw new ExportAccessException("User not authorized for bulk exports");
        }
        
        List<Map<String, Object>> exportDataList = clientDataList.stream()
            .map(clientData -> redactionService.createExportProjection(clientData, context, exportType))
            .toList();
        
        logExportActivity(context.getUserId(), "BULK_" + exportType.name(), exportDataList.size());
        
        return exportDataList;
    }
    
    /**
     * Validates export request meets security requirements
     */
    public boolean validateExportRequest(PIIAccessContext context, 
                                       PIIRedactionService.ExportType exportType,
                                       List<UUID> clientIds) {
        
        // Check user permissions
        if (!hasExportPermission(context.getUserId(), exportType)) {
            return false;
        }
        
        // Check VSP restrictions
        if (isVSPUser(context.getUserRoles()) && 
            exportType != PIIRedactionService.ExportType.VSP_SHARING) {
            return false;
        }
        
        // Check client-specific restrictions
        for (UUID clientId : clientIds) {
            if (!vspDataAccessService.canVSPAccessClient(clientId, context.getUserRoles())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets available export types for user
     */
    public List<PIIRedactionService.ExportType> getAvailableExportTypes(List<String> userRoles) {
        if (isVSPUser(userRoles)) {
            return List.of(PIIRedactionService.ExportType.VSP_SHARING);
        } else if (hasResearchRole(userRoles)) {
            return List.of(
                PIIRedactionService.ExportType.HMIS_EXPORT,
                PIIRedactionService.ExportType.RESEARCH_DATASET
            );
        } else if (hasAdminRole(userRoles)) {
            return List.of(PIIRedactionService.ExportType.values());
        } else {
            return List.of(PIIRedactionService.ExportType.HMIS_EXPORT);
        }
    }
    
    private boolean isVSPUser(List<String> userRoles) {
        return userRoles.contains("VSP") || userRoles.contains("VICTIM_SERVICE_PROVIDER");
    }
    
    private boolean hasResearchRole(List<String> userRoles) {
        return userRoles.contains("RESEARCHER") || userRoles.contains("DATA_ANALYST");
    }
    
    private boolean hasAdminRole(List<String> userRoles) {
        return userRoles.contains("ADMINISTRATOR") || userRoles.contains("SUPERVISOR");
    }
    
    private boolean hasExportPermission(UUID userId, PIIRedactionService.ExportType exportType) {
        // This would check user's specific export permissions
        return true; // Placeholder
    }
    
    private boolean hasResearchAuthorization(UUID userId, String approvalNumber) {
        // This would validate research approval with IRB/ethics board
        return approvalNumber != null && !approvalNumber.trim().isEmpty();
    }
    
    private boolean isValidCourtOrder(String courtOrder) {
        // This would validate court order format and authenticity
        return courtOrder != null && courtOrder.startsWith("COURT-");
    }
    
    private boolean hasBulkExportAuthorization(UUID userId, PIIRedactionService.ExportType exportType) {
        // This would check if user can perform bulk exports
        return true; // Placeholder
    }
    
    private void logExportActivity(UUID userId, String exportType, int recordCount) {
        System.out.println(String.format(
            "EXPORT_ACTIVITY: User=%s, Type=%s, Records=%d, Time=%s",
            userId, exportType, recordCount, Instant.now()
        ));
    }
    
    /**
     * Exception for export access violations
     */
    public static class ExportAccessException extends RuntimeException {
        public ExportAccessException(String message) {
            super(message);
        }
        
        public ExportAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}