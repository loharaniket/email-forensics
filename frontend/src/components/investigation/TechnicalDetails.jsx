import React, { useState, useMemo } from 'react'
import { Search, Layers, Copy, Check } from 'lucide-react'

export default function TechnicalDetails({
  rawHeaders = {},
  copiedKey,
  copyValue
}) {
  const [rawHeaderSearch, setRawHeaderSearch] = useState('')

  const rawHeaderEntries = useMemo(() => {
    return Object.entries(rawHeaders || {})
  }, [rawHeaders])

  const filteredRawHeaders = useMemo(() => {
    if (!rawHeaderSearch.trim()) return rawHeaderEntries
    const q = rawHeaderSearch.toLowerCase()
    return rawHeaderEntries.filter(([k, vals]) => {
      const matchKey = k.toLowerCase().includes(q)
      const matchVal = (Array.isArray(vals) ? vals : [vals]).some(
        (v) => String(v).toLowerCase().includes(q)
      )
      return matchKey || matchVal
    })
  }, [rawHeaderEntries, rawHeaderSearch])

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-[#E4E7EC] shadow-xs">
        <div className="relative flex-1 max-w-md">
          <Search className="h-4 w-4 text-[#98A2B3] absolute left-3 top-2.5" />
          <input
            id="raw-headers-search-input"
            type="text"
            placeholder="Search headers by name or value..."
            value={rawHeaderSearch}
            onChange={(e) => setRawHeaderSearch(e.target.value)}
            className="w-full bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg pl-9 pr-3 py-1.5 text-xs text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB]"
          />
        </div>
        <span className="text-xs text-[#667085]">
          Showing {filteredRawHeaders.length} of {rawHeaderEntries.length} headers
        </span>
      </div>

      <div className="space-y-3 max-h-[600px] overflow-y-auto">
        {filteredRawHeaders.length === 0 ? (
          <div className="p-8 text-center text-xs text-[#667085] bg-white rounded-xl border border-[#E4E7EC] shadow-xs">
            No headers match your search.
          </div>
        ) : (
          filteredRawHeaders.map(([headerName, headerValues], idx) => {
            const valsArray = Array.isArray(headerValues)
              ? headerValues
              : [headerValues]
            return (
              <div
                key={idx}
                className="p-4 rounded-xl bg-white border border-[#E4E7EC] space-y-2 shadow-xs"
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-bold text-[#2563EB]">
                    {headerName}
                  </span>
                  {valsArray.length > 1 && (
                    <span className="text-[11px] px-2 py-0.5 rounded bg-[#F5F6F8] text-[#667085] border border-[#E4E7EC]">
                      {valsArray.length} occurrences
                    </span>
                  )}
                </div>
                <div className="space-y-1.5 pt-1">
                  {valsArray.map((val, vIdx) => (
                    <div
                      key={vIdx}
                      className="p-2.5 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] font-mono text-[11px] text-[#202938] whitespace-pre-wrap break-all leading-relaxed select-text"
                    >
                      {String(val)}
                    </div>
                  ))}
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
