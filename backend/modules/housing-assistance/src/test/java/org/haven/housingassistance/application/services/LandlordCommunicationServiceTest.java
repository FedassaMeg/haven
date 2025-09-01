package org.haven.housingassistance.application.services;

import org.haven.clientprofile.infrastructure.security.PIIRedactionService;
import org.haven.clientprofile.infrastructure.security.VSPDataAccessService;
import org.haven.housingassistance.application.services.ContactSafetyService;
import org.haven.housingassistance.application.services.LandlordCommunicationService.*;
import org.haven.housingassistance.domain.LandlordCommunication;
import org.haven.housingassistance.infrastructure.persistence.LandlordCommunicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LandlordCommunicationService consent and privacy enforcement
 */
@ExtendWith(MockitoExtension.class)
class LandlordCommunicationServiceTest {
    
    @Mock
    private LandlordCommunicationRepository communicationRepository;
    
    @Mock
    private PIIRedactionService piiRedactionService;
    
    @Mock
    private VSPDataAccessService vspDataAccessService;
    
    @Mock
    private ContactSafetyService contactSafetyService;
    
    @InjectMocks
    private LandlordCommunicationService service;
    
    private UUID clientId;
    private UUID landlordId;
    private UUID userId;
    private RecipientContact recipient;
    private Map<String, Object> requestedFields;
    
    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        landlordId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        recipient = new RecipientContact();
        recipient.setEmail("landlord@example.com");
        recipient.setPhone("555-1234");
        
        requestedFields = new HashMap<>();
        requestedFields.put("firstName", "John");
        requestedFields.put("lastName", "Doe");
        requestedFields.put("unitNumber", "101");
        requestedFields.put("monthlyRent", 1500);
    }
    
    @Test
    @DisplayName("Should successfully send communication when safety checks pass")
    void sendCommunication_WithValidSafety_Success() {
        // Given
        when(contactSafetyService.isChannelRestricted(any(), any())).thenReturn(false);
        when(piiRedactionService.createExportProjection(any(), any(), any()))
            .thenReturn(createMinimalProjection());
        when(communicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        LandlordCommunication result = service.sendCommunication(
            clientId, landlordId, Channel.EMAIL, "Test Subject", "Test Body",
            requestedFields, recipient, userId
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConsentChecked()).isTrue();
        assertThat(result.getSentStatus()).isEqualTo(SentStatus.SENT.name());
        verify(communicationRepository).save(any(LandlordCommunication.class));
        verify(vspDataAccessService).logVSPDataAccess(eq(userId), eq(clientId), anyString(), eq(true), anyString());
    }
    
    @Test
    @DisplayName("Should reject communication when safety preferences restrict channel")
    void sendCommunication_WithRestrictedChannel_ThrowsContactSafetyException() {
        // Given
        when(contactSafetyService.isChannelRestricted(clientId, "PHONE")).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> 
            service.sendCommunication(
                clientId, landlordId, Channel.PHONE, "Test", "Body",
                requestedFields, recipient, userId
            )
        )
        .isInstanceOf(ContactSafetyException.class)
        .hasMessageContaining("Channel PHONE is restricted");
    }
    
    @Test
    @DisplayName("Should allow communication when safety preference permits channel")
    void sendCommunication_WithAllowedChannel_Success() {
        // Given
        when(contactSafetyService.isChannelRestricted(clientId, "EMAIL")).thenReturn(false);
        when(piiRedactionService.createExportProjection(any(), any(), any()))
            .thenReturn(createMinimalProjection());
        when(communicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        LandlordCommunication result = service.sendCommunication(
            clientId, landlordId, Channel.EMAIL, "Test", "Body",
            requestedFields, recipient, userId
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSentStatus()).isEqualTo(SentStatus.SENT.name());
    }
    
    @Test
    @DisplayName("Should apply PII redaction to shared fields")
    void sendCommunication_AppliesMinimumNecessaryRedaction() {
        // Given
        when(contactSafetyService.isChannelRestricted(any(), any())).thenReturn(false);
        
        Map<String, Object> fullProjection = new HashMap<>();
        fullProjection.put("firstName", "John");
        fullProjection.put("lastName", "Doe");
        fullProjection.put("unitNumber", "101");
        
        when(piiRedactionService.createExportProjection(any(), any(), any()))
            .thenReturn(fullProjection);
        when(communicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        LandlordCommunication result = service.sendCommunication(
            clientId, landlordId, Channel.EMAIL, "Test", "Body",
            requestedFields, recipient, userId
        );
        
        // Then
        assertThat(result.getSharedFields()).isNotNull();
        assertThat(result.getSharedFields()).containsKeys("firstName", "lastName", "unitNumber");
    }
    
    @Test
    @DisplayName("Should retrieve communications by client ID")
    void getCommunicationsByClient_ReturnsFilteredList() {
        // Given
        List<LandlordCommunication> expectedComms = createMockCommunications();
        when(communicationRepository.findByClientId(clientId)).thenReturn(expectedComms);
        
        // When
        List<LandlordCommunication> result = service.getCommunicationsByClient(clientId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedComms);
    }
    
    @Test
    @DisplayName("Should retrieve communications by landlord ID")
    void getCommunicationsByLandlord_ReturnsFilteredList() {
        // Given
        List<LandlordCommunication> expectedComms = createMockCommunications();
        when(communicationRepository.findByLandlordId(landlordId)).thenReturn(expectedComms);
        
        // When
        List<LandlordCommunication> result = service.getCommunicationsByLandlord(landlordId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedComms);
    }
    
    // Helper methods
    private Map<String, Object> createMinimalProjection() {
        Map<String, Object> projection = new HashMap<>();
        projection.put("firstName", "John");
        projection.put("lastName", "Doe");
        projection.put("unitNumber", "101");
        return projection;
    }
    
    private List<LandlordCommunication> createMockCommunications() {
        List<LandlordCommunication> comms = new ArrayList<>();
        
        LandlordCommunication comm1 = new LandlordCommunication();
        comm1.setId(UUID.randomUUID());
        comm1.setClientId(clientId);
        comm1.setLandlordId(landlordId);
        comm1.setChannel("EMAIL");
        comm1.setSentStatus("SENT");
        comms.add(comm1);
        
        LandlordCommunication comm2 = new LandlordCommunication();
        comm2.setId(UUID.randomUUID());
        comm2.setClientId(clientId);
        comm2.setLandlordId(landlordId);
        comm2.setChannel("PHONE");
        comm2.setSentStatus("SENT");
        comms.add(comm2);
        
        return comms;
    }
}