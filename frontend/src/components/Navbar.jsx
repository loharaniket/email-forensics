import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ShieldCheck, FolderKanban, Activity, LogOut, User, Flame, Share2 } from 'lucide-react'

export default function Navbar() {
  const { user, logout } = useAuth()
  const location = useLocation()

  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur-md sticky top-0 z-50 px-6 py-3.5">
      <div className="max-w-6xl mx-auto flex items-center justify-between">
        <div className="flex items-center gap-6">
          <Link to="/cases" className="flex items-center gap-3 group">
            <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-cyan-600 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20 group-hover:scale-105 transition-transform">
              <ShieldCheck className="h-5 w-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold tracking-tight text-white">
                  Email Forensics
                </span>
                <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-cyan-950 text-cyan-400 border border-cyan-800 font-mono">
                  Phase 10
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block">
                Threat Correlation &amp; Case Platform
              </p>
            </div>
          </Link>

          <nav className="flex items-center gap-1 sm:gap-2">
            <Link
              to="/cases"
              id="nav-cases-link"
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                location.pathname.startsWith('/cases')
                  ? 'bg-slate-800 text-white border border-slate-700'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <FolderKanban className="h-3.5 w-3.5" />
              Cases
            </Link>

            <Link
              to="/campaigns"
              id="nav-campaigns-link"
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                location.pathname.startsWith('/campaigns')
                  ? 'bg-amber-950 text-amber-300 border border-amber-800'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <Flame className="h-3.5 w-3.5 text-amber-400" />
              Campaigns
            </Link>

            <Link
              to="/graph"
              id="nav-graph-link"
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                location.pathname === '/graph'
                  ? 'bg-cyan-950 text-cyan-300 border border-cyan-800'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <Share2 className="h-3.5 w-3.5 text-cyan-400" />
              Threat Graph
            </Link>

            <Link
              to="/health"
              id="nav-health-link"
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                location.pathname === '/health'
                  ? 'bg-slate-800 text-white border border-slate-700'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <Activity className="h-3.5 w-3.5" />
              System Health
            </Link>
          </nav>
        </div>

        {user && (
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800">
              <User className="h-3.5 w-3.5 text-cyan-400" />
              <span className="text-xs font-medium text-slate-200">{user.username}</span>
              <span className="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-cyan-950 text-cyan-400 border border-cyan-900">
                {user.role}
              </span>
            </div>

            <button
              id="logout-btn"
              onClick={logout}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-rose-300 hover:bg-rose-950/30 border border-transparent hover:border-rose-900/50 transition-colors"
              title="Sign Out"
            >
              <LogOut className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
