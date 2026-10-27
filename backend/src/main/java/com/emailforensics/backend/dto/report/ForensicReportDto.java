package com.emailforensics.backend.dto.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ForensicReportDto {

    private String reportId;
    private Instant generatedAt;
    private String reportVersion;
    private String generatedBy;
    private String classificationLevel;

    private CaseContextDto caseContext;
    private EvidenceIntegrityDto evidenceIntegrity;
    private MessageSummaryDto messageSummary;
    private ExecutionStatusDto executionStatus;
    private HeaderForensicsSummaryDto headerForensics;
    private NetworkAndUrlSummaryDto networkAndUrls;
    private VisualAndArtifactSummaryDto visualsAndArtifacts;
    private ContentAnalysisSummaryDto contentAnalysis;
    private DeterministicRiskSummaryDto riskSummary;
    private CorrelationAndCampaignSummaryDto correlationsAndCampaigns;
    private AiAssessmentSummaryDto aiAssessment;
    private AnalystSignoffDto analystSignoff;

    public ForensicReportDto() {
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getReportVersion() {
        return reportVersion;
    }

    public void setReportVersion(String reportVersion) {
        this.reportVersion = reportVersion;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getClassificationLevel() {
        return classificationLevel;
    }

    public void setClassificationLevel(String classificationLevel) {
        this.classificationLevel = classificationLevel;
    }

    public CaseContextDto getCaseContext() {
        return caseContext;
    }

    public void setCaseContext(CaseContextDto caseContext) {
        this.caseContext = caseContext;
    }

    public EvidenceIntegrityDto getEvidenceIntegrity() {
        return evidenceIntegrity;
    }

    public void setEvidenceIntegrity(EvidenceIntegrityDto evidenceIntegrity) {
        this.evidenceIntegrity = evidenceIntegrity;
    }

    public MessageSummaryDto getMessageSummary() {
        return messageSummary;
    }

    public void setMessageSummary(MessageSummaryDto messageSummary) {
        this.messageSummary = messageSummary;
    }

    public ExecutionStatusDto getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(ExecutionStatusDto executionStatus) {
        this.executionStatus = executionStatus;
    }

    public HeaderForensicsSummaryDto getHeaderForensics() {
        return headerForensics;
    }

    public void setHeaderForensics(HeaderForensicsSummaryDto headerForensics) {
        this.headerForensics = headerForensics;
    }

    public NetworkAndUrlSummaryDto getNetworkAndUrls() {
        return networkAndUrls;
    }

    public void setNetworkAndUrls(NetworkAndUrlSummaryDto networkAndUrls) {
        this.networkAndUrls = networkAndUrls;
    }

    public VisualAndArtifactSummaryDto getVisualsAndArtifacts() {
        return visualsAndArtifacts;
    }

    public void setVisualsAndArtifacts(VisualAndArtifactSummaryDto visualsAndArtifacts) {
        this.visualsAndArtifacts = visualsAndArtifacts;
    }

    public ContentAnalysisSummaryDto getContentAnalysis() {
        return contentAnalysis;
    }

    public void setContentAnalysis(ContentAnalysisSummaryDto contentAnalysis) {
        this.contentAnalysis = contentAnalysis;
    }

    public DeterministicRiskSummaryDto getRiskSummary() {
        return riskSummary;
    }

    public void setRiskSummary(DeterministicRiskSummaryDto riskSummary) {
        this.riskSummary = riskSummary;
    }

    public CorrelationAndCampaignSummaryDto getCorrelationsAndCampaigns() {
        return correlationsAndCampaigns;
    }

    public void setCorrelationsAndCampaigns(CorrelationAndCampaignSummaryDto correlationsAndCampaigns) {
        this.correlationsAndCampaigns = correlationsAndCampaigns;
    }

    public AiAssessmentSummaryDto getAiAssessment() {
        return aiAssessment;
    }

    public void setAiAssessment(AiAssessmentSummaryDto aiAssessment) {
        this.aiAssessment = aiAssessment;
    }

    public AnalystSignoffDto getAnalystSignoff() {
        return analystSignoff;
    }

    public void setAnalystSignoff(AnalystSignoffDto analystSignoff) {
        this.analystSignoff = analystSignoff;
    }

    public static class CaseContextDto {
        private UUID caseId;
        private String caseNumber;
        private String title;
        private String description;
        private String status;
        private String analystNotes;
        private Instant createdAt;

        public CaseContextDto() {
        }

        public UUID getCaseId() {
            return caseId;
        }

        public void setCaseId(UUID caseId) {
            this.caseId = caseId;
        }

        public String getCaseNumber() {
            return caseNumber;
        }

        public void setCaseNumber(String caseNumber) {
            this.caseNumber = caseNumber;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getAnalystNotes() {
            return analystNotes;
        }

        public void setAnalystNotes(String analystNotes) {
            this.analystNotes = analystNotes;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class EvidenceIntegrityDto {
        private UUID emailId;
        private String originalFilename;
        private String sha256Hash;
        private Long fileSizeBytes;
        private String storageKey;
        private Instant uploadedAt;
        private String uploaderUsername;
        private boolean integrityVerified;
        private Instant integrityCheckTimestamp;
        private String recalculatedHash;
        private String integrityStatus;

        public EvidenceIntegrityDto() {
        }

        public UUID getEmailId() {
            return emailId;
        }

        public void setEmailId(UUID emailId) {
            this.emailId = emailId;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String getSha256Hash() {
            return sha256Hash;
        }

        public void setSha256Hash(String sha256Hash) {
            this.sha256Hash = sha256Hash;
        }

        public Long getFileSizeBytes() {
            return fileSizeBytes;
        }

        public void setFileSizeBytes(Long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
        }

        public String getStorageKey() {
            return storageKey;
        }

        public void setStorageKey(String storageKey) {
            this.storageKey = storageKey;
        }

        public Instant getUploadedAt() {
            return uploadedAt;
        }

        public void setUploadedAt(Instant uploadedAt) {
            this.uploadedAt = uploadedAt;
        }

        public String getUploaderUsername() {
            return uploaderUsername;
        }

        public void setUploaderUsername(String uploaderUsername) {
            this.uploaderUsername = uploaderUsername;
        }

        public boolean isIntegrityVerified() {
            return integrityVerified;
        }

        public void setIntegrityVerified(boolean integrityVerified) {
            this.integrityVerified = integrityVerified;
        }

        public Instant getIntegrityCheckTimestamp() {
            return integrityCheckTimestamp;
        }

        public void setIntegrityCheckTimestamp(Instant integrityCheckTimestamp) {
            this.integrityCheckTimestamp = integrityCheckTimestamp;
        }

        public String getRecalculatedHash() {
            return recalculatedHash;
        }

        public void setRecalculatedHash(String recalculatedHash) {
            this.recalculatedHash = recalculatedHash;
        }

        public String getIntegrityStatus() {
            return integrityStatus;
        }

        public void setIntegrityStatus(String integrityStatus) {
            this.integrityStatus = integrityStatus;
        }
    }

    public static class MessageSummaryDto {
        private String subject;
        private String fromAddress;
        private String toAddress;
        private String ccAddress;
        private String replyTo;
        private Instant sentDate;
        private String messageId;
        private String bodySnippet;
        private String bodyType;
        private int attachmentsCount;

        public MessageSummaryDto() {
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }

        public String getToAddress() {
            return toAddress;
        }

        public void setToAddress(String toAddress) {
            this.toAddress = toAddress;
        }

        public String getCcAddress() {
            return ccAddress;
        }

        public void setCcAddress(String ccAddress) {
            this.ccAddress = ccAddress;
        }

        public String getReplyTo() {
            return replyTo;
        }

        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }

        public Instant getSentDate() {
            return sentDate;
        }

        public void setSentDate(Instant sentDate) {
            this.sentDate = sentDate;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getBodySnippet() {
            return bodySnippet;
        }

        public void setBodySnippet(String bodySnippet) {
            this.bodySnippet = bodySnippet;
        }

        public String getBodyType() {
            return bodyType;
        }

        public void setBodyType(String bodyType) {
            this.bodyType = bodyType;
        }

        public int getAttachmentsCount() {
            return attachmentsCount;
        }

        public void setAttachmentsCount(int attachmentsCount) {
            this.attachmentsCount = attachmentsCount;
        }
    }

    public static class ExecutionStatusDto {
        private UUID analysisRunId;
        private String pipelineVersion;
        private String currentStage;
        private String status;
        private Instant startedAt;
        private Instant completedAt;
        private List<String> stageWarnings = new ArrayList<>();
        private String errorCode;
        private String errorMessage;

        public ExecutionStatusDto() {
        }

        public UUID getAnalysisRunId() {
            return analysisRunId;
        }

        public void setAnalysisRunId(UUID analysisRunId) {
            this.analysisRunId = analysisRunId;
        }

        public String getPipelineVersion() {
            return pipelineVersion;
        }

        public void setPipelineVersion(String pipelineVersion) {
            this.pipelineVersion = pipelineVersion;
        }

        public String getCurrentStage() {
            return currentStage;
        }

        public void setCurrentStage(String currentStage) {
            this.currentStage = currentStage;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(Instant startedAt) {
            this.startedAt = startedAt;
        }

        public Instant getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(Instant completedAt) {
            this.completedAt = completedAt;
        }

        public List<String> getStageWarnings() {
            return stageWarnings;
        }

        public void setStageWarnings(List<String> stageWarnings) {
            this.stageWarnings = stageWarnings;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public static class HeaderForensicsSummaryDto {
        private String fromAddress;
        private String fromDomain;
        private String returnPath;
        private String replyTo;
        private String spfResult;
        private String dkimResult;
        private String dmarcResult;
        private String arcResult;
        private String activeSpfResult;
        private String activeDkimResult;
        private String activeDmarcResult;
        private String activeVerificationSummary;
        private List<String> identityMismatchFlags = new ArrayList<>();
        private int hopsCount;
        private String firstOriginatingIp;
        private String finalDestinationHost;
        private List<HopSummaryDto> hopsSummary = new ArrayList<>();

        public HeaderForensicsSummaryDto() {
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }

        public String getFromDomain() {
            return fromDomain;
        }

        public void setFromDomain(String fromDomain) {
            this.fromDomain = fromDomain;
        }

        public String getReturnPath() {
            return returnPath;
        }

        public void setReturnPath(String returnPath) {
            this.returnPath = returnPath;
        }

        public String getReplyTo() {
            return replyTo;
        }

        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }

        public String getSpfResult() {
            return spfResult;
        }

        public void setSpfResult(String spfResult) {
            this.spfResult = spfResult;
        }

        public String getDkimResult() {
            return dkimResult;
        }

        public void setDkimResult(String dkimResult) {
            this.dkimResult = dkimResult;
        }

        public String getDmarcResult() {
            return dmarcResult;
        }

        public void setDmarcResult(String dmarcResult) {
            this.dmarcResult = dmarcResult;
        }

        public String getArcResult() {
            return arcResult;
        }

        public void setArcResult(String arcResult) {
            this.arcResult = arcResult;
        }

        public List<String> getIdentityMismatchFlags() {
            return identityMismatchFlags;
        }

        public void setIdentityMismatchFlags(List<String> identityMismatchFlags) {
            this.identityMismatchFlags = identityMismatchFlags;
        }

        public int getHopsCount() {
            return hopsCount;
        }

        public void setHopsCount(int hopsCount) {
            this.hopsCount = hopsCount;
        }

        public String getFirstOriginatingIp() {
            return firstOriginatingIp;
        }

        public void setFirstOriginatingIp(String firstOriginatingIp) {
            this.firstOriginatingIp = firstOriginatingIp;
        }

        public String getFinalDestinationHost() {
            return finalDestinationHost;
        }

        public void setFinalDestinationHost(String finalDestinationHost) {
            this.finalDestinationHost = finalDestinationHost;
        }

        public List<HopSummaryDto> getHopsSummary() {
            return hopsSummary;
        }

        public void setHopsSummary(List<HopSummaryDto> hopsSummary) {
            this.hopsSummary = hopsSummary;
        }

        public String getActiveSpfResult() {
            return activeSpfResult;
        }

        public void setActiveSpfResult(String activeSpfResult) {
            this.activeSpfResult = activeSpfResult;
        }

        public String getActiveDkimResult() {
            return activeDkimResult;
        }

        public void setActiveDkimResult(String activeDkimResult) {
            this.activeDkimResult = activeDkimResult;
        }

        public String getActiveDmarcResult() {
            return activeDmarcResult;
        }

        public void setActiveDmarcResult(String activeDmarcResult) {
            this.activeDmarcResult = activeDmarcResult;
        }

        public String getActiveVerificationSummary() {
            return activeVerificationSummary;
        }

        public void setActiveVerificationSummary(String activeVerificationSummary) {
            this.activeVerificationSummary = activeVerificationSummary;
        }
    }

    public static class HopSummaryDto {
        private int hopOrder;
        private String fromHost;
        private String byHost;
        private String hopIp;
        private Long delaySeconds;

        public HopSummaryDto() {
        }

        public HopSummaryDto(int hopOrder, String fromHost, String byHost, String hopIp, Long delaySeconds) {
            this.hopOrder = hopOrder;
            this.fromHost = fromHost;
            this.byHost = byHost;
            this.hopIp = hopIp;
            this.delaySeconds = delaySeconds;
        }

        public int getHopOrder() {
            return hopOrder;
        }

        public void setHopOrder(int hopOrder) {
            this.hopOrder = hopOrder;
        }

        public String getFromHost() {
            return fromHost;
        }

        public void setFromHost(String fromHost) {
            this.fromHost = fromHost;
        }

        public String getByHost() {
            return byHost;
        }

        public void setByHost(String byHost) {
            this.byHost = byHost;
        }

        public String getHopIp() {
            return hopIp;
        }

        public void setHopIp(String hopIp) {
            this.hopIp = hopIp;
        }

        public Long getDelaySeconds() {
            return delaySeconds;
        }

        public void setDelaySeconds(Long delaySeconds) {
            this.delaySeconds = delaySeconds;
        }
    }

    public static class NetworkAndUrlSummaryDto {
        private int totalUrlsCount;
        private int suspiciousUrlsCount;
        private int maliciousUrlsCount;
        private List<ReportUrlDto> urls = new ArrayList<>();
        private int totalIpsCount;
        private int privateBogonIpsCount;
        private List<ReportIpDto> ips = new ArrayList<>();

        public NetworkAndUrlSummaryDto() {
        }

        public int getTotalUrlsCount() {
            return totalUrlsCount;
        }

        public void setTotalUrlsCount(int totalUrlsCount) {
            this.totalUrlsCount = totalUrlsCount;
        }

        public int getSuspiciousUrlsCount() {
            return suspiciousUrlsCount;
        }

        public void setSuspiciousUrlsCount(int suspiciousUrlsCount) {
            this.suspiciousUrlsCount = suspiciousUrlsCount;
        }

        public int getMaliciousUrlsCount() {
            return maliciousUrlsCount;
        }

        public void setMaliciousUrlsCount(int maliciousUrlsCount) {
            this.maliciousUrlsCount = maliciousUrlsCount;
        }

        public List<ReportUrlDto> getUrls() {
            return urls;
        }

        public void setUrls(List<ReportUrlDto> urls) {
            this.urls = urls;
        }

        public int getTotalIpsCount() {
            return totalIpsCount;
        }

        public void setTotalIpsCount(int totalIpsCount) {
            this.totalIpsCount = totalIpsCount;
        }

        public int getPrivateBogonIpsCount() {
            return privateBogonIpsCount;
        }

        public void setPrivateBogonIpsCount(int privateBogonIpsCount) {
            this.privateBogonIpsCount = privateBogonIpsCount;
        }

        public List<ReportIpDto> getIps() {
            return ips;
        }

        public void setIps(List<ReportIpDto> ips) {
            this.ips = ips;
        }
    }

    public static class ReportUrlDto {
        private String originalUrl;
        private String domain;
        private boolean suspicious;
        private boolean ipLiteral;
        private boolean shortener;
        private boolean anchorMismatch;
        private boolean punycode;
        private String threatCategory;

        public ReportUrlDto() {
        }

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public boolean isSuspicious() {
            return suspicious;
        }

        public void setSuspicious(boolean suspicious) {
            this.suspicious = suspicious;
        }

        public boolean isIpLiteral() {
            return ipLiteral;
        }

        public void setIpLiteral(boolean ipLiteral) {
            this.ipLiteral = ipLiteral;
        }

        public boolean isShortener() {
            return shortener;
        }

        public void setShortener(boolean shortener) {
            this.shortener = shortener;
        }

        public boolean isAnchorMismatch() {
            return anchorMismatch;
        }

        public void setAnchorMismatch(boolean anchorMismatch) {
            this.anchorMismatch = anchorMismatch;
        }

        public boolean isPunycode() {
            return punycode;
        }

        public void setPunycode(boolean punycode) {
            this.punycode = punycode;
        }

        public String getThreatCategory() {
            return threatCategory;
        }

        public void setThreatCategory(String threatCategory) {
            this.threatCategory = threatCategory;
        }
    }

    public static class ReportIpDto {
        private String ipAddress;
        private String sourceCategory;
        private boolean privateBogon;
        private String countryCode;
        private String asn;
        private String reverseDns;

        public ReportIpDto() {
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getSourceCategory() {
            return sourceCategory;
        }

        public void setSourceCategory(String sourceCategory) {
            this.sourceCategory = sourceCategory;
        }

        public boolean isPrivateBogon() {
            return privateBogon;
        }

        public void setPrivateBogon(boolean privateBogon) {
            this.privateBogon = privateBogon;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getAsn() {
            return asn;
        }

        public void setAsn(String asn) {
            this.asn = asn;
        }

        public String getReverseDns() {
            return reverseDns;
        }

        public void setReverseDns(String reverseDns) {
            this.reverseDns = reverseDns;
        }
    }

    public static class VisualAndArtifactSummaryDto {
        private int totalAttachmentsCount;
        private List<ReportAttachmentDto> attachments = new ArrayList<>();
        private int visualArtifactsCount;
        private List<String> ocrTextSnippets = new ArrayList<>();
        private List<String> qrDecodedPayloads = new ArrayList<>();

        public VisualAndArtifactSummaryDto() {
        }

        public int getTotalAttachmentsCount() {
            return totalAttachmentsCount;
        }

        public void setTotalAttachmentsCount(int totalAttachmentsCount) {
            this.totalAttachmentsCount = totalAttachmentsCount;
        }

        public List<ReportAttachmentDto> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<ReportAttachmentDto> attachments) {
            this.attachments = attachments;
        }

        public int getVisualArtifactsCount() {
            return visualArtifactsCount;
        }

        public void setVisualArtifactsCount(int visualArtifactsCount) {
            this.visualArtifactsCount = visualArtifactsCount;
        }

        public List<String> getOcrTextSnippets() {
            return ocrTextSnippets;
        }

        public void setOcrTextSnippets(List<String> ocrTextSnippets) {
            this.ocrTextSnippets = ocrTextSnippets;
        }

        public List<String> getQrDecodedPayloads() {
            return qrDecodedPayloads;
        }

        public void setQrDecodedPayloads(List<String> qrDecodedPayloads) {
            this.qrDecodedPayloads = qrDecodedPayloads;
        }
    }

    public static class ReportAttachmentDto {
        private String filename;
        private String mimeType;
        private Long sizeBytes;
        private String sha256Hash;

        public ReportAttachmentDto() {
        }

        public ReportAttachmentDto(String filename, String mimeType, Long sizeBytes, String sha256Hash) {
            this.filename = filename;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
            this.sha256Hash = sha256Hash;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }

        public String getSha256Hash() {
            return sha256Hash;
        }

        public void setSha256Hash(String sha256Hash) {
            this.sha256Hash = sha256Hash;
        }
    }

    public static class ContentAnalysisSummaryDto {
        private boolean urgency;
        private boolean credentialRequest;
        private boolean paymentInstruction;
        private boolean impersonation;
        private boolean secrecy;
        private boolean promptInjectionDetected;
        private String tribuoPredictedLabel;
        private Double tribuoProbability;

        public ContentAnalysisSummaryDto() {
        }

        public boolean isUrgency() {
            return urgency;
        }

        public void setUrgency(boolean urgency) {
            this.urgency = urgency;
        }

        public boolean isCredentialRequest() {
            return credentialRequest;
        }

        public void setCredentialRequest(boolean credentialRequest) {
            this.credentialRequest = credentialRequest;
        }

        public boolean isPaymentInstruction() {
            return paymentInstruction;
        }

        public void setPaymentInstruction(boolean paymentInstruction) {
            this.paymentInstruction = paymentInstruction;
        }

        public boolean isImpersonation() {
            return impersonation;
        }

        public void setImpersonation(boolean impersonation) {
            this.impersonation = impersonation;
        }

        public boolean isSecrecy() {
            return secrecy;
        }

        public void setSecrecy(boolean secrecy) {
            this.secrecy = secrecy;
        }

        public boolean isPromptInjectionDetected() {
            return promptInjectionDetected;
        }

        public void setPromptInjectionDetected(boolean promptInjectionDetected) {
            this.promptInjectionDetected = promptInjectionDetected;
        }

        public String getTribuoPredictedLabel() {
            return tribuoPredictedLabel;
        }

        public void setTribuoPredictedLabel(String tribuoPredictedLabel) {
            this.tribuoPredictedLabel = tribuoPredictedLabel;
        }

        public Double getTribuoProbability() {
            return tribuoProbability;
        }

        public void setTribuoProbability(Double tribuoProbability) {
            this.tribuoProbability = tribuoProbability;
        }
    }

    public static class DeterministicRiskSummaryDto {
        private int deterministicRiskScore;
        private String riskLevel;
        private int evidenceConfidenceScore;
        private String confidenceLevel;
        private int contributingFactorsCount;
        private List<ReportRiskFactorDto> contributingFactors = new ArrayList<>();

        public DeterministicRiskSummaryDto() {
        }

        public int getDeterministicRiskScore() {
            return deterministicRiskScore;
        }

        public void setDeterministicRiskScore(int deterministicRiskScore) {
            this.deterministicRiskScore = deterministicRiskScore;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public int getEvidenceConfidenceScore() {
            return evidenceConfidenceScore;
        }

        public void setEvidenceConfidenceScore(int evidenceConfidenceScore) {
            this.evidenceConfidenceScore = evidenceConfidenceScore;
        }

        public String getConfidenceLevel() {
            return confidenceLevel;
        }

        public void setConfidenceLevel(String confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
        }

        public int getContributingFactorsCount() {
            return contributingFactorsCount;
        }

        public void setContributingFactorsCount(int contributingFactorsCount) {
            this.contributingFactorsCount = contributingFactorsCount;
        }

        public List<ReportRiskFactorDto> getContributingFactors() {
            return contributingFactors;
        }

        public void setContributingFactors(List<ReportRiskFactorDto> contributingFactors) {
            this.contributingFactors = contributingFactors;
        }
    }

    public static class ReportRiskFactorDto {
        private String factorCode;
        private String severity;
        private int weightPoints;
        private String category;
        private String explanation;

        public ReportRiskFactorDto() {
        }

        public ReportRiskFactorDto(String factorCode, String severity, int weightPoints, String category, String explanation) {
            this.factorCode = factorCode;
            this.severity = severity;
            this.weightPoints = weightPoints;
            this.category = category;
            this.explanation = explanation;
        }

        public String getFactorCode() {
            return factorCode;
        }

        public void setFactorCode(String factorCode) {
            this.factorCode = factorCode;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public int getWeightPoints() {
            return weightPoints;
        }

        public void setWeightPoints(int weightPoints) {
            this.weightPoints = weightPoints;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }

    public static class CorrelationAndCampaignSummaryDto {
        private int historicalMatchesCount;
        private List<ReportCorrelationMatchDto> matchedIndicators = new ArrayList<>();
        private boolean campaignMember;
        private String campaignCode;
        private String campaignName;
        private Integer campaignConfidence;

        public CorrelationAndCampaignSummaryDto() {
        }

        public int getHistoricalMatchesCount() {
            return historicalMatchesCount;
        }

        public void setHistoricalMatchesCount(int historicalMatchesCount) {
            this.historicalMatchesCount = historicalMatchesCount;
        }

        public List<ReportCorrelationMatchDto> getMatchedIndicators() {
            return matchedIndicators;
        }

        public void setMatchedIndicators(List<ReportCorrelationMatchDto> matchedIndicators) {
            this.matchedIndicators = matchedIndicators;
        }

        public boolean isCampaignMember() {
            return campaignMember;
        }

        public void setCampaignMember(boolean campaignMember) {
            this.campaignMember = campaignMember;
        }

        public String getCampaignCode() {
            return campaignCode;
        }

        public void setCampaignCode(String campaignCode) {
            this.campaignCode = campaignCode;
        }

        public String getCampaignName() {
            return campaignName;
        }

        public void setCampaignName(String campaignName) {
            this.campaignName = campaignName;
        }

        public Integer getCampaignConfidence() {
            return campaignConfidence;
        }

        public void setCampaignConfidence(Integer campaignConfidence) {
            this.campaignConfidence = campaignConfidence;
        }
    }

    public static class ReportCorrelationMatchDto {
        private String indicatorType;
        private String indicatorValue;
        private String matchStrength;
        private String matchedCaseNumber;

        public ReportCorrelationMatchDto() {
        }

        public ReportCorrelationMatchDto(String indicatorType, String indicatorValue, String matchStrength, String matchedCaseNumber) {
            this.indicatorType = indicatorType;
            this.indicatorValue = indicatorValue;
            this.matchStrength = matchStrength;
            this.matchedCaseNumber = matchedCaseNumber;
        }

        public String getIndicatorType() {
            return indicatorType;
        }

        public void setIndicatorType(String indicatorType) {
            this.indicatorType = indicatorType;
        }

        public String getIndicatorValue() {
            return indicatorValue;
        }

        public void setIndicatorValue(String indicatorValue) {
            this.indicatorValue = indicatorValue;
        }

        public String getMatchStrength() {
            return matchStrength;
        }

        public void setMatchStrength(String matchStrength) {
            this.matchStrength = matchStrength;
        }

        public String getMatchedCaseNumber() {
            return matchedCaseNumber;
        }

        public void setMatchedCaseNumber(String matchedCaseNumber) {
            this.matchedCaseNumber = matchedCaseNumber;
        }
    }

    public static class AiAssessmentSummaryDto {
        private boolean assessmentPresent;
        private String status;
        private String threatClassification;
        private String confidenceAssessment;
        private String summaryExplanation;
        private List<String> keyIndicators = new ArrayList<>();
        private List<String> evidenceReferences = new ArrayList<>();
        private List<String> unsupportedReferences = new ArrayList<>();
        private String limitationsAndCaveats;
        private boolean promptInjectionDetected;
        private String provider;
        private String modelName;
        private Integer tokensUsed;
        private Long generationTimeMs;

        public AiAssessmentSummaryDto() {
        }

        public boolean isAssessmentPresent() {
            return assessmentPresent;
        }

        public void setAssessmentPresent(boolean assessmentPresent) {
            this.assessmentPresent = assessmentPresent;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getThreatClassification() {
            return threatClassification;
        }

        public void setThreatClassification(String threatClassification) {
            this.threatClassification = threatClassification;
        }

        public String getConfidenceAssessment() {
            return confidenceAssessment;
        }

        public void setConfidenceAssessment(String confidenceAssessment) {
            this.confidenceAssessment = confidenceAssessment;
        }

        public String getSummaryExplanation() {
            return summaryExplanation;
        }

        public void setSummaryExplanation(String summaryExplanation) {
            this.summaryExplanation = summaryExplanation;
        }

        public List<String> getKeyIndicators() {
            return keyIndicators;
        }

        public void setKeyIndicators(List<String> keyIndicators) {
            this.keyIndicators = keyIndicators;
        }

        public List<String> getEvidenceReferences() {
            return evidenceReferences;
        }

        public void setEvidenceReferences(List<String> evidenceReferences) {
            this.evidenceReferences = evidenceReferences;
        }

        public List<String> getUnsupportedReferences() {
            return unsupportedReferences;
        }

        public void setUnsupportedReferences(List<String> unsupportedReferences) {
            this.unsupportedReferences = unsupportedReferences;
        }

        public String getLimitationsAndCaveats() {
            return limitationsAndCaveats;
        }

        public void setLimitationsAndCaveats(String limitationsAndCaveats) {
            this.limitationsAndCaveats = limitationsAndCaveats;
        }

        public boolean isPromptInjectionDetected() {
            return promptInjectionDetected;
        }

        public void setPromptInjectionDetected(boolean promptInjectionDetected) {
            this.promptInjectionDetected = promptInjectionDetected;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public Integer getTokensUsed() {
            return tokensUsed;
        }

        public void setTokensUsed(Integer tokensUsed) {
            this.tokensUsed = tokensUsed;
        }

        public Long getGenerationTimeMs() {
            return generationTimeMs;
        }

        public void setGenerationTimeMs(Long generationTimeMs) {
            this.generationTimeMs = generationTimeMs;
        }
    }

    public static class AnalystSignoffDto {
        private String certificationStatement;
        private String certifyingAnalyst;
        private Instant signoffTimestamp;
        private String notes;

        public AnalystSignoffDto() {
        }

        public String getCertificationStatement() {
            return certificationStatement;
        }

        public void setCertificationStatement(String certificationStatement) {
            this.certificationStatement = certificationStatement;
        }

        public String getCertifyingAnalyst() {
            return certifyingAnalyst;
        }

        public void setCertifyingAnalyst(String certifyingAnalyst) {
            this.certifyingAnalyst = certifyingAnalyst;
        }

        public Instant getSignoffTimestamp() {
            return signoffTimestamp;
        }

        public void setSignoffTimestamp(Instant signoffTimestamp) {
            this.signoffTimestamp = signoffTimestamp;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
