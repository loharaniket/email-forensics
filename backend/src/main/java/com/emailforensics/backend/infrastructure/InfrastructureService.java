package com.emailforensics.backend.infrastructure;

import com.emailforensics.backend.dto.infrastructure.IpArtifactDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.headers.ReceivedHop;
import com.emailforensics.backend.headers.ReceivedHopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InfrastructureService {

    private final IpArtifactRepository ipArtifactRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final ReceivedHopRepository receivedHopRepository;
    private final GeoIpService geoIpService;

    public InfrastructureService(IpArtifactRepository ipArtifactRepository,
                                 EmailEvidenceRepository evidenceRepository,
                                 AnalysisRunRepository analysisRunRepository,
                                 ReceivedHopRepository receivedHopRepository,
                                 GeoIpService geoIpService) {
        this.ipArtifactRepository = ipArtifactRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.receivedHopRepository = receivedHopRepository;
        this.geoIpService = geoIpService;
    }

    @Transactional
    public List<IpArtifactDto> analyzeAndSaveInfrastructure(UUID emailId, List<String> additionalIps) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        List<IpArtifact> existing = ipArtifactRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        Set<String> seenIps = new HashSet<>();
        for (IpArtifact art : existing) {
            seenIps.add(art.getIpAddress().toLowerCase());
        }

        List<IpArtifact> toSave = new ArrayList<>();

        List<ReceivedHop> hops = receivedHopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());
        for (ReceivedHop hop : hops) {
            String ip = hop.getHopIp();
            if (ip != null && !ip.trim().isEmpty()) {
                String clean = ip.trim().toLowerCase();
                if (clean.startsWith("[") && clean.endsWith("]")) {
                    clean = clean.substring(1, clean.length() - 1);
                }
                if (seenIps.add(clean)) {
                    IpArtifact artifact = geoIpService.enrichIp(clean, "RECEIVED_HOP_" + hop.getHopOrder());
                    artifact.setAnalysisRun(run);
                    toSave.add(artifact);
                }
            }
        }

        if (additionalIps != null) {
            for (String ip : additionalIps) {
                if (ip != null && !ip.trim().isEmpty()) {
                    String clean = ip.trim().toLowerCase();
                    if (clean.startsWith("[") && clean.endsWith("]")) {
                        clean = clean.substring(1, clean.length() - 1);
                    }
                    if (seenIps.add(clean)) {
                        IpArtifact artifact = geoIpService.enrichIp(clean, "URL_HOST_OR_DNS");
                        artifact.setAnalysisRun(run);
                        toSave.add(artifact);
                    }
                }
            }
        }

        if (!toSave.isEmpty()) {
            ipArtifactRepository.saveAll(toSave);
            ipArtifactRepository.flush();
        }

        List<IpArtifact> all = ipArtifactRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        return mapToDto(all);
    }

    @Transactional
    public List<IpArtifactDto> getInfrastructure(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        List<IpArtifact> list = ipArtifactRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        if (list.isEmpty()) {
            return analyzeAndSaveInfrastructure(emailId, null);
        }

        return mapToDto(list);
    }

    private List<IpArtifactDto> mapToDto(List<IpArtifact> list) {
        List<IpArtifactDto> dtos = new ArrayList<>();
        for (IpArtifact art : list) {
            dtos.add(new IpArtifactDto(
                    art.getId(),
                    art.getIpAddress(),
                    art.getSourceContext(),
                    art.getIsPrivate(),
                    art.getLookupStatus(),
                    art.getAsnNumber(),
                    art.getOrganization(),
                    art.getCountry(),
                    art.getCity(),
                    art.getLatitude(),
                    art.getLongitude(),
                    art.getDataSource(),
                    art.getObservedAt()
            ));
        }
        return dtos;
    }
}
