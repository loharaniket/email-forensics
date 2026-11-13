import React from 'react'
import Navbar from '../components/Navbar'
import EvidenceUploadSection from '../components/EvidenceUploadSection'
import { Shield, Sparkles, Network, FileCheck } from 'lucide-react'

export default function DashboardPage() {
  return (
    <div className="h-screen bg-[#F8FAFC] flex flex-col overflow-hidden">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-5 flex flex-col gap-3 overflow-hidden">
        {/* Compact Minimal Header Banner */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-3 sm:p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shrink-0">
          <div>
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-[#EFF6FF] text-[#2563EB] text-[11px] font-semibold border border-blue-100">
                <Shield className="h-3 w-3" />
                <span>Email Forensics Pipeline</span>
              </span>
              <span className="text-[11px] font-mono bg-slate-100 px-1.5 py-0.5 rounded text-[#202938] border border-[#E4E7EC]">
                RFC 5322 .eml
              </span>
            </div>
            <h1 className="text-base sm:text-lg font-bold text-[#202938] mt-1">
              Email Forensics &amp; Threat Investigation
            </h1>
          </div>

          <div className="flex flex-wrap items-center gap-2 text-xs text-[#202938]">
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-[#F8FAFC] border border-[#E4E7EC] rounded-md">
              <FileCheck className="h-3.5 w-3.5 text-[#2563EB]" />
              <span>SHA-256 Verified</span>
            </div>
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-[#F8FAFC] border border-[#E4E7EC] rounded-md">
              <Network className="h-3.5 w-3.5 text-[#15803D]" />
              <span>Route &amp; URLs</span>
            </div>
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-[#F8FAFC] border border-[#E4E7EC] rounded-md">
              <Sparkles className="h-3.5 w-3.5 text-[#7C3AED]" />
              <span>AI Grounded</span>
            </div>
          </div>
        </div>

        {/* Side-by-Side Split Workspace */}
        <div className="flex-1 min-h-0 overflow-hidden">
          <EvidenceUploadSection />
        </div>
      </main>
    </div>
  )
}
