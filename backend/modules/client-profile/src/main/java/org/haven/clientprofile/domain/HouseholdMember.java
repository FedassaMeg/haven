package org.haven.clientprofile.domain;

import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;

public class HouseholdMember {
    private final HouseholdMemberId id;
    private final CodeableConcept relationship;
    private final Period period;
    private final ClientId relatedClientId;
    
    public HouseholdMember(HouseholdMemberId id, CodeableConcept relationship) {
        this(id, relationship, null, null);
    }
    
    public HouseholdMember(HouseholdMemberId id, CodeableConcept relationship, 
                          Period period, ClientId relatedClientId) {
        this.id = id;
        this.relationship = relationship;
        this.period = period;
        this.relatedClientId = relatedClientId;
    }
    
    public HouseholdMemberId getId() { return id; }
    public CodeableConcept getRelationship() { return relationship; }
    public Period getPeriod() { return period; }
    public ClientId getRelatedClientId() { return relatedClientId; }
    
    public boolean isActive() {
        return period == null || period.isActive();
    }
}
