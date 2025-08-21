package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "clients", schema = "haven")
public class JpaClientEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Version
    private Long version;
    
    // Confidentiality & Privacy fields
    @Column(name = "alias_name", length = 200)
    private String aliasName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "data_system")
    private DataSystem dataSystem;
    
    @Column(name = "hmis_client_key", length = 100)
    private String hmisClientKey;
    
    @Column(name = "safe_at_home_participant")
    private Boolean safeAtHomeParticipant = false;
    
    @Column(name = "is_confidential_location")
    private Boolean isConfidentialLocation = false;
    
    @Column(name = "substitute_address_line1", length = 255)
    private String substituteAddressLine1;
    
    @Column(name = "substitute_address_line2", length = 255)
    private String substituteAddressLine2;
    
    @Column(name = "substitute_city", length = 100)
    private String substituteCity;
    
    @Column(name = "substitute_state", length = 50)
    private String substituteState;
    
    @Column(name = "substitute_postal_code", length = 20)
    private String substitutePostalCode;
    
    @Column(name = "substitute_country", length = 100)
    private String substituteCountry;
    
    @Column(name = "ok_to_text")
    private Boolean okToText = false;
    
    @Column(name = "ok_to_voicemail")
    private Boolean okToVoicemail = false;
    
    @Column(name = "contact_code_word", length = 100)
    private String contactCodeWord;
    
    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;
    
    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;
    
    // Constructors
    protected JpaClientEntity() {
        // JPA requires default constructor
    }
    
    public JpaClientEntity(UUID id, String firstName, String lastName, Instant createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
        this.dataSystem = DataSystem.COMPARABLE_DB; // Default for DV clients
    }
    
    // Factory methods
    public static JpaClientEntity fromDomain(Client client) {
        JpaClientEntity entity = new JpaClientEntity(
            client.getId().value(),
            client.getPrimaryName().getFirstName(),
            client.getPrimaryName().getLastName(),
            client.getCreatedAt()
        );
        
        // Map confidentiality fields
        entity.aliasName = client.getAliasName();
        entity.dataSystem = client.getDataSystem();
        entity.hmisClientKey = client.getHmisClientKey();
        entity.safeAtHomeParticipant = client.isSafeAtHomeParticipant();
        
        // Map contact safety preferences
        if (client.getContactSafetyPrefs() != null) {
            entity.okToText = client.getContactSafetyPrefs().okToText();
            entity.okToVoicemail = client.getContactSafetyPrefs().okToVoicemail();
            entity.contactCodeWord = client.getContactSafetyPrefs().codeWord();
            if (client.getContactSafetyPrefs().quietHours() != null) {
                entity.quietHoursStart = client.getContactSafetyPrefs().quietHours().startTime();
                entity.quietHoursEnd = client.getContactSafetyPrefs().quietHours().endTime();
            }
        }
        
        // Map address confidentiality
        if (client.getAddressConfidentiality() != null) {
            entity.isConfidentialLocation = client.getAddressConfidentiality().isConfidentialLocation();
            if (client.getAddressConfidentiality().mailingSubstituteAddress() != null) {
                var addr = client.getAddressConfidentiality().mailingSubstituteAddress();
                entity.substituteAddressLine1 = addr.line1();
                entity.substituteAddressLine2 = addr.line2();
                entity.substituteCity = addr.city();
                entity.substituteState = addr.state();
                entity.substitutePostalCode = addr.postalCode();
                entity.substituteCountry = addr.country();
            }
        }
        
        return entity;
    }
    
    public Client toDomain() {
        // For now, return a simple reconstruction
        // In a full implementation, you'd replay events from the event store
        Client client = Client.create(firstName, lastName);
        
        // Set confidentiality fields
        client.setAliasName(this.aliasName);
        client.setDataSystem(this.dataSystem);
        client.setHmisClientKey(this.hmisClientKey);
        
        // Reconstruct contact safety preferences
        if (this.okToText != null || this.okToVoicemail != null || this.contactCodeWord != null) {
            ContactSafetyPrefs.QuietHours quietHours = null;
            if (this.quietHoursStart != null && this.quietHoursEnd != null) {
                quietHours = new ContactSafetyPrefs.QuietHours(this.quietHoursStart, this.quietHoursEnd);
            }
            
            ContactSafetyPrefs prefs = new ContactSafetyPrefs(
                Boolean.TRUE.equals(this.okToText),
                Boolean.TRUE.equals(this.okToVoicemail),
                this.contactCodeWord,
                quietHours
            );
            client.updateContactSafetyPrefs(prefs);
        }
        
        return client;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    // Confidentiality & Privacy getters and setters
    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
    
    public DataSystem getDataSystem() { return dataSystem; }
    public void setDataSystem(DataSystem dataSystem) { this.dataSystem = dataSystem; }
    
    public String getHmisClientKey() { return hmisClientKey; }
    public void setHmisClientKey(String hmisClientKey) { this.hmisClientKey = hmisClientKey; }
    
    public Boolean getSafeAtHomeParticipant() { return safeAtHomeParticipant; }
    public void setSafeAtHomeParticipant(Boolean safeAtHomeParticipant) { this.safeAtHomeParticipant = safeAtHomeParticipant; }
    
    public Boolean getIsConfidentialLocation() { return isConfidentialLocation; }
    public void setIsConfidentialLocation(Boolean isConfidentialLocation) { this.isConfidentialLocation = isConfidentialLocation; }
    
    public String getSubstituteAddressLine1() { return substituteAddressLine1; }
    public void setSubstituteAddressLine1(String substituteAddressLine1) { this.substituteAddressLine1 = substituteAddressLine1; }
    
    public String getSubstituteAddressLine2() { return substituteAddressLine2; }
    public void setSubstituteAddressLine2(String substituteAddressLine2) { this.substituteAddressLine2 = substituteAddressLine2; }
    
    public String getSubstituteCity() { return substituteCity; }
    public void setSubstituteCity(String substituteCity) { this.substituteCity = substituteCity; }
    
    public String getSubstituteState() { return substituteState; }
    public void setSubstituteState(String substituteState) { this.substituteState = substituteState; }
    
    public String getSubstitutePostalCode() { return substitutePostalCode; }
    public void setSubstitutePostalCode(String substitutePostalCode) { this.substitutePostalCode = substitutePostalCode; }
    
    public String getSubstituteCountry() { return substituteCountry; }
    public void setSubstituteCountry(String substituteCountry) { this.substituteCountry = substituteCountry; }
    
    public Boolean getOkToText() { return okToText; }
    public void setOkToText(Boolean okToText) { this.okToText = okToText; }
    
    public Boolean getOkToVoicemail() { return okToVoicemail; }
    public void setOkToVoicemail(Boolean okToVoicemail) { this.okToVoicemail = okToVoicemail; }
    
    public String getContactCodeWord() { return contactCodeWord; }
    public void setContactCodeWord(String contactCodeWord) { this.contactCodeWord = contactCodeWord; }
    
    public LocalTime getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(LocalTime quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    
    public LocalTime getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(LocalTime quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
}
