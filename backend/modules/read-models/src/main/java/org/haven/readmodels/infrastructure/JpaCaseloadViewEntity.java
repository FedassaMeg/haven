package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.CaseloadView;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "caseload_view")
public class JpaCaseloadViewEntity {
    
    @Id
    private UUID caseId;
    
    @Column(name = "case_number")
    private String caseNumber;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "client_name")
    private String clientName;
    
    @Column(name = "worker_id")
    private UUID workerId;
    
    @Column(name = "worker_name")
    private String workerName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private CaseloadView.CaseStage stage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private CaseloadView.RiskLevel riskLevel;
    
    @Column(name = "program_name")
    private String programName;
    
    @Column(name = "program_id")
    private UUID programId;
    
    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;
    
    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;
    
    @Column(name = "service_count")
    private Integer serviceCount;
    
    @Column(name = "days_since_last_contact")
    private Integer daysSinceLastContact;
    
    @ElementCollection
    @CollectionTable(name = "caseload_active_alerts", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "alert")
    private List<String> activeAlerts;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CaseloadView.CaseStatus status;
    
    @Column(name = "requires_attention")
    private Boolean requiresAttention;
    
    @Column(name = "primary_need")
    private String primaryNeed;
    
    @Column(name = "last_updated")
    private Instant lastUpdated;
    
    @Column(name = "is_safe_at_home")
    private Boolean isSafeAtHome;
    
    @Column(name = "is_confidential_location")
    private Boolean isConfidentialLocation;
    
    @Column(name = "data_system")
    private String dataSystem;
    
    // Default constructor for JPA
    public JpaCaseloadViewEntity() {}
    
    // Constructor from domain object
    public JpaCaseloadViewEntity(CaseloadView view) {
        this.caseId = view.getCaseId();
        this.caseNumber = view.getCaseNumber();
        this.clientId = view.getClientId();
        this.clientName = view.getClientName();
        this.workerId = view.getWorkerId();
        this.workerName = view.getWorkerName();
        this.stage = view.getStage();
        this.riskLevel = view.getRiskLevel();
        this.programName = view.getProgramName();
        this.programId = view.getProgramId();
        this.enrollmentDate = view.getEnrollmentDate();
        this.lastServiceDate = view.getLastServiceDate();
        this.serviceCount = view.getServiceCount();
        this.daysSinceLastContact = view.getDaysSinceLastContact();
        this.activeAlerts = view.getActiveAlerts();
        this.status = view.getStatus();
        this.requiresAttention = view.getRequiresAttention();
        this.primaryNeed = view.getPrimaryNeed();
        this.lastUpdated = view.getLastUpdated();
        this.isSafeAtHome = view.getIsSafeAtHome();
        this.isConfidentialLocation = view.getIsConfidentialLocation();
        this.dataSystem = view.getDataSystem();
    }
    
    // Convert to domain object
    public CaseloadView toDomain() {
        CaseloadView view = new CaseloadView();
        view.setCaseId(this.caseId);
        view.setCaseNumber(this.caseNumber);
        view.setClientId(this.clientId);
        view.setClientName(this.clientName);
        view.setWorkerId(this.workerId);
        view.setWorkerName(this.workerName);
        view.setStage(this.stage);
        view.setRiskLevel(this.riskLevel);
        view.setProgramName(this.programName);
        view.setProgramId(this.programId);
        view.setEnrollmentDate(this.enrollmentDate);
        view.setLastServiceDate(this.lastServiceDate);
        view.setServiceCount(this.serviceCount);
        view.setDaysSinceLastContact(this.daysSinceLastContact);
        view.setActiveAlerts(this.activeAlerts);
        view.setStatus(this.status);
        view.setRequiresAttention(this.requiresAttention);
        view.setPrimaryNeed(this.primaryNeed);
        view.setLastUpdated(this.lastUpdated);
        view.setIsSafeAtHome(this.isSafeAtHome);
        view.setIsConfidentialLocation(this.isConfidentialLocation);
        view.setDataSystem(this.dataSystem);
        return view;
    }
    
    // Update from domain object
    public void updateFrom(CaseloadView view) {
        this.caseNumber = view.getCaseNumber();
        this.clientName = view.getClientName();
        this.workerId = view.getWorkerId();
        this.workerName = view.getWorkerName();
        this.stage = view.getStage();
        this.riskLevel = view.getRiskLevel();
        this.programName = view.getProgramName();
        this.programId = view.getProgramId();
        this.enrollmentDate = view.getEnrollmentDate();
        this.lastServiceDate = view.getLastServiceDate();
        this.serviceCount = view.getServiceCount();
        this.daysSinceLastContact = view.getDaysSinceLastContact();
        this.activeAlerts = view.getActiveAlerts();
        this.status = view.getStatus();
        this.requiresAttention = view.getRequiresAttention();
        this.primaryNeed = view.getPrimaryNeed();
        this.lastUpdated = view.getLastUpdated();
        this.isSafeAtHome = view.getIsSafeAtHome();
        this.isConfidentialLocation = view.getIsConfidentialLocation();
        this.dataSystem = view.getDataSystem();
    }
    
    // All getters and setters
    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public UUID getWorkerId() { return workerId; }
    public void setWorkerId(UUID workerId) { this.workerId = workerId; }
    
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    
    public CaseloadView.CaseStage getStage() { return stage; }
    public void setStage(CaseloadView.CaseStage stage) { this.stage = stage; }
    
    public CaseloadView.RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(CaseloadView.RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    
    public LocalDate getLastServiceDate() { return lastServiceDate; }
    public void setLastServiceDate(LocalDate lastServiceDate) { this.lastServiceDate = lastServiceDate; }
    
    public Integer getServiceCount() { return serviceCount; }
    public void setServiceCount(Integer serviceCount) { this.serviceCount = serviceCount; }
    
    public Integer getDaysSinceLastContact() { return daysSinceLastContact; }
    public void setDaysSinceLastContact(Integer daysSinceLastContact) { this.daysSinceLastContact = daysSinceLastContact; }
    
    public List<String> getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(List<String> activeAlerts) { this.activeAlerts = activeAlerts; }
    
    public CaseloadView.CaseStatus getStatus() { return status; }
    public void setStatus(CaseloadView.CaseStatus status) { this.status = status; }
    
    public Boolean getRequiresAttention() { return requiresAttention; }
    public void setRequiresAttention(Boolean requiresAttention) { this.requiresAttention = requiresAttention; }
    
    public String getPrimaryNeed() { return primaryNeed; }
    public void setPrimaryNeed(String primaryNeed) { this.primaryNeed = primaryNeed; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Boolean getIsSafeAtHome() { return isSafeAtHome; }
    public void setIsSafeAtHome(Boolean isSafeAtHome) { this.isSafeAtHome = isSafeAtHome; }
    
    public Boolean getIsConfidentialLocation() { return isConfidentialLocation; }
    public void setIsConfidentialLocation(Boolean isConfidentialLocation) { this.isConfidentialLocation = isConfidentialLocation; }
    
    public String getDataSystem() { return dataSystem; }
    public void setDataSystem(String dataSystem) { this.dataSystem = dataSystem; }
}