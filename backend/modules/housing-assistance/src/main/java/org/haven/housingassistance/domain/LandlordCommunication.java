package org.haven.housingassistance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing landlord communication with consent tracking
 */
@Entity
@Table(name = "landlord_communications")
public class LandlordCommunication {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "landlord_id", nullable = false)
    private UUID landlordId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "housing_assistance_id")
    private UUID housingAssistanceId;
    
    @Column(name = "channel", nullable = false, length = 30)
    private String channel;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shared_fields", columnDefinition = "jsonb")
    private Map<String, Object> sharedFields;
    
    @Column(name = "recipient_contact")
    private String recipientContact;
    
    @Column(name = "consent_checked", nullable = false)
    private Boolean consentChecked = false;
    
    @Column(name = "consent_type", length = 50)
    private String consentType;
    
    @Column(name = "sent_status", nullable = false, length = 20)
    private String sentStatus = "DRAFT";
    
    @Column(name = "sent_at")
    private Instant sentAt;
    
    @Column(name = "sent_by")
    private UUID sentBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getLandlordId() {
        return landlordId;
    }
    
    public void setLandlordId(UUID landlordId) {
        this.landlordId = landlordId;
    }
    
    public UUID getClientId() {
        return clientId;
    }
    
    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }
    
    public UUID getHousingAssistanceId() {
        return housingAssistanceId;
    }
    
    public void setHousingAssistanceId(UUID housingAssistanceId) {
        this.housingAssistanceId = housingAssistanceId;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public Map<String, Object> getSharedFields() {
        return sharedFields;
    }
    
    public void setSharedFields(Map<String, Object> sharedFields) {
        this.sharedFields = sharedFields;
    }
    
    public String getRecipientContact() {
        return recipientContact;
    }
    
    public void setRecipientContact(String recipientContact) {
        this.recipientContact = recipientContact;
    }
    
    public Boolean getConsentChecked() {
        return consentChecked;
    }
    
    public void setConsentChecked(Boolean consentChecked) {
        this.consentChecked = consentChecked;
    }
    
    public String getConsentType() {
        return consentType;
    }
    
    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }
    
    public String getSentStatus() {
        return sentStatus;
    }
    
    public void setSentStatus(String sentStatus) {
        this.sentStatus = sentStatus;
    }
    
    public Instant getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
    
    public UUID getSentBy() {
        return sentBy;
    }
    
    public void setSentBy(UUID sentBy) {
        this.sentBy = sentBy;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}