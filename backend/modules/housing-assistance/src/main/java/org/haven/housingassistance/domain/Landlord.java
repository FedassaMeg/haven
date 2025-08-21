package org.haven.housingassistance.domain;

import org.haven.shared.vo.Address;
import org.haven.shared.vo.ContactPoint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Landlord entity for housing assistance programs
 */
public class Landlord {
    private String landlordId;
    private String businessName;
    private String contactPersonName;
    private String contactTitle;
    private List<ContactPoint> contactMethods = new ArrayList<>();
    private Address businessAddress;
    private String taxId;
    private LandlordStatus status;
    private Boolean isApproved;
    private LocalDate approvalDate;
    private String approvedBy;
    private List<String> certifications = new ArrayList<>();
    private List<LandlordNote> notes = new ArrayList<>();
    private PaymentPreference paymentPreference;
    private String bankingInformation;
    private Boolean acceptsHousingVouchers;
    private List<String> specializations = new ArrayList<>(); // Senior housing, accessible units, etc.
    private Instant createdAt;
    private Instant lastModified;
    
    public Landlord(String landlordId, String businessName, String contactPersonName) {
        this.landlordId = landlordId;
        this.businessName = businessName;
        this.contactPersonName = contactPersonName;
        this.status = LandlordStatus.PENDING_VERIFICATION;
        this.isApproved = false;
        this.acceptsHousingVouchers = false;
        this.createdAt = Instant.now();
        this.lastModified = Instant.now();
    }
    
    public void addContactMethod(ContactPoint contactMethod) {
        this.contactMethods.add(contactMethod);
        updateLastModified();
    }
    
    public void updateBusinessAddress(Address address) {
        this.businessAddress = address;
        updateLastModified();
    }
    
    public void approveLandlord(String approvedBy, List<String> certifications) {
        this.isApproved = true;
        this.approvalDate = LocalDate.now();
        this.approvedBy = approvedBy;
        this.certifications = new ArrayList<>(certifications);
        this.status = LandlordStatus.APPROVED;
        updateLastModified();
    }
    
    public void suspendLandlord(String reason, String suspendedBy) {
        this.status = LandlordStatus.SUSPENDED;
        addNote(new LandlordNote(
            "SUSPENSION",
            "Landlord suspended: " + reason,
            suspendedBy,
            Instant.now()
        ));
        updateLastModified();
    }
    
    public void addNote(LandlordNote note) {
        this.notes.add(note);
        updateLastModified();
    }
    
    public void setPaymentPreference(PaymentPreference preference, String bankingInfo) {
        this.paymentPreference = preference;
        this.bankingInformation = bankingInfo;
        updateLastModified();
    }
    
    private void updateLastModified() {
        this.lastModified = Instant.now();
    }
    
    public boolean canReceivePayments() {
        return isApproved && status == LandlordStatus.APPROVED && paymentPreference != null;
    }
    
    public enum LandlordStatus {
        PENDING_VERIFICATION,
        APPROVED,
        SUSPENDED,
        TERMINATED,
        INACTIVE
    }
    
    public enum PaymentPreference {
        DIRECT_DEPOSIT,
        CHECK,
        ELECTRONIC_TRANSFER,
        THIRD_PARTY_PROCESSOR
    }
    
    public static class LandlordNote {
        private String noteType;
        private String content;
        private String authorId;
        private Instant createdAt;
        
        public LandlordNote(String noteType, String content, String authorId, Instant createdAt) {
            this.noteType = noteType;
            this.content = content;
            this.authorId = authorId;
            this.createdAt = createdAt;
        }
        
        // Getters
        public String getNoteType() { return noteType; }
        public String getContent() { return content; }
        public String getAuthorId() { return authorId; }
        public Instant getCreatedAt() { return createdAt; }
    }
    
    // Getters and setters
    public String getLandlordId() { return landlordId; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; updateLastModified(); }
    public String getContactPersonName() { return contactPersonName; }
    public void setContactPersonName(String contactPersonName) { this.contactPersonName = contactPersonName; updateLastModified(); }
    public String getContactTitle() { return contactTitle; }
    public void setContactTitle(String contactTitle) { this.contactTitle = contactTitle; updateLastModified(); }
    public List<ContactPoint> getContactMethods() { return List.copyOf(contactMethods); }
    public Address getBusinessAddress() { return businessAddress; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; updateLastModified(); }
    public LandlordStatus getStatus() { return status; }
    public Boolean getIsApproved() { return isApproved; }
    public LocalDate getApprovalDate() { return approvalDate; }
    public String getApprovedBy() { return approvedBy; }
    public List<String> getCertifications() { return List.copyOf(certifications); }
    public List<LandlordNote> getNotes() { return List.copyOf(notes); }
    public PaymentPreference getPaymentPreference() { return paymentPreference; }
    public String getBankingInformation() { return bankingInformation; }
    public Boolean getAcceptsHousingVouchers() { return acceptsHousingVouchers; }
    public void setAcceptsHousingVouchers(Boolean acceptsHousingVouchers) { this.acceptsHousingVouchers = acceptsHousingVouchers; updateLastModified(); }
    public List<String> getSpecializations() { return List.copyOf(specializations); }
    public void setSpecializations(List<String> specializations) { this.specializations = new ArrayList<>(specializations); updateLastModified(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
}