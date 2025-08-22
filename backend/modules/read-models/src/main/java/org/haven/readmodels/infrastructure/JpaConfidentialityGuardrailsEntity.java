package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.ConfidentialityGuardrails;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "confidentiality_guardrails")
public class JpaConfidentialityGuardrailsEntity {
    
    @Id
    @Column(name = "client_id")
    private UUID clientId;
    
    @Column(name = "client_name")
    private String clientName;
    
    @Column(name = "is_safe_at_home")
    private Boolean isSafeAtHome;
    
    @Column(name = "is_comparable_db_only")
    private Boolean isComparableDbOnly;
    
    @Column(name = "has_confidential_location")
    private Boolean hasConfidentialLocation;
    
    @Column(name = "has_restricted_data")
    private Boolean hasRestrictedData;
    
    @Column(name = "data_system")
    private String dataSystem;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_level")
    private ConfidentialityGuardrails.VisibilityLevel visibilityLevel;
    
    @Column(name = "last_updated")
    private Instant lastUpdated;
    
    // Default constructor for JPA
    public JpaConfidentialityGuardrailsEntity() {}
    
    // Constructor from domain object
    public JpaConfidentialityGuardrailsEntity(ConfidentialityGuardrails guardrails) {
        this.clientId = guardrails.getClientId();
        this.clientName = guardrails.getClientName();
        this.isSafeAtHome = guardrails.getIsSafeAtHome();
        this.isComparableDbOnly = guardrails.getIsComparableDbOnly();
        this.hasConfidentialLocation = guardrails.getHasConfidentialLocation();
        this.hasRestrictedData = guardrails.getHasRestrictedData();
        this.dataSystem = guardrails.getDataSystem();
        this.visibilityLevel = guardrails.getVisibilityLevel();
        this.lastUpdated = guardrails.getLastUpdated();
    }
    
    // Convert to domain object
    public ConfidentialityGuardrails toDomain() {
        ConfidentialityGuardrails guardrails = new ConfidentialityGuardrails();
        guardrails.setClientId(this.clientId);
        guardrails.setClientName(this.clientName);
        guardrails.setIsSafeAtHome(this.isSafeAtHome);
        guardrails.setIsComparableDbOnly(this.isComparableDbOnly);
        guardrails.setHasConfidentialLocation(this.hasConfidentialLocation);
        guardrails.setHasRestrictedData(this.hasRestrictedData);
        guardrails.setDataSystem(this.dataSystem);
        guardrails.setVisibilityLevel(this.visibilityLevel);
        guardrails.setLastUpdated(this.lastUpdated);
        return guardrails;
    }
    
    // Update from domain object
    public void updateFrom(ConfidentialityGuardrails guardrails) {
        this.clientName = guardrails.getClientName();
        this.isSafeAtHome = guardrails.getIsSafeAtHome();
        this.isComparableDbOnly = guardrails.getIsComparableDbOnly();
        this.hasConfidentialLocation = guardrails.getHasConfidentialLocation();
        this.hasRestrictedData = guardrails.getHasRestrictedData();
        this.dataSystem = guardrails.getDataSystem();
        this.visibilityLevel = guardrails.getVisibilityLevel();
        this.lastUpdated = guardrails.getLastUpdated();
    }
    
    // Getters and Setters
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public Boolean getIsSafeAtHome() { return isSafeAtHome; }
    public void setIsSafeAtHome(Boolean isSafeAtHome) { this.isSafeAtHome = isSafeAtHome; }
    
    public Boolean getIsComparableDbOnly() { return isComparableDbOnly; }
    public void setIsComparableDbOnly(Boolean isComparableDbOnly) { this.isComparableDbOnly = isComparableDbOnly; }
    
    public Boolean getHasConfidentialLocation() { return hasConfidentialLocation; }
    public void setHasConfidentialLocation(Boolean hasConfidentialLocation) { this.hasConfidentialLocation = hasConfidentialLocation; }
    
    public Boolean getHasRestrictedData() { return hasRestrictedData; }
    public void setHasRestrictedData(Boolean hasRestrictedData) { this.hasRestrictedData = hasRestrictedData; }
    
    public String getDataSystem() { return dataSystem; }
    public void setDataSystem(String dataSystem) { this.dataSystem = dataSystem; }
    
    public ConfidentialityGuardrails.VisibilityLevel getVisibilityLevel() { return visibilityLevel; }
    public void setVisibilityLevel(ConfidentialityGuardrails.VisibilityLevel visibilityLevel) { this.visibilityLevel = visibilityLevel; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}