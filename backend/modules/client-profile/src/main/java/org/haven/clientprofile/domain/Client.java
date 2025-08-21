package org.haven.clientprofile.domain;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.clientprofile.domain.events.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        apply(new HouseholdMemberAdded(id.value(), member.getId().value(), member.getRelationship(), Instant.now()));
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
}
