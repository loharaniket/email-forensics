import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Shield, LayoutDashboard, Activity } from 'lucide-react';

export default function Navbar() {
  const location = useLocation();

  return (
    <header className="border-b border-[#334155] bg-[#1e293b] sticky top-0 z-50 px-6 py-3 shadow-md">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="h-9 w-9 rounded-lg bg-blue-950 border border-blue-800/60 flex items-center justify-center text-blue-400 group-hover:text-blue-300 transition">
              <Shield className="h-5 w-5" />
            </div>
            <div>
              <span className="text-base font-bold tracking-tight text-white block leading-tight">
                Email Forensics
              </span>
              <span className="text-xs text-slate-400 hidden sm:block">
                Version 2 Forensic Investigation
              </span>
            </div>
          </Link>

          <nav className="flex items-center gap-1 sm:gap-2">
            <Link
              to="/"
              id="nav-dashboard-link"
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                location.pathname === '/' || location.pathname.startsWith('/evidence')
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-300 hover:bg-[#334155] hover:text-white'
              }`}
            >
              <LayoutDashboard className="h-4 w-4" />
              <span>Dashboard</span>
            </Link>

            <Link
              to="/health"
              id="nav-health-link"
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                location.pathname === '/health'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-300 hover:bg-[#334155] hover:text-white'
              }`}
            >
              <Activity className="h-4 w-4" />
              <span>System Status</span>
            </Link>
          </nav>
        </div>
      </div>
    </header>
  );
}
