package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

@Entity
@Table(name = "ce_assessments")
public class JpaCeAssessmentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false, length = 50)
    private CeAssessmentType assessmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_level", length = 50)
    private CeAssessmentLevel assessmentLevel;

    @Column(name = "tool_used", length = 100)
    private String toolUsed;

    @Column(name = "score", precision = 10, scale = 2)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioritization_status", length = 50)
    private CePrioritizationStatus prioritizationStatus;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "packet_id")
    private UUID packetId;

    @Column(name = "consent_ledger_id")
    private UUID consentLedgerId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "consent_scope", columnDefinition = "ce_share_scope[]")
    private String[] consentScope;

    protected JpaCeAssessmentEntity() {
        // JPA
    }

    public JpaCeAssessmentEntity(CeAssessment assessment) {
        this.id = assessment.getRecordId();
        this.enrollmentId = assessment.getEnrollmentId().value();
        this.clientId = assessment.getClientId().value();
        this.assessmentDate = assessment.getAssessmentDate();
        this.assessmentType = assessment.getAssessmentType();
        this.assessmentLevel = assessment.getAssessmentLevel();
        this.toolUsed = assessment.getToolUsed();
        this.score = assessment.getScore();
        this.prioritizationStatus = assessment.getPrioritizationStatus();
        this.location = assessment.getLocation();
        this.createdBy = assessment.getCreatedBy();
        this.createdAt = assessment.getCreatedAt();
        this.updatedAt = assessment.getUpdatedAt();
        this.packetId = assessment.getPacketId() != null ? assessment.getPacketId().value() : null;
        this.consentLedgerId = assessment.getConsentLedgerId();
        this.consentScope = assessment.getConsentScope().stream()
            .map(Enum::name)
            .toArray(String[]::new);
    }

    public CeAssessment toDomain() {
        EnumSet<CeShareScope> scopes = consentScope == null || consentScope.length == 0
            ? EnumSet.noneOf(CeShareScope.class)
            : Arrays.stream(consentScope)
                .map(CeShareScope::valueOf)
                .collect(() -> EnumSet.noneOf(CeShareScope.class), EnumSet::add, EnumSet::addAll);

        return CeAssessment.reconstruct(
            id,
            ProgramEnrollmentId.of(enrollmentId),
            new ClientId(clientId),
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
            packetId != null ? CePacketId.of(packetId) : null,
            consentLedgerId,
            scopes
        );
    }
}
