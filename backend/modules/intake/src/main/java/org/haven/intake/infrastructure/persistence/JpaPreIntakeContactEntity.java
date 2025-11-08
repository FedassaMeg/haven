package org.haven.intake.infrastructure.persistence;

import org.haven.intake.domain.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pre_intake_contacts", schema = "haven")
public class JpaPreIntakeContactEntity {

    @Id
    private UUID id;

    @Column(name = "client_alias", nullable = false, length = 200)
    private String clientAlias;

    @Column(name = "contact_date", nullable = false)
    private LocalDate contactDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "referral_source", nullable = false)
    private ReferralSource referralSource;

    @Column(name = "intake_worker_name", nullable = false, length = 200)
    private String intakeWorkerName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "workflow_data", columnDefinition = "jsonb")
    private Map<String, Object> workflowData = new HashMap<>();

    @Column(name = "current_step")
    private int currentStep = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "expired")
    private boolean expired = false;

    @Column(name = "promoted")
    private boolean promoted = false;

    @Column(name = "promoted_client_id")
    private UUID promotedClientId;

    @Version
    private Long version;

    // Constructors
    protected JpaPreIntakeContactEntity() {
        // JPA requires default constructor
    }

    public JpaPreIntakeContactEntity(
            UUID id,
            String clientAlias,
            LocalDate contactDate,
            ReferralSource referralSource,
            String intakeWorkerName,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.clientAlias = clientAlias;
        this.contactDate = contactDate;
        this.referralSource = referralSource;
        this.intakeWorkerName = intakeWorkerName;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.expiresAt = expiresAt;
        this.currentStep = 1;
        this.expired = false;
        this.promoted = false;
    }

    // Factory methods
    public static JpaPreIntakeContactEntity fromDomain(PreIntakeContact contact) {
        JpaPreIntakeContactEntity entity = new JpaPreIntakeContactEntity(
            contact.getId().value(),
            contact.getClientAlias(),
            contact.getContactDate(),
            contact.getReferralSource(),
            contact.getIntakeWorkerName(),
            contact.getCreatedAt(),
            contact.getExpiresAt()
        );

        entity.workflowData = new HashMap<>(contact.getWorkflowData());
        entity.currentStep = contact.getCurrentStep();
        entity.updatedAt = contact.getUpdatedAt();
        entity.expired = contact.isExpired();
        entity.promoted = contact.isPromoted();
        entity.promotedClientId = contact.getPromotedClientId();

        return entity;
    }

    public PreIntakeContact toDomain() {
        // Use reflection to rehydrate the aggregate without going through events
        // This is a simplification - in a full event-sourced system you'd replay events
        PreIntakeContact contact = new PreIntakeContact();

        try {
            // Set id field
            java.lang.reflect.Field idField = PreIntakeContact.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(contact, new PreIntakeContactId(this.id));

            // Set other fields
            setField(contact, "clientAlias", this.clientAlias);
            setField(contact, "contactDate", this.contactDate);
            setField(contact, "referralSource", this.referralSource);
            setField(contact, "intakeWorkerName", this.intakeWorkerName);
            setField(contact, "workflowData", new HashMap<>(this.workflowData));
            setField(contact, "currentStep", this.currentStep);
            setField(contact, "createdAt", this.createdAt);
            setField(contact, "updatedAt", this.updatedAt);
            setField(contact, "expiresAt", this.expiresAt);
            setField(contact, "expired", this.expired);
            setField(contact, "promoted", this.promoted);
            setField(contact, "promotedClientId", this.promotedClientId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to rehydrate PreIntakeContact from entity", e);
        }

        return contact;
    }

    private void setField(PreIntakeContact contact, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = PreIntakeContact.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(contact, value);
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getClientAlias() { return clientAlias; }
    public void setClientAlias(String clientAlias) { this.clientAlias = clientAlias; }

    public LocalDate getContactDate() { return contactDate; }
    public void setContactDate(LocalDate contactDate) { this.contactDate = contactDate; }

    public ReferralSource getReferralSource() { return referralSource; }
    public void setReferralSource(ReferralSource referralSource) { this.referralSource = referralSource; }

    public String getIntakeWorkerName() { return intakeWorkerName; }
    public void setIntakeWorkerName(String intakeWorkerName) { this.intakeWorkerName = intakeWorkerName; }

    public Map<String, Object> getWorkflowData() { return workflowData; }
    public void setWorkflowData(Map<String, Object> workflowData) { this.workflowData = workflowData; }

    public int getCurrentStep() { return currentStep; }
    public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }

    public boolean isPromoted() { return promoted; }
    public void setPromoted(boolean promoted) { this.promoted = promoted; }

    public UUID getPromotedClientId() { return promotedClientId; }
    public void setPromotedClientId(UUID promotedClientId) { this.promotedClientId = promotedClientId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
