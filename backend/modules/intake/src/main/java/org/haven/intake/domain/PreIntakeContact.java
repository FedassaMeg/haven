package org.haven.intake.domain;

import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.intake.domain.events.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pre-Intake Contact aggregate
 *
 * Represents a temporary client contact created during the intake workflow
 * before full demographic information is collected. This allows tracking
 * the intake process without requiring complete PII upfront.
 *
 * Lifecycle:
 * - Created when Step 1 (Initial Contact) is completed
 * - Updated as user progresses through Steps 2-7
 * - Promoted to full Client when Step 8 (Demographics) is completed
 * - Auto-deleted after 30 days if not promoted (TTL)
 *
 * VAWA Compliance:
 * - Minimal PII collection before consent
 * - Supports alias names for client safety
 * - TTL ensures temporary data doesn't persist indefinitely
 */
public class PreIntakeContact extends AggregateRoot<PreIntakeContactId> {

    // Basic identification
    private String clientAlias;
    private LocalDate contactDate;
    private ReferralSource referralSource;
    private String intakeWorkerName;

    // Workflow progress tracking
    private Map<String, Object> workflowData = new HashMap<>();
    private int currentStep = 1;

    // Lifecycle management
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private boolean expired = false;
    private boolean promoted = false;
    private UUID promotedClientId;

    /**
     * Create a new pre-intake contact record
     */
    public static PreIntakeContact create(
            String clientAlias,
            LocalDate contactDate,
            ReferralSource referralSource,
            String intakeWorkerName) {

        PreIntakeContactId id = PreIntakeContactId.generate();
        PreIntakeContact contact = new PreIntakeContact();

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(30L * 24 * 60 * 60); // 30 days

        contact.apply(new PreIntakeContactCreated(
            id.value(),
            clientAlias,
            contactDate,
            referralSource,
            intakeWorkerName,
            now,
            expiry
        ));

        return contact;
    }

    /**
     * Update workflow data as user progresses through intake steps
     */
    public void updateWorkflowData(int step, Map<String, Object> stepData) {
        if (this.expired) {
            throw new IllegalStateException("Cannot update expired pre-intake contact");
        }

        if (this.promoted) {
            throw new IllegalStateException("Cannot update promoted pre-intake contact");
        }

        apply(new PreIntakeWorkflowUpdated(
            this.id.value(),
            step,
            stepData,
            Instant.now()
        ));
    }

    /**
     * Update basic contact information
     */
    public void updateContactInfo(
            String clientAlias,
            LocalDate contactDate,
            ReferralSource referralSource) {

        if (this.expired) {
            throw new IllegalStateException("Cannot update expired pre-intake contact");
        }

        if (this.promoted) {
            throw new IllegalStateException("Cannot update promoted pre-intake contact");
        }

        apply(new PreIntakeContactInfoUpdated(
            this.id.value(),
            clientAlias,
            contactDate,
            referralSource,
            Instant.now()
        ));
    }

    /**
     * Mark as promoted to full client
     */
    public void markPromoted(UUID clientId) {
        if (this.promoted) {
            throw new IllegalStateException("Pre-intake contact already promoted");
        }

        if (this.expired) {
            throw new IllegalStateException("Cannot promote expired pre-intake contact");
        }

        apply(new PreIntakeContactPromoted(
            this.id.value(),
            clientId,
            Instant.now()
        ));
    }

    /**
     * Mark as expired (for TTL cleanup)
     */
    public void markExpired() {
        if (this.promoted) {
            throw new IllegalStateException("Cannot expire promoted pre-intake contact");
        }

        if (!this.expired) {
            apply(new PreIntakeContactExpired(
                this.id.value(),
                Instant.now()
            ));
        }
    }

    /**
     * Check if this contact has expired
     */
    public boolean isExpired() {
        if (this.expired) {
            return true;
        }
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * Get workflow data for a specific step
     */
    public Map<String, Object> getStepData(int step) {
        String key = "step_" + step;
        Object data = workflowData.get(key);
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stepData = (Map<String, Object>) data;
            return new HashMap<>(stepData);
        }
        return new HashMap<>();
    }

    @Override
    protected void when(DomainEvent e) {
        if (e instanceof PreIntakeContactCreated ev) {
            this.id = new PreIntakeContactId(ev.tempClientId());
            this.clientAlias = ev.clientAlias();
            this.contactDate = ev.contactDate();
            this.referralSource = ev.referralSource();
            this.intakeWorkerName = ev.intakeWorkerName();
            this.createdAt = ev.occurredAt();
            this.updatedAt = ev.occurredAt();
            this.expiresAt = ev.expiresAt();
            this.currentStep = 1;
            this.expired = false;
            this.promoted = false;
        } else if (e instanceof PreIntakeWorkflowUpdated ev) {
            this.currentStep = ev.step();
            String key = "step_" + ev.step();
            this.workflowData.put(key, new HashMap<>(ev.stepData()));
            this.updatedAt = ev.occurredAt();
        } else if (e instanceof PreIntakeContactInfoUpdated ev) {
            this.clientAlias = ev.clientAlias();
            this.contactDate = ev.contactDate();
            this.referralSource = ev.referralSource();
            this.updatedAt = ev.occurredAt();
        } else if (e instanceof PreIntakeContactPromoted ev) {
            this.promoted = true;
            this.promotedClientId = ev.clientId();
            this.updatedAt = ev.occurredAt();
        } else if (e instanceof PreIntakeContactExpired ev) {
            this.expired = true;
            this.updatedAt = ev.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + e.getClass());
        }
    }

    // Getters
    public String getClientAlias() { return clientAlias; }
    public LocalDate getContactDate() { return contactDate; }
    public ReferralSource getReferralSource() { return referralSource; }
    public String getIntakeWorkerName() { return intakeWorkerName; }
    public Map<String, Object> getWorkflowData() { return new HashMap<>(workflowData); }
    public int getCurrentStep() { return currentStep; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isPromoted() { return promoted; }
    public UUID getPromotedClientId() { return promotedClientId; }
}
