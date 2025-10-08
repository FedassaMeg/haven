package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.ClientRepository;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.List;

/**
 * Domain service for complex case management business logic
 */
public class CaseDomainService {
    
    private final CaseRepository caseRepository;
    private final ClientRepository clientRepository;
    
    public CaseDomainService(CaseRepository caseRepository, ClientRepository clientRepository) {
        this.caseRepository = caseRepository;
        this.clientRepository = clientRepository;
    }
    
    /**
     * Validate case creation business rules
     */
    public void validateCaseCreation(ClientId clientId, CodeableConcept caseType) {
        // Verify client exists and is active
        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
        
        if (!client.isActive()) {
            throw new IllegalStateException("Cannot create case for inactive client");
        }
        
        // Check for duplicate active cases of same type
        List<CaseRecord> existingCases = caseRepository.findByClientId(clientId);
        boolean hasDuplicateActiveCase = existingCases.stream()
                .anyMatch(c -> c.isActive() && 
                              c.getCaseType().equals(caseType));
        
        if (hasDuplicateActiveCase) {
            throw new CaseDuplicationException(
                "Client already has an active case of type: " + caseType.text());
        }
    }
    
    /**
     * Calculate case priority based on business rules
     */
    public CodeableConcept calculatePriority(ClientId clientId, CodeableConcept caseType) {
        // Example business logic for priority calculation
        var existingCases = caseRepository.findByClientId(clientId);
        
        // High priority if client has multiple active cases
        long activeCaseCount = existingCases.stream()
                .mapToLong(c -> c.isActive() ? 1 : 0)
                .sum();
        
        if (activeCaseCount > 2) {
            return createPriorityCoding("high", "High Priority");
        } else if (activeCaseCount > 0) {
            return createPriorityCoding("medium", "Medium Priority");
        } else {
            return createPriorityCoding("low", "Low Priority");
        }
    }
    
    /**
     * Find cases requiring attention (overdue, high priority, etc.)
     */
    public List<CaseRecord> findCasesRequiringAttention() {
        return caseRepository.findActiveCases().stream()
                .filter(this::requiresAttention)
                .toList();
    }
    
    private boolean requiresAttention(CaseRecord caseRecord) {
        // Example: cases open for more than 30 days
        if (caseRecord.getPeriod() != null && caseRecord.getPeriod().start() != null) {
            long daysSinceCreation = java.time.Duration
                    .between(caseRecord.getPeriod().start(), Instant.now())
                    .toDays();
            return daysSinceCreation > 30;
        }
        return false;
    }
    
    private CodeableConcept createPriorityCoding(String code, String display) {
        var coding = new CodeableConcept.Coding(
                "http://haven.org/fhir/CodeSystem/case-priority",
                null, code, display, null);
        return new CodeableConcept(List.of(coding), display);
    }
    
    public static class CaseDuplicationException extends RuntimeException {
        public CaseDuplicationException(String message) {
            super(message);
        }
    }
}