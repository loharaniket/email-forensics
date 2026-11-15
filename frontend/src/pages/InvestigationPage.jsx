import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import AuthenticationCard from '../components/investigation/AuthenticationCard';
import SenderAnalysisCard from '../components/investigation/SenderAnalysisCard';
import DeliveryRouteCard from '../components/investigation/DeliveryRouteCard';
import IndicatorListCard from '../components/investigation/IndicatorListCard';
import { getEmailById, getEmailIndicators, checkThreats, downloadOriginalEmail } from '../api/emailApi';
import { ArrowLeft, Download, Copy, Shield, Calendar, Mail, FileCheck, CheckCircle2 } from 'lucide-react';

export default function InvestigationPage() {
  const { emailId } = useParams();
  const navigate = useNavigate();

  const [email, setEmail] = useState(null);
  const [indicators, setIndicators] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isScanningThreats, setIsScanningThreats] = useState(false);
  const [error, setError] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    async function loadData() {
      setIsLoading(true);
      setError(null);
      try {
        const [emailData, indicatorsData] = await Promise.all([
          getEmailById(emailId),
          getEmailIndicators(emailId).catch(() => [])
        ]);
        setEmail(emailData);
        setIndicators(indicatorsData || []);
      } catch (err) {
        setError(err.message || 'Failed to load investigation details');
      } finally {
        setIsLoading(false);
      }
    }

    if (emailId) {
      loadData();
    }
  }, [emailId]);

  const handleRescanThreats = async () => {
    if (!emailId || isScanningThreats) return;
    setIsScanningThreats(true);
    try {
      const updatedIndicators = await checkThreats(emailId);
      setIndicators(updatedIndicators || []);
      const updatedEmail = await getEmailById(emailId);
      setEmail(updatedEmail);
    } catch (err) {
      alert(err.message || 'Failed to scan threat feeds');
    } finally {
      setIsScanningThreats(false);
    }
  };

  const handleCopyHash = () => {
    if (!email?.sha256) return;
    navigator.clipboard.writeText(email.sha256);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownload = async () => {
    if (!email) return;
    try {
      await downloadOriginalEmail(email.id, email.fileName);
    } catch (err) {
      alert(err.message || 'Download failed');
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    try {
      return new Date(isoString).toLocaleString();
    } catch (e) {
      return isoString;
    }
  };

  return (
    <div className="min-h-screen bg-[#0f172a] text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 flex flex-col gap-5">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#1e293b] hover:bg-[#334155] text-slate-300 hover:text-white transition text-xs border border-[#334155]"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Dashboard</span>
          </button>
        </div>

        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-400 gap-3">
            <div className="w-6 h-6 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
            <span className="text-xs">Loading forensic investigation...</span>
          </div>
        ) : error ? (
          <div className="p-4 bg-red-950/50 border border-red-500/40 rounded-xl text-xs text-red-200 space-y-2">
            <strong>Error Loading Record:</strong> {error}
          </div>
        ) : email ? (
          <div className="space-y-5">
            <div className="bg-[#1e293b] border border-[#334155] rounded-xl p-5 shadow-lg space-y-4">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pb-3 border-b border-[#334155]">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-blue-950 text-blue-400 border border-blue-800/40">
                      Case #{email.id}
                    </span>
                    <span className="text-xs font-semibold px-2 py-0.5 rounded bg-blue-900/40 text-blue-300 border border-blue-600/30">
                      {email.status || 'RECEIVED'}
                    </span>
                    <span className="text-xs font-mono px-2 py-0.5 rounded bg-[#0f172a] text-slate-300 border border-[#334155]">
                      Stage: {email.stage || 'AUTHENTICATED'}
                    </span>
                  </div>
                  <h1 className="text-lg font-bold text-white pt-1">
                    {email.subject || '(No Subject)'}
                  </h1>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={handleDownload}
                    className="px-3 py-1.5 rounded-lg bg-[#0f172a] hover:bg-[#334155] text-slate-300 hover:text-white transition text-xs border border-[#334155] flex items-center gap-1.5"
                  >
                    <Download className="w-3.5 h-3.5 text-blue-400" />
                    <span>Download Original .eml</span>
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                <div className="p-3 bg-[#0f172a] rounded-lg border border-[#334155]">
                  <span className="text-slate-400 text-[11px] block">Sender (From)</span>
                  <span className="font-mono text-slate-200 truncate block mt-0.5">
                    {email.sender || '—'}
                  </span>
                </div>

                <div className="p-3 bg-[#0f172a] rounded-lg border border-[#334155]">
                  <span className="text-slate-400 text-[11px] block">Sent Date</span>
                  <span className="font-mono text-slate-200 truncate block mt-0.5">
                    {formatDate(email.sentAt || email.createdAt)}
                  </span>
                </div>

                <div className="p-3 bg-[#0f172a] rounded-lg border border-[#334155]">
                  <span className="text-slate-400 text-[11px] block">File Name</span>
                  <span className="font-mono text-slate-200 truncate block mt-0.5">
                    {email.fileName}
                  </span>
                </div>
              </div>

              <div className="p-2.5 bg-[#0f172a] rounded-lg border border-[#334155] flex items-center gap-2 text-xs">
                <FileCheck className="w-4 h-4 text-blue-400 shrink-0" />
                <span className="text-slate-400 text-[11px]">SHA-256:</span>
                <code className="text-blue-300 font-mono text-[11px] truncate flex-1">
                  {email.sha256}
                </code>
                <button
                  onClick={handleCopyHash}
                  className="px-2 py-0.5 text-[11px] bg-[#1e293b] hover:bg-[#334155] text-slate-300 rounded border border-[#334155] flex items-center gap-1 transition"
                >
                  <Copy className="w-3 h-3" />
                  <span>{copied ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
            </div>

            <AuthenticationCard authData={email.authData} />

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 items-start">
              <SenderAnalysisCard email={email} />
              <DeliveryRouteCard authData={email.authData} />
            </div>

            <IndicatorListCard
              indicators={indicators}
              onRescanThreats={handleRescanThreats}
              isScanning={isScanningThreats}
            />
          </div>
        ) : null}
      </main>
    </div>
  );
}
