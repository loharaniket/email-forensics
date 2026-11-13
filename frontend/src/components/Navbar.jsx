import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Shield, LayoutDashboard, Activity } from 'lucide-react'

export default function Navbar() {
  const location = useLocation()

  return (
    <header className="border-b border-[#E4E7EC] bg-white sticky top-0 z-50 px-6 py-3">
      <div className="max-w-6xl mx-auto flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link to="/" className="flex items-center gap-2.5">
            <div className="h-9 w-9 rounded-lg bg-[#EFF6FF] border border-blue-100 flex items-center justify-center text-[#2563EB]">
              <Shield className="h-5 w-5" />
            </div>
            <div>
              <span className="text-base font-semibold tracking-tight text-[#202938] block leading-tight">
                Email Forensics
              </span>
              <span className="text-xs text-[#202938] hidden sm:block">
                Automated forensic investigation dashboard
              </span>
            </div>
          </Link>

          <nav className="flex items-center gap-1 sm:gap-2">
            <Link
              to="/"
              id="nav-dashboard-link"
              className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                location.pathname === '/' || location.pathname.startsWith('/evidence')
                  ? 'bg-[#EFF6FF] text-[#2563EB]'
                  : 'text-[#202938] hover:bg-slate-50'
              }`}
            >
              <LayoutDashboard className="h-4 w-4" />
              <span>Dashboard</span>
            </Link>

            <Link
              to="/health"
              id="nav-health-link"
              className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                location.pathname === '/health'
                  ? 'bg-[#EFF6FF] text-[#2563EB]'
                  : 'text-[#202938] hover:bg-slate-50'
              }`}
            >
              <Activity className="h-4 w-4" />
              <span>System Status</span>
            </Link>
          </nav>
        </div>
      </div>
    </header>
  )
}
