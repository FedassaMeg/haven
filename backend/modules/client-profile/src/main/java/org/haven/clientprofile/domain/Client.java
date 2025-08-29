package org.haven.clientprofile.domain;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.shared.vo.hmis.*;
import org.haven.clientprofile.domain.events.*;
import org.haven.clientprofile.domain.privacy.*;
import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.clientprofile.domain.privacy.UniversalDataElementPrivacyPolicy.DataAccessPurpose;
import org.haven.clientprofile.domain.privacy.RacePrivacyControl.RaceRedactionStrategy;
import org.haven.shared.vo.hmis.HmisEthnicity;
import org.haven.shared.vo.hmis.HmisEthnicity.EthnicityPrecision;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class Client extends AggregateRoot<ClientId> {
    private List<HumanName> names = new ArrayList<>();
    private AdministrativeGender gender;
    private LocalDate birthDate;
    private Boolean deceased;
    private List<Address> addresses = new ArrayList<>();
    private List<ContactPoint> telecoms = new ArrayList<>();
    private List<HouseholdMember> householdMembers = new ArrayList<>();
    private ClientStatus status;
    private Instant createdAt;
    private Period activePeriod;
    
    // Confidentiality & Privacy fields
    private String aliasName;
    private ContactSafetyPrefs contactSafetyPrefs;
    private AddressConfidentiality addressConfidentiality;
    private DataSystem dataSystem;
    private String hmisClientKey;
    private boolean safeAtHomeParticipant = false;
    
    // HMIS 2024 Comparable Database fields
    private HmisPersonalId hmisPersonalId;
    private Set<HmisRace> hmisRace = new HashSet<>();
    private HmisEthnicity hmisEthnicity = HmisEthnicity.DATA_NOT_COLLECTED;
    private Set<HmisGender> hmisGender = new HashSet<>();
    private VeteranStatus veteranStatus = VeteranStatus.DATA_NOT_COLLECTED;
    private DisablingCondition disablingCondition = DisablingCondition.DATA_NOT_COLLECTED;
    private String socialSecurityNumber;
    private Integer nameDataQuality;
    private Integer ssnDataQuality;
    private Integer dobDataQuality;
    
    // Privacy control settings
    private RaceRedactionStrategy defaultRaceStrategy = RaceRedactionStrategy.FULL_DISCLOSURE;
    private EthnicityPrecision defaultEthnicityPrecision = EthnicityPrecision.FULL;
    
    public static Client create(HumanName name, AdministrativeGender gender, LocalDate birthDate) {
        ClientId clientId = new ClientId(UUID.randomUUID());
        Client client = new Client();
        client.apply(new ClientCreated(clientId.value(), name, gender, birthDate, Instant.now()));
        return client;
    }
    
    public static Client create(String firstName, String lastName) {
        HumanName name = new HumanName(
            HumanName.NameUse.OFFICIAL,
            lastName,
            List.of(firstName),
            null,
            null,
            firstName + " " + lastName
        );
        return create(name, AdministrativeGender.UNKNOWN, LocalDate.of(1900, 1, 1));
    }

    public void updateDemographics(HumanName name, AdministrativeGender gender, LocalDate birthDate) {
        apply(new ClientDemographicsUpdated(id.value(), name, gender, birthDate, Instant.now()));
    }

    public void addAddress(Address address) {
        apply(new ClientAddressAdded(id.value(), address, Instant.now()));
    }

    public void addTelecom(ContactPoint telecom) {
        apply(new ClientTelecomAdded(id.value(), telecom, Instant.now()));
    }

    public void addHouseholdMember(HouseholdMember member) {
        // Legacy method - for backward compatibility
        // New household management should use HouseholdComposition aggregate
        apply(new HouseholdMemberAdded(
            id.value(), // compositionId (using client ID for backward compatibility)
            member.getId().value(), // membershipId
            id.value(), // memberId (self-reference for backward compatibility)
            member.getRelationship(),
            java.time.LocalDate.now(), // effectiveFrom
            null, // effectiveTo
            "system", // recordedBy
            "Legacy household member addition", // reason
            Instant.now()
        ));
    }

    public void updateStatus(ClientStatus newStatus) {
        if (this.status != newStatus) {
            apply(new ClientStatusChanged(id.value(), this.status, newStatus, Instant.now()));
        }
    }

    public void markDeceased(Instant deceasedDate) {
        if (!Boolean.TRUE.equals(this.deceased)) {
            apply(new ClientDeceasedMarked(id.value(), deceasedDate, Instant.now()));
        }
    }
    
    // Confidentiality & Privacy methods
    public void updateContactSafetyPrefs(ContactSafetyPrefs prefs) {
        apply(new ContactSafetyPrefsUpdated(id.value(), prefs, Instant.now()));
    }
    
    public void setConfidentialAddress(AddressConfidentiality addressConfidentiality) {
        apply(new ConfidentialAddressSet(id.value(), addressConfidentiality, Instant.now()));
    }
    
    public void enableSafeAtHome() {
        if (!this.safeAtHomeParticipant) {
            apply(new SafeAtHomeEnabled(id.value(), Instant.now()));
        }
    }
    
    public void disableSafeAtHome() {
        if (this.safeAtHomeParticipant) {
            apply(new SafeAtHomeDisabled(id.value(), Instant.now()));
        }
    }
    
    // Universal Data Element Privacy Methods
    
    /**
     * Updates the race data with privacy controls
     */
    public void updateRace(Set<HmisRace> races, RaceRedactionStrategy defaultStrategy) {
        this.hmisRace = races != null ? new HashSet<>(races) : new HashSet<>();
        this.defaultRaceStrategy = defaultStrategy != null ? defaultStrategy : RaceRedactionStrategy.FULL_DISCLOSURE;
    }
    
    /**
     * Updates the ethnicity data with privacy controls
     */
    public void updateEthnicity(HmisEthnicity ethnicity, EthnicityPrecision defaultPrecision) {
        this.hmisEthnicity = ethnicity != null ? ethnicity : HmisEthnicity.DATA_NOT_COLLECTED;
        this.defaultEthnicityPrecision = defaultPrecision != null ? defaultPrecision : EthnicityPrecision.FULL;
    }
    
    /**
     * Gets race data with privacy controls applied
     */
    public Set<HmisRace> getRaceWithPrivacy(PIIAccessContext context, DataAccessPurpose purpose) {
        UniversalDataElementPrivacyPolicy policy = new UniversalDataElementPrivacyPolicy();
        RaceRedactionStrategy strategy = policy.determineRaceStrategy(context, purpose, this.id.value());
        
        RacePrivacyControl control = new RacePrivacyControl.Builder()
            .withRaces(this.hmisRace)
            .withStrategy(strategy)
            .withClientId(this.id.value())
            .build();
            
        return control.getRedactedRaces();
    }
    
    /**
     * Gets ethnicity data with privacy controls applied
     */
    public HmisEthnicity getEthnicityWithPrivacy(PIIAccessContext context, DataAccessPurpose purpose) {
        UniversalDataElementPrivacyPolicy policy = new UniversalDataElementPrivacyPolicy();
        EthnicityPrecision precision = policy.determineEthnicityPrecision(context, purpose, this.id.value());
        
        EthnicityPrivacyControl control = new EthnicityPrivacyControl.Builder()
            .withEthnicity(this.hmisEthnicity)
            .withPrecision(precision)
            .withClientId(this.id.value())
            .build();
            
        return control.getRedactedEthnicity();
    }
    
    /**
     * Gets a privacy-controlled demographic projection for export/reporting
     */
    public Map<String, Object> getDemographicProjection(PIIAccessContext context, DataAccessPurpose purpose) {
        UniversalDataElementPrivacyPolicy policy = new UniversalDataElementPrivacyPolicy();
        
        Map<String, Object> projection = new HashMap<>();
        projection.put("clientId", this.id.value());
        
        // Check if demographics should be included
        if (!policy.shouldIncludeDemographics(context, purpose)) {
            projection.put("demographicsIncluded", false);
            return projection;
        }
        
        // Get privacy-controlled race data
        RaceRedactionStrategy raceStrategy = policy.determineRaceStrategy(context, purpose, this.id.value());
        RacePrivacyControl raceControl = new RacePrivacyControl.Builder()
            .withRaces(this.hmisRace)
            .withStrategy(raceStrategy)
            .withClientId(this.id.value())
            .build();
        
        // Get privacy-controlled ethnicity data
        EthnicityPrecision ethnicityPrecision = policy.determineEthnicityPrecision(context, purpose, this.id.value());
        EthnicityPrivacyControl ethnicityControl = new EthnicityPrivacyControl.Builder()
            .withEthnicity(this.hmisEthnicity)
            .withPrecision(ethnicityPrecision)
            .withClientId(this.id.value())
            .build();
        
        projection.put("demographicsIncluded", true);
        projection.put("race", raceControl.getReportingProjection());
        projection.put("ethnicity", ethnicityControl.getReportingProjection());
        projection.put("privacyNotice", policy.getPrivacyNotice(raceStrategy, ethnicityPrecision));
        
        return projection;
    }
    
    /**
     * Sets default privacy strategies for this client
     */
    public void setDefaultPrivacyStrategies(RaceRedactionStrategy raceStrategy, 
                                           EthnicityPrecision ethnicityPrecision) {
        this.defaultRaceStrategy = raceStrategy != null ? raceStrategy : RaceRedactionStrategy.FULL_DISCLOSURE;
        this.defaultEthnicityPrecision = ethnicityPrecision != null ? ethnicityPrecision : EthnicityPrecision.FULL;
    }

    @Override
    protected void when(DomainEvent e) {
        if (e instanceof ClientCreated ev) {
            this.id = new ClientId(ev.clientId());
            this.names = new ArrayList<>(List.of(ev.name()));
            this.gender = ev.gender();
            this.birthDate = ev.birthDate();
            this.status = ClientStatus.ACTIVE;
            this.deceased = false;
            this.createdAt = ev.occurredAt();
            this.activePeriod = new Period(ev.occurredAt(), null);
        } else if (e instanceof ClientDemographicsUpdated ev) {
            this.names = new ArrayList<>(List.of(ev.name()));
            this.gender = ev.gender();
            this.birthDate = ev.birthDate();
        } else if (e instanceof ClientAddressAdded ev) {
            this.addresses.add(ev.address());
        } else if (e instanceof ClientTelecomAdded ev) {
            this.telecoms.add(ev.telecom());
        } else if (e instanceof HouseholdMemberAdded ev) {
            var member = new HouseholdMember(
                new HouseholdMemberId(ev.memberId()),
                ev.relationship()
            );
            this.householdMembers.add(member);
        } else if (e instanceof ClientStatusChanged ev) {
            this.status = ev.newStatus();
        } else if (e instanceof ClientDeceasedMarked ev) {
            this.deceased = true;
            this.status = ClientStatus.INACTIVE;
            if (this.activePeriod != null && this.activePeriod.end() == null) {
                this.activePeriod = new Period(this.activePeriod.start(), ev.deceasedDate());
            }
        } else if (e instanceof ContactSafetyPrefsUpdated ev) {
            this.contactSafetyPrefs = ev.contactSafetyPrefs();
        } else if (e instanceof ConfidentialAddressSet ev) {
            this.addressConfidentiality = ev.addressConfidentiality();
        } else if (e instanceof SafeAtHomeEnabled ev) {
            this.safeAtHomeParticipant = true;
        } else if (e instanceof SafeAtHomeDisabled ev) {
            this.safeAtHomeParticipant = false;
        } else {
            throw new IllegalArgumentException("Unhandled event: " + e.getClass());
        }
    }

    public enum AdministrativeGender {
        MALE, FEMALE, OTHER, UNKNOWN
    }

    public enum ClientStatus {
        ACTIVE, INACTIVE, SUSPENDED, ENTERED_IN_ERROR
    }

    // Getters
    public List<HumanName> getNames() { return List.copyOf(names); }
    public HumanName getPrimaryName() { return names.isEmpty() ? null : names.get(0); }
    public AdministrativeGender getGender() { return gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public Boolean isDeceased() { return deceased; }
    public List<Address> getAddresses() { return List.copyOf(addresses); }
    public List<ContactPoint> getTelecoms() { return List.copyOf(telecoms); }
    public List<HouseholdMember> getHouseholdMembers() { return List.copyOf(householdMembers); }
    public ClientStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Period getActivePeriod() { return activePeriod; }
    
    public boolean isActive() {
        return status == ClientStatus.ACTIVE && (activePeriod == null || activePeriod.isActive());
    }
    
    // Confidentiality & Privacy getters
    public String getAliasName() { return aliasName; }
    public ContactSafetyPrefs getContactSafetyPrefs() { return contactSafetyPrefs; }
    public AddressConfidentiality getAddressConfidentiality() { return addressConfidentiality; }
    public DataSystem getDataSystem() { return dataSystem; }
    public String getHmisClientKey() { return hmisClientKey; }
    public boolean isSafeAtHomeParticipant() { return safeAtHomeParticipant; }
    
    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }
    
    public void setDataSystem(DataSystem dataSystem) {
        this.dataSystem = dataSystem;
    }
    
    public void setHmisClientKey(String hmisClientKey) {
        this.hmisClientKey = hmisClientKey;
    }
    
    public void updateAddressConfidentiality(AddressConfidentiality addressConfidentiality) {
        this.addressConfidentiality = addressConfidentiality;
    }
    
    // HMIS Comparable Database methods
    public void assignHmisPersonalId(HmisPersonalId hmisPersonalId) {
        this.hmisPersonalId = hmisPersonalId;
    }
    
    public void updateHmisRace(Set<HmisRace> race) {
        this.hmisRace = new HashSet<>(race);
    }
    
    public void updateHmisGender(Set<HmisGender> gender) {
        this.hmisGender = new HashSet<>(gender);
    }
    
    public void updateVeteranStatus(VeteranStatus veteranStatus) {
        this.veteranStatus = veteranStatus;
    }
    
    public void updateDisablingCondition(DisablingCondition disablingCondition) {
        this.disablingCondition = disablingCondition;
    }
    
    public void updateSocialSecurityNumber(String ssn) {
        this.socialSecurityNumber = ssn;
        this.ssnDataQuality = calculateSsnDataQuality(ssn);
    }
    
    private Integer calculateSsnDataQuality(String ssn) {
        if (ssn == null || ssn.trim().isEmpty()) {
            return 9; // Data not collected
        }
        String digitsOnly = ssn.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 9 && !digitsOnly.equals("000000000")) {
            return 1; // Full SSN reported
        }
        return 2; // Approximate or partial SSN reported
    }
    
    // HMIS getters
    public HmisPersonalId getHmisPersonalId() { 
        return hmisPersonalId != null ? hmisPersonalId : HmisPersonalId.fromClientId(id.value()); 
    }
    public Set<HmisRace> getHmisRace() { return Set.copyOf(hmisRace); }
    public Set<HmisGender> getHmisGender() { 
        if (hmisGender.isEmpty()) {
            // Map legacy gender to HMIS gender
            return Set.of(HmisGender.fromLegacyGender(gender.name()));
        }
        return Set.copyOf(hmisGender); 
    }
    public VeteranStatus getVeteranStatus() { return veteranStatus; }
    public DisablingCondition getDisablingCondition() { return disablingCondition; }
    public String getSocialSecurityNumber() { return socialSecurityNumber; }
    public Integer getNameDataQuality() { return nameDataQuality; }
    public Integer getSsnDataQuality() { return ssnDataQuality; }
    public Integer getDobDataQuality() { return dobDataQuality; }
    
    /**
     * Check if this client meets HMIS Comparable Database standards
     */
    public boolean isHmisCompliant() {
        return hmisPersonalId != null &&
               !hmisRace.isEmpty() &&
               !hmisGender.isEmpty() &&
               veteranStatus != null &&
               disablingCondition != null;
    }
    
    /**
     * Calculate overall HMIS data quality score
     */
    public Double getHmisDataQualityScore() {
        int validFields = 0;
        int totalFields = 3;
        
        if (nameDataQuality != null && nameDataQuality == 1) validFields++;
        if (ssnDataQuality != null && ssnDataQuality == 1) validFields++;
        if (dobDataQuality != null && dobDataQuality == 1) validFields++;
        
        return (double) validFields / totalFields * 100.0;
    }
}
