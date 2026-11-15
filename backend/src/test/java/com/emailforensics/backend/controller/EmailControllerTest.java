package com.emailforensics.backend.controller;

import com.emailforensics.backend.dto.EmailDetailDto;
import com.emailforensics.backend.dto.IndicatorDto;
import com.emailforensics.backend.dto.UploadResponse;
import com.emailforensics.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    @Mock
    private EmailService emailService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EmailController controller = new EmailController(emailService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldAcceptAnalyzeUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.eml",
                "message/rfc822",
                "From: test@test.com".getBytes()
        );

        UploadResponse response = new UploadResponse(
                42L,
                "sample.eml",
                "hash123456",
                "RECEIVED",
                "SAVED",
                "Email evidence preserved and registered successfully"
        );

        when(emailService.processUpload(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/emails/analyze").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.fileName").value("sample.eml"))
                .andExpect(jsonPath("$.sha256").value("hash123456"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void shouldGetEmailById() throws Exception {
        EmailDetailDto dto = new EmailDetailDto();
        dto.setId(42L);
        dto.setFileName("sample.eml");
        dto.setSha256("hash123456");
        dto.setStatus("RECEIVED");
        dto.setStage("SAVED");
        dto.setCreatedAt(Instant.now());

        when(emailService.getEmailById(42L)).thenReturn(dto);

        mockMvc.perform(get("/api/emails/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.fileName").value("sample.eml"));
    }

    @Test
    void shouldListRecentEmails() throws Exception {
        EmailDetailDto dto = new EmailDetailDto();
        dto.setId(1L);
        dto.setFileName("evidence1.eml");
        dto.setSha256("hash1");
        dto.setStatus("RECEIVED");

        when(emailService.getRecentEmails()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/emails").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fileName").value("evidence1.eml"));
    }

    @Test
    void shouldGetIndicatorsByEmailId() throws Exception {
        IndicatorDto dto = new IndicatorDto(1L, 42L, "URL", "https://phish.example", "https://phish.example", "HTML", "UNKNOWN", "{}", null);
        when(emailService.getIndicatorsByEmailId(42L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/emails/42/indicators").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("URL"))
                .andExpect(jsonPath("$[0].origin").value("HTML"))
                .andExpect(jsonPath("$[0].normalizedValue").value("https://phish.example"));
    }

    @Test
    void shouldCheckThreatIntelligenceEndpoint() throws Exception {
        IndicatorDto dto = new IndicatorDto(1L, 42L, "URL", "https://phish.example", "https://phish.example", "HTML", "MALICIOUS", "{}", null);
        when(emailService.checkThreatIntelligence(42L)).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/emails/42/check-threats").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].verdict").value("MALICIOUS"));
    }
}
