import React, { useState, useMemo } from 'react'
import {
  Search,
  Copy,
  Check,
  AlertTriangle
} from 'lucide-react'

export default function LinksAndIps({
  mode = 'urls', // 'urls' or 'infrastructure'
  urls = [],
  infrastructure = [],
  copiedKey,
  copyValue
}) {
  const [urlSearch, setUrlSearch] = useState('')
  const [infraSearch, setInfraSearch] = useState('')

  const filteredUrls = useMemo(() => {
    if (!urlSearch.trim()) return urls
    const q = urlSearch.toLowerCase()
    return urls.filter(
      (u) =>
        (u.domain && u.domain.toLowerCase().includes(q)) ||
        (u.rawUrl && u.rawUrl.toLowerCase().includes(q)) ||
        (u.normalizedUrl && u.normalizedUrl.toLowerCase().includes(q)) ||
        (u.anchorText && u.anchorText.toLowerCase().includes(q)) ||
        (u.sourceType && u.sourceType.toLowerCase().includes(q))
    )
  }, [urls, urlSearch])

  const filteredInfra = useMemo(() => {
    if (!infraSearch.trim()) return infrastructure
    const q = infraSearch.toLowerCase()
    return infrastructure.filter(
      (item) =>
        (item.ipAddress && item.ipAddress.toLowerCase().includes(q)) ||
        (item.sourceContext && item.sourceContext.toLowerCase().includes(q))
    )
  }, [infrastructure, infraSearch])

  if (mode === 'urls') {
    return (
      <div className="space-y-4">
        {/* URL search bar */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-[#E4E7EC]">
          <div className="relative flex-1 max-w-md">
            <Search className="h-4 w-4 text-[#98A2B3] absolute left-3 top-2.5" />
            <input
              id="url-search-input"
              type="text"
              placeholder="Search links by domain or text..."
              value={urlSearch}
              onChange={(e) => setUrlSearch(e.target.value)}
              className="w-full bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg pl-9 pr-3.5 py-2 text-xs text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB]"
            />
          </div>
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="text-[#667085]">
              Total links: <strong className="text-[#202938]">{urls.length}</strong>
            </span>
            {urls.filter((u) => u.anchorMismatch).length > 0 && (
              <span className="text-[#B42318] bg-[#FEF2F2] px-2 py-0.5 rounded border border-red-200 font-medium">
                {urls.filter((u) => u.anchorMismatch).length} text mismatch
              </span>
            )}
            {urls.filter((u) => (u.urlRiskScore || 0) >= 70).length > 0 && (
              <span className="text-[#B42318] bg-[#FEF2F2] px-2 py-0.5 rounded border border-red-200 font-medium">
                {urls.filter((u) => (u.urlRiskScore || 0) >= 70).length} high risk
              </span>
            )}
          </div>
        </div>

        {filteredUrls.length === 0 ? (
          <div className="p-8 rounded-xl bg-white border border-[#E4E7EC] text-center text-xs text-[#667085]">
            {urls.length === 0
              ? 'No links were found in this email.'
              : 'No links match your search.'}
          </div>
        ) : (
          <div className="space-y-3">
            {filteredUrls.map((u, idx) => {
              const isHighRisk = (u.urlRiskScore || 0) >= 70
              const isMedRisk = (u.urlRiskScore || 0) >= 40 && (u.urlRiskScore || 0) < 70
              const riskBadgeClass = isHighRisk
                ? 'bg-[#FEF2F2] text-[#B42318] border-red-200'
                : isMedRisk
                ? 'bg-[#FFFBEB] text-[#B45309] border-amber-200'
                : 'bg-[#F0FDF4] text-[#15803D] border-green-200'

              return (
                <div
                  key={idx}
                  className={`p-5 rounded-xl bg-white border transition-colors ${
                    u.anchorMismatch || isHighRisk
                      ? 'border-red-200'
                      : 'border-[#E4E7EC]'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 pb-3 border-b border-[#E4E7EC]">
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-bold font-mono text-[#202938]">
                          {u.domain || 'Unknown domain'}
                        </span>
                        <span className="px-2 py-0.5 rounded text-[11px] bg-[#F5F6F8] text-[#667085] border border-[#E4E7EC]">
                          {u.sourceType || 'Body link'}
                        </span>
                        {u.dnsStatus && (
                          <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-slate-100 text-[#667085] border border-[#E4E7EC]">
                            DNS: {u.dnsStatus}
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <span
                        className={`px-2.5 py-1 rounded-lg text-xs font-mono font-bold border ${riskBadgeClass}`}
                      >
                        Risk score: {u.urlRiskScore ?? 0}/100
                      </span>
                    </div>
                  </div>

                  {u.anchorMismatch && (
                    <div className="mt-3 p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#B42318] flex items-start gap-2.5">
                      <AlertTriangle className="h-4 w-4 text-[#B42318] shrink-0 mt-0.5" />
                      <div className="space-y-0.5">
                        <span className="font-bold block">
                          Link text mismatch detected
                        </span>
                        <p className="text-[11px] leading-relaxed">
                          The displayed text appears to be{' '}
                          <strong className="underline font-semibold">{u.anchorText}</strong>, but
                          the actual destination leads to a different domain (
                          <strong className="font-semibold">{u.domain}</strong>).
                        </p>
                      </div>
                    </div>
                  )}

                  <div className="flex flex-wrap items-center gap-1.5 pt-3">
                    {u.isIpUrl && (
                      <span className="px-2 py-0.5 rounded text-[11px] bg-[#FEF2F2] text-[#B42318] border border-red-200">
                        IP address in URL
                      </span>
                    )}
                    {u.isShortened && (
                      <span className="px-2 py-0.5 rounded text-[11px] bg-[#FFFBEB] text-[#B45309] border border-amber-200">
                        Shortened URL
                      </span>
                    )}
                    {u.unusualPort && (
                      <span className="px-2 py-0.5 rounded text-[11px] bg-[#FFFBEB] text-[#B45309] border border-amber-200">
                        Unusual port number
                      </span>
                    )}
                    {u.subdomainExcess && (
                      <span className="px-2 py-0.5 rounded text-[11px] bg-[#FFFBEB] text-[#B45309] border border-amber-200">
                        Many subdomains (&gt;3)
                      </span>
                    )}
                    {u.lookalike && (
                      <span className="px-2 py-0.5 rounded text-[11px] bg-[#FFFBEB] text-[#B45309] border border-amber-200">
                        Lookalike characters
                      </span>
                    )}
                  </div>

                  <div className="mt-3 space-y-2 text-xs">
                    <div className="bg-[#F5F6F8] p-3 rounded-lg border border-[#E4E7EC] flex items-start justify-between gap-3">
                      <div className="break-all flex-1 font-mono text-[11px]">
                        <span className="text-[#667085] block text-[10px] uppercase font-sans font-medium mb-0.5">
                          Safe link address (inert text):
                        </span>
                        <span className="text-[#202938] select-all font-semibold">
                          {u.normalizedUrl}
                        </span>
                      </div>
                      <button
                        onClick={() => copyValue(`norm-${idx}`, u.normalizedUrl)}
                        className="p-1 rounded hover:bg-white text-[#667085] hover:text-[#202938] shrink-0 cursor-pointer"
                        title="Copy normalized URL"
                      >
                        {copiedKey === `norm-${idx}` ? (
                          <Check className="h-3.5 w-3.5 text-[#15803D]" />
                        ) : (
                          <Copy className="h-3.5 w-3.5" />
                        )}
                      </button>
                    </div>

                    {u.rawUrl && u.rawUrl !== u.normalizedUrl && (
                      <div className="bg-[#F5F6F8] p-2.5 rounded-lg border border-[#E4E7EC] flex items-start justify-between gap-3 text-[11px]">
                        <div className="break-all flex-1 font-mono">
                          <span className="text-[#667085] block text-[10px] uppercase font-sans font-medium mb-0.5">
                            Original link address:
                          </span>
                          <span className="text-[#667085] select-all">{u.rawUrl}</span>
                        </div>
                        <button
                          onClick={() => copyValue(`raw-${idx}`, u.rawUrl)}
                          className="p-1 rounded hover:bg-white text-[#667085] hover:text-[#202938] shrink-0 cursor-pointer"
                          title="Copy raw URL"
                        >
                          {copiedKey === `raw-${idx}` ? (
                            <Check className="h-3 w-3 text-[#15803D]" />
                          ) : (
                            <Copy className="h-3 w-3" />
                          )}
                        </button>
                      </div>
                    )}

                    {u.anchorText && (
                      <div className="text-xs text-[#667085]">
                        <span className="font-medium text-[#202938]">Visible text: </span>
                        <span>"{u.anchorText}"</span>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    )
  }

  // Infrastructure / IP addresses mode
  return (
    <div className="space-y-4">
      {/* IP search bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-[#E4E7EC]">
        <div className="relative flex-1 max-w-md">
          <Search className="h-4 w-4 text-[#98A2B3] absolute left-3 top-2.5" />
          <input
            id="infra-search-input"
            type="text"
            placeholder="Search by IP address or context..."
            value={infraSearch}
            onChange={(e) => setInfraSearch(e.target.value)}
            className="w-full bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg pl-9 pr-3.5 py-2 text-xs text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB]"
          />
        </div>
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <span className="text-[#667085]">
            Total IPs: <strong className="text-[#202938]">{infrastructure.length}</strong>
          </span>
          <span className="text-[#2563EB] bg-[#EFF6FF] px-2 py-0.5 rounded border border-blue-100 font-medium">
            {infrastructure.filter((i) => !i.isPrivate).length} Public
          </span>
          <span className="text-[#667085] bg-[#F5F6F8] px-2 py-0.5 rounded border border-[#E4E7EC] font-medium">
            {infrastructure.filter((i) => i.isPrivate).length} Private / Internal
          </span>
        </div>
      </div>

      {filteredInfra.length === 0 ? (
        <div className="p-8 rounded-xl bg-white border border-[#E4E7EC] text-center text-xs text-[#667085]">
          {infrastructure.length === 0
            ? 'No IP addresses found in this email.'
            : 'No IP addresses match your search.'}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredInfra.map((item, idx) => (
            <div
              key={idx}
              className="p-5 rounded-xl bg-white border border-[#E4E7EC] hover:border-slate-300 transition-colors space-y-3"
            >
              <div className="flex items-start justify-between gap-2 pb-2.5 border-b border-[#E4E7EC]">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-sm font-bold text-[#202938] select-all">
                      {item.ipAddress}
                    </span>
                    <button
                      onClick={() => copyValue(`ip-${idx}`, item.ipAddress)}
                      className="p-1 rounded hover:bg-[#F5F6F8] text-[#667085] hover:text-[#202938] shrink-0 cursor-pointer"
                      title="Copy IP Address"
                    >
                      {copiedKey === `ip-${idx}` ? (
                        <Check className="h-3.5 w-3.5 text-[#15803D]" />
                      ) : (
                        <Copy className="h-3.5 w-3.5" />
                      )}
                    </button>
                  </div>
                  <span className="text-[11px] text-[#2563EB] bg-[#EFF6FF] px-2 py-0.5 rounded border border-blue-100 inline-block font-medium">
                    Context: {item.sourceContext}
                  </span>
                </div>

                <div className="flex flex-col items-end gap-1">
                  <span
                    className={`px-2 py-0.5 rounded text-[11px] font-medium border ${
                      item.isPrivate
                        ? 'bg-slate-100 text-[#667085] border-[#E4E7EC]'
                        : 'bg-[#EFF6FF] text-[#2563EB] border-blue-100'
                    }`}
                  >
                    {item.isPrivate ? 'Private (RFC 1918)' : 'Public IP'}
                  </span>
                </div>
              </div>

              <div className="text-xs text-[#667085] space-y-1">
                <div>Source: <span className="font-medium text-[#202938]">{item.sourceContext || 'Email Header'}</span></div>
                {item.reverseDns && (
                  <div>Reverse DNS: <span className="font-mono text-[#202938]">{item.reverseDns}</span></div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

