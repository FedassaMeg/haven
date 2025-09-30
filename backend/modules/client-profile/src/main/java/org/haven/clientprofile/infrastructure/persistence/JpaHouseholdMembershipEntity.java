package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.*;
import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "household_memberships", schema = "haven")
public class JpaHouseholdMembershipEntity {
    
    @Id
    private UUID membershipId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "composition_id", nullable = false)
    private JpaHouseholdCompositionEntity composition;
    
    @Column(name = "member_id", nullable = false)
    private UUID memberId;
    
    @Column(name = "relationship_code", length = 50)
    private String relationshipCode;
    
    @Column(name = "relationship_display", length = 200)
    private String relationshipDisplay;
    
    @Column(name = "relationship_system", length = 200)
    private String relationshipSystem;
    
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    
    @Column(name = "recorded_by", nullable = false, length = 100)
    private String recordedBy;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
    
    @Version
    private Long version;
    
    protected JpaHouseholdMembershipEntity() {}
    
    public static JpaHouseholdMembershipEntity fromDomain(HouseholdMembershipRecord membership, 
                                                         JpaHouseholdCompositionEntity composition) {
        JpaHouseholdMembershipEntity entity = new JpaHouseholdMembershipEntity();
        entity.membershipId = membership.getMembershipId();
        entity.composition = composition;
        entity.memberId = membership.getMemberId().value();
        
        // Extract relationship information
        CodeableConcept relationship = membership.getRelationship();
        if (relationship != null && !relationship.coding().isEmpty()) {
            var coding = relationship.coding().get(0);
            entity.relationshipCode = coding.code();
            entity.relationshipDisplay = coding.display();
            entity.relationshipSystem = coding.system();
        }
        
        entity.effectiveFrom = membership.getStartDate();
        entity.effectiveTo = membership.getEndDate();
        entity.recordedBy = membership.getRecordedBy();
        entity.reason = membership.getReason();
        entity.recordedAt = membership.getRecordedAt();
        
        return entity;
    }
    
    public HouseholdMembershipRecord toDomain() {
        // Reconstruct the CodeableConcept
        CodeableConcept relationship = null;
        if (relationshipCode != null) {
            var coding = new CodeableConcept.Coding(
                relationshipSystem,
                null, // version
                relationshipCode,
                relationshipDisplay,
                null // userSelected
            );
            relationship = new CodeableConcept(
                List.of(coding),
                relationshipDisplay
            );
        }
        
        // Reconstruct the Period
        Instant startInstant = effectiveFrom != null ? 
            effectiveFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant endInstant = effectiveTo != null ? 
            effectiveTo.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Period period = new Period(startInstant, endInstant);
        
        return new HouseholdMembershipRecord(
            membershipId,
            new ClientId(memberId),
            relationship,
            period,
            recordedBy,
            reason,
            recordedAt
        );
    }
    
    // Getters and setters
    public UUID getMembershipId() { return membershipId; }
    public void setMembershipId(UUID membershipId) { this.membershipId = membershipId; }
    
    public JpaHouseholdCompositionEntity getComposition() { return composition; }
    public void setComposition(JpaHouseholdCompositionEntity composition) { this.composition = composition; }
    
    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }
    
    public String getRelationshipCode() { return relationshipCode; }
    public void setRelationshipCode(String relationshipCode) { this.relationshipCode = relationshipCode; }
    
    public String getRelationshipDisplay() { return relationshipDisplay; }
    public void setRelationshipDisplay(String relationshipDisplay) { this.relationshipDisplay = relationshipDisplay; }
    
    public String getRelationshipSystem() { return relationshipSystem; }
    public void setRelationshipSystem(String relationshipSystem) { this.relationshipSystem = relationshipSystem; }
    
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}