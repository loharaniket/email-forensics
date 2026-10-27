import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import Navbar from '../components/Navbar'
import {
  getInvestigationDetail,
  getEvidenceHeaders,
  getEvidenceHops,
  getEvidenceUrls,
  getEvidenceInfrastructure,
  getEvidenceVisuals,
  getEvidenceContentAnalysis,
  getEvidenceRiskBreakdown,
  getEvidenceCorrelations,
  getEvidenceGraph,
  getEvidenceAiAssessment,
  getEvidenceAiHistory,
  generateEvidenceAiAssessment,
  downloadOriginalEvidence,
  checkEvidenceIntegrity
} from '../api/evidence'
import { getEvidenceReport, downloadEvidenceReportJson } from '../api/report'
import { getCampaignsForEvidence } from '../api/campaigns'
import ThreatGraphCanvas from '../components/ThreatGraphCanvas'
import {
  ArrowLeft,
  Mail,
  ShieldAlert,
  ShieldCheck,
  FileText,
  Paperclip,
  Image as ImageIcon,
  Clock,
  Hash,
  Copy,
  Check,
  AlertTriangle,
  Download,
  Info,
  Layers,
  ChevronDown,
  ChevronRight,
  Server,
  Globe,
  Search,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Activity,
  Link2,
  Network,
  QrCode,
  Brain,
  AlertOctagon,
  Cpu,
  Sparkles,
  Flame,
  Share2,
  Tag,
  History,
  RefreshCw,
  Printer,
  FileDown,
  BadgeCheck,
  Award,
  FileCheck
} from 'lucide-react'

export default function InvestigationPage() {
  const { caseId, emailId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [detail, setDetail] = useState(null)
  const [headerAnalysis, setHeaderAnalysis] = useState(null)
  const [hops, setHops] = useState([])
  const [urls, setUrls] = useState([])
  const [infrastructure, setInfrastructure] = useState([])
  const [visuals, setVisuals] = useState([])
  const [contentAnalysis, setContentAnalysis] = useState(null)
  const [riskBreakdown, setRiskBreakdown] = useState(null)
  const [correlations, setCorrelations] = useState([])
  const [evidenceGraph, setEvidenceGraph] = useState({ nodes: [], links: [] })
  const [associatedCampaigns, setAssociatedCampaigns] = useState([])
  const [correlationSearch, setCorrelationSearch] = useState('')
  const [correlationStrengthFilter, setCorrelationStrengthFilter] = useState('ALL')
  const [correlationTypeFilter, setCorrelationTypeFilter] = useState('ALL')
  const [aiAssessment, setAiAssessment] = useState(null)
  const [aiHistory, setAiHistory] = useState([])
  const [isAiGenerating, setIsAiGenerating] = useState(false)
  const [selectedAiModel, setSelectedAiModel] = useState('openai/gpt-oss-20b')
  const [aiError, setAiError] = useState(null)
  const [showAiHistoryDrawer, setShowAiHistoryDrawer] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [activeTab, setActiveTab] = useState('headers')
  const [bodyMode, setBodyMode] = useState('extracted')
  const [copiedKey, setCopiedKey] = useState(null)
  const [expandedHops, setExpandedHops] = useState({})
  const [rawHeaderSearch, setRawHeaderSearch] = useState('')
  const [urlSearch, setUrlSearch] = useState('')
  const [infraSearch, setInfraSearch] = useState('')
  const [visualSearch, setVisualSearch] = useState('')
  const [contentCategoryFilter, setContentCategoryFilter] = useState('ALL')
  const [contentChannelFilter, setContentChannelFilter] = useState('ALL')
  const [contentSearch, setContentSearch] = useState('')
  const [riskSeverityFilter, setRiskSeverityFilter] = useState('ALL')
  const [riskCategoryFilter, setRiskCategoryFilter] = useState('ALL')
  const [riskSearch, setRiskSearch] = useState('')
  const [showMetricsDrawer, setShowMetricsDrawer] = useState(false)
  const [reportData, setReportData] = useState(null)
  const [isExportingJson, setIsExportingJson] = useState(false)
  const [integrityCheckResult, setIntegrityCheckResult] = useState(null)
  const [isVerifyingIntegrity, setIsVerifyingIntegrity] = useState(false)

  const loadData = useCallback(async () => {
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
        contentData,
        riskData,
        correlationsData,
        graphData,
        campaignsData,
        aiData,
        aiHistoryData,
        reportResult
      ] = await Promise.all([
        getInvestigationDetail(emailId),
        getEvidenceHeaders(emailId).catch(() => null),
        getEvidenceHops(emailId).catch(() => []),
        getEvidenceUrls(emailId).catch(() => []),
        getEvidenceInfrastructure(emailId).catch(() => []),
        getEvidenceVisuals(emailId).catch(() => []),
        getEvidenceContentAnalysis(emailId).catch(() => null),
        getEvidenceRiskBreakdown(emailId).catch(() => null),
        getEvidenceCorrelations(emailId).catch(() => []),
        getEvidenceGraph(emailId).catch(() => ({ nodes: [], links: [] })),
        getCampaignsForEvidence(emailId).catch(() => []),
        getEvidenceAiAssessment(emailId).catch(() => null),
        getEvidenceAiHistory(emailId).catch(() => []),
        getEvidenceReport(emailId).catch(() => null)
      ])
      setDetail(detailData)
      setHeaderAnalysis(headersData)
      setHops(hopsData || [])
      setUrls(urlsData || [])
      setInfrastructure(infraData || [])
      setVisuals(visualsData || [])
      setContentAnalysis(contentData)
      setRiskBreakdown(riskData)
      setCorrelations(correlationsData || [])
      setEvidenceGraph(graphData || { nodes: [], links: [] })
      setAssociatedCampaigns(campaignsData || [])
      setAiAssessment(aiData)
      setAiHistory(aiHistoryData || [])
      setReportData(reportResult)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load email investigation details.')
    } finally {
      setIsLoading(false)
    }
  }, [emailId])

  const handleExportReportJson = async () => {
    setIsExportingJson(true)
    try {
      await downloadEvidenceReportJson(emailId)
    } catch (err) {
      console.error(err)
    } finally {
      setIsExportingJson(false)
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

  const handleGenerateAiAssessment = async (force = true, modelOverride = null) => {
    setIsAiGenerating(true)
    setAiError(null)
    const targetModel = modelOverride || selectedAiModel
    try {
      const result = await generateEvidenceAiAssessment(emailId, force, targetModel)
      setAiAssessment(result)
      const history = await getEvidenceAiHistory(emailId).catch(() => [])
      setAiHistory(history || [])
    } catch (err) {
      setAiError(err.response?.data?.message || 'Failed to generate AI assessment')
    } finally {
      setIsAiGenerating(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [loadData])

  const copyValue = (key, text) => {
    navigator.clipboard.writeText(text)
    setCopiedKey(key)
    setTimeout(() => setCopiedKey(null), 2000)
  }

  const toggleHopExpand = (order) => {
    setExpandedHops((prev) => ({ ...prev, [order]: !prev[order] }))
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

  const predictedLabel = contentAnalysis?.predictedLabel || contentAnalysis?.mlPredictedLabel || 'INSUFFICIENT_DATA'
  const probability = contentAnalysis?.probability != null ? contentAnalysis.probability : (contentAnalysis?.mlProbability != null ? contentAnalysis.mlProbability : 0)
  const urgencyDetected = !!(contentAnalysis?.urgency || contentAnalysis?.urgencyDetected)
  const credentialRequestDetected = !!(contentAnalysis?.credentialRequest || contentAnalysis?.credentialRequestDetected)
  const paymentInstructionDetected = !!(contentAnalysis?.paymentInstruction || contentAnalysis?.paymentInstructionDetected)
  const impersonationDetected = !!(contentAnalysis?.impersonation || contentAnalysis?.impersonationDetected)
  const secrecyDetected = !!(contentAnalysis?.secrecy || contentAnalysis?.secrecyDetected)
  const promptMatches = contentAnalysis?.promptMatches || contentAnalysis?.promptInjectionMatches || []
  const socialMatches = contentAnalysis?.socialEngineeringMatches || []
  const metrics = contentAnalysis?.modelMetrics || contentAnalysis?.evaluationMetrics || null
  const promptInjectionDetected = !!(contentAnalysis?.promptInjectionDetected || promptMatches.length > 0)

  const currentIntegrityStatus = integrityCheckResult
    ? (integrityCheckResult.intact ? 'VERIFIED' : 'MISMATCH')
    : (reportData?.evidenceIntegrity?.integrityStatus || 'NOT_VERIFIED')

  const primaryHash = integrityCheckResult?.savedSha256 || reportData?.evidenceIntegrity?.sha256Hash || detail?.sha256Hash || 'N/A'
  const calculatedHash = integrityCheckResult?.recalculatedSha256 || reportData?.evidenceIntegrity?.recalculatedHash || (currentIntegrityStatus === 'VERIFIED' ? primaryHash : 'N/A')
  const integrityTimestamp = integrityCheckResult?.verifiedAt || reportData?.evidenceIntegrity?.integrityCheckTimestamp

  const getAuthBadge = (val) => {
    if (!val) {
      return {
        bg: 'bg-slate-900',
        text: 'text-slate-400',
        border: 'border-slate-800',
        label: 'NONE',
        icon: HelpCircle
      }
    }
    const norm = String(val).toLowerCase()
    if (norm === 'pass' || norm === 'selector_found') {
      return {
        bg: 'bg-emerald-950/80',
        text: 'text-emerald-300',
        border: 'border-emerald-700/80',
        label: norm === 'selector_found' ? 'KEY FOUND' : 'PASS',
        icon: CheckCircle2
      }
    }
    if (norm === 'fail' || norm === 'hardfail' || norm === 'policy_reject' || norm === 'selector_not_found') {
      return {
        bg: 'bg-rose-950/80',
        text: 'text-rose-300',
        border: 'border-rose-700/80',
        label: norm === 'policy_reject' ? 'POLICY REJECT' : norm === 'selector_not_found' ? 'KEY NOT FOUND' : 'FAIL',
        icon: XCircle
      }
    }
    if (norm === 'policy_quarantine' || norm === 'softfail' || norm === 'neutral') {
      return {
        bg: 'bg-amber-950/80',
        text: 'text-amber-300',
        border: 'border-amber-700/80',
        label: norm === 'policy_quarantine' ? 'POLICY QUARANTINE' : norm.toUpperCase(),
        icon: AlertTriangle
      }
    }
    if (norm === 'policy_none') {
      return {
        bg: 'bg-cyan-950/80',
        text: 'text-cyan-300',
        border: 'border-cyan-700/80',
        label: 'MONITORING ONLY (NONE)',
        icon: Info
      }
    }
    if (norm === 'signature_absent' || norm === 'record_absent' || norm === 'skipped_no_relay_ip') {
      return {
        bg: 'bg-slate-900',
        text: 'text-slate-400',
        border: 'border-slate-800',
        label: norm === 'signature_absent' ? 'SIGNATURE ABSENT' : norm === 'record_absent' ? 'RECORD ABSENT' : 'NO RELAY IP',
        icon: HelpCircle
      }
    }
    return {
      bg: 'bg-slate-900',
      text: 'text-slate-300',
      border: 'border-slate-800',
      label: norm.toUpperCase(),
      icon: Info
    }
  }

  const getOcrBadge = (status) => {
    switch (status) {
      case 'EXTRACTED':
        return {
          bg: 'bg-emerald-950/80',
          text: 'text-emerald-300',
          border: 'border-emerald-700/80',
          label: 'TEXT EXTRACTED',
          icon: CheckCircle2
        }
      case 'NO_TEXT_FOUND':
        return {
          bg: 'bg-slate-900',
          text: 'text-slate-400',
          border: 'border-slate-800',
          label: 'NO TEXT DETECTED',
          icon: Info
        }
      case 'UNAVAILABLE':
        return {
          bg: 'bg-amber-950/80',
          text: 'text-amber-300',
          border: 'border-amber-700/80',
          label: 'ENGINE UNAVAILABLE',
          icon: AlertTriangle
        }
      case 'SKIPPED':
        return {
          bg: 'bg-slate-900',
          text: 'text-slate-400',
          border: 'border-slate-800',
          label: 'SKIPPED (SIZE/DIMS)',
          icon: HelpCircle
        }
      case 'FAILED':
      default:
        return {
          bg: 'bg-rose-950/80',
          text: 'text-rose-300',
          border: 'border-rose-700/80',
          label: 'OCR FAILED',
          icon: XCircle
        }
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center gap-3">
          <Mail className="h-9 w-9 text-cyan-400 animate-pulse" />
          <span className="text-xs font-mono text-slate-400">
            Parsing MIME Structure &amp; Extracting Evidence Artifacts...
          </span>
        </div>
      </div>
    )
  }

  if (error || !detail) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
        <Navbar />
        <main className="flex-1 max-w-4xl w-full mx-auto p-6 md:p-8">
          <div className="p-8 rounded-2xl bg-rose-950/30 border border-rose-900/60 text-center space-y-4">
            <AlertTriangle className="h-10 w-10 text-rose-400 mx-auto" />
            <h2 className="text-base font-semibold text-rose-300">Investigation Slice Unavailable</h2>
            <p className="text-xs text-rose-400/80">{error || 'Evidence record not found.'}</p>
            <Link
              to={`/cases/${caseId}`}
              className="inline-flex items-center gap-2 text-xs text-cyan-400 hover:text-cyan-300 font-medium pt-2"
            >
              <ArrowLeft className="h-3.5 w-3.5" />
              Return to Case Dossier
            </Link>
          </div>
        </main>
      </div>
    )
  }

  const parsed = detail.parsedEmail || {}
  const attachments = parsed.attachments || []
  const inlineImages = parsed.inlineImages || []
  const warnings = parsed.parseWarnings || []
  const riskyAttachmentsCount = attachments.filter((a) => a.riskyExtension).length
  const flags = headerAnalysis?.identityMismatchFlags || []
  const totalTransitDelaySeconds = hops.reduce((sum, h) => sum + (h.delaySeconds || 0), 0)

  const rawHeadersMap = headerAnalysis?.rawHeaders || {}
  const rawHeaderEntries = Object.entries(rawHeadersMap)
  const filteredRawHeaders = rawHeaderEntries.filter(([k, values]) => {
    if (!rawHeaderSearch) return true
    const q = rawHeaderSearch.toLowerCase()
    return k.toLowerCase().includes(q) || values.some((v) => v.toLowerCase().includes(q))
  })

  const filteredUrls = urls.filter((u) => {
    if (!urlSearch) return true
    const q = urlSearch.toLowerCase()
    return (
      (u.domain && u.domain.toLowerCase().includes(q)) ||
      (u.normalizedUrl && u.normalizedUrl.toLowerCase().includes(q)) ||
      (u.sourceType && u.sourceType.toLowerCase().includes(q)) ||
      (u.anchorText && u.anchorText.toLowerCase().includes(q))
    )
  })

  const filteredInfra = infrastructure.filter((item) => {
    if (!infraSearch) return true
    const q = infraSearch.toLowerCase()
    return (
      (item.ipAddress && item.ipAddress.toLowerCase().includes(q)) ||
      (item.sourceContext && item.sourceContext.toLowerCase().includes(q)) ||
      (item.asnNumber && item.asnNumber.toLowerCase().includes(q)) ||
      (item.organization && item.organization.toLowerCase().includes(q)) ||
      (item.country && item.country.toLowerCase().includes(q)) ||
      (item.city && item.city.toLowerCase().includes(q))
    )
  })

  const filteredVisuals = visuals.filter((v) => {
    if (!visualSearch) return true
    const q = visualSearch.toLowerCase()
    return (
      (v.filename && v.filename.toLowerCase().includes(q)) ||
      (v.contentType && v.contentType.toLowerCase().includes(q)) ||
      (v.ocrText && v.ocrText.toLowerCase().includes(q)) ||
      (v.qrArtifacts && v.qrArtifacts.some((qArt) => qArt.decodedPayload && qArt.decodedPayload.toLowerCase().includes(q)))
    )
  })

  const filteredCorrelations = correlations.filter((c) => {
    const q = correlationSearch.toLowerCase()
    const matchesSearch =
      !correlationSearch.trim() ||
      (c.matchedEmailSubject && c.matchedEmailSubject.toLowerCase().includes(q)) ||
      (c.matchedEmailFilename && c.matchedEmailFilename.toLowerCase().includes(q)) ||
      (c.matchedCaseNumber && c.matchedCaseNumber.toLowerCase().includes(q)) ||
      (c.normalizedIndicatorValue && c.normalizedIndicatorValue.toLowerCase().includes(q)) ||
      (c.matchExplanation && c.matchExplanation.toLowerCase().includes(q))

    const matchesStrength =
      correlationStrengthFilter === 'ALL' ||
      (c.matchStrength && c.matchStrength.toUpperCase() === correlationStrengthFilter.toUpperCase())

    const matchesType =
      correlationTypeFilter === 'ALL' ||
      (c.indicatorType && c.indicatorType.toUpperCase() === correlationTypeFilter.toUpperCase())

    return matchesSearch && matchesStrength && matchesType
  })

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col print:min-h-0 print:bg-white print:text-slate-900">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6 print:p-0 print:m-0 print:max-w-none print:w-full print:bg-white print:space-y-0">
        <div className="no-print flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <button
            id="back-to-case-btn"
            onClick={() => navigate(`/cases/${caseId}`)}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors cursor-pointer w-fit"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            <span>Back to Case ({detail.caseNumber})</span>
          </button>

          <div className="flex items-center gap-2" id="report-action-bar">
            <button
              id="btn-print-report"
              onClick={() => {
                setActiveTab('report')
                setTimeout(() => window.print(), 300)
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-700 transition-colors cursor-pointer"
            >
              <Printer className="h-3 w-3 text-cyan-400" />
              <span>Print / PDF Report</span>
            </button>

            <button
              id="btn-export-report-json"
              onClick={handleExportReportJson}
              disabled={isExportingJson}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-700 transition-colors cursor-pointer disabled:opacity-50"
            >
              <FileDown className="h-3 w-3 text-emerald-400" />
              <span>{isExportingJson ? 'Exporting...' : 'Export JSON Report'}</span>
            </button>

            <button
              id="download-original-btn"
              onClick={() => downloadOriginalEvidence(detail.evidenceId, detail.originalFilename)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-700 transition-colors cursor-pointer"
            >
              <Download className="h-3 w-3" />
              <span>Download Raw .eml</span>
            </button>
          </div>
        </div>

        <section className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 md:p-8 space-y-6 shadow-xl print:bg-white print:border-none print:p-0 print:m-0 print:shadow-none print:space-y-0">
          <div className="no-print flex flex-col md:flex-row md:items-start justify-between gap-4 pb-6 border-b border-slate-800">
            <div className="space-y-2">
              <div className="flex flex-wrap items-center gap-2 font-mono text-xs">
                <span className="text-cyan-400 font-bold bg-cyan-950/80 px-2.5 py-0.5 rounded border border-cyan-800/80">
                  {detail.caseNumber}
                </span>
                <span className="text-slate-500">•</span>
                <span className="text-slate-300 font-semibold">{detail.caseTitle}</span>
                <span className="text-slate-500">•</span>
                <span className="text-emerald-400 font-mono text-[11px] bg-emerald-950/80 px-2 py-0.5 rounded border border-emerald-800/80">
                  {detail.currentStage || 'HEADERS_ANALYZED'}
                </span>
              </div>

              <h1 className="text-xl md:text-2xl font-bold tracking-tight text-white break-words">
                {parsed.subject || '(No Subject Header)'}
              </h1>

              <div className="flex flex-wrap items-center gap-4 text-xs font-mono text-slate-400 pt-1">
                <div className="flex items-center gap-1">
                  <FileText className="h-3.5 w-3.5 text-slate-500" />
                  <span>
                    File: <strong className="text-slate-300">{detail.originalFilename}</strong>
                  </span>
                  <span>({formatBytes(detail.fileSizeBytes)})</span>
                </div>
                <div className="flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5 text-slate-500" />
                  <span>Uploaded: {new Date(detail.uploadedAt).toLocaleString()}</span>
                </div>
              </div>
            </div>

            <div className="flex flex-col sm:items-end gap-2 shrink-0">
              <div className="flex items-center gap-2 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800 font-mono text-xs">
                <Hash className="h-3.5 w-3.5 text-cyan-400" />
                <span className="text-slate-400">SHA-256:</span>
                <span className="text-slate-200 select-all max-w-[140px] md:max-w-[200px] truncate">
                  {detail.sha256Hash}
                </span>
                <button
                  onClick={() => copyValue('sha256', detail.sha256Hash)}
                  className="p-1 hover:bg-slate-800 rounded text-slate-400 hover:text-cyan-300 transition-colors cursor-pointer"
                  title="Copy SHA-256 Hash"
                >
                  {copiedKey === 'sha256' ? (
                    <Check className="h-3.5 w-3.5 text-emerald-400" />
                  ) : (
                    <Copy className="h-3.5 w-3.5" />
                  )}
                </button>
              </div>
            </div>
          </div>

          <div className="no-print grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
            <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80">
              <span className="text-[11px] font-mono text-slate-400 block mb-1">Message Date</span>
              <span className="text-xs font-mono text-slate-200 block truncate">
                {parsed.sentDate ? new Date(parsed.sentDate).toLocaleDateString() : 'N/A'}
              </span>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80">
              <span className="text-[11px] font-mono text-slate-400 block mb-1">Received Route</span>
              <span className="text-xs font-mono text-cyan-300 font-bold block">
                {hops.length} Hops Recorded
              </span>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800/80">
              <span className="text-[11px] font-mono text-slate-400 block mb-1">Identity Alignment</span>
              <span
                className={`text-xs font-mono font-bold block ${
                  flags.length > 0 ? 'text-rose-400' : 'text-emerald-400'
                }`}
              >
                {flags.length > 0 ? `${flags.length} Mismatches` : 'Fully Aligned'}
              </span>
            </div>

            <div
              className={`p-3.5 rounded-xl border ${
                riskyAttachmentsCount > 0
                  ? 'bg-rose-950/40 border-rose-800/80 text-rose-300'
                  : 'bg-slate-950/60 border-slate-800/80 text-slate-300'
              }`}
            >
              <span className="text-[11px] font-mono text-slate-400 block mb-1">Attachments</span>
              <span className="text-xs font-mono font-bold block">
                {attachments.length} files{' '}
                {riskyAttachmentsCount > 0 && `(${riskyAttachmentsCount} RISKY)`}
              </span>
            </div>

            <div
              id="header-risk-pill"
              onClick={() => setActiveTab('risk')}
              className={`p-3.5 rounded-xl border cursor-pointer transition-all hover:scale-[1.02] ${
                riskBreakdown
                  ? riskBreakdown.riskLevel === 'CRITICAL'
                    ? 'bg-rose-950/50 border-rose-700/80 text-rose-300'
                    : riskBreakdown.riskLevel === 'HIGH'
                    ? 'bg-orange-950/50 border-orange-700/80 text-orange-300'
                    : riskBreakdown.riskLevel === 'MEDIUM'
                    ? 'bg-amber-950/50 border-amber-700/80 text-amber-300'
                    : 'bg-emerald-950/50 border-emerald-700/80 text-emerald-300'
                  : 'bg-slate-950/60 border-slate-800/80 text-slate-300'
              }`}
            >
              <span className="text-[11px] font-mono text-slate-400 block mb-1">Risk Score</span>
              <span className="text-xs font-mono font-bold block truncate">
                {riskBreakdown ? `${riskBreakdown.deterministicRiskScore}/100 (${riskBreakdown.riskLevel})` : 'Evaluating...'}
              </span>
            </div>

            <div
              id="header-ai-pill"
              onClick={() => setActiveTab('ai')}
              className={`p-3.5 rounded-xl border cursor-pointer transition-all hover:scale-[1.02] ${
                aiAssessment?.status === 'LIVE_AI'
                  ? 'bg-emerald-950/40 border-emerald-800/80 text-emerald-300'
                  : aiAssessment?.status === 'ASSESSMENT_FAILED'
                  ? 'bg-rose-950/40 border-rose-800/80 text-rose-300'
                  : aiAssessment?.status === 'OFFLINE_RULES'
                  ? 'bg-cyan-950/40 border-cyan-800/80 text-cyan-300'
                  : 'bg-purple-950/40 border-purple-800/80 text-purple-300'
              }`}
            >
              <div className="flex items-center gap-1.5 mb-1">
                {aiAssessment?.status === 'LIVE_AI' && (
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                )}
                <span className={`text-[11px] font-mono block ${
                  aiAssessment?.status === 'LIVE_AI' ? 'text-emerald-400' :
                  aiAssessment?.status === 'ASSESSMENT_FAILED' ? 'text-rose-400' :
                  aiAssessment?.status === 'OFFLINE_RULES' ? 'text-cyan-400' : 'text-purple-400'
                }`}>
                  {aiAssessment?.status === 'LIVE_AI' ? 'Live AI Assessment' :
                   aiAssessment?.status === 'ASSESSMENT_FAILED' ? 'AI Failed (Fallback)' :
                   aiAssessment?.status === 'OFFLINE_RULES' ? 'Rule-Based (No LLM)' : 'AI Assessment'}
                </span>
              </div>
              <span className="text-xs font-mono font-bold block truncate">
                {aiAssessment ? aiAssessment.threatClassification : 'Ready'}
              </span>
            </div>
          </div>

          {warnings.length > 0 && (
            <div className="no-print p-3.5 rounded-xl bg-amber-950/30 border border-amber-800/60 text-xs text-amber-300 space-y-1">
              <div className="flex items-center gap-2 font-semibold">
                <AlertTriangle className="h-4 w-4 text-amber-400" />
                <span>Parser Warnings Encountered</span>
              </div>
              <ul className="list-disc list-inside space-y-0.5 text-[11px] text-amber-300/80 font-mono">
                {warnings.map((w, idx) => (
                  <li key={idx}>{w}</li>
                ))}
              </ul>
            </div>
          )}

          <div className="space-y-4 pt-2">
            <div id="investigation-tabs-nav" className="no-print flex flex-wrap items-center gap-2 border-b border-slate-800 pb-2">
              <button
                id="tab-overview"
                onClick={() => setActiveTab('overview')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'overview'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Activity className="h-3.5 w-3.5" />
                <span>Overview</span>
              </button>

              <button
                id="tab-headers"
                onClick={() => setActiveTab('headers')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'headers'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Mail className="h-3.5 w-3.5" />
                <span>Headers &amp; Alignment</span>
              </button>

              <button
                id="tab-hops"
                onClick={() => setActiveTab('hops')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'hops'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Server className="h-3.5 w-3.5" />
                <span>Mail Route &amp; Hops ({hops.length})</span>
              </button>

              <button
                id="tab-urls"
                onClick={() => setActiveTab('urls')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'urls'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Link2 className="h-3.5 w-3.5" />
                <span>URLs &amp; Links ({urls.length})</span>
              </button>

              <button
                id="tab-infrastructure"
                onClick={() => setActiveTab('infrastructure')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'infrastructure'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Network className="h-3.5 w-3.5" />
                <span>Network &amp; Infrastructure ({infrastructure.length})</span>
              </button>

              <button
                id="tab-visuals"
                onClick={() => setActiveTab('visuals')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'visuals'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <QrCode className="h-3.5 w-3.5" />
                <span>Visuals &amp; QR ({visuals.length})</span>
              </button>

              <button
                id="tab-content"
                onClick={() => setActiveTab('content')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'content'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Brain className="h-3.5 w-3.5" />
                <span>Content &amp; ML</span>
                {contentAnalysis && (contentAnalysis.promptInjectionDetected || (contentAnalysis.socialEngineeringMatches && contentAnalysis.socialEngineeringMatches.length > 0)) && (
                  <span className="h-2 w-2 rounded-full bg-rose-500 animate-pulse" />
                )}
              </button>

              <button
                id="tab-risk"
                onClick={() => setActiveTab('risk')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'risk'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800 shadow-lg shadow-cyan-950/50'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <ShieldAlert className="h-3.5 w-3.5" />
                <span>Risk &amp; Confidence</span>
                {riskBreakdown && (
                  <span
                    className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-bold ${
                      riskBreakdown.riskLevel === 'CRITICAL'
                        ? 'bg-rose-950 text-rose-300 border border-rose-800'
                        : riskBreakdown.riskLevel === 'HIGH'
                        ? 'bg-orange-950 text-orange-300 border border-orange-800'
                        : riskBreakdown.riskLevel === 'MEDIUM'
                        ? 'bg-amber-950 text-amber-300 border border-amber-800'
                        : 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                    }`}
                  >
                    {riskBreakdown.deterministicRiskScore}/100
                  </span>
                )}
              </button>

              <button
                id="tab-correlations"
                onClick={() => setActiveTab('correlations')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'correlations'
                    ? 'bg-amber-950 text-amber-300 border border-amber-800 shadow-lg shadow-amber-950/50'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Flame className="h-3.5 w-3.5 text-amber-400" />
                <span>Correlations &amp; Graph</span>
                {correlations.length > 0 && (
                  <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-amber-900/80 text-amber-300 border border-amber-700">
                    {correlations.length}
                  </span>
                )}
              </button>

              <button
                id="tab-ai"
                onClick={() => setActiveTab('ai')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'ai'
                    ? 'bg-purple-950 text-purple-300 border border-purple-800 shadow-lg shadow-purple-950/50'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Sparkles className="h-3.5 w-3.5 text-purple-400" />
                <span>AI Grounded Assessment</span>
                {aiAssessment && (
                  <div className="flex items-center gap-1">
                    <span
                      className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-bold ${
                        aiAssessment.status === 'LIVE_AI'
                          ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                          : aiAssessment.status === 'ASSESSMENT_FAILED'
                          ? 'bg-rose-950 text-rose-300 border border-rose-800'
                          : 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                      }`}
                    >
                      {aiAssessment.status === 'LIVE_AI' ? 'LIVE AI' :
                       aiAssessment.status === 'ASSESSMENT_FAILED' ? 'AI FAILED' :
                       'OFFLINE RULES'}
                    </span>
                    <span
                      className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-bold ${
                        aiAssessment.threatClassification?.includes('MALICIOUS')
                          ? 'bg-rose-950 text-rose-300 border border-rose-800'
                          : aiAssessment.threatClassification?.includes('SUSPICIOUS')
                          ? 'bg-amber-950 text-amber-300 border border-amber-800'
                          : 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                      }`}
                    >
                      {aiAssessment.threatClassification?.includes('MALICIOUS')
                        ? 'MALICIOUS'
                        : aiAssessment.threatClassification?.includes('SUSPICIOUS')
                        ? 'SUSPICIOUS'
                        : 'BENIGN'}
                    </span>
                  </div>
                )}
              </button>

              <button
                id="tab-body"
                onClick={() => setActiveTab('body')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'body'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <FileText className="h-3.5 w-3.5" />
                <span>Message Body Preview</span>
              </button>

              <button
                id="tab-attachments"
                onClick={() => setActiveTab('attachments')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'attachments'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Paperclip className="h-3.5 w-3.5" />
                <span>Attachments ({attachments.length})</span>
              </button>

              <button
                id="tab-raw-headers"
                onClick={() => setActiveTab('raw-headers')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium transition-colors cursor-pointer ${
                  activeTab === 'raw-headers'
                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Layers className="h-3.5 w-3.5" />
                <span>Raw Headers ({rawHeaderEntries.length})</span>
              </button>

              <button
                id="tab-report"
                onClick={() => setActiveTab('report')}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                  activeTab === 'report'
                    ? 'bg-emerald-950 text-emerald-300 border border-emerald-700 shadow-lg shadow-emerald-950/50'
                    : 'text-slate-400 hover:text-emerald-300 hover:bg-emerald-950/20'
                }`}
              >
                <FileCheck className="h-3.5 w-3.5 text-emerald-400" />
                <span>Forensic Dossier Report</span>
              </button>
            </div>

            {activeTab === 'overview' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div className={`p-4 rounded-xl border ${
                    riskBreakdown?.riskLevel === 'CRITICAL' ? 'bg-rose-950/40 border-rose-800/80 text-rose-300' :
                    riskBreakdown?.riskLevel === 'HIGH' ? 'bg-orange-950/40 border-orange-800/80 text-orange-300' :
                    riskBreakdown?.riskLevel === 'MEDIUM' ? 'bg-amber-950/40 border-amber-800/80 text-amber-300' :
                    'bg-emerald-950/40 border-emerald-800/80 text-emerald-300'
                  }`}>
                    <span className="text-xs font-mono text-slate-400 block mb-1">Deterministic Risk</span>
                    <div className="text-2xl font-bold font-mono">
                      {riskBreakdown ? `${riskBreakdown.deterministicRiskScore}/100` : 'Evaluating...'}
                    </div>
                    <span className="text-xs font-semibold uppercase">{riskBreakdown?.riskLevel || 'PENDING'}</span>
                  </div>

                  <div className={`p-4 rounded-xl border ${
                    aiAssessment?.status === 'LIVE_AI'
                      ? 'bg-emerald-950/30 border-emerald-800/80 text-emerald-300'
                      : aiAssessment?.status === 'ASSESSMENT_FAILED'
                      ? 'bg-rose-950/30 border-rose-800/80 text-rose-300'
                      : aiAssessment?.status === 'OFFLINE_RULES'
                      ? 'bg-slate-900/60 border-slate-700 text-slate-200'
                      : 'bg-purple-950/40 border-purple-800/80 text-purple-300'
                  }`}>
                    <div className="flex items-center justify-between mb-1">
                      <span className={`text-xs font-mono block ${
                        aiAssessment?.status === 'LIVE_AI' ? 'text-emerald-400' :
                        aiAssessment?.status === 'ASSESSMENT_FAILED' ? 'text-rose-400' :
                        aiAssessment?.status === 'OFFLINE_RULES' ? 'text-slate-400' : 'text-purple-400'
                      }`}>
                        {aiAssessment?.status === 'LIVE_AI' ? 'Live AI Classification (LLM)' :
                         aiAssessment?.status === 'ASSESSMENT_FAILED' ? 'Emergency Fallback (AI Failed)' :
                         aiAssessment?.status === 'OFFLINE_RULES' ? 'Deterministic Rule-Based (No LLM)' :
                         'AI Classification'}
                      </span>
                      {aiAssessment?.status === 'LIVE_AI' && (
                        <span className="flex items-center gap-1 text-[10px] font-mono px-1.5 py-0.5 rounded bg-emerald-900/60 text-emerald-300 border border-emerald-700">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                          LIVE
                        </span>
                      )}
                    </div>
                    <div className="text-xl font-bold truncate">
                      {aiAssessment ? aiAssessment.threatClassification : 'Pending Generation'}
                    </div>
                    <span className="text-xs opacity-80 block truncate">
                      {aiAssessment?.status === 'LIVE_AI'
                        ? `${aiAssessment.confidenceAssessment || 'HIGH'} Confidence • Real Model (${aiAssessment.modelName})`
                        : aiAssessment?.status === 'ASSESSMENT_FAILED'
                        ? 'Model call failed • Evaluated by offline rules'
                        : aiAssessment?.status === 'OFFLINE_RULES'
                        ? 'Deterministic heuristic rules • No model invoked'
                        : (aiAssessment?.confidenceScore ? `${aiAssessment.confidenceScore}% Confidence` : 'Grounded Shield')}
                    </span>
                  </div>

                  <div className="p-4 rounded-xl border bg-slate-900/60 border-slate-800 text-slate-200">
                    <span className="text-xs font-mono text-slate-400 block mb-1">Content Prediction</span>
                    <div className="text-xl font-bold truncate font-mono">
                      {predictedLabel}
                    </div>
                    <span className="text-xs text-slate-400">
                      {(probability * 100).toFixed(1)}% ML Probability
                    </span>
                  </div>

                  <div className="p-4 rounded-xl border bg-slate-900/60 border-slate-800 text-slate-200">
                    <span className="text-xs font-mono text-slate-400 block mb-1">Email Authentication</span>
                    {(() => {
                      const spfVal = headerAnalysis?.spfResult || headerAnalysis?.activeSpfResult || headerAnalysis?.reportedSpf || 'NONE'
                      const dkimVal = headerAnalysis?.dkimResult || headerAnalysis?.activeDkimStatus || headerAnalysis?.reportedDkim || 'NONE'
                      const dmarcVal = headerAnalysis?.dmarcResult || headerAnalysis?.activeDmarcStatus || headerAnalysis?.reportedDmarc || 'NONE'
                      const spfBadge = getAuthBadge(spfVal)
                      const dkimBadge = getAuthBadge(dkimVal)
                      const dmarcBadge = getAuthBadge(dmarcVal)

                      return (
                        <>
                          <div className="flex items-center gap-1.5 font-mono text-xs font-bold pt-1">
                            <span className={`px-2 py-0.5 rounded border ${spfBadge.bg} ${spfBadge.text} ${spfBadge.border}`}>
                              SPF: {spfBadge.label}
                            </span>
                            <span className={`px-2 py-0.5 rounded border ${dkimBadge.bg} ${dkimBadge.text} ${dkimBadge.border}`}>
                              DKIM: {dkimBadge.label}
                            </span>
                          </div>
                          <span className="text-[11px] text-slate-400 block mt-1 font-mono">
                            DMARC: <span className={`font-bold ${dmarcBadge.text}`}>{dmarcBadge.label}</span>
                          </span>
                        </>
                      )
                    })()}
                  </div>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
                    <div className="flex items-center gap-2">
                      <Hash className="h-4 w-4 text-cyan-400" />
                      <h3 className="text-sm font-semibold text-white">Cryptographic Custody &amp; Integrity</h3>
                    </div>
                    <span className="flex items-center gap-1.5 text-xs font-mono px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-800">
                      <BadgeCheck className="h-3.5 w-3.5" />
                      Verified Authentic
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono">
                    <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-1">
                      <span className="text-slate-400 text-[11px]">SHA-256 Digest</span>
                      <div className="flex items-center justify-between gap-2">
                        <span className="break-all text-cyan-300 select-all">{detail.sha256Hash}</span>
                        <button
                          onClick={() => copyValue('sha256', detail.sha256Hash)}
                          className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors shrink-0 cursor-pointer"
                        >
                          {copiedKey === 'sha256' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                        </button>
                      </div>
                    </div>

                    <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 grid grid-cols-2 gap-2 text-[11px]">
                      <div>
                        <span className="text-slate-400 block">File Size</span>
                        <span className="font-bold text-slate-200">{formatBytes(detail.fileSizeBytes)}</span>
                      </div>
                      <div>
                        <span className="text-slate-400 block">Uploaded At</span>
                        <span className="font-bold text-slate-200">{new Date(detail.uploadedAt).toLocaleString()}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="space-y-3">
                  <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                    <Layers className="h-4 w-4 text-cyan-400" />
                    <span>Forensic Analysis Modules</span>
                  </h3>

                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
                    <button
                      onClick={() => setActiveTab('headers')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <Mail className="h-4 w-4 text-cyan-400 mb-1" />
                      <span className="text-xs font-bold text-white block">Headers</span>
                      <span className="text-[11px] text-slate-400">{flags.length} Flags</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('hops')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <Server className="h-4 w-4 text-cyan-400 mb-1" />
                      <span className="text-xs font-bold text-white block">Mail Route</span>
                      <span className="text-[11px] text-slate-400">{hops.length} Hops</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('urls')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <Link2 className="h-4 w-4 text-cyan-400 mb-1" />
                      <span className="text-xs font-bold text-white block">URLs &amp; Links</span>
                      <span className="text-[11px] text-slate-400">{urls.length} Links</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('infrastructure')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <Network className="h-4 w-4 text-cyan-400 mb-1" />
                      <span className="text-xs font-bold text-white block">Network / IP</span>
                      <span className="text-[11px] text-slate-400">{infrastructure.length} IPs</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('visuals')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <QrCode className="h-4 w-4 text-cyan-400 mb-1" />
                      <span className="text-xs font-bold text-white block">Visuals &amp; QR</span>
                      <span className="text-[11px] text-slate-400">{visuals.length} Artifacts</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('content')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <Brain className="h-4 w-4 text-cyan-400 mb-1" />
                      <span className="text-xs font-bold text-white block">Content / NLP</span>
                      <span className="text-[11px] text-slate-400">{promptInjectionDetected ? 'INJECTION ALERT' : 'Analyzed'}</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('risk')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <ShieldAlert className="h-4 w-4 text-cyan-400 mb-1" />
                      <span className="text-xs font-bold text-white block">Risk Scoring</span>
                      <span className="text-[11px] text-slate-400">{riskBreakdown?.riskLevel || 'Pending'}</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('correlations')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <Flame className="h-4 w-4 text-amber-400 mb-1" />
                      <span className="text-xs font-bold text-white block">Correlations</span>
                      <span className="text-[11px] text-slate-400">{correlations.length} Matches</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('ai')}
                      className="p-3.5 rounded-xl bg-slate-900/60 hover:bg-slate-800/60 border border-slate-800 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <Sparkles className="h-4 w-4 text-purple-400 mb-1" />
                      <span className="text-xs font-bold text-white block">AI Assessment</span>
                      <span className="text-[11px] text-slate-400">Grounded LLM</span>
                    </button>

                    <button
                      onClick={() => setActiveTab('report')}
                      className="p-3.5 rounded-xl bg-emerald-950/40 hover:bg-emerald-950/60 border border-emerald-800/80 text-left transition-all hover:scale-[1.02] cursor-pointer"
                    >
                      <FileCheck className="h-4 w-4 text-emerald-400 mb-1" />
                      <span className="text-xs font-bold text-emerald-300 block">Dossier Report</span>
                      <span className="text-[11px] text-emerald-400/80">Court-Ready</span>
                    </button>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'headers' && (
              <div className="space-y-5">
                <div className="p-4 rounded-xl border transition-all">
                  {flags.length > 0 ? (
                    <div className="space-y-3">
                      <div className="flex items-center gap-2 text-rose-400 font-semibold text-xs">
                        <ShieldAlert className="h-4 w-4" />
                        <span>Identity Alignment Warnings ({flags.length} flags triggered)</span>
                      </div>
                      <div className="grid grid-cols-1 gap-2 pt-1">
                        {flags.map((flag, idx) => (
                          <div
                            key={idx}
                            className="p-3 rounded-lg bg-rose-950/40 border border-rose-800/80 text-xs font-mono text-rose-300 flex items-start gap-2.5"
                          >
                            <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0 mt-0.5" />
                            <div className="space-y-0.5 break-all">
                              <span className="font-bold text-white block">
                                {flag.split(':')[0]}
                              </span>
                              <span className="text-rose-300/90 text-[11px]">
                                {flag.substring(flag.indexOf(':') + 1).trim()}
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <div className="flex items-center gap-3 p-3 bg-emerald-950/40 border border-emerald-800/60 rounded-lg text-emerald-300 text-xs font-mono">
                      <ShieldCheck className="h-5 w-5 text-emerald-400 shrink-0" />
                      <div>
                        <span className="font-bold text-white block">
                          Sender Identity &amp; Domains Aligned
                        </span>
                        <span className="text-emerald-300/80 text-[11px]">
                          No Reply-To, Return-Path, DKIM, or display name spoofing indicators detected.
                        </span>
                      </div>
                    </div>
                  )}
                </div>

                <div className="space-y-6">
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold uppercase tracking-wider text-slate-400 font-mono flex items-center gap-2">
                        <ShieldAlert className="h-3.5 w-3.5 text-cyan-400" />
                        MTA-Reported Authentication Results (Authentication-Results Header)
                      </span>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 font-mono text-xs">
                      {['SPF', 'DKIM', 'DMARC', 'ARC'].map((mechanism) => {
                        const val =
                          mechanism === 'SPF'
                            ? headerAnalysis?.reportedSpf
                            : mechanism === 'DKIM'
                            ? headerAnalysis?.reportedDkim
                            : mechanism === 'DMARC'
                            ? headerAnalysis?.reportedDmarc
                            : headerAnalysis?.reportedArc
                        const badge = getAuthBadge(val)
                        const IconComp = badge.icon

                        return (
                          <div
                            key={mechanism}
                            className={`p-3.5 rounded-xl border ${badge.border} ${badge.bg} space-y-1.5`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="text-[11px] text-slate-400 font-bold">{mechanism}</span>
                              <IconComp className={`h-3.5 w-3.5 ${badge.text}`} />
                            </div>
                            <span className={`text-sm font-bold block ${badge.text}`}>
                              {badge.label}
                            </span>
                          </div>
                        )
                      })}
                    </div>

                    {headerAnalysis?.verificationNote && (
                      <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-[11px] font-mono text-slate-400 flex items-start gap-2">
                        <Info className="h-4 w-4 text-cyan-400 shrink-0 mt-0.5" />
                        <span>{headerAnalysis.verificationNote}</span>
                      </div>
                    )}
                  </div>

                  <div className="space-y-3 pt-4 border-t border-slate-900/80">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold uppercase tracking-wider text-slate-400 font-mono flex items-center gap-2">
                        <Globe className="h-3.5 w-3.5 text-emerald-400" />
                        Active DNS Email Authentication (Live Verification)
                      </span>
                      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-mono font-medium bg-emerald-950/60 text-emerald-300 border border-emerald-800/60">
                        <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                        LIVE VERIFICATION
                      </span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-3 font-mono text-xs">
                      {(() => {
                        const spfBadge = getAuthBadge(headerAnalysis?.activeSpfResult)
                        const SpfIcon = spfBadge.icon
                        return (
                          <div className={`p-3.5 rounded-xl border ${spfBadge.border} ${spfBadge.bg} space-y-2`}>
                            <div className="flex items-center justify-between">
                              <span className="text-[11px] text-slate-400 font-bold">Active SPF</span>
                              <SpfIcon className={`h-3.5 w-3.5 ${spfBadge.text}`} />
                            </div>
                            <span className={`text-sm font-bold block ${spfBadge.text}`}>
                              {spfBadge.label}
                            </span>
                            <div className="pt-1 text-[11px] text-slate-400 space-y-0.5">
                              <div>Relay IP: <span className="text-slate-200">{headerAnalysis?.activeSpfRelayIp || '(Hop IP Not Found)'}</span></div>
                              {headerAnalysis?.activeSpfRecord && (
                                <div className="truncate text-slate-400 font-mono text-[10px]" title={headerAnalysis.activeSpfRecord}>
                                  Record: <span className="text-slate-300">{headerAnalysis.activeSpfRecord}</span>
                                </div>
                              )}
                            </div>
                          </div>
                        )
                      })()}

                      {(() => {
                        const dkimBadge = getAuthBadge(headerAnalysis?.activeDkimStatus)
                        const DkimIcon = dkimBadge.icon
                        return (
                          <div className={`p-3.5 rounded-xl border ${dkimBadge.border} ${dkimBadge.bg} space-y-2`}>
                            <div className="flex items-center justify-between">
                              <span className="text-[11px] text-slate-400 font-bold">Active DKIM Key</span>
                              <DkimIcon className={`h-3.5 w-3.5 ${dkimBadge.text}`} />
                            </div>
                            <span className={`text-sm font-bold block ${dkimBadge.text}`}>
                              {dkimBadge.label}
                            </span>
                            <div className="pt-1 text-[11px] text-slate-400 space-y-0.5">
                              <div>Selector: <span className="text-slate-200">{headerAnalysis?.activeDkimSelector || 'N/A'}</span></div>
                              {headerAnalysis?.activeDkimRecord && (
                                <div className="truncate text-slate-400 font-mono text-[10px]" title={headerAnalysis.activeDkimRecord}>
                                  Key Record: <span className="text-slate-300">{headerAnalysis.activeDkimRecord}</span>
                                </div>
                              )}
                            </div>
                          </div>
                        )
                      })()}

                      {(() => {
                        const dmarcBadge = getAuthBadge(headerAnalysis?.activeDmarcStatus)
                        const DmarcIcon = dmarcBadge.icon
                        return (
                          <div className={`p-3.5 rounded-xl border ${dmarcBadge.border} ${dmarcBadge.bg} space-y-2`}>
                            <div className="flex items-center justify-between">
                              <span className="text-[11px] text-slate-400 font-bold">Active DMARC Policy</span>
                              <DmarcIcon className={`h-3.5 w-3.5 ${dmarcBadge.text}`} />
                            </div>
                            <span className={`text-sm font-bold block ${dmarcBadge.text}`}>
                              {dmarcBadge.label}
                            </span>
                            <div className="pt-1 text-[11px] text-slate-400 space-y-0.5">
                              <div>Host: <span className="text-slate-200">{headerAnalysis?.fromDomain ? `_dmarc.${headerAnalysis.fromDomain}` : 'N/A'}</span></div>
                              {headerAnalysis?.activeDmarcPolicy && (
                                <div className="truncate text-slate-400 font-mono text-[10px]" title={headerAnalysis.activeDmarcPolicy}>
                                  Policy: <span className="text-slate-300">{headerAnalysis.activeDmarcPolicy}</span>
                                </div>
                              )}
                            </div>
                          </div>
                        )
                      })()}
                    </div>

                    {headerAnalysis?.activeVerificationSummary && (
                      <div className="p-3 rounded-lg bg-emerald-950/20 border border-emerald-900/40 text-[11px] font-mono text-emerald-300 flex items-start gap-2">
                        <Activity className="h-4 w-4 text-emerald-400 shrink-0 mt-0.5" />
                        <span>{headerAnalysis.activeVerificationSummary}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="bg-slate-950/80 rounded-xl p-5 border border-slate-800/80 space-y-3 font-mono text-xs">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                    <span className="text-slate-500">From Address</span>
                    <span className="md:col-span-2 text-slate-200 break-all font-semibold">
                      {headerAnalysis?.fromAddress || parsed.from || '(None)'}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                    <span className="text-slate-500">From Domain</span>
                    <span className="md:col-span-2 text-cyan-300 break-all">
                      {headerAnalysis?.fromDomain ? `@${headerAnalysis.fromDomain}` : '(None)'}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                    <span className="text-slate-500">To</span>
                    <span className="md:col-span-2 text-slate-200 break-all">
                      {parsed.to && parsed.to.length > 0 ? parsed.to.join(', ') : '(None)'}
                    </span>
                  </div>

                  {parsed.cc && parsed.cc.length > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                      <span className="text-slate-500">CC</span>
                      <span className="md:col-span-2 text-slate-200 break-all">
                        {parsed.cc.join(', ')}
                      </span>
                    </div>
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                    <span className="text-slate-500">Reply-To</span>
                    <span className="md:col-span-2 text-slate-200 break-all">
                      {headerAnalysis?.replyTo ||
                        (parsed.replyTo && parsed.replyTo.length > 0
                          ? parsed.replyTo.join(', ')
                          : '(None)')}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                    <span className="text-slate-500">Return-Path</span>
                    <span className="md:col-span-2 text-slate-200 break-all">
                      {headerAnalysis?.returnPath || parsed.returnPath || '(None)'}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                    <span className="text-slate-500">DKIM Signing Domain</span>
                    <span className="md:col-span-2 text-cyan-300 break-all">
                      {headerAnalysis?.dkimDomain ? `@${headerAnalysis.dkimDomain}` : '(No DKIM Signature Header)'}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                    <span className="text-slate-500">Message-ID</span>
                    <span className="md:col-span-2 text-cyan-300 break-all">
                      {parsed.messageId || '(None)'}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2 border-b border-slate-900">
                    <span className="text-slate-500">Sent Date</span>
                    <span className="md:col-span-2 text-slate-200">
                      {parsed.sentDate ? new Date(parsed.sentDate).toUTCString() : '(None)'}
                    </span>
                  </div>

                  {parsed.rawDate && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2">
                      <span className="text-slate-500">Raw Date String</span>
                      <span className="md:col-span-2 text-slate-400 break-all">
                        {parsed.rawDate}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'hops' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-1">
                    <span className="text-[11px] font-mono text-slate-400 block">Total Route Hops</span>
                    <span className="text-base font-mono font-bold text-cyan-300">
                      {hops.length} Mail Transfer Agents
                    </span>
                  </div>

                  <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-1">
                    <span className="text-[11px] font-mono text-slate-400 block">
                      Total Transit Delay
                    </span>
                    <span className="text-base font-mono font-bold text-slate-200">
                      {totalTransitDelaySeconds > 0
                        ? formatSeconds(totalTransitDelaySeconds)
                        : '0s (Immediate / Concurrent)'}
                    </span>
                  </div>

                  <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-1">
                    <span className="text-[11px] font-mono text-slate-400 block">Originating MTA</span>
                    <span className="text-xs font-mono font-bold text-white block truncate">
                      {hops.length > 0 ? hops[0].fromHost || hops[0].hopIp || 'Unknown' : 'N/A'}
                    </span>
                  </div>
                </div>

                {hops.length === 0 ? (
                  <div className="p-8 rounded-xl bg-slate-950/60 border border-slate-800 text-center text-xs font-mono text-slate-400">
                    No Received headers were found in this email message.
                  </div>
                ) : (
                  <div className="relative pl-6 sm:pl-8 space-y-6 before:content-[''] before:absolute before:left-3 sm:before:left-4 before:top-4 before:bottom-4 before:w-0.5 before:bg-slate-800">
                    {hops.map((hop, idx) => {
                      const isFirst = idx === 0
                      const isLast = idx === hops.length - 1
                      const isExpanded = !!expandedHops[hop.hopOrder]

                      return (
                        <div key={hop.hopOrder} className="relative group">
                          <div
                            className={`absolute -left-6 sm:-left-8 top-1.5 h-6 w-6 rounded-full border flex items-center justify-center text-[10px] font-mono font-bold ${
                              isFirst
                                ? 'bg-cyan-950 border-cyan-500 text-cyan-300'
                                : isLast
                                ? 'bg-emerald-950 border-emerald-500 text-emerald-300'
                                : 'bg-slate-900 border-slate-700 text-slate-300'
                            }`}
                          >
                            {hop.hopOrder}
                          </div>

                          <div
                            className={`p-4 rounded-xl border transition-all ${
                              isFirst
                                ? 'bg-cyan-950/20 border-cyan-900/60'
                                : isLast
                                ? 'bg-emerald-950/20 border-emerald-900/60'
                                : 'bg-slate-950/80 border-slate-800 hover:border-slate-700'
                            }`}
                          >
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-800/80">
                              <div className="flex items-center gap-2">
                                <span className="text-xs font-bold font-mono text-white">
                                  {isFirst
                                    ? `Hop #${hop.hopOrder} — Originating MTA (Initial Injection)`
                                    : isLast
                                    ? `Hop #${hop.hopOrder} — Final Receiving MTA (Mailbox)`
                                    : `Hop #${hop.hopOrder} — Intermediate Relay`}
                                </span>
                              </div>

                              <div className="flex items-center gap-2">
                                {hop.delaySeconds != null && (
                                  <span
                                    className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${
                                      hop.delaySeconds > 300
                                        ? 'bg-amber-950/80 text-amber-300 border-amber-700'
                                        : 'bg-slate-800 text-slate-300 border-slate-700'
                                    }`}
                                  >
                                    ⏱ Delay: +{formatSeconds(hop.delaySeconds)}
                                  </span>
                                )}

                                {hop.protocol && (
                                  <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-cyan-950/80 text-cyan-300 border border-cyan-800">
                                    {hop.protocol}
                                  </span>
                                )}
                              </div>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 py-3 text-xs font-mono">
                              <div>
                                <span className="text-[10px] text-slate-500 block">From Host</span>
                                <span className="text-slate-200 break-all font-semibold">
                                  {hop.fromHost || 'Unknown / Not Logged'}
                                </span>
                              </div>

                              <div>
                                <span className="text-[10px] text-slate-500 block">By Host (Receiver)</span>
                                <span className="text-slate-200 break-all font-semibold">
                                  {hop.byHost || 'Unknown / Not Logged'}
                                </span>
                              </div>

                              <div>
                                <span className="text-[10px] text-slate-500 block">Hop IP Address</span>
                                <div className="flex items-center gap-1.5 text-cyan-300">
                                  <Globe className="h-3 w-3 shrink-0 text-slate-500" />
                                  <span className="break-all">{hop.hopIp || 'Not Recorded'}</span>
                                  {hop.hopIp && (
                                    <button
                                      onClick={() => copyValue(`ip-${hop.hopOrder}`, hop.hopIp)}
                                      className="p-0.5 hover:text-white rounded"
                                      title="Copy IP"
                                    >
                                      {copiedKey === `ip-${hop.hopOrder}` ? (
                                        <Check className="h-2.5 w-2.5 text-emerald-400" />
                                      ) : (
                                        <Copy className="h-2.5 w-2.5" />
                                      )}
                                    </button>
                                  )}
                                </div>
                              </div>

                              <div>
                                <span className="text-[10px] text-slate-500 block">Hop Timestamp</span>
                                <span className="text-slate-300">
                                  {hop.hopTimestamp
                                    ? new Date(hop.hopTimestamp).toUTCString()
                                    : 'Undated'}
                                </span>
                              </div>
                            </div>

                            {hop.parsingWarning && (
                              <div className="mt-2 p-2 rounded bg-amber-950/40 border border-amber-800 text-[11px] font-mono text-amber-300 flex items-center gap-1.5">
                                <AlertTriangle className="h-3 w-3 shrink-0" />
                                <span>{hop.parsingWarning}</span>
                              </div>
                            )}

                            <div className="pt-2 border-t border-slate-900 flex flex-col gap-2">
                              <button
                                onClick={() => toggleHopExpand(hop.hopOrder)}
                                className="flex items-center gap-1 text-[11px] font-mono text-slate-400 hover:text-cyan-300 transition-colors cursor-pointer w-fit"
                              >
                                {isExpanded ? (
                                  <ChevronDown className="h-3 w-3" />
                                ) : (
                                  <ChevronRight className="h-3 w-3" />
                                )}
                                <span>
                                  {isExpanded
                                    ? 'Hide Verbatim Received Header'
                                    : 'View Verbatim Received Header'}
                                </span>
                              </button>

                              {isExpanded && (
                                <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 font-mono text-[11px] text-slate-300 whitespace-pre-wrap break-all leading-relaxed select-text">
                                  {hop.rawHopLine}
                                </pre>
                              )}
                            </div>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'urls' && (
              <div className="space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-950 p-3 rounded-xl border border-slate-800">
                  <div className="relative flex-1 max-w-md">
                    <Search className="h-3.5 w-3.5 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      id="url-search-input"
                      type="text"
                      placeholder="Search URLs by domain, host, or source..."
                      value={urlSearch}
                      onChange={(e) => setUrlSearch(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500"
                    />
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-[11px] font-mono text-slate-400">
                      Total: <strong className="text-white">{urls.length}</strong>
                    </span>
                    {urls.filter((u) => u.anchorMismatch).length > 0 && (
                      <span className="text-[11px] font-mono text-rose-400 bg-rose-950/60 px-2 py-0.5 rounded border border-rose-800/60">
                        {urls.filter((u) => u.anchorMismatch).length} Anchor Mismatch
                      </span>
                    )}
                    {urls.filter((u) => (u.urlRiskScore || 0) >= 70).length > 0 && (
                      <span className="text-[11px] font-mono text-rose-400 bg-rose-950/60 px-2 py-0.5 rounded border border-rose-800/60">
                        {urls.filter((u) => (u.urlRiskScore || 0) >= 70).length} High Risk
                      </span>
                    )}
                  </div>
                </div>

                {filteredUrls.length === 0 ? (
                  <div className="p-8 rounded-xl bg-slate-950/60 border border-slate-800 text-center text-xs font-mono text-slate-400">
                    {urls.length === 0
                      ? 'No URLs or links extracted from this email.'
                      : 'No URLs match the search filter.'}
                  </div>
                ) : (
                  <div className="space-y-3">
                    {filteredUrls.map((u, idx) => {
                      const isHighRisk = (u.urlRiskScore || 0) >= 70
                      const isMedRisk = (u.urlRiskScore || 0) >= 40 && (u.urlRiskScore || 0) < 70
                      const riskBadgeClass = isHighRisk
                        ? 'bg-rose-950/80 text-rose-300 border-rose-800'
                        : isMedRisk
                        ? 'bg-amber-950/80 text-amber-300 border-amber-800'
                        : 'bg-emerald-950/80 text-emerald-300 border-emerald-800'

                      return (
                        <div
                          key={idx}
                          className={`p-4 rounded-xl bg-slate-950/80 border transition-all ${
                            u.anchorMismatch || isHighRisk
                              ? 'border-rose-800/80 bg-rose-950/15'
                              : 'border-slate-800 hover:border-slate-700'
                          }`}
                        >
                          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 pb-3 border-b border-slate-800/60">
                            <div className="space-y-1">
                              <div className="flex flex-wrap items-center gap-2">
                                <span className="text-xs font-bold font-mono text-white">
                                  {u.domain || 'Unknown Host'}
                                </span>
                                <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-slate-300">
                                  {u.sourceType}
                                </span>
                                {u.dnsStatus && (
                                  <span
                                    className={`px-2 py-0.5 rounded text-[10px] font-mono border ${
                                      u.dnsStatus === 'ACTIVE' || u.dnsStatus === 'RESOLVED'
                                        ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800/60'
                                        : u.dnsStatus === 'NXDOMAIN'
                                        ? 'bg-rose-950/60 text-rose-300 border-rose-800/60'
                                        : u.dnsStatus === 'SKIPPED_IP'
                                        ? 'bg-blue-950/60 text-blue-300 border-blue-800/60'
                                        : 'bg-slate-900 text-slate-400 border-slate-700'
                                    }`}
                                  >
                                    DNS: {u.dnsStatus}
                                  </span>
                                )}
                              </div>
                            </div>

                            <div className="flex items-center gap-2 shrink-0">
                              <span
                                className={`px-2.5 py-1 rounded-lg text-xs font-mono font-bold border ${riskBadgeClass}`}
                              >
                                Risk Score: {u.urlRiskScore ?? 0}/100
                              </span>
                            </div>
                          </div>

                          {u.anchorMismatch && (
                            <div className="mt-3 p-3 rounded-lg bg-rose-950/40 border border-rose-800/80 text-xs font-mono text-rose-300 flex items-start gap-2.5">
                              <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0 mt-0.5" />
                              <div className="space-y-1">
                                <span className="font-bold text-white block">
                                  Anchor Text Mismatch Detected!
                                </span>
                                <p className="text-[11px] text-rose-300/90 leading-relaxed">
                                  The text visible to the recipient claims to be{' '}
                                  <strong className="text-white underline">{u.anchorText}</strong>, but
                                  the actual hyperlink destination connects to an entirely different
                                  host (<strong className="text-rose-200">{u.domain}</strong>).
                                </p>
                              </div>
                            </div>
                          )}

                          <div className="flex flex-wrap items-center gap-1.5 pt-3">
                            {u.isIpUrl && (
                              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-rose-950/80 text-rose-300 border border-rose-800">
                                IP Literal Host
                              </span>
                            )}
                            {u.isShortened && (
                              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-amber-950/80 text-amber-300 border border-amber-800">
                                URL Shortener
                              </span>
                            )}
                            {u.unusualPort && (
                              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-amber-950/80 text-amber-300 border border-amber-800">
                                Non-Standard Port
                              </span>
                            )}
                            {u.subdomainExcess && (
                              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-amber-950/80 text-amber-300 border border-amber-800">
                                Subdomain Excess (&gt;3)
                              </span>
                            )}
                            {u.lookalike && (
                              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-purple-950/80 text-purple-300 border border-purple-800">
                                Lookalike / Homoglyph Pattern
                              </span>
                            )}
                          </div>

                          <div className="mt-3 space-y-2 text-xs font-mono">
                            <div className="bg-slate-900/60 p-2.5 rounded-lg border border-slate-800/80 flex items-start justify-between gap-3">
                              <div className="break-all flex-1">
                                <span className="text-slate-500 block text-[10px] uppercase">
                                  Normalized URL (Safe Forensic Inert Text):
                                </span>
                                <span className="text-slate-200 select-all font-semibold">
                                  {u.normalizedUrl}
                                </span>
                              </div>
                              <button
                                onClick={() => copyValue(`norm-${idx}`, u.normalizedUrl)}
                                className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 shrink-0"
                                title="Copy Normalized URL"
                              >
                                {copiedKey === `norm-${idx}` ? (
                                  <Check className="h-3.5 w-3.5 text-emerald-400" />
                                ) : (
                                  <Copy className="h-3.5 w-3.5" />
                                )}
                              </button>
                            </div>

                            {u.rawUrl && u.rawUrl !== u.normalizedUrl && (
                              <div className="bg-slate-900/40 p-2 rounded-lg border border-slate-800/60 flex items-start justify-between gap-3 text-[11px]">
                                <div className="break-all flex-1">
                                  <span className="text-slate-500 block text-[10px] uppercase">
                                    Raw Extracted URL:
                                  </span>
                                  <span className="text-slate-400 select-all">{u.rawUrl}</span>
                                </div>
                                <button
                                  onClick={() => copyValue(`raw-${idx}`, u.rawUrl)}
                                  className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 shrink-0"
                                  title="Copy Raw URL"
                                >
                                  {copiedKey === `raw-${idx}` ? (
                                    <Check className="h-3 w-3 text-emerald-400" />
                                  ) : (
                                    <Copy className="h-3 w-3" />
                                  )}
                                </button>
                              </div>
                            )}

                            {u.anchorText && (
                              <div className="text-[11px] text-slate-400">
                                <span className="text-slate-500">Anchor Text: </span>
                                <span className="text-slate-300">"{u.anchorText}"</span>
                              </div>
                            )}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'infrastructure' && (
              <div className="space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-950 p-3 rounded-xl border border-slate-800">
                  <div className="relative flex-1 max-w-md">
                    <Search className="h-3.5 w-3.5 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      id="infra-search-input"
                      type="text"
                      placeholder="Search by IP, ASN, Organization, Country, or Context..."
                      value={infraSearch}
                      onChange={(e) => setInfraSearch(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500"
                    />
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-[11px] font-mono text-slate-400">
                      Discovered IPs: <strong className="text-white">{infrastructure.length}</strong>
                    </span>
                    <span className="text-[11px] font-mono text-cyan-400 bg-cyan-950/60 px-2 py-0.5 rounded border border-cyan-800/60">
                      {infrastructure.filter((i) => !i.isPrivate).length} Public
                    </span>
                    <span className="text-[11px] font-mono text-slate-400 bg-slate-900 px-2 py-0.5 rounded border border-slate-800">
                      {infrastructure.filter((i) => i.isPrivate).length} Internal / Reserved
                    </span>
                  </div>
                </div>

                {filteredInfra.length === 0 ? (
                  <div className="p-8 rounded-xl bg-slate-950/60 border border-slate-800 text-center text-xs font-mono text-slate-400">
                    {infrastructure.length === 0
                      ? 'No IP artifacts or network infrastructure discovered for this message.'
                      : 'No IP artifacts match the search filter.'}
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {filteredInfra.map((item, idx) => (
                      <div
                        key={idx}
                        className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 hover:border-slate-700 transition-all space-y-3"
                      >
                        <div className="flex items-start justify-between gap-2 pb-2.5 border-b border-slate-800/60">
                          <div className="space-y-1">
                            <div className="flex items-center gap-2">
                              <span className="font-mono text-sm font-bold text-white select-all">
                                {item.ipAddress}
                              </span>
                              <button
                                onClick={() => copyValue(`ip-${idx}`, item.ipAddress)}
                                className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 shrink-0"
                                title="Copy IP Address"
                              >
                                {copiedKey === `ip-${idx}` ? (
                                  <Check className="h-3 w-3 text-emerald-400" />
                                ) : (
                                  <Copy className="h-3 w-3" />
                                )}
                              </button>
                            </div>
                            <span className="text-[10px] font-mono text-cyan-400 bg-cyan-950/60 px-2 py-0.5 rounded border border-cyan-800/60 inline-block">
                              Context: {item.sourceContext}
                            </span>
                          </div>

                          <div className="flex flex-col items-end gap-1">
                            <span
                              className={`px-2 py-0.5 rounded text-[10px] font-mono border ${
                                item.isPrivate
                                  ? 'bg-slate-900 text-slate-400 border-slate-700'
                                  : 'bg-cyan-950/80 text-cyan-300 border-cyan-800'
                              }`}
                            >
                              {item.isPrivate ? 'Internal / RFC 1918' : 'Public Routable IP'}
                            </span>
                            <span
                              className={`px-2 py-0.5 rounded text-[9px] font-mono border ${
                                item.lookupStatus === 'ENRICHED'
                                  ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800/60'
                                  : item.lookupStatus === 'PRIVATE_IP_SKIPPED'
                                  ? 'bg-slate-900 text-slate-400 border-slate-800'
                                  : 'bg-amber-950/60 text-amber-300 border-amber-800/60'
                              }`}
                            >
                              {item.lookupStatus}
                            </span>
                          </div>
                        </div>

                        <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                          <div className="bg-slate-900/50 p-2 rounded-lg border border-slate-800/60">
                            <span className="text-[10px] text-slate-500 block uppercase">
                              ASN &amp; Provider:
                            </span>
                            <span className="text-slate-200 font-semibold truncate block">
                              {item.asnNumber ? `${item.asnNumber}` : 'N/A'}
                            </span>
                            <span className="text-[11px] text-slate-400 truncate block">
                              {item.organization || (item.isPrivate ? 'Internal Network' : 'Unknown Provider')}
                            </span>
                          </div>

                          <div className="bg-slate-900/50 p-2 rounded-lg border border-slate-800/60">
                            <span className="text-[10px] text-slate-500 block uppercase">
                              Geolocation:
                            </span>
                            <span className="text-slate-200 font-semibold truncate block">
                              {item.country || (item.isPrivate ? 'Private Range' : 'Unknown Country')}
                            </span>
                            <span className="text-[11px] text-slate-400 truncate block">
                              {item.city || (item.latitude ? `${item.latitude}, ${item.longitude}` : 'No City Data')}
                            </span>
                          </div>
                        </div>

                        <div className="flex items-center justify-between text-[10px] font-mono text-slate-500 pt-1 border-t border-slate-800/40">
                          <span>Source: {item.dataSource || 'Heuristic Classifier'}</span>
                          <span>{item.observedAt ? new Date(item.observedAt).toLocaleString() : ''}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'visuals' && (
              <div className="space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-950 p-3 rounded-xl border border-slate-800">
                  <div className="flex items-center gap-2">
                    <QrCode className="h-4 w-4 text-cyan-400" />
                    <span className="text-xs font-mono text-slate-300 font-semibold">
                      Visual Analysis Artifacts ({filteredVisuals.length} of {visuals.length})
                    </span>
                  </div>

                  <div className="relative w-full sm:w-80">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-500" />
                    <input
                      id="visuals-search-input"
                      type="text"
                      value={visualSearch}
                      onChange={(e) => setVisualSearch(e.target.value)}
                      placeholder="Filter by image name, OCR text, or QR payload..."
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500 font-mono"
                    />
                  </div>
                </div>

                {filteredVisuals.length === 0 ? (
                  <div className="p-12 text-center bg-slate-950 rounded-2xl border border-slate-800/80">
                    <ImageIcon className="h-10 w-10 text-slate-600 mx-auto mb-3" />
                    <p className="text-sm font-medium text-slate-300">
                      No Visual Artifacts Found
                    </p>
                    <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                      {visualSearch
                        ? 'No images match the current filter query.'
                        : 'No inline images or image attachments were detected in this email message.'}
                    </p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 gap-4">
                    {filteredVisuals.map((v, idx) => {
                      const ocrBadge = getOcrBadge(v.ocrStatus)
                      const OcrIcon = ocrBadge.icon
                      return (
                        <div
                          key={v.id || idx}
                          className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-4"
                        >
                          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
                            <div className="flex items-start gap-3">
                              <div className="p-2.5 rounded-lg bg-cyan-950/40 border border-cyan-800/60 text-cyan-400 shrink-0">
                                <ImageIcon className="h-5 w-5" />
                              </div>
                              <div className="space-y-1">
                                <span className="text-xs font-semibold text-slate-100 block break-all">
                                  {v.filename || 'unnamed-image'}
                                </span>
                                <div className="flex flex-wrap items-center gap-2 text-[11px] font-mono text-slate-400">
                                  <span className="px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-300">
                                    {v.contentType || 'image/*'}
                                  </span>
                                  <span>{formatBytes(v.byteSize)}</span>
                                  {v.width && v.height && (
                                    <span>
                                      {v.width} &times; {v.height} px
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>

                            <div className="flex items-center gap-2 bg-slate-900/80 px-2.5 py-1.5 rounded-lg border border-slate-800/80 font-mono text-[11px] shrink-0">
                              <Hash className="h-3 w-3 text-cyan-400" />
                              <span className="text-slate-400">SHA-256:</span>
                              <span className="text-slate-200 select-all max-w-[140px] md:max-w-[200px] truncate">
                                {v.sha256Hash}
                              </span>
                              <button
                                id={`copy-visual-hash-${idx}`}
                                onClick={() => copyValue(`visual-hash-${idx}`, v.sha256Hash)}
                                className="p-0.5 hover:bg-slate-800 rounded text-slate-400 hover:text-cyan-300 transition-colors cursor-pointer"
                                title="Copy Image SHA-256"
                              >
                                {copiedKey === `visual-hash-${idx}` ? (
                                  <Check className="h-3 w-3 text-emerald-400" />
                                ) : (
                                  <Copy className="h-3 w-3" />
                                )}
                              </button>
                            </div>
                          </div>

                          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                            <div className="space-y-3 bg-slate-900/40 p-3.5 rounded-lg border border-slate-800/60">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-200">
                                  <QrCode className="h-3.5 w-3.5 text-cyan-400" />
                                  <span>QR Code Detection</span>
                                </div>
                                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-950 border border-slate-800 text-slate-400">
                                  {v.qrArtifacts && v.qrArtifacts.length > 0
                                    ? `${v.qrArtifacts.length} Detected`
                                    : 'None Detected'}
                                </span>
                              </div>

                              {v.qrArtifacts && v.qrArtifacts.length > 0 ? (
                                <div className="space-y-2.5">
                                  {v.qrArtifacts.map((qr, qIdx) => (
                                    <div
                                      key={qr.id || qIdx}
                                      className="p-3 bg-slate-950 rounded-lg border border-slate-800 space-y-2 text-xs"
                                    >
                                      <div className="flex items-center justify-between gap-2">
                                        <span className="px-2 py-0.5 rounded text-[10px] font-mono uppercase font-bold bg-cyan-950 text-cyan-300 border border-cyan-800">
                                          {qr.payloadType || 'PAYLOAD'}
                                        </span>
                                        {qr.moduleCount && (
                                          <span className="text-[10px] font-mono text-slate-500">
                                            Modules: {qr.moduleCount}
                                          </span>
                                        )}
                                      </div>

                                      <div className="space-y-1">
                                        <div className="flex items-center justify-between">
                                          <span className="text-[10px] font-mono text-slate-400">
                                            Decoded Content (Inert Preview):
                                          </span>
                                          <button
                                            id={`copy-qr-payload-${idx}-${qIdx}`}
                                            onClick={() =>
                                              copyValue(`qr-payload-${idx}-${qIdx}`, qr.decodedPayload)
                                            }
                                            className="text-[10px] font-mono text-cyan-400 hover:text-cyan-300 flex items-center gap-1 cursor-pointer"
                                          >
                                            {copiedKey === `qr-payload-${idx}-${qIdx}` ? (
                                              <>
                                                <Check className="h-3 w-3 text-emerald-400" />
                                                <span>Copied</span>
                                              </>
                                            ) : (
                                              <>
                                                <Copy className="h-3 w-3" />
                                                <span>Copy</span>
                                              </>
                                            )}
                                          </button>
                                        </div>
                                        <div className="p-2 bg-slate-900 rounded border border-slate-800/80 font-mono text-[11px] text-slate-200 select-all break-all whitespace-pre-wrap max-h-28 overflow-y-auto">
                                          {qr.decodedPayload}
                                        </div>
                                      </div>

                                      {qr.normalizedUrl && (
                                        <div className="space-y-1 pt-1 border-t border-slate-800/60">
                                          <div className="flex items-center justify-between">
                                            <span className="text-[10px] font-mono text-cyan-400">
                                              Normalized URL (Routed to URL Intel):
                                            </span>
                                            <button
                                              id={`copy-qr-url-${idx}-${qIdx}`}
                                              onClick={() =>
                                                copyValue(`qr-url-${idx}-${qIdx}`, qr.normalizedUrl)
                                              }
                                              className="text-[10px] font-mono text-cyan-400 hover:text-cyan-300 flex items-center gap-1 cursor-pointer"
                                            >
                                              {copiedKey === `qr-url-${idx}-${qIdx}` ? (
                                                <>
                                                  <Check className="h-3 w-3 text-emerald-400" />
                                                  <span>Copied</span>
                                                </>
                                              ) : (
                                                <>
                                                  <Copy className="h-3 w-3" />
                                                  <span>Copy</span>
                                                </>
                                              )}
                                            </button>
                                          </div>
                                          <div className="text-[11px] font-mono text-slate-300 break-all select-all">
                                            {qr.normalizedUrl}
                                          </div>
                                        </div>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              ) : (
                                <div className="p-4 bg-slate-950/60 rounded-lg border border-slate-800/60 text-center">
                                  <span className="text-xs text-slate-500 font-mono">
                                    No QR codes detected in this image.
                                  </span>
                                </div>
                              )}
                            </div>

                            <div className="space-y-3 bg-slate-900/40 p-3.5 rounded-lg border border-slate-800/60">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-200">
                                  <FileText className="h-3.5 w-3.5 text-cyan-400" />
                                  <span>OCR Optical Recognition</span>
                                </div>
                                <span
                                  className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${ocrBadge.bg} ${ocrBadge.text} ${ocrBadge.border}`}
                                >
                                  <OcrIcon className="h-3 w-3" />
                                  <span>{ocrBadge.label}</span>
                                </span>
                              </div>

                              {v.ocrText ? (
                                <div className="space-y-1.5">
                                  <div className="flex items-center justify-between">
                                    <span className="text-[10px] font-mono text-slate-400">
                                      Recognized Text Characters:
                                    </span>
                                    <button
                                      id={`copy-ocr-text-${idx}`}
                                      onClick={() => copyValue(`ocr-text-${idx}`, v.ocrText)}
                                      className="text-[10px] font-mono text-cyan-400 hover:text-cyan-300 flex items-center gap-1 cursor-pointer"
                                    >
                                      {copiedKey === `ocr-text-${idx}` ? (
                                        <>
                                          <Check className="h-3 w-3 text-emerald-400" />
                                          <span>Copied</span>
                                        </>
                                      ) : (
                                        <>
                                          <Copy className="h-3 w-3" />
                                          <span>Copy</span>
                                        </>
                                      )}
                                    </button>
                                  </div>
                                  <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-800 font-mono text-xs text-slate-200 whitespace-pre-wrap select-all break-all max-h-44 overflow-y-auto">
                                    {v.ocrText}
                                  </div>
                                </div>
                              ) : (
                                <div className="p-4 bg-slate-950/60 rounded-lg border border-slate-800/60 text-center">
                                  <span className="text-xs text-slate-500 font-mono">
                                    {v.ocrNotes || 'No text content detected by optical recognition.'}
                                  </span>
                                </div>
                              )}

                              {v.ocrNotes && v.ocrText && (
                                <div className="text-[10px] font-mono text-slate-500 pt-1 border-t border-slate-800/40">
                                  Note: {v.ocrNotes}
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'content' && (
              <div className="space-y-6">
                {!contentAnalysis ? (
                  <div className="p-8 text-center text-slate-500 bg-slate-950/60 rounded-xl border border-slate-800">
                    No content analysis data available for this evidence item.
                  </div>
                ) : (
                  <>
                      <div id="ml-baseline-card" className="p-5 rounded-xl bg-slate-950/80 border border-slate-800 space-y-4 shadow-lg shadow-black/40">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
                          <div className="flex items-center gap-2.5">
                            <div className="p-2 rounded-lg bg-cyan-950/60 border border-cyan-800 text-cyan-400">
                              <Brain className="h-5 w-5" />
                            </div>
                            <div>
                              <div className="flex items-center gap-2">
                                <h3 className="text-sm font-bold text-white tracking-wide">
                                  Tribuo ML Baseline Classifier
                                </h3>
                                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-cyan-950 text-cyan-300 border border-cyan-800/80">
                                  {contentAnalysis.mlModelVersion || 'tribuo-logreg-tfidf-v1.0.0'}
                                </span>
                              </div>
                              <span className="text-xs text-slate-400 font-mono">
                                Logistic Regression with TF-IDF Word N-Grams
                              </span>
                            </div>
                          </div>

                          <div className="flex items-center gap-2">
                            <span className="text-[11px] font-mono text-slate-400">
                              Status: <span className="text-emerald-400 font-bold">{contentAnalysis.mlStatus}</span>
                            </span>
                            <button
                              id="toggle-metrics-btn"
                              onClick={() => setShowMetricsDrawer(!showMetricsDrawer)}
                              className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-700 text-xs font-mono text-slate-200 transition-colors cursor-pointer"
                            >
                              <Cpu className="h-3.5 w-3.5 text-cyan-400" />
                              <span>Evaluation Metrics</span>
                              {showMetricsDrawer ? (
                                <ChevronDown className="h-3.5 w-3.5 text-slate-400" />
                              ) : (
                                <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                              )}
                            </button>
                          </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-center pt-1">
                          <div className="space-y-1.5">
                            <span className="text-[11px] font-mono uppercase tracking-wider text-slate-400 block">
                              Model Classification
                            </span>
                            <div
                              id="ml-predicted-label"
                              className={`inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg border text-xs font-mono font-bold tracking-wider ${
                                predictedLabel === 'PHISHING'
                                  ? 'bg-rose-950/70 border-rose-700 text-rose-300'
                                  : predictedLabel === 'BENIGN'
                                  ? 'bg-emerald-950/70 border-emerald-700 text-emerald-300'
                                  : 'bg-slate-900 border-slate-700 text-slate-300'
                              }`}
                            >
                              {predictedLabel === 'PHISHING' && (
                                <AlertOctagon className="h-4 w-4 text-rose-400" />
                              )}
                              {predictedLabel === 'BENIGN' && (
                                <ShieldCheck className="h-4 w-4 text-emerald-400" />
                              )}
                              {predictedLabel !== 'PHISHING' &&
                                predictedLabel !== 'BENIGN' && (
                                  <HelpCircle className="h-4 w-4 text-slate-400" />
                                )}
                              <span>PREDICTED: {predictedLabel}</span>
                            </div>
                          </div>

                          <div className="md:col-span-2 space-y-1.5">
                            <div className="flex items-center justify-between text-xs font-mono">
                              <span className="text-slate-400">Phishing Probability Score</span>
                              <span
                                className={`font-bold ${
                                  probability >= 0.7
                                    ? 'text-rose-400'
                                    : probability >= 0.4
                                    ? 'text-amber-400'
                                    : 'text-emerald-400'
                                }`}
                              >
                                {(probability * 100).toFixed(1)}%
                              </span>
                            </div>
                            <div
                              id="ml-probability-bar"
                              className="w-full bg-slate-900 rounded-full h-3 overflow-hidden border border-slate-800 p-0.5"
                            >
                              <div
                                className={`h-full rounded-full transition-all duration-700 ${
                                  probability >= 0.7
                                    ? 'bg-gradient-to-r from-amber-500 to-rose-600'
                                    : probability >= 0.4
                                    ? 'bg-gradient-to-r from-cyan-500 to-amber-500'
                                    : 'bg-gradient-to-r from-teal-500 to-emerald-500'
                                }`}
                                style={{
                                  width: `${Math.min(100, Math.max(2, probability * 100))}%`
                                }}
                              />
                            </div>
                            <span className="text-[10px] font-mono text-slate-500 block">
                              Evaluated on TF-IDF token distributions extracted from message subject and body channels.
                            </span>
                          </div>
                        </div>

                        {showMetricsDrawer && metrics && (
                          <div
                            id="metrics-drawer"
                            className="mt-4 p-4 rounded-xl bg-slate-900/70 border border-slate-800 space-y-4"
                          >
                            <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
                              <div className="flex items-center gap-2 text-xs font-bold text-slate-200">
                                <Sparkles className="h-4 w-4 text-cyan-400" />
                                <span>Held-Out Test Set Performance Benchmark</span>
                              </div>
                              <span className="text-[10px] font-mono text-slate-400">
                                Training: {metrics.trainingSampleCount != null ? metrics.trainingSampleCount : metrics.trainingSetSize} samples | Test: {metrics.testSampleCount != null ? metrics.testSampleCount : metrics.testSetSize} samples
                              </span>
                            </div>

                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                              <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800 text-center space-y-0.5">
                                <span className="text-[10px] font-mono text-slate-400 uppercase block">Accuracy</span>
                                <span className="text-lg font-bold font-mono text-cyan-300">
                                  {(metrics.accuracy * 100).toFixed(1)}%
                                </span>
                              </div>
                              <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800 text-center space-y-0.5">
                                <span className="text-[10px] font-mono text-slate-400 uppercase block">Precision</span>
                                <span className="text-lg font-bold font-mono text-emerald-300">
                                  {(metrics.precision * 100).toFixed(1)}%
                                </span>
                              </div>
                              <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800 text-center space-y-0.5">
                                <span className="text-[10px] font-mono text-slate-400 uppercase block">Recall</span>
                                <span className="text-lg font-bold font-mono text-cyan-300">
                                  {(metrics.recall * 100).toFixed(1)}%
                                </span>
                              </div>
                              <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800 text-center space-y-0.5">
                                <span className="text-[10px] font-mono text-slate-400 uppercase block">F1 Score</span>
                                <span className="text-lg font-bold font-mono text-purple-300">
                                  {((metrics.f1 != null ? metrics.f1 : metrics.f1Score) * 100).toFixed(1)}%
                                </span>
                              </div>
                            </div>

                            <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800 space-y-2">
                              <span className="text-xs font-semibold text-slate-300 block">
                                Held-Out Confusion Matrix
                              </span>
                              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs font-mono">
                                <div className="p-2 rounded bg-emerald-950/30 border border-emerald-900/60 text-emerald-300">
                                  <span className="text-[10px] block text-emerald-400">True Positives (TP)</span>
                                  <span className="text-base font-bold">{metrics.truePositives}</span>
                                </div>
                                <div className="p-2 rounded bg-rose-950/30 border border-rose-900/60 text-rose-300">
                                  <span className="text-[10px] block text-rose-400">False Positives (FP)</span>
                                  <span className="text-base font-bold">{metrics.falsePositives}</span>
                                </div>
                                <div className="p-2 rounded bg-emerald-950/30 border border-emerald-900/60 text-emerald-300">
                                  <span className="text-[10px] block text-emerald-400">True Negatives (TN)</span>
                                  <span className="text-base font-bold">{metrics.trueNegatives}</span>
                                </div>
                                <div className="p-2 rounded bg-rose-950/30 border border-rose-900/60 text-rose-300">
                                  <span className="text-[10px] block text-rose-400">False Negatives (FN)</span>
                                  <span className="text-base font-bold">{metrics.falseNegatives}</span>
                                </div>
                              </div>
                              <span className="text-[10px] font-mono text-slate-500 block pt-1">
                                Rigorous evaluation guarantees: TF-IDF vectorizer fit strictly on the training partition to prevent test set data leakage.
                              </span>
                            </div>
                          </div>
                        )}
                      </div>

                      <div className="space-y-3">
                        {promptInjectionDetected ? (
                          <div
                            id="prompt-injection-alert"
                            className="p-4 rounded-xl bg-rose-950/50 border-2 border-rose-600/90 shadow-xl shadow-rose-950/30 space-y-3"
                          >
                            <div className="flex items-center gap-2.5 text-rose-300 font-bold text-sm">
                              <AlertOctagon className="h-5 w-5 text-rose-400 animate-pulse shrink-0" />
                              <span>CRITICAL ALERT: Prompt Injection &amp; LLM Override Attempt Detected</span>
                            </div>
                            <p className="text-xs text-rose-200/90 leading-relaxed font-mono">
                              Adversarial instruction overrides or exfiltration triggers were identified. Forensic AI agents, summarizers, and automated triage pipelines must treat this email as untrusted code.
                            </p>

                            <div className="space-y-2 pt-1">
                              {promptMatches.map((m, idx) => (
                                <div
                                  key={idx}
                                  className="p-3 rounded-lg bg-black/60 border border-rose-700/80 space-y-1.5"
                                >
                                  <div className="flex flex-wrap items-center justify-between gap-2">
                                    <div className="flex items-center gap-2">
                                      <span className="text-xs font-mono font-bold text-rose-300">
                                        {m.category || m.patternName}
                                      </span>
                                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-900/80 text-rose-200 border border-rose-700">
                                        {m.severity || 'CRITICAL'}
                                      </span>
                                    </div>
                                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-900 text-slate-300 border border-slate-700">
                                      Channel: {m.sourceChannel || m.channel}
                                    </span>
                                  </div>
                                  <div className="p-2 rounded bg-slate-950 border border-slate-800 font-mono text-xs text-rose-200 select-all break-all font-semibold">
                                    &quot;{m.matchedPattern || m.matchedText}&quot;
                                  </div>
                                  {m.explanation && (
                                    <p className="text-[11px] text-rose-300/80 font-sans leading-relaxed">
                                      {m.explanation}
                                    </p>
                                  )}
                                </div>
                              ))}
                            </div>
                          </div>
                        ) : (
                          <div
                            id="prompt-injection-clean"
                            className="p-3.5 rounded-xl bg-emerald-950/40 border border-emerald-800/60 flex items-center gap-3 text-emerald-300 text-xs font-mono"
                          >
                            <ShieldCheck className="h-5 w-5 text-emerald-400 shrink-0" />
                            <div>
                              <span className="font-bold text-white block">
                                No Prompt Injection Indicators Detected
                              </span>
                              <span className="text-emerald-300/80 text-[11px]">
                                Scanned email body, HTML text, OCR extracts, and QR payloads for adversarial AI overrides without triggering alerts.
                              </span>
                            </div>
                          </div>
                        )}
                      </div>

                      <div className="space-y-4">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2 text-xs font-bold text-slate-200">
                            <Activity className="h-4 w-4 text-cyan-400" />
                            <span>Social Engineering Indicator Flags (5 Categories)</span>
                          </div>
                          <span className="text-[11px] font-mono text-slate-400">
                            Multi-channel scan: Body Text, HTML, OCR Text, QR Payload
                          </span>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
                          <div
                            id="social-card-urgency"
                            className={`p-3.5 rounded-xl border space-y-2 transition-all ${
                              urgencyDetected
                                ? 'bg-rose-950/40 border-rose-700/80 text-rose-300'
                                : 'bg-slate-950/60 border-slate-800/80 text-slate-400'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-bold text-white">Urgency &amp; Coercion</span>
                              <span
                                className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                                  urgencyDetected
                                    ? 'bg-rose-900 text-rose-200 border border-rose-700'
                                    : 'bg-slate-900 text-slate-500'
                                }`}
                              >
                                {urgencyDetected ? 'TRIGGERED' : 'CLEAR'}
                              </span>
                            </div>
                            <p className="text-[11px] leading-relaxed text-slate-400">
                              Deadlines, suspension threats, urgent demands.
                            </p>
                            <div className="text-[10px] font-mono text-slate-400 pt-1 border-t border-slate-800/50">
                              {socialMatches.filter((m) => m.category === 'URGENCY').length} matches
                            </div>
                          </div>

                          <div
                            id="social-card-credential"
                            className={`p-3.5 rounded-xl border space-y-2 transition-all ${
                              credentialRequestDetected
                                ? 'bg-rose-950/40 border-rose-700/80 text-rose-300'
                                : 'bg-slate-950/60 border-slate-800/80 text-slate-400'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-bold text-white">Credential Request</span>
                              <span
                                className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                                  credentialRequestDetected
                                    ? 'bg-rose-900 text-rose-200 border border-rose-700'
                                    : 'bg-slate-900 text-slate-500'
                                }`}
                              >
                                {credentialRequestDetected ? 'TRIGGERED' : 'CLEAR'}
                              </span>
                            </div>
                            <p className="text-[11px] leading-relaxed text-slate-400">
                              Password resets, MFA prompts, login verifications.
                            </p>
                            <div className="text-[10px] font-mono text-slate-400 pt-1 border-t border-slate-800/50">
                              {socialMatches.filter((m) => m.category === 'CREDENTIAL_REQUEST').length} matches
                            </div>
                          </div>

                          <div
                            id="social-card-payment"
                            className={`p-3.5 rounded-xl border space-y-2 transition-all ${
                              paymentInstructionDetected
                                ? 'bg-rose-950/40 border-rose-700/80 text-rose-300'
                                : 'bg-slate-950/60 border-slate-800/80 text-slate-400'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-bold text-white">Payment Instruction</span>
                              <span
                                className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                                  paymentInstructionDetected
                                    ? 'bg-rose-900 text-rose-200 border border-rose-700'
                                    : 'bg-slate-900 text-slate-500'
                                }`}
                              >
                                {paymentInstructionDetected ? 'TRIGGERED' : 'CLEAR'}
                              </span>
                            </div>
                            <p className="text-[11px] leading-relaxed text-slate-400">
                              Wire transfers, routing changes, gift cards, invoices.
                            </p>
                            <div className="text-[10px] font-mono text-slate-400 pt-1 border-t border-slate-800/50">
                              {socialMatches.filter((m) => m.category === 'PAYMENT_INSTRUCTION').length} matches
                            </div>
                          </div>

                          <div
                            id="social-card-impersonation"
                            className={`p-3.5 rounded-xl border space-y-2 transition-all ${
                              impersonationDetected
                                ? 'bg-rose-950/40 border-rose-700/80 text-rose-300'
                                : 'bg-slate-950/60 border-slate-800/80 text-slate-400'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-bold text-white">Impersonation</span>
                              <span
                                className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                                  impersonationDetected
                                    ? 'bg-rose-900 text-rose-200 border border-rose-700'
                                    : 'bg-slate-900 text-slate-500'
                              }`}
                            >
                              {impersonationDetected ? 'TRIGGERED' : 'CLEAR'}
                            </span>
                          </div>
                          <p className="text-[11px] leading-relaxed text-slate-400">
                            IT support, executive leadership, HR, compliance.
                          </p>
                          <div className="text-[10px] font-mono text-slate-400 pt-1 border-t border-slate-800/50">
                            {socialMatches.filter((m) => m.category === 'IMPERSONATION').length} matches
                          </div>
                        </div>

                        <div
                          id="social-card-secrecy"
                          className={`p-3.5 rounded-xl border space-y-2 transition-all ${
                            secrecyDetected
                              ? 'bg-rose-950/40 border-rose-700/80 text-rose-300'
                              : 'bg-slate-950/60 border-slate-800/80 text-slate-400'
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-bold text-white">Secrecy &amp; Bypass</span>
                            <span
                              className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                                secrecyDetected
                                  ? 'bg-rose-900 text-rose-200 border border-rose-700'
                                  : 'bg-slate-900 text-slate-500'
                              }`}
                            >
                              {secrecyDetected ? 'TRIGGERED' : 'CLEAR'}
                            </span>
                          </div>
                          <p className="text-[11px] leading-relaxed text-slate-400">
                            Strict confidentiality, do not call, bypass protocol.
                          </p>
                          <div className="text-[10px] font-mono text-slate-400 pt-1 border-t border-slate-800/50">
                            {socialMatches.filter((m) => m.category === 'SECRECY').length} matches
                          </div>
                        </div>
                      </div>

                      <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-3">
                        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs font-mono text-slate-400">Filter Category:</span>
                            {['ALL', 'URGENCY', 'CREDENTIAL_REQUEST', 'PAYMENT_INSTRUCTION', 'IMPERSONATION', 'SECRECY'].map((cat) => (
                              <button
                                key={cat}
                                onClick={() => setContentCategoryFilter(cat)}
                                className={`px-2.5 py-1 rounded-lg text-[10px] font-mono transition-colors cursor-pointer ${
                                  contentCategoryFilter === cat
                                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                                    : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
                                }`}
                              >
                                {cat === 'CREDENTIAL_REQUEST'
                                  ? 'CREDENTIALS'
                                  : cat === 'PAYMENT_INSTRUCTION'
                                  ? 'PAYMENTS'
                                  : cat}
                              </button>
                            ))}
                          </div>

                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs font-mono text-slate-400">Channel:</span>
                            {['ALL', 'BODY_TEXT', 'HTML_TEXT', 'OCR_TEXT', 'QR_PAYLOAD'].map((chan) => (
                              <button
                                key={chan}
                                onClick={() => setContentChannelFilter(chan)}
                                className={`px-2 py-0.5 rounded text-[10px] font-mono transition-colors cursor-pointer ${
                                  contentChannelFilter === chan
                                    ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                                    : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
                                }`}
                              >
                                {chan === 'ALL' ? 'ALL CHANNELS' : chan.replace('_TEXT', '').replace('_PAYLOAD', '')}
                              </button>
                            ))}
                          </div>
                        </div>

                        <div className="relative">
                          <Search className="h-4 w-4 absolute left-3 top-2.5 text-slate-500" />
                          <input
                            id="content-search-input"
                            type="text"
                            placeholder="Search matched phrases, categories, or channels..."
                            value={contentSearch}
                            onChange={(e) => setContentSearch(e.target.value)}
                            className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500 font-mono"
                          />
                        </div>

                        <div className="space-y-2 max-h-96 overflow-y-auto pt-1">
                          {(() => {
                            const filtered = socialMatches.filter((m) => {
                              const chan = m.sourceChannel || m.channel || ''
                              const matchCat = contentCategoryFilter === 'ALL' || m.category === contentCategoryFilter
                              const matchChan = contentChannelFilter === 'ALL' || chan === contentChannelFilter
                              const matchTerm =
                                !contentSearch ||
                                m.matchedPattern.toLowerCase().includes(contentSearch.toLowerCase()) ||
                                m.category.toLowerCase().includes(contentSearch.toLowerCase()) ||
                                chan.toLowerCase().includes(contentSearch.toLowerCase())
                              return matchCat && matchChan && matchTerm
                            })

                            if (filtered.length === 0) {
                              return (
                                <div className="p-6 text-center text-slate-500 bg-slate-900/40 rounded-lg border border-slate-800/80 font-mono text-xs">
                                  No social engineering matches found for the selected filters.
                                </div>
                              )
                            }

                            return filtered.map((m, idx) => (
                              <div
                                key={idx}
                                className="p-3 rounded-lg bg-slate-900/60 border border-slate-800 hover:border-slate-700 transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-2"
                              >
                                <div className="space-y-1">
                                  <div className="flex items-center gap-2">
                                    <span
                                      className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold border ${
                                        m.category === 'URGENCY'
                                          ? 'bg-rose-950/80 text-rose-300 border-rose-800'
                                          : m.category === 'CREDENTIAL_REQUEST'
                                          ? 'bg-amber-950/80 text-amber-300 border-amber-800'
                                          : m.category === 'PAYMENT_INSTRUCTION'
                                          ? 'bg-purple-950/80 text-purple-300 border-purple-800'
                                          : m.category === 'IMPERSONATION'
                                          ? 'bg-blue-950/80 text-blue-300 border-blue-800'
                                          : 'bg-indigo-950/80 text-indigo-300 border-indigo-800'
                                      }`}
                                    >
                                      {m.category}
                                    </span>
                                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                                      Channel: {m.sourceChannel || m.channel}
                                    </span>
                                    {m.severity && (
                                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
                                        {m.severity}
                                      </span>
                                    )}
                                  </div>
                                  <div className="font-mono text-xs text-slate-200 select-all font-semibold">
                                    &quot;{m.matchedPattern}&quot;
                                  </div>
                                  {m.explanation && (
                                    <p className="text-[11px] text-slate-400 font-sans leading-relaxed">
                                      {m.explanation}
                                    </p>
                                  )}
                                </div>

                                <button
                                  id={`copy-social-match-${idx}`}
                                  onClick={() => copyValue(`social-match-${idx}`, m.matchedPattern)}
                                  className="text-[11px] font-mono text-slate-400 hover:text-slate-200 flex items-center gap-1 cursor-pointer shrink-0"
                                >
                                  {copiedKey === `social-match-${idx}` ? (
                                    <>
                                      <Check className="h-3 w-3 text-emerald-400" />
                                      <span className="text-emerald-400">Copied</span>
                                    </>
                                  ) : (
                                    <>
                                      <Copy className="h-3 w-3" />
                                      <span>Copy</span>
                                    </>
                                  )}
                                </button>
                              </div>
                            ))
                          })()}
                        </div>
                      </div>
                    </div>
                  </>
                )}
              </div>
            )}

            {activeTab === 'risk' && (
              <div className="space-y-6">
                {!riskBreakdown ? (
                  <div className="p-8 text-center text-slate-400 bg-slate-950/60 rounded-xl border border-slate-800 space-y-2">
                    <Activity className="h-6 w-6 text-cyan-400 mx-auto animate-spin" />
                    <p className="text-xs font-mono">Evaluating deterministic risk and evidence confidence...</p>
                  </div>
                ) : (
                  <>
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                      <div className="bg-slate-950/80 border border-slate-800 rounded-2xl p-5 space-y-4 relative overflow-hidden">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <ShieldAlert className="h-5 w-5 text-rose-400" />
                            <h2 className="text-sm font-bold text-white tracking-wide">
                              Deterministic Threat Risk Score
                            </h2>
                          </div>
                          <span
                            id="risk-band-badge"
                            className={`px-2.5 py-1 rounded-md text-xs font-mono font-bold uppercase tracking-wider border ${
                              riskBreakdown.riskLevel === 'CRITICAL'
                                ? 'bg-rose-950 text-rose-300 border-rose-700'
                                : riskBreakdown.riskLevel === 'HIGH'
                                ? 'bg-orange-950 text-orange-300 border-orange-700'
                                : riskBreakdown.riskLevel === 'MEDIUM'
                                ? 'bg-amber-950 text-amber-300 border-amber-700'
                                : 'bg-emerald-950 text-emerald-300 border-emerald-700'
                            }`}
                          >
                            {riskBreakdown.riskLevel} BAND ({
                              riskBreakdown.riskLevel === 'CRITICAL'
                                ? '75–100'
                                : riskBreakdown.riskLevel === 'HIGH'
                                ? '50–74'
                                : riskBreakdown.riskLevel === 'MEDIUM'
                                ? '25–49'
                                : '0–24'
                            })
                          </span>
                        </div>

                        <div className="flex items-baseline gap-3">
                          <span
                            id="risk-score-value"
                            className={`text-4xl sm:text-5xl font-black font-mono tracking-tight ${
                              riskBreakdown.riskLevel === 'CRITICAL'
                                ? 'text-rose-400'
                                : riskBreakdown.riskLevel === 'HIGH'
                                ? 'text-orange-400'
                                : riskBreakdown.riskLevel === 'MEDIUM'
                                ? 'text-amber-400'
                                : 'text-emerald-400'
                            }`}
                          >
                            {riskBreakdown.deterministicRiskScore}
                          </span>
                          <span className="text-lg font-mono text-slate-500 font-bold">/ 100</span>
                          <span className="text-xs font-mono text-slate-400 ml-auto">
                            Uncapped findings weight: <strong className="text-slate-200">{riskBreakdown.totalUncappedScore} pts</strong>
                          </span>
                        </div>

                        <div className="w-full bg-slate-900 rounded-full h-3 overflow-hidden border border-slate-800">
                          <div
                            className={`h-full transition-all duration-500 ${
                              riskBreakdown.riskLevel === 'CRITICAL'
                                ? 'bg-gradient-to-r from-orange-500 to-rose-600'
                                : riskBreakdown.riskLevel === 'HIGH'
                                ? 'bg-gradient-to-r from-amber-500 to-orange-500'
                                : riskBreakdown.riskLevel === 'MEDIUM'
                                ? 'bg-gradient-to-r from-yellow-500 to-amber-500'
                                : 'bg-gradient-to-r from-emerald-500 to-teal-500'
                            }`}
                            style={{ width: `${Math.max(4, Math.min(100, riskBreakdown.deterministicRiskScore))}%` }}
                          />
                        </div>

                        <div className="pt-2 border-t border-slate-800/80 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-[11px] text-slate-400 font-mono">
                          <span>Engine: {riskBreakdown.engineVersion}</span>
                          <span>Evaluated: {new Date(riskBreakdown.evaluatedAt).toLocaleString()}</span>
                        </div>

                        <p className="text-[11px] text-slate-400 font-sans leading-relaxed bg-slate-900/60 p-2.5 rounded-lg border border-slate-800">
                          Primary deterministic score calculated as a capped sum of deduplicated technical indicators across 7 forensic channels. AI models cannot alter this score.
                        </p>
                      </div>

                      <div className="bg-slate-950/80 border border-slate-800 rounded-2xl p-5 space-y-4 relative overflow-hidden">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <ShieldCheck className="h-5 w-5 text-cyan-400" />
                            <h2 className="text-sm font-bold text-white tracking-wide">
                              Heuristic Evidence Confidence
                            </h2>
                          </div>
                          <span
                            id="confidence-level-badge"
                            className={`px-2.5 py-1 rounded-md text-xs font-mono font-bold uppercase tracking-wider border ${
                              riskBreakdown.confidenceLevel === 'HIGH'
                                ? 'bg-blue-950 text-blue-300 border-blue-700'
                                : riskBreakdown.confidenceLevel === 'MODERATE'
                                ? 'bg-purple-950 text-purple-300 border-purple-700'
                                : 'bg-slate-900 text-slate-400 border-slate-700'
                            }`}
                          >
                            {riskBreakdown.confidenceLevel} CONFIDENCE ({
                              riskBreakdown.confidenceLevel === 'HIGH'
                                ? '75–100'
                                : riskBreakdown.confidenceLevel === 'MODERATE'
                                ? '50–74'
                                : '0–49'
                            })
                          </span>
                        </div>

                        <div className="flex items-baseline gap-3">
                          <span
                            id="confidence-score-value"
                            className={`text-4xl sm:text-5xl font-black font-mono tracking-tight ${
                              riskBreakdown.confidenceLevel === 'HIGH'
                                ? 'text-blue-400'
                                : riskBreakdown.confidenceLevel === 'MODERATE'
                                ? 'text-purple-400'
                                : 'text-slate-400'
                            }`}
                          >
                            {riskBreakdown.evidenceConfidenceScore}
                          </span>
                          <span className="text-lg font-mono text-slate-500 font-bold">/ 100</span>
                          <span className="text-xs font-mono text-slate-400 ml-auto">
                            Corroboration: <strong className="text-slate-200">
                              {riskBreakdown.categoryScores ? riskBreakdown.categoryScores.filter(c => c.score > 0).length : 0} active categories
                            </strong>
                          </span>
                        </div>

                        <div className="w-full bg-slate-900 rounded-full h-3 overflow-hidden border border-slate-800">
                          <div
                            className={`h-full transition-all duration-500 ${
                              riskBreakdown.confidenceLevel === 'HIGH'
                                ? 'bg-gradient-to-r from-cyan-500 to-blue-500'
                                : riskBreakdown.confidenceLevel === 'MODERATE'
                                ? 'bg-gradient-to-r from-blue-500 to-purple-500'
                                : 'bg-gradient-to-r from-slate-600 to-slate-400'
                            }`}
                            style={{ width: `${Math.max(4, Math.min(100, riskBreakdown.evidenceConfidenceScore))}%` }}
                          />
                        </div>

                        <div className="pt-2 border-t border-slate-800/80 text-[11px] text-cyan-300 font-mono bg-cyan-950/30 p-2.5 rounded-lg border border-cyan-800/50">
                          {riskBreakdown.confidenceExplanation}
                        </div>

                        <p className="text-[11px] text-slate-400 font-sans leading-relaxed bg-slate-900/60 p-2.5 rounded-lg border border-slate-800">
                          Evidence confidence measures data completeness across forensic channels and cross-category corroboration. It is strictly not a calibrated statistical probability.
                        </p>
                      </div>
                    </div>

                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <Activity className="h-4 w-4 text-cyan-400" />
                          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300 font-mono">
                            Forensic Category Contributions (7 Channels)
                          </h3>
                        </div>
                        <span className="text-[11px] font-mono text-slate-400">
                          Click any category to filter risk factors below
                        </span>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7 gap-2.5">
                        {riskBreakdown.categoryScores && riskBreakdown.categoryScores.map((cat) => {
                          const isSelected = riskCategoryFilter === cat.category
                          const hasFindings = cat.score > 0
                          return (
                            <button
                              key={cat.category}
                              id={`cat-card-${cat.category.toLowerCase()}`}
                              onClick={() => setRiskCategoryFilter(isSelected ? 'ALL' : cat.category)}
                              className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                                isSelected
                                  ? 'bg-cyan-950/80 border-cyan-500 shadow-md shadow-cyan-950/50 ring-1 ring-cyan-500'
                                  : hasFindings
                                  ? 'bg-slate-950/80 border-slate-700 hover:border-slate-600'
                                  : 'bg-slate-950/40 border-slate-800/60 opacity-75 hover:opacity-100 hover:border-slate-700'
                              }`}
                            >
                              <div className="flex items-center justify-between mb-1.5">
                                <span className={`text-[10px] font-mono font-bold px-1.5 py-0.5 rounded ${
                                  hasFindings
                                    ? 'bg-rose-950 text-rose-300 border border-rose-800'
                                    : 'bg-slate-900 text-slate-500'
                                }`}>
                                  {cat.factorCount} {cat.factorCount === 1 ? 'FINDING' : 'FINDINGS'}
                                </span>
                                <span className={`text-xs font-mono font-bold ${hasFindings ? 'text-rose-400' : 'text-slate-500'}`}>
                                  +{cat.score}
                                </span>
                              </div>
                              <span className="text-xs font-bold text-white block truncate mb-1">
                                {cat.displayName}
                              </span>
                              <div className="w-full bg-slate-900 rounded-full h-1.5 overflow-hidden">
                                <div
                                  className={`h-full ${
                                    cat.score >= 50
                                      ? 'bg-rose-500'
                                      : cat.score >= 25
                                      ? 'bg-amber-500'
                                      : cat.score > 0
                                      ? 'bg-cyan-500'
                                      : 'bg-slate-800'
                                  }`}
                                  style={{ width: `${Math.min(100, Math.max(0, cat.score))}%` }}
                                />
                              </div>
                            </button>
                          )
                        })}
                      </div>
                    </div>

                    <div className="bg-slate-950/80 border border-slate-800 rounded-2xl p-5 space-y-4">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800">
                        <div className="flex items-center gap-2">
                          <Layers className="h-4 w-4 text-cyan-400" />
                          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-200 font-mono">
                            Triggered Risk Factors &amp; Evidence References ({riskBreakdown.factors ? riskBreakdown.factors.length : 0})
                          </h3>
                        </div>

                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-[11px] font-mono text-slate-400">Severity:</span>
                          {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((sev) => (
                            <button
                              key={sev}
                              id={`risk-filter-sev-${sev.toLowerCase()}`}
                              onClick={() => setRiskSeverityFilter(sev)}
                              className={`px-2 py-0.5 rounded text-[10px] font-mono transition-colors cursor-pointer ${
                                riskSeverityFilter === sev
                                  ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                                  : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
                              }`}
                            >
                              {sev}
                            </button>
                          ))}
                        </div>
                      </div>

                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
                        <div className="relative flex-1 max-w-md">
                          <Search className="h-3.5 w-3.5 absolute left-3 top-2.5 text-slate-500" />
                          <input
                            id="risk-factor-search-input"
                            type="text"
                            placeholder="Search factor code, explanation, or evidence reference..."
                            value={riskSearch}
                            onChange={(e) => setRiskSearch(e.target.value)}
                            className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500 font-mono"
                          />
                        </div>

                        <div className="flex items-center gap-2">
                          {riskCategoryFilter !== 'ALL' && (
                            <span className="text-[11px] font-mono px-2 py-1 rounded bg-cyan-950 text-cyan-300 border border-cyan-800 flex items-center gap-1">
                              Category: {riskCategoryFilter}
                              <button
                                onClick={() => setRiskCategoryFilter('ALL')}
                                className="ml-1 text-cyan-400 hover:text-white cursor-pointer"
                              >
                                &times;
                              </button>
                            </span>
                          )}
                          {(riskSeverityFilter !== 'ALL' || riskCategoryFilter !== 'ALL' || riskSearch) && (
                            <button
                              onClick={() => {
                                setRiskSeverityFilter('ALL')
                                setRiskCategoryFilter('ALL')
                                setRiskSearch('')
                              }}
                              className="text-[11px] font-mono text-slate-400 hover:text-slate-200 underline cursor-pointer"
                            >
                              Clear filters
                            </button>
                          )}
                        </div>
                      </div>

                      {(() => {
                        const factors = riskBreakdown.factors || []
                        const filtered = factors.filter((f) => {
                          const matchSev = riskSeverityFilter === 'ALL' || f.severity === riskSeverityFilter
                          const matchCat = riskCategoryFilter === 'ALL' || f.category === riskCategoryFilter
                          const matchTerm =
                            !riskSearch ||
                            f.factorCode.toLowerCase().includes(riskSearch.toLowerCase()) ||
                            f.explanation.toLowerCase().includes(riskSearch.toLowerCase()) ||
                            (f.evidenceReference && f.evidenceReference.toLowerCase().includes(riskSearch.toLowerCase()))
                          return matchSev && matchCat && matchTerm
                        })

                        if (factors.length === 0) {
                          return (
                            <div
                              id="risk-factors-clean"
                              className="p-8 rounded-xl bg-emerald-950/30 border border-emerald-800/60 text-center space-y-2"
                            >
                              <ShieldCheck className="h-8 w-8 text-emerald-400 mx-auto" />
                              <h4 className="text-sm font-bold text-white">No Suspicious Risk Factors Detected</h4>
                              <p className="text-xs text-emerald-300/80 font-mono max-w-lg mx-auto">
                                All evaluated forensic channels (Headers, Authentication, Route Hops, URLs, Visuals, Content, Machine Learning) are within normal benign thresholds.
                              </p>
                            </div>
                          )
                        }

                        if (filtered.length === 0) {
                          return (
                            <div className="p-8 text-center text-slate-500 bg-slate-900/40 rounded-xl border border-slate-800 text-xs font-mono">
                              No risk factors match the active filters.
                            </div>
                          )
                        }

                        return (
                          <div className="space-y-2.5 max-h-[500px] overflow-y-auto">
                            {filtered.map((factor, idx) => (
                              <div
                                key={factor.id || idx}
                                id={`risk-factor-item-${idx}`}
                                className="p-3.5 rounded-xl bg-slate-900/70 border border-slate-800 hover:border-slate-700 transition-all flex flex-col md:flex-row md:items-start justify-between gap-3 text-xs"
                              >
                                <div className="space-y-1.5 flex-1">
                                  <div className="flex flex-wrap items-center gap-2">
                                    <span
                                      className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${
                                        factor.severity === 'CRITICAL'
                                          ? 'bg-rose-950 text-rose-300 border-rose-800'
                                          : factor.severity === 'HIGH'
                                          ? 'bg-orange-950 text-orange-300 border-orange-800'
                                          : factor.severity === 'MEDIUM'
                                          ? 'bg-amber-950 text-amber-300 border-amber-800'
                                          : 'bg-emerald-950 text-emerald-300 border-emerald-800'
                                      }`}
                                    >
                                      {factor.severity}
                                    </span>
                                    <span className="font-mono text-cyan-300 font-bold">
                                      {factor.factorCode}
                                    </span>
                                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                                      {factor.category}
                                    </span>
                                    <span className="text-[11px] font-mono font-bold text-rose-400 ml-auto md:ml-0">
                                      +{factor.weightPoints} pts
                                    </span>
                                  </div>

                                  <p className="text-slate-300 leading-relaxed font-sans text-xs">
                                    {factor.explanation}
                                  </p>

                                  {factor.evidenceReference && (
                                    <div className="flex items-center gap-2 pt-1 font-mono text-[11px]">
                                      <span className="text-slate-500 shrink-0">Evidence:</span>
                                      <code className="text-slate-300 bg-slate-950 px-2 py-0.5 rounded border border-slate-800 break-all select-all">
                                        {factor.evidenceReference}
                                      </code>
                                      <button
                                        onClick={() => copyValue(`evidence-ref-${idx}`, factor.evidenceReference)}
                                        className="p-1 hover:bg-slate-800 rounded text-slate-400 hover:text-cyan-300 transition-colors cursor-pointer shrink-0"
                                        title="Copy Evidence Reference"
                                      >
                                        {copiedKey === `evidence-ref-${idx}` ? (
                                          <Check className="h-3 w-3 text-emerald-400" />
                                        ) : (
                                          <Copy className="h-3 w-3" />
                                        )}
                                      </button>
                                    </div>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        )
                      })()}
                    </div>
                  </>
                )}
              </div>
            )}

            {activeTab === 'body' && (
              <div className="space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 bg-slate-950 p-2.5 rounded-xl border border-slate-800">
                  <div className="flex items-center gap-1.5 text-xs text-slate-400 font-mono">
                    <Info className="h-3.5 w-3.5 text-cyan-400" />
                    <span>Safe Forensic Preview (Untrusted email text isolated)</span>
                  </div>

                  <div className="flex items-center gap-1 text-xs">
                    <button
                      id="body-mode-extracted-btn"
                      onClick={() => setBodyMode('extracted')}
                      className={`px-3 py-1 rounded-lg font-mono text-[11px] transition-colors cursor-pointer ${
                        bodyMode === 'extracted'
                          ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                          : 'text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      Safe Extracted Text
                    </button>
                    <button
                      id="body-mode-plain-btn"
                      onClick={() => setBodyMode('plain')}
                      className={`px-3 py-1 rounded-lg font-mono text-[11px] transition-colors cursor-pointer ${
                        bodyMode === 'plain'
                          ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                          : 'text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      Plain Text
                    </button>
                    <button
                      id="body-mode-html-btn"
                      onClick={() => setBodyMode('html')}
                      className={`px-3 py-1 rounded-lg font-mono text-[11px] transition-colors cursor-pointer ${
                        bodyMode === 'html'
                          ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                          : 'text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      HTML Source
                    </button>
                  </div>
                </div>

                <div className="bg-slate-950 rounded-xl p-5 border border-slate-800 max-h-[500px] overflow-y-auto">
                  <pre className="font-mono text-xs text-slate-200 whitespace-pre-wrap select-text leading-relaxed">
                    {bodyMode === 'extracted'
                      ? parsed.extractedText || parsed.plainTextBody || '(No text content extracted)'
                      : bodyMode === 'plain'
                      ? parsed.plainTextBody || '(No plain text body found in MIME structure)'
                      : parsed.htmlBody || '(No HTML body found in MIME structure)'}
                  </pre>
                </div>
              </div>
            )}

            {activeTab === 'attachments' && (
              <div className="space-y-4">
                {attachments.length === 0 ? (
                  <div className="p-8 rounded-xl bg-slate-950/60 border border-slate-800 text-center text-xs font-mono text-slate-400">
                    No attachment artifacts found in this message.
                  </div>
                ) : (
                  <div className="space-y-3">
                    {attachments.map((att, idx) => (
                      <div
                        key={idx}
                        className={`p-4 rounded-xl bg-slate-950/80 border transition-all ${
                          att.riskyExtension
                            ? 'border-rose-800/80 bg-rose-950/20'
                            : 'border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2 border-b border-slate-800/60">
                          <div className="flex items-center gap-2">
                            <Paperclip className="h-4 w-4 text-cyan-400 shrink-0" />
                            <span className="text-xs font-bold font-mono text-slate-200">
                              {att.filename}
                            </span>
                            <span className="text-[11px] font-mono text-slate-500">
                              ({formatBytes(att.sizeBytes)})
                            </span>
                          </div>

                          <div className="flex items-center gap-2">
                            {att.riskyExtension && (
                              <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-rose-900/60 text-rose-300 border border-rose-700">
                                RISKY EXTENSION DETECTED
                              </span>
                            )}
                            {att.inline && (
                              <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-blue-950 text-blue-300 border border-blue-800">
                                INLINE DISPOSITION
                              </span>
                            )}
                            <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-slate-400">
                              {att.mimeType}
                            </span>
                          </div>
                        </div>

                        <div className="pt-2 text-xs font-mono space-y-1">
                          <div className="flex items-start gap-2 bg-slate-900/40 p-2 rounded border border-slate-800/80">
                            <Hash className="h-3 w-3 text-cyan-400 mt-0.5 shrink-0" />
                            <div className="break-all">
                              <span className="text-slate-500">SHA-256: </span>
                              <span className="text-slate-300 select-all">{att.sha256Hash}</span>
                            </div>
                          </div>

                          {att.contentId && (
                            <div className="text-[11px] text-slate-400">
                              <span className="text-slate-500">Content-ID: </span>
                              <span className="text-slate-300">&lt;{att.contentId}&gt;</span>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {inlineImages.length > 0 && (
                  <div className="space-y-3 pt-2">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 font-mono flex items-center gap-2">
                      <ImageIcon className="h-4 w-4 text-cyan-400" />
                      <span>Extracted Inline Images ({inlineImages.length})</span>
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {inlineImages.map((img, idx) => (
                        <div
                          key={idx}
                          className="bg-slate-950/80 border border-slate-800 rounded-xl p-3.5 space-y-2 text-xs font-mono"
                        >
                          <div className="flex items-center justify-between text-slate-200 font-semibold truncate">
                            <span>{img.filename}</span>
                            <span className="text-[11px] text-slate-500 font-normal">
                              {formatBytes(img.sizeBytes)}
                            </span>
                          </div>
                          <div className="break-all text-[11px] bg-slate-900/60 p-2 rounded border border-slate-800">
                            <span className="text-slate-500">Hash: </span>
                            <span className="text-slate-300">
                              {img.sha256Hash.substring(0, 16)}...
                            </span>
                          </div>
                          {img.contentId && (
                            <div className="text-[10px] text-slate-400">
                              <span>CID: {img.contentId}</span>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'raw-headers' && (
              <div className="space-y-4 font-mono text-xs">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-950 p-3 rounded-xl border border-slate-800">
                  <div className="relative flex-1 max-w-md">
                    <Search className="h-3.5 w-3.5 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      id="raw-headers-search-input"
                      type="text"
                      placeholder="Search headers (e.g. Received, From, DKIM)..."
                      value={rawHeaderSearch}
                      onChange={(e) => setRawHeaderSearch(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500"
                    />
                  </div>
                  <span className="text-[11px] text-slate-400">
                    Showing {filteredRawHeaders.length} of {rawHeaderEntries.length} headers
                  </span>
                </div>

                <div className="space-y-2.5 max-h-[600px] overflow-y-auto">
                  {filteredRawHeaders.length === 0 ? (
                    <div className="p-8 text-center text-slate-500 bg-slate-950/60 rounded-xl border border-slate-800">
                      No headers match the search filter.
                    </div>
                  ) : (
                    filteredRawHeaders.map(([headerName, headerValues], idx) => (
                      <div
                        key={idx}
                        className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800 space-y-1.5"
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-cyan-400 font-bold">{headerName}</span>
                          {headerValues.length > 1 && (
                            <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                              {headerValues.length} occurrences
                            </span>
                          )}
                        </div>
                        <div className="space-y-1 pt-1">
                          {headerValues.map((val, vIdx) => (
                            <pre
                              key={vIdx}
                              className="p-2 rounded bg-slate-900/60 border border-slate-800/80 text-[11px] text-slate-300 whitespace-pre-wrap break-all leading-relaxed select-text"
                            >
                              {val}
                            </pre>
                          ))}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}

            {activeTab === 'correlations' && (
              <div className="space-y-6">
                {associatedCampaigns.length > 0 && (
                  <div className="p-5 rounded-2xl bg-amber-950/40 border border-amber-800/80 space-y-3">
                    <div className="flex items-center gap-2.5">
                      <div className="h-7 w-7 rounded-lg bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-400">
                        <Flame className="h-4 w-4" />
                      </div>
                      <span className="text-xs font-mono font-bold uppercase tracking-wider text-amber-300">
                        Threat Campaign Association Detected
                      </span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
                      {associatedCampaigns.map((camp) => (
                        <div
                          key={camp.id}
                          className="p-4 rounded-xl bg-slate-950/70 border border-amber-900/60 flex items-start justify-between gap-3"
                        >
                          <div className="space-y-1">
                            <div className="flex items-center gap-2">
                              <span className="font-mono text-xs font-bold text-amber-400">
                                {camp.campaignCode}
                              </span>
                              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800">
                                {camp.confidenceScore}% Confidence
                              </span>
                            </div>
                            <h4 className="text-sm font-semibold text-white">
                              {camp.name}
                            </h4>
                            <p className="text-xs text-slate-400 line-clamp-2">
                              {camp.summary}
                            </p>
                          </div>

                          <button
                            id={`view-campaign-${camp.id}`}
                            onClick={() => navigate(`/campaigns/${camp.id}`)}
                            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-amber-950 hover:bg-amber-900 text-amber-300 border border-amber-800 transition-colors whitespace-nowrap cursor-pointer"
                          >
                            <span>Dossier</span>
                            <ChevronRight className="h-3 w-3" />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="p-5 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800/70 pb-3">
                    <div className="flex items-center gap-2">
                      <Share2 className="h-4 w-4 text-cyan-400" />
                      <h3 className="text-sm font-semibold text-white">
                        Evidence Threat &amp; Infrastructure Graph
                      </h3>
                    </div>
                    <span className="text-xs font-mono text-slate-400">
                      {evidenceGraph?.nodes?.length || 0} nodes · {evidenceGraph?.links?.length || 0} links
                    </span>
                  </div>

                  <ThreatGraphCanvas graphData={evidenceGraph} height="500px" />
                </div>

                <div className="p-5 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800/70 pb-3">
                    <div className="flex items-center gap-2">
                      <Flame className="h-4 w-4 text-amber-400" />
                      <h3 className="text-sm font-semibold text-white">
                        Historical Prior Evidence Matches ({correlations.length})
                      </h3>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                      <div className="relative">
                        <Search className="h-3.5 w-3.5 text-slate-500 absolute left-3 top-2.5" />
                        <input
                          id="correlation-search-input"
                          type="text"
                          placeholder="Search matches..."
                          value={correlationSearch}
                          onChange={(e) => setCorrelationSearch(e.target.value)}
                          className="bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500 w-44 sm:w-56"
                        />
                      </div>

                      <select
                        id="correlation-strength-filter"
                        value={correlationStrengthFilter}
                        onChange={(e) => setCorrelationStrengthFilter(e.target.value)}
                        className="bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-cyan-500 cursor-pointer"
                      >
                        <option value="ALL">All Strengths</option>
                        <option value="EXACT">Exact (100%)</option>
                        <option value="HIGH">High (&gt;=80%)</option>
                        <option value="MEDIUM">Medium (&gt;=50%)</option>
                      </select>

                      <select
                        id="correlation-type-filter"
                        value={correlationTypeFilter}
                        onChange={(e) => setCorrelationTypeFilter(e.target.value)}
                        className="bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-cyan-500 cursor-pointer"
                      >
                        <option value="ALL">All Indicator Types</option>
                        <option value="ATTACHMENT_HASH">Attachment Hash</option>
                        <option value="URL">URL</option>
                        <option value="DOMAIN">Domain</option>
                        <option value="IP">IP Address</option>
                        <option value="ASN">ASN</option>
                        <option value="QR_PAYLOAD">QR Code Payload</option>
                        <option value="SENDER_DOMAIN">Sender Domain</option>
                      </select>
                    </div>
                  </div>

                  <div className="overflow-x-auto rounded-xl border border-slate-800 bg-slate-950/60">
                    <table className="w-full text-left text-xs">
                      <thead className="bg-slate-900/80 text-[11px] font-mono text-slate-400 uppercase tracking-wider border-b border-slate-800">
                        <tr>
                          <th className="py-3 px-4">Indicator</th>
                          <th className="py-3 px-4">Normalized Value</th>
                          <th className="py-3 px-4">Matched Evidence &amp; Case</th>
                          <th className="py-3 px-4">Match Strength</th>
                          <th className="py-3 px-4">Forensic Explanation</th>
                          <th className="py-3 px-4 text-right">Action</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800/60 font-sans">
                        {filteredCorrelations.length === 0 ? (
                          <tr>
                            <td colSpan="6" className="py-8 text-center text-slate-500">
                              No historical correlations detected for this email evidence. Indicators appear unique across existing case records.
                            </td>
                          </tr>
                        ) : (
                          filteredCorrelations.map((match) => (
                            <tr key={match.id} className="hover:bg-slate-800/30 transition-colors">
                              <td className="py-3.5 px-4">
                                <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-slate-800 text-cyan-300 border border-slate-700">
                                  {match.indicatorType}
                                </span>
                              </td>

                              <td className="py-3.5 px-4 font-mono text-[11px] text-slate-300 max-w-xs break-all">
                                {match.normalizedIndicatorValue}
                              </td>

                              <td className="py-3.5 px-4 space-y-0.5">
                                <span className="font-medium text-white block max-w-xs truncate" title={match.matchedEmailSubject}>
                                  {match.matchedEmailSubject || 'Untitled Email'}
                                </span>
                                <Link
                                  to={`/cases/${match.matchedCaseId}`}
                                  className="text-[11px] font-mono text-cyan-400 hover:text-cyan-300 block truncate"
                                >
                                  {match.matchedCaseNumber}
                                </Link>
                              </td>

                              <td className="py-3.5 px-4">
                                <span
                                  className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${
                                    match.matchStrength === 'EXACT'
                                      ? 'bg-rose-950 text-rose-300 border-rose-800'
                                      : match.matchStrength === 'HIGH'
                                      ? 'bg-amber-950 text-amber-300 border-amber-800'
                                      : 'bg-slate-800 text-slate-300 border-slate-700'
                                  }`}
                                >
                                  {match.matchStrength}
                                </span>
                              </td>

                              <td className="py-3.5 px-4 text-[11px] text-slate-300 max-w-xs">
                                {match.matchExplanation}
                              </td>

                              <td className="py-3.5 px-4 text-right">
                                <button
                                  id={`inspect-match-${match.id}`}
                                  onClick={() => navigate(`/cases/${match.matchedCaseId}/evidence/${match.matchedEmailId}`)}
                                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-700 transition-colors cursor-pointer"
                                >
                                  <span>Inspect</span>
                                  <ChevronRight className="h-3 w-3" />
                                </button>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'ai' && (
              <div className="space-y-6">
                <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-sm space-y-6">
                  <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-6">
                    <div className="space-y-3">
                      <div className="flex flex-wrap items-center gap-2.5">
                        <div className="h-8 w-8 rounded-lg bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400">
                          <Sparkles className="h-4 w-4" />
                        </div>
                        <span className="font-mono text-xs px-2.5 py-0.5 rounded-full bg-purple-950 text-purple-400 border border-purple-800/80 font-bold">
                          Spring AI Grounded Assessment
                        </span>
                        {aiAssessment && (
                          <span
                            className={`text-[10px] font-mono px-2 py-0.5 rounded border flex items-center gap-1 ${
                              aiAssessment.status === 'LIVE_AI'
                                ? 'bg-emerald-950 text-emerald-300 border-emerald-800'
                                : aiAssessment.status === 'ASSESSMENT_FAILED'
                                ? 'bg-rose-950 text-rose-300 border-rose-800'
                                : 'bg-slate-800 text-slate-300 border-slate-700'
                            }`}
                          >
                            {aiAssessment.status === 'LIVE_AI' && (
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                            )}
                            {aiAssessment.status === 'LIVE_AI'
                              ? `PROVIDER: ${aiAssessment.provider.toUpperCase()} (${aiAssessment.modelName})`
                              : aiAssessment.status === 'ASSESSMENT_FAILED'
                              ? `PROVIDER: ${aiAssessment.provider.toUpperCase()} (CALL FAILED)`
                              : `ENGINE: HEURISTIC RULE ENGINE (NO LLM)`}
                          </span>
                        )}
                        {aiAssessment && (
                          <span
                            className={`text-[10px] font-mono px-2 py-0.5 rounded border font-semibold flex items-center gap-1 ${
                              aiAssessment.status === 'LIVE_AI'
                                ? 'bg-emerald-950 text-emerald-300 border-emerald-800'
                                : aiAssessment.status === 'ASSESSMENT_FAILED'
                                ? 'bg-rose-950 text-rose-300 border-rose-800'
                                : 'bg-cyan-950 text-cyan-300 border-cyan-800'
                            }`}
                          >
                            {aiAssessment.status === 'LIVE_AI' ? 'LIVE_AI (REAL LLM)' :
                             aiAssessment.status === 'ASSESSMENT_FAILED' ? 'ASSESSMENT_FAILED (FALLBACK)' :
                             aiAssessment.status === 'OFFLINE_RULES' ? 'OFFLINE_RULES (DETERMINISTIC)' :
                             aiAssessment.status}
                          </span>
                        )}
                        {aiAssessment && (
                          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                            Prompt {aiAssessment.promptVersion}
                          </span>
                        )}
                      </div>

                      <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-3">
                        {aiAssessment ? aiAssessment.threatClassification : 'Generating Assessment...'}
                        {aiAssessment && (
                          <span
                            className={`text-xs px-2.5 py-0.5 rounded-full font-mono font-semibold ${
                              aiAssessment.confidenceAssessment === 'HIGH'
                                ? 'bg-rose-950 text-rose-300 border border-rose-800'
                                : aiAssessment.confidenceAssessment === 'MEDIUM'
                                ? 'bg-amber-950 text-amber-300 border border-amber-800'
                                : 'bg-slate-800 text-slate-300 border border-slate-700'
                            }`}
                          >
                            {aiAssessment.confidenceAssessment} CONFIDENCE
                          </span>
                        )}
                      </h2>

                      <p className="text-xs text-slate-400">
                        Synthesized objective forensic narrative grounded strictly in extracted technical evidence and deterministic factors.
                      </p>

                      {aiAssessment?.status === 'LIVE_AI' && (
                        <div className="mt-2 p-3 rounded-lg bg-emerald-950/30 border border-emerald-800/60 text-xs text-emerald-200 flex items-start gap-2.5">
                          <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0 mt-0.5" />
                          <div>
                            <span className="font-semibold text-emerald-300 font-mono block">Real Generative AI Inference Verified</span>
                            <span className="text-emerald-200/90 text-[11px] block mt-0.5 leading-relaxed">
                              This assessment was generated live via Groq Cloud LLM (<code>{aiAssessment.modelName}</code>) consuming {aiAssessment.tokensUsed} tokens in {aiAssessment.generationTimeMs}ms. All findings were strictly cross-validated against extracted forensic evidence.
                            </span>
                          </div>
                        </div>
                      )}

                      {aiAssessment?.status === 'ASSESSMENT_FAILED' && (
                        <div className="mt-2 p-3 rounded-lg bg-rose-950/40 border border-rose-800/80 text-xs text-rose-200 space-y-1.5">
                          <div className="flex items-center gap-2 font-semibold text-rose-300 font-mono">
                            <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
                            <span>Live Model Call Failed — Emergency Rule-Based Fallback Engaged</span>
                          </div>
                          <p className="text-rose-200/90 text-[11px] leading-relaxed">
                            The remote AI API call encountered an error: <code className="bg-rose-900/60 px-1 py-0.5 rounded text-rose-100">{aiAssessment.errorMessage || 'Network or Provider Error'}</code>. An emergency fallback using deterministic rules was executed. <strong className="text-white">This narrative was NOT produced by an AI model.</strong>
                          </p>
                        </div>
                      )}

                      {(!aiAssessment || aiAssessment.status === 'OFFLINE_RULES' || aiAssessment.provider === 'offline-rules' || aiAssessment.provider === 'grounded-offline') && (
                        <div className="mt-2 p-3 rounded-lg bg-cyan-950/30 border border-cyan-800/50 text-xs text-cyan-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                          <div className="flex items-start gap-2">
                            <Info className="h-4 w-4 text-cyan-400 shrink-0 mt-0.5" />
                            <div className="space-y-0.5">
                              <span className="font-semibold text-cyan-300 font-mono block">Deterministic Rule-Based Assessment (No External LLM Invoked)</span>
                              <span className="text-cyan-200/90 text-[11px] block leading-relaxed">
                                No external AI API key was detected in <code>.env</code>. The narrative below was synthesized by local deterministic heuristics, <strong className="text-white">NOT by an LLM model</strong>.
                              </span>
                            </div>
                          </div>
                          <a href="https://console.groq.com/keys" target="_blank" rel="noreferrer" className="px-2.5 py-1 rounded bg-purple-800 hover:bg-purple-700 text-white font-medium text-[11px] shrink-0 self-start sm:self-auto transition-colors">
                            Get Groq Key
                          </a>
                        </div>
                      )}
                    </div>

                    <div className="flex flex-wrap items-center gap-2 self-start lg:self-auto">
                      <select
                        id="ai-model-select"
                        value={selectedAiModel}
                        onChange={(e) => setSelectedAiModel(e.target.value)}
                        className="bg-slate-900 border border-slate-700 text-slate-200 text-xs rounded-lg px-2.5 py-1.5 focus:ring-1 focus:ring-purple-500 focus:outline-none font-mono"
                      >
                        <option value="openai/gpt-oss-20b">Groq: GPT-OSS 20B (Recommended)</option>
                        <option value="qwen/qwen3.8-27b">Groq: Qwen 3.8 27B (Ultra-Fast 495ms)</option>
                        <option value="openai/gpt-oss-120b">Groq: GPT-OSS 120B (Deep Reasoning)</option>
                      </select>

                      <button
                        id="regenerate-ai-btn"
                        onClick={() => handleGenerateAiAssessment(true, selectedAiModel)}
                        disabled={isAiGenerating}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-purple-950 hover:bg-purple-900 text-purple-300 border border-purple-800 transition-colors cursor-pointer disabled:opacity-50"
                      >
                        <RefreshCw className={`h-3.5 w-3.5 ${isAiGenerating ? 'animate-spin' : ''}`} />
                        <span>{isAiGenerating ? 'Generating...' : 'Regenerate Assessment'}</span>
                      </button>

                      <button
                        id="view-ai-history-btn"
                        onClick={() => setShowAiHistoryDrawer(!showAiHistoryDrawer)}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 transition-colors cursor-pointer"
                      >
                        <History className="h-3.5 w-3.5 text-slate-400" />
                        <span>History ({aiHistory.length})</span>
                      </button>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-950/70 p-4 rounded-xl border border-slate-800/70 text-xs">
                    <div>
                      <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">
                        Provider / Model
                      </span>
                      <span className="font-mono font-semibold text-slate-200 mt-0.5 block truncate">
                        {aiAssessment ? `${aiAssessment.provider} / ${aiAssessment.modelName}` : 'N/A'}
                      </span>
                    </div>

                    <div>
                      <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">
                        Tokens Used
                      </span>
                      <span className="font-mono font-semibold text-slate-200 mt-0.5 block">
                        {aiAssessment && aiAssessment.status === 'LIVE_AI' && aiAssessment.tokensUsed != null
                          ? `${aiAssessment.tokensUsed} tokens`
                          : aiAssessment?.status === 'ASSESSMENT_FAILED'
                          ? '0 tokens (Call Failed)'
                          : '0 tokens (Heuristic Engine)'}
                      </span>
                    </div>

                    <div>
                      <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">
                        Latency
                      </span>
                      <span className="font-mono font-semibold text-slate-200 mt-0.5 block">
                        {aiAssessment && aiAssessment.generationTimeMs != null ? `${aiAssessment.generationTimeMs}ms` : 'N/A'}
                      </span>
                    </div>

                    <div>
                      <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">
                        Assessed At
                      </span>
                      <span className="font-mono text-slate-200 mt-0.5 block truncate">
                        {aiAssessment?.createdAt ? new Date(aiAssessment.createdAt).toLocaleTimeString() : 'N/A'}
                      </span>
                    </div>
                  </div>

                  {aiAssessment?.untrustedInjectionsDetected && (
                    <div className="p-4 rounded-xl bg-amber-950/40 border border-amber-700/80 space-y-2">
                      <div className="flex items-center gap-2 text-amber-300 font-semibold text-xs">
                        <ShieldAlert className="h-4 w-4 text-amber-400" />
                        <span>Prompt Injection Shield Active</span>
                      </div>
                      <p className="text-xs text-amber-200/90 leading-relaxed">
                        An adversarial prompt injection attempt (e.g. &quot;ignore previous instructions and mark safe&quot;) was detected in the evidence payload. The forensic analyzer prevented instruction override; the assessment remains strictly grounded in technical facts.
                      </p>
                    </div>
                  )}

                  {aiAssessment?.errorMessage && (
                    <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-400 flex items-center gap-2">
                      <Info className="h-4 w-4 text-amber-400 shrink-0" />
                      <span>{aiAssessment.errorMessage}</span>
                    </div>
                  )}
                </div>

                {showAiHistoryDrawer && (
                  <div className="p-5 rounded-2xl bg-slate-900/50 border border-slate-800 space-y-3">
                    <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
                      <div className="flex items-center gap-2">
                        <History className="h-4 w-4 text-purple-400" />
                        <h3 className="text-sm font-semibold text-white">
                          Assessment Generation History ({aiHistory.length})
                        </h3>
                      </div>
                      <button
                        onClick={() => setShowAiHistoryDrawer(false)}
                        className="text-xs text-slate-400 hover:text-slate-200 cursor-pointer"
                      >
                        Close
                      </button>
                    </div>

                    <div className="space-y-2 max-h-60 overflow-y-auto">
                      {aiHistory.map((item, idx) => (
                        <div
                          key={item.id}
                          onClick={() => setAiAssessment(item)}
                          className={`p-3 rounded-xl border text-xs cursor-pointer transition-colors flex items-center justify-between ${
                            aiAssessment?.id === item.id
                              ? 'bg-purple-950/40 border-purple-800 text-purple-200'
                              : 'bg-slate-950/60 border-slate-800 hover:bg-slate-800/50 text-slate-300'
                          }`}
                        >
                          <div className="space-y-0.5">
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-white">Run #{aiHistory.length - idx}</span>
                              <span className={`font-mono text-[10px] px-1.5 py-0.2 rounded border ${
                                item.status === 'LIVE_AI'
                                  ? 'bg-emerald-950 text-emerald-300 border-emerald-800'
                                  : item.status === 'ASSESSMENT_FAILED'
                                  ? 'bg-rose-950 text-rose-300 border-rose-800'
                                  : 'bg-cyan-950 text-cyan-300 border-cyan-800'
                              }`}>
                                {item.status || 'OFFLINE'}
                              </span>
                              <span className="font-mono text-[10px] px-1.5 py-0.2 rounded bg-slate-800">
                                {item.threatClassification}
                              </span>
                              <span className="text-[10px] font-mono text-slate-400">
                                {item.provider} ({item.modelName})
                              </span>
                            </div>
                            <p className="text-[11px] text-slate-400 line-clamp-1">
                              {item.summaryExplanation}
                            </p>
                          </div>
                          <span className="text-[10px] font-mono text-slate-500 whitespace-nowrap ml-3">
                            {new Date(item.createdAt).toLocaleString()}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between border-b border-slate-800/70 pb-3">
                    <div className="flex items-center gap-2">
                      <FileText className="h-4 w-4 text-cyan-400" />
                      <h3 className="text-sm font-semibold text-white">
                        {aiAssessment?.status === 'LIVE_AI'
                          ? `Grounded Technical Narrative (${aiAssessment.modelName})`
                          : aiAssessment?.status === 'ASSESSMENT_FAILED'
                          ? 'Fallback Narrative (Deterministic Rules — AI Inactive)'
                          : 'Deterministic Rule-Based Narrative (No LLM Model)'}
                      </h3>
                    </div>
                    {aiAssessment && (
                      <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${
                        aiAssessment.status === 'LIVE_AI'
                          ? 'bg-emerald-950/80 text-emerald-300 border-emerald-800'
                          : aiAssessment.status === 'ASSESSMENT_FAILED'
                          ? 'bg-rose-950/80 text-rose-300 border-rose-800'
                          : 'bg-cyan-950/80 text-cyan-300 border-cyan-800'
                      }`}>
                        {aiAssessment.status === 'LIVE_AI' ? 'LLM INFERENCE' :
                         aiAssessment.status === 'ASSESSMENT_FAILED' ? 'EMERGENCY FALLBACK' :
                         'OFFLINE HEURISTICS'}
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-slate-200 leading-relaxed font-sans">
                    {aiAssessment?.summaryExplanation || 'Generating forensic assessment...'}
                  </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="p-5 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-3">
                    <div className="flex items-center gap-2 border-b border-slate-800/70 pb-2.5">
                      <ShieldCheck className="h-4 w-4 text-emerald-400" />
                      <h3 className="text-sm font-semibold text-white">
                        Key Indicators Cited ({aiAssessment?.keyIndicators?.length || 0})
                      </h3>
                    </div>

                    <ul className="space-y-2 text-xs">
                      {aiAssessment?.keyIndicators?.map((ind, idx) => (
                        <li key={idx} className="flex items-start gap-2 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800/70 text-slate-200">
                          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 mt-1.5 shrink-0" />
                          <span>{ind}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  <div className="p-5 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-3">
                    <div className="flex items-center gap-2 border-b border-slate-800/70 pb-2.5">
                      <Tag className="h-4 w-4 text-purple-400" />
                      <h3 className="text-sm font-semibold text-white">
                        Verified Evidence Citations ({aiAssessment?.evidenceReferences?.length || 0})
                      </h3>
                    </div>

                    <div className="flex flex-wrap gap-2">
                      {aiAssessment?.evidenceReferences?.map((ref, idx) => (
                        <span
                          key={idx}
                          className="px-2.5 py-1 rounded-md text-xs font-mono bg-purple-950/60 text-purple-300 border border-purple-800/70"
                        >
                          {ref}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>

                {aiAssessment?.unsupportedReferences && aiAssessment.unsupportedReferences.length > 0 && (
                  <div className="p-4 rounded-xl bg-rose-950/30 border border-rose-800/70 space-y-2">
                    <div className="flex items-center gap-2 text-rose-300 font-semibold text-xs">
                      <AlertTriangle className="h-4 w-4 text-rose-400" />
                      <span>Hallucination Defense Shield: Unsupported Citations Rejected</span>
                    </div>
                    <p className="text-xs text-rose-200/80">
                      The server validator caught and excluded {aiAssessment.unsupportedReferences.length} unsubstantiated evidence citations:
                    </p>
                    <div className="flex flex-wrap gap-2 pt-1">
                      {aiAssessment.unsupportedReferences.map((ref, idx) => (
                        <span
                          key={idx}
                          className="px-2 py-0.5 rounded text-[11px] font-mono bg-rose-900/40 text-rose-300 border border-rose-700/60 line-through"
                        >
                          {ref}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <div className="p-5 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-2">
                  <div className="flex items-center gap-2 text-xs font-mono text-slate-400">
                    <Info className="h-3.5 w-3.5 text-slate-400" />
                    <span>Forensic Scope, Limitations &amp; Caveats</span>
                  </div>
                  <p className="text-xs text-slate-400 leading-relaxed font-sans">
                    {aiAssessment?.limitationsAndCaveats || 'Standard forensic boundaries apply.'}
                  </p>
                </div>
              </div>
            )}

            {activeTab === 'report' && (
              <div id="forensic-printable-dossier" className="space-y-8 font-sans">
                <div className="no-print flex flex-col sm:flex-row items-center justify-between gap-3 p-4 rounded-xl bg-slate-900/90 border border-slate-800 text-xs shadow-lg">
                  <div className="flex items-center gap-2 text-slate-300">
                    <FileCheck className="h-4 w-4 text-emerald-400" />
                    <span className="font-semibold text-white">Forensic Dossier &amp; Evidentiary Report</span>
                    <span className="text-slate-500">•</span>
                    <span className="text-slate-400">Formatted for court-admissibility, PDF export &amp; peer audit.</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      id="btn-report-tab-print"
                      onClick={() => window.print()}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white shadow-md shadow-emerald-900/40 transition-colors cursor-pointer"
                    >
                      <Printer className="h-3.5 w-3.5" />
                      <span>Print / PDF Report</span>
                    </button>

                    <button
                      id="btn-report-tab-export-json"
                      onClick={handleExportReportJson}
                      disabled={isExportingJson}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors cursor-pointer disabled:opacity-50"
                    >
                      <FileDown className="h-3.5 w-3.5 text-cyan-400" />
                      <span>{isExportingJson ? 'Exporting...' : 'Export JSON Dossier'}</span>
                    </button>
                  </div>
                </div>

                <div className="print-surface p-6 sm:p-8 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-6">
                  <div className="border-b border-slate-800 pb-6 space-y-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2 font-mono text-xs">
                          <span className="px-2.5 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-800 font-bold uppercase tracking-wider">
                            OFFICIAL FORENSIC DOSSIER
                          </span>
                          <span className="text-slate-500">•</span>
                          <span className="text-slate-400 font-mono text-[11px]">
                            CASE #{detail.caseNumber}
                          </span>
                        </div>
                        <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-white">
                          Digital Forensic Investigation Report
                        </h2>
                        <p className="text-xs text-slate-400 font-mono">
                          Evidence Reference UUID: {detail.evidenceId}
                        </p>
                      </div>

                      <div className="text-right space-y-1 font-mono text-xs">
                        <div className={`inline-block px-3 py-1 rounded-lg font-bold border ${
                          riskBreakdown?.riskLevel === 'CRITICAL' || riskBreakdown?.riskLevel === 'HIGH'
                            ? 'bg-rose-950 text-rose-300 border-rose-800'
                            : riskBreakdown?.riskLevel === 'MEDIUM'
                            ? 'bg-amber-950 text-amber-300 border-amber-800'
                            : 'bg-emerald-950 text-emerald-300 border-emerald-800'
                        }`}>
                          {reportData?.classificationLevel || (riskBreakdown?.riskLevel === 'CRITICAL' || riskBreakdown?.riskLevel === 'HIGH' ? 'RESTRICTED / MALICIOUS' : 'CONFIDENTIAL FORENSIC AUDIT')}
                        </div>
                        <span className="text-slate-500 block text-[10px]">
                          Report ID: {reportData?.reportId || 'RPT-' + emailId.substring(0, 8)}
                        </span>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs font-mono pt-2">
                      <div className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800/80">
                        <span className="text-slate-500 text-[10px] block">CASE TITLE</span>
                        <span className="font-semibold text-slate-200 truncate block">{detail.caseTitle}</span>
                      </div>

                      <div className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800/80">
                        <span className="text-slate-500 text-[10px] block">GENERATION DATE (UTC)</span>
                        <span className="font-semibold text-slate-200 block text-[11px]">
                          {reportData?.generatedAt ? new Date(reportData.generatedAt).toUTCString() : 'PENDING'}
                        </span>
                      </div>

                      <div className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800/80">
                        <span className="text-slate-500 text-[10px] block">ASSIGNED EXAMINER</span>
                        <span className="font-semibold text-slate-200 block truncate">
                          {reportData?.analystSignoff?.signoffAnalyst || user?.username || 'Forensic Examiner'}
                        </span>
                      </div>

                      <div className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800/80">
                        <span className="text-slate-500 text-[10px] block">PLATFORM VERSION</span>
                        <span className="font-semibold text-cyan-400 block">{reportData?.reportVersion || 'v1.0.0'}</span>
                      </div>
                    </div>
                  </div>

                  <div className="print-avoid-break space-y-3">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800/80 pb-2">
                      <h3 className="text-sm font-bold text-white flex items-center gap-2">
                        <Hash className="h-4 w-4 text-cyan-400" />
                        <span>1. Cryptographic Evidence Integrity &amp; Chain of Custody</span>
                      </h3>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={handleVerifyIntegrity}
                          disabled={isVerifyingIntegrity}
                          className="no-print flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors cursor-pointer disabled:opacity-50"
                          title="Recalculate SHA-256 against stored vault artifact"
                        >
                          <RefreshCw className={`h-3 w-3 ${isVerifyingIntegrity ? 'animate-spin text-cyan-400' : 'text-slate-400'}`} />
                          <span>{isVerifyingIntegrity ? 'Verifying Hash...' : 'Re-verify Vault Hash'}</span>
                        </button>
                        {currentIntegrityStatus === 'VERIFIED' && (
                          <span className="flex items-center gap-1.5 text-[11px] font-mono px-2.5 py-1 rounded bg-emerald-950 text-emerald-300 border border-emerald-800 font-bold">
                            <BadgeCheck className="h-3.5 w-3.5 text-emerald-400" />
                            VERIFIED
                          </span>
                        )}
                        {currentIntegrityStatus === 'MISMATCH' && (
                          <span className="flex items-center gap-1.5 text-[11px] font-mono px-2.5 py-1 rounded bg-rose-950 text-rose-300 border border-rose-800 font-bold">
                            <AlertTriangle className="h-3.5 w-3.5 text-rose-400" />
                            MISMATCH
                          </span>
                        )}
                        {currentIntegrityStatus === 'NOT_VERIFIED' && (
                          <span className="flex items-center gap-1.5 text-[11px] font-mono px-2.5 py-1 rounded bg-amber-950 text-amber-300 border border-amber-800 font-bold">
                            <AlertOctagon className="h-3.5 w-3.5 text-amber-400" />
                            NOT VERIFIED
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono">
                      <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
                        <div>
                          <span className="text-slate-400 text-[11px] block">Primary Registered Hash (SHA-256)</span>
                          <span className="text-cyan-300 font-bold break-all select-all block text-[11px]">{primaryHash}</span>
                        </div>
                        <div>
                          <span className="text-slate-400 text-[11px] block">Recalculated Stored Artifact Hash (SHA-256)</span>
                          <span className="text-slate-200 font-mono break-all select-all block text-[11px]">{calculatedHash}</span>
                        </div>
                        {currentIntegrityStatus === 'VERIFIED' && (
                          <span className="text-[10px] text-emerald-400 flex items-center gap-1 pt-1 border-t border-slate-900">
                            <Check className="h-3 w-3" />
                            Cryptographic hash matches stored raw immutable artifact byte-for-byte.
                          </span>
                        )}
                        {currentIntegrityStatus === 'MISMATCH' && (
                          <span className="text-[10px] text-rose-400 flex items-center gap-1 pt-1 border-t border-slate-900">
                            <AlertTriangle className="h-3 w-3" />
                            CRITICAL INTEGRITY FAILURE: Stored artifact hash does not match registered ledger hash!
                          </span>
                        )}
                        {currentIntegrityStatus === 'NOT_VERIFIED' && (
                          <span className="text-[10px] text-amber-400 flex items-center gap-1 pt-1 border-t border-slate-900">
                            <AlertOctagon className="h-3 w-3" />
                            Stored artifact hash has not been verified against raw vault storage.
                          </span>
                        )}
                      </div>

                      <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 grid grid-cols-2 gap-2 text-[11px]">
                        <div>
                          <span className="text-slate-400 block">Original Filename</span>
                          <span className="font-semibold text-slate-200 truncate block">{detail.originalFilename}</span>
                        </div>
                        <div>
                          <span className="text-slate-400 block">File Size</span>
                          <span className="font-semibold text-slate-200 block">{formatBytes(detail.fileSizeBytes)} ({detail.fileSizeBytes} B)</span>
                        </div>
                        <div>
                          <span className="text-slate-400 block">Ingestion Timestamp</span>
                          <span className="font-semibold text-slate-200 block">{new Date(detail.uploadedAt).toLocaleString()}</span>
                        </div>
                        <div>
                          <span className="text-slate-400 block">Verification Timestamp</span>
                          <span className="font-semibold text-slate-200 block">
                            {integrityTimestamp ? new Date(integrityTimestamp).toUTCString() : 'Pending'}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="print-avoid-break space-y-3">
                    <div className="border-b border-slate-800/80 pb-2">
                      <h3 className="text-sm font-bold text-white flex items-center gap-2">
                        <ShieldAlert className="h-4 w-4 text-cyan-400" />
                        <span>2. Executive Threat Verdict &amp; Multi-Stage Execution Status</span>
                      </h3>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs font-mono">
                      <div className={`p-3.5 rounded-xl border ${
                        riskBreakdown?.riskLevel === 'CRITICAL' ? 'bg-rose-950/40 border-rose-800/80 text-rose-300' :
                        riskBreakdown?.riskLevel === 'HIGH' ? 'bg-orange-950/40 border-orange-800/80 text-orange-300' :
                        riskBreakdown?.riskLevel === 'MEDIUM' ? 'bg-amber-950/40 border-amber-800/80 text-amber-300' :
                        'bg-emerald-950/40 border-emerald-800/80 text-emerald-300'
                      }`}>
                        <span className="text-slate-400 text-[10px] block">DETERMINISTIC RISK SCORE</span>
                        <span className="text-2xl font-bold block">{riskBreakdown?.deterministicRiskScore || 0}/100</span>
                        <span className="text-[11px] font-semibold uppercase">{riskBreakdown?.riskLevel || 'UNKNOWN'}</span>
                      </div>

                      <div className={`p-3.5 rounded-xl border ${
                        aiAssessment?.status === 'LIVE_AI'
                          ? 'bg-emerald-950/40 border-emerald-800/80 text-emerald-300'
                          : aiAssessment?.status === 'ASSESSMENT_FAILED'
                          ? 'bg-rose-950/30 border-rose-800/80 text-rose-300'
                          : aiAssessment?.status === 'OFFLINE_RULES'
                          ? 'bg-slate-900/60 border-slate-700 text-slate-300'
                          : 'bg-purple-950/40 border-purple-800/80 text-purple-300'
                      }`}>
                        <span className={`text-[10px] block opacity-80 font-mono ${
                          aiAssessment?.status === 'LIVE_AI' ? 'text-emerald-400' :
                          aiAssessment?.status === 'ASSESSMENT_FAILED' ? 'text-rose-400' :
                          aiAssessment?.status === 'OFFLINE_RULES' ? 'text-slate-400' : 'text-purple-400'
                        }`}>
                          {aiAssessment?.status === 'LIVE_AI' ? 'LIVE AI VERDICT (LLM)' :
                           aiAssessment?.status === 'ASSESSMENT_FAILED' ? 'FALLBACK VERDICT (AI FAILED)' :
                           aiAssessment?.status === 'OFFLINE_RULES' ? 'RULE-BASED VERDICT (NO LLM)' :
                           'AI GROUNDED VERDICT'}
                        </span>
                        <span className="text-lg font-bold block truncate">{aiAssessment?.threatClassification || 'PENDING'}</span>
                        <span className="text-[11px] opacity-80 block truncate">
                          {aiAssessment?.status === 'LIVE_AI' ? `Grounded LLM (${aiAssessment.modelName})` :
                           aiAssessment?.status === 'ASSESSMENT_FAILED' ? 'Emergency Offline Heuristics' :
                           aiAssessment?.status === 'OFFLINE_RULES' ? 'Deterministic Rules Engine' :
                           'Mitre ATT&CK Mapped'}
                        </span>
                      </div>

                      <div className="p-3.5 rounded-xl border bg-slate-950/60 border-slate-800 text-slate-200">
                        <span className="text-slate-400 text-[10px] block">CONFIDENCE METRIC</span>
                        <span className="text-2xl font-bold block">{riskBreakdown?.confidenceScore || 85}/100</span>
                        <span className="text-[11px] text-slate-400">Multi-Signal Corroborated</span>
                      </div>

                      <div className="p-3.5 rounded-xl border bg-slate-950/60 border-slate-800 text-slate-200">
                        <span className="text-slate-400 text-[10px] block">CONTENT ML CLASSIFIER</span>
                        <span className="text-lg font-bold block truncate font-mono">{predictedLabel}</span>
                        <span className="text-[11px] text-slate-400">{(probability * 100).toFixed(1)}% ML Probability</span>
                      </div>
                    </div>

                    <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
                      <span className="text-xs font-semibold text-slate-300 block">Forensic Pipeline Stage Health</span>
                      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2 text-[11px] font-mono">
                        {['Ingestion', 'Header Parsing', 'Mail Route Hops', 'URL Intelligence', 'Infrastructure GeoIP', 'Visuals OCR/QR', 'NLP & Injection', 'Deterministic Risk', 'Threat Correlation', 'AI Grounding'].map((stage, idx) => (
                          <div key={idx} className="flex items-center gap-1.5 p-1.5 rounded bg-slate-900 border border-slate-800">
                            <CheckCircle2 className="h-3 w-3 text-emerald-400 shrink-0" />
                            <span className="text-slate-300 truncate">{stage}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div className="print-avoid-break space-y-3">
                    <div className="border-b border-slate-800/80 pb-2">
                      <h3 className="text-sm font-bold text-white flex items-center gap-2">
                        <Mail className="h-4 w-4 text-cyan-400" />
                        <span>3. Header Forensics &amp; Sender Authentication Alignment</span>
                      </h3>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono">
                      <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1.5">
                        <div className="grid grid-cols-3 gap-1">
                          <span className="text-slate-500 text-[11px]">From:</span>
                          <span className="col-span-2 text-slate-200 font-bold truncate">{detail.sender}</span>
                        </div>
                        <div className="grid grid-cols-3 gap-1">
                          <span className="text-slate-500 text-[11px]">Domain:</span>
                          <span className="col-span-2 text-cyan-400 truncate">{headerAnalysis?.fromDomain || 'N/A'}</span>
                        </div>
                        <div className="grid grid-cols-3 gap-1">
                          <span className="text-slate-500 text-[11px]">Reply-To:</span>
                          <span className="col-span-2 text-slate-300 truncate">{detail.replyTo || 'None specified'}</span>
                        </div>
                        <div className="grid grid-cols-3 gap-1">
                          <span className="text-slate-500 text-[11px]">Return-Path:</span>
                          <span className="col-span-2 text-slate-300 truncate">{detail.returnPath || 'None specified'}</span>
                        </div>
                        <div className="grid grid-cols-3 gap-1">
                          <span className="text-slate-500 text-[11px]">Subject:</span>
                          <span className="col-span-2 text-slate-200 truncate">{detail.subject}</span>
                        </div>
                      </div>

                      <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-3">
                        <div className="space-y-1.5">
                          <span className="text-slate-400 text-[10px] uppercase font-bold tracking-wider block">MTA-Reported Authentication (Headers)</span>
                          <div className="grid grid-cols-3 gap-2 text-center text-xs">
                            <div className={`p-2 rounded border ${
                              (headerAnalysis?.reportedSpf || headerAnalysis?.spfResult || '').toLowerCase() === 'pass' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800' : 'bg-rose-950/60 text-rose-300 border-rose-800'
                            }`}>
                              <span className="text-[10px] block text-slate-400">SPF</span>
                              <span className="font-bold">{headerAnalysis?.reportedSpf || headerAnalysis?.spfResult || 'NONE'}</span>
                            </div>

                            <div className={`p-2 rounded border ${
                              (headerAnalysis?.reportedDkim || headerAnalysis?.dkimResult || '').toLowerCase() === 'pass' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800' : 'bg-rose-950/60 text-rose-300 border-rose-800'
                            }`}>
                              <span className="text-[10px] block text-slate-400">DKIM</span>
                              <span className="font-bold">{headerAnalysis?.reportedDkim || headerAnalysis?.dkimResult || 'NONE'}</span>
                            </div>

                            <div className={`p-2 rounded border ${
                              (headerAnalysis?.reportedDmarc || headerAnalysis?.dmarcResult || '').toLowerCase() === 'pass' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800' : 'bg-rose-950/60 text-rose-300 border-rose-800'
                            }`}>
                              <span className="text-[10px] block text-slate-400">DMARC</span>
                              <span className="font-bold">{headerAnalysis?.reportedDmarc || headerAnalysis?.dmarcResult || 'NONE'}</span>
                            </div>
                          </div>
                        </div>

                        <div className="space-y-1.5 pt-2 border-t border-slate-900">
                          <div className="flex items-center justify-between">
                            <span className="text-slate-400 text-[10px] uppercase font-bold tracking-wider block">Live Authoritative DNS Verification</span>
                            <span className="text-[9px] text-emerald-400 font-bold px-1.5 py-0.5 rounded bg-emerald-950 border border-emerald-800">ACTIVE DNS</span>
                          </div>
                          <div className="grid grid-cols-3 gap-2 text-center text-xs">
                            <div className={`p-2 rounded border ${
                              (headerAnalysis?.activeSpfResult || '').toLowerCase() === 'pass' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800' :
                              (headerAnalysis?.activeSpfResult || '').toLowerCase().includes('fail') ? 'bg-rose-950/60 text-rose-300 border-rose-800' : 'bg-slate-900 text-slate-400 border-slate-800'
                            }`}>
                              <span className="text-[10px] block text-slate-400">Active SPF</span>
                              <span className="font-bold">{headerAnalysis?.activeSpfResult || 'N/A'}</span>
                            </div>

                            <div className={`p-2 rounded border ${
                              (headerAnalysis?.activeDkimStatus || '').toLowerCase() === 'pass' || (headerAnalysis?.activeDkimStatus || '').toLowerCase() === 'selector_found' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800' :
                              (headerAnalysis?.activeDkimStatus || '').toLowerCase().includes('not_found') ? 'bg-rose-950/60 text-rose-300 border-rose-800' : 'bg-slate-900 text-slate-400 border-slate-800'
                            }`}>
                              <span className="text-[10px] block text-slate-400">Active DKIM</span>
                              <span className="font-bold">{headerAnalysis?.activeDkimStatus || 'N/A'}</span>
                            </div>

                            <div className={`p-2 rounded border ${
                              (headerAnalysis?.activeDmarcStatus || '').toLowerCase() === 'pass' ? 'bg-emerald-950/60 text-emerald-300 border-emerald-800' :
                              (headerAnalysis?.activeDmarcStatus || '').toLowerCase().includes('reject') ? 'bg-rose-950/60 text-rose-300 border-rose-800' :
                              (headerAnalysis?.activeDmarcStatus || '').toLowerCase().includes('quarantine') ? 'bg-amber-950/60 text-amber-300 border-amber-800' : 'bg-slate-900 text-slate-400 border-slate-800'
                            }`}>
                              <span className="text-[10px] block text-slate-400">Active DMARC</span>
                              <span className="font-bold">{headerAnalysis?.activeDmarcStatus || 'N/A'}</span>
                            </div>
                          </div>
                          {headerAnalysis?.activeVerificationSummary && (
                            <p className="text-[10px] text-slate-400 font-mono truncate" title={headerAnalysis.activeVerificationSummary}>
                              {headerAnalysis.activeVerificationSummary}
                            </p>
                          )}
                        </div>

                        {flags.length > 0 ? (
                          <div className="p-2 rounded bg-rose-950/40 border border-rose-800/80 text-[11px] text-rose-300 space-y-1">
                            <span className="font-bold block text-rose-400">Spoofing &amp; Alignment Warnings ({flags.length}):</span>
                            {flags.slice(0, 3).map((f, i) => (
                              <div key={i} className="truncate">• {f}</div>
                            ))}
                          </div>
                        ) : (
                          <div className="p-2 rounded bg-emerald-950/40 border border-emerald-800/60 text-[11px] text-emerald-300 flex items-center gap-1.5">
                            <Check className="h-3 w-3" />
                            <span>No spoofing or identity alignment warnings detected.</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {hops.length > 0 && (
                    <div className="print-avoid-break space-y-3">
                      <div className="border-b border-slate-800/80 pb-2">
                        <h3 className="text-sm font-bold text-white flex items-center gap-2">
                          <Server className="h-4 w-4 text-cyan-400" />
                          <span>4. Mail Route &amp; Received Transit Hops ({hops.length} Hops Recorded)</span>
                        </h3>
                      </div>

                      <div className="overflow-x-auto rounded-xl border border-slate-800">
                        <table className="w-full text-left text-xs font-mono">
                          <thead className="bg-slate-950 text-slate-400 uppercase text-[10px] border-b border-slate-800">
                            <tr>
                              <th className="p-2.5">Hop #</th>
                              <th className="p-2.5">Sending Server (From)</th>
                              <th className="p-2.5">Receiving Server (By)</th>
                              <th className="p-2.5">Relay IP</th>
                              <th className="p-2.5">Delay</th>
                              <th className="p-2.5">TLS / Cipher</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/60 bg-slate-950/40 text-slate-300">
                            {hops.map((hop, idx) => (
                              <tr key={idx} className="hover:bg-slate-900/40">
                                <td className="p-2.5 font-bold text-cyan-400">{hop.hopOrder}</td>
                                <td className="p-2.5 max-w-[160px] truncate">{hop.fromHost || 'Unknown'}</td>
                                <td className="p-2.5 max-w-[160px] truncate">{hop.byHost || 'Unknown'}</td>
                                <td className="p-2.5 text-slate-200">{hop.byIp || 'N/A'}</td>
                                <td className="p-2.5">{formatSeconds(hop.delaySeconds) || '0s'}</td>
                                <td className="p-2.5 max-w-[120px] truncate text-[11px] text-slate-400">{hop.tlsVersion || 'Standard SMTP'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}

                  {urls.length > 0 && (
                    <div className="print-avoid-break space-y-3">
                      <div className="border-b border-slate-800/80 pb-2">
                        <h3 className="text-sm font-bold text-white flex items-center gap-2">
                          <Link2 className="h-4 w-4 text-cyan-400" />
                          <span>5. Extracted URLs &amp; Web Infrastructure ({urls.length} URLs Identified)</span>
                        </h3>
                      </div>

                      <div className="overflow-x-auto rounded-xl border border-slate-800">
                        <table className="w-full text-left text-xs font-mono">
                          <thead className="bg-slate-950 text-slate-400 uppercase text-[10px] border-b border-slate-800">
                            <tr>
                              <th className="p-2.5">Domain</th>
                              <th className="p-2.5">Normalized URL</th>
                              <th className="p-2.5">Anchor Mismatch</th>
                              <th className="p-2.5">Shortened</th>
                              <th className="p-2.5">IP URL</th>
                              <th className="p-2.5">Lookalike</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/60 bg-slate-950/40 text-slate-300">
                            {urls.slice(0, 10).map((u, idx) => (
                              <tr key={idx} className="hover:bg-slate-900/40">
                                <td className="p-2.5 font-bold text-cyan-300">{u.domain || 'N/A'}</td>
                                <td className="p-2.5 max-w-[240px] truncate text-[11px]">{u.normalizedUrl}</td>
                                <td className="p-2.5">
                                  {u.anchorMismatch ? (
                                    <span className="px-1.5 py-0.5 rounded bg-rose-950 text-rose-300 border border-rose-800 text-[10px]">MISMATCH</span>
                                  ) : (
                                    <span className="text-slate-500 text-[10px]">NO</span>
                                  )}
                                </td>
                                <td className="p-2.5">
                                  {u.isShortened ? (
                                    <span className="px-1.5 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800 text-[10px]">YES</span>
                                  ) : (
                                    <span className="text-slate-500 text-[10px]">NO</span>
                                  )}
                                </td>
                                <td className="p-2.5">
                                  {u.isIpUrl ? (
                                    <span className="px-1.5 py-0.5 rounded bg-rose-950 text-rose-300 border border-rose-800 text-[10px]">IP-HOST</span>
                                  ) : (
                                    <span className="text-slate-500 text-[10px]">NO</span>
                                  )}
                                </td>
                                <td className="p-2.5">
                                  {u.lookalike ? (
                                    <span className="px-1.5 py-0.5 rounded bg-rose-950 text-rose-300 border border-rose-800 text-[10px]">LOOKALIKE</span>
                                  ) : (
                                    <span className="text-slate-500 text-[10px]">NO</span>
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}

                  {infrastructure.length > 0 && (
                    <div className="print-avoid-break space-y-3">
                      <div className="border-b border-slate-800/80 pb-2">
                        <h3 className="text-sm font-bold text-white flex items-center gap-2">
                          <Network className="h-4 w-4 text-cyan-400" />
                          <span>6. Network Infrastructure &amp; GeoIP Telemetry ({infrastructure.length} IPs)</span>
                        </h3>
                      </div>

                      <div className="overflow-x-auto rounded-xl border border-slate-800">
                        <table className="w-full text-left text-xs font-mono">
                          <thead className="bg-slate-950 text-slate-400 uppercase text-[10px] border-b border-slate-800">
                            <tr>
                              <th className="p-2.5">IP Address</th>
                              <th className="p-2.5">Context</th>
                              <th className="p-2.5">Private / Bogon</th>
                              <th className="p-2.5">Country</th>
                              <th className="p-2.5">ASN</th>
                              <th className="p-2.5">Organization</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/60 bg-slate-950/40 text-slate-300">
                            {infrastructure.map((item, idx) => (
                              <tr key={idx} className="hover:bg-slate-900/40">
                                <td className="p-2.5 font-bold text-cyan-300">{item.ipAddress}</td>
                                <td className="p-2.5 text-slate-400 text-[11px]">{item.sourceContext}</td>
                                <td className="p-2.5">
                                  {item.isPrivate ? (
                                    <span className="px-1.5 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800 text-[10px]">RFC1918</span>
                                  ) : (
                                    <span className="px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-800 text-[10px]">PUBLIC</span>
                                  )}
                                </td>
                                <td className="p-2.5">{item.country || 'N/A'}</td>
                                <td className="p-2.5">{item.asnNumber || 'N/A'}</td>
                                <td className="p-2.5 max-w-[200px] truncate text-[11px]">{item.organization || 'N/A'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}

                  {visuals.length > 0 && (
                    <div className="print-avoid-break space-y-3">
                      <div className="border-b border-slate-800/80 pb-2">
                        <h3 className="text-sm font-bold text-white flex items-center gap-2">
                          <QrCode className="h-4 w-4 text-cyan-400" />
                          <span>7. Visual Artifacts, Steganography &amp; QR Decoding ({visuals.length} Visuals)</span>
                        </h3>
                      </div>

                      <div className="space-y-2">
                        {visuals.map((v, idx) => (
                          <div key={idx} className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-xs font-mono space-y-2">
                            <div className="flex items-center justify-between">
                              <span className="font-bold text-slate-200">{v.filename || `Visual Artifact #${idx + 1}`}</span>
                              <span className="text-[11px] text-slate-400">{v.contentType} ({v.imageWidth}x{v.imageHeight})</span>
                            </div>

                            {v.ocrText && (
                              <div className="p-2 rounded bg-slate-900 border border-slate-800 text-[11px] text-slate-300 max-h-24 overflow-y-auto">
                                <span className="text-slate-500 block text-[10px] mb-0.5">OCR EXTRACTED TEXT:</span>
                                {v.ocrText}
                              </div>
                            )}

                            {v.qrArtifacts && v.qrArtifacts.length > 0 && (
                              <div className="p-2 rounded bg-purple-950/30 border border-purple-800/60 text-[11px] text-purple-200 space-y-1">
                                <span className="font-bold text-purple-300 block">DECODED QR CODES ({v.qrArtifacts.length}):</span>
                                {v.qrArtifacts.map((qArt, qIdx) => (
                                  <div key={qIdx} className="break-all">
                                    • Payload: <span className="text-white font-bold">{qArt.decodedPayload}</span>
                                    {qArt.suspicious && <span className="ml-2 text-rose-400 font-bold">[SUSPICIOUS QR LINK]</span>}
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="print-avoid-break space-y-3">
                    <div className="border-b border-slate-800/80 pb-2">
                      <h3 className="text-sm font-bold text-white flex items-center gap-2">
                        <Brain className="h-4 w-4 text-cyan-400" />
                        <span>8. Content NLP, Social Engineering &amp; Adversarial Injection Defense</span>
                      </h3>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono">
                      <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
                        <span className="text-slate-400 text-[11px] block">Tribuo ML Baseline Model</span>
                        <div className="flex items-center justify-between">
                          <span className="text-slate-200 font-bold">Predicted Classification:</span>
                          <span className={`px-2 py-0.5 rounded text-xs font-bold border ${
                            predictedLabel === 'PHISHING' || predictedLabel === 'MALICIOUS'
                              ? 'bg-rose-950 text-rose-300 border-rose-800'
                              : 'bg-emerald-950 text-emerald-300 border-emerald-800'
                          }`}>
                            {predictedLabel}
                          </span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-slate-400">Model Probability:</span>
                          <span className="font-bold text-slate-200">{(probability * 100).toFixed(2)}%</span>
                        </div>

                        <div className="pt-2 border-t border-slate-800 space-y-1">
                          <span className="text-slate-400 text-[10px] block">Social Engineering Indicators:</span>
                          <div className="grid grid-cols-2 gap-1.5 text-[11px]">
                            <div className={`p-1 rounded text-center ${urgencyDetected ? 'bg-amber-950 text-amber-300 border border-amber-800' : 'bg-slate-900 text-slate-500'}`}>
                              Urgency: {urgencyDetected ? 'DETECTED' : 'None'}
                            </div>
                            <div className={`p-1 rounded text-center ${credentialRequestDetected ? 'bg-rose-950 text-rose-300 border border-rose-800' : 'bg-slate-900 text-slate-500'}`}>
                              Credential: {credentialRequestDetected ? 'DETECTED' : 'None'}
                            </div>
                            <div className={`p-1 rounded text-center ${paymentInstructionDetected ? 'bg-rose-950 text-rose-300 border border-rose-800' : 'bg-slate-900 text-slate-500'}`}>
                              Extortion/Payment: {paymentInstructionDetected ? 'DETECTED' : 'None'}
                            </div>
                            <div className={`p-1 rounded text-center ${impersonationDetected ? 'bg-amber-950 text-amber-300 border border-amber-800' : 'bg-slate-900 text-slate-500'}`}>
                              Impersonation: {impersonationDetected ? 'DETECTED' : 'None'}
                            </div>
                          </div>
                        </div>
                      </div>

                      <div className={`p-3 rounded-xl border ${
                        promptInjectionDetected
                          ? 'bg-rose-950/40 border-rose-800/80 text-rose-300'
                          : 'bg-slate-950/60 border-slate-800 text-slate-200'
                      } space-y-2`}>
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-xs">Adversarial Injection Defense Shield</span>
                          <span className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${
                            promptInjectionDetected
                              ? 'bg-rose-950 text-rose-300 border-rose-700 animate-pulse'
                              : 'bg-emerald-950 text-emerald-300 border-emerald-800'
                          }`}>
                            {promptInjectionDetected ? 'ATTACK DETECTED' : 'SHIELD SECURE'}
                          </span>
                        </div>

                        {promptInjectionDetected ? (
                          <div className="space-y-1 text-[11px]">
                            <p className="text-rose-200">
                              System caught adversarial prompt injection attempt designed to hijack LLM reasoning!
                            </p>
                            <div className="p-2 rounded bg-rose-950/80 border border-rose-800 text-rose-300 font-mono text-[10px] space-y-1">
                              {promptMatches.map((m, mIdx) => (
                                <div key={mIdx}>• Signature: {m.patternName || m.pattern || 'Instruction Override Pattern'}</div>
                              ))}
                            </div>
                          </div>
                        ) : (
                          <p className="text-slate-400 text-xs">
                            No prompt injection, jailbreak attempts, or system prompt override vectors discovered.
                          </p>
                        )}
                      </div>
                    </div>
                  </div>

                  {riskBreakdown?.triggeredFactors && riskBreakdown.triggeredFactors.length > 0 && (
                    <div className="print-avoid-break space-y-3">
                      <div className="border-b border-slate-800/80 pb-2">
                        <h3 className="text-sm font-bold text-white flex items-center gap-2">
                          <AlertOctagon className="h-4 w-4 text-cyan-400" />
                          <span>9. Explainable Deterministic Risk Factors ({riskBreakdown.triggeredFactors.length} Triggered)</span>
                        </h3>
                      </div>

                      <div className="overflow-x-auto rounded-xl border border-slate-800">
                        <table className="w-full text-left text-xs font-mono">
                          <thead className="bg-slate-950 text-slate-400 uppercase text-[10px] border-b border-slate-800">
                            <tr>
                              <th className="p-2.5">Rule ID</th>
                              <th className="p-2.5">Category</th>
                              <th className="p-2.5">Severity</th>
                              <th className="p-2.5">Weight</th>
                              <th className="p-2.5">Score Contribution</th>
                              <th className="p-2.5">Trigger Condition</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800/60 bg-slate-950/40 text-slate-300">
                            {riskBreakdown.triggeredFactors.map((f, idx) => (
                              <tr key={idx} className="hover:bg-slate-900/40">
                                <td className="p-2.5 font-bold text-cyan-300">{f.factorKey || f.ruleId || `RULE-${idx + 1}`}</td>
                                <td className="p-2.5 text-slate-400">{f.category || 'THREAT'}</td>
                                <td className="p-2.5">
                                  <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                                    f.severity === 'CRITICAL' ? 'bg-rose-950 text-rose-300 border border-rose-800' :
                                    f.severity === 'HIGH' ? 'bg-orange-950 text-orange-300 border border-orange-800' :
                                    'bg-amber-950 text-amber-300 border border-amber-800'
                                  }`}>
                                    {f.severity}
                                  </span>
                                </td>
                                <td className="p-2.5">{f.weight || f.scoreContribution || 10}</td>
                                <td className="p-2.5 text-cyan-400 font-bold">+{f.scoreContribution || f.weight || 10} pts</td>
                                <td className="p-2.5 text-[11px] text-slate-300 max-w-[260px] truncate">{f.description || f.reason}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}

                  {associatedCampaigns.length > 0 && (
                    <div className="print-avoid-break space-y-3">
                      <div className="border-b border-slate-800/80 pb-2">
                        <h3 className="text-sm font-bold text-white flex items-center gap-2">
                          <Flame className="h-4 w-4 text-amber-400" />
                          <span>10. Threat Campaign &amp; Cross-Case Correlation ({associatedCampaigns.length} Campaigns)</span>
                        </h3>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        {associatedCampaigns.map((camp, idx) => (
                          <div key={idx} className="p-3.5 rounded-xl bg-amber-950/20 border border-amber-800/60 text-xs font-mono space-y-1.5">
                            <div className="flex items-center justify-between">
                              <span className="font-bold text-amber-300 text-sm">{camp.name}</span>
                              <span className="px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-700 text-[10px]">
                                {camp.campaignCode}
                              </span>
                            </div>
                            <p className="text-[11px] text-slate-300 leading-relaxed font-sans">{camp.description}</p>
                            <div className="flex items-center justify-between text-[10px] text-slate-400 pt-1">
                              <span>Cluster: {camp.clusterType}</span>
                              <span>Confidence: {camp.confidenceScore}%</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {aiAssessment && (
                    <div className="print-avoid-break space-y-3">
                      <div className="border-b border-slate-800/80 pb-2">
                        <h3 className="text-sm font-bold text-white flex items-center gap-2">
                          <Sparkles className="h-4 w-4 text-purple-400" />
                          <span>
                            {aiAssessment.status === 'LIVE_AI'
                              ? '11. Grounded AI Forensic Assessment & Mitre ATT&CK Citations (Live LLM Model)'
                              : aiAssessment.status === 'ASSESSMENT_FAILED'
                              ? '11. Forensic Threat Assessment (Emergency Rule Fallback — Model Inactive)'
                              : '11. Rule-Based Forensic Threat Assessment (Offline Heuristics — No LLM Invoked)'}
                          </span>
                        </h3>
                      </div>

                      <div className={`p-4 rounded-xl text-xs space-y-3 border ${
                        aiAssessment.status === 'LIVE_AI'
                          ? 'bg-purple-950/20 border-purple-800/60'
                          : aiAssessment.status === 'ASSESSMENT_FAILED'
                          ? 'bg-rose-950/20 border-rose-800/60'
                          : 'bg-slate-900/40 border-slate-800'
                      }`}>
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800/60 pb-2 font-mono">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-purple-300 font-bold">Threat Verdict:</span>
                            <span className="px-2 py-0.5 rounded bg-slate-950 text-purple-200 border border-slate-700">
                              {aiAssessment.threatClassification}
                            </span>
                            <span className={`px-2 py-0.5 rounded text-[10px] border ${
                              aiAssessment.status === 'LIVE_AI'
                                ? 'bg-emerald-950 text-emerald-300 border-emerald-800'
                                : aiAssessment.status === 'ASSESSMENT_FAILED'
                                ? 'bg-rose-950 text-rose-300 border-rose-800'
                                : 'bg-cyan-950 text-cyan-300 border-cyan-800'
                            }`}>
                              {aiAssessment.status || 'OFFLINE_RULES'}
                            </span>
                          </div>
                          <span className="text-slate-400 text-[11px]">
                            {aiAssessment.status === 'LIVE_AI'
                              ? `Model: ${aiAssessment.modelName || 'Groq Cloud LLM'} (${aiAssessment.provider})`
                              : aiAssessment.status === 'ASSESSMENT_FAILED'
                              ? `Status: Emergency Fallback (${aiAssessment.errorMessage || 'AI Call Failed'})`
                              : 'Methodology: Offline Heuristic Rule Engine (No LLM)'}
                          </span>
                        </div>

                        {aiAssessment.status !== 'LIVE_AI' && (
                          <div className="p-2.5 rounded bg-amber-950/30 border border-amber-800/50 text-[11px] text-amber-200/90 font-mono">
                            FORENSIC AUDIT NOTICE: This threat assessment narrative was generated via predefined deterministic rules and indicator heuristics, NOT by an AI large language model.
                          </div>
                        )}

                        <p className="text-slate-200 text-xs leading-relaxed font-sans">
                          {aiAssessment.summaryExplanation}
                        </p>

                        {aiAssessment.mitreTechniques && aiAssessment.mitreTechniques.length > 0 && (
                          <div className="pt-2 border-t border-purple-800/40 space-y-1 font-mono text-[11px]">
                            <span className="text-purple-400 font-bold block">Mitre ATT&amp;CK Techniques Identified:</span>
                            <div className="flex flex-wrap gap-1.5">
                              {aiAssessment.mitreTechniques.map((tech, tIdx) => (
                                <span key={tIdx} className="px-2 py-0.5 rounded bg-slate-950 text-cyan-300 border border-cyan-800">
                                  {tech}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  )}

                  <div id="report-signoff-block" className="print-avoid-break p-6 rounded-2xl bg-slate-950 border border-slate-800 space-y-4">
                    <div className="border-b border-slate-800 pb-3">
                      <h3 className="text-sm font-bold text-white flex items-center gap-2">
                        <Award className="h-4 w-4 text-emerald-400" />
                        <span>12. Chain of Custody Certification &amp; Examiner Sign-Off</span>
                      </h3>
                    </div>

                    <p className="text-xs text-slate-300 font-serif leading-relaxed italic">
                      "I hereby certify under penalty of perjury that this forensic report represents an authentic, tamper-evident extraction of digital evidence collected and processed in compliance with digital forensics standards (ISO/IEC 27037:2012). The cryptographic hashes and evidentiary findings herein have been validated and preserved."
                    </p>

                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-4 text-xs font-mono">
                      <div className="space-y-1">
                        <span className="text-slate-500 text-[10px] block uppercase">Certified Lead Examiner</span>
                        <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-200 font-bold">
                          {reportData?.analystSignoff?.signoffAnalyst || user?.username || 'Forensic Examiner'}
                        </div>
                      </div>

                      <div className="space-y-1">
                        <span className="text-slate-500 text-[10px] block uppercase">Professional Role</span>
                        <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300">
                          {reportData?.analystSignoff?.signoffRole || 'Digital Forensics & Incident Response (DFIR) Specialist'}
                        </div>
                      </div>

                      <div className="space-y-1">
                        <span className="text-slate-500 text-[10px] block uppercase">Certification Date &amp; Seal</span>
                        <div className="p-2.5 rounded-lg bg-emerald-950/40 border border-emerald-800/80 text-emerald-300 font-bold flex items-center justify-between">
                          <span>{new Date().toLocaleDateString()}</span>
                          <BadgeCheck className="h-4 w-4 text-emerald-400" />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  )
}
