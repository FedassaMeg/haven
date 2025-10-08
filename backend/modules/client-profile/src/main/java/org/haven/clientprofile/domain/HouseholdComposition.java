package org.haven.clientprofile.domain;

import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import org.haven.clientprofile.domain.events.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced household composition with full audit trail and backdating support
 * Addresses the "effective-dated household" gotcha where custody/household changes are common
 */
public class HouseholdComposition extends AggregateRoot<HouseholdCompositionId> {
    
    private ClientId headOfHouseholdId;
    private LocalDate compositionDate; // The date this composition was effective
    private List<HouseholdMembershipRecord> membershipHistory = new ArrayList<>();
    private HouseholdType householdType;
    private String notes;
    private Instant createdAt;
    
    public enum HouseholdType {
        SINGLE_ADULT("Single adult household"),
        FAMILY_WITH_CHILDREN("Family with children"),
        COUPLE_NO_CHILDREN("Couple without children"),
        MULTIGENERATIONAL("Multigenerational household"),
        SHARED_HOUSING("Shared housing arrangement"),
        TEMPORARY_CUSTODY("Temporary custody arrangement"),
        FOSTER_CARE("Foster care placement"),
        OTHER("Other arrangement");
        
        private final String description;
        
        HouseholdType(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public static HouseholdComposition create(ClientId headOfHouseholdId, LocalDate effectiveDate, 
                                            HouseholdType householdType, String recordedBy) {
        HouseholdCompositionId id = HouseholdCompositionId.generate();
        HouseholdComposition composition = new HouseholdComposition();
        composition.apply(new HouseholdCompositionCreated(
            id.getValue(), headOfHouseholdId.value(), effectiveDate, householdType, recordedBy, Instant.now()
        ));
        return composition;
    }
    
    public static HouseholdComposition reconstruct() {
        return new HouseholdComposition();
    }
    
    /**
     * Add a member with effective date (supports backdating)
     */
    public void addMember(ClientId memberId, CodeableConcept relationship, LocalDate effectiveFrom,
                         String recordedBy, String reason) {
        addMember(memberId, relationship, effectiveFrom, null, recordedBy, reason);
    }
    
    /**
     * Add a member with both start and end dates (for historical records)
     */
    public void addMember(ClientId memberId, CodeableConcept relationship, LocalDate effectiveFrom,
                         LocalDate effectiveTo, String recordedBy, String reason) {
        
        // Check for overlapping memberships
        if (hasOverlappingMembership(memberId, effectiveFrom, effectiveTo)) {
            throw new IllegalStateException("Member has overlapping household membership period");
        }
        
        UUID membershipId = UUID.randomUUID();
        apply(new HouseholdMemberAdded(
            id.getValue(), membershipId, memberId.value(), relationship, 
            effectiveFrom, effectiveTo, recordedBy, reason, Instant.now()
        ));
    }
    
    /**
     * End a member's household membership (supports backdating)
     */
    public void removeMember(ClientId memberId, LocalDate effectiveDate, String recordedBy, String reason) {
        Optional<HouseholdMembershipRecord> activeMembership = getActiveMembership(memberId, effectiveDate);
        
        if (activeMembership.isEmpty()) {
            throw new IllegalStateException("No active membership found for member on specified date");
        }
        
        HouseholdMembershipRecord membership = activeMembership.get();
        
        apply(new HouseholdMemberRemoved(
            id.getValue(), membership.getMembershipId(), memberId.value(), 
            effectiveDate, recordedBy, reason, Instant.now()
        ));
    }
    
    /**
     * Update a member's relationship (creates new membership record)
     */
    public void updateMemberRelationship(ClientId memberId, CodeableConcept newRelationship,
                                       LocalDate effectiveDate, String recordedBy, String reason) {
        
        // End current membership
        removeMember(memberId, effectiveDate.minusDays(1), recordedBy, 
                    "Relationship change: " + reason);
        
        // Add new membership with updated relationship
        addMember(memberId, newRelationship, effectiveDate, recordedBy, reason);
    }
    
    /**
     * Record a custody change (special case with enhanced audit)
     */
    public void recordCustodyChange(ClientId childId, CodeableConcept newCustodyRelationship,
                                  LocalDate effectiveDate, String courtOrder, String recordedBy) {
        
        apply(new CustodyChangeRecorded(
            id.getValue(), childId.value(), newCustodyRelationship, effectiveDate,
            courtOrder, recordedBy, Instant.now()
        ));
        
        // Update the actual membership
        updateMemberRelationship(childId, newCustodyRelationship, effectiveDate, recordedBy,
                               "Custody change per court order: " + courtOrder);
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof HouseholdCompositionCreated e) {
            this.id = HouseholdCompositionId.from(e.compositionId());
            this.headOfHouseholdId = new ClientId(e.headOfHouseholdId());
            this.compositionDate = e.effectiveDate();
            this.householdType = e.householdType();
            this.createdAt = e.occurredAt();
        } else if (event instanceof HouseholdMemberAdded e) {
            HouseholdMembershipRecord membership = new HouseholdMembershipRecord(
                e.membershipId(), new ClientId(e.memberId()), e.relationship(),
                new Period(e.effectiveFrom().atStartOfDay().toInstant(java.time.ZoneOffset.UTC), 
                          e.effectiveTo() != null ? e.effectiveTo().atStartOfDay().toInstant(java.time.ZoneOffset.UTC) : null),
                e.recordedBy(), e.reason(), e.occurredAt()
            );
            this.membershipHistory.add(membership);
        } else if (event instanceof HouseholdMemberRemoved e) {
            // Find and update the membership record
            membershipHistory.stream()
                .filter(m -> m.getMembershipId().equals(e.membershipId()))
                .findFirst()
                .ifPresent(membership -> {
                    HouseholdMembershipRecord updatedMembership = membership.endMembership(
                        e.effectiveDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC), 
                        e.recordedBy(), e.reason()
                    );
                    membershipHistory.remove(membership);
                    membershipHistory.add(updatedMembership);
                });
        } else if (event instanceof CustodyChangeRecorded e) {
            // This is handled by the subsequent HouseholdMemberAdded/Removed events
            // The event serves as an audit marker for custody changes
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    // Query methods
    public List<HouseholdMembershipRecord> getActiveMemberships(LocalDate asOfDate) {
        return membershipHistory.stream()
            .filter(m -> m.isActiveOn(asOfDate))
            .toList();
    }
    
    public List<HouseholdMembershipRecord> getAllMemberships() {
        return List.copyOf(membershipHistory);
    }
    
    public Optional<HouseholdMembershipRecord> getActiveMembership(ClientId memberId, LocalDate asOfDate) {
        return membershipHistory.stream()
            .filter(m -> m.getMemberId().equals(memberId) && m.isActiveOn(asOfDate))
            .findFirst();
    }
    
    public List<HouseholdMembershipRecord> getMembershipHistory(ClientId memberId) {
        return membershipHistory.stream()
            .filter(m -> m.getMemberId().equals(memberId))
            .toList();
    }
    
    public boolean hasOverlappingMembership(ClientId memberId, LocalDate startDate, LocalDate endDate) {
        return membershipHistory.stream()
            .filter(m -> m.getMemberId().equals(memberId))
            .anyMatch(m -> m.overlaps(startDate, endDate));
    }
    
    public int getHouseholdSizeOn(LocalDate date) {
        return getActiveMemberships(date).size() + 1; // +1 for head of household
    }
    
    public List<HouseholdMembershipRecord> getChildrenOn(LocalDate date) {
        return getActiveMemberships(date).stream()
            .filter(m -> isChildRelationship(m.getRelationship()))
            .toList();
    }
    
    private boolean isChildRelationship(CodeableConcept relationship) {
        // This would check if the relationship indicates a child
        String code = relationship.coding().isEmpty() ? "" : relationship.coding().get(0).code();
        return code.contains("child") || code.contains("son") || code.contains("daughter");
    }
    
    // Getters
    public ClientId getHeadOfHouseholdId() { return headOfHouseholdId; }
    public LocalDate getCompositionDate() { return compositionDate; }
    public List<HouseholdMembershipRecord> getMembershipHistory() { return List.copyOf(membershipHistory); }
    public HouseholdType getHouseholdType() { return householdType; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}