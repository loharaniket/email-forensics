import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { getCampaigns } from '../api/campaigns';
import {
  Flame,
  Search,
  Filter,
  ShieldAlert,
  Calendar,
  Users,
  ChevronRight,
  ExternalLink,
  Tag,
  ArrowUpDown,
  RefreshCw,
  FolderKanban
} from 'lucide-react';

export default function CampaignsPage() {
  const [campaigns, setCampaigns] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [minConfidence, setMinConfidence] = useState(0);

  const navigate = useNavigate();

  const fetchCampaigns = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getCampaigns(page, 12);
      setCampaigns(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load threat campaigns');
    } finally {
      setIsLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchCampaigns();
  }, [fetchCampaigns]);

  const filteredCampaigns = campaigns.filter((c) => {
    const matchesSearch =
      !searchQuery.trim() ||
      c.campaignCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.summary.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesConfidence = c.confidenceScore >= minConfidence;
    return matchesSearch && matchesConfidence;
  });

  const getConfidenceBadge = (score) => {
    if (score >= 85) {
      return {
        bg: 'bg-rose-950/70 border-rose-800 text-rose-300',
        label: 'HIGH CONFIDENCE'
      };
    }
    if (score >= 60) {
      return {
        bg: 'bg-amber-950/70 border-amber-800 text-amber-300',
        label: 'MODERATE CONFIDENCE'
      };
    }
    return {
      bg: 'bg-slate-800 border-slate-700 text-slate-300',
      label: 'EMERGING'
    };
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-6">
          <div className="space-y-1">
            <div className="flex items-center gap-2.5">
              <div className="h-9 w-9 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
                <Flame className="h-5 w-5" />
              </div>
              <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-3">
                Threat Campaigns
                <span className="text-xs px-2.5 py-0.5 rounded-full font-mono font-semibold bg-amber-950 text-amber-400 border border-amber-800/80">
                  {totalElements} Active Clusters
                </span>
              </h1>
            </div>
            <p className="text-xs text-slate-400">
              Coordinated cross-case phishing and attack campaigns discovered via multi-dimensional indicator correlation.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchCampaigns}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
            <button
              onClick={() => navigate('/graph')}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-semibold shadow-md shadow-cyan-600/20 transition-colors"
            >
              <span>Explore Global Graph</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-2xl border border-slate-800/80">
          <div className="relative flex-1 min-w-[260px]">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              id="campaign-search-input"
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by campaign code, target, or attack vector..."
              className="w-full pl-9 pr-4 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-amber-500"
            />
          </div>

          <div className="flex items-center gap-2">
            <Filter className="w-3.5 h-3.5 text-slate-400" />
            <span className="text-xs text-slate-400">Confidence:</span>
            {[0, 60, 80].map((score) => (
              <button
                key={score}
                onClick={() => setMinConfidence(score)}
                className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${
                  minConfidence === score
                    ? 'bg-amber-600 text-white'
                    : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                }`}
              >
                {score === 0 ? 'All' : `${score}%+`}
              </button>
            ))}
          </div>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-950/50 border border-rose-800/80 text-rose-300 text-xs flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-slate-500 gap-3">
            <RefreshCw className="w-6 h-6 animate-spin text-amber-500" />
            <p className="text-xs">Analyzing and clustering threat campaigns...</p>
          </div>
        ) : filteredCampaigns.length === 0 ? (
          <div className="py-20 flex flex-col items-center justify-center text-slate-500 gap-3 border border-dashed border-slate-800 rounded-2xl">
            <Flame className="w-8 h-8 text-slate-600" />
            <p className="text-sm font-medium text-slate-400">No Threat Campaigns Found</p>
            <p className="text-xs text-slate-500">
              When multiple emails share indicators (URLs, attachment hashes, transit IPs), campaigns will be clustered automatically.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {filteredCampaigns.map((camp) => {
              const badge = getConfidenceBadge(camp.confidenceScore);

              return (
                <div
                  key={camp.id}
                  id={`campaign-card-${camp.campaignCode.toLowerCase()}`}
                  onClick={() => navigate(`/campaigns/${camp.id}`)}
                  className="group relative flex flex-col justify-between bg-slate-900/70 hover:bg-slate-900 border border-slate-800/80 hover:border-amber-500/50 rounded-2xl p-5 shadow-lg transition-all hover:shadow-amber-500/5 cursor-pointer"
                >
                  <div className="space-y-3">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-mono text-xs font-bold px-2 py-0.5 rounded-lg bg-amber-950/80 text-amber-400 border border-amber-800/60">
                        {camp.campaignCode}
                      </span>
                      <span
                        className={`text-[10px] font-mono uppercase px-2 py-0.5 rounded-full font-semibold border ${badge.bg}`}
                      >
                        {badge.label} ({camp.confidenceScore}%)
                      </span>
                    </div>

                    <div>
                      <h3 className="text-sm font-bold text-white group-hover:text-amber-400 transition-colors line-clamp-1">
                        {camp.name}
                      </h3>
                      <p className="text-xs text-slate-400 mt-1 line-clamp-2 leading-relaxed">
                        {camp.summary}
                      </p>
                    </div>

                    <div className="w-full bg-slate-950 rounded-full h-1.5 overflow-hidden">
                      <div
                        className="bg-gradient-to-r from-amber-500 to-rose-500 h-full rounded-full transition-all duration-500"
                        style={{ width: `${camp.confidenceScore}%` }}
                      />
                    </div>

                    {camp.sharedIndicators && camp.sharedIndicators.length > 0 && (
                      <div className="flex flex-wrap gap-1.5 pt-1">
                        {camp.sharedIndicators.slice(0, 3).map((ind, i) => (
                          <span
                            key={i}
                            className="inline-flex items-center gap-1 text-[10px] font-mono px-2 py-0.5 rounded-md bg-slate-950 border border-slate-800 text-slate-400 max-w-[200px] truncate"
                          >
                            <Tag className="w-2.5 h-2.5 text-slate-500 shrink-0" />
                            <span className="truncate">{ind}</span>
                          </span>
                        ))}
                        {camp.sharedIndicators.length > 3 && (
                          <span className="text-[10px] font-mono text-slate-500 px-1 py-0.5">
                            +{camp.sharedIndicators.length - 3}
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="pt-4 mt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
                    <span className="flex items-center gap-1.5 font-medium text-slate-300">
                      <Users className="w-3.5 h-3.5 text-cyan-400" />
                      <span>{camp.memberCount} Associated Emails</span>
                    </span>

                    <span className="flex items-center gap-1 text-amber-400 group-hover:translate-x-0.5 transition-transform text-[11px] font-semibold">
                      Inspect
                      <ChevronRight className="w-3.5 h-3.5" />
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
