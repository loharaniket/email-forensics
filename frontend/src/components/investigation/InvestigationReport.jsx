import React from 'react'
import {
  Printer,
  FileDown,
  FileCheck,
  ShieldAlert,
  ShieldCheck,
  Mail,
  Server,
  Link2,
  Network,
  QrCode,
  Brain,
  AlertOctagon,
  Sparkles,
  Check,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Hash,
  User
} from 'lucide-react'

export default function InvestigationReport({
  detail = {},
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
  currentIntegrityStatus,
  primaryHash,
  calculatedHash,
  integrityTimestamp,
  formatBytes,
  formatSeconds
}) {
  const predictedLabel =
    contentAnalysis?.predictedLabel ||
    contentAnalysis?.mlPredictedLabel ||
    'INSUFFICIENT_DATA'
  const probability =
    contentAnalysis?.probability != null
      ? contentAnalysis.probability
      : contentAnalysis?.mlProbability != null
      ? contentAnalysis.mlProbability
      : 0
  const urgencyDetected = !!(
    contentAnalysis?.urgency || contentAnalysis?.urgencyDetected
  )
  const credentialRequestDetected = !!(
    contentAnalysis?.credentialRequest ||
    contentAnalysis?.credentialRequestDetected
  )
  const paymentInstructionDetected = !!(
    contentAnalysis?.paymentInstruction ||
    contentAnalysis?.paymentInstructionDetected
  )
  const impersonationDetected = !!(
    contentAnalysis?.impersonation || contentAnalysis?.impersonationDetected
  )
  const promptMatches =
    contentAnalysis?.promptMatches ||
    contentAnalysis?.promptInjectionMatches ||
    []
  const promptInjectionDetected = !!(
    contentAnalysis?.promptInjectionDetected || promptMatches.length > 0
  )

  return (
    <div id="forensic-printable-dossier" className="space-y-6 font-sans">
      {/* Action bar (hidden in print) */}
      <div className="no-print flex flex-col sm:flex-row items-center justify-between gap-3 p-4 rounded-xl bg-white border border-[#E4E7EC] text-xs shadow-xs">
        <div className="flex items-center gap-2 text-[#202938]">
          <FileCheck className="h-4 w-4 text-[#2563EB]" />
          <span className="font-semibold text-[#202938]">Investigation report</span>
          <span className="text-[#98A2B3]">•</span>
          <span className="text-[#667085]">
            Formatted for printing, PDF export and review.
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            id="btn-report-tab-print"
            onClick={() => window.print()}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer"
          >
            <Printer className="h-3.5 w-3.5" />
            <span>Print / PDF report</span>
          </button>

          <button
            id="btn-report-tab-export-json"
            onClick={handleExportReportJson}
            disabled={isExportingJson}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer disabled:opacity-50"
          >
            <FileDown className="h-3.5 w-3.5 text-[#667085]" />
            <span>{isExportingJson ? 'Exporting...' : 'Export JSON report'}</span>
          </button>
        </div>
      </div>

      {/* Main Printable Document Surface */}
      <div className="print-surface p-6 sm:p-8 rounded-xl bg-white border border-[#E4E7EC] space-y-6 shadow-xs text-[#202938]">
        {/* Document Header */}
        <div className="border-b border-[#E4E7EC] pb-6 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="space-y-1">
              <span className="text-xs font-mono font-semibold text-[#2563EB] uppercase">
                Case #{detail.caseNumber || 'N/A'}
              </span>
              <h1 className="text-2xl font-bold tracking-tight text-[#202938]">
                Investigation report
              </h1>
              <p className="text-xs font-mono text-[#667085]">
                Evidence ID: {detail.evidenceId || emailId}
              </p>
            </div>

            <div className="text-right space-y-1 text-xs">
              <span className="inline-block px-3 py-1 rounded-md text-xs font-semibold bg-[#F5F6F8] text-[#202938] border border-[#E4E7EC]">
                {reportData?.classificationLevel || 'Forensic investigation'}
              </span>
              <span className="text-[#667085] block text-[11px] font-mono">
                Report ID: {reportData?.reportId || 'RPT-' + (emailId ? emailId.substring(0, 8) : '001')}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs pt-2">
            <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
              <span className="text-[#667085] text-[11px] block">Case title</span>
              <span className="font-semibold text-[#202938] truncate block">
                {detail.caseTitle || 'Investigation'}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
              <span className="text-[#667085] text-[11px] block">Date generated</span>
              <span className="font-semibold text-[#202938] block text-[11px]">
                {reportData?.generatedAt ? new Date(reportData.generatedAt).toLocaleString() : new Date().toLocaleString()}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
              <span className="text-[#667085] text-[11px] block">Investigator</span>
              <span className="font-semibold text-[#202938] block truncate">
                {reportData?.analystSignoff?.signoffAnalyst || user?.username || 'analyst'}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
              <span className="text-[#667085] text-[11px] block">Application version</span>
              <span className="font-semibold text-[#202938] block">
                {reportData?.reportVersion || 'v1.0.0'}
              </span>
            </div>
          </div>
        </div>

        {/* Section 1: Evidence information & file integrity */}
        <div className="print-avoid-break space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#E4E7EC] pb-2">
            <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
              <FileCheck className="h-4 w-4 text-[#2563EB]" />
              <span>1. Evidence information and file integrity</span>
            </h3>

            <div className="no-print flex items-center gap-2">
              <button
                id="btn-verify-custody"
                onClick={handleVerifyIntegrity}
                disabled={isVerifyingIntegrity}
                className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-[#F5F6F8] hover:bg-slate-100 text-xs font-medium text-[#202938] border border-[#E4E7EC] cursor-pointer"
              >
                <span>{isVerifyingIntegrity ? 'Verifying...' : 'Verify integrity'}</span>
              </button>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] space-y-2 text-xs">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <div>
                <span className="text-[#667085]">Original filename: </span>
                <span className="font-semibold text-[#202938]">{detail.originalFilename}</span>
              </div>
              <div>
                <span className="text-[#667085]">File size: </span>
                <span className="text-[#202938]">{formatBytes(detail.fileSizeBytes)}</span>
              </div>
              <div className="sm:col-span-2 break-all font-mono text-[11px]">
                <span className="text-[#667085]">SHA-256 hash: </span>
                <span className="text-[#202938] select-all">{primaryHash}</span>
              </div>
              {calculatedHash && calculatedHash !== 'N/A' && calculatedHash !== primaryHash && (
                <div className="sm:col-span-2 break-all font-mono text-[11px]">
                  <span className="text-[#B42318]">Recalculated hash: </span>
                  <span className="text-[#B42318] select-all">{calculatedHash}</span>
                </div>
              )}
            </div>

            <div className="pt-2 border-t border-[#E4E7EC] flex items-center justify-between text-[11px]">
              <span className="font-semibold text-[#15803D]">
                {currentIntegrityStatus === 'VERIFIED'
                  ? 'File integrity verified: Stored file matches registered SHA-256 digest.'
                  : `Integrity status: ${currentIntegrityStatus}`}
              </span>
              <span className="text-[#667085]">
                {integrityTimestamp ? new Date(integrityTimestamp).toLocaleString() : ''}
              </span>
            </div>
          </div>
        </div>

        {/* Section 2: Executive threat synthesis & deterministic scoring */}
        <div className="print-avoid-break space-y-3">
          <div className="border-b border-[#E4E7EC] pb-2">
            <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
              <ShieldAlert className="h-4 w-4 text-[#2563EB]" />
              <span>2. Investigation summary and risk score</span>
            </h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
            <div className="p-4 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC]">
              <span className="text-[#667085] text-[11px] block">Risk score</span>
              <span className="text-2xl font-bold font-mono text-[#202938]">
                {riskBreakdown?.deterministicRiskScore ?? 0} / 100
              </span>
              <span className="text-xs text-[#667085] block mt-0.5">
                Level: {riskBreakdown?.riskLevel || 'LOW'}
              </span>
            </div>

            <div className="p-4 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC]">
              <span className="text-[#667085] text-[11px] block">Evidence confidence</span>
              <span className="text-2xl font-bold font-mono text-[#202938]">
                {riskBreakdown?.evidenceConfidenceScore ?? riskBreakdown?.confidenceScore ?? 0}%
              </span>
              <span className="text-xs text-[#667085] block mt-0.5">
                Level: {riskBreakdown?.confidenceLevel || 'LOW'}
              </span>
            </div>

            <div className="p-4 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC]">
              <span className="text-[#667085] text-[11px] block">Classification</span>
              <span className="text-lg font-bold text-[#202938] block truncate">
                {predictedLabel === 'PHISHING' ? 'Phishing' : predictedLabel === 'BENIGN' ? 'Legitimate' : predictedLabel}
              </span>
              <span className="text-xs text-[#667085]">
                {(probability * 100).toFixed(1)}% model probability
              </span>
            </div>
          </div>
        </div>

        {/* Section 3: Sender authentication */}
        <div className="print-avoid-break space-y-3">
          <div className="border-b border-[#E4E7EC] pb-2">
            <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
              <Mail className="h-4 w-4 text-[#2563EB]" />
              <span>3. Sender checks and email authentication</span>
            </h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
            <div className="p-4 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] space-y-1.5">
              <div className="grid grid-cols-3 gap-1">
                <span className="text-[#667085]">From:</span>
                <span className="col-span-2 font-mono text-[#202938] font-semibold truncate">{detail.sender || '(None)'}</span>
              </div>
              <div className="grid grid-cols-3 gap-1">
                <span className="text-[#667085]">Domain:</span>
                <span className="col-span-2 font-mono text-[#2563EB] truncate">{headerAnalysis?.fromDomain || 'N/A'}</span>
              </div>
              <div className="grid grid-cols-3 gap-1">
                <span className="text-[#667085]">Reply-To:</span>
                <span className="col-span-2 font-mono text-[#202938] truncate">{detail.replyTo || 'None specified'}</span>
              </div>
              <div className="grid grid-cols-3 gap-1">
                <span className="text-[#667085]">Return-Path:</span>
                <span className="col-span-2 font-mono text-[#202938] truncate">{detail.returnPath || 'None specified'}</span>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] space-y-2">
              <span className="text-xs font-semibold text-[#202938] block">Authentication results</span>
              <div className="grid grid-cols-3 gap-2 text-center text-xs">
                <div className="p-2 rounded bg-white border border-[#E4E7EC]">
                  <span className="text-[10px] text-[#667085] block">SPF</span>
                  <span className="font-semibold text-[#202938]">
                    {headerAnalysis?.reportedSpf || headerAnalysis?.spfResult || 'NONE'}
                  </span>
                </div>
                <div className="p-2 rounded bg-white border border-[#E4E7EC]">
                  <span className="text-[10px] text-[#667085] block">DKIM</span>
                  <span className="font-semibold text-[#202938]">
                    {headerAnalysis?.reportedDkim || headerAnalysis?.dkimResult || 'NONE'}
                  </span>
                </div>
                <div className="p-2 rounded bg-white border border-[#E4E7EC]">
                  <span className="text-[10px] text-[#667085] block">DMARC</span>
                  <span className="font-semibold text-[#202938]">
                    {headerAnalysis?.reportedDmarc || headerAnalysis?.dmarcResult || 'NONE'}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Section 4: Mail route hops */}
        {hops.length > 0 && (
          <div className="print-avoid-break space-y-3">
            <div className="border-b border-[#E4E7EC] pb-2">
              <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
                <Server className="h-4 w-4 text-[#2563EB]" />
                <span>4. Mail route hops ({hops.length} hops)</span>
              </h3>
            </div>

            <div className="overflow-x-auto rounded-lg border border-[#E4E7EC]">
              <table className="w-full text-left text-xs">
                <thead className="bg-[#F5F6F8] text-[#667085] uppercase text-[10px] border-b border-[#E4E7EC]">
                  <tr>
                    <th className="p-2.5">Hop #</th>
                    <th className="p-2.5">Sending server</th>
                    <th className="p-2.5">Receiving server</th>
                    <th className="p-2.5">IP address</th>
                    <th className="p-2.5">Delay</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {hops.map((hop, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2.5 font-bold text-[#2563EB]">{hop.hopOrder}</td>
                      <td className="p-2.5 max-w-[160px] truncate font-mono text-[11px]">{hop.fromHost || 'Unknown'}</td>
                      <td className="p-2.5 max-w-[160px] truncate font-mono text-[11px]">{hop.byHost || 'Unknown'}</td>
                      <td className="p-2.5 font-mono text-[11px]">{hop.byIp || hop.hopIp || 'N/A'}</td>
                      <td className="p-2.5">{formatSeconds(hop.delaySeconds) || '0s'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Section 5: Links and URLs */}
        {urls.length > 0 && (
          <div className="print-avoid-break space-y-3">
            <div className="border-b border-[#E4E7EC] pb-2">
              <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
                <Link2 className="h-4 w-4 text-[#2563EB]" />
                <span>5. Extracted links ({urls.length} links)</span>
              </h3>
            </div>

            <div className="overflow-x-auto rounded-lg border border-[#E4E7EC]">
              <table className="w-full text-left text-xs">
                <thead className="bg-[#F5F6F8] text-[#667085] uppercase text-[10px] border-b border-[#E4E7EC]">
                  <tr>
                    <th className="p-2.5">Domain</th>
                    <th className="p-2.5">Safe link address</th>
                    <th className="p-2.5">Anchor mismatch</th>
                    <th className="p-2.5">Risk score</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {urls.slice(0, 10).map((u, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2.5 font-bold font-mono text-[#2563EB]">{u.domain || 'N/A'}</td>
                      <td className="p-2.5 max-w-[280px] truncate font-mono text-[11px]">{u.normalizedUrl}</td>
                      <td className="p-2.5">
                        {u.anchorMismatch ? (
                          <span className="px-1.5 py-0.5 rounded bg-[#FEF2F2] text-[#B42318] text-[10px] font-semibold">
                            Mismatch
                          </span>
                        ) : (
                          <span className="text-[#667085] text-[10px]">No</span>
                        )}
                      </td>
                      <td className="p-2.5 font-mono font-semibold text-[#202938]">
                        {u.urlRiskScore ?? 0} / 100
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Section 6: Network infrastructure */}
        {infrastructure.length > 0 && (
          <div className="print-avoid-break space-y-3">
            <div className="border-b border-[#E4E7EC] pb-2">
              <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
                <Network className="h-4 w-4 text-[#2563EB]" />
                <span>6. IP addresses ({infrastructure.length} IPs)</span>
              </h3>
            </div>

            <div className="overflow-x-auto rounded-lg border border-[#E4E7EC]">
              <table className="w-full text-left text-xs">
                <thead className="bg-[#F5F6F8] text-[#667085] uppercase text-[10px] border-b border-[#E4E7EC]">
                  <tr>
                    <th className="p-2.5">IP address</th>
                    <th className="p-2.5">Context</th>
                    <th className="p-2.5">Classification</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {infrastructure.map((item, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2.5 font-bold font-mono text-[#2563EB]">{item.ipAddress}</td>
                      <td className="p-2.5 text-[#667085] text-[11px]">{item.sourceContext}</td>
                      <td className="p-2.5">
                        {item.isPrivate ? (
                          <span className="px-2 py-0.5 rounded bg-[#F5F6F8] text-[#667085] border border-[#E4E7EC] text-[10px]">
                            Private (RFC 1918)
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded bg-[#EFF6FF] text-[#2563EB] border border-blue-100 text-[10px]">
                            Public IP
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Section 7: Visuals and QR codes */}
        {visuals.length > 0 && (
          <div className="print-avoid-break space-y-3">
            <div className="border-b border-[#E4E7EC] pb-2">
              <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
                <QrCode className="h-4 w-4 text-[#2563EB]" />
                <span>7. Images and QR codes ({visuals.length} images)</span>
              </h3>
            </div>

            <div className="space-y-2">
              {visuals.map((v, idx) => (
                <div key={idx} className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] text-xs space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-[#202938]">{v.filename || `Image #${idx + 1}`}</span>
                    <span className="text-[11px] text-[#667085]">{v.contentType} ({formatBytes(v.byteSize)})</span>
                  </div>

                  {v.qrArtifacts && v.qrArtifacts.length > 0 && (
                    <div className="p-2.5 rounded bg-white border border-[#E4E7EC] space-y-1">
                      <span className="font-semibold text-[#2563EB] block">
                        QR code payload:
                      </span>
                      {v.qrArtifacts.map((qr, qIdx) => (
                        <div key={qIdx} className="font-mono text-[11px] text-[#202938] break-all">
                          {qr.decodedPayload}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Section 8: Message content & indicators */}
        <div className="print-avoid-break space-y-3">
          <div className="border-b border-[#E4E7EC] pb-2">
            <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
              <Brain className="h-4 w-4 text-[#2563EB]" />
              <span>8. Message content analysis</span>
            </h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div className="p-3.5 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] space-y-1.5">
              <span className="font-semibold text-[#202938] block">Suspicious language</span>
              <div className="space-y-1 text-[#667085]">
                <div>Urgent language: <strong className="text-[#202938]">{urgencyDetected ? 'Detected' : 'None'}</strong></div>
                <div>Password requests: <strong className="text-[#202938]">{credentialRequestDetected ? 'Detected' : 'None'}</strong></div>
                <div>Payment requests: <strong className="text-[#202938]">{paymentInstructionDetected ? 'Detected' : 'None'}</strong></div>
                <div>Impersonation: <strong className="text-[#202938]">{impersonationDetected ? 'Detected' : 'None'}</strong></div>
              </div>
            </div>

            <div className="p-3.5 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] space-y-1.5">
              <span className="font-semibold text-[#202938] block">Adversarial prompt injection</span>
              <p className="text-xs text-[#667085]">
                {promptInjectionDetected
                  ? 'Prompt injection attempts detected in message text.'
                  : 'No adversarial prompt injection indicators found.'}
              </p>
            </div>
          </div>
        </div>

        {/* Section 9: Risk factors */}
        {riskBreakdown?.factors && riskBreakdown.factors.length > 0 && (
          <div className="print-avoid-break space-y-3">
            <div className="border-b border-[#E4E7EC] pb-2">
              <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
                <AlertOctagon className="h-4 w-4 text-[#2563EB]" />
                <span>9. Triggered risk factors ({riskBreakdown.factors.length})</span>
              </h3>
            </div>

            <div className="overflow-x-auto rounded-lg border border-[#E4E7EC]">
              <table className="w-full text-left text-xs">
                <thead className="bg-[#F5F6F8] text-[#667085] uppercase text-[10px] border-b border-[#E4E7EC]">
                  <tr>
                    <th className="p-2.5">Severity</th>
                    <th className="p-2.5">Category</th>
                    <th className="p-2.5">Score impact</th>
                    <th className="p-2.5">Explanation</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E4E7EC]">
                  {riskBreakdown.factors.map((f, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="p-2.5">
                        <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-[#F5F6F8] text-[#202938] border border-[#E4E7EC]">
                          {f.severity}
                        </span>
                      </td>
                      <td className="p-2.5 text-[#667085]">{f.category}</td>
                      <td className="p-2.5 font-mono font-bold text-[#B42318]">+{f.scoreImpact} pts</td>
                      <td className="p-2.5 text-[#202938]">{f.explanation}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Section 10: Automated assessment narrative */}
        {aiAssessment && (
          <div className="print-avoid-break space-y-3">
            <div className="border-b border-[#E4E7EC] pb-2">
              <h3 className="text-sm font-bold text-[#202938] flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-[#2563EB]" />
                <span>10. Automated assessment narrative</span>
              </h3>
            </div>

            <div className="p-4 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] space-y-2 text-xs">
              <p className="text-[#202938] leading-relaxed">
                {aiAssessment.assessmentSummary || aiAssessment.summaryExplanation}
              </p>
            </div>
          </div>
        )}

        {/* Section 11: Sign-off block */}
        <div id="report-signoff-block" className="print-avoid-break p-6 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] space-y-4">
          <div className="border-b border-[#E4E7EC] pb-2">
            <h3 className="text-sm font-bold text-[#202938]">
              11. Investigation sign-off and verification
            </h3>
          </div>

          <p className="text-xs text-[#667085] leading-relaxed">
            This investigation report summarizes digital findings and extracted indicators from the preserved email evidence. Technical hashes and analysis records reflect the current state of the investigation.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 text-xs">
            <div className="p-3 rounded-lg bg-white border border-[#E4E7EC] space-y-0.5">
              <span className="text-[#667085] text-[10px] block uppercase">Investigator</span>
              <span className="font-semibold text-[#202938] block">
                {reportData?.analystSignoff?.signoffAnalyst || user?.username || 'analyst'}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-white border border-[#E4E7EC] space-y-0.5">
              <span className="text-[#667085] text-[10px] block uppercase">Role</span>
              <span className="font-semibold text-[#202938] block">
                {reportData?.analystSignoff?.signoffRole || user?.role || 'Email Investigator'}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-white border border-[#E4E7EC] space-y-0.5">
              <span className="text-[#667085] text-[10px] block uppercase">Date</span>
              <span className="font-semibold text-[#202938] block">
                {new Date().toLocaleDateString()}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
