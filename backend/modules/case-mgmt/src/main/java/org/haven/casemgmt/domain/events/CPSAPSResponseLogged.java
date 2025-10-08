package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class CPSAPSResponseLogged extends DomainEvent {
    private final UUID clientId;
    private final UUID mandatedReportId;
    private final String agencyName;
    private final String caseworkerName;
    private final String caseworkerContact;
    private final LocalDate responseDate;
    private final CodeableConcept responseType;
    private final String investigationStatus;
    private final String findingsOverview;
    private final String actionsRequired;
    private final String followUpDate;
    private final String responseNotes;
    private final String loggedBy;
    private final UUID loggedByUserId;
    private final boolean requiresImmediateAction;

    public CPSAPSResponseLogged(UUID caseId, UUID clientId, UUID mandatedReportId, String agencyName, String caseworkerName, String caseworkerContact, LocalDate responseDate, CodeableConcept responseType, String investigationStatus, String findingsOverview, String actionsRequired, String followUpDate, String responseNotes, String loggedBy, UUID loggedByUserId, boolean requiresImmediateAction, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (mandatedReportId == null) throw new IllegalArgumentException("Mandated report ID cannot be null");
        if (agencyName == null || agencyName.trim().isEmpty()) throw new IllegalArgumentException("Agency name cannot be null or empty");
        if (responseDate == null) throw new IllegalArgumentException("Response date cannot be null");
        if (responseType == null) throw new IllegalArgumentException("Response type cannot be null");
        if (loggedBy == null || loggedBy.trim().isEmpty()) throw new IllegalArgumentException("Logged by cannot be null or empty");

        this.clientId = clientId;
        this.mandatedReportId = mandatedReportId;
        this.agencyName = agencyName;
        this.caseworkerName = caseworkerName;
        this.caseworkerContact = caseworkerContact;
        this.responseDate = responseDate;
        this.responseType = responseType;
        this.investigationStatus = investigationStatus;
        this.findingsOverview = findingsOverview;
        this.actionsRequired = actionsRequired;
        this.followUpDate = followUpDate;
        this.responseNotes = responseNotes;
        this.loggedBy = loggedBy;
        this.loggedByUserId = loggedByUserId;
        this.requiresImmediateAction = requiresImmediateAction;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID mandatedReportId() {
        return mandatedReportId;
    }

    public String agencyName() {
        return agencyName;
    }

    public String caseworkerName() {
        return caseworkerName;
    }

    public String caseworkerContact() {
        return caseworkerContact;
    }

    public LocalDate responseDate() {
        return responseDate;
    }

    public CodeableConcept responseType() {
        return responseType;
    }

    public String investigationStatus() {
        return investigationStatus;
    }

    public String findingsOverview() {
        return findingsOverview;
    }

    public String actionsRequired() {
        return actionsRequired;
    }

    public String followUpDate() {
        return followUpDate;
    }

    public String responseNotes() {
        return responseNotes;
    }

    public String loggedBy() {
        return loggedBy;
    }

    public UUID loggedByUserId() {
        return loggedByUserId;
    }

    public boolean requiresImmediateAction() {
        return requiresImmediateAction;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getMandatedReportId() { return mandatedReportId; }
    public String getAgencyName() { return agencyName; }
    public String getCaseworkerName() { return caseworkerName; }
    public String getCaseworkerContact() { return caseworkerContact; }
    public LocalDate getResponseDate() { return responseDate; }
    public CodeableConcept getResponseType() { return responseType; }
    public String getInvestigationStatus() { return investigationStatus; }
    public String getFindingsOverview() { return findingsOverview; }
    public String getActionsRequired() { return actionsRequired; }
    public String getFollowUpDate() { return followUpDate; }
    public String getResponseNotes() { return responseNotes; }
    public String getLoggedBy() { return loggedBy; }
    public UUID getLoggedByUserId() { return loggedByUserId; }
    public boolean getRequiresImmediateAction() { return requiresImmediateAction; }
}