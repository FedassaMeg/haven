package org.haven.api.caseload;

import org.haven.readmodels.domain.CaseloadView;
import org.haven.readmodels.infrastructure.CaseloadViewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/caseload")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class CaseloadController {
    
    private final CaseloadViewRepository caseloadRepository;
    
    public CaseloadController(CaseloadViewRepository caseloadRepository) {
        this.caseloadRepository = caseloadRepository;
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'CASE_MANAGER')")
    public ResponseEntity<CaseloadResponseDto> getCaseload(
            @RequestParam(required = false) String workerId,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String programId,
            @RequestParam(required = false) Boolean requiresAttention,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "riskLevel,ASC") String sort) {
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && 
            sortParams[1].equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        Page<CaseloadView> cases;
        
        // Apply filters
        if (workerId != null && stage != null) {
            CaseloadView.CaseStage caseStage = CaseloadView.CaseStage.valueOf(stage);
            cases = caseloadRepository.findByWorkerIdAndStage(
                UUID.fromString(workerId), caseStage, pageable
            );
        } else if (workerId != null && riskLevel != null) {
            CaseloadView.RiskLevel risk = CaseloadView.RiskLevel.valueOf(riskLevel);
            cases = caseloadRepository.findByWorkerIdAndRiskLevel(
                UUID.fromString(workerId), risk, pageable
            );
        } else if (workerId != null && requiresAttention != null && requiresAttention) {
            cases = caseloadRepository.findByWorkerIdAndRequiresAttentionTrue(
                UUID.fromString(workerId), pageable
            );
        } else if (workerId != null) {
            cases = caseloadRepository.findByWorkerId(UUID.fromString(workerId), pageable);
        } else if (programId != null) {
            cases = caseloadRepository.findByProgramAndStatus(
                UUID.fromString(programId), 
                CaseloadView.CaseStatus.OPEN, 
                pageable
            );
        } else {
            cases = caseloadRepository.findAll(pageable);
        }
        
        // Convert to DTOs
        List<CaseloadItemDto> items = cases.getContent().stream()
            .map(this::toCaseloadItemDto)
            .collect(Collectors.toList());
        
        // Get summary statistics
        Map<String, Long> stageCounts = new HashMap<>();
        Map<String, Long> riskCounts = new HashMap<>();
        
        if (workerId != null) {
            UUID workerUuid = UUID.fromString(workerId);
            for (CaseloadView.CaseStage s : CaseloadView.CaseStage.values()) {
                stageCounts.put(s.name(), caseloadRepository.countByWorkerAndStage(workerUuid, s));
            }
        }
        
        CaseloadResponseDto response = new CaseloadResponseDto();
        response.setCases(items);
        response.setTotalElements(cases.getTotalElements());
        response.setTotalPages(cases.getTotalPages());
        response.setCurrentPage(page);
        response.setStageCounts(stageCounts);
        response.setRiskCounts(riskCounts);
        response.setHighRiskCount(caseloadRepository.findHighRiskCases().size());
        response.setOverdueCount(caseloadRepository.findOverdueCases(7).size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my-cases")
    @PreAuthorize("hasAnyRole('CASE_MANAGER', 'SUPERVISOR')")
    public ResponseEntity<CaseloadResponseDto> getMyCases(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return getCaseload(userId, null, null, null, null, page, size, "riskLevel,ASC");
    }
    
    @GetMapping("/team-overview")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public ResponseEntity<TeamOverviewDto> getTeamOverview() {
        
        List<CaseloadView> allCases = caseloadRepository.findAll();
        
        // Group by worker
        Map<UUID, List<CaseloadView>> casesByWorker = allCases.stream()
            .filter(c -> c.getWorkerId() != null)
            .collect(Collectors.groupingBy(CaseloadView::getWorkerId));
        
        List<WorkerCaseloadDto> workerCaseloads = new ArrayList<>();
        
        for (Map.Entry<UUID, List<CaseloadView>> entry : casesByWorker.entrySet()) {
            WorkerCaseloadDto workerDto = new WorkerCaseloadDto();
            workerDto.setWorkerId(entry.getKey().toString());
            
            List<CaseloadView> workerCases = entry.getValue();
            workerDto.setTotalCases(workerCases.size());
            
            // Count by stage
            Map<CaseloadView.CaseStage, Long> stageCounts = workerCases.stream()
                .collect(Collectors.groupingBy(CaseloadView::getStage, Collectors.counting()));
            workerDto.setIntakeCases(stageCounts.getOrDefault(CaseloadView.CaseStage.INTAKE, 0L).intValue());
            workerDto.setActiveCases(stageCounts.getOrDefault(CaseloadView.CaseStage.ACTIVE, 0L).intValue());
            workerDto.setHousingSearchCases(stageCounts.getOrDefault(CaseloadView.CaseStage.HOUSING_SEARCH, 0L).intValue());
            
            // Count high risk
            long highRiskCount = workerCases.stream()
                .filter(c -> c.getRiskLevel() == CaseloadView.RiskLevel.CRITICAL || 
                            c.getRiskLevel() == CaseloadView.RiskLevel.HIGH)
                .count();
            workerDto.setHighRiskCases((int) highRiskCount);
            
            // Count requiring attention
            long attentionCount = workerCases.stream()
                .filter(c -> Boolean.TRUE.equals(c.getRequiresAttention()))
                .count();
            workerDto.setRequiringAttention((int) attentionCount);
            
            workerCaseloads.add(workerDto);
        }
        
        TeamOverviewDto overview = new TeamOverviewDto();
        overview.setWorkerCaseloads(workerCaseloads);
        overview.setTotalActiveCases(allCases.size());
        overview.setAverageCaseload(workerCaseloads.isEmpty() ? 0 : 
            allCases.size() / workerCaseloads.size());
        
        return ResponseEntity.ok(overview);
    }
    
    @GetMapping("/confidential")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<CaseloadItemDto>> getConfidentialCases() {
        
        List<CaseloadView> safeAtHome = caseloadRepository.findSafeAtHomeCases();
        List<CaseloadView> comparableDb = caseloadRepository.findComparableDbOnlyCases();
        
        Set<CaseloadView> confidentialCases = new HashSet<>();
        confidentialCases.addAll(safeAtHome);
        confidentialCases.addAll(comparableDb);
        
        List<CaseloadItemDto> items = confidentialCases.stream()
            .map(this::toCaseloadItemDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(items);
    }
    
    private CaseloadItemDto toCaseloadItemDto(CaseloadView view) {
        CaseloadItemDto dto = new CaseloadItemDto();
        dto.setCaseId(view.getCaseId().toString());
        dto.setCaseNumber(view.getCaseNumber());
        dto.setClientId(view.getClientId().toString());
        dto.setClientName(view.getClientName());
        dto.setWorkerId(view.getWorkerId() != null ? view.getWorkerId().toString() : null);
        dto.setWorkerName(view.getWorkerName());
        dto.setStage(view.getStage().name());
        dto.setStageDescription(view.getStageDescription());
        dto.setRiskLevel(view.getRiskLevel().name());
        dto.setProgramName(view.getProgramName());
        dto.setEnrollmentDate(view.getEnrollmentDate());
        dto.setLastServiceDate(view.getLastServiceDate());
        dto.setServiceCount(view.getServiceCount());
        dto.setDaysSinceLastContact(view.getDaysSinceLastContact());
        dto.setActiveAlerts(view.getActiveAlerts());
        dto.setStatus(view.getStatus().name());
        dto.setRequiresAttention(view.getRequiresAttention());
        dto.setNeedsUrgentAttention(view.needsUrgentAttention());
        dto.setIsOverdue(view.isOverdue());
        dto.setIsSafeAtHome(view.getIsSafeAtHome());
        dto.setIsConfidentialLocation(view.getIsConfidentialLocation());
        dto.setDataSystem(view.getDataSystem());
        return dto;
    }
    
    // DTOs
    public static class CaseloadResponseDto {
        private List<CaseloadItemDto> cases;
        private Long totalElements;
        private Integer totalPages;
        private Integer currentPage;
        private Map<String, Long> stageCounts;
        private Map<String, Long> riskCounts;
        private Integer highRiskCount;
        private Integer overdueCount;
        
        // Getters and Setters
        public List<CaseloadItemDto> getCases() { return cases; }
        public void setCases(List<CaseloadItemDto> cases) { this.cases = cases; }
        
        public Long getTotalElements() { return totalElements; }
        public void setTotalElements(Long totalElements) { this.totalElements = totalElements; }
        
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
        
        public Integer getCurrentPage() { return currentPage; }
        public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
        
        public Map<String, Long> getStageCounts() { return stageCounts; }
        public void setStageCounts(Map<String, Long> stageCounts) { this.stageCounts = stageCounts; }
        
        public Map<String, Long> getRiskCounts() { return riskCounts; }
        public void setRiskCounts(Map<String, Long> riskCounts) { this.riskCounts = riskCounts; }
        
        public Integer getHighRiskCount() { return highRiskCount; }
        public void setHighRiskCount(Integer highRiskCount) { this.highRiskCount = highRiskCount; }
        
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
    }
    
    public static class CaseloadItemDto {
        private String caseId;
        private String caseNumber;
        private String clientId;
        private String clientName;
        private String workerId;
        private String workerName;
        private String stage;
        private String stageDescription;
        private String riskLevel;
        private String programName;
        private LocalDate enrollmentDate;
        private LocalDate lastServiceDate;
        private Integer serviceCount;
        private Integer daysSinceLastContact;
        private List<String> activeAlerts;
        private String status;
        private Boolean requiresAttention;
        private Boolean needsUrgentAttention;
        private Boolean isOverdue;
        private Boolean isSafeAtHome;
        private Boolean isConfidentialLocation;
        private String dataSystem;
        
        // All getters and setters
        public String getCaseId() { return caseId; }
        public void setCaseId(String caseId) { this.caseId = caseId; }
        
        public String getCaseNumber() { return caseNumber; }
        public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        
        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }
        
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        
        public String getStageDescription() { return stageDescription; }
        public void setStageDescription(String stageDescription) { this.stageDescription = stageDescription; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        
        public String getProgramName() { return programName; }
        public void setProgramName(String programName) { this.programName = programName; }
        
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
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Boolean getRequiresAttention() { return requiresAttention; }
        public void setRequiresAttention(Boolean requiresAttention) { this.requiresAttention = requiresAttention; }
        
        public Boolean getNeedsUrgentAttention() { return needsUrgentAttention; }
        public void setNeedsUrgentAttention(Boolean needsUrgentAttention) { this.needsUrgentAttention = needsUrgentAttention; }
        
        public Boolean getIsOverdue() { return isOverdue; }
        public void setIsOverdue(Boolean isOverdue) { this.isOverdue = isOverdue; }
        
        public Boolean getIsSafeAtHome() { return isSafeAtHome; }
        public void setIsSafeAtHome(Boolean isSafeAtHome) { this.isSafeAtHome = isSafeAtHome; }
        
        public Boolean getIsConfidentialLocation() { return isConfidentialLocation; }
        public void setIsConfidentialLocation(Boolean isConfidentialLocation) { this.isConfidentialLocation = isConfidentialLocation; }
        
        public String getDataSystem() { return dataSystem; }
        public void setDataSystem(String dataSystem) { this.dataSystem = dataSystem; }
    }
    
    public static class TeamOverviewDto {
        private List<WorkerCaseloadDto> workerCaseloads;
        private Integer totalActiveCases;
        private Integer averageCaseload;
        
        public List<WorkerCaseloadDto> getWorkerCaseloads() { return workerCaseloads; }
        public void setWorkerCaseloads(List<WorkerCaseloadDto> workerCaseloads) { this.workerCaseloads = workerCaseloads; }
        
        public Integer getTotalActiveCases() { return totalActiveCases; }
        public void setTotalActiveCases(Integer totalActiveCases) { this.totalActiveCases = totalActiveCases; }
        
        public Integer getAverageCaseload() { return averageCaseload; }
        public void setAverageCaseload(Integer averageCaseload) { this.averageCaseload = averageCaseload; }
    }
    
    public static class WorkerCaseloadDto {
        private String workerId;
        private String workerName;
        private Integer totalCases;
        private Integer intakeCases;
        private Integer activeCases;
        private Integer housingSearchCases;
        private Integer highRiskCases;
        private Integer requiringAttention;
        
        // Getters and Setters
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        
        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }
        
        public Integer getTotalCases() { return totalCases; }
        public void setTotalCases(Integer totalCases) { this.totalCases = totalCases; }
        
        public Integer getIntakeCases() { return intakeCases; }
        public void setIntakeCases(Integer intakeCases) { this.intakeCases = intakeCases; }
        
        public Integer getActiveCases() { return activeCases; }
        public void setActiveCases(Integer activeCases) { this.activeCases = activeCases; }
        
        public Integer getHousingSearchCases() { return housingSearchCases; }
        public void setHousingSearchCases(Integer housingSearchCases) { this.housingSearchCases = housingSearchCases; }
        
        public Integer getHighRiskCases() { return highRiskCases; }
        public void setHighRiskCases(Integer highRiskCases) { this.highRiskCases = highRiskCases; }
        
        public Integer getRequiringAttention() { return requiringAttention; }
        public void setRequiringAttention(Integer requiringAttention) { this.requiringAttention = requiringAttention; }
    }
}