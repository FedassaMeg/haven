package org.haven.programenrollment.domain.ce;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Domain model representing a Coordinated Entry referral with consent controls.
 */
public class CeReferral {

    private final UUID referralId;
    private final ProgramEnrollmentId enrollmentId;
    private final ClientId clientId;
    private final LocalDateTime referralDate;
    private final UUID referredProjectId;
    private final String referredProjectName;
    private final String referredOrganization;
    private final CeEventType referralType;
    private final CeReferralStatus status;
    private final CeReferralResult result;
    private final LocalDate expirationDate;
    private final Integer priorityLevel;
    private final Double vulnerabilityScore;
    private final String caseManagerName;
    private final String caseManagerContact;
    private final boolean vawaProtection;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final CePacketId packetId;
    private final UUID consentLedgerId;
    private final EnumSet<CeShareScope> consentScope;
    private final LocalDate acceptedDate;
    private final LocalDate housingMoveInDate;
    private final String rejectionReason;
    private final String rejectionNotes;
    private final String cancellationReason;

    private CeReferral(Builder builder) {
        this.referralId = builder.referralId;
        this.enrollmentId = builder.enrollmentId;
        this.clientId = builder.clientId;
        this.referralDate = builder.referralDate;
        this.referredProjectId = builder.referredProjectId;
        this.referredProjectName = builder.referredProjectName;
        this.referredOrganization = builder.referredOrganization;
        this.referralType = builder.referralType;
        this.status = builder.status;
        this.result = builder.result;
        this.expirationDate = builder.expirationDate;
        this.priorityLevel = builder.priorityLevel;
        this.vulnerabilityScore = builder.vulnerabilityScore;
        this.caseManagerName = builder.caseManagerName;
        this.caseManagerContact = builder.caseManagerContact;
        this.vawaProtection = builder.vawaProtection;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.packetId = builder.packetId;
        this.consentLedgerId = builder.consentLedgerId;
        this.consentScope = builder.consentScope == null
            ? EnumSet.noneOf(CeShareScope.class)
            : EnumSet.copyOf(builder.consentScope);
        this.acceptedDate = builder.acceptedDate;
        this.housingMoveInDate = builder.housingMoveInDate;
        this.rejectionReason = builder.rejectionReason;
        this.rejectionNotes = builder.rejectionNotes;
        this.cancellationReason = builder.cancellationReason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CeReferral updateResult(CeReferralResult result, LocalDateTime resultDate,
                                   String rejectionReason, String rejectionNotes,
                                   LocalDate acceptedDate, LocalDate housingMoveInDate) {
        Builder builder = this.toBuilder()
            .result(result)
            .updatedAt(Instant.now());

        if (result == CeReferralResult.SUCCESSFUL_CLIENT_ACCEPTED) {
            builder.status(CeReferralStatus.ACCEPTED)
                .acceptedDate(acceptedDate)
                .housingMoveInDate(housingMoveInDate);
        } else if (result == CeReferralResult.UNSUCCESSFUL_CLIENT_REJECTED ||
                   result == CeReferralResult.UNSUCCESSFUL_PROVIDER_REJECTED) {
            builder.status(CeReferralStatus.REJECTED)
                .rejectionReason(rejectionReason)
                .rejectionNotes(rejectionNotes);
        }

        return builder.build();
    }

    public CeReferral cancel(String cancellationReason) {
        return this.toBuilder()
            .status(CeReferralStatus.CANCELLED)
            .cancellationReason(cancellationReason)
            .updatedAt(Instant.now())
            .build();
    }

    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    public boolean isActive() {
        return status == CeReferralStatus.PENDING && !isExpired();
    }

    public boolean allowsShareScope(CeShareScope scope) {
        return consentScope.contains(scope);
    }

    private Builder toBuilder() {
        return new Builder(this);
    }

    // Getters
    public UUID getReferralId() { return referralId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDateTime getReferralDate() { return referralDate; }
    public UUID getReferredProjectId() { return referredProjectId; }
    public String getReferredProjectName() { return referredProjectName; }
    public String getReferredOrganization() { return referredOrganization; }
    public CeEventType getReferralType() { return referralType; }
    public CeReferralStatus getStatus() { return status; }
    public CeReferralResult getResult() { return result; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public Integer getPriorityLevel() { return priorityLevel; }
    public Double getVulnerabilityScore() { return vulnerabilityScore; }
    public String getCaseManagerName() { return caseManagerName; }
    public String getCaseManagerContact() { return caseManagerContact; }
    public boolean isVawaProtection() { return vawaProtection; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public CePacketId getPacketId() { return packetId; }
    public UUID getConsentLedgerId() { return consentLedgerId; }
    public EnumSet<CeShareScope> getConsentScope() { return EnumSet.copyOf(consentScope); }
    public LocalDate getAcceptedDate() { return acceptedDate; }
    public LocalDate getHousingMoveInDate() { return housingMoveInDate; }
    public String getRejectionReason() { return rejectionReason; }
    public String getRejectionNotes() { return rejectionNotes; }
    public String getCancellationReason() { return cancellationReason; }

    public static class Builder {
        private UUID referralId = UUID.randomUUID();
        private ProgramEnrollmentId enrollmentId;
        private ClientId clientId;
        private LocalDateTime referralDate;
        private UUID referredProjectId;
        private String referredProjectName;
        private String referredOrganization;
        private CeEventType referralType;
        private CeReferralStatus status = CeReferralStatus.PENDING;
        private CeReferralResult result;
        private LocalDate expirationDate;
        private Integer priorityLevel;
        private Double vulnerabilityScore;
        private String caseManagerName;
        private String caseManagerContact;
        private boolean vawaProtection;
        private String createdBy;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private CePacketId packetId;
        private UUID consentLedgerId;
        private EnumSet<CeShareScope> consentScope = EnumSet.of(CeShareScope.COC_COORDINATED_ENTRY);
        private LocalDate acceptedDate;
        private LocalDate housingMoveInDate;
        private String rejectionReason;
        private String rejectionNotes;
        private String cancellationReason;

        private Builder() {}

        private Builder(CeReferral existing) {
            this.referralId = existing.referralId;
            this.enrollmentId = existing.enrollmentId;
            this.clientId = existing.clientId;
            this.referralDate = existing.referralDate;
            this.referredProjectId = existing.referredProjectId;
            this.referredProjectName = existing.referredProjectName;
            this.referredOrganization = existing.referredOrganization;
            this.referralType = existing.referralType;
            this.status = existing.status;
            this.result = existing.result;
            this.expirationDate = existing.expirationDate;
            this.priorityLevel = existing.priorityLevel;
            this.vulnerabilityScore = existing.vulnerabilityScore;
            this.caseManagerName = existing.caseManagerName;
            this.caseManagerContact = existing.caseManagerContact;
            this.vawaProtection = existing.vawaProtection;
            this.createdBy = existing.createdBy;
            this.createdAt = existing.createdAt;
            this.updatedAt = existing.updatedAt;
            this.packetId = existing.packetId;
            this.consentLedgerId = existing.consentLedgerId;
            this.consentScope = existing.consentScope;
            this.acceptedDate = existing.acceptedDate;
            this.housingMoveInDate = existing.housingMoveInDate;
            this.rejectionReason = existing.rejectionReason;
            this.rejectionNotes = existing.rejectionNotes;
            this.cancellationReason = existing.cancellationReason;
        }

        public Builder referralId(UUID referralId) {
            this.referralId = referralId;
            return this;
        }

        public Builder enrollmentId(ProgramEnrollmentId enrollmentId) {
            this.enrollmentId = enrollmentId;
            return this;
        }

        public Builder clientId(ClientId clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder referralDate(LocalDateTime referralDate) {
            this.referralDate = referralDate;
            return this;
        }

        public Builder referredProjectId(UUID referredProjectId) {
            this.referredProjectId = referredProjectId;
            return this;
        }

        public Builder referredProjectName(String referredProjectName) {
            this.referredProjectName = referredProjectName;
            return this;
        }

        public Builder referredOrganization(String referredOrganization) {
            this.referredOrganization = referredOrganization;
            return this;
        }

        public Builder referralType(CeEventType referralType) {
            this.referralType = referralType;
            return this;
        }

        public Builder status(CeReferralStatus status) {
            this.status = status;
            return this;
        }

        public Builder result(CeReferralResult result) {
            this.result = result;
            return this;
        }

        public Builder expirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder priorityLevel(Integer priorityLevel) {
            this.priorityLevel = priorityLevel;
            return this;
        }

        public Builder vulnerabilityScore(Double vulnerabilityScore) {
            this.vulnerabilityScore = vulnerabilityScore;
            return this;
        }

        public Builder caseManagerName(String caseManagerName) {
            this.caseManagerName = caseManagerName;
            return this;
        }

        public Builder caseManagerContact(String caseManagerContact) {
            this.caseManagerContact = caseManagerContact;
            return this;
        }

        public Builder vawaProtection(boolean vawaProtection) {
            this.vawaProtection = vawaProtection;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder packetId(CePacketId packetId) {
            this.packetId = packetId;
            return this;
        }

        public Builder consentLedgerId(UUID consentLedgerId) {
            this.consentLedgerId = consentLedgerId;
            return this;
        }

        public Builder consentScope(Set<CeShareScope> consentScope) {
            this.consentScope = consentScope == null || consentScope.isEmpty()
                ? EnumSet.noneOf(CeShareScope.class)
                : EnumSet.copyOf(consentScope);
            return this;
        }

        public Builder acceptedDate(LocalDate acceptedDate) {
            this.acceptedDate = acceptedDate;
            return this;
        }

        public Builder housingMoveInDate(LocalDate housingMoveInDate) {
            this.housingMoveInDate = housingMoveInDate;
            return this;
        }

        public Builder rejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }

        public Builder rejectionNotes(String rejectionNotes) {
            this.rejectionNotes = rejectionNotes;
            return this;
        }

        public Builder cancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
            return this;
        }

        public CeReferral build() {
            Objects.requireNonNull(enrollmentId, "enrollmentId is required");
            Objects.requireNonNull(clientId, "clientId is required");
            Objects.requireNonNull(referralDate, "referralDate is required");
            Objects.requireNonNull(referredProjectId, "referredProjectId is required");
            Objects.requireNonNull(referredProjectName, "referredProjectName is required");
            Objects.requireNonNull(referredOrganization, "referredOrganization is required");
            Objects.requireNonNull(referralType, "referralType is required");
            Objects.requireNonNull(status, "status is required");
            Objects.requireNonNull(createdBy, "createdBy is required");
            Objects.requireNonNull(packetId, "packetId is required");

            if (referralDate.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Referral date cannot be in the future");
            }

            return new CeReferral(this);
        }
    }
}