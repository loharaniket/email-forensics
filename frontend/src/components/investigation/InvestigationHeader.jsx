import React from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowLeft,
  Printer,
  FileDown,
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
  onExportJson,
  isExportingJson,
  onDownloadOriginal,
  caseId,
  formatBytes
}) {
  if (!detail) return null

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <Link
          to={`/cases/${caseId}`}
          className="flex items-center gap-1.5 text-xs text-[#667085] hover:text-[#202938] transition-colors cursor-pointer w-fit"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          <span>Back to case ({detail.caseNumber})</span>
        </Link>

        <div className="flex flex-wrap items-center gap-2" id="report-action-bar">
          <button
            id="btn-print-report"
            onClick={onPrint}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer shadow-xs"
          >
            <Printer className="h-3.5 w-3.5 text-[#2563EB]" />
            <span>Print / PDF report</span>
          </button>

          <button
            id="btn-export-report-json"
            onClick={onExportJson}
            disabled={isExportingJson}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer disabled:opacity-50 shadow-xs"
          >
            <FileDown className="h-3.5 w-3.5 text-[#15803D]" />
            <span>{isExportingJson ? 'Exporting...' : 'Export JSON report'}</span>
          </button>

          <button
            id="download-original-btn"
            onClick={onDownloadOriginal}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer shadow-xs"
          >
            <Download className="h-3.5 w-3.5 text-[#667085]" />
            <span>Download original .eml</span>
          </button>
        </div>
      </div>

      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 shadow-xs space-y-5">
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-4 pb-5 border-b border-[#E4E7EC]">
          <div className="space-y-2 flex-1">
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <span className="text-[#2563EB] font-mono font-semibold bg-[#EFF6FF] px-2.5 py-0.5 rounded border border-blue-100">
                {detail.caseNumber}
              </span>
              <span className="text-[#98A2B3]">•</span>
              <span className="text-[#667085] font-medium">{detail.caseTitle}</span>
              <span className="text-[#98A2B3]">•</span>
              <span className="text-[#15803D] text-[11px] bg-[#F0FDF4] px-2 py-0.5 rounded border border-green-200 font-medium">
                Analysis complete
              </span>
            </div>

            <h1 className="text-xl md:text-2xl font-bold tracking-tight text-[#202938] break-words">
              {parsed?.subject || '(No subject)'}
            </h1>

            <div className="flex flex-wrap items-center gap-4 text-xs text-[#667085] pt-1">
              <div className="flex items-center gap-1.5">
                <FileText className="h-3.5 w-3.5 text-[#98A2B3]" />
                <span>
                  File: <strong className="text-[#202938] font-medium">{detail.originalFilename}</strong>
                </span>
                <span>({formatBytes(detail.fileSizeBytes)})</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5 text-[#98A2B3]" />
                <span>Uploaded: {new Date(detail.uploadedAt).toLocaleString()}</span>
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:items-end gap-2 shrink-0">
            <div className="flex items-center gap-2 bg-[#F5F6F8] px-3 py-1.5 rounded-lg border border-[#E4E7EC] text-xs">
              <Hash className="h-3.5 w-3.5 text-[#2563EB]" />
              <span className="text-[#667085]">SHA-256:</span>
              <span className="font-mono text-[#202938] select-all max-w-[140px] md:max-w-[200px] truncate text-[11px]">
                {detail.sha256Hash}
              </span>
              <button
                onClick={() => copyValue('sha256', detail.sha256Hash)}
                className="p-1 hover:bg-white rounded text-[#667085] hover:text-[#202938] transition-colors cursor-pointer"
                title="Copy SHA-256 Hash"
              >
                {copiedKey === 'sha256' ? (
                  <Check className="h-3.5 w-3.5 text-[#15803D]" />
                ) : (
                  <Copy className="h-3.5 w-3.5" />
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Quick details grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
          <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
            <span className="text-xs text-[#667085] block mb-1">Message date</span>
            <span className="text-xs font-medium text-[#202938] block truncate">
              {parsed?.messageDate ? new Date(parsed.messageDate).toLocaleDateString() : '—'}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
            <span className="text-xs text-[#667085] block mb-1">From address</span>
            <span className="text-xs font-medium text-[#202938] block truncate font-mono text-[11px]">
              {parsed?.from || '—'}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
            <span className="text-xs text-[#667085] block mb-1">Return-Path</span>
            <span className="text-xs font-medium text-[#202938] block truncate font-mono text-[11px]">
              {parsed?.returnPath || '—'}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
            <span className="text-xs text-[#667085] block mb-1">Reply-To</span>
            <span className="text-xs font-medium text-[#202938] block truncate font-mono text-[11px]">
              {parsed?.replyTo || '—'}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
            <span className="text-xs text-[#667085] block mb-1">Message-ID</span>
            <span className="text-xs font-medium text-[#202938] block truncate font-mono text-[11px]" title={parsed?.messageId}>
              {parsed?.messageId || '—'}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC]">
            <span className="text-xs text-[#667085] block mb-1">Sender IP</span>
            <span className="text-xs font-medium text-[#202938] block truncate font-mono text-[11px]">
              {parsed?.originatingIp || '—'}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

