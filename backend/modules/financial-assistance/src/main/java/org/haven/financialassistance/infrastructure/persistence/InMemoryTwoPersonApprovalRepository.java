package org.haven.financialassistance.infrastructure.persistence;

import org.haven.financialassistance.application.services.TwoPersonApprovalService;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of TwoPersonApprovalRepository.
 * This is a temporary implementation - should be replaced with proper JPA persistence.
 */
@Repository
public class InMemoryTwoPersonApprovalRepository implements TwoPersonApprovalService.TwoPersonApprovalRepository {

    private final Map<UUID, TwoPersonApprovalService.TwoPersonApprovalWorkflow> workflows = new ConcurrentHashMap<>();

    @Override
    public void save(TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow) {
        workflows.put(workflow.getWorkflowId(), workflow);
    }

    @Override
    public Optional<TwoPersonApprovalService.TwoPersonApprovalWorkflow> findById(UUID workflowId) {
        return Optional.ofNullable(workflows.get(workflowId));
    }

    @Override
    public List<TwoPersonApprovalService.TwoPersonApprovalWorkflow> findPendingApprovalsByUserRole(String userRole) {
        return workflows.values().stream()
            .filter(workflow -> workflow.getStatus() == TwoPersonApprovalService.ApprovalStatus.PENDING)
            .filter(workflow -> canRoleApprove(userRole, workflow))
            .collect(Collectors.toList());
    }

    @Override
    public List<TwoPersonApprovalService.TwoPersonApprovalWorkflow> findByStatus(TwoPersonApprovalService.ApprovalStatus status) {
        return workflows.values().stream()
            .filter(workflow -> workflow.getStatus() == status)
            .collect(Collectors.toList());
    }

    @Override
    public List<TwoPersonApprovalService.TwoPersonApprovalWorkflow> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return workflows.values().stream()
            .filter(workflow -> {
                LocalDate workflowDate = workflow.getRequestedAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                return !workflowDate.isBefore(startDate) && !workflowDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
    }

    private boolean canRoleApprove(String userRole, TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow) {
        TwoPersonApprovalService.ApprovalRequirement requirement = workflow.getApprovalRequirement();
        return requirement.allowedRoles().contains(userRole);
    }

    /**
     * Clear all workflows - useful for testing
     */
    public void clear() {
        workflows.clear();
    }

    /**
     * Get count of workflows by status - useful for monitoring
     */
    public long countByStatus(TwoPersonApprovalService.ApprovalStatus status) {
        return workflows.values().stream()
            .filter(workflow -> workflow.getStatus() == status)
            .count();
    }
}