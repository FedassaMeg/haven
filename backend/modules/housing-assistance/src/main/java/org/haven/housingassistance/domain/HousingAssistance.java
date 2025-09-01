package org.haven.housingassistance.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.housingassistance.domain.events.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Housing Assistance aggregate managing RRH/TH flows, approvals, and payments
 */
public class HousingAssistance extends AggregateRoot<HousingAssistanceId> {
    
    private ClientId clientId;
    private ProgramEnrollmentId enrollmentId;
    private RentalAssistanceType assistanceType;
    private AssistanceStatus status;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private Integer requestedDurationMonths;
    private Integer approvedDurationMonths;
    private String justification;
    private String requestedBy;
    
    // Approval workflow
    private ApprovalChain approvalChain;
    private String fundingSourceCode;
    
    // Unit and lease management
    private String assignedUnitId;
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;
    private BigDecimal monthlyRent;
    private String landlordId;
    
    // Payment tracking
    private List<Payment> payments = new ArrayList<>();
    private BigDecimal totalPaid;
    
    private Instant createdAt;
    private Instant lastModified;
    
    public static HousingAssistance request(ClientId clientId, ProgramEnrollmentId enrollmentId,
                                          RentalAssistanceType assistanceType, BigDecimal requestedAmount,
                                          Integer requestedDurationMonths, String justification, String requestedBy) {
        HousingAssistanceId id = HousingAssistanceId.generate();
        HousingAssistance assistance = new HousingAssistance();
        assistance.apply(new HousingAssistanceRequested(
            id.value(),
            clientId.value(),
            enrollmentId.value(),
            assistanceType,
            requestedAmount,
            requestedDurationMonths,
            justification,
            requestedBy,
            Instant.now()
        ));
        return assistance;
    }
    
    public void initiateApproval(String requiredApprovalLevel, Integer requiredApprovalCount) {
        if (status != AssistanceStatus.REQUESTED) {
            throw new IllegalStateException("Cannot initiate approval for assistance not in REQUESTED status");
        }
        this.approvalChain = new ApprovalChain(requiredApprovalLevel, requiredApprovalCount);
        this.status = AssistanceStatus.PENDING_APPROVAL;
    }
    
    public void addApproval(UUID approverId, String approverRole, String approverName, String notes) {
        if (approvalChain == null) {
            throw new IllegalStateException("Approval chain not initialized");
        }
        
        approvalChain.addApproval(approverId, approverRole, approverName, notes);
        
        if (approvalChain.isApproved()) {
            // Auto-approve with requested amounts if approval chain is complete
            approveAssistance(requestedAmount, requestedDurationMonths, null, approverId.toString(), notes);
        }
    }
    
    public void approveAssistance(BigDecimal approvedAmount, Integer approvedDurationMonths, 
                                String fundingSourceCode, String finalApproverId, String approvalNotes) {
        if (status != AssistanceStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot approve assistance not pending approval");
        }
        
        apply(new HousingAssistanceApproved(
            id.value(),
            approvedAmount,
            approvedDurationMonths,
            fundingSourceCode,
            finalApproverId,
            approvalNotes,
            Instant.now()
        ));
    }
    
    public void assignUnit(String unitId, String landlordId, BigDecimal monthlyRent, 
                          LocalDate leaseStartDate, LocalDate leaseEndDate) {
        if (status != AssistanceStatus.APPROVED) {
            throw new IllegalStateException("Cannot assign unit to non-approved assistance");
        }
        
        this.assignedUnitId = unitId;
        this.landlordId = landlordId;
        this.monthlyRent = monthlyRent;
        this.leaseStartDate = leaseStartDate;
        this.leaseEndDate = leaseEndDate;
        this.status = AssistanceStatus.UNIT_ASSIGNED;
    }
    
    public void authorizePayment(BigDecimal amount, LocalDate paymentDate, String paymentType,
                               String payeeId, String payeeName, String authorizedBy) {
        authorizePayment(amount, paymentDate, paymentType, AssistancePaymentSubtype.OTHER, 
                        null, null, payeeId, payeeName, authorizedBy);
    }
    
    public void authorizePayment(BigDecimal amount, LocalDate paymentDate, String paymentType,
                               AssistancePaymentSubtype subtype, LocalDate periodStart, LocalDate periodEnd,
                               String payeeId, String payeeName, String authorizedBy) {
        if (status != AssistanceStatus.UNIT_ASSIGNED && status != AssistanceStatus.ACTIVE) {
            throw new IllegalStateException("Cannot authorize payment for assistance not unit assigned or active");
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
            // Check arrears period is within approved duration
            if (approvedDurationMonths != null) {
                long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(periodStart, periodEnd);
                if (monthsBetween >= approvedDurationMonths) {
                    throw new IllegalArgumentException("Arrears period exceeds approved duration");
                }
            }
        }
        
        UUID paymentId = UUID.randomUUID();
        apply(new PaymentAuthorized(
            id.value(),
            paymentId,
            amount,
            paymentDate,
            paymentType,
            subtype,
            periodStart,
            periodEnd,
            payeeId,
            payeeName,
            fundingSourceCode,
            authorizedBy,
            Instant.now()
        ));
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof HousingAssistanceRequested e) {
            this.id = HousingAssistanceId.of(e.housingAssistanceId());
            this.clientId = new ClientId(e.clientId());
            this.enrollmentId = new ProgramEnrollmentId(e.enrollmentId());
            this.assistanceType = e.assistanceType();
            this.requestedAmount = e.requestedAmount();
            this.requestedDurationMonths = e.requestedDurationMonths();
            this.justification = e.justification();
            this.requestedBy = e.requestedBy();
            this.status = AssistanceStatus.REQUESTED;
            this.totalPaid = BigDecimal.ZERO;
            this.createdAt = e.occurredAt();
            this.lastModified = e.occurredAt();
            
        } else if (event instanceof HousingAssistanceApproved e) {
            this.approvedAmount = e.approvedAmount();
            this.approvedDurationMonths = e.approvedDurationMonths();
            this.fundingSourceCode = e.fundingSourceCode();
            this.status = AssistanceStatus.APPROVED;
            this.lastModified = e.occurredAt();
            
        } else if (event instanceof PaymentAuthorized e) {
            Payment payment = new Payment(
                e.paymentId(),
                e.amount(),
                e.paymentDate(),
                e.paymentType(),
                e.subtype() != null ? e.subtype() : AssistancePaymentSubtype.OTHER,
                e.periodStart(),
                e.periodEnd(),
                e.payeeId(),
                e.payeeName(),
                e.authorizedBy()
            );
            this.payments.add(payment);
            this.totalPaid = totalPaid.add(e.amount());
            
            if (status == AssistanceStatus.UNIT_ASSIGNED) {
                this.status = AssistanceStatus.ACTIVE;
            }
            this.lastModified = e.occurredAt();
            
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum AssistanceStatus {
        REQUESTED,
        PENDING_APPROVAL,
        APPROVED,
        UNIT_ASSIGNED,
        ACTIVE,
        COMPLETED,
        TERMINATED,
        DENIED
    }
    
    public enum AssistancePaymentSubtype {
        RENT_CURRENT,
        RENT_ARREARS,
        UTILITY_CURRENT,
        UTILITY_ARREARS,
        SECURITY_DEPOSIT,
        APPLICATION_FEE,
        MOVING_COSTS,
        OTHER
    }
    
    public static class Payment {
        private UUID paymentId;
        private BigDecimal amount;
        private LocalDate paymentDate;
        private String paymentType;
        private AssistancePaymentSubtype subtype;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String payeeId;
        private String payeeName;
        private String authorizedBy;
        private PaymentStatus status;
        
        public Payment(UUID paymentId, BigDecimal amount, LocalDate paymentDate, 
                      String paymentType, String payeeId, String payeeName, String authorizedBy) {
            this(paymentId, amount, paymentDate, paymentType, AssistancePaymentSubtype.OTHER, 
                 null, null, payeeId, payeeName, authorizedBy);
        }
        
        public Payment(UUID paymentId, BigDecimal amount, LocalDate paymentDate, 
                      String paymentType, AssistancePaymentSubtype subtype, 
                      LocalDate periodStart, LocalDate periodEnd,
                      String payeeId, String payeeName, String authorizedBy) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.paymentDate = paymentDate;
            this.paymentType = paymentType;
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
        public String getPaymentType() { return paymentType; }
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
    public RentalAssistanceType getAssistanceType() { return assistanceType; }
    public AssistanceStatus getStatus() { return status; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public Integer getRequestedDurationMonths() { return requestedDurationMonths; }
    public Integer getApprovedDurationMonths() { return approvedDurationMonths; }
    public String getJustification() { return justification; }
    public String getRequestedBy() { return requestedBy; }
    public ApprovalChain getApprovalChain() { return approvalChain; }
    public String getFundingSourceCode() { return fundingSourceCode; }
    public String getAssignedUnitId() { return assignedUnitId; }
    public LocalDate getLeaseStartDate() { return leaseStartDate; }
    public LocalDate getLeaseEndDate() { return leaseEndDate; }
    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public String getLandlordId() { return landlordId; }
    public List<Payment> getPayments() { return List.copyOf(payments); }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    
    public BigDecimal getRemainingBudget() {
        return approvedAmount != null ? approvedAmount.subtract(totalPaid) : BigDecimal.ZERO;
    }
    
    public boolean isActive() {
        return status == AssistanceStatus.ACTIVE;
    }
}