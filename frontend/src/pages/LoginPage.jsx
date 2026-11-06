import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Shield, Lock, User, AlertCircle, Activity } from 'lucide-react'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
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
        setError('Failed to sign in. Please check your username and password.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col justify-between p-6">
      <header className="max-w-6xl w-full mx-auto flex items-center justify-between py-2">
        <div className="flex items-center gap-2.5">
          <div className="h-8 w-8 rounded-lg bg-[#EFF6FF] border border-blue-100 flex items-center justify-center text-[#2563EB]">
            <Shield className="h-5 w-5" />
          </div>
          <span className="text-sm font-semibold tracking-tight text-[#202938]">
            Email Forensics
          </span>
        </div>

        <Link
          to="/health"
          id="login-health-check-link"
          className="flex items-center gap-1.5 text-xs text-[#667085] hover:text-[#2563EB] transition-colors"
        >
          <Activity className="h-3.5 w-3.5" />
          <span>Service status</span>
        </Link>
      </header>

      <main className="max-w-md w-full mx-auto my-auto py-8">
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-8">
          <div className="text-center mb-6">
            <div className="inline-flex h-12 w-12 rounded-full bg-[#EFF6FF] items-center justify-center mb-3 text-[#2563EB]">
              <Lock className="h-6 w-6" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-[#202938]">
              Sign in
            </h1>
            <p className="text-sm text-[#667085] mt-1">
              Sign in to review cases and email evidence.
            </p>
          </div>

          {error && (
            <div
              id="login-error-alert"
              className="mb-5 p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#B42318] flex items-start gap-2.5"
            >
              <AlertCircle className="h-4 w-4 text-[#B42318] shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-[#202938] mb-1.5">
                Username
              </label>
              <div className="relative">
                <User className="h-4 w-4 text-[#98A2B3] absolute left-3 top-3" />
                <input
                  id="username-input"
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Enter username"
                  className="w-full bg-white border border-[#E4E7EC] rounded-lg pl-9 pr-3.5 py-2.5 text-sm text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB] transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-[#202938] mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="h-4 w-4 text-[#98A2B3] absolute left-3 top-3" />
                <input
                  id="password-input"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  className="w-full bg-white border border-[#E4E7EC] rounded-lg pl-9 pr-3.5 py-2.5 text-sm text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB] transition-colors"
                />
              </div>
            </div>

            <button
              id="login-submit-btn"
              type="submit"
              disabled={isSubmitting}
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg text-sm font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors disabled:opacity-50 mt-6 cursor-pointer"
            >
              <span>{isSubmitting ? 'Signing in...' : 'Sign in'}</span>
            </button>
          </form>
        </div>
      </main>

      <footer className="text-center py-4 text-xs text-[#667085]">
        Email Forensics
      </footer>
    </div>
  )
}
