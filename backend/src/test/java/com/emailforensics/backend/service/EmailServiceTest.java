package com.emailforensics.backend.service;

import com.emailforensics.backend.dto.IndicatorDto;
import com.emailforensics.backend.dto.ParsedHeaderResult;
import com.emailforensics.backend.dto.UploadResponse;
import com.emailforensics.backend.entity.Email;
import com.emailforensics.backend.entity.Indicator;
import com.emailforensics.backend.exception.InvalidEmailException;
import com.emailforensics.backend.repository.EmailRepository;
import com.emailforensics.backend.repository.IndicatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private EmailStorageService emailStorageService;

    @Mock
    private EmailParsingService emailParsingService;

    @Mock
    private MailAuthService mailAuthService;

    @Mock
    private IndicatorExtractionService indicatorExtractionService;

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private ThreatIntelligenceService threatIntelligenceService;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(
                emailRepository,
                emailStorageService,
                emailParsingService,
                mailAuthService,
                indicatorExtractionService,
                indicatorRepository,
                threatIntelligenceService
        );
    }

    @Test
    void shouldUploadAndPreserveValidEml() {
        byte[] content = "From: alice@test.com\r\nSubject: Hi\r\n\r\nHello".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "sample.eml", "message/rfc822", content);

        when(emailStorageService.computeSha256(content)).thenReturn("abc123hash");
        when(emailStorageService.saveOriginalEmail(content)).thenReturn("1234/original.eml");
        when(emailStorageService.sanitizeFileName("sample.eml")).thenReturn("sample.eml");
        when(emailStorageService.getRootPath()).thenReturn(Paths.get("storage/emails"));

        ParsedHeaderResult headerResult = new ParsedHeaderResult();
        headerResult.setSender("alice@test.com");
        headerResult.setSubject("Hi");
        when(emailParsingService.parseHeaders(any(InputStream.class))).thenReturn(headerResult);

        when(mailAuthService.authenticateEmail(any(Path.class))).thenReturn("{\"spf\":{\"status\":\"PASS\"}}");
        when(indicatorExtractionService.extractIndicators(any(), any())).thenReturn(Collections.emptyList());

        Email savedEmail = new Email();
        savedEmail.setId(101L);
        savedEmail.setFileName("sample.eml");
        savedEmail.setFilePath("1234/original.eml");
        savedEmail.setSha256("abc123hash");
        savedEmail.setStatus("RECEIVED");
        savedEmail.setStage("THREAT_INTEL_CHECKED");

        when(emailRepository.save(any(Email.class))).thenReturn(savedEmail);

        UploadResponse response = emailService.processUpload(file);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("sample.eml", response.getFileName());
        assertEquals("abc123hash", response.getSha256());
        assertEquals("RECEIVED", response.getStatus());

        verify(emailRepository, org.mockito.Mockito.atLeastOnce()).save(any(Email.class));
    }

    @Test
    void shouldCheckThreatIntelligence() {
        IndicatorDto dto = new IndicatorDto(1L, 101L, "URL", "http://evil.com", "http://evil.com", "HTML", "MALICIOUS", "{}", Instant.now());
        when(threatIntelligenceService.checkEmailIndicators(101L)).thenReturn(List.of(dto));

        List<IndicatorDto> results = emailService.checkThreatIntelligence(101L);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("MALICIOUS", results.get(0).getVerdict());
    }

    @Test
    void shouldGetIndicatorsByEmailId() {
        Indicator ind = new Indicator(101L, "URL", "https://example.com", "https://example.com", "HTML");
        ind.setId(55L);
        when(indicatorRepository.findByEmailId(101L)).thenReturn(List.of(ind));

        List<IndicatorDto> results = emailService.getIndicatorsByEmailId(101L);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("URL", results.get(0).getType());
        assertEquals("https://example.com", results.get(0).getNormalizedValue());
    }

    @Test
    void shouldRejectNonEmlFile() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[]{1, 2, 3});
        assertThrows(InvalidEmailException.class, () -> emailService.processUpload(file));
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.eml", "message/rfc822", new byte[0]);
        assertThrows(InvalidEmailException.class, () -> emailService.processUpload(file));
    }

    @Test
    void shouldRejectOversizedFile() {
        byte[] oversized = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "large.eml", "message/rfc822", oversized);
        assertThrows(InvalidEmailException.class, () -> emailService.processUpload(file));
    }
}
