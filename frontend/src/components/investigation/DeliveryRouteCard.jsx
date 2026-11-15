import React from 'react';
import { Network, Server, ArrowDown, Clock } from 'lucide-react';

export default function DeliveryRouteCard({ authData }) {
  let hops = [];
  try {
    const auth = typeof authData === 'string' ? JSON.parse(authData) : authData;
    if (auth?.parsedHops && Array.isArray(auth.parsedHops)) {
      hops = auth.parsedHops;
    } else if (auth?.receivedChain && Array.isArray(auth.receivedChain)) {
      hops = auth.receivedChain;
    }
  } catch (e) {
    hops = [];
  }

  return (
    <div className="bg-[#1e293b] border border-[#334155] rounded-xl p-5 shadow-lg text-slate-100">
      <div className="flex items-center justify-between pb-3 mb-4 border-b border-[#334155]">
        <div className="flex items-center gap-2">
          <Network className="w-5 h-5 text-blue-400" />
          <h3 className="text-base font-bold text-white">Mail Delivery Route (Received Hops)</h3>
        </div>
        <span className="text-xs px-2.5 py-0.5 rounded-full bg-[#0f172a] text-slate-300 border border-[#334155]">
          {hops.length} Hops Reconstructed
        </span>
      </div>

      {hops.length === 0 ? (
        <div className="p-6 text-center text-xs text-slate-400 bg-[#0f172a] rounded-lg border border-[#334155]">
          No Received relay hops found in this email message.
        </div>
      ) : (
        <div className="space-y-2.5">
          {hops.map((hop, index) => (
            <div key={index} className="flex flex-col">
              <div className="p-3 bg-[#0f172a] rounded-lg border border-[#334155] hover:border-slate-500 transition text-xs space-y-2">
                <div className="flex items-center justify-between">
                  <span className="px-2 py-0.5 rounded bg-blue-950 text-blue-300 font-mono font-bold text-[11px] border border-blue-700/40">
                    Hop #{hop.hop || index + 1}
                  </span>
                  {hop.ip && (
                    <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-200 font-mono text-[11px] border border-[#334155]">
                      IP: {hop.ip}
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-[11px] text-slate-300">
                  <div className="truncate">
                    <span className="text-slate-500">From:</span> {hop.from || '—'}
                  </div>
                  <div className="truncate">
                    <span className="text-slate-500">By:</span> {hop.by || '—'}
                  </div>
                </div>

                <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1 border-t border-slate-800">
                  <span><span className="text-slate-500">Protocol:</span> {hop.with || 'SMTP'}</span>
                  {hop.timestamp && (
                    <span className="flex items-center gap-1 font-mono text-slate-500">
                      <Clock className="w-3 h-3" />
                      {hop.timestamp}
                    </span>
                  )}
                </div>
              </div>

              {index < hops.length - 1 && (
                <div className="flex justify-center my-0.5 text-slate-600">
                  <ArrowDown className="w-3.5 h-3.5" />
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
