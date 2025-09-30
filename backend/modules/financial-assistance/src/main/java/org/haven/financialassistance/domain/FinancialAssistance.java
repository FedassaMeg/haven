package org.haven.financialassistance.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.financialassistance.domain.events.*;
import org.haven.financialassistance.domain.ApprovalChain;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Financial Assistance aggregate for non-housing assistance needs
 */
public class FinancialAssistance extends AggregateRoot<FinancialAssistanceId> {
    
    private ClientId clientId;
    private ProgramEnrollmentId enrollmentId;
    private FinancialAssistanceType assistanceType;
    private AssistanceStatus status;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private String purpose;
    private String justification;
    private String requestedBy;
    private Boolean isEmergency;
    
    // Approval workflow
    private ApprovalChain approvalChain;
    private String fundingSourceCode;
    
    // Vendor/Payee information
    private String vendorId;
    private String vendorName;
    private String vendorType; // DIRECT_PAYMENT, VOUCHER, REIMBURSEMENT
    
    // Payment tracking
    private List<Payment> payments = new ArrayList<>();
    private BigDecimal totalPaid;
    private LocalDate paymentDueDate;
    
    private Instant createdAt;
    private Instant lastModified;
    
    public static FinancialAssistance request(ClientId clientId, ProgramEnrollmentId enrollmentId,
                                            FinancialAssistanceType assistanceType, BigDecimal requestedAmount,
                                            String purpose, String justification, String requestedBy, Boolean isEmergency) {
        FinancialAssistanceId id = FinancialAssistanceId.generate();
        FinancialAssistance assistance = new FinancialAssistance();
        assistance.apply(new FinancialAssistanceRequested(
            id.value(),
            clientId.value(),
            enrollmentId.value(),
            assistanceType,
            requestedAmount,
            purpose,
            justification,
            requestedBy,
            isEmergency,
            Instant.now()
        ));
        return assistance;
    }
    
    public void initiateApproval(String requiredApprovalLevel, Integer requiredApprovalCount) {
        if (status != AssistanceStatus.REQUESTED) {
            throw new IllegalStateException("Cannot initiate approval for assistance not in REQUESTED status");
        }
        
        // Emergency requests may have expedited approval processes
        if (isEmergency) {
            this.approvalChain = new ApprovalChain(requiredApprovalLevel, Math.min(requiredApprovalCount, 1));
        } else {
            this.approvalChain = new ApprovalChain(requiredApprovalLevel, requiredApprovalCount);
        }
        this.status = AssistanceStatus.PENDING_APPROVAL;
    }
    
    public void addApproval(UUID approverId, String approverRole, String approverName, String notes) {
        if (approvalChain == null) {
            throw new IllegalStateException("Approval chain not initialized");
        }
        
        approvalChain.addApproval(approverId, approverRole, approverName, notes);
        
        if (approvalChain.isApproved()) {
            // Auto-approve with requested amounts if approval chain is complete
            approveAssistance(requestedAmount, null, approverId.toString(), notes);
        }
    }
    
    public void approveAssistance(BigDecimal approvedAmount, String fundingSourceCode, 
                                String finalApproverId, String approvalNotes) {
        if (status != AssistanceStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot approve assistance not pending approval");
        }
        
        this.approvedAmount = approvedAmount;
        this.fundingSourceCode = fundingSourceCode;
        this.status = AssistanceStatus.APPROVED;
        this.lastModified = Instant.now();
        
        // For emergency requests, set immediate payment due date
        if (isEmergency) {
            this.paymentDueDate = LocalDate.now().plusDays(1);
        } else {
            this.paymentDueDate = LocalDate.now().plusDays(7);
        }
    }
    
    public void assignVendor(String vendorId, String vendorName, String vendorType) {
        if (status != AssistanceStatus.APPROVED) {
            throw new IllegalStateException("Cannot assign vendor to non-approved assistance");
        }
        
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.vendorType = vendorType;
        this.status = AssistanceStatus.VENDOR_ASSIGNED;
    }
    
    public void authorizePayment(BigDecimal amount, LocalDate paymentDate, String paymentMethod,
                               String authorizedBy) {
        authorizePayment(amount, paymentDate, paymentMethod, AssistancePaymentSubtype.OTHER,
                        null, null, authorizedBy);
    }
    
    public void authorizePayment(BigDecimal amount, LocalDate paymentDate, String paymentMethod,
                               AssistancePaymentSubtype subtype, LocalDate periodStart, LocalDate periodEnd,
                               String authorizedBy) {
        if (status != AssistanceStatus.VENDOR_ASSIGNED && status != AssistanceStatus.ACTIVE) {
            throw new IllegalStateException("Cannot authorize payment for assistance not vendor assigned or active");
        }
        
        // Check if we have remaining budget
        BigDecimal remainingBudget = approvedAmount.subtract(totalPaid);
        if (amount.compareTo(remainingBudget) > 0) {
            throw new IllegalStateException("Payment amount exceeds remaining budget");
        }
        
        // Additional validation for arrears
        if ((subtype == AssistancePaymentSubtype.RENT_ARREARS || 
             subtype == AssistancePaymentSubtype.UTILITY_ARREARS)) {
            if (periodStart == null || periodEnd == null) {
                throw new IllegalArgumentException("Arrears payments must specify period start and end dates");
            }
            if (periodStart.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Arrears period cannot be in the future");
            }
        }
        
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(
            paymentId,
            amount,
            paymentDate,
            paymentMethod,
            subtype,
            periodStart,
            periodEnd,
            vendorId,
            vendorName,
            authorizedBy
        );
        this.payments.add(payment);
        this.totalPaid = totalPaid.add(amount);
        
        if (status == AssistanceStatus.VENDOR_ASSIGNED) {
            this.status = AssistanceStatus.ACTIVE;
        }
        
        // Mark as completed if fully paid
        if (totalPaid.compareTo(approvedAmount) >= 0) {
            this.status = AssistanceStatus.COMPLETED;
        }
        
        this.lastModified = Instant.now();
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof FinancialAssistanceRequested e) {
            this.id = FinancialAssistanceId.of(e.financialAssistanceId());
            this.clientId = new ClientId(e.clientId());
            this.enrollmentId = new ProgramEnrollmentId(e.enrollmentId());
            this.assistanceType = e.assistanceType();
            this.requestedAmount = e.requestedAmount();
            this.purpose = e.purpose();
            this.justification = e.justification();
            this.requestedBy = e.requestedBy();
            this.isEmergency = e.isEmergency();
            this.status = AssistanceStatus.REQUESTED;
            this.totalPaid = BigDecimal.ZERO;
            this.createdAt = e.occurredAt();
            this.lastModified = e.occurredAt();
            
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum AssistanceStatus {
        REQUESTED,
        PENDING_APPROVAL,
        APPROVED,
        VENDOR_ASSIGNED,
        ACTIVE,
        COMPLETED,
        DENIED,
        CANCELLED
    }
    
    public static class Payment {
        private UUID paymentId;
        private BigDecimal amount;
        private LocalDate paymentDate;
        private String paymentMethod;
        private AssistancePaymentSubtype subtype;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String payeeId;
        private String payeeName;
        private String authorizedBy;
        private PaymentStatus status;
        
        public Payment(UUID paymentId, BigDecimal amount, LocalDate paymentDate, 
                      String paymentMethod, String payeeId, String payeeName, String authorizedBy) {
            this(paymentId, amount, paymentDate, paymentMethod, AssistancePaymentSubtype.OTHER,
                 null, null, payeeId, payeeName, authorizedBy);
        }
        
        public Payment(UUID paymentId, BigDecimal amount, LocalDate paymentDate, 
                      String paymentMethod, AssistancePaymentSubtype subtype,
                      LocalDate periodStart, LocalDate periodEnd,
                      String payeeId, String payeeName, String authorizedBy) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.paymentDate = paymentDate;
            this.paymentMethod = paymentMethod;
            this.subtype = subtype;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
            this.payeeId = payeeId;
            this.payeeName = payeeName;
            this.authorizedBy = authorizedBy;
            this.status = PaymentStatus.AUTHORIZED;
            
            // Validate arrears periods
            if (isArrearsSubtype() && (periodStart == null || periodEnd == null)) {
                throw new IllegalArgumentException("Arrears payments must specify period start and end dates");
            }
            if (periodStart != null && periodEnd != null && periodStart.isAfter(periodEnd)) {
                throw new IllegalArgumentException("Period start date must not be after period end date");
            }
        }
        
        private boolean isArrearsSubtype() {
            return subtype == AssistancePaymentSubtype.RENT_ARREARS || 
                   subtype == AssistancePaymentSubtype.UTILITY_ARREARS;
        }
        
        public enum PaymentStatus {
            AUTHORIZED, PROCESSED, FAILED, CANCELLED
        }
        
        // Getters
        public UUID getPaymentId() { return paymentId; }
        public BigDecimal getAmount() { return amount; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public String getPaymentMethod() { return paymentMethod; }
        public AssistancePaymentSubtype getSubtype() { return subtype; }
        public LocalDate getPeriodStart() { return periodStart; }
        public LocalDate getPeriodEnd() { return periodEnd; }
        public String getPayeeId() { return payeeId; }
        public String getPayeeName() { return payeeName; }
        public String getAuthorizedBy() { return authorizedBy; }
        public PaymentStatus getStatus() { return status; }
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public FinancialAssistanceType getAssistanceType() { return assistanceType; }
    public AssistanceStatus getStatus() { return status; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public String getPurpose() { return purpose; }
    public String getJustification() { return justification; }
    public String getRequestedBy() { return requestedBy; }
    public Boolean getIsEmergency() { return isEmergency; }
    public ApprovalChain getApprovalChain() { return approvalChain; }
    public String getFundingSourceCode() { return fundingSourceCode; }
    public String getVendorId() { return vendorId; }
    public String getVendorName() { return vendorName; }
    public String getVendorType() { return vendorType; }
    public List<Payment> getPayments() { return List.copyOf(payments); }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    
    public BigDecimal getRemainingBudget() {
        return approvedAmount != null ? approvedAmount.subtract(totalPaid) : BigDecimal.ZERO;
    }
    
    public boolean isEmergencyRequest() {
        return Boolean.TRUE.equals(isEmergency);
    }
    
    public boolean isOverdue() {
        return paymentDueDate != null && LocalDate.now().isAfter(paymentDueDate) && 
               status != AssistanceStatus.COMPLETED;
    }

    public enum AssistancePaymentSubtype {
        RENT_CURRENT,
        RENT_ARREARS,
        UTILITY_CURRENT,
        UTILITY_ARREARS,
        SECURITY_DEPOSIT,
        MOVING_COSTS,
        OTHER
    }
}
