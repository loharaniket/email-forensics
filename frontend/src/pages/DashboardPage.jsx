import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import FileUpload from '../components/FileUpload';
import RecentUploads from '../components/RecentUploads';
import { Shield, Sparkles, Network, FileCheck, Database } from 'lucide-react';

export default function DashboardPage() {
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleUploadSuccess = () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  return (
    <div className="min-h-screen bg-[#0f172a] text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 flex flex-col gap-6">
        <div className="bg-[#1e293b] border border-[#334155] rounded-xl p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-lg">
          <div>
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md bg-blue-950 text-blue-400 text-xs font-semibold border border-blue-800/50">
                <Shield className="h-3.5 w-3.5" />
                <span>Email Forensics V2</span>
              </span>
              <span className="text-xs font-mono bg-[#0f172a] px-2 py-0.5 rounded text-slate-300 border border-[#334155]">
                RFC 5322 .eml
              </span>
            </div>
            <h1 className="text-lg sm:text-xl font-bold text-white mt-1.5">
              Email Forensics &amp; Threat Investigation
            </h1>
            <p className="text-xs text-slate-400 mt-0.5">
              Cryptographic evidence preservation, header analysis, threat intelligence, and AI-assisted evaluation
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 text-xs">
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-[#0f172a] border border-[#334155] rounded-lg text-slate-300">
              <FileCheck className="h-3.5 w-3.5 text-blue-400" />
              <span>SHA-256 Hashed</span>
            </div>
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-[#0f172a] border border-[#334155] rounded-lg text-slate-300">
              <Database className="h-3.5 w-3.5 text-emerald-400" />
              <span>PostgreSQL Preserved</span>
            </div>
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-[#0f172a] border border-[#334155] rounded-lg text-slate-300">
              <Sparkles className="h-3.5 w-3.5 text-purple-400" />
              <span>Privacy-Safe AI</span>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 flex-1 items-start">
          <div className="lg:col-span-7">
            <FileUpload onUploadSuccess={handleUploadSuccess} />
          </div>

          <div className="lg:col-span-5 h-[560px]">
            <RecentUploads refreshTrigger={refreshTrigger} />
          </div>
        </div>
      </main>
    </div>
  );
}
