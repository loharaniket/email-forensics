import React, { useState, useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Search, ChevronRight } from 'lucide-react'

export default function RelatedCases({ correlations = [] }) {
  const navigate = useNavigate()
  const [correlationSearch, setCorrelationSearch] = useState('')
  const [correlationStrengthFilter, setCorrelationStrengthFilter] = useState('ALL')
  const [correlationTypeFilter, setCorrelationTypeFilter] = useState('ALL')

  const filteredCorrelations = useMemo(() => {
    return correlations.filter((match) => {
      const matchStrength =
        correlationStrengthFilter === 'ALL' ||
        match.matchStrength === correlationStrengthFilter
      const matchType =
        correlationTypeFilter === 'ALL' ||
        match.indicatorType === correlationTypeFilter
      const query = correlationSearch.toLowerCase().trim()
      const matchText =
        !query ||
        (match.indicatorType && match.indicatorType.toLowerCase().includes(query)) ||
        (match.normalizedIndicatorValue &&
          match.normalizedIndicatorValue.toLowerCase().includes(query)) ||
        (match.matchedEmailSubject &&
          match.matchedEmailSubject.toLowerCase().includes(query)) ||
        (match.matchedCaseNumber &&
          match.matchedCaseNumber.toLowerCase().includes(query)) ||
        (match.matchExplanation &&
          match.matchExplanation.toLowerCase().includes(query))

      return matchStrength && matchType && matchText
    })
  }, [correlations, correlationStrengthFilter, correlationTypeFilter, correlationSearch])

  return (
    <div className="space-y-6">
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#E4E7EC] pb-4">
          <div>
            <h2 className="text-sm font-bold text-[#202938]">
              Related cases ({correlations.length})
            </h2>
            <p className="text-xs text-[#667085] mt-0.5">
              Indicators matching previously analyzed emails and cases.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="h-3.5 w-3.5 text-[#98A2B3] absolute left-3 top-2.5" />
              <input
                id="correlation-search-input"
                type="text"
                placeholder="Search matches..."
                value={correlationSearch}
                onChange={(e) => setCorrelationSearch(e.target.value)}
                className="bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg pl-9 pr-3 py-1.5 text-xs text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB] w-44 sm:w-56"
              />
            </div>

            <select
              id="correlation-strength-filter"
              value={correlationStrengthFilter}
              onChange={(e) => setCorrelationStrengthFilter(e.target.value)}
              className="bg-white border border-[#E4E7EC] rounded-lg px-2.5 py-1.5 text-xs text-[#202938] focus:outline-none focus:border-[#2563EB] cursor-pointer"
            >
              <option value="ALL">All strengths</option>
              <option value="EXACT">Exact (100%)</option>
              <option value="HIGH">High (&gt;=80%)</option>
              <option value="MEDIUM">Medium (&gt;=50%)</option>
            </select>

            <select
              id="correlation-type-filter"
              value={correlationTypeFilter}
              onChange={(e) => setCorrelationTypeFilter(e.target.value)}
              className="bg-white border border-[#E4E7EC] rounded-lg px-2.5 py-1.5 text-xs text-[#202938] focus:outline-none focus:border-[#2563EB] cursor-pointer"
            >
              <option value="ALL">All indicator types</option>
              <option value="EMAIL_HASH">Email hash</option>
              <option value="ATTACHMENT_HASH">Attachment hash</option>
              <option value="URL">Link / URL</option>
              <option value="DOMAIN">Domain</option>
              <option value="IP_ADDRESS">IP address</option>
              <option value="SENDER_DOMAIN">Sender domain</option>
              <option value="QR_PAYLOAD">QR code payload</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto rounded-lg border border-[#E4E7EC]">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F5F6F8] text-[11px] text-[#667085] uppercase tracking-wider border-b border-[#E4E7EC]">
              <tr>
                <th className="py-3 px-4 font-semibold">Indicator</th>
                <th className="py-3 px-4 font-semibold">Indicator value</th>
                <th className="py-3 px-4 font-semibold">Related email &amp; case</th>
                <th className="py-3 px-4 font-semibold">Match strength</th>
                <th className="py-3 px-4 font-semibold">Explanation</th>
                <th className="py-3 px-4 text-right font-semibold">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E4E7EC]">
              {filteredCorrelations.length === 0 ? (
                <tr>
                  <td colSpan="6" className="py-8 text-center text-[#667085]">
                    No related case matches found. Indicators appear unique across existing records.
                  </td>
                </tr>
              ) : (
                filteredCorrelations.map((match) => (
                  <tr key={match.id} className="hover:bg-slate-50 transition-colors">
                    <td className="py-3.5 px-4">
                      <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
                        {match.indicatorType}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 font-mono text-[11px] text-[#202938] max-w-xs break-all">
                      {match.normalizedIndicatorValue}
                    </td>

                    <td className="py-3.5 px-4 space-y-0.5">
                      <span
                        className="font-medium text-[#202938] block max-w-xs truncate"
                        title={match.matchedEmailSubject}
                      >
                        {match.matchedEmailSubject || 'Untitled email'}
                      </span>
                      <Link
                        to={`/cases/${match.matchedCaseId}`}
                        className="text-[11px] text-[#2563EB] hover:underline block truncate"
                      >
                        {match.matchedCaseNumber}
                      </Link>
                    </td>

                    <td className="py-3.5 px-4">
                      <span
                        className={`px-2 py-0.5 rounded text-[11px] font-medium ${
                          match.matchStrength === 'EXACT'
                            ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200'
                            : match.matchStrength === 'HIGH'
                            ? 'bg-[#FFFBEB] text-[#B45309] border border-amber-200'
                            : 'bg-slate-100 text-[#667085] border border-[#E4E7EC]'
                        }`}
                      >
                        {match.matchStrength}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 text-xs text-[#667085] max-w-xs">
                      {match.matchExplanation}
                    </td>

                    <td className="py-3.5 px-4 text-right">
                      <button
                        id={`inspect-match-${match.id}`}
                        onClick={() =>
                          navigate(
                            `/cases/${match.matchedCaseId}/evidence/${match.matchedEmailId}`
                          )
                        }
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer"
                      >
                        <span>View</span>
                        <ChevronRight className="h-3 w-3 text-[#667085]" />
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
  )
}

