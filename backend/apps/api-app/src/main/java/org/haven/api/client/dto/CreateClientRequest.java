package org.haven.api.client.dto;

import org.haven.clientprofile.domain.Client.AdministrativeGender;
import org.haven.shared.vo.HumanName;
import org.haven.shared.vo.Address;
import org.haven.shared.vo.ContactPoint;
import org.haven.shared.vo.CodeableConcept;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * FHIR-compliant request DTO for creating a new client
 * Maps frontend FHIR structure to internal command
 */
public record CreateClientRequest(
    @Valid
    @NotNull(message = "Name is required")
    HumanNameDto name,
    
    @NotNull(message = "Gender is required")
    AdministrativeGender gender,
    
    @JsonProperty("birthDate")
    LocalDate birthDate,
    
    @JsonProperty("maritalStatus")
    CodeableConceptDto maritalStatus,
    
    List<@Valid AddressDto> addresses,
    
    List<@Valid ContactPointDto> telecoms,
    
    List<@Valid ContactDto> contact,
    
    List<@Valid CommunicationDto> communication,
    
    String status
) {

    /**
     * FHIR HumanName DTO
     */
    public static record HumanNameDto(
        @JsonProperty("use")
        String use,
        
        @NotNull(message = "Family name is required")
        String family,
        
        @NotNull(message = "Given name is required")
        List<String> given,
        
        List<String> prefix,
        List<String> suffix,
        String text
    ) {
        public HumanName toValueObject() {
            HumanName.NameUse nameUse = use != null ? 
                HumanName.NameUse.valueOf(use.toUpperCase()) : 
                HumanName.NameUse.OFFICIAL;
            
            return new HumanName(nameUse, family, given, prefix, suffix, text);
        }
    }

    /**
     * FHIR Address DTO
     */
    public static record AddressDto(
        String use,
        String type,
        String text,
        List<String> line,
        String city,
        String district,
        String state,
        @JsonProperty("postalCode")
        String postalCode,
        String country
    ) {
        public Address toValueObject() {
            Address.AddressUse addressUse = use != null ? 
                Address.AddressUse.valueOf(use.toUpperCase()) : 
                Address.AddressUse.HOME;
            
            Address.AddressType addressType = type != null ? 
                Address.AddressType.valueOf(type.toUpperCase()) : 
                Address.AddressType.BOTH;
            
            String line1 = (line != null && !line.isEmpty()) ? line.get(0) : "";
            String line2 = (line != null && line.size() > 1) ? line.get(1) : null;
                
            return new Address(line1, line2, city, state, postalCode, 
                             country != null ? country : "US", addressType, addressUse);
        }
    }

    /**
     * FHIR ContactPoint DTO
     */
    public static record ContactPointDto(
        @NotNull(message = "Contact system is required")
        String system,
        
        @NotNull(message = "Contact value is required")
        String value,
        
        String use,
        Integer rank,
        String period
    ) {
        public ContactPoint toValueObject() {
            ContactPoint.ContactSystem contactSystem = 
                ContactPoint.ContactSystem.valueOf(system.toUpperCase());
            
            ContactPoint.ContactUse contactUse = use != null ? 
                ContactPoint.ContactUse.valueOf(use.toUpperCase()) : 
                ContactPoint.ContactUse.HOME;
                
            return new ContactPoint(contactSystem, value, contactUse, rank);
        }
    }

    /**
     * FHIR Contact (emergency contact) DTO
     */
    public static record ContactDto(
        List<CodeableConceptDto> relationship,
        HumanNameDto name,
        List<ContactPointDto> telecom,
        AddressDto address,
        String gender,
        String organization,
        String period
    ) {}

    /**
     * FHIR Communication DTO
     */
    public static record CommunicationDto(
        CodeableConceptDto language,
        boolean preferred
    ) {}

    /**
     * FHIR CodeableConcept DTO
     */
    public static record CodeableConceptDto(
        String text,
        List<CodingDto> coding
    ) {
        public CodeableConcept toValueObject() {
            return new CodeableConcept(List.of(), text);
        }
    }

    /**
     * FHIR Coding DTO
     */
    public static record CodingDto(
        String system,
        String version,
        String code,
        String display,
        boolean userSelected
    ) {}
}