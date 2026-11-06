import React from 'react'
import {
  ShieldCheck,
  ShieldAlert,
  Globe,
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
        text: 'text-[#667085]',
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
      text: 'text-[#667085]',
      border: 'border-[#E4E7EC]',
      label: val,
      icon: Info
    }
  }

  return (
    <div className="space-y-6">
      {/* Sender authentication overview */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 shadow-xs space-y-5">
        <div>
          <h2 className="text-sm font-bold text-[#202938]">
            Reported email authentication
          </h2>
          <p className="text-xs text-[#667085] mt-0.5">
            Results reported by the receiving mail server in the Authentication-Results header.
          </p>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {['SPF', 'DKIM', 'DMARC', 'ARC'].map((mechanism) => {
            const val =
              mechanism === 'SPF'
                ? headerAnalysis?.reportedSpf
                : mechanism === 'DKIM'
                ? headerAnalysis?.reportedDkim
                : mechanism === 'DMARC'
                ? headerAnalysis?.reportedDmarc
                : headerAnalysis?.reportedArc
            const badge = getAuthBadge(val)
            const IconComp = badge.icon

            return (
              <div
                key={mechanism}
                className={`p-4 rounded-xl border ${badge.border} ${badge.bg} space-y-1.5`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs text-[#667085] font-semibold">{mechanism}</span>
                  <IconComp className={`h-4 w-4 ${badge.text}`} />
                </div>
                <span className={`text-base font-bold block ${badge.text}`}>
                  {badge.label}
                </span>
              </div>
            )
          })}
        </div>

        {headerAnalysis?.verificationNote && (
          <div className="p-3.5 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] text-xs text-[#667085] flex items-start gap-2.5">
            <Info className="h-4 w-4 text-[#2563EB] shrink-0 mt-0.5" />
            <span>{headerAnalysis.verificationNote}</span>
          </div>
        )}
      </div>

      {/* Active DNS verification */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 shadow-xs space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 className="text-sm font-bold text-[#202938]">
              DNS sender verification
            </h2>
            <p className="text-xs text-[#667085] mt-0.5">
              Live checks performed against the sender domain's DNS records.
            </p>
          </div>
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#F0FDF4] text-[#15803D] border border-green-200 w-fit">
            <span className="h-1.5 w-1.5 rounded-full bg-[#15803D]"></span>
            Live verified
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {(() => {
            const spfBadge = getAuthBadge(headerAnalysis?.activeSpfResult)
            const SpfIcon = spfBadge.icon
            return (
              <div className={`p-4 rounded-xl border ${spfBadge.border} ${spfBadge.bg} space-y-2`}>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-[#667085]">SPF record check</span>
                  <SpfIcon className={`h-4 w-4 ${spfBadge.text}`} />
                </div>
                <span className={`text-base font-bold block ${spfBadge.text}`}>
                  {spfBadge.label}
                </span>
                <div className="pt-2 text-xs text-[#667085] space-y-1 border-t border-slate-200/60">
                  <div>Relay IP: <span className="font-mono text-[#202938]">{headerAnalysis?.activeSpfRelayIp || '(Not found)'}</span></div>
                  {headerAnalysis?.activeSpfRecord && (
                    <div className="truncate text-[11px] font-mono text-[#667085]" title={headerAnalysis.activeSpfRecord}>
                      Record: {headerAnalysis.activeSpfRecord}
                    </div>
                  )}
                </div>
              </div>
            )
          })()}

          {(() => {
            const dkimBadge = getAuthBadge(headerAnalysis?.activeDkimStatus)
            const DkimIcon = dkimBadge.icon
            return (
              <div className={`p-4 rounded-xl border ${dkimBadge.border} ${dkimBadge.bg} space-y-2`}>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-[#667085]">DKIM public key</span>
                  <DkimIcon className={`h-4 w-4 ${dkimBadge.text}`} />
                </div>
                <span className={`text-base font-bold block ${dkimBadge.text}`}>
                  {dkimBadge.label}
                </span>
                <div className="pt-2 text-xs text-[#667085] space-y-1 border-t border-slate-200/60">
                  <div>Selector: <span className="font-mono text-[#202938]">{headerAnalysis?.activeDkimSelector || 'N/A'}</span></div>
                  {headerAnalysis?.activeDkimRecord && (
                    <div className="truncate text-[11px] font-mono text-[#667085]" title={headerAnalysis.activeDkimRecord}>
                      Key: {headerAnalysis.activeDkimRecord}
                    </div>
                  )}
                </div>
              </div>
            )
          })()}

          {(() => {
            const dmarcBadge = getAuthBadge(headerAnalysis?.activeDmarcStatus)
            const DmarcIcon = dmarcBadge.icon
            return (
              <div className={`p-4 rounded-xl border ${dmarcBadge.border} ${dmarcBadge.bg} space-y-2`}>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-[#667085]">DMARC policy</span>
                  <DmarcIcon className={`h-4 w-4 ${dmarcBadge.text}`} />
                </div>
                <span className={`text-base font-bold block ${dmarcBadge.text}`}>
                  {dmarcBadge.label}
                </span>
                <div className="pt-2 text-xs text-[#667085] space-y-1 border-t border-slate-200/60">
                  <div>Host: <span className="font-mono text-[#202938]">{headerAnalysis?.fromDomain ? `_dmarc.${headerAnalysis.fromDomain}` : 'N/A'}</span></div>
                  {headerAnalysis?.activeDmarcPolicy && (
                    <div className="truncate text-[11px] font-mono text-[#667085]" title={headerAnalysis.activeDmarcPolicy}>
                      Policy: {headerAnalysis.activeDmarcPolicy}
                    </div>
                  )}
                </div>
              </div>
            )
          })()}
        </div>

        {headerAnalysis?.activeVerificationSummary && (
          <div className="p-3.5 rounded-lg bg-[#EFF6FF] border border-blue-100 text-xs text-[#2563EB] flex items-start gap-2">
            <Info className="h-4 w-4 shrink-0 mt-0.5" />
            <span>{headerAnalysis.activeVerificationSummary}</span>
          </div>
        )}
      </div>

      {/* Sender and recipient headers */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 shadow-xs space-y-4">
        <h2 className="text-sm font-bold text-[#202938] pb-3 border-b border-[#E4E7EC]">
          Sender and routing addresses
        </h2>

        <div className="divide-y divide-[#E4E7EC] text-xs">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#667085] font-medium">From address</span>
            <span className="md:col-span-2 font-mono text-[#202938] font-semibold break-all">
              {headerAnalysis?.fromAddress || parsed?.from || '(None)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#667085] font-medium">From domain</span>
            <span className="md:col-span-2 font-mono text-[#2563EB] break-all">
              {headerAnalysis?.fromDomain ? `@${headerAnalysis.fromDomain}` : '(None)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#667085] font-medium">To</span>
            <span className="md:col-span-2 font-mono text-[#202938] break-all">
              {parsed?.to && parsed.to.length > 0 ? parsed.to.join(', ') : '(None)'}
            </span>
          </div>

          {parsed?.cc && parsed.cc.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
              <span className="text-[#667085] font-medium">CC</span>
              <span className="md:col-span-2 font-mono text-[#202938] break-all">
                {parsed.cc.join(', ')}
              </span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#667085] font-medium">Reply-To</span>
            <span className="md:col-span-2 font-mono text-[#202938] break-all">
              {headerAnalysis?.replyTo ||
                (parsed?.replyTo && parsed.replyTo.length > 0
                  ? parsed.replyTo.join(', ')
                  : '(None)')}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#667085] font-medium">Return-Path</span>
            <span className="md:col-span-2 font-mono text-[#202938] break-all">
              {headerAnalysis?.returnPath || parsed?.returnPath || '(None)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#667085] font-medium">DKIM signing domain</span>
            <span className="md:col-span-2 font-mono text-[#2563EB] break-all">
              {headerAnalysis?.dkimDomain ? `@${headerAnalysis.dkimDomain}` : '(No DKIM signature)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#667085] font-medium">Message-ID</span>
            <span className="md:col-span-2 font-mono text-[#202938] break-all">
              {parsed?.messageId || '(None)'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 py-2.5">
            <span className="text-[#667085] font-medium">Sent date</span>
            <span className="md:col-span-2 text-[#202938]">
              {parsed?.sentDate ? new Date(parsed.sentDate).toUTCString() : '(None)'}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

