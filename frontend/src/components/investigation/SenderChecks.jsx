import React from 'react'
import {
  Info,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  HelpCircle
} from 'lucide-react'

export default function SenderChecks({ headerAnalysis, parsed }) {
  const getAuthBadge = (val) => {
    if (!val) {
      return {
        bg: 'bg-slate-100',
        text: 'text-[#202938]',
        border: 'border-[#E4E7EC]',
        label: 'None',
        icon: HelpCircle
      }
    }
    const norm = String(val).toLowerCase()
    if (norm === 'pass' || norm === 'selector_found') {
      return {
        bg: 'bg-[#F0FDF4]',
        text: 'text-[#15803D]',
        border: 'border-green-200',
        label: norm === 'selector_found' ? 'Key found' : 'Pass',
        icon: CheckCircle2
      }
    }
    if (norm === 'fail' || norm === 'hardfail' || norm === 'reject') {
      return {
        bg: 'bg-[#FEF2F2]',
        text: 'text-[#B42318]',
        border: 'border-red-200',
        label: 'Fail',
        icon: XCircle
      }
    }
    if (norm === 'softfail' || norm === 'quarantine') {
      return {
        bg: 'bg-[#FFFBEB]',
        text: 'text-[#B45309]',
        border: 'border-amber-200',
        label: norm === 'quarantine' ? 'Quarantine' : 'Softfail',
        icon: AlertTriangle
      }
    }
    return {
      bg: 'bg-slate-100',
      text: 'text-[#202938]',
      border: 'border-[#E4E7EC]',
      label: val,
      icon: Info
    }
  }

  return (
    <div className="space-y-6">
      {/* Sender authentication overview */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 className="text-sm font-bold text-[#202938]">
              Email authentication analysis
            </h2>
            <p className="text-xs text-[#202938] mt-0.5">
              Verified via {headerAnalysis?.authApiProvider || 'external API'} with automatic fallback.
            </p>
          </div>
          {headerAnalysis?.authApiProvider && (
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#F0FDF4] text-[#15803D] border border-green-200 w-fit">
              {headerAnalysis.authApiProvider}
            </span>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {['SPF', 'DKIM', 'DMARC', 'ARC'].map((mechanism) => {
            const val =
              mechanism === 'SPF'
                ? headerAnalysis?.reportedSpf || headerAnalysis?.spfResult
                : mechanism === 'DKIM'
                ? headerAnalysis?.reportedDkim || headerAnalysis?.dkimResult
                : mechanism === 'DMARC'
                ? headerAnalysis?.reportedDmarc || headerAnalysis?.dmarcResult
                : headerAnalysis?.reportedArc || headerAnalysis?.arcResult
            const badge = getAuthBadge(val)
            const IconComp = badge.icon

            return (
              <div
                key={mechanism}
                className={`p-4 rounded-xl border ${badge.border} ${badge.bg} space-y-1.5`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs text-[#202938] font-semibold">{mechanism}</span>
                  <IconComp className={`h-4 w-4 ${badge.text}`} />
                </div>
                <span className={`text-base font-bold block ${badge.text}`}>
                  {badge.label}
                </span>
              </div>
            )
          })}
        </div>
      </div>

      {/* Sender and recipient headers */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-4">
        <h2 className="text-sm font-bold text-[#202938] pb-3 border-b border-[#E4E7EC]">
          Sender and routing addresses
        </h2>

        <div className="divide-y divide-[#E4E7EC] text-xs">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#202938] font-medium">From address</span>
            <span className="md:col-span-2 font-mono text-[#202938] font-semibold break-all">
              {headerAnalysis?.fromAddress || parsed?.from || '(None)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#202938] font-medium">From domain</span>
            <span className="md:col-span-2 font-mono text-[#2563EB] break-all">
              {headerAnalysis?.fromDomain ? `@${headerAnalysis.fromDomain}` : '(None)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#202938] font-medium">To</span>
            <span className="md:col-span-2 font-mono text-[#202938] break-all">
              {parsed?.to && parsed.to.length > 0 ? parsed.to.join(', ') : '(None)'}
            </span>
          </div>

          {parsed?.cc && parsed.cc.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
              <span className="text-[#202938] font-medium">CC</span>
              <span className="md:col-span-2 font-mono text-[#202938] break-all">
                {parsed.cc.join(', ')}
              </span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#202938] font-medium">Reply-To</span>
            <span className="md:col-span-2 font-mono text-[#202938] break-all">
              {headerAnalysis?.replyTo ||
                (parsed?.replyTo && parsed.replyTo.length > 0
                  ? parsed.replyTo.join(', ')
                  : '(None)')}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#202938] font-medium">Return-Path</span>
            <span className="md:col-span-2 font-mono text-[#202938] break-all">
              {headerAnalysis?.returnPath || parsed?.returnPath || '(None)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#202938] font-medium">DKIM signing domain</span>
            <span className="md:col-span-2 font-mono text-[#2563EB] break-all">
              {headerAnalysis?.dkimDomain ? `@${headerAnalysis.dkimDomain}` : '(No DKIM signature)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#202938] font-medium">Message-ID</span>
            <span className="md:col-span-2 font-mono text-[#202938] break-all">
              {parsed?.messageId || '(None)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#202938] font-medium">Sent date</span>
            <span className="md:col-span-2 text-[#202938]">
              {(() => {
                const dVal = parsed?.sentDate || parsed?.messageDate || parsed?.rawDate
                if (!dVal) return '(None)'
                try {
                  const d = new Date(dVal)
                  return !isNaN(d.getTime()) ? d.toUTCString() : String(dVal)
                } catch {
                  return String(dVal)
                }
              })()}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

