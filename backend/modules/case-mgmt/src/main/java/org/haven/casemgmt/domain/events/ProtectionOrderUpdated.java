package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProtectionOrderUpdated extends DomainEvent {
    private final UUID clientId;
    private final UUID protectionOrderId;
    private final LocalDate updateDate;
    private final String updateType;
    private final CodeableConcept updateReason;
    private final LocalDate newExpirationDate;
    private final String updatedConditions;
    private final String courtName;
    private final String updatedBy;
    private final UUID updatedByUserId;
    private final String updateNotes;

    public ProtectionOrderUpdated(UUID caseId, UUID clientId, UUID protectionOrderId, LocalDate updateDate, String updateType, CodeableConcept updateReason, LocalDate newExpirationDate, String updatedConditions, String courtName, String updatedBy, UUID updatedByUserId, String updateNotes, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (protectionOrderId == null) throw new IllegalArgumentException("Protection order ID cannot be null");
        if (updateDate == null) throw new IllegalArgumentException("Update date cannot be null");
        if (updateType == null || updateType.trim().isEmpty()) throw new IllegalArgumentException("Update type cannot be null or empty");
        if (updatedBy == null || updatedBy.trim().isEmpty()) throw new IllegalArgumentException("Updated by cannot be null or empty");

        this.clientId = clientId;
        this.protectionOrderId = protectionOrderId;
        this.updateDate = updateDate;
        this.updateType = updateType;
        this.updateReason = updateReason;
        this.newExpirationDate = newExpirationDate;
        this.updatedConditions = updatedConditions;
        this.courtName = courtName;
        this.updatedBy = updatedBy;
        this.updatedByUserId = updatedByUserId;
        this.updateNotes = updateNotes;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID protectionOrderId() {
        return protectionOrderId;
    }

    public LocalDate updateDate() {
        return updateDate;
    }

    public String updateType() {
        return updateType;
    }

    public CodeableConcept updateReason() {
        return updateReason;
    }

    public LocalDate newExpirationDate() {
        return newExpirationDate;
    }

    public String updatedConditions() {
        return updatedConditions;
    }

    public String courtName() {
        return courtName;
    }

    public String updatedBy() {
        return updatedBy;
    }

    public UUID updatedByUserId() {
        return updatedByUserId;
    }

    public String updateNotes() {
        return updateNotes;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getProtectionOrderId() { return protectionOrderId; }
    public LocalDate getUpdateDate() { return updateDate; }
    public String getUpdateType() { return updateType; }
    public CodeableConcept getUpdateReason() { return updateReason; }
    public LocalDate getNewExpirationDate() { return newExpirationDate; }
    public String getUpdatedConditions() { return updatedConditions; }
    public String getCourtName() { return courtName; }
    public String getUpdatedBy() { return updatedBy; }
    public UUID getUpdatedByUserId() { return updatedByUserId; }
    public String getUpdateNotes() { return updateNotes; }
}