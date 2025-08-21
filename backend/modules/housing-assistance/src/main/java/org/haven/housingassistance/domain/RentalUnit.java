package org.haven.housingassistance.domain;

import org.haven.shared.vo.Address;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a rental unit in housing assistance programs
 */
public class RentalUnit {
    private String unitId;
    private Address address;
    private String landlordId;
    private BigDecimal marketRent;
    private BigDecimal assistedRent;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal squareFootage;
    private UnitType unitType;
    private UnitStatus status;
    private List<InspectionRecord> inspections = new ArrayList<>();
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;
    private String propertyManagerContact;
    private Boolean isAccessible;
    private List<String> accessibilityFeatures = new ArrayList<>();
    
    public RentalUnit(String unitId, Address address, String landlordId, 
                     BigDecimal marketRent, UnitType unitType) {
        this.unitId = unitId;
        this.address = address;
        this.landlordId = landlordId;
        this.marketRent = marketRent;
        this.unitType = unitType;
        this.status = UnitStatus.AVAILABLE;
    }
    
    public void addInspection(InspectionRecord inspection) {
        this.inspections.add(inspection);
        updateStatusBasedOnInspections();
    }
    
    private void updateStatusBasedOnInspections() {
        // Find most recent inspection
        InspectionRecord latestInspection = inspections.stream()
            .max((i1, i2) -> i1.getInspectionDate().compareTo(i2.getInspectionDate()))
            .orElse(null);
            
        if (latestInspection != null) {
            if (latestInspection.getResult() == InspectionRecord.InspectionResult.PASS) {
                this.status = UnitStatus.INSPECTION_PASSED;
            } else if (latestInspection.getResult() == InspectionRecord.InspectionResult.FAIL) {
                this.status = UnitStatus.INSPECTION_FAILED;
            }
        }
    }
    
    public boolean isEligibleForAssistance() {
        return status == UnitStatus.INSPECTION_PASSED || status == UnitStatus.AVAILABLE;
    }
    
    public enum UnitType {
        APARTMENT,
        SINGLE_FAMILY_HOME,
        TOWNHOUSE,
        CONDOMINIUM,
        MOBILE_HOME,
        SHARED_HOUSING,
        SRO, // Single Room Occupancy
        OTHER
    }
    
    public enum UnitStatus {
        AVAILABLE,
        INSPECTION_PENDING,
        INSPECTION_PASSED,
        INSPECTION_FAILED,
        OCCUPIED,
        UNAVAILABLE,
        CONDEMNED
    }
    
    // Getters and setters
    public String getUnitId() { return unitId; }
    public Address getAddress() { return address; }
    public String getLandlordId() { return landlordId; }
    public BigDecimal getMarketRent() { return marketRent; }
    public void setMarketRent(BigDecimal marketRent) { this.marketRent = marketRent; }
    public BigDecimal getAssistedRent() { return assistedRent; }
    public void setAssistedRent(BigDecimal assistedRent) { this.assistedRent = assistedRent; }
    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }
    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }
    public BigDecimal getSquareFootage() { return squareFootage; }
    public void setSquareFootage(BigDecimal squareFootage) { this.squareFootage = squareFootage; }
    public UnitType getUnitType() { return unitType; }
    public UnitStatus getStatus() { return status; }
    public void setStatus(UnitStatus status) { this.status = status; }
    public List<InspectionRecord> getInspections() { return List.copyOf(inspections); }
    public LocalDate getLeaseStartDate() { return leaseStartDate; }
    public void setLeaseStartDate(LocalDate leaseStartDate) { this.leaseStartDate = leaseStartDate; }
    public LocalDate getLeaseEndDate() { return leaseEndDate; }
    public void setLeaseEndDate(LocalDate leaseEndDate) { this.leaseEndDate = leaseEndDate; }
    public String getPropertyManagerContact() { return propertyManagerContact; }
    public void setPropertyManagerContact(String propertyManagerContact) { this.propertyManagerContact = propertyManagerContact; }
    public Boolean getIsAccessible() { return isAccessible; }
    public void setIsAccessible(Boolean isAccessible) { this.isAccessible = isAccessible; }
    public List<String> getAccessibilityFeatures() { return List.copyOf(accessibilityFeatures); }
    public void setAccessibilityFeatures(List<String> accessibilityFeatures) { this.accessibilityFeatures = new ArrayList<>(accessibilityFeatures); }
}