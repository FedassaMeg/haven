package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.casemgmt.domain.events.*;
import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Legal Advocacy aggregate for tracking protection orders and legal proceedings
 * Manages legal documentation and court proceedings for DV survivors
 */
public class LegalAdvocacyAggregate extends AggregateRoot<LegalAdvocacyId> {
    
    private ClientId clientId;
    private CaseId caseId;
    private String advocateName;
    private String advocateContact;
    private String legalService;
    private LegalAdvocacyStatus status;
    
    // Protection Order tracking
    private List<ProtectionOrder> protectionOrders = new ArrayList<>();
    
    // Court proceedings
    private List<CourtHearing> scheduledHearings = new ArrayList<>();
    private List<CourtHearing> completedHearings = new ArrayList<>();
    
    private Instant createdAt;
    private Instant lastModified;
    
    public static LegalAdvocacyAggregate initiate(ClientId clientId, CaseId caseId, 
                                                 String advocateName, String advocateContact,
                                                 String legalService) {
        LegalAdvocacyId legalAdvocacyId = LegalAdvocacyId.generate();
        LegalAdvocacyAggregate advocacy = new LegalAdvocacyAggregate();
        advocacy.id = legalAdvocacyId;
        advocacy.clientId = clientId;
        advocacy.caseId = caseId;
        advocacy.advocateName = advocateName;
        advocacy.advocateContact = advocateContact;
        advocacy.legalService = legalService;
        advocacy.status = LegalAdvocacyStatus.ACTIVE;
        advocacy.createdAt = Instant.now();
        advocacy.lastModified = Instant.now();
        return advocacy;
    }
    
    public void fileProtectionOrder(CodeableConcept orderType, LocalDate filedDate, 
                                  LocalDate effectiveDate, LocalDate expirationDate,
                                  String courtName, String judgeOrCommissioner, String caseNumber,
                                  String protectedParties, String restrainedParties,
                                  String orderConditions, boolean isTemporary, String filedBy) {
        UUID protectionOrderId = UUID.randomUUID();
        
        apply(new ProtectionOrderFiled(
            caseId.value(),
            clientId.value(),
            protectionOrderId,
            orderType,
            filedDate,
            effectiveDate,
            expirationDate,
            courtName,
            judgeOrCommissioner,
            caseNumber,
            protectedParties,
            restrainedParties,
            orderConditions,
            isTemporary,
            filedBy,
            UUID.randomUUID(), // filedByUserId
            Instant.now()
        ));
    }
    
    public void updateProtectionOrder(UUID protectionOrderId, LocalDate updateDate,
                                    String updateType, CodeableConcept updateReason,
                                    LocalDate newExpirationDate, String updatedConditions,
                                    String courtName, String updatedBy, String updateNotes) {
        apply(new ProtectionOrderUpdated(
            caseId.value(),
            clientId.value(),
            protectionOrderId,
            updateDate,
            updateType,
            updateReason,
            newExpirationDate,
            updatedConditions,
            courtName,
            updatedBy,
            UUID.randomUUID(), // updatedByUserId
            updateNotes,
            Instant.now()
        ));
    }
    
    public void scheduleHearing(LocalDateTime hearingDateTime, String courtName,
                              String hearingType, String purpose, String judgeName,
                              String address, String scheduledBy, String notes) {
        apply(new HearingScheduled(
            id.value(),
            clientId.value(),
            caseId.value(),
            hearingDateTime,
            courtName,
            hearingType,
            purpose,
            judgeName,
            address,
            scheduledBy,
            notes,
            Instant.now()
        ));
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof ProtectionOrderFiled e) {
            ProtectionOrder order = new ProtectionOrder(
                e.protectionOrderId(),
                e.orderType(),
                e.filedDate(),
                e.effectiveDate(),
                e.expirationDate(),
                e.courtName(),
                e.judgeOrCommissioner(),
                e.caseNumber(),
                e.protectedParties(),
                e.restrainedParties(),
                e.orderConditions(),
                e.isTemporary(),
                e.filedBy(),
                ProtectionOrderStatus.ACTIVE
            );
            this.protectionOrders.add(order);
            this.lastModified = e.occurredAt();
        } else if (event instanceof ProtectionOrderUpdated e) {
            // Find and update the protection order
            protectionOrders.stream()
                .filter(order -> order.protectionOrderId().equals(e.protectionOrderId()))
                .findFirst()
                .ifPresent(order -> {
                    // Update order details (in a real implementation, create a new immutable version)
                });
            this.lastModified = e.occurredAt();
        } else if (event instanceof HearingScheduled e) {
            CourtHearing hearing = new CourtHearing(
                UUID.randomUUID(),
                e.hearingDateTime(),
                e.courtName(),
                e.hearingType(),
                e.purpose(),
                e.judgeName(),
                e.address(),
                e.scheduledBy(),
                e.notes(),
                HearingStatus.SCHEDULED
            );
            this.scheduledHearings.add(hearing);
            this.lastModified = e.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum LegalAdvocacyStatus {
        ACTIVE, COMPLETED, SUSPENDED, TRANSFERRED
    }
    
    public enum ProtectionOrderStatus {
        ACTIVE, EXPIRED, MODIFIED, DISMISSED, VIOLATED
    }
    
    public enum HearingStatus {
        SCHEDULED, COMPLETED, CANCELLED, POSTPONED
    }
    
    public record ProtectionOrder(
        UUID protectionOrderId,
        CodeableConcept orderType,
        LocalDate filedDate,
        LocalDate effectiveDate,
        LocalDate expirationDate,
        String courtName,
        String judgeOrCommissioner,
        String caseNumber,
        String protectedParties,
        String restrainedParties,
        String orderConditions,
        boolean isTemporary,
        String filedBy,
        ProtectionOrderStatus status
    ) {}
    
    public record CourtHearing(
        UUID hearingId,
        LocalDateTime hearingDateTime,
        String courtName,
        String hearingType,
        String purpose,
        String judgeName,
        String address,
        String scheduledBy,
        String notes,
        HearingStatus status
    ) {}
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public CaseId getCaseId() { return caseId; }
    public String getAdvocateName() { return advocateName; }
    public String getAdvocateContact() { return advocateContact; }
    public String getLegalService() { return legalService; }
    public LegalAdvocacyStatus getStatus() { return status; }
    public List<ProtectionOrder> getProtectionOrders() { return List.copyOf(protectionOrders); }
    public List<CourtHearing> getScheduledHearings() { return List.copyOf(scheduledHearings); }
    public List<CourtHearing> getCompletedHearings() { return List.copyOf(completedHearings); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    
    public boolean hasActiveProtectionOrders() {
        return protectionOrders.stream()
            .anyMatch(order -> order.status() == ProtectionOrderStatus.ACTIVE);
    }
    
    public boolean hasUpcomingHearings() {
        return scheduledHearings.stream()
            .anyMatch(hearing -> hearing.status() == HearingStatus.SCHEDULED &&
                       hearing.hearingDateTime().isAfter(LocalDateTime.now()));
    }
}