import React, { useState } from 'react';
import {
  Globe,
  Link2,
  Server,
  Search,
  Copy,
  Check,
  Filter,
  AlertTriangle,
  AlertCircle,
  ShieldCheck,
  HelpCircle,
  ChevronDown,
  ChevronUp,
  RefreshCw
} from 'lucide-react';

export default function IndicatorListCard({ indicators = [], onRescanThreats, isScanning = false }) {
  const [activeFilter, setActiveFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [copiedId, setCopiedId] = useState(null);
  const [expandedId, setExpandedId] = useState(null);

  const totalCount = indicators.length;
  const urlCount = indicators.filter(i => i.type === 'URL').length;
  const domainCount = indicators.filter(i => i.type === 'DOMAIN').length;
  const ipCount = indicators.filter(i => i.type === 'IP').length;

  const maliciousCount = indicators.filter(i => i.verdict === 'MALICIOUS').length;
  const suspiciousCount = indicators.filter(i => i.verdict === 'SUSPICIOUS').length;
  const cleanCount = indicators.filter(i => i.verdict === 'NO_MATCH').length;
  const unknownCount = indicators.filter(i => !i.verdict || i.verdict === 'UNKNOWN').length;

  const handleCopy = (id, text) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const toggleExpand = (id) => {
    setExpandedId(prev => (prev === id ? null : id));
  };

  const filteredIndicators = indicators.filter(ind => {
    if (activeFilter === 'THREATS' && ind.verdict !== 'MALICIOUS' && ind.verdict !== 'SUSPICIOUS') {
      return false;
    }
    if (activeFilter !== 'ALL' && activeFilter !== 'THREATS' && ind.type !== activeFilter) {
      return false;
    }
    if (searchQuery.trim() !== '') {
      const q = searchQuery.toLowerCase();
      const val = (ind.normalizedValue || ind.value || '').toLowerCase();
      const origin = (ind.origin || '').toLowerCase();
      const verdict = (ind.verdict || '').toLowerCase();
      return val.includes(q) || origin.includes(q) || verdict.includes(q);
    }
    return true;
  });

  const getOriginBadge = (origin) => {
    switch (origin) {
      case 'HEADER':
        return 'bg-purple-950/60 text-purple-300 border-purple-800/50';
      case 'BODY':
        return 'bg-blue-950/60 text-blue-300 border-blue-800/50';
      case 'HTML':
        return 'bg-emerald-950/60 text-emerald-300 border-emerald-800/50';
      case 'DNS':
        return 'bg-amber-950/60 text-amber-300 border-amber-800/50';
      case 'IMAGE_OCR':
        return 'bg-pink-950/60 text-pink-300 border-pink-800/50';
      case 'QR':
        return 'bg-cyan-950/60 text-cyan-300 border-cyan-800/50';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'URL':
        return <Link2 className="w-3.5 h-3.5 text-amber-400" />;
      case 'DOMAIN':
        return <Globe className="w-3.5 h-3.5 text-cyan-400" />;
      case 'IP':
        return <Server className="w-3.5 h-3.5 text-indigo-400" />;
      default:
        return <Filter className="w-3.5 h-3.5 text-slate-400" />;
    }
  };

  const getVerdictBadge = (verdict) => {
    switch (verdict) {
      case 'MALICIOUS':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold bg-red-950/70 text-red-300 border border-red-700/60">
            <AlertTriangle className="w-3 h-3 text-red-400" />
            <span>MALICIOUS</span>
          </span>
        );
      case 'SUSPICIOUS':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-950/70 text-amber-300 border border-amber-700/60">
            <AlertCircle className="w-3 h-3 text-amber-400" />
            <span>SUSPICIOUS</span>
          </span>
        );
      case 'NO_MATCH':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-medium bg-emerald-950/70 text-emerald-300 border border-emerald-700/60">
            <ShieldCheck className="w-3 h-3 text-emerald-400" />
            <span>NO MATCH</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-slate-400 border border-slate-700">
            <HelpCircle className="w-3 h-3 text-slate-400" />
            <span>{verdict || 'UNKNOWN'}</span>
          </span>
        );
    }
  };

  const parseDetails = (detailsStr) => {
    if (!detailsStr) return null;
    try {
      if (typeof detailsStr === 'object') return detailsStr;
      return JSON.parse(detailsStr);
    } catch (e) {
      return null;
    }
  };

  return (
    <div className="bg-[#1e293b] border border-[#334155] rounded-xl p-5 shadow-lg space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#334155]">
        <div className="flex items-center gap-2.5">
          <div className="p-1.5 rounded-lg bg-blue-950/60 border border-blue-800/50 text-blue-400">
            <Filter className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white tracking-wide">
              Threat Intelligence & Extracted Indicators
            </h2>
            <p className="text-[11px] text-slate-400">
              Evaluated against Google Safe Browsing, URLhaus, PhishTank, ThreatFox, AbuseIPDB, and IPinfo
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {onRescanThreats && (
            <button
              onClick={onRescanThreats}
              disabled={isScanning}
              className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white text-xs font-medium border border-blue-400 transition shadow-sm"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isScanning ? 'animate-spin' : ''}`} />
              <span>{isScanning ? 'Querying Feeds...' : 'Query Threat Feeds'}</span>
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
        <div className="p-2.5 bg-[#0f172a] rounded-lg border border-[#334155] flex items-center justify-between">
          <span className="text-slate-400 text-[11px]">Malicious</span>
          <span className="font-bold text-red-400 font-mono text-sm">{maliciousCount}</span>
        </div>
        <div className="p-2.5 bg-[#0f172a] rounded-lg border border-[#334155] flex items-center justify-between">
          <span className="text-slate-400 text-[11px]">Suspicious</span>
          <span className="font-bold text-amber-400 font-mono text-sm">{suspiciousCount}</span>
        </div>
        <div className="p-2.5 bg-[#0f172a] rounded-lg border border-[#334155] flex items-center justify-between">
          <span className="text-slate-400 text-[11px]">Clean (No Match)</span>
          <span className="font-bold text-emerald-400 font-mono text-sm">{cleanCount}</span>
        </div>
        <div className="p-2.5 bg-[#0f172a] rounded-lg border border-[#334155] flex items-center justify-between">
          <span className="text-slate-400 text-[11px]">Total Indicators</span>
          <span className="font-bold text-slate-200 font-mono text-sm">{totalCount}</span>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-1.5 overflow-x-auto text-xs">
          <button
            onClick={() => setActiveFilter('ALL')}
            className={`px-2.5 py-1 rounded-lg border transition text-xs font-medium ${
              activeFilter === 'ALL'
                ? 'bg-blue-600 text-white border-blue-500 shadow-sm'
                : 'bg-[#0f172a] text-slate-400 hover:text-white border-[#334155]'
            }`}
          >
            All ({totalCount})
          </button>
          <button
            onClick={() => setActiveFilter('THREATS')}
            className={`px-2.5 py-1 rounded-lg border transition text-xs font-medium flex items-center gap-1 ${
              activeFilter === 'THREATS'
                ? 'bg-red-600 text-white border-red-500 shadow-sm'
                : 'bg-[#0f172a] text-slate-400 hover:text-white border-[#334155]'
            }`}
          >
            <AlertTriangle className="w-3 h-3 text-red-400" />
            Threats ({maliciousCount + suspiciousCount})
          </button>
          <button
            onClick={() => setActiveFilter('URL')}
            className={`px-2.5 py-1 rounded-lg border transition text-xs font-medium flex items-center gap-1 ${
              activeFilter === 'URL'
                ? 'bg-amber-600 text-white border-amber-500 shadow-sm'
                : 'bg-[#0f172a] text-slate-400 hover:text-white border-[#334155]'
            }`}
          >
            <Link2 className="w-3 h-3" />
            URLs ({urlCount})
          </button>
          <button
            onClick={() => setActiveFilter('DOMAIN')}
            className={`px-2.5 py-1 rounded-lg border transition text-xs font-medium flex items-center gap-1 ${
              activeFilter === 'DOMAIN'
                ? 'bg-cyan-600 text-white border-cyan-500 shadow-sm'
                : 'bg-[#0f172a] text-slate-400 hover:text-white border-[#334155]'
            }`}
          >
            <Globe className="w-3 h-3" />
            Domains ({domainCount})
          </button>
          <button
            onClick={() => setActiveFilter('IP')}
            className={`px-2.5 py-1 rounded-lg border transition text-xs font-medium flex items-center gap-1 ${
              activeFilter === 'IP'
                ? 'bg-indigo-600 text-white border-indigo-500 shadow-sm'
                : 'bg-[#0f172a] text-slate-400 hover:text-white border-[#334155]'
            }`}
          >
            <Server className="w-3 h-3" />
            Public IPs ({ipCount})
          </button>
        </div>

        <div className="relative w-full sm:w-64">
          <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search indicator, origin, verdict..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-8 pr-3 py-1 bg-[#0f172a] border border-[#334155] rounded-lg text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>
      </div>

      {filteredIndicators.length === 0 ? (
        <div className="py-8 text-center text-xs text-slate-400 bg-[#0f172a] border border-[#334155] rounded-lg">
          No threat indicators matching the criteria.
        </div>
      ) : (
        <div className="overflow-x-auto border border-[#334155] rounded-lg">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#0f172a] text-slate-400 border-b border-[#334155]">
              <tr>
                <th className="py-2.5 px-3 font-semibold w-24">Type</th>
                <th className="py-2.5 px-3 font-semibold w-24">Origin</th>
                <th className="py-2.5 px-3 font-semibold">Normalized Value</th>
                <th className="py-2.5 px-3 font-semibold w-32">Verdict</th>
                <th className="py-2.5 px-3 font-semibold w-20 text-right">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#334155] bg-[#1e293b]/70">
              {filteredIndicators.map((ind) => {
                const details = parseDetails(ind.details);
                const hasDetails = details && Object.keys(details).length > 0;
                const isExpanded = expandedId === ind.id;

                return (
                  <React.Fragment key={ind.id || `${ind.type}-${ind.origin}-${ind.normalizedValue}`}>
                    <tr className="hover:bg-slate-800/50 transition">
                      <td className="py-2 px-3 whitespace-nowrap">
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-[#0f172a] text-slate-300 border border-[#334155]">
                          {getTypeIcon(ind.type)}
                          <span>{ind.type}</span>
                        </span>
                      </td>
                      <td className="py-2 px-3 whitespace-nowrap">
                        <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-semibold border ${getOriginBadge(ind.origin)}`}>
                          {ind.origin}
                        </span>
                      </td>
                      <td className="py-2 px-3">
                        <div className="space-y-0.5">
                          <span className="font-mono text-slate-200 text-xs break-all block">
                            {ind.normalizedValue}
                          </span>
                          {ind.value && ind.value !== ind.normalizedValue ? (
                            <span className="font-mono text-[10px] text-slate-400 break-all block">
                              Raw: {ind.value}
                            </span>
                          ) : null}
                        </div>
                      </td>
                      <td className="py-2 px-3 whitespace-nowrap">
                        {getVerdictBadge(ind.verdict)}
                      </td>
                      <td className="py-2 px-3 whitespace-nowrap text-right">
                        <div className="inline-flex items-center gap-1">
                          <button
                            onClick={() => handleCopy(ind.id, ind.normalizedValue)}
                            className="p-1 rounded hover:bg-[#334155] text-slate-400 hover:text-white transition"
                            title="Copy indicator"
                          >
                            {copiedId === ind.id ? (
                              <Check className="w-3.5 h-3.5 text-emerald-400" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                          {hasDetails && (
                            <button
                              onClick={() => toggleExpand(ind.id)}
                              className="p-1 rounded hover:bg-[#334155] text-slate-400 hover:text-white transition"
                              title={isExpanded ? 'Hide threat intelligence details' : 'View threat intelligence details'}
                            >
                              {isExpanded ? (
                                <ChevronUp className="w-3.5 h-3.5 text-blue-400" />
                              ) : (
                                <ChevronDown className="w-3.5 h-3.5" />
                              )}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>

                    {isExpanded && hasDetails && (
                      <tr className="bg-[#0f172a]/90">
                        <td colSpan={5} className="p-3 border-t border-[#334155]">
                          <div className="space-y-2">
                            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                              Provider Intelligence Findings
                            </span>
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
                              {Object.entries(details).map(([provKey, provVal]) => {
                                const pName = provVal?.providerName || provKey;
                                const pStatus = provVal?.status || 'UNKNOWN';
                                const pVerdict = provVal?.verdict || 'UNKNOWN';
                                const pMsg = provVal?.message || '';
                                const pDet = provVal?.details || {};

                                const statusColor = pStatus === 'MATCH'
                                  ? 'text-red-400 border-red-800/40 bg-red-950/30'
                                  : pStatus === 'NO_MATCH'
                                  ? 'text-emerald-400 border-emerald-800/40 bg-emerald-950/30'
                                  : 'text-slate-400 border-slate-700 bg-slate-900/40';

                                return (
                                  <div key={provKey} className="p-2.5 rounded bg-[#1e293b] border border-[#334155] space-y-1 text-xs">
                                    <div className="flex items-center justify-between">
                                      <span className="font-bold text-white text-[11px]">{pName}</span>
                                      <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded border ${statusColor}`}>
                                        {pStatus}
                                      </span>
                                    </div>
                                    <p className="text-[11px] text-slate-300">{pMsg || 'Query completed'}</p>
                                    {pDet && Object.keys(pDet).length > 0 && (
                                      <div className="pt-1 text-[10px] text-slate-400 font-mono space-y-0.5 border-t border-[#334155]/60 mt-1">
                                        {Object.entries(pDet).map(([k, v]) => (
                                          <div key={k} className="truncate">
                                            <span className="text-slate-500">{k}:</span> {String(v)}
                                          </div>
                                        ))}
                                      </div>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
