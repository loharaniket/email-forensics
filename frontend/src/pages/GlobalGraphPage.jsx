import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import ThreatGraphCanvas from '../components/ThreatGraphCanvas';
import { getGlobalGraph } from '../api/graph';
import {
  Share2,
  RefreshCw,
  SlidersHorizontal,
  Layers,
  Mail,
  Globe,
  Server,
  FileText,
  ShieldAlert
} from 'lucide-react';

export default function GlobalGraphPage() {
  const [graphData, setGraphData] = useState({ nodes: [], links: [] });
  const [limit, setLimit] = useState(150);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchGraph = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getGlobalGraph(limit);
      setGraphData(data || { nodes: [], links: [] });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load global cyber threat graph');
    } finally {
      setIsLoading(false);
    }
  }, [limit]);

  useEffect(() => {
    fetchGraph();
  }, [fetchGraph]);

  const nodes = graphData.nodes || [];
  const links = graphData.links || [];

  const countsByType = nodes.reduce((acc, curr) => {
    acc[curr.type] = (acc[curr.type] || 0) + 1;
    return acc;
  }, {});

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-6">
          <div className="space-y-1">
            <div className="flex items-center gap-2.5">
              <div className="h-9 w-9 rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
                <Share2 className="h-5 w-5" />
              </div>
              <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-3">
                Global Threat Graph
                <span className="text-xs px-2.5 py-0.5 rounded-full font-mono font-semibold bg-cyan-950 text-cyan-400 border border-cyan-800">
                  {nodes.length} Nodes · {links.length} Relations
                </span>
              </h1>
            </div>
            <p className="text-xs text-slate-400">
              Interactive cyber graph cross-referencing email evidence, malicious URLs, infrastructure IPs, and attachment hashes across all case files.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 px-3 py-1.5 rounded-lg text-xs">
              <SlidersHorizontal className="h-3.5 w-3.5 text-slate-400" />
              <span className="text-slate-400 font-mono text-[11px]">Max Nodes:</span>
              <select
                id="global-limit-select"
                value={limit}
                onChange={(e) => setLimit(Number(e.target.value))}
                className="bg-transparent text-white font-mono font-semibold focus:outline-none cursor-pointer"
              >
                <option value={50} className="bg-slate-900">50</option>
                <option value={100} className="bg-slate-900">100</option>
                <option value={150} className="bg-slate-900">150</option>
                <option value={250} className="bg-slate-900">250</option>
                <option value={500} className="bg-slate-900">500</option>
              </select>
            </div>

            <button
              id="refresh-global-graph-btn"
              onClick={fetchGraph}
              disabled={isLoading}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-cyan-950 hover:bg-cyan-900 text-cyan-300 border border-cyan-800 transition-colors cursor-pointer"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              <span>Refresh Graph</span>
            </button>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-4 gap-2.5">
          <div className="bg-slate-900/50 border border-slate-800 p-3 rounded-xl flex items-center gap-2.5">
            <Mail className="h-4 w-4 text-cyan-400" />
            <div>
              <span className="text-[10px] font-mono text-slate-400 block uppercase">Emails</span>
              <span className="text-sm font-bold font-mono text-white">
                {countsByType['EMAIL'] || 0}
              </span>
            </div>
          </div>

          <div className="bg-slate-900/50 border border-slate-800 p-3 rounded-xl flex items-center gap-2.5">
            <Globe className="h-4 w-4 text-orange-400" />
            <div>
              <span className="text-[10px] font-mono text-slate-400 block uppercase">URLs & Domains</span>
              <span className="text-sm font-bold font-mono text-white">
                {(countsByType['URL'] || 0) + (countsByType['DOMAIN'] || 0)}
              </span>
            </div>
          </div>

          <div className="bg-slate-900/50 border border-slate-800 p-3 rounded-xl flex items-center gap-2.5">
            <Server className="h-4 w-4 text-rose-400" />
            <div>
              <span className="text-[10px] font-mono text-slate-400 block uppercase">IP Infrastructure</span>
              <span className="text-sm font-bold font-mono text-white">
                {countsByType['IP'] || 0}
              </span>
            </div>
          </div>

          <div className="bg-slate-900/50 border border-slate-800 p-3 rounded-xl flex items-center gap-2.5">
            <FileText className="h-4 w-4 text-pink-400" />
            <div>
              <span className="text-[10px] font-mono text-slate-400 block uppercase">Attachments</span>
              <span className="text-sm font-bold font-mono text-white">
                {countsByType['ATTACHMENT'] || 0}
              </span>
            </div>
          </div>
        </div>

        {error ? (
          <div className="p-6 rounded-2xl bg-rose-950/20 border border-rose-900 text-center space-y-2">
            <ShieldAlert className="h-8 w-8 text-rose-400 mx-auto" />
            <h3 className="text-sm font-bold text-rose-300">Graph Load Error</h3>
            <p className="text-xs text-rose-400/80">{error}</p>
          </div>
        ) : (
          <div className="p-4 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-mono font-medium text-slate-400 flex items-center gap-2">
                <Layers className="h-3.5 w-3.5 text-cyan-400" />
                Cross-Case Threat Indicator Topology Canvas
              </span>
            </div>
            <ThreatGraphCanvas graphData={graphData} height="680px" />
          </div>
        )}
      </main>
    </div>
  );
}
