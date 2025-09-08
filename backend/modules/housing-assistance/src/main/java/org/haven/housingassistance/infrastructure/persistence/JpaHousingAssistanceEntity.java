package org.haven.housingassistance.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.housingassistance.domain.HousingAssistance;
import org.haven.housingassistance.domain.HousingAssistanceId;
import org.haven.housingassistance.domain.RentalAssistanceType;
import org.haven.programenrollment.domain.ProgramEnrollmentId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "housing_assistance", schema = "haven")
public class JpaHousingAssistanceEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "assistance_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RentalAssistanceType assistanceType;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private HousingAssistance.AssistanceStatus status;
    
    @Column(name = "requested_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedAmount;
    
    @Column(name = "approved_amount", precision = 10, scale = 2)
    private BigDecimal approvedAmount;
    
    @Column(name = "requested_duration_months")
    private Integer requestedDurationMonths;
    
    @Column(name = "approved_duration_months")
    private Integer approvedDurationMonths;
    
    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;
    
    @Column(name = "requested_by")
    private UUID requestedBy;
    
    @Column(name = "approval_level", length = 100)
    private String approvalLevel;
    
    @Column(name = "required_approval_count")
    private Integer requiredApprovalCount;
    
    @Column(name = "funding_source_code", length = 50)
    private String fundingSourceCode;
    
    @Column(name = "assigned_unit_id")
    private UUID assignedUnitId;
    
    @Column(name = "lease_start_date")
    private LocalDate leaseStartDate;
    
    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;
    
    @Column(name = "monthly_rent", precision = 10, scale = 2)
    private BigDecimal monthlyRent;
    
    @Column(name = "landlord_id")
    private UUID landlordId;
    
    @Column(name = "total_paid", precision = 10, scale = 2)
    private BigDecimal totalPaid;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "version")
    private Integer version;
    
    // Default constructor
    protected JpaHousingAssistanceEntity() {}
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }
    
    public RentalAssistanceType getAssistanceType() { return assistanceType; }
    public void setAssistanceType(RentalAssistanceType assistanceType) { this.assistanceType = assistanceType; }
    
    public HousingAssistance.AssistanceStatus getStatus() { return status; }
    public void setStatus(HousingAssistance.AssistanceStatus status) { this.status = status; }
    
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    
    public Integer getRequestedDurationMonths() { return requestedDurationMonths; }
    public void setRequestedDurationMonths(Integer requestedDurationMonths) { this.requestedDurationMonths = requestedDurationMonths; }
    
    public Integer getApprovedDurationMonths() { return approvedDurationMonths; }
    public void setApprovedDurationMonths(Integer approvedDurationMonths) { this.approvedDurationMonths = approvedDurationMonths; }
    
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    
    public UUID getRequestedBy() { return requestedBy; }
    public void setRequestedBy(UUID requestedBy) { this.requestedBy = requestedBy; }
    
    public String getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(String approvalLevel) { this.approvalLevel = approvalLevel; }
    
    public Integer getRequiredApprovalCount() { return requiredApprovalCount; }
    public void setRequiredApprovalCount(Integer requiredApprovalCount) { this.requiredApprovalCount = requiredApprovalCount; }
    
    public String getFundingSourceCode() { return fundingSourceCode; }
    public void setFundingSourceCode(String fundingSourceCode) { this.fundingSourceCode = fundingSourceCode; }
    
    public UUID getAssignedUnitId() { return assignedUnitId; }
    public void setAssignedUnitId(UUID assignedUnitId) { this.assignedUnitId = assignedUnitId; }
    
    public LocalDate getLeaseStartDate() { return leaseStartDate; }
    public void setLeaseStartDate(LocalDate leaseStartDate) { this.leaseStartDate = leaseStartDate; }
    
    public LocalDate getLeaseEndDate() { return leaseEndDate; }
    public void setLeaseEndDate(LocalDate leaseEndDate) { this.leaseEndDate = leaseEndDate; }
    
    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(BigDecimal monthlyRent) { this.monthlyRent = monthlyRent; }
    
    public UUID getLandlordId() { return landlordId; }
    public void setLandlordId(UUID landlordId) { this.landlordId = landlordId; }
    
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    // Conversion methods
    public static JpaHousingAssistanceEntity fromDomain(HousingAssistance domain) {
        JpaHousingAssistanceEntity entity = new JpaHousingAssistanceEntity();
        entity.setId(domain.getId().getValue());
        entity.setClientId(domain.getClientId().getValue());
        entity.setEnrollmentId(domain.getEnrollmentId().getValue());
        entity.setAssistanceType(domain.getAssistanceType());
        entity.setStatus(domain.getStatus());
        entity.setRequestedAmount(domain.getRequestedAmount());
        entity.setApprovedAmount(domain.getApprovedAmount());
        entity.setRequestedDurationMonths(domain.getRequestedDurationMonths());
        entity.setApprovedDurationMonths(domain.getApprovedDurationMonths());
        entity.setJustification(domain.getJustification());
        entity.setRequestedBy(domain.getRequestedBy() != null ? UUID.fromString(domain.getRequestedBy()) : null);
        entity.setFundingSourceCode(domain.getFundingSourceCode());
        entity.setAssignedUnitId(domain.getAssignedUnitId() != null ? UUID.fromString(domain.getAssignedUnitId()) : null);
        entity.setLeaseStartDate(domain.getLeaseStartDate());
        entity.setLeaseEndDate(domain.getLeaseEndDate());
        entity.setMonthlyRent(domain.getMonthlyRent());
        entity.setLandlordId(domain.getLandlordId() != null ? UUID.fromString(domain.getLandlordId()) : null);
        entity.setTotalPaid(domain.getTotalPaid());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getLastModified());
        return entity;
    }
    
    public HousingAssistance toDomain() {
        // This would require access to the HousingAssistance constructor or factory method
        // For now, returning null - this would need to be implemented based on the domain model
        return null;
    }
}