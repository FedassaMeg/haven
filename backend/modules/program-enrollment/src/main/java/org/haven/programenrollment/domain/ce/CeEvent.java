package org.haven.programenrollment.domain.ce;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Coordinated Entry event or referral record captured under consent scopes.
 */
public class CeEvent {

    private final UUID recordId;
    private final ProgramEnrollmentId enrollmentId;
    private final ClientId clientId;
    private final LocalDate eventDate;
    private final CeEventType eventType;
    private final CeEventResult result;
    private final CeEventStatus status;
    private final String referralDestination;
    private final LocalDate outcomeDate;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final CePacketId packetId;
    private final UUID consentLedgerId;
    private final EnumSet<CeShareScope> consentScope;

    private CeEvent(UUID recordId,
                    ProgramEnrollmentId enrollmentId,
                    ClientId clientId,
                    LocalDate eventDate,
                    CeEventType eventType,
                    CeEventResult result,
                    CeEventStatus status,
                    String referralDestination,
                    LocalDate outcomeDate,
                    String createdBy,
                    Instant createdAt,
                    Instant updatedAt,
                    CePacketId packetId,
                    UUID consentLedgerId,
                    EnumSet<CeShareScope> consentScope) {
        this.recordId = recordId;
        this.enrollmentId = enrollmentId;
        this.clientId = clientId;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.result = result;
        this.status = status;
        this.referralDestination = referralDestination;
        this.outcomeDate = outcomeDate;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.packetId = packetId;
        this.consentLedgerId = consentLedgerId;
        this.consentScope = consentScope == null
            ? EnumSet.noneOf(CeShareScope.class)
            : EnumSet.copyOf(consentScope);
    }

    public static CeEvent create(ProgramEnrollmentId enrollmentId,
                                 ClientId clientId,
                                 LocalDate eventDate,
                                 CeEventType eventType,
                                 CeEventResult result,
                                 CeEventStatus status,
                                 String referralDestination,
                                 LocalDate outcomeDate,
                                 String createdBy,
                                 CePacketId packetId,
                                 UUID consentLedgerId,
                                 Set<CeShareScope> consentScope) {
        Objects.requireNonNull(enrollmentId, "enrollmentId is required");
        Objects.requireNonNull(clientId, "clientId is required");
        Objects.requireNonNull(eventDate, "eventDate is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdBy, "createdBy is required");
        Objects.requireNonNull(packetId, "packetId is required");

        if (eventDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Event date cannot be in the future");
        }
        if (outcomeDate != null && outcomeDate.isBefore(eventDate)) {
            throw new IllegalArgumentException("Outcome date cannot precede event date");
        }

        EnumSet<CeShareScope> scope = consentScope == null || consentScope.isEmpty()
            ? EnumSet.of(CeShareScope.COC_COORDINATED_ENTRY)
            : EnumSet.copyOf(consentScope);

        Instant now = Instant.now();
        return new CeEvent(
            UUID.randomUUID(),
            enrollmentId,
            clientId,
            eventDate,
            eventType,
            result,
            status,
            referralDestination,
            outcomeDate,
            createdBy,
            now,
            now,
            packetId,
            consentLedgerId,
            scope
        );
    }

    public static CeEvent reconstruct(UUID recordId,
                                      ProgramEnrollmentId enrollmentId,
                                      ClientId clientId,
                                      LocalDate eventDate,
                                      CeEventType eventType,
                                      CeEventResult result,
                                      CeEventStatus status,
                                      String referralDestination,
                                      LocalDate outcomeDate,
                                      String createdBy,
                                      Instant createdAt,
                                      Instant updatedAt,
                                      CePacketId packetId,
                                      UUID consentLedgerId,
                                      EnumSet<CeShareScope> consentScope) {
        return new CeEvent(
            recordId,
            enrollmentId,
            clientId,
            eventDate,
            eventType,
            result,
            status,
            referralDestination,
            outcomeDate,
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

    public LocalDate getEventDate() {
        return eventDate;
    }

    public CeEventType getEventType() {
        return eventType;
    }

    public CeEventResult getResult() {
        return result;
    }

    public CeEventStatus getStatus() {
        return status;
    }

    public String getReferralDestination() {
        return referralDestination;
    }

    public LocalDate getOutcomeDate() {
        return outcomeDate;
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
