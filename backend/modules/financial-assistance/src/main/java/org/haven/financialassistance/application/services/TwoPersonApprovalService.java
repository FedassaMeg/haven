package org.haven.financialassistance.application.services;

import org.haven.financialassistance.domain.ledger.FinancialLedgerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing two-person approval workflow for large disbursements
 */
@Service
@Transactional
public class TwoPersonApprovalService {

    private final TwoPersonApprovalRepository approvalRepository;
    private final FinancialLedgerService ledgerService;

    // Configurable thresholds
    private static final BigDecimal LARGE_DISBURSEMENT_THRESHOLD = new BigDecimal("2500.00");
    private static final BigDecimal CRITICAL_DISBURSEMENT_THRESHOLD = new BigDecimal("10000.00");

    public TwoPersonApprovalService(TwoPersonApprovalRepository approvalRepository,
                                  FinancialLedgerService ledgerService) {
        this.approvalRepository = approvalRepository;
        this.ledgerService = ledgerService;
    }

    /**
     * Initiate a two-person approval workflow for a large disbursement
     */
    public TwoPersonApprovalWorkflow initiateApproval(FinancialLedgerId ledgerId, String transactionId,
                                                    BigDecimal amount, String payeeId, String payeeName,
                                                    String purpose, String requestedBy) {

        ApprovalRequirement requirement = determineApprovalRequirement(amount);

        TwoPersonApprovalWorkflow workflow = new TwoPersonApprovalWorkflow(
            UUID.randomUUID(),
            ledgerId,
            transactionId,
            amount,
            payeeId,
            payeeName,
            purpose,
            requirement,
            requestedBy,
            Instant.now()
        );

        approvalRepository.save(workflow);
        return workflow;
    }

    /**
     * Add an approval to an existing workflow
     */
    public void addApproval(UUID workflowId, String approverId, String approverRole,
                          String approverName, String comments) {
        TwoPersonApprovalWorkflow workflow = approvalRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Approval workflow not found: " + workflowId));

        if (workflow.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot add approval to non-pending workflow");
        }

        if (workflow.hasApprovalFromUser(approverId)) {
            throw new IllegalStateException("User has already provided approval for this workflow");
        }

        ApprovalRecord approval = new ApprovalRecord(
            UUID.randomUUID(),
            approverId,
            approverRole,
            approverName,
            comments,
            Instant.now()
        );

        workflow.addApproval(approval);

        // Check if workflow is now complete
        if (workflow.isApprovalComplete()) {
            workflow.approve();
            // Process the approved transaction
            processApprovedTransaction(workflow);
        }

        approvalRepository.save(workflow);
    }

    /**
     * Reject an approval workflow
     */
    public void rejectApproval(UUID workflowId, String rejectedBy, String rejectionReason) {
        TwoPersonApprovalWorkflow workflow = approvalRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Approval workflow not found: " + workflowId));

        if (workflow.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot reject non-pending workflow");
        }

        workflow.reject(rejectedBy, rejectionReason);
        approvalRepository.save(workflow);
    }

    /**
     * Get pending approvals for a specific user
     */
    @Transactional(readOnly = true)
    public List<TwoPersonApprovalWorkflow> getPendingApprovalsForUser(String userId, String userRole) {
        return approvalRepository.findPendingApprovalsByUserRole(userRole).stream()
            .filter(workflow -> !workflow.hasApprovalFromUser(userId))
            .filter(workflow -> canUserApprove(userId, userRole, workflow))
            .toList();
    }

    /**
     * Get all pending approvals (admin view)
     */
    @Transactional(readOnly = true)
    public List<TwoPersonApprovalWorkflow> getAllPendingApprovals() {
        return approvalRepository.findByStatus(ApprovalStatus.PENDING);
    }

    /**
     * Get approval history for audit purposes
     */
    @Transactional(readOnly = true)
    public List<TwoPersonApprovalWorkflow> getApprovalHistory(LocalDate startDate, LocalDate endDate) {
        return approvalRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Check if a transaction requires two-person approval
     */
    public boolean requiresTwoPersonApproval(BigDecimal amount) {
        return amount.compareTo(LARGE_DISBURSEMENT_THRESHOLD) >= 0;
    }

    /**
     * Get approval requirement details for an amount
     */
    public ApprovalRequirement getApprovalRequirement(BigDecimal amount) {
        return determineApprovalRequirement(amount);
    }

    private ApprovalRequirement determineApprovalRequirement(BigDecimal amount) {
        if (amount.compareTo(CRITICAL_DISBURSEMENT_THRESHOLD) >= 0) {
            return new ApprovalRequirement(
                2, // Required approvals
                List.of("FINANCIAL_ADMIN", "SUPERVISOR"), // Required roles
                true, // Requires supervisor
                "Critical disbursement over $10,000"
            );
        } else if (amount.compareTo(LARGE_DISBURSEMENT_THRESHOLD) >= 0) {
            return new ApprovalRequirement(
                2, // Required approvals
                List.of("CASE_MANAGER", "FINANCIAL_ADMIN"), // Allowed roles
                false, // Supervisor not required
                "Large disbursement over $2,500"
            );
        } else {
            return new ApprovalRequirement(
                1, // Single approval sufficient
                List.of("CASE_MANAGER"), // Allowed roles
                false, // Supervisor not required
                "Standard disbursement"
            );
        }
    }

    private boolean canUserApprove(String userId, String userRole, TwoPersonApprovalWorkflow workflow) {
        ApprovalRequirement requirement = workflow.getApprovalRequirement();

        // User cannot approve their own request
        if (userId.equals(workflow.getRequestedBy())) {
            return false;
        }

        // Check if user role is allowed
        if (!requirement.allowedRoles().contains(userRole)) {
            return false;
        }

        // If supervisor is required, check if we have supervisor approval
        if (requirement.requiresSupervisor()) {
            boolean hasSupervisorApproval = workflow.getApprovals().stream()
                .anyMatch(approval -> "SUPERVISOR".equals(approval.approverRole()));

            if (!hasSupervisorApproval && !"SUPERVISOR".equals(userRole)) {
                return false;
            }
        }

        return true;
    }

    private void processApprovedTransaction(TwoPersonApprovalWorkflow workflow) {
        // Record the transaction in the ledger
        try {
            ledgerService.recordPaymentTransaction(
                workflow.getLedgerId(),
                workflow.getTransactionId(),
                null, // assistance ID
                workflow.getAmount(),
                null, // funding source (to be determined)
                null, // HUD category (to be determined)
                org.haven.financialassistance.domain.ledger.PaymentSubtype.OTHER,
                workflow.getPayeeId(),
                workflow.getPayeeName(),
                LocalDate.now(),
                null, // period start
                null, // period end
                "TWO_PERSON_APPROVAL_SYSTEM"
            );
        } catch (Exception e) {
            // If transaction processing fails, mark workflow as failed
            workflow.fail("Transaction processing failed: " + e.getMessage());
            approvalRepository.save(workflow);
        }
    }

    // Domain classes for approval workflow
    public static class TwoPersonApprovalWorkflow {
        private UUID workflowId;
        private FinancialLedgerId ledgerId;
        private String transactionId;
        private BigDecimal amount;
        private String payeeId;
        private String payeeName;
        private String purpose;
        private ApprovalRequirement approvalRequirement;
        private ApprovalStatus status;
        private String requestedBy;
        private Instant requestedAt;
        private List<ApprovalRecord> approvals;
        private String rejectedBy;
        private String rejectionReason;
        private Instant completedAt;

        public TwoPersonApprovalWorkflow(UUID workflowId, FinancialLedgerId ledgerId, String transactionId,
                                       BigDecimal amount, String payeeId, String payeeName, String purpose,
                                       ApprovalRequirement approvalRequirement, String requestedBy, Instant requestedAt) {
            this.workflowId = workflowId;
            this.ledgerId = ledgerId;
            this.transactionId = transactionId;
            this.amount = amount;
            this.payeeId = payeeId;
            this.payeeName = payeeName;
            this.purpose = purpose;
            this.approvalRequirement = approvalRequirement;
            this.status = ApprovalStatus.PENDING;
            this.requestedBy = requestedBy;
            this.requestedAt = requestedAt;
            this.approvals = new ArrayList<>();
        }

        public void addApproval(ApprovalRecord approval) {
            this.approvals.add(approval);
        }

        public boolean hasApprovalFromUser(String userId) {
            return approvals.stream()
                .anyMatch(approval -> userId.equals(approval.approverId()));
        }

        public boolean isApprovalComplete() {
            return approvals.size() >= approvalRequirement.requiredApprovals();
        }

        public void approve() {
            this.status = ApprovalStatus.APPROVED;
            this.completedAt = Instant.now();
        }

        public void reject(String rejectedBy, String rejectionReason) {
            this.status = ApprovalStatus.REJECTED;
            this.rejectedBy = rejectedBy;
            this.rejectionReason = rejectionReason;
            this.completedAt = Instant.now();
        }

        public void fail(String failureReason) {
            this.status = ApprovalStatus.FAILED;
            this.rejectionReason = failureReason;
            this.completedAt = Instant.now();
        }

        // Getters
        public UUID getWorkflowId() { return workflowId; }
        public FinancialLedgerId getLedgerId() { return ledgerId; }
        public String getTransactionId() { return transactionId; }
        public BigDecimal getAmount() { return amount; }
        public String getPayeeId() { return payeeId; }
        public String getPayeeName() { return payeeName; }
        public String getPurpose() { return purpose; }
        public ApprovalRequirement getApprovalRequirement() { return approvalRequirement; }
        public ApprovalStatus getStatus() { return status; }
        public String getRequestedBy() { return requestedBy; }
        public Instant getRequestedAt() { return requestedAt; }
        public List<ApprovalRecord> getApprovals() { return List.copyOf(approvals); }
        public String getRejectedBy() { return rejectedBy; }
        public String getRejectionReason() { return rejectionReason; }
        public Instant getCompletedAt() { return completedAt; }
    }

    public record ApprovalRecord(
        UUID approvalId,
        String approverId,
        String approverRole,
        String approverName,
        String comments,
        Instant approvedAt
    ) {}

    public record ApprovalRequirement(
        int requiredApprovals,
        List<String> allowedRoles,
        boolean requiresSupervisor,
        String description
    ) {}

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        FAILED
    }

    // Repository interface (to be implemented)
    public interface TwoPersonApprovalRepository {
        void save(TwoPersonApprovalWorkflow workflow);
        Optional<TwoPersonApprovalWorkflow> findById(UUID workflowId);
        List<TwoPersonApprovalWorkflow> findPendingApprovalsByUserRole(String userRole);
        List<TwoPersonApprovalWorkflow> findByStatus(ApprovalStatus status);
        List<TwoPersonApprovalWorkflow> findByDateRange(LocalDate startDate, LocalDate endDate);
    }
}