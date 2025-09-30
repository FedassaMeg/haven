package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
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

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "gender", columnDefinition = "gender")
    private Client.AdministrativeGender gender = Client.AdministrativeGender.UNKNOWN;

    @Column(name = "date_of_birth")
    private LocalDate birthDate;
    
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

    // Basic contact/address fields stored on clients table
    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_primary", length = 20)
    private String phonePrimary;

    @Column(name = "phone_secondary", length = 20)
    private String phoneSecondary;
    
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

        // Core demographics
        entity.gender = client.getGender();
        entity.birthDate = client.getBirthDate();
        
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

        // Map first address to flat columns
        var addresses = client.getAddresses();
        if (addresses != null && !addresses.isEmpty()) {
            var addr = addresses.get(0);
            entity.addressLine1 = addr.line1();
            entity.addressLine2 = addr.line2();
            entity.city = addr.city();
            entity.state = addr.state();
            entity.postalCode = addr.postalCode();
            entity.country = addr.country();
        }

        // Map telecoms to flat columns
        var telecoms = client.getTelecoms();
        if (telecoms != null && !telecoms.isEmpty()) {
            // Primary/secondary phones and first email
            int phoneCount = 0;
            for (var t : telecoms) {
                if (t.system() == org.haven.shared.vo.ContactPoint.ContactSystem.PHONE) {
                    if (phoneCount == 0) {
                        entity.phonePrimary = t.value();
                        phoneCount++;
                    } else if (phoneCount == 1) {
                        entity.phoneSecondary = t.value();
                        phoneCount++;
                    }
                } else if (t.system() == org.haven.shared.vo.ContactPoint.ContactSystem.EMAIL && entity.email == null) {
                    entity.email = t.value();
                }
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
        // Use persisted gender and birthDate instead of defaults
        Client client = Client.create(
            new org.haven.shared.vo.HumanName(
                org.haven.shared.vo.HumanName.NameUse.OFFICIAL,
                this.lastName,
                java.util.List.of(this.firstName),
                java.util.List.of(),
                java.util.List.of(),
                null
            ),
            this.gender != null ? this.gender : Client.AdministrativeGender.UNKNOWN,
            this.birthDate
        );
        
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

        // Reconstruct primary address if present
        if (this.addressLine1 != null || this.city != null || this.state != null || this.postalCode != null) {
            String countryVal = (this.country != null && !this.country.isBlank()) ? this.country : "US";
            try {
                var addr = new org.haven.shared.vo.Address(
                    this.addressLine1 != null ? this.addressLine1 : "",
                    this.addressLine2,
                    this.city != null ? this.city : "",
                    this.state != null ? this.state : "",
                    this.postalCode != null ? this.postalCode : "",
                    countryVal,
                    org.haven.shared.vo.Address.AddressType.BOTH,
                    org.haven.shared.vo.Address.AddressUse.HOME
                );
                client.addAddress(addr);
            } catch (Exception ignored) {
                // Skip invalid address reconstruction to avoid breaking reads
            }
        }

        // Reconstruct telecoms if present
        if (this.phonePrimary != null && !this.phonePrimary.isBlank()) {
            client.addTelecom(new org.haven.shared.vo.ContactPoint(
                org.haven.shared.vo.ContactPoint.ContactSystem.PHONE,
                this.phonePrimary,
                org.haven.shared.vo.ContactPoint.ContactUse.HOME,
                1
            ));
        }
        if (this.phoneSecondary != null && !this.phoneSecondary.isBlank()) {
            client.addTelecom(new org.haven.shared.vo.ContactPoint(
                org.haven.shared.vo.ContactPoint.ContactSystem.PHONE,
                this.phoneSecondary,
                org.haven.shared.vo.ContactPoint.ContactUse.WORK,
                2
            ));
        }
        if (this.email != null && !this.email.isBlank()) {
            client.addTelecom(new org.haven.shared.vo.ContactPoint(
                org.haven.shared.vo.ContactPoint.ContactSystem.EMAIL,
                this.email,
                org.haven.shared.vo.ContactPoint.ContactUse.HOME,
                null
            ));
        }
        
        return client;
    }
    
    // (no-op helpers removed; using PostgreSQLEnumType binding)
    
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
