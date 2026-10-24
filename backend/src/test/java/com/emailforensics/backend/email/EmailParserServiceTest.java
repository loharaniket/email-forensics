package com.emailforensics.backend.email;

import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailParserServiceTest {

    private EmailParserService parserService;
    private final Path fixturesDir = Paths.get("..", "test-fixtures", "eml");

    @BeforeEach
    void setUp() {
        parserService = new EmailParserService();
    }

    @Test
    void shouldParsePlainLegitimateEmail() throws Exception {
        Path emlPath = fixturesDir.resolve("01-plain-legitimate.eml");
        try (InputStream in = Files.newInputStream(emlPath)) {
            ParsedEmailDto dto = parserService.parse(in);

            assertEquals("Scheduled Security Maintenance Notice", dto.getSubject());
            assertEquals("<maintenance-20261021-001@internal-corp.test>", dto.getMessageId());
            assertEquals("security-alert@internal-corp.test", dto.getFrom());
            assertTrue(dto.getTo().contains("analyst@emailforensics.local"));
            assertNotNull(dto.getSentDate());
            assertNotNull(dto.getPlainTextBody());
            assertTrue(dto.getPlainTextBody().contains("upcoming internal security maintenance"));
            assertTrue(dto.getExtractedText().contains("All systems are expected to remain operational"));
            assertTrue(dto.getAttachments().isEmpty());
        }
    }

    @Test
    void shouldParseHtmlAndExtractCleanText() throws Exception {
        Path emlPath = fixturesDir.resolve("02-html-anchor-mismatch.eml");
        try (InputStream in = Files.newInputStream(emlPath)) {
            ParsedEmailDto dto = parserService.parse(in);

            assertEquals("Urgent Action Required: Verify Account Access", dto.getSubject());
            assertEquals("<alert-20261022-9988@secure-bank.test>", dto.getMessageId());
            assertNotNull(dto.getHtmlBody());
            assertTrue(dto.getHtmlBody().contains("<h2>Urgent Banking Security Notification</h2>"));
            assertNotNull(dto.getExtractedText());
            assertTrue(dto.getExtractedText().contains("Dear customer"));
            assertFalse(dto.getExtractedText().contains("<h2>"));
            assertFalse(dto.getExtractedText().contains("</a>"));
        }
    }

    @Test
    void shouldParseMultipartAttachmentsAndIdentifyRiskyExtensions() throws Exception {
        Path emlPath = fixturesDir.resolve("03-multipart-attachment.eml");
        try (InputStream in = Files.newInputStream(emlPath)) {
            ParsedEmailDto dto = parserService.parse(in);

            assertEquals("Overdue Payment Invoice #INV-88910", dto.getSubject());
            assertEquals(2, dto.getAttachments().size());

            AttachmentMetaDto pdf = dto.getAttachments().stream()
                    .filter(a -> a.getFilename().equals("invoice_october.pdf"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(pdf);
            assertEquals("application/pdf", pdf.getMimeType());
            assertFalse(pdf.isRiskyExtension());
            assertEquals(64, pdf.getSha256Hash().length());

            AttachmentMetaDto exe = dto.getAttachments().stream()
                    .filter(a -> a.getFilename().equals("payment_voucher.exe"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(exe);
            assertTrue(exe.isRiskyExtension());
            assertEquals(64, exe.getSha256Hash().length());
        }
    }

    @Test
    void shouldDecodeUnicodeAndRfc2047Subject() throws Exception {
        Path emlPath = fixturesDir.resolve("04-unicode-subject.eml");
        try (InputStream in = Files.newInputStream(emlPath)) {
            ParsedEmailDto dto = parserService.parse(in);

            assertTrue(dto.getFrom().contains("Aniket Lohar"));
            assertTrue(dto.getSubject().contains("Urgent Security Alert"));
            assertNotNull(dto.getPlainTextBody());
            assertTrue(dto.getPlainTextBody().contains("UTF-8 character encoding"));
        }
    }

    @Test
    void shouldHandleMalformedMimeWithoutCrashing() throws Exception {
        Path emlPath = fixturesDir.resolve("05-malformed-mime.eml");
        try (InputStream in = Files.newInputStream(emlPath)) {
            ParsedEmailDto dto = parserService.parse(in);

            assertNotNull(dto);
            assertEquals("Malformed MIME Test", dto.getSubject());
        }
    }
}
