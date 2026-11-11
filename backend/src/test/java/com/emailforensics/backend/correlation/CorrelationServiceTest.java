package com.emailforensics.backend.correlation;

import com.emailforensics.backend.dto.correlation.CorrelationMatchDto;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import com.emailforensics.backend.email.EmailParserService;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.AnalysisStatus;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.analysis.AnalysisResult;
import com.emailforensics.backend.analysis.AnalysisResultRepository;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.emailforensics.backend.infrastructure.IpArtifact;
import com.emailforensics.backend.infrastructure.IpArtifactRepository;
import com.emailforensics.backend.urlintel.ExtractedUrl;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import com.emailforensics.backend.visual.QrArtifactRepository;
import com.emailforensics.backend.visual.VisualArtifactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CorrelationServiceTest {

    @Mock
    private HistoricalMatchRepository matchRepository;

    @Mock
    private EmailEvidenceRepository evidenceRepository;

    @Mock
    private AnalysisRunRepository runRepository;

    @Mock
    private ExtractedUrlRepository urlRepository;

    @Mock
    private IpArtifactRepository ipRepository;

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @Mock
    private VisualArtifactRepository visualRepository;

    @Mock
    private QrArtifactRepository qrRepository;

    @Mock
    private EvidenceStorageService storageService;

    @Mock
    private EmailParserService emailParserService;


    private CorrelationService correlationService;

    private EmailEvidence email1;
    private EmailEvidence email2;
    private AnalysisRun run1;
    private AnalysisRun run2;

    @BeforeEach
    void setUp() {
        correlationService = new CorrelationService(
                matchRepository,
                evidenceRepository,
                runRepository,
                urlRepository,
                ipRepository,
                analysisResultRepository,
                visualRepository,
                qrRepository,
                storageService,
                emailParserService
        );

        email1 = new EmailEvidence(UUID.randomUUID(), "phish1.eml", "storage-1", 1024L,
                "a1b2c3d4e5f60000000000000000000000000000000000000000000000000001");
        email1.setSubject("Urgent Account Verification");

        email2 = new EmailEvidence(UUID.randomUUID(), "phish2.eml", "storage-2", 2048L,
                "a1b2c3d4e5f60000000000000000000000000000000000000000000000000002");
        email2.setSubject("Security Alert Notice");

        run1 = new AnalysisRun(UUID.randomUUID(), email1, "v1.0.0", AnalysisStatus.COMPLETED, "MIME_PARSED");
        run2 = new AnalysisRun(UUID.randomUUID(), email2, "v1.0.0", AnalysisStatus.COMPLETED, "MIME_PARSED");
    }

    @Test
    void testCorrelateAndSave_SharedDomainAndUrl_GeneratesMatchesWithoutSelfMatch() {
        when(evidenceRepository.findById(email1.getId())).thenReturn(Optional.of(email1));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(email1.getId())).thenReturn(Optional.of(run1));

        ExtractedUrl url1 = new ExtractedUrl(UUID.randomUUID(), run1, "http://malicious-login.com/login",
                "http://malicious-login.com/login", "malicious-login.com", "HTML_ANCHOR", "Verify Login",
                false, false, false, false, false, false, "NXDOMAIN", 80);

        when(urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(List.of(url1));
        when(ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(analysisResultRepository.findByAnalysisRunId(run1.getId())).thenReturn(Optional.empty());
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(storageService.loadAsResource(any())).thenReturn(new ByteArrayResource(new byte[0]));
        when(emailParserService.parse(any(InputStream.class))).thenReturn(new ParsedEmailDto());
        when(evidenceRepository.findAllBySha256Hash(email1.getSha256Hash())).thenReturn(List.of(email1));

        ExtractedUrl matchedUrl = new ExtractedUrl(UUID.randomUUID(), run2, "http://malicious-login.com/login",
                "http://malicious-login.com/login", "malicious-login.com", "HTML_ANCHOR", "Verify Login",
                false, false, false, false, false, false, "NXDOMAIN", 80);

        when(urlRepository.findByNormalizedUrl("http://malicious-login.com/login")).thenReturn(List.of(url1, matchedUrl));
        when(urlRepository.findByDomain("malicious-login.com")).thenReturn(List.of(url1, matchedUrl));

        when(matchRepository.existsBySourceRunIdAndMatchedEmailIdAndIndicatorTypeAndNormalizedIndicatorValue(
                any(), any(), any(), any())).thenReturn(false);

        HistoricalMatch persistedMatch = new HistoricalMatch(
                UUID.randomUUID(), run1, email2, "URL", "http://malicious-login.com/login", "EXACT", "Exact URL match"
        );
        when(matchRepository.save(any(HistoricalMatch.class))).thenReturn(persistedMatch);
        when(matchRepository.findBySourceEmailId(email1.getId())).thenReturn(List.of(persistedMatch));

        List<CorrelationMatchDto> results = correlationService.correlateAndSave(email1.getId());

        assertNotNull(results);
        assertEquals(1, results.size());
        CorrelationMatchDto match = results.get(0);
        assertEquals("URL", match.getIndicatorType());
        assertEquals("EXACT", match.getMatchStrength());
        assertEquals(email2.getId(), match.getMatchedEmailId());
        assertEquals("phish2.eml", match.getMatchedEmailFilename());

    }

    @Test
    void testCorrelateAndSave_IdenticalAttachmentHash_CreatesExactMatch() {
        when(evidenceRepository.findById(email1.getId())).thenReturn(Optional.of(email1));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(email1.getId())).thenReturn(Optional.of(run1));

        when(urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(analysisResultRepository.findByAnalysisRunId(run1.getId())).thenReturn(Optional.empty());
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(evidenceRepository.findAllBySha256Hash(email1.getSha256Hash())).thenReturn(List.of(email1));

        String attachmentHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        AttachmentMetaDto att1 = new AttachmentMetaDto("invoice.pdf", "application/pdf", 1024L, attachmentHash, false, null, false);
        AttachmentMetaDto att2 = new AttachmentMetaDto("invoice_copy.pdf", "application/pdf", 1024L, attachmentHash, false, null, false);

        ParsedEmailDto parsed1 = new ParsedEmailDto();
        parsed1.setAttachments(List.of(att1));
        ParsedEmailDto parsed2 = new ParsedEmailDto();
        parsed2.setAttachments(List.of(att2));

        when(storageService.loadAsResource(any())).thenReturn(new ByteArrayResource(new byte[0]));
        when(emailParserService.parse(any(InputStream.class))).thenReturn(parsed1, parsed2);

        when(evidenceRepository.findAll()).thenReturn(List.of(email1, email2));

        HistoricalMatch attMatch = new HistoricalMatch(
                UUID.randomUUID(), run1, email2, "ATTACHMENT_HASH", attachmentHash, "EXACT", "Identical attachment"
        );
        when(matchRepository.save(any(HistoricalMatch.class))).thenReturn(attMatch);
        when(matchRepository.findBySourceEmailId(email1.getId())).thenReturn(List.of(attMatch));

        List<CorrelationMatchDto> results = correlationService.correlateAndSave(email1.getId());

        assertEquals(1, results.size());
        assertEquals("ATTACHMENT_HASH", results.get(0).getIndicatorType());
        assertEquals("EXACT", results.get(0).getMatchStrength());
    }

    @Test
    void testCorrelateAndSave_UnrelatedEvidence_ReturnsNoMatches() {
        when(evidenceRepository.findById(email1.getId())).thenReturn(Optional.of(email1));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(email1.getId())).thenReturn(Optional.of(run1));

        when(urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(analysisResultRepository.findByAnalysisRunId(run1.getId())).thenReturn(Optional.empty());
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(storageService.loadAsResource(any())).thenReturn(new ByteArrayResource(new byte[0]));
        when(emailParserService.parse(any(InputStream.class))).thenReturn(new ParsedEmailDto());
        when(evidenceRepository.findAllBySha256Hash(email1.getSha256Hash())).thenReturn(List.of(email1));

        when(matchRepository.findBySourceEmailId(email1.getId())).thenReturn(Collections.emptyList());

        List<CorrelationMatchDto> results = correlationService.correlateAndSave(email1.getId());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testCorrelateAndSave_UnreadableSourceEvidenceFile_ThrowsIllegalStateException() {
        when(evidenceRepository.findById(email1.getId())).thenReturn(Optional.of(email1));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(email1.getId())).thenReturn(Optional.of(run1));

        when(urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(analysisResultRepository.findByAnalysisRunId(run1.getId())).thenReturn(Optional.empty());
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(storageService.loadAsResource(email1.getStorageKey())).thenThrow(new RuntimeException("Disk I/O failure reading evidence"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> correlationService.correlateAndSave(email1.getId()));
        assertTrue(ex.getMessage().contains("Failed to read source evidence attachments for correlation"));
    }

    @Test
    void testCorrelateAndSave_UnreadableCorrelatedEvidenceFile_LogsAndContinues() {
        when(evidenceRepository.findById(email1.getId())).thenReturn(Optional.of(email1));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(email1.getId())).thenReturn(Optional.of(run1));

        when(urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(analysisResultRepository.findByAnalysisRunId(run1.getId())).thenReturn(Optional.empty());
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(evidenceRepository.findAllBySha256Hash(email1.getSha256Hash())).thenReturn(List.of(email1));

        String attachmentHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        AttachmentMetaDto att1 = new AttachmentMetaDto("invoice.pdf", "application/pdf", 1024L, attachmentHash, false, null, false);
        ParsedEmailDto parsed1 = new ParsedEmailDto();
        parsed1.setAttachments(List.of(att1));

        when(storageService.loadAsResource(email1.getStorageKey())).thenReturn(new ByteArrayResource(new byte[0]));
        when(emailParserService.parse(any(InputStream.class))).thenReturn(parsed1);

        when(storageService.loadAsResource(email2.getStorageKey())).thenThrow(new RuntimeException("Corrupted file"));
        when(evidenceRepository.findAll()).thenReturn(List.of(email1, email2));
        when(matchRepository.findBySourceEmailId(email1.getId())).thenReturn(Collections.emptyList());

        List<CorrelationMatchDto> results = correlationService.correlateAndSave(email1.getId());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
