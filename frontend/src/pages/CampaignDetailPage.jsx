import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import ThreatGraphCanvas from '../components/ThreatGraphCanvas';
import { getCampaignDetail } from '../api/campaigns';
import {
  Flame,
  ArrowLeft,
  Calendar,
  Users,
  ShieldAlert,
  Search,
  Tag,
  ExternalLink,
  Layers,
  ChevronRight,
  RefreshCw,
  FolderKanban,
  FileText
} from 'lucide-react';

export default function CampaignDetailPage() {
  const { campaignId } = useParams();
  const navigate = useNavigate();

  const [campaign, setCampaign] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [memberSearch, setMemberSearch] = useState('');
  const [riskFilter, setRiskFilter] = useState('ALL');

  const fetchCampaign = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getCampaignDetail(campaignId);
      setCampaign(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load campaign dossier');
    } finally {
      setIsLoading(false);
    }
  }, [campaignId]);

  useEffect(() => {
    fetchCampaign();
  }, [fetchCampaign]);

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

  const getRiskBadge = (level, score) => {
    if (level === 'CRITICAL' || score >= 80) {
      return 'bg-rose-950/60 border-rose-800/80 text-rose-300';
    }
    if (level === 'HIGH' || score >= 60) {
      return 'bg-orange-950/60 border-orange-800/80 text-orange-300';
    }
    if (level === 'MEDIUM' || score >= 40) {
      return 'bg-amber-950/60 border-amber-800/80 text-amber-300';
    }
    return 'bg-emerald-950/60 border-emerald-800/80 text-emerald-300';
  };

  const members = campaign?.members || [];
  const filteredMembers = members.filter((m) => {
    const q = memberSearch.toLowerCase();
    const matchesSearch =
      !memberSearch.trim() ||
      (m.subject && m.subject.toLowerCase().includes(q)) ||
      (m.filename && m.filename.toLowerCase().includes(q)) ||
      (m.caseNumber && m.caseNumber.toLowerCase().includes(q)) ||
      (m.membershipReason && m.membershipReason.toLowerCase().includes(q));

    const matchesRisk =
      riskFilter === 'ALL' ||
      (m.riskLevel && m.riskLevel.toUpperCase() === riskFilter.toUpperCase());

    return matchesSearch && matchesRisk;
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <button
            id="back-to-campaigns-btn"
            onClick={() => navigate('/campaigns')}
            className="flex items-center gap-2 text-xs font-medium text-slate-400 hover:text-cyan-400 transition-colors cursor-pointer w-fit"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Return to Threat Campaigns</span>
          </button>

          <button
            id="refresh-campaign-btn"
            onClick={fetchCampaign}
            disabled={isLoading}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 transition-colors cursor-pointer self-start sm:self-auto"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Refresh Dossier</span>
          </button>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/70 text-rose-300 text-xs flex items-center justify-between">
            <span>{error}</span>
            <button
              onClick={fetchCampaign}
              className="px-2.5 py-1 bg-rose-900/60 hover:bg-rose-900 rounded font-medium cursor-pointer"
            >
              Retry
            </button>
          </div>
        )}

        {isLoading && !campaign && (
          <div className="p-16 text-center space-y-3">
            <RefreshCw className="h-8 w-8 text-cyan-400 animate-spin mx-auto" />
            <p className="text-xs text-slate-400 font-mono">Loading campaign cluster dossier & graph...</p>
          </div>
        )}

        {campaign && (
          <>
            <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-sm space-y-6">
              <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-6">
                <div className="space-y-3">
                  <div className="flex flex-wrap items-center gap-2.5">
                    <div className="h-8 w-8 rounded-lg bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
                      <Flame className="h-4 w-4" />
                    </div>
                    <span className="font-mono text-xs px-2.5 py-0.5 rounded-full bg-amber-950 text-amber-400 border border-amber-800/80 font-bold">
                      {campaign.campaignCode}
                    </span>
                    <span
                      className={`text-[10px] font-mono px-2 py-0.5 rounded border ${
                        getConfidenceBadge(campaign.confidenceScore).bg
                      }`}
                    >
                      {getConfidenceBadge(campaign.confidenceScore).label} ({campaign.confidenceScore}%)
                    </span>
                  </div>

                  <h1 className="text-2xl font-bold tracking-tight text-white">
                    {campaign.name}
                  </h1>

                  <p className="text-sm text-slate-300 leading-relaxed max-w-3xl">
                    {campaign.summary}
                  </p>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 bg-slate-950/70 p-4 rounded-xl border border-slate-800/70 min-w-[280px]">
                  <div>
                    <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">
                      Members
                    </span>
                    <span className="text-lg font-bold text-white font-mono flex items-center gap-1.5 mt-0.5">
                      <Users className="h-4 w-4 text-cyan-400" />
                      {campaign.members ? campaign.members.length : 0}
                    </span>
                  </div>

                  <div>
                    <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">
                      First Seen
                    </span>
                    <span className="text-xs text-slate-200 font-mono block mt-1">
                      {campaign.firstSeen ? new Date(campaign.firstSeen).toLocaleDateString() : 'N/A'}
                    </span>
                  </div>

                  <div>
                    <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">
                      Last Seen
                    </span>
                    <span className="text-xs text-slate-200 font-mono block mt-1">
                      {campaign.lastSeen ? new Date(campaign.lastSeen).toLocaleDateString() : 'N/A'}
                    </span>
                  </div>
                </div>
              </div>

              {campaign.sharedIndicators && campaign.sharedIndicators.length > 0 && (
                <div className="space-y-2 pt-4 border-t border-slate-800/70">
                  <div className="flex items-center gap-2 text-xs font-mono text-slate-400">
                    <Tag className="h-3.5 w-3.5 text-amber-400" />
                    <span>Correlated Shared Threat Indicators ({campaign.sharedIndicators.length})</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {campaign.sharedIndicators.map((ind, idx) => (
                      <span
                        key={idx}
                        className="px-2.5 py-1 rounded-md text-xs font-mono bg-slate-950/80 text-amber-300 border border-amber-900/50"
                      >
                        {ind}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <section className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Layers className="h-4 w-4 text-cyan-400" />
                  <h2 className="text-base font-semibold text-white">Campaign Threat Sub-Graph</h2>
                </div>
                <span className="text-xs font-mono text-slate-400">
                  {campaign.graphData?.nodes?.length || 0} nodes · {campaign.graphData?.links?.length || 0} links
                </span>
              </div>

              <ThreatGraphCanvas graphData={campaign.graphData} height="520px" />
            </section>

            <section className="space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4 text-cyan-400" />
                  <h2 className="text-base font-semibold text-white">
                    Member Evidence Dossier ({members.length})
                  </h2>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <div className="relative">
                    <Search className="h-3.5 w-3.5 text-slate-500 absolute left-3 top-2.5" />
                    <input
                      id="member-search-input"
                      type="text"
                      placeholder="Search member emails..."
                      value={memberSearch}
                      onChange={(e) => setMemberSearch(e.target.value)}
                      className="bg-slate-900 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500 w-48 sm:w-64"
                    />
                  </div>

                  <select
                    id="member-risk-filter"
                    value={riskFilter}
                    onChange={(e) => setRiskFilter(e.target.value)}
                    className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-cyan-500 cursor-pointer"
                  >
                    <option value="ALL">All Risk Levels</option>
                    <option value="CRITICAL">Critical</option>
                    <option value="HIGH">High</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="LOW">Low</option>
                  </select>
                </div>
              </div>

              <div className="overflow-x-auto rounded-xl border border-slate-800/80 bg-slate-900/40">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-950/80 text-[11px] font-mono text-slate-400 uppercase tracking-wider border-b border-slate-800">
                    <tr>
                      <th className="py-3 px-4">Evidence Subject & Filename</th>
                      <th className="py-3 px-4">Case Dossier</th>
                      <th className="py-3 px-4">Risk Evaluation</th>
                      <th className="py-3 px-4">Cluster Reason</th>
                      <th className="py-3 px-4">Uploaded</th>
                      <th className="py-3 px-4 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-sans">
                    {filteredMembers.length === 0 ? (
                      <tr>
                        <td colSpan="6" className="py-8 text-center text-slate-500">
                          No member evidence matches the selected filters.
                        </td>
                      </tr>
                    ) : (
                      filteredMembers.map((member) => (
                        <tr key={member.id} className="hover:bg-slate-800/30 transition-colors">
                          <td className="py-3.5 px-4 space-y-0.5">
                            <span className="font-medium text-white block max-w-xs truncate" title={member.subject}>
                              {member.subject || 'Untitled Email'}
                            </span>
                            <span className="text-[11px] font-mono text-slate-400 block truncate max-w-xs">
                              {member.filename}
                            </span>
                          </td>

                          <td className="py-3.5 px-4">
                            <Link
                              to={`/cases/${member.caseId}`}
                              className="inline-flex items-center gap-1.5 text-cyan-400 hover:text-cyan-300 font-mono text-[11px] group"
                            >
                              <FolderKanban className="h-3 w-3 text-cyan-500" />
                              <span>{member.caseNumber}</span>
                              <ExternalLink className="h-2.5 w-2.5 opacity-0 group-hover:opacity-100 transition-opacity" />
                            </Link>
                          </td>

                          <td className="py-3.5 px-4">
                            <span
                              className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${getRiskBadge(
                                member.riskLevel,
                                member.riskScore
                              )}`}
                            >
                              {member.riskLevel || 'LOW'} ({member.riskScore != null ? member.riskScore : 0}/100)
                            </span>
                          </td>

                          <td className="py-3.5 px-4 space-y-1">
                            <span className="text-[11px] text-slate-300 block max-w-xs truncate" title={member.membershipReason}>
                              {member.membershipReason || 'Correlated Indicator'}
                            </span>
                            <span className="text-[10px] font-mono text-amber-400 block">
                              Match Score: {member.matchScore || 0}%
                            </span>
                          </td>

                          <td className="py-3.5 px-4 text-[11px] font-mono text-slate-400 whitespace-nowrap">
                            {member.uploadedAt ? new Date(member.uploadedAt).toLocaleString() : 'N/A'}
                          </td>

                          <td className="py-3.5 px-4 text-right">
                            <button
                              id={`investigate-evidence-${member.emailId}`}
                              onClick={() => navigate(`/cases/${member.caseId}/evidence/${member.emailId}`)}
                              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-cyan-950/70 hover:bg-cyan-900 text-cyan-300 border border-cyan-800/80 transition-colors cursor-pointer"
                            >
                              <span>Investigate</span>
                              <ChevronRight className="h-3.5 w-3.5" />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
