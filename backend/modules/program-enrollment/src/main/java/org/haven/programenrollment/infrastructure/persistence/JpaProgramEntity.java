package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.shared.vo.hmis.HmisProjectType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programs", schema = "haven")
public class JpaProgramEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "hmis_project_type")
    private Integer hmisProjectType;
    
    @Column(name = "joint_project_group_code")
    private String jointProjectGroupCode;
    
    @Column(name = "active", nullable = false)
    private Boolean active;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Version
    @Column(name = "version")
    private Integer version;
    
    protected JpaProgramEntity() {}
    
    public JpaProgramEntity(UUID id, String name, String description, Integer hmisProjectType,
                           String jointProjectGroupCode, Boolean active, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hmisProjectType = hmisProjectType;
        this.jointProjectGroupCode = jointProjectGroupCode;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getHmisProjectType() { return hmisProjectType; }
    public void setHmisProjectType(Integer hmisProjectType) { this.hmisProjectType = hmisProjectType; }
    
    public String getJointProjectGroupCode() { return jointProjectGroupCode; }
    public void setJointProjectGroupCode(String jointProjectGroupCode) { 
        this.jointProjectGroupCode = jointProjectGroupCode; 
    }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public HmisProjectType getHmisProjectTypeEnum() {
        return hmisProjectType != null ? HmisProjectType.fromHmisTypeId(hmisProjectType) : null;
    }
}