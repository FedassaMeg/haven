package org.haven.financialassistance.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implements two-person integrity for financial approvals
 */
public class ApprovalChain {
    private List<Approval> approvals = new ArrayList<>();
    private ApprovalStatus status;
    private String requiredApprovalLevel;
    private Integer requiredApprovalCount;
    private Instant createdAt;
    
    public ApprovalChain(String requiredApprovalLevel, Integer requiredApprovalCount) {
        this.requiredApprovalLevel = requiredApprovalLevel;
        this.requiredApprovalCount = requiredApprovalCount;
        this.status = ApprovalStatus.PENDING;
        this.createdAt = Instant.now();
    }
    
    public void addApproval(UUID approverId, String approverRole, String approverName, String notes) {
        // Check if this person has already approved
        boolean alreadyApproved = approvals.stream()
            .anyMatch(approval -> approval.getApproverId().equals(approverId));
            
        if (alreadyApproved) {
            throw new IllegalStateException("User has already provided approval");
        }
        
        Approval approval = new Approval(approverId, approverRole, approverName, notes);
        approvals.add(approval);
        
        updateStatus();
    }
    
    public void reject(UUID approverId, String approverRole, String approverName, String reason) {
        Approval rejection = new Approval(approverId, approverRole, approverName, reason);
        rejection.setApprovalType(ApprovalType.REJECTED);
        approvals.add(rejection);
        this.status = ApprovalStatus.REJECTED;
    }
    
    private void updateStatus() {
        long approvedCount = approvals.stream()
            .filter(approval -> approval.getApprovalType() == ApprovalType.APPROVED)
            .count();
            
        if (approvedCount >= requiredApprovalCount) {
            this.status = ApprovalStatus.APPROVED;
        } else {
            this.status = ApprovalStatus.PENDING;
        }
    }
    
    public boolean isApproved() {
        return status == ApprovalStatus.APPROVED;
    }
    
    public boolean isRejected() {
        return status == ApprovalStatus.REJECTED;
    }
    
    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
    
    public enum ApprovalType {
        APPROVED, REJECTED
    }
    
    public static class Approval {
        private UUID approverId;
        private String approverRole;
        private String approverName;
        private ApprovalType approvalType;
        private String notes;
        private Instant approvedAt;
        
        public Approval(UUID approverId, String approverRole, String approverName, String notes) {
            this.approverId = approverId;
            this.approverRole = approverRole;
            this.approverName = approverName;
            this.notes = notes;
            this.approvalType = ApprovalType.APPROVED;
            this.approvedAt = Instant.now();
        }
        
        // Getters and setters
        public UUID getApproverId() { return approverId; }
        public String getApproverRole() { return approverRole; }
        public String getApproverName() { return approverName; }
        public ApprovalType getApprovalType() { return approvalType; }
        public void setApprovalType(ApprovalType approvalType) { this.approvalType = approvalType; }
        public String getNotes() { return notes; }
        public Instant getApprovedAt() { return approvedAt; }
    }
    
    // Getters
    public List<Approval> getApprovals() { return List.copyOf(approvals); }
    public ApprovalStatus getStatus() { return status; }
    public String getRequiredApprovalLevel() { return requiredApprovalLevel; }
    public Integer getRequiredApprovalCount() { return requiredApprovalCount; }
    public Instant getCreatedAt() { return createdAt; }
}