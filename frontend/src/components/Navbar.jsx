import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Shield, FolderKanban, Activity, LogOut, User } from 'lucide-react'

export default function Navbar() {
  const { user, logout } = useAuth()
  const location = useLocation()

  return (
    <header className="border-b border-[#E4E7EC] bg-white sticky top-0 z-50 px-6 py-3">
      <div className="max-w-6xl mx-auto flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link to="/cases" className="flex items-center gap-2.5">
            <div className="h-9 w-9 rounded-lg bg-[#EFF6FF] border border-blue-100 flex items-center justify-center text-[#2563EB]">
              <Shield className="h-5 w-5" />
            </div>
            <div>
              <span className="text-base font-semibold tracking-tight text-[#202938] block leading-tight">
                Email Forensics
              </span>
              <span className="text-xs text-[#667085] hidden sm:block">
                Email investigation workspace
              </span>
            </div>
          </Link>

          <nav className="flex items-center gap-1 sm:gap-2">
            <Link
              to="/cases"
              id="nav-cases-link"
              className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                location.pathname.startsWith('/cases')
                  ? 'bg-[#EFF6FF] text-[#2563EB]'
                  : 'text-[#667085] hover:text-[#202938] hover:bg-slate-50'
              }`}
            >
              <FolderKanban className="h-4 w-4" />
              <span>Cases</span>
            </Link>

            <Link
              to="/health"
              id="nav-health-link"
              className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                location.pathname === '/health'
                  ? 'bg-[#EFF6FF] text-[#2563EB]'
                  : 'text-[#667085] hover:text-[#202938] hover:bg-slate-50'
              }`}
            >
              <Activity className="h-4 w-4" />
              <span>Service status</span>
            </Link>
          </nav>
        </div>

        {user && (
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 bg-[#F5F6F8] px-3 py-1.5 rounded-lg border border-[#E4E7EC]">
              <User className="h-3.5 w-3.5 text-[#667085]" />
              <span className="text-xs font-medium text-[#202938]">{user.username}</span>
              {user.role && (
                <span className="text-[11px] px-1.5 py-0.5 rounded bg-white text-[#667085] border border-[#E4E7EC] font-medium">
                  {user.role}
                </span>
              )}
            </div>

            <button
              id="logout-btn"
              onClick={logout}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-[#667085] hover:text-[#B42318] hover:bg-red-50 border border-transparent hover:border-red-200 transition-colors cursor-pointer"
            >
              <LogOut className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Sign out</span>
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
