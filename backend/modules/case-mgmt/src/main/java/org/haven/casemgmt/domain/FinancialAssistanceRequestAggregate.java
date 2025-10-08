package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.casemgmt.domain.events.*;
import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Financial Assistance Request aggregate for case management
 * Tracks requests for emergency financial assistance within cases
 */
public class FinancialAssistanceRequestAggregate extends AggregateRoot<FinancialAssistanceRequestId> {
    
    private ClientId clientId;
    private CaseId caseId;
    private ProgramEnrollmentId enrollmentId;
    private CodeableConcept assistanceType;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private BigDecimal paidAmount;
    private String purpose;
    private String justification;
    private String requestedBy;
    private boolean isEmergency;
    private RequestStatus status;
    private String vendorName;
    private String paymentMethod;
    private LocalDate requestDate;
    private LocalDate approvalDate;
    private LocalDate paymentDate;
    private String approvedBy;
    private String denialReason;
    private String paymentReference;
    private Instant createdAt;
    private Instant lastModified;
    
    public static FinancialAssistanceRequestAggregate submit(ClientId clientId, CaseId caseId,
                                                           ProgramEnrollmentId enrollmentId,
                                                           CodeableConcept assistanceType,
                                                           BigDecimal requestedAmount, String purpose,
                                                           String justification, String requestedBy,
                                                           boolean isEmergency) {
        FinancialAssistanceRequestId requestId = FinancialAssistanceRequestId.generate();
        FinancialAssistanceRequestAggregate request = new FinancialAssistanceRequestAggregate();
        request.apply(new RequestSubmitted(
            requestId.value(),
            clientId.value(),
            caseId.value(),
            enrollmentId.value(),
            assistanceType,
            requestedAmount,
            purpose,
            justification,
            requestedBy,
            isEmergency,
            Instant.now()
        ));
        return request;
    }
    
    public void approve(BigDecimal approvedAmount, String approvedBy, String approvalNotes) {
        if (status != RequestStatus.SUBMITTED) {
            throw new IllegalStateException("Can only approve submitted requests");
        }
        
        apply(new RequestApproved(
            id.value(),
            clientId.value(),
            requestedAmount,
            approvedAmount,
            approvedBy,
            approvalNotes,
            Instant.now()
        ));
    }
    
    public void deny(String denialReason, String deniedBy) {
        if (status != RequestStatus.SUBMITTED) {
            throw new IllegalStateException("Can only deny submitted requests");
        }
        
        apply(new RequestDenied(
            id.value(),
            clientId.value(),
            requestedAmount,
            denialReason,
            deniedBy,
            Instant.now()
        ));
    }
    
    public void recordPayment(BigDecimal paidAmount, String paymentMethod, String vendorName,
                            String paymentReference, String paidBy) {
        if (status != RequestStatus.APPROVED) {
            throw new IllegalStateException("Can only record payment for approved requests");
        }
        
        apply(new RequestPaid(
            id.value(),
            clientId.value(),
            approvedAmount,
            paidAmount,
            paymentMethod,
            vendorName,
            paymentReference,
            paidBy,
            Instant.now()
        ));
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof RequestSubmitted e) {
            this.id = new FinancialAssistanceRequestId(e.requestId());
            this.clientId = new ClientId(e.clientId());
            this.caseId = new CaseId(e.caseId());
            this.enrollmentId = new ProgramEnrollmentId(e.enrollmentId());
            this.assistanceType = e.assistanceType();
            this.requestedAmount = e.requestedAmount();
            this.purpose = e.purpose();
            this.justification = e.justification();
            this.requestedBy = e.requestedBy();
            this.isEmergency = e.isEmergency();
            this.status = RequestStatus.SUBMITTED;
            this.requestDate = LocalDate.now();
            this.createdAt = e.occurredAt();
            this.lastModified = e.occurredAt();
        } else if (event instanceof RequestApproved e) {
            this.approvedAmount = e.approvedAmount();
            this.approvedBy = e.approvedBy();
            this.status = RequestStatus.APPROVED;
            this.approvalDate = LocalDate.now();
            this.lastModified = e.occurredAt();
        } else if (event instanceof RequestDenied e) {
            this.denialReason = e.denialReason();
            this.status = RequestStatus.DENIED;
            this.lastModified = e.occurredAt();
        } else if (event instanceof RequestPaid e) {
            this.paidAmount = e.paidAmount();
            this.paymentMethod = e.paymentMethod();
            this.vendorName = e.vendorName();
            this.paymentReference = e.paymentReference();
            this.status = RequestStatus.PAID;
            this.paymentDate = LocalDate.now();
            this.lastModified = e.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum RequestStatus {
        SUBMITTED, APPROVED, DENIED, PAID, CANCELLED
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public CaseId getCaseId() { return caseId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public CodeableConcept getAssistanceType() { return assistanceType; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public String getPurpose() { return purpose; }
    public String getJustification() { return justification; }
    public String getRequestedBy() { return requestedBy; }
    public boolean isEmergency() { return isEmergency; }
    public RequestStatus getStatus() { return status; }
    public String getVendorName() { return vendorName; }
    public String getPaymentMethod() { return paymentMethod; }
    public LocalDate getRequestDate() { return requestDate; }
    public LocalDate getApprovalDate() { return approvalDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getApprovedBy() { return approvedBy; }
    public String getDenialReason() { return denialReason; }
    public String getPaymentReference() { return paymentReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    
    public boolean isPending() {
        return status == RequestStatus.SUBMITTED;
    }
    
    public boolean isApproved() {
        return status == RequestStatus.APPROVED;
    }
    
    public boolean isCompleted() {
        return status == RequestStatus.PAID;
    }
    
    public BigDecimal getRemainingAmount() {
        if (approvedAmount == null || paidAmount == null) {
            return BigDecimal.ZERO;
        }
        return approvedAmount.subtract(paidAmount);
    }
}