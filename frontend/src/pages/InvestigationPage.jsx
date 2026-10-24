import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import {
  getInvestigationDetail,
  getEvidenceHeaders,
  getEvidenceHops,
  downloadOriginalEvidence
} from '../api/evidence'
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
  Activity
} from 'lucide-react'

export default function InvestigationPage() {
  const { caseId, emailId } = useParams()
  const navigate = useNavigate()

  const [detail, setDetail] = useState(null)
  const [headerAnalysis, setHeaderAnalysis] = useState(null)
  const [hops, setHops] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [activeTab, setActiveTab] = useState('headers')
  const [bodyMode, setBodyMode] = useState('extracted')
  const [copiedKey, setCopiedKey] = useState(null)
  const [expandedHops, setExpandedHops] = useState({})
  const [rawHeaderSearch, setRawHeaderSearch] = useState('')

  const loadData = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const [detailData, headersData, hopsData] = await Promise.all([
        getInvestigationDetail(emailId),
        getEvidenceHeaders(emailId).catch(() => null),
        getEvidenceHops(emailId).catch(() => [])
      ])
      setDetail(detailData)
      setHeaderAnalysis(headersData)
      setHops(hopsData || [])
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load email investigation details.')
    } finally {
      setIsLoading(false)
    }
  }, [emailId])

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
    const norm = val.toLowerCase()
    if (norm === 'pass') {
      return {
        bg: 'bg-emerald-950/80',
        text: 'text-emerald-300',
        border: 'border-emerald-700/80',
        label: 'PASS',
        icon: CheckCircle2
      }
    }
    if (norm === 'fail' || norm === 'hardfail') {
      return {
        bg: 'bg-rose-950/80',
        text: 'text-rose-300',
        border: 'border-rose-700/80',
        label: 'FAIL',
        icon: XCircle
      }
    }
    if (norm === 'softfail' || norm === 'neutral') {
      return {
        bg: 'bg-amber-950/80',
        text: 'text-amber-300',
        border: 'border-amber-700/80',
        label: norm.toUpperCase(),
        icon: AlertTriangle
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

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <button
            id="back-to-case-btn"
            onClick={() => navigate(`/cases/${caseId}`)}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors cursor-pointer w-fit"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            <span>Back to Case ({detail.caseNumber})</span>
          </button>

          <div className="flex items-center gap-2">
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

        <section className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 md:p-8 space-y-6 shadow-xl">
          <div className="flex flex-col md:flex-row md:items-start justify-between gap-4 pb-6 border-b border-slate-800">
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

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
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
          </div>

          {warnings.length > 0 && (
            <div className="p-3.5 rounded-xl bg-amber-950/30 border border-amber-800/60 text-xs text-amber-300 space-y-1">
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
            <div className="flex flex-wrap items-center gap-2 border-b border-slate-800 pb-2">
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
            </div>

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

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-400 font-mono flex items-center gap-2">
                      <ShieldAlert className="h-3.5 w-3.5 text-cyan-400" />
                      Reported Authentication Results (Authentication-Results Header)
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
          </div>
        </section>
      </main>
    </div>
  )
}
