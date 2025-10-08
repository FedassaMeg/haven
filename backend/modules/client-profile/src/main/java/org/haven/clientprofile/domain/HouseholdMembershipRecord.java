package org.haven.clientprofile.domain;

import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Immutable record of a household membership with full audit trail
 */
public class HouseholdMembershipRecord {
    private final UUID membershipId;
    private final ClientId memberId;
    private final CodeableConcept relationship;
    private final Period membershipPeriod;
    private final String recordedBy;
    private final String reason;
    private final Instant recordedAt;
    
    public HouseholdMembershipRecord(UUID membershipId, ClientId memberId, CodeableConcept relationship,
                                   Period membershipPeriod, String recordedBy, String reason, Instant recordedAt) {
        this.membershipId = membershipId;
        this.memberId = memberId;
        this.relationship = relationship;
        this.membershipPeriod = membershipPeriod;
        this.recordedBy = recordedBy;
        this.reason = reason;
        this.recordedAt = recordedAt;
    }
    
    public boolean isActiveOn(LocalDate date) {
        if (membershipPeriod == null) return false;
        
        Instant dateInstant = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        return membershipPeriod.contains(dateInstant);
    }
    
    public boolean isActive() {
        return membershipPeriod != null && membershipPeriod.isActive();
    }
    
    public boolean overlaps(LocalDate startDate, LocalDate endDate) {
        if (membershipPeriod == null) return false;
        
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate != null ? 
            endDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        
        // Manual overlap check since Period doesn't have overlaps method
        Instant thisStart = membershipPeriod.start();
        Instant thisEnd = membershipPeriod.end();
        
        // Two periods overlap if start1 <= end2 && start2 <= end1 (handling nulls as "unbounded")
        boolean startOverlaps = (thisStart == null || endInstant == null || !thisStart.isAfter(endInstant));
        boolean endOverlaps = (startInstant == null || thisEnd == null || !startInstant.isAfter(thisEnd));
        
        return startOverlaps && endOverlaps;
    }
    
    public HouseholdMembershipRecord endMembership(Instant endDate, String endedBy, String endReason) {
        Period newPeriod = new Period(membershipPeriod.start(), endDate);
        return new HouseholdMembershipRecord(
            this.membershipId, this.memberId, this.relationship, newPeriod,
            endedBy, this.reason + " | Ended: " + endReason, Instant.now()
        );
    }
    
    public LocalDate getStartDate() {
        return membershipPeriod != null && membershipPeriod.start() != null ?
            membershipPeriod.start().atZone(ZoneOffset.UTC).toLocalDate() : null;
    }
    
    public LocalDate getEndDate() {
        return membershipPeriod != null && membershipPeriod.end() != null ?
            membershipPeriod.end().atZone(ZoneOffset.UTC).toLocalDate() : null;
    }
    
    public long getDurationDays() {
        if (membershipPeriod == null || membershipPeriod.start() == null) {
            return 0;
        }
        
        Instant end = membershipPeriod.end() != null ? membershipPeriod.end() : Instant.now();
        return java.time.Duration.between(membershipPeriod.start(), end).toDays();
    }
    
    // Getters
    public UUID getMembershipId() { return membershipId; }
    public ClientId getMemberId() { return memberId; }
    public CodeableConcept getRelationship() { return relationship; }
    public Period getMembershipPeriod() { return membershipPeriod; }
    public String getRecordedBy() { return recordedBy; }
    public String getReason() { return reason; }
    public Instant getRecordedAt() { return recordedAt; }
}