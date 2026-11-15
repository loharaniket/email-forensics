import React from 'react';
import { ShieldCheck, ShieldAlert, Shield, AlertTriangle, HelpCircle } from 'lucide-react';

export default function AuthenticationCard({ authData }) {
  if (!authData) {
    return null;
  }

  let auth = {};
  try {
    auth = typeof authData === 'string' ? JSON.parse(authData) : authData;
  } catch (e) {
    auth = {};
  }

  const getBadgeStyle = (status) => {
    const s = (status || 'NOT_VERIFIABLE').toUpperCase();
    switch (s) {
      case 'PASS':
        return 'bg-emerald-950/70 text-emerald-300 border-emerald-600/50';
      case 'FAIL':
        return 'bg-red-950/70 text-red-300 border-red-600/50';
      case 'TEMPERROR':
        return 'bg-amber-950/70 text-amber-300 border-amber-600/50';
      case 'PERMERROR':
        return 'bg-purple-950/70 text-purple-300 border-purple-600/50';
      case 'NONE':
        return 'bg-slate-800 text-slate-300 border-slate-600';
      default:
        return 'bg-blue-950/60 text-blue-300 border-blue-600/40';
    }
  };

  const getStatusIcon = (status) => {
    const s = (status || 'NOT_VERIFIABLE').toUpperCase();
    if (s === 'PASS') return <ShieldCheck className="w-4 h-4 text-emerald-400" />;
    if (s === 'FAIL') return <ShieldAlert className="w-4 h-4 text-red-400" />;
    if (s === 'TEMPERROR' || s === 'PERMERROR') return <AlertTriangle className="w-4 h-4 text-amber-400" />;
    return <HelpCircle className="w-4 h-4 text-slate-400" />;
  };

  const spf = auth.spf || {};
  const dkim = auth.dkim || {};
  const dmarc = auth.dmarc || {};
  const arc = auth.arc || {};

  return (
    <div className="bg-[#1e293b] border border-[#334155] rounded-xl p-5 shadow-lg text-slate-100">
      <div className="flex items-center justify-between pb-3 mb-4 border-b border-[#334155]">
        <div className="flex items-center gap-2">
          <Shield className="w-5 h-5 text-blue-400" />
          <h3 className="text-base font-bold text-white">Email Authentication Protocols</h3>
        </div>
        <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-[#0f172a] text-slate-300 border border-[#334155]">
          mailauth verified
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3.5">
        <div className="p-3.5 bg-[#0f172a] rounded-lg border border-[#334155] flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-slate-300">SPF</span>
            <span className={`px-2 py-0.5 rounded text-[11px] font-mono font-bold border flex items-center gap-1 ${getBadgeStyle(spf.status)}`}>
              {getStatusIcon(spf.status)}
              {spf.status || 'NOT_VERIFIABLE'}
            </span>
          </div>
          <div className="text-[11px] text-slate-400 space-y-1">
            <div className="truncate"><span className="text-slate-500">Domain:</span> {spf.domain || '—'}</div>
            <div className="truncate"><span className="text-slate-500">Client IP:</span> {spf.clientIp || 'Inferred'}</div>
          </div>
        </div>

        <div className="p-3.5 bg-[#0f172a] rounded-lg border border-[#334155] flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-slate-300">DKIM</span>
            <span className={`px-2 py-0.5 rounded text-[11px] font-mono font-bold border flex items-center gap-1 ${getBadgeStyle(dkim.status)}`}>
              {getStatusIcon(dkim.status)}
              {dkim.status || 'NONE'}
            </span>
          </div>
          <div className="text-[11px] text-slate-400 space-y-1">
            <div className="truncate"><span className="text-slate-500">Signing:</span> {dkim.domain || '—'}</div>
            <div className="truncate"><span className="text-slate-500">Aligned:</span> {dkim.aligned ? 'YES' : 'NO / NONE'}</div>
          </div>
        </div>

        <div className="p-3.5 bg-[#0f172a] rounded-lg border border-[#334155] flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-slate-300">DMARC</span>
            <span className={`px-2 py-0.5 rounded text-[11px] font-mono font-bold border flex items-center gap-1 ${getBadgeStyle(dmarc.status)}`}>
              {getStatusIcon(dmarc.status)}
              {dmarc.status || 'NONE'}
            </span>
          </div>
          <div className="text-[11px] text-slate-400 space-y-1">
            <div className="truncate"><span className="text-slate-500">Policy:</span> {dmarc.policy || 'none'}</div>
            <div className="truncate"><span className="text-slate-500">Aligned:</span> {dmarc.aligned ? 'YES' : 'NO'}</div>
          </div>
        </div>

        <div className="p-3.5 bg-[#0f172a] rounded-lg border border-[#334155] flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-slate-300">ARC</span>
            <span className={`px-2 py-0.5 rounded text-[11px] font-mono font-bold border flex items-center gap-1 ${getBadgeStyle(arc.status)}`}>
              {getStatusIcon(arc.status)}
              {arc.status || 'NONE'}
            </span>
          </div>
          <div className="text-[11px] text-slate-400 space-y-1">
            <div className="truncate"><span className="text-slate-500">Chain:</span> {arc.status === 'PASS' ? 'Intact' : 'Not Validated'}</div>
            <div className="truncate"><span className="text-slate-500">Type:</span> RFC 8617</div>
          </div>
        </div>
      </div>
    </div>
  );
}
