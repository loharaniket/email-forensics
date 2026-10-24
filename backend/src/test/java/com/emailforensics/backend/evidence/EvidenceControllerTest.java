package com.emailforensics.backend.evidence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String authToken;
    private String caseId;

    @BeforeEach
    void setUp() throws Exception {
        String loginJson = "{\"username\":\"analyst\",\"password\":\"Analyst@123\"}";
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        int tokenStart = loginBody.indexOf("\"accessToken\":\"") + 15;
        int tokenEnd = loginBody.indexOf("\"", tokenStart);
        authToken = loginBody.substring(tokenStart, tokenEnd);

        String createCaseJson = "{\"title\":\"Forensic Case Evidence Test\",\"description\":\"Testing upload and preservation\"}";
        MvcResult caseResult = mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCaseJson))
                .andExpect(status().isCreated())
                .andReturn();

        String caseBody = caseResult.getResponse().getContentAsString();
        int idStart = caseBody.indexOf("\"id\":\"") + 6;
        int idEnd = caseBody.indexOf("\"", idStart);
        caseId = caseBody.substring(idStart, idEnd);
    }

    @Test
    void shouldUploadEmlEvidenceSuccessfullyAndReturn202() throws Exception {
        byte[] emlBytes = ("From: alice@test.org\r\n" +
                "To: bob@test.org\r\n" +
                "Subject: Test Evidence Email\r\n\r\n" +
                "This is the raw content of evidence.").getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample-evidence.eml",
                "message/rfc822",
                emlBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/cases/" + caseId + "/evidence/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.emailId").isNotEmpty())
                .andExpect(jsonPath("$.analysisRunId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.statusUrl").isNotEmpty())
                .andExpect(jsonPath("$.sha256Hash").isNotEmpty())
                .andExpect(jsonPath("$.originalFilename").value("sample-evidence.eml"))
                .andExpect(jsonPath("$.fileSizeBytes").value(emlBytes.length))
                .andReturn();

        String uploadBody = uploadResult.getResponse().getContentAsString();
        int emailIdStart = uploadBody.indexOf("\"emailId\":\"") + 11;
        int emailIdEnd = uploadBody.indexOf("\"", emailIdStart);
        String emailId = uploadBody.substring(emailIdStart, emailIdEnd);

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/status")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value(emailId))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.currentStage").value("QUEUED"));

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/integrity")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value(emailId))
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.savedSha256").isNotEmpty())
                .andExpect(jsonPath("$.recalculatedSha256").isNotEmpty());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/overview")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value(emailId))
                .andExpect(jsonPath("$.originalFilename").value("sample-evidence.eml"))
                .andExpect(jsonPath("$.processingStatus").value("QUEUED"));

        mockMvc.perform(get("/api/v1/cases/" + caseId + "/evidence")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(emailId));

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/original")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"sample-evidence.eml\""))
                .andExpect(content().bytes(emlBytes));

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/investigation")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceId").value(emailId))
                .andExpect(jsonPath("$.parsedEmail.subject").value("Test Evidence Email"))
                .andExpect(jsonPath("$.parsedEmail.from").value("alice@test.org"))
                .andExpect(jsonPath("$.parsedEmail.extractedText").value("This is the raw content of evidence."));

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/attachments")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/headers")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAddress").value("alice@test.org"))
                .andExpect(jsonPath("$.fromDomain").value("test.org"))
                .andExpect(jsonPath("$.verificationNote").isNotEmpty());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/hops")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/urls")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/infrastructure")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldRejectUploadWithNonEmlExtension() throws Exception {
        byte[] exeBytes = "MZ...executable".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                exeBytes
        );

        mockMvc.perform(multipart("/api/v1/cases/" + caseId + "/evidence/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldRejectEmptyUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.eml",
                "message/rfc822",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/cases/" + caseId + "/evidence/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnNotFoundWhenUploadingToNonexistentCase() throws Exception {
        UUID nonExistentCaseId = UUID.randomUUID();
        byte[] emlBytes = "From: a@b.com\r\nSubject: Hi\r\n\r\nTest".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid.eml",
                "message/rfc822",
                emlBytes
        );

        mockMvc.perform(multipart("/api/v1/cases/" + nonExistentCaseId + "/evidence/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectUnauthenticatedEvidenceAccess() throws Exception {
        mockMvc.perform(get("/api/v1/cases/" + caseId + "/evidence"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
