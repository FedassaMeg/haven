package org.haven.api.projectlinkage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.haven.api.projectlinkage.dto.*;
import org.haven.programenrollment.application.services.ProjectLinkageService;
import org.haven.programenrollment.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("test")
class ProjectLinkageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectLinkageService linkageService;

    private UUID testThProjectId;
    private UUID testRrhProjectId;
    private UUID testLinkageId;
    private UUID testUserId;
    private ProjectLinkage testLinkage;

    @BeforeEach
    void setUp() {
        testThProjectId = UUID.randomUUID();
        testRrhProjectId = UUID.randomUUID();
        testLinkageId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        // Create a test linkage using the static factory method
        testLinkage = ProjectLinkage.create(
            testThProjectId,
            testRrhProjectId,
            "TH-2024-001",
            "RRH-2024-001",
            "Test TH Project",
            "Test RRH Project",
            LocalDate.now(),
            "Test linkage reason",
            "Test User",
            testUserId
        );
    }

    @Test
    @DisplayName("Create linkage - Success")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void createLinkage_Success() throws Exception {
        // Arrange
        CreateProjectLinkageRequest request = new CreateProjectLinkageRequest(
            testThProjectId,
            testRrhProjectId,
            "TH-2024-001",
            "RRH-2024-001",
            "Test TH Project",
            "Test RRH Project",
            LocalDate.now(),
            "Test linkage reason",
            "Test User",
            testUserId
        );

        when(linkageService.createLinkage(any())).thenReturn(ProjectLinkageId.of(testLinkageId));

        // Act & Assert
        mockMvc.perform(post("/api/v1/project-linkages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linkageId").value(testLinkageId.toString()))
                .andExpect(jsonPath("$.message").value("Project linkage created successfully"));
    }

    @Test
    @DisplayName("Create linkage - Validation Error")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void createLinkage_ValidationError() throws Exception {
        // Arrange - Invalid request with missing required fields
        CreateProjectLinkageRequest request = new CreateProjectLinkageRequest();

        // Act & Assert
        mockMvc.perform(post("/api/v1/project-linkages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Create linkage - Duplicate Linkage")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void createLinkage_DuplicateLinkage() throws Exception {
        // Arrange
        CreateProjectLinkageRequest request = new CreateProjectLinkageRequest(
            testThProjectId,
            testRrhProjectId,
            "TH-2024-001",
            "RRH-2024-001",
            "Test TH Project",
            "Test RRH Project",
            LocalDate.now(),
            "Test linkage reason",
            "Test User",
            testUserId
        );

        when(linkageService.createLinkage(any()))
            .thenThrow(new IllegalStateException("Active linkage already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/project-linkages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Active linkage already exists"));
    }

    @Test
    @DisplayName("Create linkage - Unauthorized")
    @WithMockUser(roles = {"CASE_MANAGER"})
    void createLinkage_Unauthorized() throws Exception {
        // Arrange
        CreateProjectLinkageRequest request = new CreateProjectLinkageRequest(
            testThProjectId,
            testRrhProjectId,
            "TH-2024-001",
            "RRH-2024-001",
            "Test TH Project",
            "Test RRH Project",
            LocalDate.now(),
            "Test linkage reason",
            "Test User",
            testUserId
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/project-linkages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpected(status().isForbidden());
    }

    @Test
    @DisplayName("Modify linkage - Success")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void modifyLinkage_Success() throws Exception {
        // Arrange
        ModifyProjectLinkageRequest request = new ModifyProjectLinkageRequest(
            "Updated linkage reason",
            "Updated notes"
        );

        // Act & Assert
        mockMvc.perform(put("/api/v1/project-linkages/{linkageId}", testLinkageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Linkage modified successfully"));
    }

    @Test
    @DisplayName("Revoke linkage - Success")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void revokeLinkage_Success() throws Exception {
        // Arrange
        RevokeLinkageRequest request = new RevokeLinkageRequest(
            LocalDate.now(),
            "No longer needed"
        );

        // Act & Assert
        mockMvc.perform(delete("/api/v1/project-linkages/{linkageId}", testLinkageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Linkage revoked successfully"));
    }

    @Test
    @DisplayName("Get active linkages for project - Success")
    @WithMockUser(roles = {"CASE_MANAGER"})
    void getActiveLinkagesForProject_Success() throws Exception {
        // Arrange
        when(linkageService.getActiveLinkagesForProject(testThProjectId))
            .thenReturn(List.of(testLinkage));

        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/project/{projectId}", testThProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].thProjectId").value(testThProjectId.toString()))
                .andExpect(jsonPath("$[0].rrhProjectId").value(testRrhProjectId.toString()));
    }

    @Test
    @DisplayName("Get linkage audit trail - Success")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void getLinkageAuditTrail_Success() throws Exception {
        // Arrange
        when(linkageService.getLinkageAuditTrail(testThProjectId))
            .thenReturn(List.of(testLinkage));

        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/project/{projectId}/audit-trail", testThProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].thProjectId").value(testThProjectId.toString()));
    }

    @Test
    @DisplayName("Check if projects can be linked - Success")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void canLinkProjects_Success() throws Exception {
        // Arrange
        when(linkageService.canLinkProjects(testThProjectId, testRrhProjectId))
            .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/can-link")
                .param("thProjectId", testThProjectId.toString())
                .param("rrhProjectId", testRrhProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canLink").value(true))
                .andExpect(jsonPath("$.message").value("Projects can be linked"));
    }

    @Test
    @DisplayName("Check if projects can be linked - Cannot Link")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void canLinkProjects_CannotLink() throws Exception {
        // Arrange
        when(linkageService.canLinkProjects(testThProjectId, testRrhProjectId))
            .thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/can-link")
                .param("thProjectId", testThProjectId.toString())
                .param("rrhProjectId", testRrhProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canLink").value(false))
                .andExpect(jsonPath("$.message").value("Projects cannot be linked"));
    }

    @Test
    @DisplayName("Get linkages requiring review - Success")
    @WithMockUser(roles = {"PROGRAM_MANAGER"})
    void getLinkagesRequiringReview_Success() throws Exception {
        // Arrange
        when(linkageService.getLinkagesRequiringReview())
            .thenReturn(List.of(testLinkage));

        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/requiring-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].thProjectId").value(testThProjectId.toString()));
    }

    @Test
    @DisplayName("Get dashboard overview - Success")
    @WithMockUser(roles = {"CASE_MANAGER"})
    void getDashboardOverview_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/dashboard/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get alerts summary widget - Success")
    @WithMockUser(roles = {"CASE_MANAGER"})
    void getAlertsSummaryWidget_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/dashboard/alerts-summary"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get linkage health widget - Success")
    @WithMockUser(roles = {"CASE_MANAGER"})
    void getLinkageHealthWidget_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/dashboard/linkage-health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get violation trends widget - Success")
    @WithMockUser(roles = {"CASE_MANAGER"})
    void getViolationTrendsWidget_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/project-linkages/dashboard/violation-trends"))
                .andExpect(status().isOk());
    }
}