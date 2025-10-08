package org.haven.financialassistance.application.services;

import org.haven.financialassistance.domain.ledger.FinancialLedgerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for TwoPersonApprovalService
 */
@ExtendWith(MockitoExtension.class)
class TwoPersonApprovalServiceTest {

    @Mock
    private TwoPersonApprovalService.TwoPersonApprovalRepository approvalRepository;

    @Mock
    private FinancialLedgerService ledgerService;

    private TwoPersonApprovalService approvalService;

    private FinancialLedgerId ledgerId;

    @BeforeEach
    void setUp() {
        approvalService = new TwoPersonApprovalService(approvalRepository, ledgerService);
        ledgerId = FinancialLedgerId.generate();
    }

    @Test
    @DisplayName("Should initiate approval for large disbursement")
    void shouldInitiateApprovalForLargeDisbursement() {
        // Given
        BigDecimal largeAmount = new BigDecimal("3000.00"); // Above threshold

        // When
        TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow = approvalService.initiateApproval(
            ledgerId,
            "TXN_001",
            largeAmount,
            "LANDLORD_001",
            "ABC Property",
            "Rent payment",
            "CASE_MANAGER_001"
        );

        // Then
        assertNotNull(workflow);
        assertEquals(ledgerId, workflow.getLedgerId());
        assertEquals("TXN_001", workflow.getTransactionId());
        assertEquals(largeAmount, workflow.getAmount());
        assertEquals(TwoPersonApprovalService.ApprovalStatus.PENDING, workflow.getStatus());
        assertEquals("CASE_MANAGER_001", workflow.getRequestedBy());

        verify(approvalRepository).save(workflow);
    }

    @Test
    @DisplayName("Should require two approvals for large disbursement")
    void shouldRequireTwoApprovalsForLargeDisbursement() {
        // Given
        BigDecimal largeAmount = new BigDecimal("5000.00");

        // When
        boolean requiresApproval = approvalService.requiresTwoPersonApproval(largeAmount);
        TwoPersonApprovalService.ApprovalRequirement requirement = approvalService.getApprovalRequirement(largeAmount);

        // Then
        assertTrue(requiresApproval);
        assertEquals(2, requirement.requiredApprovals());
        assertTrue(requirement.allowedRoles().contains("CASE_MANAGER") ||
                  requirement.allowedRoles().contains("FINANCIAL_ADMIN"));
    }

    @Test
    @DisplayName("Should not require approval for small disbursement")
    void shouldNotRequireApprovalForSmallDisbursement() {
        // Given
        BigDecimal smallAmount = new BigDecimal("1000.00"); // Below threshold

        // When
        boolean requiresApproval = approvalService.requiresTwoPersonApproval(smallAmount);

        // Then
        assertFalse(requiresApproval);
    }

    @Test
    @DisplayName("Should add approval and complete workflow when sufficient approvals")
    void shouldAddApprovalAndCompleteWorkflowWhenSufficientApprovals() {
        // Given
        TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow = createTestWorkflow();
        when(approvalRepository.findById(workflow.getWorkflowId())).thenReturn(Optional.of(workflow));

        // Add first approval
        approvalService.addApproval(
            workflow.getWorkflowId(),
            "APPROVER_001",
            "FINANCIAL_ADMIN",
            "Admin User",
            "Approved - legitimate expense"
        );

        // Verify first approval added but workflow still pending
        assertEquals(TwoPersonApprovalService.ApprovalStatus.PENDING, workflow.getStatus());
        assertEquals(1, workflow.getApprovals().size());

        // Add second approval
        approvalService.addApproval(
            workflow.getWorkflowId(),
            "APPROVER_002",
            "SUPERVISOR",
            "Supervisor User",
            "Approved - within budget"
        );

        // Then
        assertEquals(TwoPersonApprovalService.ApprovalStatus.APPROVED, workflow.getStatus());
        assertEquals(2, workflow.getApprovals().size());
        assertNotNull(workflow.getCompletedAt());

        verify(approvalRepository, times(2)).save(workflow);
        verify(ledgerService).recordPaymentTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should prevent duplicate approval from same user")
    void shouldPreventDuplicateApprovalFromSameUser() {
        // Given
        TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow = createTestWorkflow();
        when(approvalRepository.findById(workflow.getWorkflowId())).thenReturn(Optional.of(workflow));

        // Add first approval
        approvalService.addApproval(
            workflow.getWorkflowId(),
            "APPROVER_001",
            "FINANCIAL_ADMIN",
            "Admin User",
            "First approval"
        );

        // When & Then - attempt duplicate approval
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            approvalService.addApproval(
                workflow.getWorkflowId(),
                "APPROVER_001", // Same user
                "FINANCIAL_ADMIN",
                "Admin User",
                "Duplicate approval"
            );
        });

        assertTrue(exception.getMessage().contains("already provided approval"));
    }

    @Test
    @DisplayName("Should reject approval workflow")
    void shouldRejectApprovalWorkflow() {
        // Given
        TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow = createTestWorkflow();
        when(approvalRepository.findById(workflow.getWorkflowId())).thenReturn(Optional.of(workflow));

        // When
        approvalService.rejectApproval(
            workflow.getWorkflowId(),
            "SUPERVISOR_001",
            "Insufficient documentation"
        );

        // Then
        assertEquals(TwoPersonApprovalService.ApprovalStatus.REJECTED, workflow.getStatus());
        assertEquals("SUPERVISOR_001", workflow.getRejectedBy());
        assertEquals("Insufficient documentation", workflow.getRejectionReason());
        assertNotNull(workflow.getCompletedAt());

        verify(approvalRepository).save(workflow);
    }

    @Test
    @DisplayName("Should prevent approval after rejection")
    void shouldPreventApprovalAfterRejection() {
        // Given
        TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow = createTestWorkflow();
        workflow.reject("SUPERVISOR_001", "Rejected");
        when(approvalRepository.findById(workflow.getWorkflowId())).thenReturn(Optional.of(workflow));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            approvalService.addApproval(
                workflow.getWorkflowId(),
                "APPROVER_001",
                "FINANCIAL_ADMIN",
                "Admin User",
                "Attempting approval after rejection"
            );
        });

        assertTrue(exception.getMessage().contains("Cannot add approval to non-pending workflow"));
    }

    @Test
    @DisplayName("Should get pending approvals for user role")
    void shouldGetPendingApprovalsForUserRole() {
        // Given
        TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow1 = createTestWorkflow();
        TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow2 = createTestWorkflow();

        List<TwoPersonApprovalService.TwoPersonApprovalWorkflow> mockWorkflows = List.of(workflow1, workflow2);
        when(approvalRepository.findPendingApprovalsByUserRole("FINANCIAL_ADMIN")).thenReturn(mockWorkflows);

        // When
        List<TwoPersonApprovalService.TwoPersonApprovalWorkflow> result =
            approvalService.getPendingApprovalsForUser("USER_001", "FINANCIAL_ADMIN");

        // Then
        assertFalse(result.isEmpty());
        verify(approvalRepository).findPendingApprovalsByUserRole("FINANCIAL_ADMIN");
    }

    @Test
    @DisplayName("Should require higher approval for critical disbursements")
    void shouldRequireHigherApprovalForCriticalDisbursements() {
        // Given
        BigDecimal criticalAmount = new BigDecimal("15000.00"); // Above critical threshold

        // When
        TwoPersonApprovalService.ApprovalRequirement requirement = approvalService.getApprovalRequirement(criticalAmount);

        // Then
        assertEquals(2, requirement.requiredApprovals());
        assertTrue(requirement.requiresSupervisor());
        assertTrue(requirement.allowedRoles().contains("SUPERVISOR") ||
                  requirement.allowedRoles().contains("FINANCIAL_ADMIN"));
    }

    @Test
    @DisplayName("Should handle workflow completion with ledger service integration")
    void shouldHandleWorkflowCompletionWithLedgerServiceIntegration() {
        // Given
        TwoPersonApprovalService.TwoPersonApprovalWorkflow workflow = createTestWorkflow();
        when(approvalRepository.findById(workflow.getWorkflowId())).thenReturn(Optional.of(workflow));

        // When - add sufficient approvals to complete workflow
        approvalService.addApproval(workflow.getWorkflowId(), "APPROVER_001", "FINANCIAL_ADMIN", "Admin", "Approved");
        approvalService.addApproval(workflow.getWorkflowId(), "APPROVER_002", "SUPERVISOR", "Supervisor", "Approved");

        // Then - verify ledger service was called to record transaction
        verify(ledgerService).recordPaymentTransaction(
            eq(workflow.getLedgerId()),
            eq(workflow.getTransactionId()),
            any(), // assistance ID
            eq(workflow.getAmount()),
            any(), // funding source
            any(), // HUD category
            any(), // payment subtype
            eq(workflow.getPayeeId()),
            eq(workflow.getPayeeName()),
            any(), // payment date
            any(), // period start
            any(), // period end
            eq("TWO_PERSON_APPROVAL_SYSTEM")
        );
    }

    private TwoPersonApprovalService.TwoPersonApprovalWorkflow createTestWorkflow() {
        return new TwoPersonApprovalService.TwoPersonApprovalWorkflow(
            UUID.randomUUID(),
            ledgerId,
            "TXN_001",
            new BigDecimal("5000.00"),
            "LANDLORD_001",
            "ABC Property",
            "Large rent payment",
            new TwoPersonApprovalService.ApprovalRequirement(
                2,
                List.of("FINANCIAL_ADMIN", "SUPERVISOR"),
                false,
                "Large disbursement"
            ),
            "CASE_MANAGER_001",
            java.time.Instant.now()
        );
    }
}