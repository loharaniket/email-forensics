import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FileText, Download, ExternalLink, RefreshCw, Hash, Clock, CheckCircle } from 'lucide-react';
import { getRecentEmails, downloadOriginalEmail } from '../api/emailApi';

export default function RecentUploads({ refreshTrigger }) {
  const navigate = useNavigate();
  const [emails, setEmails] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchEmails = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getRecentEmails();
      setEmails(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load recent evidence records');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEmails();
  }, [refreshTrigger]);

  const handleDownload = async (id, fileName, e) => {
    e.stopPropagation();
    try {
      await downloadOriginalEmail(id, fileName);
    } catch (err) {
      alert(err.message || 'Download failed');
    }
  };

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return isoString;
    }
  };

  return (
    <div className="bg-[#1e293b] border border-[#334155] rounded-xl p-6 shadow-xl text-slate-100 flex flex-col h-full">
      <div className="flex items-center justify-between mb-4 pb-3 border-b border-[#334155]">
        <div className="flex items-center gap-2">
          <FileText className="w-5 h-5 text-blue-400" />
          <h2 className="text-lg font-bold text-white">Recent Evidence Records</h2>
          <span className="text-xs px-2 py-0.5 rounded-full bg-[#0f172a] text-slate-300 border border-[#334155]">
            {emails.length}
          </span>
        </div>
        <button
          onClick={fetchEmails}
          disabled={isLoading}
          className="p-1.5 rounded-lg bg-[#0f172a] hover:bg-[#334155] text-slate-400 hover:text-white transition border border-[#334155]"
          title="Refresh"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-blue-400' : ''}`} />
        </button>
      </div>

      {error && (
        <div className="p-3 mb-3 bg-red-950/40 border border-red-500/30 rounded-lg text-xs text-red-200">
          {error}
        </div>
      )}

      <div className="flex-1 overflow-y-auto min-h-0 pr-1 space-y-2">
        {isLoading && emails.length === 0 ? (
          <div className="flex items-center justify-center py-12 text-slate-400 text-xs gap-2">
            <div className="w-4 h-4 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
            <span>Loading evidence records...</span>
          </div>
        ) : emails.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center text-slate-400">
            <Hash className="w-10 h-10 text-slate-600 mb-2" />
            <p className="text-sm font-medium text-slate-300">No email evidence preserved yet</p>
            <p className="text-xs text-slate-500 mt-1">Upload a .eml file above to register your first record.</p>
          </div>
        ) : (
          emails.map((email) => (
            <div
              key={email.id}
              onClick={() => navigate(`/evidence/${email.id}`)}
              className="p-3.5 bg-[#0f172a] hover:bg-slate-800/80 border border-[#334155] hover:border-blue-500/50 rounded-lg cursor-pointer transition flex flex-col gap-2 group"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0">
                  <span className="text-xs font-mono font-bold text-blue-400 shrink-0">
                    #{email.id}
                  </span>
                  <span className="text-xs font-medium text-slate-200 truncate group-hover:text-blue-300 transition">
                    {email.fileName}
                  </span>
                </div>
                <div className="flex items-center gap-1.5 shrink-0">
                  <button
                    onClick={(e) => handleDownload(email.id, email.fileName, e)}
                    className="p-1 rounded hover:bg-[#334155] text-slate-400 hover:text-white transition"
                    title="Download Original .eml"
                  >
                    <Download className="w-3.5 h-3.5" />
                  </button>
                  <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-blue-900/40 text-blue-300 border border-blue-600/30">
                    {email.status || 'RECEIVED'}
                  </span>
                </div>
              </div>

              <div className="flex items-center justify-between text-[11px] text-slate-400 font-mono">
                <span className="truncate max-w-[200px]" title={email.sha256}>
                  sha256: {email.sha256 ? `${email.sha256.substring(0, 16)}...` : '—'}
                </span>
                <span className="flex items-center gap-1 text-slate-500 font-sans shrink-0">
                  <Clock className="w-3 h-3" />
                  {formatDate(email.createdAt)}
                </span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
