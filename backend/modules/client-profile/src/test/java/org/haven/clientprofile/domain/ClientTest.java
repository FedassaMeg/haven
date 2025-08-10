package org.haven.clientprofile.domain;

import org.haven.shared.vo.HumanName;
import org.haven.shared.vo.Address;
import org.haven.shared.vo.ContactPoint;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void shouldCreateClientWithBasicInformation() {
        // Arrange
        var name = new HumanName(
            HumanName.NameUse.OFFICIAL,
            "Smith",
            List.of("John"),
            List.of(),
            List.of(),
            null
        );
        var gender = Client.AdministrativeGender.MALE;
        var birthDate = LocalDate.of(1990, 1, 15);

        // Act
        var client = Client.create(name, gender, birthDate);

        // Assert
        assertNotNull(client.getId());
        assertEquals("John Smith", client.getPrimaryName().getFullName());
        assertEquals(gender, client.getGender());
        assertEquals(birthDate, client.getBirthDate());
        assertEquals(Client.ClientStatus.ACTIVE, client.getStatus());
        assertFalse(client.isDeceased());
        assertTrue(client.isActive());
        assertEquals(1, client.getPendingEvents().size());
    }

    @Test
    void shouldUpdateDemographics() {
        // Arrange
        var originalName = new HumanName(
            HumanName.NameUse.OFFICIAL, "Smith", List.of("John"), List.of(), List.of(), null
        );
        var client = Client.create(originalName, Client.AdministrativeGender.MALE, LocalDate.of(1990, 1, 15));
        client.clearPendingEvents();

        var updatedName = new HumanName(
            HumanName.NameUse.OFFICIAL, "Johnson", List.of("John", "Michael"), List.of(), List.of(), null
        );

        // Act
        client.updateDemographics(updatedName, Client.AdministrativeGender.MALE, LocalDate.of(1990, 1, 15));

        // Assert
        assertEquals("John Michael Johnson", client.getPrimaryName().getFullName());
        assertEquals(1, client.getPendingEvents().size());
    }

    @Test
    void shouldAddAddress() {
        // Arrange
        var name = new HumanName(
            HumanName.NameUse.OFFICIAL, "Smith", List.of("John"), List.of(), List.of(), null
        );
        var client = Client.create(name, Client.AdministrativeGender.MALE, LocalDate.of(1990, 1, 15));
        client.clearPendingEvents();

        var address = new Address(
            "123 Main St", null, "Anytown", "CA", "90210", "USA",
            Address.AddressType.PHYSICAL, Address.AddressUse.HOME
        );

        // Act
        client.addAddress(address);

        // Assert
        assertEquals(1, client.getAddresses().size());
        assertEquals("123 Main St", client.getAddresses().get(0).line1());
        assertEquals(1, client.getPendingEvents().size());
    }

    @Test
    void shouldAddTelecom() {
        // Arrange
        var name = new HumanName(
            HumanName.NameUse.OFFICIAL, "Smith", List.of("John"), List.of(), List.of(), null
        );
        var client = Client.create(name, Client.AdministrativeGender.MALE, LocalDate.of(1990, 1, 15));
        client.clearPendingEvents();

        var phone = new ContactPoint(
            ContactPoint.ContactSystem.PHONE, "555-1234", ContactPoint.ContactUse.HOME, 1
        );

        // Act
        client.addTelecom(phone);

        // Assert
        assertEquals(1, client.getTelecoms().size());
        assertEquals("555-1234", client.getTelecoms().get(0).value());
        assertEquals(1, client.getPendingEvents().size());
    }

    @Test
    void shouldMarkDeceased() {
        // Arrange
        var name = new HumanName(
            HumanName.NameUse.OFFICIAL, "Smith", List.of("John"), List.of(), List.of(), null
        );
        var client = Client.create(name, Client.AdministrativeGender.MALE, LocalDate.of(1990, 1, 15));
        client.clearPendingEvents();

        // Act
        client.markDeceased(java.time.Instant.now());

        // Assert
        assertTrue(client.isDeceased());
        assertEquals(Client.ClientStatus.INACTIVE, client.getStatus());
        assertFalse(client.isActive());
        assertEquals(1, client.getPendingEvents().size());
    }
}