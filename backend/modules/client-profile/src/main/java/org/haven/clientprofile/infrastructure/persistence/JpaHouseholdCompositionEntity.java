package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.*;
import org.haven.clientprofile.domain.events.*;
import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "household_compositions", schema = "haven")
public class JpaHouseholdCompositionEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "head_of_household_id", nullable = false)
    private UUID headOfHouseholdId;
    
    @Column(name = "composition_date", nullable = false)
    private LocalDate compositionDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "household_type", nullable = false)
    private HouseholdComposition.HouseholdType householdType;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @OneToMany(mappedBy = "composition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<JpaHouseholdMembershipEntity> memberships = new ArrayList<>();
    
    @Version
    private Long version;
    
    protected JpaHouseholdCompositionEntity() {}
    
    public static JpaHouseholdCompositionEntity fromDomain(HouseholdComposition composition) {
        JpaHouseholdCompositionEntity entity = new JpaHouseholdCompositionEntity();
        entity.id = composition.getId().getValue();
        entity.headOfHouseholdId = composition.getHeadOfHouseholdId().value();
        entity.compositionDate = composition.getCompositionDate();
        entity.householdType = composition.getHouseholdType();
        entity.notes = composition.getNotes();
        entity.createdAt = composition.getCreatedAt();
        
        // Convert membership history to entities
        for (HouseholdMembershipRecord membership : composition.getMembershipHistory()) {
            JpaHouseholdMembershipEntity membershipEntity = JpaHouseholdMembershipEntity.fromDomain(membership, entity);
            entity.memberships.add(membershipEntity);
        }
        
        return entity;
    }
    
    public HouseholdComposition toDomain() {
        // Create an empty composition for reconstruction
        HouseholdComposition composition = HouseholdComposition.reconstruct();
        
        // Reconstruct using events to preserve the original ID
        HouseholdCompositionCreated creationEvent = new HouseholdCompositionCreated(
            id, headOfHouseholdId, compositionDate, householdType, "system", createdAt
        );
        composition.replay(creationEvent, 1);
        
        // Apply membership events
        long eventVersion = 2;
        for (JpaHouseholdMembershipEntity membershipEntity : memberships) {
            HouseholdMembershipRecord membership = membershipEntity.toDomain();
            
            // Create member added event from the membership record
            HouseholdMemberAdded memberAddedEvent = new HouseholdMemberAdded(
                id,
                membership.getMembershipId(),
                membership.getMemberId().value(),
                membership.getRelationship(),
                membership.getStartDate(),
                membership.getEndDate(),
                membership.getRecordedBy(),
                membership.getReason(),
                membership.getRecordedAt()
            );
            
            composition.replay(memberAddedEvent, eventVersion++);
            
            // If the membership has an end date, also replay the removal event
            if (membership.getEndDate() != null) {
                HouseholdMemberRemoved memberRemovedEvent = new HouseholdMemberRemoved(
                    id,
                    membership.getMembershipId(),
                    membership.getMemberId().value(),
                    membership.getEndDate(),
                    membership.getRecordedBy(),
                    membership.getReason(),
                    membership.getRecordedAt()
                );
                
                composition.replay(memberRemovedEvent, eventVersion++);
            }
        }
        
        return composition;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getHeadOfHouseholdId() { return headOfHouseholdId; }
    public void setHeadOfHouseholdId(UUID headOfHouseholdId) { this.headOfHouseholdId = headOfHouseholdId; }
    
    public LocalDate getCompositionDate() { return compositionDate; }
    public void setCompositionDate(LocalDate compositionDate) { this.compositionDate = compositionDate; }
    
    public HouseholdComposition.HouseholdType getHouseholdType() { return householdType; }
    public void setHouseholdType(HouseholdComposition.HouseholdType householdType) { this.householdType = householdType; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public List<JpaHouseholdMembershipEntity> getMemberships() { return memberships; }
    public void setMemberships(List<JpaHouseholdMembershipEntity> memberships) { this.memberships = memberships; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}