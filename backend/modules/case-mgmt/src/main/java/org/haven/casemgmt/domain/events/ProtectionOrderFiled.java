package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProtectionOrderFiled extends DomainEvent {
    private final UUID clientId;
    private final UUID protectionOrderId;
    private final CodeableConcept orderType;
    private final LocalDate filedDate;
    private final LocalDate effectiveDate;
    private final LocalDate expirationDate;
    private final String courtName;
    private final String judgeOrCommissioner;
    private final String caseNumber;
    private final String protectedParties;
    private final String restrainedParties;
    private final String orderConditions;
    private final boolean isTemporary;
    private final String filedBy;
    private final UUID filedByUserId;

    public ProtectionOrderFiled(UUID caseId, UUID clientId, UUID protectionOrderId, CodeableConcept orderType, LocalDate filedDate, LocalDate effectiveDate, LocalDate expirationDate, String courtName, String judgeOrCommissioner, String caseNumber, String protectedParties, String restrainedParties, String orderConditions, boolean isTemporary, String filedBy, UUID filedByUserId, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (protectionOrderId == null) throw new IllegalArgumentException("Protection order ID cannot be null");
        if (orderType == null) throw new IllegalArgumentException("Order type cannot be null");
        if (filedDate == null) throw new IllegalArgumentException("Filed date cannot be null");
        if (courtName == null || courtName.trim().isEmpty()) throw new IllegalArgumentException("Court name cannot be null or empty");
        if (protectedParties == null || protectedParties.trim().isEmpty()) throw new IllegalArgumentException("Protected parties cannot be null or empty");
        if (restrainedParties == null || restrainedParties.trim().isEmpty()) throw new IllegalArgumentException("Restrained parties cannot be null or empty");
        if (filedBy == null || filedBy.trim().isEmpty()) throw new IllegalArgumentException("Filed by cannot be null or empty");

        this.clientId = clientId;
        this.protectionOrderId = protectionOrderId;
        this.orderType = orderType;
        this.filedDate = filedDate;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.courtName = courtName;
        this.judgeOrCommissioner = judgeOrCommissioner;
        this.caseNumber = caseNumber;
        this.protectedParties = protectedParties;
        this.restrainedParties = restrainedParties;
        this.orderConditions = orderConditions;
        this.isTemporary = isTemporary;
        this.filedBy = filedBy;
        this.filedByUserId = filedByUserId;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID protectionOrderId() {
        return protectionOrderId;
    }

    public CodeableConcept orderType() {
        return orderType;
    }

    public LocalDate filedDate() {
        return filedDate;
    }

    public LocalDate effectiveDate() {
        return effectiveDate;
    }

    public LocalDate expirationDate() {
        return expirationDate;
    }

    public String courtName() {
        return courtName;
    }

    public String judgeOrCommissioner() {
        return judgeOrCommissioner;
    }

    public String caseNumber() {
        return caseNumber;
    }

    public String protectedParties() {
        return protectedParties;
    }

    public String restrainedParties() {
        return restrainedParties;
    }

    public String orderConditions() {
        return orderConditions;
    }

    public boolean isTemporary() {
        return isTemporary;
    }

    public String filedBy() {
        return filedBy;
    }

    public UUID filedByUserId() {
        return filedByUserId;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getProtectionOrderId() { return protectionOrderId; }
    public CodeableConcept getOrderType() { return orderType; }
    public LocalDate getFiledDate() { return filedDate; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getCourtName() { return courtName; }
    public String getJudgeOrCommissioner() { return judgeOrCommissioner; }
    public String getCaseNumber() { return caseNumber; }
    public String getProtectedParties() { return protectedParties; }
    public String getRestrainedParties() { return restrainedParties; }
    public String getOrderConditions() { return orderConditions; }
    public boolean IsTemporary() { return isTemporary; }
    public String getFiledBy() { return filedBy; }
    public UUID getFiledByUserId() { return filedByUserId; }
}