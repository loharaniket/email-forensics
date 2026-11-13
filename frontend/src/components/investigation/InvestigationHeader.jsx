import React from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowLeft,
  Printer,
  Download,
  Hash,
  Copy,
  Check,
  Clock,
  FileText
} from 'lucide-react'

export default function InvestigationHeader({
  detail,
  parsed,
  copiedKey,
  copyValue,
  onPrint,
  onDownloadOriginal,
  caseId,
  formatBytes
}) {
  if (!detail) return null

  return (
    <header className="h-12 bg-white border-b border-[#E4E7EC] px-4 sm:px-6 flex items-center justify-between gap-3 shrink-0 select-none z-10">
      {/* Left: Back Link + Email Subject & Filename */}
      <div className="flex items-center gap-3 min-w-0">
        <Link
          to="/"
          id="back-to-dashboard-btn"
          className="flex items-center gap-1.5 text-xs text-[#202938] hover:text-[#2563EB] transition-colors cursor-pointer shrink-0 font-medium"
          title="Back to forensic dashboard"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Dashboard</span>
        </Link>

        <span className="h-4 w-px bg-[#E4E7EC] shrink-0" />

        <div className="flex items-center gap-2 min-w-0">
          <h1
            className="text-[#2563EB] font-mono text-xs sm:text-sm font-semibold bg-[#EFF6FF] px-2.5 py-1 rounded border border-blue-100 truncate max-w-[240px] sm:max-w-[380px] md:max-w-[500px]"
            title={detail.originalFilename}
          >
            {detail.originalFilename || 'Email Evidence'}
          </h1>

          <span className="hidden sm:inline-flex text-[#15803D] text-[10px] bg-[#F0FDF4] px-1.5 py-0.5 rounded border border-green-200 font-semibold shrink-0">
            Complete
          </span>
        </div>
      </div>

      {/* Right: SHA-256 Hash Box + Action Buttons */}
      <div className="flex items-center gap-2 shrink-0" id="report-action-bar">
        {/* SHA-256 Copy Pill */}
        <div className="hidden xl:flex items-center gap-1.5 bg-[#F5F6F8] px-2.5 py-1 rounded-md border border-[#E4E7EC] text-xs">
          <Hash className="h-3 w-3 text-[#2563EB]" />
          <span className="text-[#202938] text-[11px]">SHA-256:</span>
          <span
            className="font-mono text-[#202938] select-all max-w-[120px] 2xl:max-w-[180px] truncate text-[11px]"
            title={detail.sha256Hash}
          >
            {detail.sha256Hash}
          </span>
          <button
            onClick={() => copyValue('sha256', detail.sha256Hash)}
            className="p-0.5 hover:bg-white rounded text-[#202938] transition-colors cursor-pointer"
            title="Copy SHA-256 Hash"
          >
            {copiedKey === 'sha256' ? (
              <Check className="h-3 w-3 text-[#15803D]" />
            ) : (
              <Copy className="h-3 w-3" />
            )}
          </button>
        </div>

        {/* Action Button: Print Report */}
        <button
          id="btn-print-report"
          onClick={onPrint}
          className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-md text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer"
          title="Print or export PDF report"
        >
          <Printer className="h-3.5 w-3.5 text-[#2563EB]" />
          <span className="hidden sm:inline">PDF Report</span>
        </button>

        {/* Action Button: Download .eml */}
        <button
          id="download-original-btn"
          onClick={onDownloadOriginal}
          className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-md text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer"
          title="Download original .eml file"
        >
          <Download className="h-3.5 w-3.5 text-[#202938]" />
          <span className="hidden md:inline">Download .eml</span>
        </button>
      </div>
    </header>
  )
}
