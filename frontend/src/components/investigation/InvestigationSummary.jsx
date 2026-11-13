import React, { useMemo } from 'react'
import {
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  Link2,
  Paperclip,
  Image as ImageIcon,
  QrCode,
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
  visuals = [],
  qrCodes = [],
  hops,
  infrastructure,
  onNavigateTab,
  isAiGenerating = false,
  onGenerateAi
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
    return <span className="px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-[#202938] border border-[#E4E7EC]">{s.toLowerCase()}</span>
  }

  // Deduplicate findings by risk category so each category is shown at most once in Summary
  const topFindings = useMemo(() => {
    const factors = riskBreakdown?.factors || []
    const categoryScores = riskBreakdown?.categoryScores || []

    const catScoreMap = new Map()
    for (const cs of categoryScores) {
      if (cs.category) {
        catScoreMap.set(cs.category, cs)
      }
    }

    const severityRank = { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 }

    // Group factors by category
    const grouped = new Map()
    for (const factor of factors) {
      const cat = factor.category || 'GENERAL'
      if (!grouped.has(cat)) {
        grouped.set(cat, [])
      }
      grouped.get(cat).push(factor)
    }

    const results = []
    for (const [cat, catFactors] of grouped.entries()) {
      // Sort factors within this category to find the most critical one
      const sortedFactors = [...catFactors].sort((a, b) => {
        const scoreA = a.weightPoints ?? a.scoreImpact ?? 0
        const scoreB = b.weightPoints ?? b.scoreImpact ?? 0
        if (scoreB !== scoreA) return scoreB - scoreA
        const sevA = severityRank[a.severity] || 0
        const sevB = severityRank[b.severity] || 0
        return sevB - sevA
      })

      const topFactor = sortedFactors[0]
      const catScoreInfo = catScoreMap.get(cat)

      const totalCategoryScore = catScoreInfo?.score ?? catFactors.reduce(
        (sum, f) => sum + (f.weightPoints ?? f.scoreImpact ?? 0), 0
      )

      results.push({
        category: cat,
        categoryDisplayName: catScoreInfo?.displayName || cat,
        primaryFactor: topFactor,
        factorCount: catFactors.length,
        totalScore: totalCategoryScore,
        severity: topFactor?.severity || 'LOW',
        code: topFactor?.factorCode || topFactor?.factorName || topFactor?.factorType || cat,
        title: topFactor?.factorName || topFactor?.factorCode || catScoreInfo?.displayName || cat,
        description: topFactor?.explanation || topFactor?.description || 'Risk indicators detected in this category.',
      })
    }

    // Fallback if factors array was empty but categoryScores had triggered items
    if (results.length === 0 && categoryScores.length > 0) {
      for (const cs of categoryScores) {
        if (cs.score > 0) {
          results.push({
            category: cs.category,
            categoryDisplayName: cs.displayName || cs.category,
            primaryFactor: null,
            factorCount: cs.factorCount || 1,
            totalScore: cs.score,
            severity: cs.score >= 40 ? 'CRITICAL' : cs.score >= 25 ? 'HIGH' : cs.score >= 10 ? 'MEDIUM' : 'LOW',
            code: cs.category,
            title: cs.displayName || cs.category,
            description: `Suspicious indicators detected in ${cs.displayName || cs.category}.`,
          })
        }
      }
    }

    // Sort categories by total score descending, then by severity
    results.sort((a, b) => {
      if (b.totalScore !== a.totalScore) return b.totalScore - a.totalScore
      const sevA = severityRank[a.severity] || 0
      const sevB = severityRank[b.severity] || 0
      return sevB - sevA
    })

    return results
  }, [riskBreakdown?.factors, riskBreakdown?.categoryScores])

  return (
    <div className="space-y-4">
      {/* High-level score cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
        {/* Card 1: Classification */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs text-[#202938]">
            <span>Classification</span>
            <ShieldAlert className="h-4 w-4 text-[#202938]" />
          </div>
          <div className="mt-3">
            {getThreatBadge(threatClass)}
          </div>
          <p className="text-xs text-[#202938] mt-2">
            Overall assessment based on indicators found.
          </p>
        </div>

        {/* Card 2: Risk Score */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs text-[#202938]">
            <span>Risk score</span>
            <span className={`text-xs px-2 py-0.5 rounded font-medium ${getRiskCardStyle(riskLevel)}`}>
              {riskLevel}
            </span>
          </div>
          <div className="mt-2">
            <span className="text-2xl font-bold text-[#202938] font-mono">
              {riskScore}
            </span>
            <span className="text-xs text-[#202938] ml-1">/ 100</span>
          </div>
          <p className="text-xs text-[#202938] mt-2">
            A score based on suspicious indicators found.
          </p>
        </div>

        {/* Card 3: Evidence Confidence */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-4 flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs text-[#202938]">
            <span>Evidence confidence</span>
            <span className="text-xs px-2 py-0.5 rounded font-medium bg-slate-100 text-[#202938]">
              {confidenceLevel}
            </span>
          </div>
          <div className="mt-2">
            <span className="text-2xl font-bold text-[#202938] font-mono">
              {confidenceScore}%
            </span>
          </div>
          <p className="text-xs text-[#202938] mt-2">
            Evaluation confidence based on available evidence.
          </p>
        </div>

        {/* Card 4: Sender Authentication */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-4 flex flex-col justify-between">
          <div className="text-xs text-[#202938]">
            <span>Sender authentication</span>
          </div>
          <div className="mt-2 space-y-1">
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#202938]">SPF</span>
              {getStatusPill(spfStatus)}
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#202938]">DKIM</span>
              {getStatusPill(dkimStatus)}
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#202938]">DMARC</span>
              {getStatusPill(dmarcStatus)}
            </div>
          </div>
          <p className="text-xs text-[#202938] mt-2">
            Email sender verification checks.
          </p>
        </div>
      </div>

      {/* Quick counts */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2.5">
        <button
          onClick={() => onNavigateTab('urls')}
          className="bg-white hover:bg-slate-50 border border-[#E4E7EC] rounded-xl p-4 text-left transition-colors cursor-pointer"
        >
          <div className="flex items-center justify-between text-[#202938]">
            <span className="text-xs">Links</span>
            <Link2 className="h-4 w-4 text-[#202938]" />
          </div>
          <div className="text-2xl font-bold text-[#202938] mt-2 font-mono">
            {urls?.length || 0}
          </div>
          <span className="text-xs text-[#2563EB] mt-1 inline-flex items-center gap-1 font-medium">
            View links <ArrowRight className="h-3 w-3" />
          </span>
        </button>

        <button
          onClick={() => onNavigateTab('qr')}
          className="bg-white hover:bg-slate-50 border border-[#E4E7EC] rounded-xl p-4 text-left transition-colors cursor-pointer"
        >
          <div className="flex items-center justify-between text-[#202938]">
            <span className="text-xs">QR codes</span>
            <QrCode className="h-4 w-4 text-[#202938]" />
          </div>
          <div className="text-2xl font-bold text-[#202938] mt-2 font-mono">
            {qrCodes?.length || visuals?.reduce((acc, v) => acc + (v.qrArtifacts?.length || 0), 0) || 0}
          </div>
          <span className="text-xs text-[#2563EB] mt-1 inline-flex items-center gap-1 font-medium">
            View QR codes <ArrowRight className="h-3 w-3" />
          </span>
        </button>

        <button
          onClick={() => onNavigateTab('attachments')}
          className="bg-white hover:bg-slate-50 border border-[#E4E7EC] rounded-xl p-4 text-left transition-colors cursor-pointer"
        >
          <div className="flex items-center justify-between text-[#202938]">
            <span className="text-xs">Attachments</span>
            <Paperclip className="h-4 w-4 text-[#202938]" />
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
          <div className="flex items-center justify-between text-[#202938]">
            <span className="text-xs">Mail route hops</span>
            <Server className="h-4 w-4 text-[#202938]" />
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
          <div className="flex items-center justify-between text-[#202938]">
            <span className="text-xs">IP addresses</span>
            <Network className="h-4 w-4 text-[#202938]" />
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
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-4 sm:p-5 space-y-3">
        <div className="flex items-center justify-between pb-3 border-b border-[#E4E7EC]">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-[#2563EB]" />
            <h2 className="text-sm font-bold text-[#202938]">
              {aiAssessment?.status === 'LIVE_AI'
                ? 'Automated assessment'
                : aiAssessment?.status
                ? 'Rule-based assessment'
                : 'Automated assessment'}
            </h2>
          </div>
          <button
            onClick={() => onNavigateTab('ai')}
            className="text-xs text-[#2563EB] hover:underline font-medium inline-flex items-center gap-1 cursor-pointer"
          >
            <span>View full assessment</span>
            <ArrowRight className="h-3 w-3" />
          </button>
        </div>

        {aiAssessment?.summaryExplanation || aiAssessment?.assessmentSummary ? (
          <p className="text-sm text-[#202938] leading-relaxed">
            {aiAssessment.summaryExplanation || aiAssessment.assessmentSummary}
          </p>
        ) : isAiGenerating ? (
          <p className="text-sm text-[#202938] leading-relaxed animate-pulse">
            Generating automated assessment summary...
          </p>
        ) : (
          <div className="flex items-center justify-between gap-4">
            <p className="text-sm text-[#202938] leading-relaxed">
              No assessment summary generated yet.
            </p>
            {onGenerateAi && (
              <button
                onClick={onGenerateAi}
                className="px-3 py-1.5 rounded-lg text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer shrink-0"
              >
                Generate assessment
              </button>
            )}
          </div>
        )}
      </div>

      {/* Contributing findings */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-4 sm:p-5 space-y-3">
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
          <p className="text-xs text-[#202938]">No significant suspicious findings were detected.</p>
        ) : (
          <div className="space-y-3">
            {topFindings.map((finding, idx) => (
              <div
                key={finding.category || idx}
                className="p-3.5 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
              >
                <div className="space-y-1 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-[#202938]">
                      {finding.title}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-white text-[#202938] border border-[#E4E7EC]">
                      {finding.category}
                    </span>
                    {finding.severity && (
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                          finding.severity === 'CRITICAL'
                            ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200'
                            : finding.severity === 'HIGH' || finding.severity === 'MEDIUM'
                            ? 'bg-[#FFFBEB] text-[#B45309] border border-amber-200'
                            : 'bg-[#F0FDF4] text-[#15803D] border border-green-200'
                        }`}
                      >
                        {finding.severity}
                      </span>
                    )}
                    {finding.factorCount > 1 && (
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-[#EFF6FF] text-[#2563EB] border border-blue-200">
                        {finding.factorCount} findings
                      </span>
                    )}
                  </div>
                  <p className="text-[#202938] leading-relaxed">{finding.description}</p>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="font-mono text-xs font-semibold text-[#B42318]">
                    +{finding.totalScore} pts
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

