import React, { useState, useMemo } from 'react'
import {
  ShieldAlert,
  ShieldCheck,
  Search,
  Activity,
  Layers,
  Info
} from 'lucide-react'

export default function RiskFindings({ riskBreakdown }) {
  const [riskSeverityFilter, setRiskSeverityFilter] = useState('ALL')
  const [riskCategoryFilter, setRiskCategoryFilter] = useState('ALL')
  const [riskSearch, setRiskSearch] = useState('')

  if (!riskBreakdown) {
    return (
      <div className="p-8 text-center text-xs text-[#667085] bg-white rounded-xl border border-[#E4E7EC] shadow-xs">
        Calculating risk and findings...
      </div>
    )
  }

  const riskScore = riskBreakdown.deterministicRiskScore ?? 0
  const riskLevel = riskBreakdown.riskLevel || 'LOW'
  const confidenceScore = riskBreakdown.evidenceConfidenceScore ?? riskBreakdown.confidenceScore ?? 0
  const confidenceLevel = riskBreakdown.confidenceLevel || 'LOW'

  const factors = riskBreakdown.factors || []
  const filteredFactors = factors.filter((f) => {
    const matchSev =
      riskSeverityFilter === 'ALL' || f.severity === riskSeverityFilter
    const matchCat =
      riskCategoryFilter === 'ALL' || f.category === riskCategoryFilter
    const matchTerm =
      !riskSearch ||
      (f.factorCode && f.factorCode.toLowerCase().includes(riskSearch.toLowerCase())) ||
      (f.explanation && f.explanation.toLowerCase().includes(riskSearch.toLowerCase())) ||
      (f.evidenceReference &&
        f.evidenceReference.toLowerCase().includes(riskSearch.toLowerCase()))
    return matchSev && matchCat && matchTerm
  })

  return (
    <div className="space-y-6">
      {/* Top Scores Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Risk Score Card */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShieldAlert className="h-5 w-5 text-[#2563EB]" />
              <h2 className="text-sm font-bold text-[#202938]">
                Risk score
              </h2>
            </div>
            <span
              id="risk-band-badge"
              className={`px-2.5 py-1 rounded text-xs font-semibold uppercase ${
                riskLevel === 'CRITICAL'
                  ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200'
                  : riskLevel === 'HIGH'
                  ? 'bg-[#FFFBEB] text-[#B45309] border border-amber-200'
                  : riskLevel === 'MEDIUM'
                  ? 'bg-[#FFFBEB] text-[#B45309] border border-amber-200'
                  : 'bg-[#F0FDF4] text-[#15803D] border border-green-200'
              }`}
            >
              {riskLevel} ({
                riskLevel === 'CRITICAL'
                  ? '75–100'
                  : riskLevel === 'HIGH'
                  ? '50–74'
                  : riskLevel === 'MEDIUM'
                  ? '25–49'
                  : '0–24'
              })
            </span>
          </div>

          <div className="flex items-baseline gap-2">
            <span
              id="risk-score-value"
              className={`text-4xl sm:text-5xl font-black font-mono ${
                riskLevel === 'CRITICAL'
                  ? 'text-[#B42318]'
                  : riskLevel === 'HIGH'
                  ? 'text-[#B45309]'
                  : riskLevel === 'MEDIUM'
                  ? 'text-[#B45309]'
                  : 'text-[#15803D]'
              }`}
            >
              {riskScore}
            </span>
            <span className="text-base text-[#98A2B3]">/ 100</span>
            <span className="text-xs text-[#667085] ml-auto">
              Uncapped score: <strong>{riskBreakdown.totalUncappedScore || riskScore} pts</strong>
            </span>
          </div>

          <div className="w-full bg-[#F5F6F8] rounded-full h-2.5 overflow-hidden border border-[#E4E7EC]">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                riskLevel === 'CRITICAL'
                  ? 'bg-[#B42318]'
                  : riskLevel === 'HIGH'
                  ? 'bg-[#B45309]'
                  : riskLevel === 'MEDIUM'
                  ? 'bg-[#B45309]'
                  : 'bg-[#15803D]'
              }`}
              style={{ width: `${Math.max(4, Math.min(100, riskScore))}%` }}
            />
          </div>

          <p className="text-xs text-[#667085] bg-[#F5F6F8] p-3 rounded-lg border border-[#E4E7EC] leading-relaxed">
            A score based on suspicious indicators found in the email.
          </p>
        </div>

        {/* Evidence Confidence Card */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-[#2563EB]" />
              <h2 className="text-sm font-bold text-[#202938]">
                Evidence confidence
              </h2>
            </div>
            <span
              id="confidence-level-badge"
              className="px-2.5 py-1 rounded text-xs font-semibold bg-[#EFF6FF] text-[#2563EB] border border-blue-100 uppercase"
            >
              {confidenceLevel} confidence
            </span>
          </div>

          <div className="flex items-baseline gap-2">
            <span
              id="confidence-score-value"
              className="text-4xl sm:text-5xl font-black font-mono text-[#202938]"
            >
              {confidenceScore}
            </span>
            <span className="text-base text-[#98A2B3]">/ 100</span>
          </div>

          <div className="w-full bg-[#F5F6F8] rounded-full h-2.5 overflow-hidden border border-[#E4E7EC]">
            <div
              className="h-full bg-[#2563EB] rounded-full transition-all duration-500"
              style={{ width: `${Math.max(4, Math.min(100, confidenceScore))}%` }}
            />
          </div>

          <p className="text-xs text-[#667085] bg-[#F5F6F8] p-3 rounded-lg border border-[#E4E7EC] leading-relaxed">
            {riskBreakdown.confidenceExplanation ||
              'An indication of how much supporting information is available across different email channels.'}
          </p>
        </div>
      </div>

      {/* Category breakdown */}
      {riskBreakdown.categoryScores && riskBreakdown.categoryScores.length > 0 && (
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-[#E4E7EC]">
            <h3 className="text-sm font-bold text-[#202938]">
              Risk contribution by category
            </h3>
            <span className="text-xs text-[#667085]">
              Click a category to filter findings
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7 gap-3">
            {riskBreakdown.categoryScores.map((cat) => {
              const isSelected = riskCategoryFilter === cat.category
              const hasFindings = cat.score > 0
              return (
                <button
                  key={cat.category}
                  id={`cat-card-${cat.category.toLowerCase()}`}
                  onClick={() =>
                    setRiskCategoryFilter(isSelected ? 'ALL' : cat.category)
                  }
                  className={`p-3.5 rounded-xl border text-left transition-colors cursor-pointer ${
                    isSelected
                      ? 'bg-[#EFF6FF] border-[#2563EB]'
                      : hasFindings
                      ? 'bg-[#F5F6F8] border-[#E4E7EC] hover:border-slate-300'
                      : 'bg-white border-[#E4E7EC] hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-[11px] text-[#667085]">
                      {cat.factorCount} {cat.factorCount === 1 ? 'finding' : 'findings'}
                    </span>
                    <span className={`text-xs font-mono font-bold ${hasFindings ? 'text-[#B42318]' : 'text-[#667085]'}`}>
                      +{cat.score}
                    </span>
                  </div>
                  <span className="text-xs font-semibold text-[#202938] block truncate">
                    {cat.displayName || cat.category}
                  </span>
                </button>
              )
            })}
          </div>
        </div>
      )}

      {/* Triggered Risk Factors List */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#E4E7EC]">
          <h3 className="text-sm font-bold text-[#202938]">
            Triggered risk factors ({factors.length})
          </h3>

          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs text-[#667085]">Severity:</span>
            {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((sev) => (
              <button
                key={sev}
                id={`risk-filter-sev-${sev.toLowerCase()}`}
                onClick={() => setRiskSeverityFilter(sev)}
                className={`px-2.5 py-1 rounded-md text-xs font-medium transition-colors cursor-pointer ${
                  riskSeverityFilter === sev
                    ? 'bg-[#EFF6FF] text-[#2563EB] font-semibold'
                    : 'text-[#667085] hover:text-[#202938] hover:bg-slate-50'
                }`}
              >
                {sev === 'ALL' ? 'All' : sev.charAt(0) + sev.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search className="h-4 w-4 absolute left-3 top-2.5 text-[#98A2B3]" />
            <input
              id="risk-factor-search-input"
              type="text"
              placeholder="Search risk factors..."
              value={riskSearch}
              onChange={(e) => setRiskSearch(e.target.value)}
              className="w-full bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg pl-9 pr-3 py-1.5 text-xs text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB]"
            />
          </div>

          <div className="flex items-center gap-2">
            {riskCategoryFilter !== 'ALL' && (
              <span className="text-xs px-2.5 py-1 rounded bg-[#EFF6FF] text-[#2563EB] border border-blue-100 flex items-center gap-1">
                Category: {riskCategoryFilter}
                <button
                  onClick={() => setRiskCategoryFilter('ALL')}
                  className="ml-1 text-[#2563EB] hover:text-blue-800 cursor-pointer"
                >
                  &times;
                </button>
              </span>
            )}
            {(riskSeverityFilter !== 'ALL' ||
              riskCategoryFilter !== 'ALL' ||
              riskSearch) && (
              <button
                onClick={() => {
                  setRiskSeverityFilter('ALL')
                  setRiskCategoryFilter('ALL')
                  setRiskSearch('')
                }}
                className="text-xs text-[#2563EB] hover:underline cursor-pointer"
              >
                Reset filters
              </button>
            )}
          </div>
        </div>

        {factors.length === 0 ? (
          <div
            id="risk-factors-clean"
            className="p-8 rounded-xl bg-[#F0FDF4] border border-[#DCFCE7] text-center space-y-2"
          >
            <ShieldCheck className="h-8 w-8 text-[#15803D] mx-auto" />
            <h4 className="text-sm font-bold text-[#15803D]">
              No suspicious indicators detected
            </h4>
            <p className="text-xs text-[#667085] max-w-md mx-auto">
              All evaluated email indicators are within safe thresholds.
            </p>
          </div>
        ) : filteredFactors.length === 0 ? (
          <div className="p-8 text-center text-xs text-[#667085] bg-[#F5F6F8] rounded-xl border border-[#E4E7EC]">
            No risk factors match your search filters.
          </div>
        ) : (
          <div className="space-y-2.5 max-h-[500px] overflow-y-auto">
            {filteredFactors.map((factor, idx) => (
              <div
                key={factor.id || idx}
                id={`risk-factor-item-${idx}`}
                className="p-4 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] flex flex-col md:flex-row md:items-start justify-between gap-3 text-xs"
              >
                <div className="space-y-1 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span
                      className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                        factor.severity === 'CRITICAL'
                          ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200'
                          : factor.severity === 'HIGH'
                          ? 'bg-[#FFFBEB] text-[#B45309] border border-amber-200'
                          : factor.severity === 'MEDIUM'
                          ? 'bg-[#FFFBEB] text-[#B45309] border border-amber-200'
                          : 'bg-[#F0FDF4] text-[#15803D] border border-green-200'
                      }`}
                    >
                      {factor.severity}
                    </span>
                    <span className="font-mono text-[#202938] font-bold">
                      {factor.factorCode}
                    </span>
                    <span className="text-[11px] bg-white text-[#667085] px-2 py-0.5 rounded border border-[#E4E7EC]">
                      {factor.category}
                    </span>
                  </div>

                  <p className="text-[#202938] text-xs pt-0.5 leading-relaxed">
                    {factor.explanation}
                  </p>

                  {factor.evidenceReference && (
                    <div className="pt-1 text-[11px] font-mono text-[#667085]">
                      Reference: <span className="text-[#202938]">{factor.evidenceReference}</span>
                    </div>
                  )}
                </div>

                <div className="shrink-0 flex items-center">
                  <span className="font-mono text-sm font-bold text-[#B42318]">
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
