package org.haven.api.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.haven.clientprofile.application.commands.CreateClientCmd;
import org.haven.clientprofile.application.services.ClientAppService;
import org.haven.clientprofile.domain.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb-clients",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@DisplayName("ClientController integration")
class ClientControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ClientAppService clientAppService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Client IDs returned from search should resolve via detail endpoint")
    void shouldResolveClientIdsFromSearchResults() throws Exception {
        // Arrange
        var clientId = clientAppService.handle(new CreateClientCmd(
            "Integration",
            "Tester",
            Client.AdministrativeGender.OTHER,
            LocalDate.of(1999, 1, 1)
        ));

        // Act
        MvcResult listResult = mockMvc.perform(get("/clients")
                .param("activeOnly", "true")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(clientId.value().toString()))
            .andReturn();

        List<Map<String, Object>> clients = objectMapper.readValue(
            listResult.getResponse().getContentAsString(),
            new TypeReference<>() {}
        );

        assertThat(clients).isNotEmpty();
        String idFromList = (String) clients.get(0).get("id");
        assertThat(idFromList).isEqualTo(clientId.value().toString());

        // Assert detail endpoint
        mockMvc.perform(get("/clients/{id}", UUID.fromString(idFromList)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(idFromList));
    }
}
