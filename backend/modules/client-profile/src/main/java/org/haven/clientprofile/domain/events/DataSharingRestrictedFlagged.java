package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DataSharingRestrictedFlagged extends DomainEvent {
    private final UUID clientId;
    private final List<String> restrictedDataTypes;
    private final CodeableConcept restrictionReason;
    private final String restrictionLevel;
    private final List<String> authorizedRoles;
    private final List<String> authorizedAgencies;
    private final String flaggedBy;
    private final UUID flaggedByUserId;
    private final String restrictionNotes;
    private final boolean isPermanentRestriction;
    private final Instant restrictionExpiryDate;
    private final String legalBasis;

    public DataSharingRestrictedFlagged(
        UUID clientId,
        List<String> restrictedDataTypes,
        CodeableConcept restrictionReason,
        String restrictionLevel,
        List<String> authorizedRoles,
        List<String> authorizedAgencies,
        String flaggedBy,
        UUID flaggedByUserId,
        String restrictionNotes,
        boolean isPermanentRestriction,
        Instant restrictionExpiryDate,
        String legalBasis,
        Instant occurredAt
    ) {
        super(clientId, occurredAt != null ? occurredAt : Instant.now());
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (restrictedDataTypes == null || restrictedDataTypes.isEmpty()) throw new IllegalArgumentException("Restricted data types cannot be null or empty");
        if (restrictionReason == null) throw new IllegalArgumentException("Restriction reason cannot be null");
        if (restrictionLevel == null || restrictionLevel.trim().isEmpty()) throw new IllegalArgumentException("Restriction level cannot be null or empty");
        if (flaggedBy == null || flaggedBy.trim().isEmpty()) throw new IllegalArgumentException("Flagged by cannot be null or empty");

        this.clientId = clientId;
        this.restrictedDataTypes = restrictedDataTypes;
        this.restrictionReason = restrictionReason;
        this.restrictionLevel = restrictionLevel;
        this.authorizedRoles = authorizedRoles;
        this.authorizedAgencies = authorizedAgencies;
        this.flaggedBy = flaggedBy;
        this.flaggedByUserId = flaggedByUserId;
        this.restrictionNotes = restrictionNotes;
        this.isPermanentRestriction = isPermanentRestriction;
        this.restrictionExpiryDate = restrictionExpiryDate;
        this.legalBasis = legalBasis;
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public List<String> restrictedDataTypes() {
        return restrictedDataTypes;
    }

    public CodeableConcept restrictionReason() {
        return restrictionReason;
    }

    public String restrictionLevel() {
        return restrictionLevel;
    }

    public List<String> authorizedRoles() {
        return authorizedRoles;
    }

    public List<String> authorizedAgencies() {
        return authorizedAgencies;
    }

    public String flaggedBy() {
        return flaggedBy;
    }

    public UUID flaggedByUserId() {
        return flaggedByUserId;
    }

    public String restrictionNotes() {
        return restrictionNotes;
    }

    public boolean isPermanentRestriction() {
        return isPermanentRestriction;
    }

    public Instant restrictionExpiryDate() {
        return restrictionExpiryDate;
    }

    public String legalBasis() {
        return legalBasis;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public List<String> getRestrictedDataTypes() {
        return restrictedDataTypes;
    }

    public CodeableConcept getRestrictionReason() {
        return restrictionReason;
    }

    public String getRestrictionLevel() {
        return restrictionLevel;
    }

    public List<String> getAuthorizedRoles() {
        return authorizedRoles;
    }

    public List<String> getAuthorizedAgencies() {
        return authorizedAgencies;
    }

    public String getFlaggedBy() {
        return flaggedBy;
    }

    public UUID getFlaggedByUserId() {
        return flaggedByUserId;
    }

    public String getRestrictionNotes() {
        return restrictionNotes;
    }

    public boolean getIsPermanentRestriction() {
        return isPermanentRestriction;
    }

    public Instant getRestrictionExpiryDate() {
        return restrictionExpiryDate;
    }

    public String getLegalBasis() {
        return legalBasis;
    }
}