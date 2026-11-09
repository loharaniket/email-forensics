import React from 'react'
import {
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  Link2,
  Paperclip,
  Server,
  Network,
  Sparkles,
  ArrowRight
} from 'lucide-react'

export default function InvestigationSummary({
  riskBreakdown,
  aiAssessment,
  headerAnalysis,
  urls,
  attachments,
  hops,
  infrastructure,
  onNavigateTab
}) {
  const riskScore = riskBreakdown?.deterministicRiskScore ?? 0
  const riskLevel = riskBreakdown?.riskLevel || 'LOW'
  const confidenceScore = riskBreakdown?.evidenceConfidenceScore ?? 0
  const confidenceLevel = riskBreakdown?.confidenceLevel || 'LOW'

  const threatClass = aiAssessment?.threatClassification || (
    riskLevel === 'CRITICAL' || riskLevel === 'HIGH' ? 'SUSPICIOUS' : 'BENIGN'
  )

  const getThreatBadge = (classification) => {
    if (classification?.includes('MALICIOUS')) {
      return (
        <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-[#FEF2F2] text-[#B42318] border border-red-200">
          <AlertTriangle className="h-3.5 w-3.5" />
          Malicious
        </span>
      )
    }
    if (classification?.includes('SUSPICIOUS')) {
      return (
        <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-[#FFFBEB] text-[#B45309] border border-amber-200">
          <AlertTriangle className="h-3.5 w-3.5" />
          Suspicious
        </span>
      )
    }
    return (
      <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-[#F0FDF4] text-[#15803D] border border-green-200">
        <ShieldCheck className="h-3.5 w-3.5" />
        Benign
      </span>
    )
  }

  const getRiskCardStyle = (level) => {
    if (level === 'CRITICAL' || level === 'HIGH') {
      return 'bg-[#FEF2F2] border-red-200 text-[#B42318]'
    }
    if (level === 'MEDIUM') {
      return 'bg-[#FFFBEB] border-amber-200 text-[#B45309]'
    }
    return 'bg-[#F0FDF4] border-green-200 text-[#15803D]'
  }

  const spfStatus =
    headerAnalysis?.spfResult ||
    headerAnalysis?.reportedSpf ||
    headerAnalysis?.activeSpfResult ||
    'NONE'
  const dkimStatus =
    headerAnalysis?.dkimResult ||
    headerAnalysis?.reportedDkim ||
    headerAnalysis?.activeDkimStatus ||
    'NONE'
  const dmarcStatus =
    headerAnalysis?.dmarcResult ||
    headerAnalysis?.reportedDmarc ||
    headerAnalysis?.activeDmarcStatus ||
    'NONE'

  const getStatusPill = (status) => {
    const s = String(status).toUpperCase()
    if (s === 'PASS' || s === 'SELECTOR_FOUND') {
      return <span className="px-2 py-0.5 rounded text-xs font-medium bg-[#F0FDF4] text-[#15803D] border border-green-200">Pass</span>
    }
    if (s === 'FAIL' || s === 'HARDFAIL' || s === 'REJECT') {
      return <span className="px-2 py-0.5 rounded text-xs font-medium bg-[#FEF2F2] text-[#B42318] border border-red-200">Fail</span>
    }
    if (s === 'SOFTFAIL' || s === 'NEUTRAL' || s === 'QUARANTINE') {
      return <span className="px-2 py-0.5 rounded text-xs font-medium bg-[#FFFBEB] text-[#B45309] border border-amber-200">{s.toLowerCase()}</span>
    }
    return <span className="px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-[#667085] border border-[#E4E7EC]">{s.toLowerCase()}</span>
  }

  const topFindings = riskBreakdown?.factors || []

  return (
    <div className="space-y-6">
      {/* High-level score cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        {/* Card 1: Classification */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs text-[#667085]">
            <span>Classification</span>
            <ShieldAlert className="h-4 w-4 text-[#98A2B3]" />
          </div>
          <div className="mt-4">
            {getThreatBadge(threatClass)}
          </div>
          <p className="text-xs text-[#667085] mt-3">
            Overall assessment based on indicators found.
          </p>
        </div>

        {/* Card 2: Risk Score */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs text-[#667085]">
            <span>Risk score</span>
            <span className={`text-xs px-2 py-0.5 rounded font-medium ${getRiskCardStyle(riskLevel)}`}>
              {riskLevel}
            </span>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-bold text-[#202938] font-mono">
              {riskScore}
            </span>
            <span className="text-sm text-[#98A2B3] ml-1">/ 100</span>
          </div>
          <p className="text-xs text-[#667085] mt-3">
            A score based on suspicious indicators found.
          </p>
        </div>

        {/* Card 3: Evidence Confidence */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs text-[#667085]">
            <span>Evidence confidence</span>
            <span className="text-xs px-2 py-0.5 rounded font-medium bg-slate-100 text-[#667085]">
              {confidenceLevel}
            </span>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-bold text-[#202938] font-mono">
              {confidenceScore}%
            </span>
          </div>
          <p className="text-xs text-[#667085] mt-3">
            How much supporting information is available.
          </p>
        </div>

        {/* Card 4: Sender Authentication */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-5 flex flex-col justify-between">
          <div className="text-xs text-[#667085]">
            <span>Sender authentication</span>
          </div>
          <div className="mt-3 space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#667085]">SPF</span>
              {getStatusPill(spfStatus)}
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#667085]">DKIM</span>
              {getStatusPill(dkimStatus)}
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#667085]">DMARC</span>
              {getStatusPill(dmarcStatus)}
            </div>
          </div>
          <p className="text-xs text-[#667085] mt-3">
            Email sender verification checks.
          </p>
        </div>
      </div>

      {/* Quick counts */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <button
          onClick={() => onNavigateTab('urls')}
          className="bg-white hover:bg-slate-50 border border-[#E4E7EC] rounded-xl p-4 text-left transition-colors cursor-pointer"
        >
          <div className="flex items-center justify-between text-[#667085]">
            <span className="text-xs">Links</span>
            <Link2 className="h-4 w-4 text-[#98A2B3]" />
          </div>
          <div className="text-2xl font-bold text-[#202938] mt-2 font-mono">
            {urls?.length || 0}
          </div>
          <span className="text-xs text-[#2563EB] mt-1 inline-flex items-center gap-1 font-medium">
            View links <ArrowRight className="h-3 w-3" />
          </span>
        </button>

        <button
          onClick={() => onNavigateTab('attachments')}
          className="bg-white hover:bg-slate-50 border border-[#E4E7EC] rounded-xl p-4 text-left transition-colors cursor-pointer"
        >
          <div className="flex items-center justify-between text-[#667085]">
            <span className="text-xs">Attachments</span>
            <Paperclip className="h-4 w-4 text-[#98A2B3]" />
          </div>
          <div className="text-2xl font-bold text-[#202938] mt-2 font-mono">
            {attachments?.length || 0}
          </div>
          <span className="text-xs text-[#2563EB] mt-1 inline-flex items-center gap-1 font-medium">
            View files <ArrowRight className="h-3 w-3" />
          </span>
        </button>

        <button
          onClick={() => onNavigateTab('hops')}
          className="bg-white hover:bg-slate-50 border border-[#E4E7EC] rounded-xl p-4 text-left transition-colors cursor-pointer"
        >
          <div className="flex items-center justify-between text-[#667085]">
            <span className="text-xs">Mail route hops</span>
            <Server className="h-4 w-4 text-[#98A2B3]" />
          </div>
          <div className="text-2xl font-bold text-[#202938] mt-2 font-mono">
            {hops?.length || 0}
          </div>
          <span className="text-xs text-[#2563EB] mt-1 inline-flex items-center gap-1 font-medium">
            View route <ArrowRight className="h-3 w-3" />
          </span>
        </button>

        <button
          onClick={() => onNavigateTab('infrastructure')}
          className="bg-white hover:bg-slate-50 border border-[#E4E7EC] rounded-xl p-4 text-left transition-colors cursor-pointer"
        >
          <div className="flex items-center justify-between text-[#667085]">
            <span className="text-xs">IP addresses</span>
            <Network className="h-4 w-4 text-[#98A2B3]" />
          </div>
          <div className="text-2xl font-bold text-[#202938] mt-2 font-mono">
            {infrastructure?.length || 0}
          </div>
          <span className="text-xs text-[#2563EB] mt-1 inline-flex items-center gap-1 font-medium">
            View IPs <ArrowRight className="h-3 w-3" />
          </span>
        </button>
      </div>

      {/* Automated assessment summary */}
      {aiAssessment && (
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-3">
          <div className="flex items-center justify-between pb-3 border-b border-[#E4E7EC]">
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-[#2563EB]" />
              <h2 className="text-sm font-bold text-[#202938]">
                {aiAssessment.status === 'LIVE_AI' ? 'Automated assessment' : 'Rule-based assessment'}
              </h2>
            </div>
            <button
              onClick={() => onNavigateTab('ai')}
              className="text-xs text-[#2563EB] hover:underline font-medium inline-flex items-center gap-1"
            >
              <span>View full assessment</span>
              <ArrowRight className="h-3 w-3" />
            </button>
          </div>
          <p className="text-sm text-[#202938] leading-relaxed">
            {aiAssessment.assessmentSummary || 'No assessment summary available.'}
          </p>
        </div>
      )}

      {/* Contributing findings */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-[#E4E7EC]">
          <h2 className="text-sm font-bold text-[#202938]">
            Important findings ({topFindings.length})
          </h2>
          <button
            onClick={() => onNavigateTab('risk')}
            className="text-xs text-[#2563EB] hover:underline font-medium inline-flex items-center gap-1"
          >
            <span>View all risk factors</span>
            <ArrowRight className="h-3 w-3" />
          </button>
        </div>

        {topFindings.length === 0 ? (
          <p className="text-xs text-[#667085]">No significant suspicious findings were detected.</p>
        ) : (
          <div className="space-y-3">
            {topFindings.slice(0, 5).map((factor, idx) => (
              <div
                key={idx}
                className="p-3.5 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
              >
                <div className="space-y-1 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-[#202938]">{factor.factorName || factor.factorType}</span>
                    <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-white text-[#667085] border border-[#E4E7EC]">
                      {factor.category || 'General'}
                    </span>
                  </div>
                  <p className="text-[#667085]">{factor.description}</p>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="font-mono text-xs font-semibold text-[#B42318]">
                    +{factor.scoreImpact} pts
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

