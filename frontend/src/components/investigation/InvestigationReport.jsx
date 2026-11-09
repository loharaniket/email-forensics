import React from 'react'

export default function InvestigationReport({
  detail = {},
  parsed = {},
  attachments = [],
  correlations = [],
  reportData,
  riskBreakdown,
  headerAnalysis,
  hops = [],
  urls = [],
  infrastructure = [],
  visuals = [],
  contentAnalysis,
  aiAssessment,
  flags = [],
  user,
  emailId,
  isExportingJson,
  handleExportReportJson,
  isVerifyingIntegrity,
  handleVerifyIntegrity,
  currentIntegrityStatus = 'NOT_VERIFIED',
  primaryHash,
  calculatedHash,
  integrityTimestamp,
  formatBytes,
  formatSeconds
}) {
  const safeFormatBytes = (bytes) => {
    if (typeof formatBytes === 'function') return formatBytes(bytes)
    if (!bytes || isNaN(bytes)) return '0 B'
    const units = ['B', 'KB', 'MB', 'GB']
    let size = Number(bytes)
    let unitIdx = 0
    while (size >= 1024 && unitIdx < units.length - 1) {
      size /= 1024
      unitIdx++
    }
    return `${size.toFixed(1)} ${units[unitIdx]}`
  }

  const safeFormatSeconds = (seconds) => {
    if (typeof formatSeconds === 'function') return formatSeconds(seconds)
    if (!seconds || seconds <= 0) return '0s'
    if (seconds < 60) return `${seconds}s`
    const mins = Math.floor(seconds / 60)
    const rem = seconds % 60
    return rem > 0 ? `${mins}m ${rem}s` : `${mins}m`
  }

  const formatUtcDate = (dateVal) => {
    if (!dateVal) return 'N/A'
    try {
      const d = new Date(dateVal)
      return !isNaN(d.getTime()) ? d.toUTCString() : String(dateVal)
    } catch {
      return String(dateVal)
    }
  }

  const formatLocalDate = (dateVal) => {
    if (!dateVal) return 'N/A'
    try {
      const d = new Date(dateVal)
      return !isNaN(d.getTime()) ? d.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : String(dateVal)
    } catch {
      return String(dateVal)
    }
  }

  const predictedLabel = contentAnalysis?.predictedLabel || 'INSUFFICIENT_DATA'
  const probability = contentAnalysis?.probability ?? 0
  const urgencyDetected = !!contentAnalysis?.urgency
  const credentialRequestDetected = !!contentAnalysis?.credentialRequest
  const paymentInstructionDetected = !!contentAnalysis?.paymentInstruction
  const impersonationDetected = !!contentAnalysis?.impersonation
  const secrecyDetected = !!contentAnalysis?.secrecy
  const promptMatches = contentAnalysis?.promptMatches || []
  const promptInjectionDetected = !!(
    contentAnalysis?.promptInjectionDetected || promptMatches.length > 0
  )

  const riskScore = riskBreakdown?.deterministicRiskScore ?? 0
  const riskLevel = riskBreakdown?.riskLevel || 'LOW'
  const confidenceScore = riskBreakdown?.evidenceConfidenceScore ?? 0
  const confidenceLevel = riskBreakdown?.confidenceLevel || 'LOW'
  const factors = riskBreakdown?.factors || []

  const reportId = reportData?.reportId || `DFIR-${(emailId || '00000000').substring(0, 8).toUpperCase()}`
  const caseNumber = detail.caseNumber || 'N/A'
  const caseTitle = detail.caseTitle || 'Email Evidence Investigation'
  const examinerName = reportData?.analystSignoff?.signoffAnalyst || user?.username || 'analyst'
  const examinerRole = reportData?.analystSignoff?.signoffRole || user?.role || 'Digital Forensics Analyst'
  const reportDate = formatLocalDate(reportData?.generatedAt || new Date())
  const sentDateDisplay = formatUtcDate(parsed?.sentDate || parsed?.messageDate || parsed?.rawDate || detail?.sentDate)
  const uploadDateDisplay = formatUtcDate(detail?.uploadedAt)
  const replyToDisplay = Array.isArray(parsed?.replyTo)
    ? (parsed.replyTo.length > 0 ? parsed.replyTo.join(', ') : 'None specified')
    : (parsed?.replyTo || detail?.replyTo || 'None specified')

  const verifiedHash = primaryHash || detail.sha256Hash || 'N/A'

  return (
    <div id="forensic-printable-dossier" className="space-y-6 font-sans text-[#202938]">
      {/* Action Toolbar (hidden during printing / PDF generation) */}
      <div className="no-print flex flex-col sm:flex-row items-center justify-between gap-3 p-4 rounded-xl bg-white border border-[#E4E7EC] text-xs">
        <div className="flex items-center gap-2">
          <span className="font-semibold text-[#202938]">Investigation Report</span>
          <span className="text-[#98A2B3]">•</span>
          <span className="text-[#667085]">
            NIST SP 800-86 aligned forensic examination report. Ready for print or PDF export.
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            id="btn-report-tab-print"
            onClick={() => window.print()}
            className="px-3.5 py-1.5 rounded-lg text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer"
          >
            Print / Save as PDF
          </button>

          <button
            id="btn-report-tab-export-json"
            onClick={handleExportReportJson}
            disabled={isExportingJson}
            className="px-3.5 py-1.5 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer disabled:opacity-50"
          >
            {isExportingJson ? 'Exporting...' : 'Export JSON Report'}
          </button>
        </div>
      </div>

      {/* Main Forensic Document Body */}
      <div className="print-surface bg-white border border-[#E4E7EC] p-8 sm:p-10 rounded-xl space-y-8">
        
        {/* Document Classification & Official Header */}
        <div className="border-b-2 border-[#202938] pb-5 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-[#E4E7EC] pb-2 text-[10px] tracking-wider uppercase font-mono text-[#667085]">
            <span>CONFIDENTIAL // DIGITAL FORENSIC EVIDENCE REPORT</span>
            <span>NIST SP 800-86 &amp; ISO/IEC 27037 METHODOLOGY</span>
          </div>

          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 pt-1">
            <div className="space-y-1">
              <span className="text-xs font-mono font-bold tracking-widest uppercase text-[#2563EB] block">
                Digital Forensics &amp; Incident Response Laboratory
              </span>
              <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-[#202938]">
                Forensic Examination Report
              </h1>
              <p className="text-xs text-[#667085]">
                Electronic Mail Evidence Attribution, Header Routing &amp; Threat Indicator Analysis
              </p>
            </div>

            <div className="sm:text-right space-y-1 text-xs">
              <div className="font-mono text-[#202938]">
                <span className="text-[#667085]">Report ID: </span>
                <span className="font-bold">{reportId}</span>
              </div>
              <div className="font-mono text-[#202938]">
                <span className="text-[#667085]">Date of Report: </span>
                <span>{reportDate}</span>
              </div>
              <div className="font-mono text-[#202938]">
                <span className="text-[#667085]">Evidence ID: </span>
                <span className="text-[11px]">{emailId || 'N/A'}</span>
              </div>
            </div>
          </div>

          {/* Case & Investigation Metadata Table */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-px bg-[#E4E7EC] border border-[#E4E7EC] text-xs pt-0 rounded overflow-hidden">
            <div className="bg-white p-2.5 space-y-0.5">
              <span className="text-[#667085] text-[10px] uppercase tracking-wider block font-medium">Case Number</span>
              <span className="font-bold font-mono text-[#202938] block">{caseNumber}</span>
            </div>
            <div className="bg-white p-2.5 space-y-0.5">
              <span className="text-[#667085] text-[10px] uppercase tracking-wider block font-medium">Case Title</span>
              <span className="font-semibold text-[#202938] block truncate">{caseTitle}</span>
            </div>
            <div className="bg-white p-2.5 space-y-0.5">
              <span className="text-[#667085] text-[10px] uppercase tracking-wider block font-medium">Lead Examiner</span>
              <span className="font-semibold text-[#202938] block">{examinerName} ({examinerRole})</span>
            </div>
            <div className="bg-white p-2.5 space-y-0.5">
              <span className="text-[#667085] text-[10px] uppercase tracking-wider block font-medium">Analysis Pipeline</span>
              <span className="font-semibold text-[#202938] block font-mono">{reportData?.pipelineVersion || 'v1.0.0'}</span>
            </div>
          </div>
        </div>

        {/* Section 1: Executive Summary & Objective Findings */}
        <section className="print-avoid-break space-y-3">
          <div className="border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              1. Executive Summary &amp; Threat Classification
            </h2>
          </div>

          <p className="text-xs text-[#202938] leading-relaxed">
            This digital forensic report provides findings from technical examination of an email message preserved in digital evidence custody. Analysis was conducted across cryptographic integrity, RFC 5322 header structure, sender authentication protocols (SPF, DKIM, DMARC), sequential transport routing hops, embedded hyperlinks, attached files, visual artifacts, and content behavioral signals.
          </p>

          <div className="border border-[#E4E7EC] rounded overflow-hidden">
            <table className="w-full text-left text-xs border-collapse">
              <tbody className="divide-y divide-[#E4E7EC]">
                <tr>
                  <td className="w-1/3 p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Deterministic Risk Score
                  </td>
                  <td className="p-2.5 text-[#202938] font-bold font-mono">
                    {riskScore} / 100 <span className="ml-2 font-sans font-semibold text-xs">({riskLevel} RISK)</span>
                  </td>
                </tr>
                <tr>
                  <td className="p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Evidence Confidence Assessment
                  </td>
                  <td className="p-2.5 text-[#202938] font-mono">
                    {confidenceScore}% confidence ({confidenceLevel} confidence band)
                  </td>
                </tr>
                <tr>
                  <td className="p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Classification Verdict
                  </td>
                  <td className="p-2.5 text-[#202938] font-semibold">
                    {predictedLabel === 'PHISHING' ? 'PHISHING EMAIL' : predictedLabel === 'BENIGN' ? 'LEGITIMATE / BENIGN' : predictedLabel}
                    <span className="ml-2 font-normal text-xs text-[#667085]">
                      ({(probability * 100).toFixed(1)}% statistical model probability)
                    </span>
                  </td>
                </tr>
                <tr>
                  <td className="p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Primary Threat Attributes
                  </td>
                  <td className="p-2.5 text-[#202938]">
                    {[
                      urgencyDetected && 'Urgent/coercive timeline',
                      credentialRequestDetected && 'Credential harvesting attempt',
                      paymentInstructionDetected && 'Fraudulent payment demand',
                      impersonationDetected && 'Identity impersonation',
                      secrecyDetected && 'Secrecy request',
                      promptInjectionDetected && 'Adversarial prompt injection payload'
                    ].filter(Boolean).join('; ') || 'No anomalous behavioral heuristics detected.'}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {/* Section 2: Evidence Identification & Cryptographic Custody */}
        <section className="print-avoid-break space-y-3">
          <div className="flex items-center justify-between border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              2. Evidence Identification &amp; Chain of Custody (NIST SP 800-86)
            </h2>
            <div className="no-print">
              <button
                id="btn-verify-custody"
                onClick={handleVerifyIntegrity}
                disabled={isVerifyingIntegrity}
                className="px-2.5 py-1 rounded bg-[#F8FAFC] hover:bg-slate-100 text-[11px] font-medium text-[#202938] border border-[#E4E7EC] cursor-pointer"
              >
                {isVerifyingIntegrity ? 'Verifying...' : 'Verify Cryptographic Hash'}
              </button>
            </div>
          </div>

          <div className="border border-[#E4E7EC] rounded overflow-hidden">
            <table className="w-full text-left text-xs border-collapse">
              <tbody className="divide-y divide-[#E4E7EC]">
                <tr>
                  <td className="w-1/3 p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Evidence Reference Number
                  </td>
                  <td className="p-2.5 font-mono text-[#202938]">
                    EV-01 (ID: {detail.evidenceId || emailId})
                  </td>
                </tr>
                <tr>
                  <td className="p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Original Ingested File Name
                  </td>
                  <td className="p-2.5 font-semibold text-[#202938]">
                    {detail.originalFilename || 'evidence.eml'}
                  </td>
                </tr>
                <tr>
                  <td className="p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Preserved File Size
                  </td>
                  <td className="p-2.5 text-[#202938]">
                    {safeFormatBytes(detail.fileSizeBytes)} ({detail.fileSizeBytes ? Number(detail.fileSizeBytes).toLocaleString() : 0} bytes)
                  </td>
                </tr>
                <tr>
                  <td className="p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Intake &amp; Preservation Timestamp
                  </td>
                  <td className="p-2.5 text-[#202938] font-mono">
                    {uploadDateDisplay} (Recorded by: {detail.uploadedByUsername || examinerName})
                  </td>
                </tr>
                <tr>
                  <td className="p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Primary SHA-256 Digest
                  </td>
                  <td className="p-2.5 font-mono text-[11px] text-[#202938] break-all select-all">
                    {verifiedHash}
                  </td>
                </tr>
                {calculatedHash && calculatedHash !== 'N/A' && calculatedHash !== verifiedHash && (
                  <tr>
                    <td className="p-2.5 bg-[#FEF2F2] font-medium text-[#B42318] border-r border-[#E4E7EC]">
                      Recalculated Hash Discrepancy
                    </td>
                    <td className="p-2.5 font-mono text-[11px] text-[#B42318] break-all">
                      {calculatedHash}
                    </td>
                  </tr>
                )}
                <tr>
                  <td className="p-2.5 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">
                    Chain of Custody Integrity Status
                  </td>
                  <td className="p-2.5 font-semibold text-[#202938]">
                    {currentIntegrityStatus === 'VERIFIED'
                      ? 'INTEGRITY VERIFIED — Cryptographic digest matches custody baseline.'
                      : `STATUS: ${currentIntegrityStatus}`}
                    {integrityTimestamp && (
                      <span className="block text-[11px] font-normal text-[#667085] mt-0.5">
                        Verified at: {formatUtcDate(integrityTimestamp)}
                      </span>
                    )}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {/* Section 3: Email Header & Sender Verification */}
        <section className="print-avoid-break space-y-3">
          <div className="border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              3. Email Header &amp; Sender Verification Analysis
            </h2>
          </div>

          <div className="border border-[#E4E7EC] rounded overflow-hidden">
            <table className="w-full text-left text-xs border-collapse">
              <tbody className="divide-y divide-[#E4E7EC]">
                <tr>
                  <td className="w-1/4 p-2 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">Subject Line</td>
                  <td className="p-2 font-semibold text-[#202938] col-span-3">{parsed?.subject || detail?.subject || '(No Subject)'}</td>
                </tr>
                <tr>
                  <td className="p-2 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">Header From</td>
                  <td className="p-2 font-mono text-[#202938] break-all">
                    {parsed?.from || detail?.parsedEmail?.from || detail?.sender || '(None specified)'}
                  </td>
                </tr>
                <tr>
                  <td className="p-2 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">Sent Date (UTC)</td>
                  <td className="p-2 font-mono text-[#202938]">{sentDateDisplay}</td>
                </tr>
                <tr>
                  <td className="p-2 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">Envelope Return-Path</td>
                  <td className="p-2 font-mono text-[#202938] break-all">
                    {parsed?.returnPath || detail?.returnPath || headerAnalysis?.returnPath || '(None specified)'}
                  </td>
                </tr>
                <tr>
                  <td className="p-2 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">Reply-To Address</td>
                  <td className="p-2 font-mono text-[#202938] break-all">{replyToDisplay}</td>
                </tr>
                <tr>
                  <td className="p-2 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">Message-ID</td>
                  <td className="p-2 font-mono text-[#202938] break-all">{parsed?.messageId || '(None specified)'}</td>
                </tr>
                <tr>
                  <td className="p-2 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">Sender Domain</td>
                  <td className="p-2 font-mono text-[#2563EB]">{headerAnalysis?.fromDomain || 'N/A'}</td>
                </tr>
                <tr>
                  <td className="p-2 bg-[#F8FAFC] font-medium text-[#667085] border-r border-[#E4E7EC]">Originating Transit IP</td>
                  <td className="p-2 font-mono text-[#202938]">{parsed?.originatingIp || '(Not detected)'}</td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* Authentication Protocols Evaluation */}
          <div className="pt-2">
            <span className="text-xs font-bold uppercase tracking-wider text-[#667085] block mb-2">
              Sender Authentication Protocol Validation
            </span>

            <div className="border border-[#E4E7EC] rounded overflow-hidden">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                  <tr>
                    <th className="p-2 font-semibold">Protocol</th>
                    <th className="p-2 font-semibold">Reported Result</th>
                    <th className="p-2 font-semibold">Active Verification</th>
                    <th className="p-2 font-semibold">Technical Detail</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  <tr>
                    <td className="p-2 font-bold text-[#202938]">SPF (Sender Policy Framework)</td>
                    <td className="p-2 font-mono font-semibold">
                      {headerAnalysis?.reportedSpf || headerAnalysis?.spfResult || 'NONE'}
                    </td>
                    <td className="p-2 font-mono">
                      {headerAnalysis?.activeSpfResult || 'Not performed'}
                    </td>
                    <td className="p-2 text-[#667085] text-[11px]">
                      Relay: {headerAnalysis?.activeSpfRelayIp || 'N/A'}
                      {headerAnalysis?.activeSpfRecord ? ` | Record: ${headerAnalysis.activeSpfRecord}` : ''}
                    </td>
                  </tr>
                  <tr>
                    <td className="p-2 font-bold text-[#202938]">DKIM (DomainKeys Identified Mail)</td>
                    <td className="p-2 font-mono font-semibold">
                      {headerAnalysis?.reportedDkim || headerAnalysis?.dkimResult || 'NONE'}
                    </td>
                    <td className="p-2 font-mono">
                      {headerAnalysis?.activeDkimStatus || 'Not performed'}
                    </td>
                    <td className="p-2 text-[#667085] text-[11px]">
                      Selector: {headerAnalysis?.activeDkimSelector || 'N/A'} | Domain: {headerAnalysis?.dkimDomain || 'N/A'}
                    </td>
                  </tr>
                  <tr>
                    <td className="p-2 font-bold text-[#202938]">DMARC (Domain-based Message Auth)</td>
                    <td className="p-2 font-mono font-semibold">
                      {headerAnalysis?.reportedDmarc || headerAnalysis?.dmarcResult || 'NONE'}
                    </td>
                    <td className="p-2 font-mono">
                      {headerAnalysis?.activeDmarcStatus || 'Not performed'}
                    </td>
                    <td className="p-2 text-[#667085] text-[11px]">
                      Policy: {headerAnalysis?.activeDmarcPolicy || 'N/A'}
                    </td>
                  </tr>
                  <tr>
                    <td className="p-2 font-bold text-[#202938]">ARC (Authenticated Received Chain)</td>
                    <td className="p-2 font-mono font-semibold">
                      {headerAnalysis?.reportedArc || 'NONE'}
                    </td>
                    <td className="p-2 font-mono">N/A</td>
                    <td className="p-2 text-[#667085] text-[11px]">
                      {headerAnalysis?.reportedArc ? 'ARC validation sealed' : 'No ARC headers present'}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        {/* Section 4: Mail Route Transport Path */}
        <section className="print-avoid-break space-y-3">
          <div className="border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              4. Mail Transport Route &amp; Server Transmission Hops
            </h2>
          </div>

          <p className="text-xs text-[#202938]">
            Sequential transmission path extracted from Received headers, listed in chronological order from origin MTA to recipient.
          </p>

          {hops && hops.length > 0 ? (
            <div className="border border-[#E4E7EC] rounded overflow-hidden">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                  <tr>
                    <th className="p-2 font-semibold">Hop</th>
                    <th className="p-2 font-semibold">Sending Server (from)</th>
                    <th className="p-2 font-semibold">Receiving Server (by)</th>
                    <th className="p-2 font-semibold">Transit IP</th>
                    <th className="p-2 font-semibold">Delay</th>
                    <th className="p-2 font-semibold">Timestamp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {hops.map((hop, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2 font-mono font-bold text-[#2563EB]">{hop.hopOrder || idx + 1}</td>
                      <td className="p-2 font-mono text-[11px] text-[#202938] max-w-[150px] truncate" title={hop.fromHost}>
                        {hop.fromHost || 'Unknown'}
                      </td>
                      <td className="p-2 font-mono text-[11px] text-[#202938] max-w-[150px] truncate" title={hop.byHost}>
                        {hop.byHost || 'Unknown'}
                      </td>
                      <td className="p-2 font-mono text-[11px] text-[#202938]">
                        {hop.hopIp || hop.byIp || 'N/A'}
                      </td>
                      <td className="p-2 font-mono text-[11px] text-[#202938]">
                        {safeFormatSeconds(hop.delaySeconds)}
                      </td>
                      <td className="p-2 font-mono text-[11px] text-[#667085]">
                        {formatUtcDate(hop.hopTimestamp || hop.timestamp)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-4 border border-[#E4E7EC] rounded text-xs text-[#667085] bg-[#F8FAFC]">
              No Received header transmission hops recorded for this message.
            </div>
          )}
        </section>

        {/* Section 5: Extracted Hyperlinks & URL Intelligence */}
        <section className="print-avoid-break space-y-3">
          <div className="border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              5. Extracted Hyperlinks &amp; URL Indicator Intelligence ({urls.length} Links)
            </h2>
          </div>

          {urls && urls.length > 0 ? (
            <div className="border border-[#E4E7EC] rounded overflow-hidden">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                  <tr>
                    <th className="p-2 font-semibold">#</th>
                    <th className="p-2 font-semibold">Destination Domain</th>
                    <th className="p-2 font-semibold">Target URL Address</th>
                    <th className="p-2 font-semibold">Anchor Text</th>
                    <th className="p-2 font-semibold">Discrepancy</th>
                    <th className="p-2 font-semibold">Risk Score</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {urls.map((u, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2 font-mono text-[11px] text-[#667085]">{idx + 1}</td>
                      <td className="p-2 font-mono font-bold text-[#2563EB]">{u.domain || 'N/A'}</td>
                      <td className="p-2 font-mono text-[11px] text-[#202938] max-w-[260px] truncate" title={u.rawUrl || u.normalizedUrl}>
                        {u.normalizedUrl || u.rawUrl}
                      </td>
                      <td className="p-2 text-[11px] text-[#667085] max-w-[120px] truncate" title={u.anchorText}>
                        {u.anchorText || '(Direct URL)'}
                      </td>
                      <td className="p-2 text-[11px]">
                        {u.anchorMismatch ? (
                          <span className="font-semibold text-[#B42318]">ANCHOR MISMATCH</span>
                        ) : u.isIpUrl ? (
                          <span className="font-semibold text-[#B45309]">IP LITERAL</span>
                        ) : (
                          <span className="text-[#667085]">None</span>
                        )}
                      </td>
                      <td className="p-2 font-mono font-semibold text-[#202938]">
                        {u.urlRiskScore ?? 0} / 100
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-4 border border-[#E4E7EC] rounded text-xs text-[#667085] bg-[#F8FAFC]">
              No external hyperlinks or URL indicators extracted from message body.
            </div>
          )}
        </section>

        {/* Section 6: Network Infrastructure & Extracted IP Addresses */}
        <section className="print-avoid-break space-y-3">
          <div className="border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              6. Network Infrastructure &amp; Extracted IP Addresses ({infrastructure.length} IPs)
            </h2>
          </div>

          {infrastructure && infrastructure.length > 0 ? (
            <div className="border border-[#E4E7EC] rounded overflow-hidden">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                  <tr>
                    <th className="p-2 font-semibold">#</th>
                    <th className="p-2 font-semibold">IP Address</th>
                    <th className="p-2 font-semibold">Header Context</th>
                    <th className="p-2 font-semibold">Routing Classification</th>
                    <th className="p-2 font-semibold">Lookup Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {infrastructure.map((item, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2 font-mono text-[11px] text-[#667085]">{idx + 1}</td>
                      <td className="p-2 font-mono font-bold text-[#202938]">{item.ipAddress}</td>
                      <td className="p-2 text-[#667085]">{item.sourceContext || 'Email Header'}</td>
                      <td className="p-2 font-mono text-[11px]">
                        {item.isPrivate ? 'Private / RFC 1918 Scope' : 'Public Internet Address'}
                      </td>
                      <td className="p-2 text-[#667085] font-mono text-[11px]">{item.lookupStatus || 'PROCESSED'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-4 border border-[#E4E7EC] rounded text-xs text-[#667085] bg-[#F8FAFC]">
              No IP addresses cataloged from network headers.
            </div>
          )}
        </section>

        {/* Section 7: Attachment & Visual Artifact Inventory */}
        <section className="print-avoid-break space-y-3">
          <div className="border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              7. Attachment &amp; Visual Artifact Inventory
            </h2>
          </div>

          {/* Attachments Table */}
          {attachments && attachments.length > 0 ? (
            <div className="border border-[#E4E7EC] rounded overflow-hidden mb-3">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                  <tr>
                    <th className="p-2 font-semibold">File Name</th>
                    <th className="p-2 font-semibold">Content Type</th>
                    <th className="p-2 font-semibold">Size</th>
                    <th className="p-2 font-semibold">Extension Risk</th>
                    <th className="p-2 font-semibold">SHA-256 Digest</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {attachments.map((att, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2 font-semibold text-[#202938]">{att.filename}</td>
                      <td className="p-2 font-mono text-[11px] text-[#667085]">{att.mimeType || 'application/octet-stream'}</td>
                      <td className="p-2 font-mono text-[11px] text-[#202938]">{safeFormatBytes(att.sizeBytes)}</td>
                      <td className="p-2 font-semibold text-xs">
                        {att.riskyExtension ? (
                          <span className="text-[#B42318]">HIGH RISK EXTENSION</span>
                        ) : (
                          <span className="text-[#667085]">Standard</span>
                        )}
                      </td>
                      <td className="p-2 font-mono text-[11px] text-[#202938] break-all select-all">
                        {att.sha256Hash || 'N/A'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-3 border border-[#E4E7EC] rounded text-xs text-[#667085] bg-[#F8FAFC]">
              No file attachments accompanied this email message.
            </div>
          )}

          {/* Visual Artifacts & QR Codes */}
          {visuals && visuals.length > 0 && (
            <div className="pt-2 space-y-2">
              <span className="text-xs font-bold uppercase tracking-wider text-[#667085] block">
                Visual Artifacts &amp; Decoded QR Payloads ({visuals.length} Visuals)
              </span>

              <div className="border border-[#E4E7EC] rounded overflow-hidden">
                <table className="w-full text-left text-xs border-collapse">
                  <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                    <tr>
                      <th className="p-2 font-semibold">Image File</th>
                      <th className="p-2 font-semibold">MIME Type</th>
                      <th className="p-2 font-semibold">Size</th>
                      <th className="p-2 font-semibold">Decoded QR Code Payload</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#E4E7EC]">
                    {visuals.map((v, idx) => (
                      <tr key={idx} className="hover:bg-slate-50">
                        <td className="p-2 font-semibold text-[#202938]">{v.filename || `Visual #${idx + 1}`}</td>
                        <td className="p-2 font-mono text-[11px] text-[#667085]">{v.contentType || 'image/*'}</td>
                        <td className="p-2 font-mono text-[11px]">{safeFormatBytes(v.fileSizeBytes || v.byteSize || 0)}</td>
                        <td className="p-2 font-mono text-[11px] text-[#202938] break-all">
                          {v.qrArtifacts && v.qrArtifacts.length > 0 ? (
                            v.qrArtifacts.map((qr, qIdx) => (
                              <div key={qIdx} className="text-[#2563EB] font-bold">
                                {qr.decodedPayload}
                              </div>
                            ))
                          ) : (
                            <span className="text-[#667085]">No barcode / QR detected</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </section>

        {/* Section 8: Message Content & Behavioral Signals */}
        <section className="print-avoid-break space-y-3">
          <div className="border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              8. Message Content Analysis &amp; Behavioral Indicators
            </h2>
          </div>

          <div className="border border-[#E4E7EC] rounded overflow-hidden">
            <table className="w-full text-left text-xs border-collapse">
              <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                <tr>
                  <th className="p-2 font-semibold w-1/3">Behavioral Heuristic Category</th>
                  <th className="p-2 font-semibold w-1/4">Evaluation Result</th>
                  <th className="p-2 font-semibold">Forensic Finding Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E4E7EC]">
                <tr>
                  <td className="p-2 font-semibold text-[#202938]">Urgency &amp; Coercive Timelines</td>
                  <td className="p-2 font-bold font-mono">
                    {urgencyDetected ? 'INDICATOR PRESENT' : 'NEGATIVE'}
                  </td>
                  <td className="p-2 text-[#667085]">
                    {urgencyDetected ? 'Language engineered to induce immediate action or bypass standard verification controls.' : 'No excessive urgency detected.'}
                  </td>
                </tr>
                <tr>
                  <td className="p-2 font-semibold text-[#202938]">Credential Harvesting Indicators</td>
                  <td className="p-2 font-bold font-mono">
                    {credentialRequestDetected ? 'INDICATOR PRESENT' : 'NEGATIVE'}
                  </td>
                  <td className="p-2 text-[#667085]">
                    {credentialRequestDetected ? 'Explicit solicitation of login credentials, account secrets, or MFA verification tokens.' : 'No credential requests detected.'}
                  </td>
                </tr>
                <tr>
                  <td className="p-2 font-semibold text-[#202938]">Financial &amp; Wire Transfer Demands</td>
                  <td className="p-2 font-bold font-mono">
                    {paymentInstructionDetected ? 'INDICATOR PRESENT' : 'NEGATIVE'}
                  </td>
                  <td className="p-2 text-[#667085]">
                    {paymentInstructionDetected ? 'Payment instructions, banking account changes, or invoice redirect demands identified.' : 'No financial transaction requests detected.'}
                  </td>
                </tr>
                <tr>
                  <td className="p-2 font-semibold text-[#202938]">Identity &amp; Executive Impersonation</td>
                  <td className="p-2 font-bold font-mono">
                    {impersonationDetected ? 'INDICATOR PRESENT' : 'NEGATIVE'}
                  </td>
                  <td className="p-2 text-[#667085]">
                    {impersonationDetected ? 'Sender header identity discrepancies matching VIP or executive impersonation patterns.' : 'No executive impersonation patterns flagged.'}
                  </td>
                </tr>
                <tr>
                  <td className="p-2 font-semibold text-[#202938]">Adversarial Prompt Injection</td>
                  <td className="p-2 font-bold font-mono">
                    {promptInjectionDetected ? 'INJECTION DETECTED' : 'CLEAN'}
                  </td>
                  <td className="p-2 text-[#667085]">
                    {promptInjectionDetected ? 'Payload matches heuristic signatures designed to subvert automated LLM/AI parsing pipelines.' : 'No prompt injection signatures discovered.'}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {/* Section 9: Triggered Deterministic Risk Factors */}
        <section className="print-avoid-break space-y-3">
          <div className="border-b border-[#202938] pb-1">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              9. Triggered Deterministic Risk Factors ({factors.length} Factors)
            </h2>
          </div>

          {factors && factors.length > 0 ? (
            <div className="border border-[#E4E7EC] rounded overflow-hidden">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                  <tr>
                    <th className="p-2 font-semibold">Severity</th>
                    <th className="p-2 font-semibold">Category</th>
                    <th className="p-2 font-semibold">Code</th>
                    <th className="p-2 font-semibold">Score Impact</th>
                    <th className="p-2 font-semibold">Explanation &amp; Evidence Grounding</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {factors.map((f, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2 font-bold font-mono text-[11px]">
                        {f.severity || 'LOW'}
                      </td>
                      <td className="p-2 text-[#667085] text-[11px]">{f.category || 'GENERAL'}</td>
                      <td className="p-2 font-mono text-[11px] text-[#202938]">{f.factorCode}</td>
                      <td className="p-2 font-mono font-bold text-[#B42318]">
                        +{f.scoreImpact != null ? f.scoreImpact : f.weightPoints != null ? f.weightPoints : 0} pts
                      </td>
                      <td className="p-2 text-[#202938]">
                        <div>{f.explanation}</div>
                        {f.evidenceReference && (
                          <div className="text-[11px] font-mono text-[#667085] mt-0.5">
                            Reference: {f.evidenceReference}
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-4 border border-[#E4E7EC] rounded text-xs text-[#667085] bg-[#F8FAFC]">
              No deterministic threat factors were triggered during rule evaluation.
            </div>
          )}
        </section>

        {/* Section 10: Correlated Incident History */}
        {correlations && correlations.length > 0 && (
          <section className="print-avoid-break space-y-3">
            <div className="border-b border-[#202938] pb-1">
              <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
                10. Correlated Incident History &amp; Related Investigations ({correlations.length} Matches)
              </h2>
            </div>

            <div className="border border-[#E4E7EC] rounded overflow-hidden">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[#F8FAFC] border-b border-[#E4E7EC] text-[11px] text-[#667085] uppercase">
                  <tr>
                    <th className="p-2 font-semibold">Related Case #</th>
                    <th className="p-2 font-semibold">Matched Artifact</th>
                    <th className="p-2 font-semibold">Indicator Type</th>
                    <th className="p-2 font-semibold">Indicator Value</th>
                    <th className="p-2 font-semibold">Match Strength</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {correlations.map((c, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2 font-bold font-mono text-[#2563EB]">{c.matchedCaseNumber || 'N/A'}</td>
                      <td className="p-2 text-[#202938]">{c.matchedEmailSubject || 'Untitled'}</td>
                      <td className="p-2 font-mono text-[11px] text-[#667085]">{c.indicatorType}</td>
                      <td className="p-2 font-mono text-[11px] text-[#202938] max-w-[200px] truncate" title={c.normalizedIndicatorValue}>
                        {c.normalizedIndicatorValue}
                      </td>
                      <td className="p-2 font-bold font-mono text-[11px]">{c.matchStrength}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}

        {/* Section 11: Automated Assessment Narrative */}
        {aiAssessment && (
          <section className="print-avoid-break space-y-3">
            <div className="border-b border-[#202938] pb-1">
              <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
                11. Automated Forensic Synthesis Narrative
              </h2>
            </div>

            <div className="p-4 rounded border border-[#E4E7EC] bg-[#F8FAFC] space-y-3 text-xs leading-relaxed text-[#202938]">
              <div>
                <span className="font-bold block uppercase text-[10px] text-[#667085] tracking-wider mb-1">
                  Executive Assessment Narrative
                </span>
                <p>
                  {aiAssessment.assessmentSummary || aiAssessment.summaryExplanation || 'Assessment synthesis complete.'}
                </p>
              </div>

              {aiAssessment.limitationsAndCaveats && (
                <div className="pt-2 border-t border-[#E4E7EC]">
                  <span className="font-bold block uppercase text-[10px] text-[#667085] tracking-wider mb-1">
                    Limitations &amp; Analytical Caveats
                  </span>
                  <p className="text-[#667085]">
                    {aiAssessment.limitationsAndCaveats}
                  </p>
                </div>
              )}
            </div>
          </section>
        )}

        {/* Section 12: Examiner Certification, Chain of Custody & Sign-off */}
        <section id="report-signoff-block" className="print-avoid-break border border-[#202938] p-6 rounded space-y-5 bg-[#F8FAFC]">
          <div className="border-b border-[#202938] pb-1.5">
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#202938]">
              12. Examiner Certification &amp; Custody Declaration
            </h2>
          </div>

          <p className="text-xs text-[#202938] leading-relaxed">
            I hereby certify under digital forensics professional standards (NIST SP 800-86 and ISO/IEC 27037) that the evidence cataloged in this report was ingested, hashed, verified, and analyzed in accordance with forensic best practices. Cryptographic integrity checks were performed to ensure no post-acquisition alteration or data degradation occurred. The analytical evaluations and indicator extractions recorded in this dossier reflect the objective state of the evidence.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t border-[#E4E7EC] text-xs">
            <div className="space-y-1">
              <span className="text-[10px] uppercase font-mono tracking-wider text-[#667085] block">Examining Analyst</span>
              <span className="font-bold text-[#202938] block">{examinerName}</span>
              <span className="text-[#667085] text-[11px] block">{examinerRole}</span>
            </div>

            <div className="space-y-1">
              <span className="text-[10px] uppercase font-mono tracking-wider text-[#667085] block">Attribution Facility</span>
              <span className="font-semibold text-[#202938] block">Email Forensics Laboratory</span>
              <span className="text-[#667085] text-[11px] block">DFIR Operations Unit</span>
            </div>

            <div className="space-y-1">
              <span className="text-[10px] uppercase font-mono tracking-wider text-[#667085] block">Date Certified</span>
              <span className="font-mono font-semibold text-[#202938] block">{reportDate}</span>
              <span className="text-[#667085] text-[11px] block">Standard Time</span>
            </div>

            <div className="space-y-1">
              <span className="text-[10px] uppercase font-mono tracking-wider text-[#667085] block">Signature</span>
              <div className="h-6 border-b border-[#202938] mt-1"></div>
              <span className="text-[10px] font-mono text-[#667085] block">Authorized Examiner Sign-off</span>
            </div>
          </div>
        </section>

      </div>
    </div>
  )
}
