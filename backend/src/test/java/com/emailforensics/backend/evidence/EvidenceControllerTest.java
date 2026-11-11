package com.emailforensics.backend.evidence;

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

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.emailId").isNotEmpty())
                .andExpect(jsonPath("$.analysisRunId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.statusUrl").isNotEmpty())
                .andExpect(jsonPath("$.sha256Hash").isNotEmpty())
                .andExpect(jsonPath("$.originalFilename").value("sample-evidence.eml"))
                .andExpect(jsonPath("$.fileSizeBytes").value(emlBytes.length))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andReturn();

        String uploadBody = uploadResult.getResponse().getContentAsString();
        int emailIdStart = uploadBody.indexOf("\"emailId\":\"") + 11;
        int emailIdEnd = uploadBody.indexOf("\"", emailIdStart);
        String emailId = uploadBody.substring(emailIdStart, emailIdEnd);

        // Await background forensic pipeline completion
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult res = mockMvc.perform(get("/api/v1/evidence/" + emailId + "/status"))
                    .andReturn();
            if (res.getResponse().getContentAsString().contains("\"status\":\"COMPLETED\"")) {
                break;
            }
            Thread.sleep(50);
        }

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value(emailId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/integrity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value(emailId))
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.savedSha256").isNotEmpty())
                .andExpect(jsonPath("$.recalculatedSha256").isNotEmpty());

        mockMvc.perform(get("/api/v1/evidence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/original"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"sample-evidence.eml\""))
                .andExpect(content().bytes(emlBytes));

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/investigation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceId").value(emailId))
                .andExpect(jsonPath("$.parsedEmail.subject").value("Test Evidence Email"))
                .andExpect(jsonPath("$.parsedEmail.from").value("alice@test.org"))
                .andExpect(jsonPath("$.parsedEmail.extractedText").value("This is the raw content of evidence."));

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/headers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAddress").value("alice@test.org"))
                .andExpect(jsonPath("$.fromDomain").value("test.org"))
                .andExpect(jsonPath("$.verificationNote").isNotEmpty());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/hops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/infrastructure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/visuals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/qr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/qr-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/content-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mlModelVersion").value("tribuo-logreg-tfidf-v1.0.0"))
                .andExpect(jsonPath("$.predictedLabel").isNotEmpty());
    }

    @Test
    void shouldDeduplicateIdenticalEmailAndReturnPreviousResults() throws Exception {
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
        MvcResult firstUploadResult = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(file1))
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

        mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(file2))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.emailId").value(initialEmailId))
                .andExpect(jsonPath("$.sha256Hash").value(initialHash));

        // Verify only 1 record exists in database for this hash
        List<EmailEvidence> matching = evidenceRepository.findAllBySha256Hash(initialHash);
        assertThat(matching).hasSize(1);
        assertThat(matching.get(0).getId().toString()).isEqualTo(initialEmailId);
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

        mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(file))
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

        mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldRetryAnalysisSuccessfully() throws Exception {
        String testRunId = UUID.randomUUID().toString();
        byte[] emlBytes = ("From: retry-test@victim.org\r\n" +
                "To: analyst@security.org\r\n" +
                "Subject: Retry Pipeline Analysis Test\r\n" +
                "X-Test-Run-Id: " + testRunId + "\r\n\r\n" +
                "Testing retry orchestration endpoint.").getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "retry-sample.eml",
                "message/rfc822",
                emlBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andReturn();

        String uploadBody = uploadResult.getResponse().getContentAsString();
        int emailIdStart = uploadBody.indexOf("\"emailId\":\"") + 11;
        int emailIdEnd = uploadBody.indexOf("\"", emailIdStart);
        String emailId = uploadBody.substring(emailIdStart, emailIdEnd);

        // Wait for initial run to complete
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult res = mockMvc.perform(get("/api/v1/evidence/" + emailId + "/status"))
                    .andReturn();
            if (res.getResponse().getContentAsString().contains("\"status\":\"COMPLETED\"")) {
                break;
            }
            Thread.sleep(50);
        }

        // Trigger retry
        mockMvc.perform(post("/api/v1/evidence/" + emailId + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value(emailId))
                .andExpect(jsonPath("$.attemptNumber").value(2));
    }

    @Test
    void shouldRetrieveImagesAndQrEndpoints() throws Exception {
        String testUrl = "https://phishing-gateway.security-check.test/login";
        com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix matrix = writer.encode(testUrl, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);
        java.io.ByteArrayOutputStream imgBaos = new java.io.ByteArrayOutputStream();
        com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(matrix, "PNG", imgBaos);
        String base64Img = java.util.Base64.getMimeEncoder().encodeToString(imgBaos.toByteArray());

        String eml = "From: security@alerts.test\r\n" +
                "To: user@test.org\r\n" +
                "Subject: Security Code\r\n" +
                "MIME-Version: 1.0\r\n" +
                "Content-Type: multipart/related; boundary=\"----=_Part_Boundary_Img\"\r\n\r\n" +
                "------=_Part_Boundary_Img\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n\r\n" +
                "<html><body><p>Scan QR code</p></body></html>\r\n" +
                "------=_Part_Boundary_Img\r\n" +
                "Content-Type: image/png\r\n" +
                "Content-Disposition: inline; filename=\"test_qr.png\"\r\n" +
                "Content-Transfer-Encoding: base64\r\n\r\n" +
                base64Img + "\r\n" +
                "------=_Part_Boundary_Img--\r\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-qr-email.eml",
                "message/rfc822",
                eml.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(file))
                .andExpect(status().isAccepted())
                .andReturn();

        String uploadBody = uploadResult.getResponse().getContentAsString();
        int emailIdStart = uploadBody.indexOf("\"emailId\":\"") + 11;
        int emailIdEnd = uploadBody.indexOf("\"", emailIdStart);
        String emailId = uploadBody.substring(emailIdStart, emailIdEnd);

        // Wait for pipeline completion
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult res = mockMvc.perform(get("/api/v1/evidence/" + emailId + "/status")).andReturn();
            if (res.getResponse().getContentAsString().contains("\"status\":\"COMPLETED\"")) {
                break;
            }
            Thread.sleep(50);
        }

        // Test GET /images
        MvcResult imagesResult = mockMvc.perform(get("/api/v1/evidence/" + emailId + "/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].filename").value("test_qr.png"))
                .andExpect(jsonPath("$[0].imageUrl").isNotEmpty())
                .andReturn();

        String imagesJson = imagesResult.getResponse().getContentAsString();
        int imageIdStart = imagesJson.indexOf("\"id\":\"") + 6;
        int imageIdEnd = imagesJson.indexOf("\"", imageIdStart);
        String imageId = imagesJson.substring(imageIdStart, imageIdEnd);

        // Test GET /qr
        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/qr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].decodedPayload").value(testUrl))
                .andExpect(jsonPath("$[0].imageId").value(imageId));

        // Test GET /images/{imageId}/content
        mockMvc.perform(get("/api/v1/evidence/" + emailId + "/images/" + imageId + "/content"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"test_qr.png\""));
    }
}
