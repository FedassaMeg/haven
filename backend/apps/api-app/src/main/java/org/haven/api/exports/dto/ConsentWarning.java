package org.haven.api.exports.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing a VAWA consent warning for export
 */
public class ConsentWarning {
    private UUID clientId;
    private String clientInitials;
    private String warningType; // MISSING_CONSENT, CONSENT_REVOKED, CONSENT_EXPIRED
    private String consentStatus;
    private LocalDate consentDate;
    private LocalDate consentExpiryDate;
    private boolean blocksIndividualData;
    private boolean requiresAggregateOnlyMode;
    private String affectedDataElements;
    private String recommendation;

    public ConsentWarning() {}

    public ConsentWarning(UUID clientId, String clientInitials, String warningType,
                         String consentStatus, LocalDate consentDate, LocalDate consentExpiryDate,
                         boolean blocksIndividualData, boolean requiresAggregateOnlyMode,
                         String affectedDataElements, String recommendation) {
        this.clientId = clientId;
        this.clientInitials = clientInitials;
        this.warningType = warningType;
        this.consentStatus = consentStatus;
        this.consentDate = consentDate;
        this.consentExpiryDate = consentExpiryDate;
        this.blocksIndividualData = blocksIndividualData;
        this.requiresAggregateOnlyMode = requiresAggregateOnlyMode;
        this.affectedDataElements = affectedDataElements;
        this.recommendation = recommendation;
    }

    // Getters and setters
    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getClientInitials() {
        return clientInitials;
    }

    public void setClientInitials(String clientInitials) {
        this.clientInitials = clientInitials;
    }

    public String getWarningType() {
        return warningType;
    }

    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }

    public String getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(String consentStatus) {
        this.consentStatus = consentStatus;
    }

    public LocalDate getConsentDate() {
        return consentDate;
    }

    public void setConsentDate(LocalDate consentDate) {
        this.consentDate = consentDate;
    }

    public LocalDate getConsentExpiryDate() {
        return consentExpiryDate;
    }

    public void setConsentExpiryDate(LocalDate consentExpiryDate) {
        this.consentExpiryDate = consentExpiryDate;
    }

    public boolean getBlocksIndividualData() {
        return blocksIndividualData;
    }

    public void setBlocksIndividualData(boolean blocksIndividualData) {
        this.blocksIndividualData = blocksIndividualData;
    }

    public boolean getRequiresAggregateOnlyMode() {
        return requiresAggregateOnlyMode;
    }

    public void setRequiresAggregateOnlyMode(boolean requiresAggregateOnlyMode) {
        this.requiresAggregateOnlyMode = requiresAggregateOnlyMode;
    }

    public String getAffectedDataElements() {
        return affectedDataElements;
    }

    public void setAffectedDataElements(String affectedDataElements) {
        this.affectedDataElements = affectedDataElements;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
