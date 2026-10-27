package com.emailforensics.backend.casefile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        String loginJson = "{\"username\":\"analyst\",\"password\":\"analyst123\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        int tokenStart = responseBody.indexOf("\"accessToken\":\"") + 15;
        int tokenEnd = responseBody.indexOf("\"", tokenStart);
        authToken = responseBody.substring(tokenStart, tokenEnd);
    }

    @Test
    void shouldCreateAndRetrieveCaseSuccessfully() throws Exception {
        String createCaseJson = "{\"title\":\"Phishing Campaign Involving Invoice Scams\",\"description\":\"Suspicious email attachments targeting finance department\"}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCaseJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseNumber").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Phishing Campaign Involving Invoice Scams"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdByUsername").value("analyst"))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        int idStart = responseBody.indexOf("\"id\":\"") + 6;
        int idEnd = responseBody.indexOf("\"", idStart);
        String caseId = responseBody.substring(idStart, idEnd);

        mockMvc.perform(get("/api/v1/cases/" + caseId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.title").value("Phishing Campaign Involving Invoice Scams"));
    }

    @Test
    void shouldRejectCaseCreationWithBlankTitle() throws Exception {
        String invalidCaseJson = "{\"title\":\"   \",\"description\":\"Missing valid title\"}";

        mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCaseJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldListCasesWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/cases?page=0&size=10")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void shouldUpdateCaseNotesAndStatus() throws Exception {
        String createCaseJson = "{\"title\":\"Urgent Wire Transfer Probe\",\"description\":\"Executive impersonation email\"}";
        MvcResult createResult = mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCaseJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        int idStart = responseBody.indexOf("\"id\":\"") + 6;
        int idEnd = responseBody.indexOf("\"", idStart);
        String caseId = responseBody.substring(idStart, idEnd);

        String patchJson = "{\"analystNotes\":\"Initial triage completed. Awaiting full headers.\",\"status\":\"IN_REVIEW\"}";
        mockMvc.perform(patch("/api/v1/cases/" + caseId + "/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analystNotes").value("Initial triage completed. Awaiting full headers."))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test
    void shouldReturnNotFoundForNonexistentCaseId() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/cases/" + randomId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectUnauthenticatedCaseRequests() throws Exception {
        mockMvc.perform(get("/api/v1/cases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
