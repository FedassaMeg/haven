package org.haven.programenrollment.domain.ce;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Domain model representing a Coordinated Entry assessment payload captured under consent controls.
 */
public class CeAssessment {

    private final UUID recordId;
    private final ProgramEnrollmentId enrollmentId;
    private final ClientId clientId;
    private final LocalDate assessmentDate;
    private final CeAssessmentType assessmentType;
    private final CeAssessmentLevel assessmentLevel;
    private final String toolUsed;
    private final BigDecimal score;
    private final CePrioritizationStatus prioritizationStatus;
    private final String location;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final CePacketId packetId;
    private final UUID consentLedgerId;
    private final EnumSet<CeShareScope> consentScope;

    private CeAssessment(UUID recordId,
                         ProgramEnrollmentId enrollmentId,
                         ClientId clientId,
                         LocalDate assessmentDate,
                         CeAssessmentType assessmentType,
                         CeAssessmentLevel assessmentLevel,
                         String toolUsed,
                         BigDecimal score,
                         CePrioritizationStatus prioritizationStatus,
                         String location,
                         String createdBy,
                         Instant createdAt,
                         Instant updatedAt,
                         CePacketId packetId,
                         UUID consentLedgerId,
                         EnumSet<CeShareScope> consentScope) {
        this.recordId = recordId;
        this.enrollmentId = enrollmentId;
        this.clientId = clientId;
        this.assessmentDate = assessmentDate;
        this.assessmentType = assessmentType;
        this.assessmentLevel = assessmentLevel;
        this.toolUsed = toolUsed;
        this.score = score;
        this.prioritizationStatus = prioritizationStatus;
        this.location = location;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.packetId = packetId;
        this.consentLedgerId = consentLedgerId;
        this.consentScope = consentScope == null
            ? EnumSet.noneOf(CeShareScope.class)
            : EnumSet.copyOf(consentScope);
    }

    public static CeAssessment create(ProgramEnrollmentId enrollmentId,
                                      ClientId clientId,
                                      LocalDate assessmentDate,
                                      CeAssessmentType assessmentType,
                                      CeAssessmentLevel assessmentLevel,
                                      String toolUsed,
                                      BigDecimal score,
                                      CePrioritizationStatus prioritizationStatus,
                                      String location,
                                      String createdBy,
                                      CePacketId packetId,
                                      UUID consentLedgerId,
                                      Set<CeShareScope> consentScope) {
        Objects.requireNonNull(enrollmentId, "enrollmentId is required");
        Objects.requireNonNull(clientId, "clientId is required");
        Objects.requireNonNull(assessmentDate, "assessmentDate is required");
        Objects.requireNonNull(assessmentType, "assessmentType is required");
        Objects.requireNonNull(createdBy, "createdBy is required");
        Objects.requireNonNull(packetId, "packetId is required");

        if (assessmentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Assessment date cannot be in the future");
        }

        EnumSet<CeShareScope> scope = consentScope == null || consentScope.isEmpty()
            ? EnumSet.of(CeShareScope.COC_COORDINATED_ENTRY)
            : EnumSet.copyOf(consentScope);

        Instant now = Instant.now();
        return new CeAssessment(
            UUID.randomUUID(),
            enrollmentId,
            clientId,
            assessmentDate,
            assessmentType,
            assessmentLevel,
            toolUsed,
            score,
            prioritizationStatus,
            location,
            createdBy,
            now,
            now,
            packetId,
            consentLedgerId,
            scope
        );
    }

    public static CeAssessment reconstruct(UUID recordId,
                                           ProgramEnrollmentId enrollmentId,
                                           ClientId clientId,
                                            LocalDate assessmentDate,
                                           CeAssessmentType assessmentType,
                                           CeAssessmentLevel assessmentLevel,
                                           String toolUsed,
                                           BigDecimal score,
                                           CePrioritizationStatus prioritizationStatus,
                                           String location,
                                           String createdBy,
                                           Instant createdAt,
                                           Instant updatedAt,
                                           CePacketId packetId,
                                           UUID consentLedgerId,
                                           EnumSet<CeShareScope> consentScope) {
        return new CeAssessment(
            recordId,
            enrollmentId,
            clientId,
            assessmentDate,
            assessmentType,
            assessmentLevel,
            toolUsed,
            score,
            prioritizationStatus,
            location,
            createdBy,
            createdAt,
            updatedAt,
            packetId,
            consentLedgerId,
            consentScope
        );
    }

    public UUID getRecordId() {
        return recordId;
    }

    public ProgramEnrollmentId getEnrollmentId() {
        return enrollmentId;
    }

    public ClientId getClientId() {
        return clientId;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public CeAssessmentType getAssessmentType() {
        return assessmentType;
    }

    public CeAssessmentLevel getAssessmentLevel() {
        return assessmentLevel;
    }

    public String getToolUsed() {
        return toolUsed;
    }

    public BigDecimal getScore() {
        return score;
    }

    public CePrioritizationStatus getPrioritizationStatus() {
        return prioritizationStatus;
    }

    public String getLocation() {
        return location;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public CePacketId getPacketId() {
        return packetId;
    }

    public UUID getConsentLedgerId() {
        return consentLedgerId;
    }

    public EnumSet<CeShareScope> getConsentScope() {
        return consentScope.isEmpty()
            ? EnumSet.noneOf(CeShareScope.class)
            : EnumSet.copyOf(consentScope);
    }
}
