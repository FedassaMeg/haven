package org.haven.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.haven.api.services.dto.CreateServiceEpisodeRequest;
import org.haven.shared.vo.services.ServiceDeliveryMode;
import org.haven.shared.vo.services.ServiceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ServiceEpisodeController
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class ServiceEpisodeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = {"CASE_MANAGER"})
    void testCreateServiceEpisode() throws Exception {
        // Given
        CreateServiceEpisodeRequest request = new CreateServiceEpisodeRequest(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            "PROG-001",
            "Test Program",
            ServiceType.CASE_MANAGEMENT,
            ServiceDeliveryMode.IN_PERSON,
            LocalDate.now(),
            60,
            "PROV-001",
            "Test Provider",
            "HUD-COC",
            "HUD Continuum of Care",
            "GRANT-2024-001",
            "Test service description",
            false
        );

        // When & Then
        mockMvc.perform(post("/api/v1/service-episodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.episodeId").exists());
    }

    @Test
    @WithMockUser(roles = {"CASE_MANAGER"})
    void testGetServiceTypes() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/service-episodes/service-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].value").exists())
                .andExpect(jsonPath("$[0].label").exists())
                .andExpect(jsonPath("$[0].category").exists());
    }

    @Test
    @WithMockUser(roles = {"CASE_MANAGER"})
    void testGetDeliveryModes() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/service-episodes/delivery-modes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].value").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    @WithMockUser(roles = {"CASE_MANAGER"})
    void testGetFundingSources() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/service-episodes/funding-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].funderId").exists())
                .andExpect(jsonPath("$[0].funderName").exists());
    }

    @Test
    @WithMockUser(roles = {"SUPERVISOR"})
    void testGetStatistics() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/service-episodes/statistics")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalServices").exists())
                .andExpect(jsonPath("$.completedServices").exists())
                .andExpect(jsonPath("$.pendingServices").exists());
    }

    @Test
    @WithMockUser(roles = {"CASE_MANAGER"})
    void testCreateCrisisInterventionService() throws Exception {
        // Given
        var request = Map.of(
            "clientId", UUID.randomUUID().toString(),
            "enrollmentId", UUID.randomUUID().toString(),
            "programId", "CRISIS-PROG-001",
            "providerId", "PROV-001",
            "providerName", "Crisis Counselor",
            "isConfidential", true
        );

        // When & Then
        mockMvc.perform(post("/api/v1/service-episodes/quick/crisis-intervention")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpected(status().isCreated())
                .andExpect(jsonPath("$.episodeId").exists());
    }

    @Test
    @WithMockUser(roles = {"CASE_MANAGER"})
    void testSearchServices() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/service-episodes/search")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31")
                .param("serviceType", "CASE_MANAGEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = {"CASE_MANAGER"})
    void testGetServicesRequiringFollowUp() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/service-episodes/follow-up"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$").isArray());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/service-episodes/statistics")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"READ_ONLY"})
    void testInsufficientPermissions() throws Exception {
        // Given
        CreateServiceEpisodeRequest request = new CreateServiceEpisodeRequest(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            "PROG-001",
            "Test Program",
            ServiceType.CASE_MANAGEMENT,
            ServiceDeliveryMode.IN_PERSON,
            LocalDate.now(),
            60,
            "PROV-001",
            "Test Provider",
            "HUD-COC",
            "HUD Continuum of Care",
            "GRANT-2024-001",
            "Test service description",
            false
        );

        // When & Then
        mockMvc.perform(post("/api/v1/service-episodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}