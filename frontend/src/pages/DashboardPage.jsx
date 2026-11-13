import React from 'react'
import Navbar from '../components/Navbar'
import EvidenceUploadSection from '../components/EvidenceUploadSection'
import { Shield, Sparkles, Network, FileCheck } from 'lucide-react'

export default function DashboardPage() {
  return (
    <div className="min-h-screen bg-[#F8FAFC]">
      <Navbar />

      <main className="max-w-6xl mx-auto px-4 sm:px-6 py-8 space-y-8">
        {/* Hero Section */}
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 sm:p-8 space-y-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="space-y-2 max-w-2xl">
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-[#EFF6FF] text-[#2563EB] text-xs font-medium border border-blue-100">
                <Shield className="h-3.5 w-3.5" />
                <span>Automated Email Forensic Pipeline</span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-[#202938]">
                Email Forensics &amp; Threat Investigation
              </h1>
              <p className="text-sm text-[#202938] leading-relaxed">
                Upload raw RFC 5322 <span className="font-mono text-xs bg-slate-100 px-1.5 py-0.5 rounded text-[#202938]">.eml</span> files
                to automatically analyze authentication headers, server hops, URLs, IP addresses, visual attachments, QR codes,
                behavioral phishing patterns, historical correlations, and AI threat assessments.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 shrink-0">
              <div className="p-3 bg-[#F8FAFC] border border-[#E4E7EC] rounded-lg text-xs space-y-1">
                <div className="flex items-center gap-1.5 font-medium text-[#202938]">
                  <FileCheck className="h-4 w-4 text-[#2563EB]" />
                  <span>Integrity</span>
                </div>
                <p className="text-[#202938] text-[11px]">SHA-256 evidence hashing</p>
              </div>

              <div className="p-3 bg-[#F8FAFC] border border-[#E4E7EC] rounded-lg text-xs space-y-1">
                <div className="flex items-center gap-1.5 font-medium text-[#202938]">
                  <Network className="h-4 w-4 text-[#15803D]" />
                  <span>Network</span>
                </div>
                <p className="text-[#202938] text-[11px]">Hop routing &amp; URL intel</p>
              </div>

              <div className="p-3 bg-[#F8FAFC] border border-[#E4E7EC] rounded-lg text-xs space-y-1">
                <div className="flex items-center gap-1.5 font-medium text-[#202938]">
                  <Sparkles className="h-4 w-4 text-[#7C3AED]" />
                  <span>AI Grounded</span>
                </div>
                <p className="text-[#202938] text-[11px]">Evidence-grounded report</p>
              </div>

              <div className="p-3 bg-[#F8FAFC] border border-[#E4E7EC] rounded-lg text-xs space-y-1">
                <div className="flex items-center gap-1.5 font-medium text-[#202938]">
                  <Shield className="h-4 w-4 text-[#B45309]" />
                  <span>Explainable</span>
                </div>
                <p className="text-[#202938] text-[11px]">Deterministic risk scoring</p>
              </div>
            </div>
          </div>
        </div>

        {/* Upload & Evidence Management */}
        <EvidenceUploadSection />
      </main>
    </div>
  )
}

