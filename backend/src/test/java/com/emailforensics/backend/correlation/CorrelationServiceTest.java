package com.emailforensics.backend.correlation;

import com.emailforensics.backend.casefile.CaseFile;
import com.emailforensics.backend.dto.correlation.CorrelationMatchDto;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.AnalysisStatus;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceService;
import com.emailforensics.backend.headers.HeaderAnalysis;
import com.emailforensics.backend.headers.HeaderAnalysisRepository;
import com.emailforensics.backend.infrastructure.IpArtifact;
import com.emailforensics.backend.infrastructure.IpArtifactRepository;
import com.emailforensics.backend.urlintel.ExtractedUrl;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import com.emailforensics.backend.user.Role;
import com.emailforensics.backend.user.User;
import com.emailforensics.backend.visual.QrArtifactRepository;
import com.emailforensics.backend.visual.VisualArtifactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private HeaderAnalysisRepository headerRepository;

    @Mock
    private VisualArtifactRepository visualRepository;

    @Mock
    private QrArtifactRepository qrRepository;

    @Mock
    private EvidenceService evidenceService;


    private CorrelationService correlationService;

    private User analyst;
    private CaseFile caseFile1;
    private CaseFile caseFile2;
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
                headerRepository,
                visualRepository,
                qrRepository,
                evidenceService
        );

        analyst = new User(UUID.randomUUID(), "analyst", "analyst@test.local", "hashed", Role.ANALYST, true, Instant.now());
        caseFile1 = new CaseFile(UUID.randomUUID(), "CASE-2026-0001", "Phishing Campaign Alpha", "Desc", com.emailforensics.backend.casefile.CaseStatus.OPEN, "notes", analyst, Instant.now(), Instant.now());
        caseFile2 = new CaseFile(UUID.randomUUID(), "CASE-2026-0002", "Credential Harvest Beta", "Desc", com.emailforensics.backend.casefile.CaseStatus.OPEN, "notes", analyst, Instant.now(), Instant.now());

        email1 = new EmailEvidence(UUID.randomUUID(), caseFile1, "phish1.eml", "storage-1", 1024L,
                "a1b2c3d4e5f60000000000000000000000000000000000000000000000000001", analyst);
        email1.setSubject("Urgent Account Verification");

        email2 = new EmailEvidence(UUID.randomUUID(), caseFile2, "phish2.eml", "storage-2", 2048L,
                "a1b2c3d4e5f60000000000000000000000000000000000000000000000000002", analyst);
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
        when(headerRepository.findByAnalysisRunId(run1.getId())).thenReturn(Optional.empty());
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(evidenceService.getEvidenceAttachments(email1.getId())).thenReturn(Collections.emptyList());
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
        assertEquals("CASE-2026-0002", match.getMatchedCaseNumber());

    }

    @Test
    void testCorrelateAndSave_IdenticalAttachmentHash_CreatesExactMatch() {
        when(evidenceRepository.findById(email1.getId())).thenReturn(Optional.of(email1));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(email1.getId())).thenReturn(Optional.of(run1));

        when(urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(headerRepository.findByAnalysisRunId(run1.getId())).thenReturn(Optional.empty());
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(evidenceRepository.findAllBySha256Hash(email1.getSha256Hash())).thenReturn(List.of(email1));

        String attachmentHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        AttachmentMetaDto att1 = new AttachmentMetaDto("invoice.pdf", "application/pdf", 1024L, attachmentHash, false, null, false);
        when(evidenceService.getEvidenceAttachments(email1.getId())).thenReturn(List.of(att1));

        when(evidenceRepository.findAll()).thenReturn(List.of(email1, email2));
        AttachmentMetaDto att2 = new AttachmentMetaDto("invoice_copy.pdf", "application/pdf", 1024L, attachmentHash, false, null, false);
        when(evidenceService.getEvidenceAttachments(email2.getId())).thenReturn(List.of(att2));

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
        when(headerRepository.findByAnalysisRunId(run1.getId())).thenReturn(Optional.empty());
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run1.getId())).thenReturn(Collections.emptyList());
        when(evidenceService.getEvidenceAttachments(email1.getId())).thenReturn(Collections.emptyList());
        when(evidenceRepository.findAllBySha256Hash(email1.getSha256Hash())).thenReturn(List.of(email1));

        when(matchRepository.findBySourceEmailId(email1.getId())).thenReturn(Collections.emptyList());

        List<CorrelationMatchDto> results = correlationService.correlateAndSave(email1.getId());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
