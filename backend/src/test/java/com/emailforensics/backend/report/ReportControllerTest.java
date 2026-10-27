package com.emailforensics.backend.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

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
    void getEvidenceReport_unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/00000000-0000-0000-0000-000000000000/report"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCaseReport_unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/cases/00000000-0000-0000-0000-000000000000/report"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEvidenceReport_notFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/00000000-0000-0000-0000-000000000000/report")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}
