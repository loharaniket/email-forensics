import React, { useState } from 'react'
import {
  Server,
  Clock,
  ChevronDown,
  ChevronRight
} from 'lucide-react'

export default function MailRoute({ hops, formatSeconds }) {
  const [expandedHops, setExpandedHops] = useState({})

  const toggleHopExpand = (order) => {
    setExpandedHops((prev) => ({ ...prev, [order]: !prev[order] }))
  }

  const totalTransitDelaySeconds = (hops || []).reduce(
    (acc, h) => acc + (h.delaySeconds || 0),
    0
  )

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-5 rounded-xl bg-white border border-[#E4E7EC]">
          <span className="text-xs text-[#202938] block">Total mail route hops</span>
          <span className="text-2xl font-bold text-[#202938] mt-1 block font-mono">
            {hops?.length || 0}
          </span>
          <span className="text-xs text-[#202938] mt-1 block">Servers traversed</span>
        </div>

        <div className="p-5 rounded-xl bg-white border border-[#E4E7EC]">
          <span className="text-xs text-[#202938] block">Total transit delay</span>
          <span className="text-2xl font-bold text-[#202938] mt-1 block font-mono">
            {totalTransitDelaySeconds > 0
              ? formatSeconds(totalTransitDelaySeconds)
              : '0s (Immediate)'}
          </span>
          <span className="text-xs text-[#202938] mt-1 block">Accumulated delay</span>
        </div>

        <div className="p-5 rounded-xl bg-white border border-[#E4E7EC]">
          <span className="text-xs text-[#202938] block">Originating server</span>
          <span className="text-sm font-semibold text-[#202938] mt-2 block truncate font-mono">
            {hops && hops.length > 0
              ? hops[0].fromHost || hops[0].hopIp || 'Unknown'
              : 'N/A'}
          </span>
          <span className="text-xs text-[#202938] mt-1 block">First hop</span>
        </div>
      </div>

      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-6">
        <div>
          <h2 className="text-sm font-bold text-[#202938]">
            Mail route
          </h2>
        </div>

        {!hops || hops.length === 0 ? (
          <div className="p-8 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] text-center text-xs text-[#202938]">
            No Received headers were found in this email message.
          </div>
        ) : (
          <div className="relative pl-6 sm:pl-8 space-y-6 before:content-[''] before:absolute before:left-3 sm:before:left-4 before:top-4 before:bottom-4 before:w-0.5 before:bg-[#E4E7EC]">
            {hops.map((hop, idx) => {
              const isFirst = idx === 0
              const isLast = idx === hops.length - 1
              const isExpanded = !!expandedHops[hop.hopOrder]

              return (
                <div key={hop.hopOrder || idx} className="relative group">
                  <div
                    className={`absolute -left-6 sm:-left-8 top-1.5 h-6 w-6 rounded-full border flex items-center justify-center text-xs font-mono font-bold ${
                      isFirst
                        ? 'bg-[#EFF6FF] border-blue-300 text-[#2563EB]'
                        : isLast
                        ? 'bg-[#F0FDF4] border-green-300 text-[#15803D]'
                        : 'bg-white border-[#E4E7EC] text-[#202938]'
                    }`}
                  >
                    {hop.hopOrder || idx + 1}
                  </div>

                  <div className="bg-[#F5F6F8] border border-[#E4E7EC] rounded-xl p-4 md:p-5 space-y-3">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2 border-b border-[#E4E7EC]">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold text-[#202938]">
                          Hop {hop.hopOrder || idx + 1}
                        </span>
                        {isFirst && (
                          <span className="text-[11px] font-medium bg-[#EFF6FF] text-[#2563EB] px-2 py-0.5 rounded border border-blue-100">
                            Originating hop
                          </span>
                        )}
                        {isLast && (
                          <span className="text-[11px] font-medium bg-[#F0FDF4] text-[#15803D] px-2 py-0.5 rounded border border-green-200">
                            Final destination
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-2 text-xs text-[#202938]">
                        <Clock className="h-3.5 w-3.5 text-[#202938]" />
                        <span>
                          {(() => {
                            const tVal = hop.hopTimestamp || hop.timestamp
                            if (!tVal) return '(No timestamp)'
                            try {
                              const d = new Date(tVal)
                              return !isNaN(d.getTime()) ? d.toLocaleString() : String(tVal)
                            } catch {
                              return String(tVal)
                            }
                          })()}
                        </span>
                        {hop.delaySeconds != null && hop.delaySeconds > 0 && (
                          <span className="font-mono text-[#B45309] bg-[#FFFBEB] px-1.5 py-0.5 rounded border border-amber-200 text-[11px]">
                            +{formatSeconds(hop.delaySeconds)} delay
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3 text-xs">
                      <div>
                        <span className="text-[#202938] block">Sending server:</span>
                        <span className="font-mono text-[#202938] font-semibold break-all">
                          {hop.fromHost || 'Unknown'}
                        </span>
                      </div>

                      <div>
                        <span className="text-[#202938] block">Receiving server:</span>
                        <span className="font-mono text-[#202938] font-semibold break-all">
                          {hop.byHost || 'Unknown'}
                        </span>
                      </div>

                      <div>
                        <span className="text-[#202938] block">IP address:</span>
                        <span className="font-mono text-[#2563EB] font-semibold break-all">
                          {hop.hopIp || '(No IP in header)'}
                        </span>
                      </div>

                      {hop.protocol && (
                        <div>
                          <span className="text-[#202938] block">Protocol / Transport:</span>
                          <span className="font-mono text-[#202938]">
                            {hop.protocol}
                          </span>
                        </div>
                      )}

                      {hop.tlsCipher && (
                        <div className="sm:col-span-2">
                          <span className="text-[#202938] block">TLS encryption:</span>
                          <span className="font-mono text-xs text-[#15803D]">
                            {hop.tlsCipher}
                          </span>
                        </div>
                      )}
                    </div>

                    {(hop.rawHopLine || hop.rawHeader) && (
                      <div className="pt-2 border-t border-[#E4E7EC]">
                        <button
                          onClick={() => toggleHopExpand(hop.hopOrder)}
                          className="flex items-center gap-1.5 text-xs text-[#202938] cursor-pointer"
                        >
                          {isExpanded ? (
                            <ChevronDown className="h-3.5 w-3.5" />
                          ) : (
                            <ChevronRight className="h-3.5 w-3.5" />
                          )}
                          <span>{isExpanded ? 'Hide raw Received header' : 'Show raw Received header'}</span>
                        </button>

                        {isExpanded && (
                          <div className="mt-2 p-3 rounded-lg bg-white border border-[#E4E7EC] font-mono text-[11px] text-[#202938] break-all whitespace-pre-wrap leading-relaxed select-all">
                            {hop.rawHopLine || hop.rawHeader}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}

