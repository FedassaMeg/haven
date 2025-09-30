package org.haven.servicedelivery.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.services.ServiceDeliveryMode;
import org.haven.shared.vo.services.ServiceType;
import org.haven.shared.vo.services.FundingSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ServiceEpisode domain entity
 */
public class ServiceEpisodeTest {

    @Test
    void testCreateServiceEpisode() {
        // Given
        ClientId clientId = new ClientId(UUID.randomUUID());
        String enrollmentId = UUID.randomUUID().toString();
        String programId = "PROG-001";
        String programName = "Crisis Response Program";
        ServiceType serviceType = ServiceType.CRISIS_INTERVENTION;
        ServiceDeliveryMode deliveryMode = ServiceDeliveryMode.IN_PERSON;
        LocalDate serviceDate = LocalDate.now();
        Integer plannedDuration = 60;
        String providerId = "PROV-001";
        String providerName = "Jane Smith";
        FundingSource fundingSource = FundingSource.vawa("VAWA-2024-001", "VAWA Grant 2024");
        String description = "Crisis intervention session";
        boolean isConfidential = true;
        String createdBy = "user-123";

        // When
        ServiceEpisode episode = ServiceEpisode.create(
            clientId, enrollmentId, programId, programName, serviceType, deliveryMode,
            serviceDate, plannedDuration, providerId, providerName, fundingSource,
            description, isConfidential, createdBy
        );

        // Then
        assertNotNull(episode.getId());
        assertEquals(clientId, episode.getClientId());
        assertEquals(enrollmentId, episode.getEnrollmentId());
        assertEquals(programId, episode.getProgramId());
        assertEquals(programName, episode.getProgramName());
        assertEquals(serviceType, episode.getServiceType());
        assertEquals(deliveryMode, episode.getDeliveryMode());
        assertEquals(serviceDate, episode.getServiceDate());
        assertEquals(plannedDuration, episode.getPlannedDurationMinutes());
        assertEquals(providerId, episode.getPrimaryProviderId());
        assertEquals(providerName, episode.getPrimaryProviderName());
        assertEquals(fundingSource, episode.getPrimaryFundingSource());
        assertEquals(description, episode.getServiceDescription());
        assertEquals(isConfidential, episode.isConfidential());
        assertEquals(createdBy, episode.getCreatedBy());
        assertEquals(ServiceEpisode.ServiceCompletionStatus.SCHEDULED, episode.getCompletionStatus());
        assertNotNull(episode.getCreatedAt());
    }

    @Test
    void testStartService() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        LocalDateTime startTime = LocalDateTime.now();
        String location = "Office Room A";

        // When
        episode.startService(startTime, location);

        // Then
        assertEquals(startTime, episode.getStartTime());
        assertEquals(location, episode.getServiceLocation());
        assertEquals(ServiceEpisode.ServiceCompletionStatus.IN_PROGRESS, episode.getCompletionStatus());
    }

    @Test
    void testCompleteService() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(45);
        episode.startService(startTime, "Office");

        // When
        episode.completeService(endTime, "Client made progress", ServiceEpisode.ServiceCompletionStatus.COMPLETED, "Good session");

        // Then
        assertEquals(endTime, episode.getEndTime());
        assertEquals("Client made progress", episode.getServiceOutcome());
        assertEquals(ServiceEpisode.ServiceCompletionStatus.COMPLETED, episode.getCompletionStatus());
        assertEquals("Good session", episode.getNotes());
        assertEquals(Integer.valueOf(45), episode.getActualDurationMinutes());
        assertNotNull(episode.getTotalBillableAmount());
    }

    @Test
    void testCannotStartServiceTwice() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        episode.startService(LocalDateTime.now(), "Office");

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            episode.startService(LocalDateTime.now(), "Another location");
        });
    }

    @Test
    void testCannotCompleteServiceWithoutStarting() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            episode.completeService(LocalDateTime.now(), "Outcome", ServiceEpisode.ServiceCompletionStatus.COMPLETED, "Notes");
        });
    }

    @Test
    void testAddProvider() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        String additionalProviderId = "PROV-002";
        String additionalProviderName = "John Doe";
        String role = "Supervisor";

        // When
        episode.addProvider(additionalProviderId, additionalProviderName, role);

        // Then
        assertTrue(episode.getAdditionalProviderIds().contains(additionalProviderId));
    }

    @Test
    void testAddFundingSource() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        FundingSource additionalFunding = FundingSource.hudCoc("HUD-COC-2024", "HUD Continuum of Care");
        double allocation = 25.0;

        // When
        episode.addFundingSource(additionalFunding, allocation);

        // Then
        assertTrue(episode.getAdditionalFundingSources().contains(additionalFunding));
    }

    @Test
    void testAddFundingSourceInvalidAllocation() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        FundingSource additionalFunding = FundingSource.hudCoc("HUD-COC-2024", "HUD Continuum of Care");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            episode.addFundingSource(additionalFunding, 0.0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            episode.addFundingSource(additionalFunding, 101.0);
        });
    }

    @Test
    void testUpdateOutcome() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        String outcome = "Client achieved housing stability";
        String followUpRequired = "Check-in in 30 days";
        LocalDate followUpDate = LocalDate.now().plusDays(30);

        // When
        episode.updateOutcome(outcome, followUpRequired, followUpDate);

        // Then
        assertEquals(outcome, episode.getServiceOutcome());
        assertEquals(followUpRequired, episode.getFollowUpRequired());
        assertEquals(followUpDate, episode.getFollowUpDate());
    }

    @Test
    void testMarkAsCourtOrdered() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        String courtOrderNumber = "CO-2024-001";

        // When
        episode.markAsCourtOrdered(courtOrderNumber);

        // Then
        assertTrue(episode.isCourtOrdered());
        assertEquals(courtOrderNumber, episode.getCourtOrderNumber());
    }

    @Test
    void testBusinessLogicMethods() {
        // Given
        ServiceEpisode episode = createTestServiceEpisode();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(45);

        // When
        episode.startService(startTime, "Office");
        episode.completeService(endTime, "Outcome", ServiceEpisode.ServiceCompletionStatus.COMPLETED, "Notes");
        episode.updateOutcome("Outcome", "Follow up needed", LocalDate.now().plusDays(1));

        // Then
        assertTrue(episode.isCompleted());
        assertFalse(episode.isInProgress());
        assertTrue(episode.requiresFollowUp());
        assertFalse(episode.isOverdue()); // Follow-up is tomorrow
        assertEquals(Double.valueOf(0.75), episode.getActualBillableHours()); // 45 minutes = 0.75 hours
    }

    private ServiceEpisode createTestServiceEpisode() {
        return ServiceEpisode.create(
            new ClientId(UUID.randomUUID()),
            UUID.randomUUID().toString(),
            "PROG-001",
            "Test Program",
            ServiceType.CASE_MANAGEMENT,
            ServiceDeliveryMode.IN_PERSON,
            LocalDate.now(),
            60,
            "PROV-001",
            "Test Provider",
            FundingSource.hudCoc("HUD-COC-2024", "HUD Continuum of Care"),
            "Test service description",
            false,
            "test-user"
        );
    }
}