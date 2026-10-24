import React, { useState, useEffect, useCallback } from 'react'
import {
  ShieldCheck,
  ShieldAlert,
  Activity,
  RefreshCw,
  Server,
  Radio,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Terminal,
  ArrowRight,
  Layers
} from 'lucide-react'
import { fetchHealth } from './api/health'

export default function App() {
  const [healthData, setHealthData] = useState(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [latency, setLatency] = useState(null)
  const [lastChecked, setLastChecked] = useState(null)
  const [autoRefresh, setAutoRefresh] = useState(true)

  const checkStatus = useCallback(async () => {
    setIsLoading(true)
    const startTime = performance.now()
    try {
      const data = await fetchHealth()
      const endTime = performance.now()
      setLatency(Math.round(endTime - startTime))
      setHealthData(data)
      setError(null)
      setLastChecked(new Date().toLocaleTimeString())
    } catch (err) {
      const endTime = performance.now()
      setLatency(Math.round(endTime - startTime))
      setHealthData(null)
      setError(err.message || 'Failed to connect to backend server')
      setLastChecked(new Date().toLocaleTimeString())
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    checkStatus()
  }, [checkStatus])

  useEffect(() => {
    if (!autoRefresh) return
    const interval = setInterval(() => {
      checkStatus()
    }, 5000)
    return () => clearInterval(interval)
  }, [autoRefresh, checkStatus])

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-cyan-500 selection:text-black">
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur-md sticky top-0 z-50 px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-cyan-600 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
              <ShieldCheck className="h-6 w-6 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold tracking-tight text-white">
                  Email Forensics &amp; Threat Correlation
                </h1>
                <span className="text-xs px-2 py-0.5 rounded-full bg-cyan-950 text-cyan-400 border border-cyan-800 font-mono">
                  v0.0.1-dev
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Phase 0 — Setup and Stable Skeleton
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 bg-slate-900 px-3 py-1.5 rounded-lg border border-slate-800">
              <Radio
                className={`h-4 w-4 ${
                  healthData
                    ? 'text-emerald-400 animate-pulse'
                    : 'text-rose-500'
                }`}
              />
              <span className="text-xs font-mono">
                {healthData ? 'GATEWAY: ONLINE' : 'GATEWAY: OFFLINE'}
              </span>
            </div>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-8">
        <section className="bg-gradient-to-b from-slate-900/80 to-slate-900/40 rounded-2xl border border-slate-800 p-6 md:p-8 shadow-xl">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-6 border-b border-slate-800">
            <div>
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-md bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-medium mb-3">
                <Activity className="h-3.5 w-3.5" />
                Backend Health Handshake
              </div>
              <h2 className="text-2xl font-semibold tracking-tight text-white">
                Integration Health Status
              </h2>
              <p className="text-sm text-slate-400 mt-1 max-w-xl">
                Validating connectivity between the Vite React frontend and the Spring Boot 4.1.x REST API through the reverse proxy on <code className="text-cyan-400 bg-slate-950 px-1.5 py-0.5 rounded border border-slate-800">/api/v1/health</code>.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                id="toggle-refresh-btn"
                onClick={() => setAutoRefresh(!autoRefresh)}
                className={`px-3 py-2 rounded-lg text-xs font-medium border transition-colors ${
                  autoRefresh
                    ? 'bg-cyan-950/60 border-cyan-800 text-cyan-300'
                    : 'bg-slate-800/80 border-slate-700 text-slate-400'
                }`}
              >
                Auto-Refresh (5s): {autoRefresh ? 'ON' : 'OFF'}
              </button>

              <button
                id="refresh-health-btn"
                onClick={checkStatus}
                disabled={isLoading}
                className="flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-medium bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white shadow-md shadow-cyan-600/20 transition-all disabled:opacity-50"
              >
                <RefreshCw
                  className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`}
                />
                Ping Endpoint
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800/80">
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span>API Health Response</span>
                <Server className="h-4 w-4 text-slate-500" />
              </div>
              <div className="mt-3 flex items-center gap-2">
                {healthData ? (
                  <>
                    <span className="relative flex h-3 w-3">
                      <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                      <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
                    </span>
                    <span className="font-semibold text-emerald-400 font-mono text-sm">
                      STATUS: {healthData.status?.toUpperCase() || 'OK'}
                    </span>
                  </>
                ) : (
                  <>
                    <span className="h-3 w-3 rounded-full bg-rose-500"></span>
                    <span className="font-semibold text-rose-400 font-mono text-sm">
                      CONNECTION FAILED
                    </span>
                  </>
                )}
              </div>
            </div>

            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800/80">
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span>Round-Trip Latency</span>
                <Clock className="h-4 w-4 text-slate-500" />
              </div>
              <div className="mt-3">
                <span className="text-xl font-bold font-mono text-slate-200">
                  {latency !== null ? `${latency} ms` : '—'}
                </span>
                <span className="text-xs text-slate-500 ml-2 font-mono">
                  HTTP/1.1
                </span>
              </div>
            </div>

            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800/80">
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span>Last Verified At</span>
                <Activity className="h-4 w-4 text-slate-500" />
              </div>
              <div className="mt-3">
                <span className="text-base font-medium font-mono text-slate-300">
                  {lastChecked || 'Checking...'}
                </span>
              </div>
            </div>
          </div>

          <div className="mt-6">
            {healthData ? (
              <div
                id="health-success-banner"
                className="bg-emerald-950/20 border border-emerald-800/50 rounded-xl p-5"
              >
                <div className="flex items-start gap-3">
                  <CheckCircle2 className="h-5 w-5 text-emerald-400 mt-0.5 shrink-0" />
                  <div className="space-y-2 flex-1">
                    <h3 className="text-sm font-semibold text-emerald-300">
                      Spring Boot Backend Responding Successfully
                    </h3>
                    <p className="text-xs text-emerald-400/80">
                      The backend service on port 8080 replied with HTTP 200 OK. The proxy configuration in Vite is routing calls from frontend on port 5173 seamlessly.
                    </p>
                    <div className="bg-slate-950/80 rounded-lg p-3 border border-emerald-900/60 font-mono text-xs text-slate-300 flex items-center justify-between">
                      <span>GET /api/v1/health</span>
                      <span className="text-emerald-400">
                        {JSON.stringify(healthData)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div
                id="health-error-banner"
                className="bg-rose-950/30 border border-rose-800/60 rounded-xl p-5"
              >
                <div className="flex items-start gap-3">
                  <AlertTriangle className="h-5 w-5 text-rose-400 mt-0.5 shrink-0" />
                  <div className="space-y-2 flex-1">
                    <h3 className="text-sm font-semibold text-rose-300">
                      Cannot Connect to Backend Gateway
                    </h3>
                    <p className="text-xs text-rose-300/80">
                      {error}
                    </p>
                    <div className="bg-slate-950/90 rounded-lg p-3 border border-rose-900/60 font-mono text-xs text-rose-400">
                      Target: http://localhost:8080/api/v1/health — Verify Spring Boot process is running.
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </section>

        <section className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <Layers className="h-5 w-5 text-cyan-400" />
              <h3 className="text-base font-semibold text-white">
                Phase 0 Milestone Deliverables
              </h3>
            </div>
            <ul className="space-y-3 text-xs">
              <li className="flex items-center gap-2.5 text-slate-300">
                <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>Spring Boot 4.1.x Maven backend with Java 21 LTS</span>
              </li>
              <li className="flex items-center gap-2.5 text-slate-300">
                <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>React 19.x frontend with Vite 8 &amp; Tailwind CSS</span>
              </li>
              <li className="flex items-center gap-2.5 text-slate-300">
                <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>Vite reverse proxy configured for <code className="text-cyan-400">/api</code> to <code className="text-cyan-400">http://localhost:8080</code></span>
              </li>
              <li className="flex items-center gap-2.5 text-slate-300">
                <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>Spring Web endpoint <code className="text-cyan-400">GET /api/v1/health</code> returning JSON</span>
              </li>
              <li className="flex items-center gap-2.5 text-slate-300">
                <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>Resilient frontend connection handler with error display</span>
              </li>
            </ul>
          </div>

          <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <Terminal className="h-5 w-5 text-blue-400" />
              <h3 className="text-base font-semibold text-white">
                Upcoming Roadmap: Phase 1
              </h3>
            </div>
            <div className="space-y-3 text-xs text-slate-400">
              <p>
                Following the implementation specification, the next sequential phase encompasses:
              </p>
              <div className="bg-slate-950/70 p-3 rounded-xl border border-slate-800 space-y-2">
                <div className="flex items-center gap-2 text-slate-200 font-medium">
                  <ArrowRight className="h-3.5 w-3.5 text-cyan-400" />
                  <span>PostgreSQL 16 &amp; Flyway Migration (V1 Core Schema)</span>
                </div>
                <div className="flex items-center gap-2 text-slate-200 font-medium">
                  <ArrowRight className="h-3.5 w-3.5 text-cyan-400" />
                  <span>Spring Security JWT authentication &amp; User entities</span>
                </div>
                <div className="flex items-center gap-2 text-slate-200 font-medium">
                  <ArrowRight className="h-3.5 w-3.5 text-cyan-400" />
                  <span>Case management endpoints (create, list, detail, notes)</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-800/80 bg-slate-950 py-4 px-6 text-xs text-slate-500">
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-2">
          <span>AI-Powered Email Forensic Investigation &amp; Threat Correlation</span>
          <span className="font-mono">Baseline: Spring Boot 4.1.1 · Java 21 · React 19 · Vite 8</span>
        </div>
      </footer>
    </div>
  )
}
