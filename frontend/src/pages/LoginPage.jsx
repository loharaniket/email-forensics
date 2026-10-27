import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ShieldCheck, Lock, User, AlertCircle, ArrowRight, Activity } from 'lucide-react'

export default function LoginPage() {
  const [username, setUsername] = useState('analyst')
  const [password, setPassword] = useState('Analyst@123')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await login(username, password)
      navigate('/cases')
    } catch (err) {
      if (err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message)
      } else {
        setError('Failed to authenticate. Ensure the backend is reachable.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between p-6">
      <div className="max-w-6xl w-full mx-auto flex items-center justify-between py-2">
        <div className="flex items-center gap-2.5">
          <div className="h-8 w-8 rounded-lg bg-gradient-to-tr from-cyan-600 to-blue-600 flex items-center justify-center">
            <ShieldCheck className="h-5 w-5 text-white" />
          </div>
          <span className="text-sm font-semibold tracking-tight text-white">
            Email Forensics Lab
          </span>
        </div>

        <Link
          to="/health"
          id="login-health-check-link"
          className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-cyan-400 transition-colors"
        >
          <Activity className="h-3.5 w-3.5" />
          Backend Gateway Status
        </Link>
      </div>

      <div className="max-w-md w-full mx-auto my-auto">
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-8 shadow-2xl backdrop-blur-md">
          <div className="text-center mb-6">
            <div className="inline-flex h-12 w-12 rounded-2xl bg-cyan-950 border border-cyan-800 items-center justify-center mb-3 text-cyan-400">
              <Lock className="h-6 w-6" />
            </div>
            <h1 className="text-xl font-bold tracking-tight text-white">
              Analyst Access Portal
            </h1>
            <p className="text-xs text-slate-400 mt-1">
              Authenticate to access forensic cases and investigation workflows
            </p>
          </div>

          {error && (
            <div
              id="login-error-alert"
              className="mb-5 p-3 rounded-xl bg-rose-950/40 border border-rose-800/80 text-xs text-rose-300 flex items-start gap-2.5"
            >
              <AlertCircle className="h-4 w-4 text-rose-400 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">
                Analyst Username
              </label>
              <div className="relative">
                <User className="h-4 w-4 text-slate-500 absolute left-3 top-3" />
                <input
                  id="username-input"
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="analyst"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="h-4 w-4 text-slate-500 absolute left-3 top-3" />
                <input
                  id="password-input"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 transition-colors"
                />
              </div>
            </div>

            <button
              id="login-submit-btn"
              type="submit"
              disabled={isSubmitting}
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs font-medium bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white shadow-lg shadow-cyan-600/20 transition-all disabled:opacity-50 mt-6"
            >
              <span>{isSubmitting ? 'Verifying Credentials...' : 'Sign In as Analyst'}</span>
              <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-slate-800 text-center">
            <span className="text-[11px] text-slate-400">Default Seed Credentials:</span>
            <div className="mt-1.5 font-mono text-[11px] bg-slate-950/80 px-2.5 py-1.5 rounded-lg border border-slate-800 text-slate-300 flex justify-center items-center gap-2">
              <span className="text-cyan-400">analyst</span>
              <span className="text-slate-600">/</span>
              <span className="text-slate-400">Analyst@123</span>
            </div>
          </div>
        </div>
      </div>

      <footer className="text-center py-2 text-[11px] text-slate-500 font-mono">
        Digital Email Forensics &amp; Threat Correlation Platform
      </footer>
    </div>
  )
}
