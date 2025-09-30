package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

@Entity
@Table(name = "ce_events")
public class JpaCeEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private CeEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 100)
    private CeEventResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private CeEventStatus status;

    @Column(name = "referral_destination", length = 200)
    private String referralDestination;

    @Column(name = "outcome_date")
    private LocalDate outcomeDate;

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

    protected JpaCeEventEntity() {
        // JPA
    }

    public JpaCeEventEntity(CeEvent event) {
        this.id = event.getRecordId();
        this.enrollmentId = event.getEnrollmentId().value();
        this.clientId = event.getClientId().value();
        this.eventDate = event.getEventDate();
        this.eventType = event.getEventType();
        this.result = event.getResult();
        this.status = event.getStatus();
        this.referralDestination = event.getReferralDestination();
        this.outcomeDate = event.getOutcomeDate();
        this.createdBy = event.getCreatedBy();
        this.createdAt = event.getCreatedAt();
        this.updatedAt = event.getUpdatedAt();
        this.packetId = event.getPacketId() != null ? event.getPacketId().value() : null;
        this.consentLedgerId = event.getConsentLedgerId();
        this.consentScope = event.getConsentScope().stream()
            .map(Enum::name)
            .toArray(String[]::new);
    }

    public CeEvent toDomain() {
        EnumSet<CeShareScope> scopes = consentScope == null || consentScope.length == 0
            ? EnumSet.noneOf(CeShareScope.class)
            : Arrays.stream(consentScope)
                .map(CeShareScope::valueOf)
                .collect(() -> EnumSet.noneOf(CeShareScope.class), EnumSet::add, EnumSet::addAll);

        return CeEvent.reconstruct(
            id,
            ProgramEnrollmentId.of(enrollmentId),
            new ClientId(clientId),
            eventDate,
            eventType,
            result,
            status,
            referralDestination,
            outcomeDate,
            createdBy,
            createdAt,
            updatedAt,
            packetId != null ? CePacketId.of(packetId) : null,
            consentLedgerId,
            scopes
        );
    }
}
