package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.HmisProjectType;

import java.time.Instant;
import java.util.UUID;

/**
 * Program domain entity
 * Represents a program that clients can enroll in
 */
public class Program {
    
    private final UUID id;
    private final String name;
    private final String description;
    private final HmisProjectType hmisProjectType;
    private final String jointProjectGroupCode;
    private final boolean active;
    private final Instant createdAt;
    
    public Program(UUID id, String name, String description, HmisProjectType hmisProjectType, 
                   String jointProjectGroupCode, boolean active, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hmisProjectType = hmisProjectType;
        this.jointProjectGroupCode = jointProjectGroupCode;
        this.active = active;
        this.createdAt = createdAt;
    }
    
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public HmisProjectType getHmisProjectType() { return hmisProjectType; }
    public String getJointProjectGroupCode() { return jointProjectGroupCode; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    
    /**
     * Check if this program supports Joint TH/RRH transitions
     */
    public boolean supportsJointThRrh() {
        return hmisProjectType != null && hmisProjectType.supportsJointFlow();
    }
    
    /**
     * Check if this program can be a TH component in Joint TH/RRH
     */
    public boolean isThComponent() {
        return hmisProjectType != null && 
               (hmisProjectType.isTransitionalHousing() || hmisProjectType.isJointThRrh());
    }
    
    /**
     * Check if this program can be an RRH component in Joint TH/RRH
     */
    public boolean isRrhComponent() {
        return hmisProjectType != null && 
               (hmisProjectType.isRapidRehousing() || hmisProjectType.isJointThRrh());
    }
    
    /**
     * Check if this program is part of the same Joint TH/RRH project as another program
     */
    public boolean isJointWith(Program otherProgram) {
        return jointProjectGroupCode != null && 
               otherProgram.jointProjectGroupCode != null &&
               jointProjectGroupCode.equals(otherProgram.jointProjectGroupCode);
    }
}