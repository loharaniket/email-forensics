import React from 'react';
import { Mail, AlertTriangle, CheckCircle, ArrowRightLeft } from 'lucide-react';

export default function SenderAnalysisCard({ email }) {
  if (!email) return null;

  let senderAnalysis = {};
  try {
    const auth = typeof email.authData === 'string' ? JSON.parse(email.authData) : email.authData;
    senderAnalysis = auth?.senderAnalysis || {};
  } catch (e) {
    senderAnalysis = {};
  }

  const from = email.sender || senderAnalysis.from || '—';
  const replyTo = email.replyTo || senderAnalysis.replyTo || '—';
  const returnPath = senderAnalysis.returnPath || '—';
  const replyToMismatch = !!senderAnalysis.replyToMismatch;
  const returnPathMismatch = !!senderAnalysis.returnPathMismatch;

  return (
    <div className="bg-[#1e293b] border border-[#334155] rounded-xl p-5 shadow-lg text-slate-100">
      <div className="flex items-center justify-between pb-3 mb-4 border-b border-[#334155]">
        <div className="flex items-center gap-2">
          <Mail className="w-5 h-5 text-blue-400" />
          <h3 className="text-base font-bold text-white">Sender Identity &amp; Header Alignment</h3>
        </div>
        {(replyToMismatch || returnPathMismatch) ? (
          <span className="flex items-center gap-1 text-[11px] font-semibold px-2.5 py-0.5 rounded bg-red-950/70 text-red-300 border border-red-600/40">
            <AlertTriangle className="w-3.5 h-3.5" />
            Domain Mismatch Detected
          </span>
        ) : (
          <span className="flex items-center gap-1 text-[11px] font-semibold px-2.5 py-0.5 rounded bg-emerald-950/70 text-emerald-300 border border-emerald-600/40">
            <CheckCircle className="w-3.5 h-3.5" />
            Headers Aligned
          </span>
        )}
      </div>

      <div className="space-y-3 text-xs">
        <div className="p-3 bg-[#0f172a] rounded-lg border border-[#334155] flex flex-col sm:flex-row sm:items-center justify-between gap-1">
          <span className="text-slate-400 font-medium">From (Header Display)</span>
          <span className="font-mono text-slate-100 truncate">{from}</span>
        </div>

        <div className={`p-3 rounded-lg border flex flex-col sm:flex-row sm:items-center justify-between gap-1 ${
          replyToMismatch ? 'bg-amber-950/30 border-amber-600/40' : 'bg-[#0f172a] border-[#334155]'
        }`}>
          <div className="flex items-center gap-1.5">
            <span className="text-slate-400 font-medium">Reply-To</span>
            {replyToMismatch && (
              <span className="px-1.5 py-0.2 rounded text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/30">
                Mismatch
              </span>
            )}
          </div>
          <span className="font-mono text-slate-100 truncate">{replyTo}</span>
        </div>

        <div className={`p-3 rounded-lg border flex flex-col sm:flex-row sm:items-center justify-between gap-1 ${
          returnPathMismatch ? 'bg-amber-950/30 border-amber-600/40' : 'bg-[#0f172a] border-[#334155]'
        }`}>
          <div className="flex items-center gap-1.5">
            <span className="text-slate-400 font-medium">Return-Path (Envelope)</span>
            {returnPathMismatch && (
              <span className="px-1.5 py-0.2 rounded text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/30">
                Mismatch
              </span>
            )}
          </div>
          <span className="font-mono text-slate-100 truncate">{returnPath}</span>
        </div>

        {replyToMismatch && (
          <div className="p-3 bg-red-950/40 border border-red-500/30 rounded-lg flex items-start gap-2 text-xs text-red-200">
            <ArrowRightLeft className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
            <div>
              <strong className="block font-semibold">Reply-To Address Redirection:</strong>
              Replies to this email will be directed to a different domain than the sender. This is a common spoofing and credential harvesting technique.
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
