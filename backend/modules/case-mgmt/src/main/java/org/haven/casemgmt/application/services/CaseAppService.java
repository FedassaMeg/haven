package org.haven.casemgmt.application.services;

import org.haven.casemgmt.application.commands.*;
import org.haven.casemgmt.application.queries.*;
import org.haven.casemgmt.application.dto.*;
import org.haven.casemgmt.domain.*;
import org.haven.clientprofile.domain.ClientId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CaseAppService {
    
    private final CaseRepository caseRepository;
    private final CaseDomainService caseDomainService;
    
    public CaseAppService(CaseRepository caseRepository, CaseDomainService caseDomainService) {
        this.caseRepository = caseRepository;
        this.caseDomainService = caseDomainService;
    }
    
    public CaseId handle(OpenCaseCmd cmd) {
        caseDomainService.validateCaseCreation(cmd.clientId(), cmd.caseType());
        
        // Auto-calculate priority if not provided
        var priority = cmd.priority() != null ? 
            cmd.priority() : 
            caseDomainService.calculatePriority(cmd.clientId(), cmd.caseType());
        
        CaseRecord caseRecord = CaseRecord.open(
            cmd.clientId(), 
            cmd.caseType(), 
            priority, 
            cmd.description()
        );
        
        caseRepository.save(caseRecord);
        return caseRecord.getId();
    }
    
    public void handle(AssignCaseCmd cmd) {
        CaseRecord caseRecord = caseRepository.findById(cmd.caseId())
            .orElseThrow(() -> new IllegalArgumentException("Case not found: " + cmd.caseId()));
            
        caseRecord.assignTo(cmd.assigneeId(), cmd.assigneeName(), cmd.role(), 
                           cmd.assignmentType(), cmd.reason(), cmd.assignedBy());
        caseRepository.save(caseRecord);
    }
    
    public void handle(AddCaseNoteCmd cmd) {
        CaseRecord caseRecord = caseRepository.findById(cmd.caseId())
            .orElseThrow(() -> new IllegalArgumentException("Case not found: " + cmd.caseId()));
            
        caseRecord.addNote(cmd.content(), cmd.authorId());
        caseRepository.save(caseRecord);
    }
    
    public void handle(UpdateCaseStatusCmd cmd) {
        CaseRecord caseRecord = caseRepository.findById(cmd.caseId())
            .orElseThrow(() -> new IllegalArgumentException("Case not found: " + cmd.caseId()));
            
        caseRecord.updateStatus(cmd.newStatus());
        caseRepository.save(caseRecord);
    }
    
    public void handle(CloseCaseCmd cmd) {
        CaseRecord caseRecord = caseRepository.findById(cmd.caseId())
            .orElseThrow(() -> new IllegalArgumentException("Case not found: " + cmd.caseId()));
        caseRecord.close(cmd.reason());
        caseRepository.save(caseRecord);
    }
    
    @Transactional(readOnly = true)
    public Optional<CaseDto> handle(GetCaseQuery query) {
        return caseRepository.findById(query.caseId())
            .map(this::toDto);
    }
    
    @Transactional(readOnly = true)
    public List<CaseDto> handle(GetCasesByClientQuery query) {
        return caseRepository.findByClientId(query.clientId())
            .stream().map(this::toDto).toList();
    }
    
    @Transactional(readOnly = true)
    public List<CaseDto> handle(GetCasesByAssigneeQuery query) {
        return caseRepository.findByAssignee(query.assigneeId())
            .stream().map(this::toDto).toList();
    }
    
    @Transactional(readOnly = true)
    public List<CaseDto> handle(GetActiveCasesQuery query) {
        return caseRepository.findActiveCases()
            .stream().map(this::toDto).toList();
    }
    
    @Transactional(readOnly = true)
    public List<CaseDto> getCasesRequiringAttention() {
        return caseDomainService.findCasesRequiringAttention()
            .stream().map(this::toDto).toList();
    }
    
    private CaseDto toDto(CaseRecord caseRecord) {
        return new CaseDto(
            caseRecord.getId().value(),
            caseRecord.getClientId().value(),
            caseRecord.getCaseType(),
            caseRecord.getPriority(),
            caseRecord.getStatus(),
            caseRecord.getDescription(),
            caseRecord.getCurrentPrimaryAssignment().orElse(null),
            caseRecord.getNotes().size(),
            caseRecord.getCreatedAt(),
            caseRecord.getPeriod()
        );
    }
}