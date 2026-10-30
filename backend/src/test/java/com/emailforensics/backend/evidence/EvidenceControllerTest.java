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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private EmailEvidenceRepository evidenceRepository;

    private String authToken;
    private String caseId;

    @BeforeEach
    void setUp() throws Exception {
        String loginJson = "{\"username\":\"analyst\",\"password\":\"analyst123\"}";
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
        String testRunId = UUID.randomUUID().toString();
        byte[] emlBytes = ("From: alice@test.org\r\n" +
                "To: bob@test.org\r\n" +
                "Subject: Test Evidence Email\r\n" +
                "X-Test-Run-Id: " + testRunId + "\r\n\r\n" +
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
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.existingCaseId").value(caseId))
                .andExpect(jsonPath("$.existingCaseTitle").value("Forensic Case Evidence Test"))
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

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/visuals")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/content-analysis")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mlModelVersion").value("tribuo-logreg-tfidf-v1.0.0"))
                .andExpect(jsonPath("$.predictedLabel").isNotEmpty());
    }

    @Test
    void shouldDeduplicateIdenticalEmailInSameCaseAndReturnPreviousResults() throws Exception {
        String testRunId = UUID.randomUUID().toString();
        byte[] emlBytes = ("From: dedupe-test@domain.com\r\n" +
                "To: target@victim.org\r\n" +
                "Subject: Deduplication Test Email\r\n" +
                "X-Test-Run-Id: " + testRunId + "\r\n\r\n" +
                "This email content tests exact SHA-256 deduplication.").getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "dedupe-original.eml",
                "message/rfc822",
                emlBytes
        );

        // First upload: should create new evidence
        MvcResult firstUploadResult = mockMvc.perform(multipart("/api/v1/cases/" + caseId + "/evidence/upload")
                        .file(file1)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.emailId").isNotEmpty())
                .andExpect(jsonPath("$.sha256Hash").isNotEmpty())
                .andReturn();

        String body1 = firstUploadResult.getResponse().getContentAsString();
        int idStart = body1.indexOf("\"emailId\":\"") + 11;
        int idEnd = body1.indexOf("\"", idStart);
        String initialEmailId = body1.substring(idStart, idEnd);

        int hashStart = body1.indexOf("\"sha256Hash\":\"") + 14;
        int hashEnd = body1.indexOf("\"", hashStart);
        String initialHash = body1.substring(hashStart, hashEnd);

        // Second upload with identical bytes: should detect duplicate and return existing evidence
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "dedupe-duplicate-upload.eml",
                "message/rfc822",
                emlBytes
        );

        mockMvc.perform(multipart("/api/v1/cases/" + caseId + "/evidence/upload")
                        .file(file2)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.emailId").value(initialEmailId))
                .andExpect(jsonPath("$.sha256Hash").value(initialHash))
                .andExpect(jsonPath("$.existingCaseId").value(caseId))
                .andExpect(jsonPath("$.existingCaseTitle").value("Forensic Case Evidence Test"));

        // Verify only 1 record exists in database for this hash
        List<EmailEvidence> matching = evidenceRepository.findAllBySha256Hash(initialHash);
        assertThat(matching).hasSize(1);
        assertThat(matching.get(0).getId().toString()).isEqualTo(initialEmailId);
    }

    @Test
    void shouldDeduplicateIdenticalEmailAcrossDifferentCasesGlobally() throws Exception {
        String testRunId = UUID.randomUUID().toString();
        byte[] emlBytes = ("From: global-dedupe@domain.com\r\n" +
                "To: security@company.org\r\n" +
                "Subject: Cross-Case Global Deduplication Test\r\n" +
                "X-Test-Run-Id: " + testRunId + "\r\n\r\n" +
                "Unique content for cross-case deduplication testing.").getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "first-case-file.eml",
                "message/rfc822",
                emlBytes
        );

        // Upload in Case 1
        MvcResult firstUploadResult = mockMvc.perform(multipart("/api/v1/cases/" + caseId + "/evidence/upload")
                        .file(file1)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.duplicate").value(false))
                .andReturn();

        String body1 = firstUploadResult.getResponse().getContentAsString();
        int idStart = body1.indexOf("\"emailId\":\"") + 11;
        int idEnd = body1.indexOf("\"", idStart);
        String initialEmailId = body1.substring(idStart, idEnd);

        int hashStart = body1.indexOf("\"sha256Hash\":\"") + 14;
        int hashEnd = body1.indexOf("\"", hashStart);
        String initialHash = body1.substring(hashStart, hashEnd);

        // Create Case 2
        String createCase2Json = "{\"title\":\"Second Forensic Case\",\"description\":\"Testing cross case dedupe\"}";
        MvcResult case2Result = mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCase2Json))
                .andExpect(status().isCreated())
                .andReturn();

        String case2Body = case2Result.getResponse().getContentAsString();
        int case2IdStart = case2Body.indexOf("\"id\":\"") + 6;
        int case2IdEnd = case2Body.indexOf("\"", case2IdStart);
        String case2Id = case2Body.substring(case2IdStart, case2IdEnd);

        // Upload identical email in Case 2: should return Case 1's evidence and duplicate=true
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "second-case-same-file.eml",
                "message/rfc822",
                emlBytes
        );

        mockMvc.perform(multipart("/api/v1/cases/" + case2Id + "/evidence/upload")
                        .file(file2)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.emailId").value(initialEmailId))
                .andExpect(jsonPath("$.sha256Hash").value(initialHash))
                .andExpect(jsonPath("$.existingCaseId").value(caseId))
                .andExpect(jsonPath("$.existingCaseTitle").value("Forensic Case Evidence Test"));

        // Verify only 1 record exists in the database
        List<EmailEvidence> matching = evidenceRepository.findAllBySha256Hash(initialHash);
        assertThat(matching).hasSize(1);
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
