package org.haven.housingassistance.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Records housing quality standards inspections
 */
public class InspectionRecord {
    private UUID inspectionId;
    private LocalDate inspectionDate;
    private String inspectorId;
    private String inspectorName;
    private InspectionType inspectionType;
    private InspectionResult result;
    private List<InspectionDeficiency> deficiencies = new ArrayList<>();
    private String overallNotes;
    private LocalDate nextInspectionDue;
    private Boolean meetsFairMarketRent;
    private Boolean meetsHQS; // Housing Quality Standards
    
    public InspectionRecord(LocalDate inspectionDate, String inspectorId, 
                          String inspectorName, InspectionType inspectionType) {
        this.inspectionId = UUID.randomUUID();
        this.inspectionDate = inspectionDate;
        this.inspectorId = inspectorId;
        this.inspectorName = inspectorName;
        this.inspectionType = inspectionType;
    }
    
    public void addDeficiency(InspectionDeficiency deficiency) {
        this.deficiencies.add(deficiency);
        updateResult();
    }
    
    public void markDeficiencyResolved(UUID deficiencyId, String resolvedBy, String resolutionNotes) {
        deficiencies.stream()
            .filter(d -> d.getId().equals(deficiencyId))
            .findFirst()
            .ifPresent(d -> d.markResolved(resolvedBy, resolutionNotes));
        updateResult();
    }
    
    private void updateResult() {
        boolean hasUnresolvedCritical = deficiencies.stream()
            .anyMatch(d -> !d.isResolved() && d.getSeverity() == DeficiencySeverity.CRITICAL);
            
        if (hasUnresolvedCritical) {
            this.result = InspectionResult.FAIL;
        } else {
            boolean hasUnresolvedMajor = deficiencies.stream()
                .anyMatch(d -> !d.isResolved() && d.getSeverity() == DeficiencySeverity.MAJOR);
            this.result = hasUnresolvedMajor ? InspectionResult.CONDITIONAL_PASS : InspectionResult.PASS;
        }
    }
    
    public enum InspectionType {
        INITIAL,
        ANNUAL,
        COMPLAINT,
        QUALITY_CONTROL,
        MOVE_OUT,
        EMERGENCY
    }
    
    public enum InspectionResult {
        PASS,
        CONDITIONAL_PASS,
        FAIL,
        INCOMPLETE
    }
    
    public enum DeficiencySeverity {
        CRITICAL,    // Life threatening or uninhabitable
        MAJOR,       // Significant but not immediately dangerous
        MINOR        // Cosmetic or minor issues
    }
    
    public static class InspectionDeficiency {
        private UUID id;
        private String category; // Electrical, Plumbing, HVAC, etc.
        private String description;
        private DeficiencySeverity severity;
        private Boolean isResolved;
        private String resolvedBy;
        private String resolutionNotes;
        private LocalDate resolvedDate;
        
        public InspectionDeficiency(String category, String description, DeficiencySeverity severity) {
            this.id = UUID.randomUUID();
            this.category = category;
            this.description = description;
            this.severity = severity;
            this.isResolved = false;
        }
        
        public void markResolved(String resolvedBy, String resolutionNotes) {
            this.isResolved = true;
            this.resolvedBy = resolvedBy;
            this.resolutionNotes = resolutionNotes;
            this.resolvedDate = LocalDate.now();
        }
        
        // Getters
        public UUID getId() { return id; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public DeficiencySeverity getSeverity() { return severity; }
        public Boolean isResolved() { return isResolved; }
        public String getResolvedBy() { return resolvedBy; }
        public String getResolutionNotes() { return resolutionNotes; }
        public LocalDate getResolvedDate() { return resolvedDate; }
    }
    
    // Getters
    public UUID getInspectionId() { return inspectionId; }
    public LocalDate getInspectionDate() { return inspectionDate; }
    public String getInspectorId() { return inspectorId; }
    public String getInspectorName() { return inspectorName; }
    public InspectionType getInspectionType() { return inspectionType; }
    public InspectionResult getResult() { return result; }
    public List<InspectionDeficiency> getDeficiencies() { return List.copyOf(deficiencies); }
    public String getOverallNotes() { return overallNotes; }
    public void setOverallNotes(String overallNotes) { this.overallNotes = overallNotes; }
    public LocalDate getNextInspectionDue() { return nextInspectionDue; }
    public void setNextInspectionDue(LocalDate nextInspectionDue) { this.nextInspectionDue = nextInspectionDue; }
    public Boolean getMeetsFairMarketRent() { return meetsFairMarketRent; }
    public void setMeetsFairMarketRent(Boolean meetsFairMarketRent) { this.meetsFairMarketRent = meetsFairMarketRent; }
    public Boolean getMeetsHQS() { return meetsHQS; }
    public void setMeetsHQS(Boolean meetsHQS) { this.meetsHQS = meetsHQS; }
}