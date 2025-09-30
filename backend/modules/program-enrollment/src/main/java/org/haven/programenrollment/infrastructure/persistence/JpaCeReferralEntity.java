package org.haven.programenrollment.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA entity mapping the ce_referrals table.
 */
@Entity
@Table(name = "ce_referrals")
public class JpaCeReferralEntity {

    @Id
    @Column(name = "referral_id")
    private UUID referralId;

    @Column(name = "enrollment_id")
    private UUID enrollmentId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "referral_date", nullable = false)
    private LocalDateTime referralDate;

    @Column(name = "referred_project_id", nullable = false)
    private UUID referredProjectId;

    @Column(name = "referred_project_name", nullable = false)
    private String referredProjectName;

    @Column(name = "referred_organization", nullable = false)
    private String referredOrganization;

    @Enumerated(EnumType.STRING)
    @Column(name = "referral_type", nullable = false)
    private CeEventType referralType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CeReferralStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    private CeReferralResult result;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "priority_level")
    private Integer priorityLevel;

    @Column(name = "vulnerability_score")
    private Double vulnerabilityScore;

    @Column(name = "case_manager_name")
    private String caseManagerName;

    @Column(name = "case_manager_contact")
    private String caseManagerContact;

    @Column(name = "vawa_protection", nullable = false)
    private boolean vawaProtection;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "packet_id")
    private UUID packetId;

    @Column(name = "consent_ledger_id")
    private UUID consentLedgerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consent_scope", columnDefinition = "jsonb")
    private Set<String> consentScope;

    @Column(name = "accepted_date")
    private LocalDate acceptedDate;

    @Column(name = "housing_move_in_date")
    private LocalDate housingMoveInDate;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "rejection_notes")
    private String rejectionNotes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    protected JpaCeReferralEntity() {
        // JPA constructor
    }

    public JpaCeReferralEntity(CeReferral domain) {
        this.referralId = domain.getReferralId();
        this.enrollmentId = domain.getEnrollmentId() != null ? domain.getEnrollmentId().value() : null;
        this.clientId = domain.getClientId().value();
        this.referralDate = domain.getReferralDate();
        this.referredProjectId = domain.getReferredProjectId();
        this.referredProjectName = domain.getReferredProjectName();
        this.referredOrganization = domain.getReferredOrganization();
        this.referralType = domain.getReferralType();
        this.status = domain.getStatus();
        this.result = domain.getResult();
        this.expirationDate = domain.getExpirationDate();
        this.priorityLevel = domain.getPriorityLevel();
        this.vulnerabilityScore = domain.getVulnerabilityScore();
        this.caseManagerName = domain.getCaseManagerName();
        this.caseManagerContact = domain.getCaseManagerContact();
        this.vawaProtection = domain.isVawaProtection();
        this.createdBy = domain.getCreatedBy();
        this.createdAt = domain.getCreatedAt();
        this.updatedAt = domain.getUpdatedAt();
        this.packetId = domain.getPacketId() != null ? domain.getPacketId().value() : null;
        this.consentLedgerId = domain.getConsentLedgerId();
        this.consentScope = domain.getConsentScope().stream()
            .map(Enum::name)
            .collect(Collectors.toSet());
        this.acceptedDate = domain.getAcceptedDate();
        this.housingMoveInDate = domain.getHousingMoveInDate();
        this.rejectionReason = domain.getRejectionReason();
        this.rejectionNotes = domain.getRejectionNotes();
        this.cancellationReason = domain.getCancellationReason();
    }

    public CeReferral toDomain() {
        return CeReferral.builder()
            .referralId(referralId)
            .enrollmentId(enrollmentId != null ? ProgramEnrollmentId.of(enrollmentId) : null)
            .clientId(ClientId.of(clientId))
            .referralDate(referralDate)
            .referredProjectId(referredProjectId)
            .referredProjectName(referredProjectName)
            .referredOrganization(referredOrganization)
            .referralType(referralType)
            .status(status)
            .result(result)
            .expirationDate(expirationDate)
            .priorityLevel(priorityLevel)
            .vulnerabilityScore(vulnerabilityScore)
            .caseManagerName(caseManagerName)
            .caseManagerContact(caseManagerContact)
            .vawaProtection(vawaProtection)
            .createdBy(createdBy)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .packetId(packetId != null ? CePacketId.of(packetId) : null)
            .consentLedgerId(consentLedgerId)
            .consentScope(consentScope != null ?
                consentScope.stream()
                    .map(CeShareScope::valueOf)
                    .collect(Collectors.toSet()) :
                EnumSet.noneOf(CeShareScope.class))
            .acceptedDate(acceptedDate)
            .housingMoveInDate(housingMoveInDate)
            .rejectionReason(rejectionReason)
            .rejectionNotes(rejectionNotes)
            .cancellationReason(cancellationReason)
            .build();
    }

    // Getters and setters
    public UUID getReferralId() { return referralId; }
    public void setReferralId(UUID referralId) { this.referralId = referralId; }

    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public LocalDateTime getReferralDate() { return referralDate; }
    public void setReferralDate(LocalDateTime referralDate) { this.referralDate = referralDate; }

    public UUID getReferredProjectId() { return referredProjectId; }
    public void setReferredProjectId(UUID referredProjectId) { this.referredProjectId = referredProjectId; }

    public String getReferredProjectName() { return referredProjectName; }
    public void setReferredProjectName(String referredProjectName) { this.referredProjectName = referredProjectName; }

    public String getReferredOrganization() { return referredOrganization; }
    public void setReferredOrganization(String referredOrganization) { this.referredOrganization = referredOrganization; }

    public CeEventType getReferralType() { return referralType; }
    public void setReferralType(CeEventType referralType) { this.referralType = referralType; }

    public CeReferralStatus getStatus() { return status; }
    public void setStatus(CeReferralStatus status) { this.status = status; }

    public CeReferralResult getResult() { return result; }
    public void setResult(CeReferralResult result) { this.result = result; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Integer getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }

    public Double getVulnerabilityScore() { return vulnerabilityScore; }
    public void setVulnerabilityScore(Double vulnerabilityScore) { this.vulnerabilityScore = vulnerabilityScore; }

    public String getCaseManagerName() { return caseManagerName; }
    public void setCaseManagerName(String caseManagerName) { this.caseManagerName = caseManagerName; }

    public String getCaseManagerContact() { return caseManagerContact; }
    public void setCaseManagerContact(String caseManagerContact) { this.caseManagerContact = caseManagerContact; }

    public boolean isVawaProtection() { return vawaProtection; }
    public void setVawaProtection(boolean vawaProtection) { this.vawaProtection = vawaProtection; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getPacketId() { return packetId; }
    public void setPacketId(UUID packetId) { this.packetId = packetId; }

    public UUID getConsentLedgerId() { return consentLedgerId; }
    public void setConsentLedgerId(UUID consentLedgerId) { this.consentLedgerId = consentLedgerId; }

    public Set<String> getConsentScope() { return consentScope; }
    public void setConsentScope(Set<String> consentScope) { this.consentScope = consentScope; }

    public LocalDate getAcceptedDate() { return acceptedDate; }
    public void setAcceptedDate(LocalDate acceptedDate) { this.acceptedDate = acceptedDate; }

    public LocalDate getHousingMoveInDate() { return housingMoveInDate; }
    public void setHousingMoveInDate(LocalDate housingMoveInDate) { this.housingMoveInDate = housingMoveInDate; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRejectionNotes() { return rejectionNotes; }
    public void setRejectionNotes(String rejectionNotes) { this.rejectionNotes = rejectionNotes; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}