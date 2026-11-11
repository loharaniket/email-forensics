import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import {
  getInvestigationDetail,
  getEvidenceHeaders,
  getEvidenceHops,
  getEvidenceUrls,
  getEvidenceInfrastructure,
  getEvidenceVisuals,
  getEvidenceImages,
  getEvidenceQrCodes,
  getEvidenceContentAnalysis,
  getEvidenceRiskBreakdown,
  getEvidenceCorrelations,
  getEvidenceAiAssessment,
  generateEvidenceAiAssessment,
  downloadOriginalEvidence,
  checkEvidenceIntegrity,
  getEvidenceStatus,
  retryEvidenceAnalysis
} from '../api/evidence'
import { getEvidenceReport } from '../api/report'

import InvestigationHeader from '../components/investigation/InvestigationHeader'
import InvestigationSummary from '../components/investigation/InvestigationSummary'
import SenderChecks from '../components/investigation/SenderChecks'
import MailRoute from '../components/investigation/MailRoute'
import LinksAndIps from '../components/investigation/LinksAndIps'
import AttachmentsAndImages from '../components/investigation/AttachmentsAndImages'
import MessageContent from '../components/investigation/MessageContent'
import RiskFindings from '../components/investigation/RiskFindings'
import RelatedCases from '../components/investigation/RelatedCases'
import AutomatedAssessment from '../components/investigation/AutomatedAssessment'
import InvestigationReport from '../components/investigation/InvestigationReport'

import {
  Mail,
  ShieldAlert,
  Server,
  FileText,
  Paperclip,
  Image as ImageIcon,
  Activity,
  Link2,
  Network,
  QrCode,
  Brain,
  Sparkles,
  Layers,
  FileCheck,
  RefreshCw,
  AlertTriangle,
  ArrowLeft
} from 'lucide-react'

const STAGE_LABELS = {
  QUEUED: { label: 'In queue for analysis', step: 1, desc: 'Evidence stored. Initializing analysis run...' },
  PARSING_EMAIL: { label: 'Reading email structure', step: 2, desc: 'Extracting headers, message body, and attachments...' },
  ANALYZING_HEADERS: { label: 'Checking email headers', step: 3, desc: 'Checking SPF, DKIM, DMARC, and delivery servers...' },
  EXTRACTING_INDICATORS: { label: 'Extracting links and IP addresses', step: 4, desc: 'Extracting URLs, analyzing link destinations, and classifying IP addresses...' },
  ANALYZING_VISUALS: { label: 'Analyzing images and QR codes', step: 5, desc: 'Extracting embedded images and reading QR codes...' },
  ANALYZING_CONTENT: { label: 'Analyzing message content', step: 6, desc: 'Scanning for suspicious requests, urgent language, and phishing indicators...' },
  CORRELATING_THREATS: { label: 'Checking related cases', step: 7, desc: 'Matching indicators against other cases in the local database...' },
  ASSESSING_RISK: { label: 'Calculating risk score', step: 8, desc: 'Calculating overall risk score and evidence confidence...' },
  AI_ASSESSMENT: { label: 'Generating automated assessment', step: 9, desc: 'Producing an explanation based on technical evidence...' },
  FINALIZING: { label: 'Completing analysis', step: 10, desc: 'Saving investigation results...' },
  COMPLETED: { label: 'Analysis complete', step: 10, desc: 'All investigation steps completed successfully.' }
}

// Helper to extract clean parsed email fields from backend response
function normalizeParsedEmail(detail, hops, reportData, infrastructure) {
  const parsed = detail?.parsedEmail || {}
  const emailDate = parsed.sentDate || parsed.rawDate || reportData?.messageSummary?.sentDate || null
  const originatingIp =
    hops?.[0]?.hopIp ||
    hops?.[0]?.ip ||
    reportData?.headerForensics?.firstOriginatingIp ||
    infrastructure?.[0]?.ipAddress ||
    null

  return {
    ...parsed,
    subject: parsed.subject || '(No subject)',
    from: parsed.from || '—',
    returnPath: parsed.returnPath || '—',
    replyTo: parsed.replyTo || [],
    messageId: parsed.messageId || '—',
    sentDate: emailDate,
    rawDate: parsed.rawDate || emailDate,
    originatingIp,
    to: parsed.to || [],
    cc: parsed.cc || [],
    extractedText: parsed.extractedText || '',
    plainTextBody: parsed.plainTextBody || '',
    htmlBody: parsed.htmlBody || '',
    attachments: parsed.attachments || [],
    inlineImages: parsed.inlineImages || []
  }
}

export default function InvestigationPage() {
  const { emailId } = useParams()
  const navigate = useNavigate()

  const [detail, setDetail] = useState(null)
  const [pipelineStatus, setPipelineStatus] = useState(null)
  const [isRetrying, setIsRetrying] = useState(false)
  const [headerAnalysis, setHeaderAnalysis] = useState(null)
  const [hops, setHops] = useState([])
  const [urls, setUrls] = useState([])
  const [infrastructure, setInfrastructure] = useState([])
  const [visuals, setVisuals] = useState([])
  const [qrCodes, setQrCodes] = useState([])
  const [contentAnalysis, setContentAnalysis] = useState(null)
  const [riskBreakdown, setRiskBreakdown] = useState(null)
  const [correlations, setCorrelations] = useState([])
  const [aiAssessment, setAiAssessment] = useState(null)
  const [isAiGenerating, setIsAiGenerating] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [activeTab, setActiveTab] = useState('overview')
  const [copiedKey, setCopiedKey] = useState(null)
  const [reportData, setReportData] = useState(null)
  const [integrityCheckResult, setIntegrityCheckResult] = useState(null)
  const [isVerifyingIntegrity, setIsVerifyingIntegrity] = useState(false)

  const loadInvestigationData = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const [
        detailData,
        headersData,
        hopsData,
        urlsData,
        infraData,
        visualsData,
        qrData,
        contentData,
        riskData,
        correlationsData,
        aiData,
        reportResult
      ] = await Promise.all([
        getInvestigationDetail(emailId),
        getEvidenceHeaders(emailId).catch(() => null),
        getEvidenceHops(emailId).catch(() => []),
        getEvidenceUrls(emailId).catch(() => []),
        getEvidenceInfrastructure(emailId).catch(() => []),
        getEvidenceImages(emailId).catch(() => []),
        getEvidenceQrCodes(emailId).catch(() => []),
        getEvidenceContentAnalysis(emailId).catch(() => null),
        getEvidenceRiskBreakdown(emailId).catch(() => null),
        getEvidenceCorrelations(emailId).catch(() => []),
        getEvidenceAiAssessment(emailId).catch(() => null),
        getEvidenceReport(emailId).catch(() => null)
      ])
      setDetail(detailData)
      setHeaderAnalysis(headersData)
      setHops(hopsData || [])
      setUrls(urlsData || [])
      setInfrastructure(infraData || [])
      setVisuals(visualsData || [])
      setQrCodes(qrData || [])
      setContentAnalysis(contentData)
      setRiskBreakdown(riskData)
      setCorrelations(correlationsData || [])
      setAiAssessment(aiData)
      setReportData(reportResult)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load email investigation details.')
    } finally {
      setIsLoading(false)
    }
  }, [emailId])

  useEffect(() => {
    let intervalId = null
    let isCancelled = false

    const checkStatusAndProceed = async () => {
      try {
        const statusResp = await getEvidenceStatus(emailId)
        if (isCancelled) return
        setPipelineStatus(statusResp)

        if (statusResp.status === 'COMPLETED' || statusResp.status === 'PARTIAL') {
          loadInvestigationData()
        } else if (statusResp.status === 'FAILED') {
          setIsLoading(false)
          setError(statusResp.safeErrorMessage || 'Email analysis failed.')
        } else if (statusResp.status === 'QUEUED' || statusResp.status === 'RUNNING') {
          intervalId = setInterval(async () => {
            try {
              const pollResp = await getEvidenceStatus(emailId)
              if (isCancelled) return
              setPipelineStatus(pollResp)

              if (pollResp.status === 'COMPLETED' || pollResp.status === 'PARTIAL') {
                clearInterval(intervalId)
                loadInvestigationData()
              } else if (pollResp.status === 'FAILED') {
                clearInterval(intervalId)
                setIsLoading(false)
                setError(pollResp.safeErrorMessage || 'Email analysis failed.')
              }
            } catch (pollErr) {
              console.error('Error polling analysis status:', pollErr)
            }
          }, 1500)
        }
      } catch (err) {
        if (isCancelled) return
        setError(err.response?.data?.message || 'Failed to retrieve evidence status.')
        setIsLoading(false)
      }
    }

    checkStatusAndProceed()

    return () => {
      isCancelled = true
      if (intervalId) clearInterval(intervalId)
    }
  }, [emailId, loadInvestigationData])

  const handleRetryAnalysis = async () => {
    setIsRetrying(true)
    setError(null)
    try {
      const resp = await retryEvidenceAnalysis(emailId)
      setPipelineStatus(resp)
      setIsLoading(true)

      const pollInterval = setInterval(async () => {
        try {
          const pollResp = await getEvidenceStatus(emailId)
          setPipelineStatus(pollResp)
          if (pollResp.status === 'COMPLETED' || pollResp.status === 'PARTIAL') {
            clearInterval(pollInterval)
            setIsRetrying(false)
            loadInvestigationData()
          } else if (pollResp.status === 'FAILED') {
            clearInterval(pollInterval)
            setIsRetrying(false)
            setIsLoading(false)
            setError(pollResp.safeErrorMessage || 'Retry analysis failed.')
          }
        } catch (e) {
          console.error(e)
        }
      }, 1500)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to restart analysis.')
      setIsRetrying(false)
    }
  }

  const handleVerifyIntegrity = async () => {
    setIsVerifyingIntegrity(true)
    try {
      const result = await checkEvidenceIntegrity(emailId)
      setIntegrityCheckResult(result)
    } catch (err) {
      console.error(err)
    } finally {
      setIsVerifyingIntegrity(false)
    }
  }

  const handleGenerateAiAssessment = async (force = true) => {
    setIsAiGenerating(true)
    try {
      const result = await generateEvidenceAiAssessment(emailId, force)
      setAiAssessment(result)
    } catch (err) {
      console.error(err)
    } finally {
      setIsAiGenerating(false)
    }
  }

  const copyValue = (key, text) => {
    navigator.clipboard.writeText(text)
    setCopiedKey(key)
    setTimeout(() => setCopiedKey(null), 2000)
  }

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const formatSeconds = (sec) => {
    if (sec == null) return null
    if (sec < 60) return `${sec}s`
    const mins = Math.floor(sec / 60)
    const rem = sec % 60
    return rem > 0 ? `${mins}m ${rem}s (${sec}s)` : `${mins}m (${sec}s)`
  }

  // Loading / Queued / Running Pipeline State
  if (pipelineStatus?.status === 'QUEUED' || pipelineStatus?.status === 'RUNNING') {
    const stageInfo = STAGE_LABELS[pipelineStatus.currentStage] || {
      label: pipelineStatus.currentStage || 'Analyzing email',
      step: 1,
      desc: 'Processing investigation stages in sequence...'
    }

    return (
      <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
        <Navbar />
        <main className="flex-1 max-w-xl w-full mx-auto p-6 flex flex-col items-center justify-center">
          <div className="w-full bg-white border border-[#E4E7EC] rounded-xl p-8 space-y-6">
            <div className="flex items-center justify-between border-b border-[#E4E7EC] pb-4">
              <div>
                <h2 className="text-base font-bold text-[#202938]">Email analysis</h2>
                <p className="text-xs text-[#667085]">Analyzing email evidence in order...</p>
              </div>
              <span className="px-2.5 py-1 rounded-md text-xs font-medium bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
                {pipelineStatus.status === 'RUNNING' ? 'Analyzing' : 'In queue'}
              </span>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-[#202938]">{stageInfo.label}</span>
                <span className="text-[#667085]">Step {stageInfo.step} of 10</span>
              </div>
              <div className="h-2 w-full bg-[#F5F6F8] rounded-full overflow-hidden border border-[#E4E7EC]">
                <div
                  className="h-full bg-[#2563EB] rounded-full transition-all duration-500"
                  style={{ width: `${Math.max(10, (stageInfo.step / 10) * 100)}%` }}
                />
              </div>
              <p className="text-xs text-[#667085] leading-relaxed">{stageInfo.desc}</p>
            </div>

            <div className="flex flex-col items-center justify-center gap-2 pt-2 text-xs text-[#667085]">
              <div className="flex items-center gap-2">
                <RefreshCw className="h-4 w-4 animate-spin text-[#2563EB]" />
                <span>Processing indicators...</span>
              </div>
              <button
                type="button"
                onClick={handleRetryAnalysis}
                disabled={isRetrying}
                className="mt-2 text-xs text-[#2563EB] hover:underline cursor-pointer"
              >
                {isRetrying ? 'Restarting analysis...' : 'Taking too long? Restart analysis'}
              </button>
            </div>
          </div>
        </main>
      </div>
    )
  }

  // Error / Pipeline Halting State
  if (pipelineStatus?.status === 'FAILED' || error || (!isLoading && !detail)) {
    return (
      <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
        <Navbar />
        <main className="flex-1 max-w-xl w-full mx-auto p-6 flex flex-col items-center justify-center">
          <div className="w-full bg-white border border-[#E4E7EC] rounded-xl p-8 text-center space-y-5">
            <div className="inline-flex p-3 rounded-full bg-[#FEF2F2] text-[#B42318]">
              <AlertTriangle className="h-8 w-8" />
            </div>
            <div className="space-y-1.5">
              <h2 className="text-lg font-bold text-[#202938]">Analysis failed</h2>
              <p className="text-sm text-[#667085] max-w-md mx-auto leading-relaxed">
                {pipelineStatus?.safeErrorMessage || error || 'An error occurred during email analysis.'}
              </p>
            </div>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-3">
              <button
                id="btn-retry-analysis"
                onClick={handleRetryAnalysis}
                disabled={isRetrying}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg bg-[#2563EB] hover:bg-blue-700 text-white text-sm font-medium transition-colors cursor-pointer disabled:opacity-50"
              >
                <RefreshCw className={`h-4 w-4 ${isRetrying ? 'animate-spin' : ''}`} />
                <span>{isRetrying ? 'Retrying...' : 'Retry analysis'}</span>
              </button>
              <Link
                to="/"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-lg bg-white hover:bg-slate-50 border border-[#E4E7EC] text-sm text-[#202938] font-medium transition-colors"
              >
                <ArrowLeft className="h-4 w-4 text-[#667085]" />
                <span>Back to dashboard</span>
              </Link>
            </div>
          </div>
        </main>
      </div>
    )
  }

  // Loading State
  if (isLoading || !detail) {
    return (
      <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center gap-3">
          <RefreshCw className="h-8 w-8 text-[#2563EB] animate-spin" />
          <span className="text-sm text-[#667085]">Loading analysis...</span>
        </div>
      </div>
    )
  }

  const parsed = normalizeParsedEmail(detail, hops, reportData, infrastructure)
  const attachments = parsed.attachments
  const inlineImages = parsed.inlineImages

  const currentIntegrityStatus = integrityCheckResult
    ? integrityCheckResult.intact ? 'VERIFIED' : 'MISMATCH'
    : reportData?.evidenceIntegrity?.integrityStatus || 'NOT_VERIFIED'

  const primaryHash =
    integrityCheckResult?.savedSha256 ||
    reportData?.evidenceIntegrity?.sha256Hash ||
    detail?.sha256Hash ||
    'N/A'

  const calculatedHash =
    integrityCheckResult?.recalculatedSha256 ||
    reportData?.evidenceIntegrity?.recalculatedHash ||
    (currentIntegrityStatus === 'VERIFIED' ? primaryHash : 'N/A')

  const integrityTimestamp =
    integrityCheckResult?.verifiedAt ||
    reportData?.evidenceIntegrity?.integrityCheckTimestamp

  const flags = headerAnalysis?.identityMismatchFlags || []

  return (
    <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6">
        {/* Header section */}
        <InvestigationHeader
          detail={detail}
          parsed={parsed}
          copiedKey={copiedKey}
          copyValue={copyValue}
          onPrint={() => {
            setActiveTab('report')
            setTimeout(() => window.print(), 300)
          }}
          onDownloadOriginal={() =>
            downloadOriginalEvidence(detail.evidenceId, detail.originalFilename)
          }
          formatBytes={formatBytes}
        />

        {/* Tab Navigation Bar */}
        <div
          id="investigation-tabs-nav"
          className="no-print flex flex-wrap items-center gap-1.5 border-b border-[#E4E7EC] pb-2 text-xs"
        >
          <button
            id="tab-overview"
            onClick={() => setActiveTab('overview')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'overview'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Activity className="h-3.5 w-3.5" />
            <span>Summary</span>
          </button>

          <button
            id="tab-headers"
            onClick={() => setActiveTab('headers')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'headers'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Mail className="h-3.5 w-3.5" />
            <span>Sender checks</span>
          </button>

          <button
            id="tab-hops"
            onClick={() => setActiveTab('hops')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'hops'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Server className="h-3.5 w-3.5" />
            <span>Mail route ({hops.length})</span>
          </button>

          <button
            id="tab-urls"
            onClick={() => setActiveTab('urls')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'urls'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Link2 className="h-3.5 w-3.5" />
            <span>Links ({urls.length})</span>
          </button>

          <button
            id="tab-infrastructure"
            onClick={() => setActiveTab('infrastructure')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'infrastructure'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Network className="h-3.5 w-3.5" />
            <span>IP addresses ({infrastructure.length})</span>
          </button>

          <button
            id="tab-visuals"
            onClick={() => setActiveTab('visuals')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'visuals' || activeTab === 'images'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <ImageIcon className="h-3.5 w-3.5" />
            <span>Images ({visuals.length})</span>
          </button>

          <button
            id="tab-qr"
            onClick={() => setActiveTab('qr')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'qr'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <QrCode className="h-3.5 w-3.5" />
            <span>QR codes ({qrCodes.length || visuals.reduce((acc, v) => acc + (v.qrArtifacts?.length || 0), 0)})</span>
          </button>

          <button
            id="tab-content"
            onClick={() => setActiveTab('content')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'content'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Brain className="h-3.5 w-3.5" />
            <span>Message content</span>
          </button>

          <button
            id="tab-risk"
            onClick={() => setActiveTab('risk')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'risk'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <ShieldAlert className="h-3.5 w-3.5" />
            <span>Risk score</span>
            {riskBreakdown && (
              <span className="px-1.5 py-0.2 rounded text-[11px] font-mono font-semibold bg-white border border-[#E4E7EC] text-[#202938]">
                {riskBreakdown.deterministicRiskScore}/100
              </span>
            )}
          </button>

          <button
            id="tab-correlations"
            onClick={() => setActiveTab('correlations')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'correlations'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Layers className="h-3.5 w-3.5" />
            <span>Correlations</span>
            {correlations.length > 0 && (
              <span className="px-1.5 py-0.2 rounded text-[11px] font-mono font-semibold bg-white border border-[#E4E7EC] text-[#202938]">
                {correlations.length}
              </span>
            )}
          </button>

          <button
            id="tab-ai"
            onClick={() => setActiveTab('ai')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'ai'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Sparkles className="h-3.5 w-3.5" />
            <span>Automated assessment</span>
          </button>

          <button
            id="tab-body"
            onClick={() => setActiveTab('body')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'body'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <FileText className="h-3.5 w-3.5" />
            <span>Message text</span>
          </button>

          <button
            id="tab-attachments"
            onClick={() => setActiveTab('attachments')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'attachments'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <Paperclip className="h-3.5 w-3.5" />
            <span>Attachments ({attachments.length})</span>
          </button>

          <button
            id="tab-report"
            onClick={() => setActiveTab('report')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-lg font-medium transition-colors cursor-pointer ${
              activeTab === 'report'
                ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                : 'text-[#667085] hover:text-[#202938] hover:bg-slate-100'
            }`}
          >
            <FileCheck className="h-3.5 w-3.5" />
            <span>Investigation report</span>
          </button>
        </div>

        {/* Tab Content Panes */}
        {activeTab === 'overview' && (
          <InvestigationSummary
            riskBreakdown={riskBreakdown}
            aiAssessment={aiAssessment}
            headerAnalysis={headerAnalysis}
            urls={urls}
            attachments={attachments}
            visuals={visuals}
            qrCodes={qrCodes}
            hops={hops}
            infrastructure={infrastructure}
            onNavigateTab={(tab) => setActiveTab(tab)}
          />
        )}

        {activeTab === 'headers' && (
          <SenderChecks
            headerAnalysis={headerAnalysis}
            parsed={parsed}
          />
        )}

        {activeTab === 'hops' && (
          <MailRoute
            hops={hops}
            formatSeconds={formatSeconds}
          />
        )}

        {activeTab === 'urls' && (
          <LinksAndIps
            mode="urls"
            urls={urls}
            copiedKey={copiedKey}
            copyValue={copyValue}
          />
        )}

        {activeTab === 'infrastructure' && (
          <LinksAndIps
            mode="infrastructure"
            infrastructure={infrastructure}
            copiedKey={copiedKey}
            copyValue={copyValue}
          />
        )}

        {(activeTab === 'visuals' || activeTab === 'images') && (
          <AttachmentsAndImages
            mode="visuals"
            emailId={emailId}
            visuals={visuals}
            qrCodes={qrCodes}
            copiedKey={copiedKey}
            copyValue={copyValue}
            formatBytes={formatBytes}
            onNavigateTab={(tab) => setActiveTab(tab)}
          />
        )}

        {activeTab === 'qr' && (
          <AttachmentsAndImages
            mode="qr"
            emailId={emailId}
            visuals={visuals}
            qrCodes={qrCodes}
            copiedKey={copiedKey}
            copyValue={copyValue}
            formatBytes={formatBytes}
            onNavigateTab={(tab) => setActiveTab(tab)}
          />
        )}

        {activeTab === 'attachments' && (
          <AttachmentsAndImages
            mode="attachments"
            emailId={emailId}
            attachments={attachments}
            inlineImages={inlineImages}
            copiedKey={copiedKey}
            copyValue={copyValue}
            formatBytes={formatBytes}
            onNavigateTab={(tab) => setActiveTab(tab)}
          />
        )}

        {activeTab === 'content' && (
          <MessageContent
            mode="content"
            contentAnalysis={contentAnalysis}
          />
        )}

        {activeTab === 'body' && (
          <MessageContent
            mode="body"
            parsed={parsed}
          />
        )}

        {activeTab === 'risk' && (
          <RiskFindings
            riskBreakdown={riskBreakdown}
          />
        )}

        {activeTab === 'correlations' && (
          <RelatedCases
            correlations={correlations}
          />
        )}

        {activeTab === 'ai' && (
          <AutomatedAssessment
            aiAssessment={aiAssessment}
            isAiGenerating={isAiGenerating}
            onRegenerate={() => handleGenerateAiAssessment(true)}
          />
        )}



        {activeTab === 'report' && (
          <InvestigationReport
            detail={detail}
            parsed={parsed}
            attachments={attachments}
            correlations={correlations}
            reportData={reportData}
            riskBreakdown={riskBreakdown}
            headerAnalysis={headerAnalysis}
            hops={hops}
            urls={urls}
            infrastructure={infrastructure}
            visuals={visuals}
            contentAnalysis={contentAnalysis}
            aiAssessment={aiAssessment}
            flags={flags}
            emailId={emailId}
            isVerifyingIntegrity={isVerifyingIntegrity}
            handleVerifyIntegrity={handleVerifyIntegrity}
            currentIntegrityStatus={currentIntegrityStatus}
            primaryHash={primaryHash}
            calculatedHash={calculatedHash}
            integrityTimestamp={integrityTimestamp}
            formatBytes={formatBytes}
            formatSeconds={formatSeconds}
          />
        )}
      </main>
    </div>
  )
}
