package org.haven.api.enrollments;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.application.services.ProgramEnrollmentAppService;
import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.shared.vo.hmis.HmisProjectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@DisplayName("Joint TH/RRH Integration Tests")
public class JointThRrhIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProgramEnrollmentAppService enrollmentAppService;

    @Autowired
    private ProgramEnrollmentRepository enrollmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    @DisplayName("Should successfully transition from TH to RRH enrollment")
    void shouldTransitionThToRrh() throws Exception {
        // Arrange
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        UUID clientId = UUID.randomUUID();
        UUID thProgramId = UUID.randomUUID();
        UUID rrhProgramId = UUID.randomUUID();
        
        // Create a TH enrollment
        ProgramEnrollment thEnrollment = ProgramEnrollment.create(
            new ClientId(clientId),
            thProgramId,
            LocalDate.now().minusDays(30),
            null, // relationshipToHead
            null, // residencePriorToEntry
            "intake-worker"
        );
        
        // Set project type to TH
        thEnrollment = ProgramEnrollment.createFromTransition(
            thEnrollment.getId(),
            new ClientId(clientId),
            thProgramId,
            null, // No predecessor
            LocalDate.now().minusDays(30),
            null, // No move-in date for TH
            UUID.randomUUID().toString(), // household ID
            null, // relationshipToHoH
            null, // priorLivingSituation
            null, // lengthOfStay
            null, // disablingCondition
            HmisProjectType.TRANSITIONAL_HOUSING
        );
        
        enrollmentRepository.save(thEnrollment);
        
        // Prepare transition request
        var transitionRequest = new EnrollmentController.TransitionToRrhRequest(
            rrhProgramId,
            LocalDate.now(),
            LocalDate.now().plusDays(7)
        );
        
        String requestJson = objectMapper.writeValueAsString(transitionRequest);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/enrollments/{thEnrollmentId}/transition-to-rrh", thEnrollment.getId().value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rrhEnrollmentId").exists())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.rrhProgramId").value(rrhProgramId.toString()))
                .andExpect(jsonPath("$.residentialMoveInDate").exists());
    }
    
    @Test
    @DisplayName("Should retrieve enrollment chain for linked enrollments")
    void shouldRetrieveEnrollmentChain() throws Exception {
        // Arrange
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        UUID clientId = UUID.randomUUID();
        UUID thProgramId = UUID.randomUUID();
        UUID rrhProgramId = UUID.randomUUID();
        
        // Create TH enrollment
        ProgramEnrollment thEnrollment = ProgramEnrollment.createFromTransition(
            ProgramEnrollmentId.generate(),
            new ClientId(clientId),
            thProgramId,
            null,
            LocalDate.now().minusDays(30),
            null,
            UUID.randomUUID().toString(),
            null, null, null, null,
            HmisProjectType.TRANSITIONAL_HOUSING
        );
        enrollmentRepository.save(thEnrollment);
        
        // Create RRH enrollment linked to TH
        ProgramEnrollment rrhEnrollment = ProgramEnrollment.createFromTransition(
            ProgramEnrollmentId.generate(),
            new ClientId(clientId),
            rrhProgramId,
            thEnrollment.getId().value(), // predecessor
            LocalDate.now(),
            LocalDate.now().plusDays(7),
            thEnrollment.getHouseholdId(), // same household ID
            null, null, null, null,
            HmisProjectType.RAPID_REHOUSING
        );
        enrollmentRepository.save(rrhEnrollment);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/enrollments/{enrollmentId}/chain", thEnrollment.getId().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(thEnrollment.getId().value().toString()))
                .andExpect(jsonPath("$[0].predecessorEnrollmentId").doesNotExist());
    }
    
    @Test
    @DisplayName("Should update residential move-in date for RRH enrollment")
    void shouldUpdateMoveInDate() throws Exception {
        // Arrange
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        UUID clientId = UUID.randomUUID();
        UUID rrhProgramId = UUID.randomUUID();
        
        ProgramEnrollment rrhEnrollment = ProgramEnrollment.createFromTransition(
            ProgramEnrollmentId.generate(),
            new ClientId(clientId),
            rrhProgramId,
            UUID.randomUUID(), // predecessor
            LocalDate.now(),
            null, // No move-in date initially
            UUID.randomUUID().toString(),
            null, null, null, null,
            HmisProjectType.RAPID_REHOUSING
        );
        enrollmentRepository.save(rrhEnrollment);
        
        LocalDate newMoveInDate = LocalDate.now().plusDays(14);
        var updateRequest = new EnrollmentController.UpdateMoveInDateRequest(newMoveInDate);
        String requestJson = objectMapper.writeValueAsString(updateRequest);
        
        // Act & Assert
        mockMvc.perform(patch("/api/v1/enrollments/{enrollmentId}/move-in-date", rrhEnrollment.getId().value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNoContent());
        
        // Verify the update
        var updatedEnrollment = enrollmentRepository.findById(rrhEnrollment.getId());
        assertTrue(updatedEnrollment.isPresent());
        assertEquals(newMoveInDate, updatedEnrollment.get().getResidentialMoveInDate());
    }
    
    @Test
    @DisplayName("Should validate TH to RRH transition business rules")
    void shouldValidateTransitionBusinessRules() {
        // Arrange
        UUID clientId = UUID.randomUUID();
        UUID thProgramId = UUID.randomUUID();
        UUID rrhProgramId = UUID.randomUUID();
        
        // Create an exited TH enrollment (should fail transition)
        ProgramEnrollment exitedThEnrollment = ProgramEnrollment.createFromTransition(
            ProgramEnrollmentId.generate(),
            new ClientId(clientId),
            thProgramId,
            null,
            LocalDate.now().minusDays(60),
            null,
            UUID.randomUUID().toString(),
            null, null, null, null,
            HmisProjectType.TRANSITIONAL_HOUSING
        );
        
        // Exit the enrollment
        exitedThEnrollment.exitProgram(
            LocalDate.now().minusDays(1),
            null, // exitReason
            null, // destination
            "case-manager"
        );
        
        enrollmentRepository.save(exitedThEnrollment);
        
        // Attempt transition
        var transitionCommand = new ProgramEnrollmentAppService.TransitionToRrhCommand(
            exitedThEnrollment.getId().value(),
            rrhProgramId,
            LocalDate.now(),
            LocalDate.now().plusDays(7)
        );
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            enrollmentAppService.transitionToRrh(transitionCommand);
        });
    }
    
    @Test
    @DisplayName("Should preserve household ID across TH to RRH transition")
    void shouldPreserveHouseholdIdInTransition() {
        // Arrange
        UUID clientId = UUID.randomUUID();
        UUID thProgramId = UUID.randomUUID();
        UUID rrhProgramId = UUID.randomUUID();
        String householdId = "HH-" + UUID.randomUUID().toString();
        
        ProgramEnrollment thEnrollment = ProgramEnrollment.createFromTransition(
            ProgramEnrollmentId.generate(),
            new ClientId(clientId),
            thProgramId,
            null,
            LocalDate.now().minusDays(30),
            null,
            householdId,
            null, null, null, null,
            HmisProjectType.TRANSITIONAL_HOUSING
        );
        enrollmentRepository.save(thEnrollment);
        
        // Act
        var transitionCommand = new ProgramEnrollmentAppService.TransitionToRrhCommand(
            thEnrollment.getId().value(),
            rrhProgramId,
            LocalDate.now(),
            LocalDate.now().plusDays(7)
        );
        
        var result = enrollmentAppService.transitionToRrh(transitionCommand);
        
        // Assert
        assertEquals(householdId, result.householdId());
        
        // Verify RRH enrollment has the same household ID
        var rrhEnrollment = enrollmentRepository.findById(ProgramEnrollmentId.of(result.rrhEnrollmentId()));
        assertTrue(rrhEnrollment.isPresent());
        assertEquals(householdId, rrhEnrollment.get().getHouseholdId());
    }
}