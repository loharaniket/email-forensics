import React, { useState, useEffect, useCallback } from 'react'
import Navbar from '../components/Navbar'
import { fetchHealth, truncateDatabaseAndEvidence } from '../api/health'
import {
  Activity,
  RefreshCw,
  Server,
  CheckCircle2,
  AlertTriangle,
  Clock,
  ChevronDown,
  ChevronUp,
  Trash2,
  Database,
  Layers
} from 'lucide-react'

export default function HealthPage() {
  const [healthData, setHealthData] = useState(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [latency, setLatency] = useState(null)
  const [lastChecked, setLastChecked] = useState(null)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false)

  const [isTruncating, setIsTruncating] = useState(false)
  const [truncateResult, setTruncateResult] = useState(null)
  const [truncateError, setTruncateError] = useState(null)

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
      setError(err.message || 'Cannot connect to backend service.')
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

  const handleTruncate = async () => {
    setIsTruncating(true)
    setTruncateError(null)
    setTruncateResult(null)
    try {
      const result = await truncateDatabaseAndEvidence()
      setTruncateResult(result)
      await checkStatus()
    } catch (err) {
      setTruncateError(err.message || 'Failed to truncate database and evidence.')
    } finally {
      setIsTruncating(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#F8FAFC] text-[#1E293B] flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6">
        <section className="bg-white rounded-xl border border-[#E2E8F0] p-6 md:p-8 shadow-sm">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-[#E2E8F0]">
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-[#0F172A]">
                System Status &amp; Database Health
              </h1>
              <p className="text-sm text-[#64748B] mt-1 max-w-xl">
                Real-time connection probe between React frontend, Spring Boot backend, and PostgreSQL (email_forensics_db).
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <button
                id="toggle-refresh-btn"
                onClick={() => setAutoRefresh(!autoRefresh)}
                className={`px-3.5 py-2 rounded-lg text-xs font-medium border transition-colors cursor-pointer ${
                  autoRefresh
                    ? 'bg-[#EFF6FF] border-[#BFDBFE] text-[#2563EB]'
                    : 'bg-white border-[#E2E8F0] text-[#64748B] hover:bg-slate-50'
                }`}
              >
                Auto refresh: {autoRefresh ? 'Active (5s)' : 'Paused'}
              </button>

              <button
                id="manual-refresh-btn"
                onClick={checkStatus}
                disabled={isLoading}
                className="flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-semibold bg-[#2563EB] hover:bg-blue-700 text-white transition-colors disabled:opacity-50 cursor-pointer shadow-sm"
              >
                <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
                <span>Probe Health</span>
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-6">
            <div className="bg-[#F8FAFC] p-5 rounded-xl border border-[#E2E8F0]">
              <div className="flex items-center justify-between text-xs text-[#64748B]">
                <span>Backend service</span>
                <Server className="h-4 w-4 text-[#64748B]" />
              </div>
              <div className="mt-3 flex items-center gap-2">
                {healthData && healthData.status === 'UP' ? (
                  <>
                    <span className="h-2.5 w-2.5 rounded-full bg-[#10B981]"></span>
                    <span className="font-semibold text-[#0F172A] text-sm">
                      Online (UP)
                    </span>
                  </>
                ) : (
                  <>
                    <span className="h-2.5 w-2.5 rounded-full bg-[#EF4444]"></span>
                    <span className="font-semibold text-[#EF4444] text-sm">
                      Disconnected
                    </span>
                  </>
                )}
              </div>
            </div>

            <div className="bg-[#F8FAFC] p-5 rounded-xl border border-[#E2E8F0]">
              <div className="flex items-center justify-between text-xs text-[#64748B]">
                <span>PostgreSQL database</span>
                <Database className="h-4 w-4 text-[#64748B]" />
              </div>
              <div className="mt-3 flex items-center gap-2">
                {healthData && healthData.database === 'CONNECTED' ? (
                  <>
                    <span className="h-2.5 w-2.5 rounded-full bg-[#10B981]"></span>
                    <span className="font-semibold text-[#0F172A] text-sm">
                      Connected ({healthData.databaseName || 'email_forensics_db'})
                    </span>
                  </>
                ) : (
                  <>
                    <span className="h-2.5 w-2.5 rounded-full bg-[#EF4444]"></span>
                    <span className="font-semibold text-[#EF4444] text-sm">
                      Disconnected
                    </span>
                  </>
                )}
              </div>
            </div>

            <div className="bg-[#F8FAFC] p-5 rounded-xl border border-[#E2E8F0]">
              <div className="flex items-center justify-between text-xs text-[#64748B]">
                <span>Response time</span>
                <Clock className="h-4 w-4 text-[#64748B]" />
              </div>
              <div className="mt-3">
                <span className="text-xl font-bold font-mono text-[#0F172A]">
                  {latency !== null ? `${latency} ms` : '—'}
                </span>
              </div>
            </div>

            <div className="bg-[#F8FAFC] p-5 rounded-xl border border-[#E2E8F0]">
              <div className="flex items-center justify-between text-xs text-[#64748B]">
                <span>Active V2 tables</span>
                <Layers className="h-4 w-4 text-[#64748B]" />
              </div>
              <div className="mt-3">
                <span className="text-sm font-semibold text-[#0F172A]">
                  {healthData && healthData.tables ? healthData.tables.join(', ') : 'emails, indicators'}
                </span>
              </div>
            </div>
          </div>

          <div className="mt-6">
            {healthData && healthData.status === 'UP' ? (
              <div
                id="health-success-banner"
                className="bg-[#F0FDF4] border border-[#DCFCE7] rounded-xl p-5"
              >
                <div className="flex items-start gap-3">
                  <CheckCircle2 className="h-5 w-5 text-[#10B981] mt-0.5 shrink-0" />
                  <div className="space-y-1 flex-1">
                    <h3 className="text-sm font-semibold text-[#047857]">
                      Phase 1 Foundation Operational
                    </h3>
                    <p className="text-xs text-[#065F46]">
                      Spring Boot backend on port 8080 and PostgreSQL database ({healthData.databaseName}) are healthy. The two tables (emails: {healthData.emailCount || 0}, indicators: {healthData.indicatorCount || 0}) are verified.
                    </p>
                  </div>
                </div>
              </div>
            ) : (
              <div
                id="health-error-banner"
                className="bg-[#FEF2F2] border border-[#FEE2E2] rounded-xl p-5"
              >
                <div className="flex items-start gap-3">
                  <AlertTriangle className="h-5 w-5 text-[#EF4444] mt-0.5 shrink-0" />
                  <div className="space-y-1 flex-1">
                    <h3 className="text-sm font-semibold text-[#B91C1C]">
                      Cannot connect to backend service
                    </h3>
                    <p className="text-xs text-[#B91C1C]">{error || 'Service is unreachable'}</p>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="mt-8 pt-6 border-t border-[#E2E8F0]">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 bg-[#FEF2F2] border border-[#FEE2E2] rounded-xl">
              <div className="space-y-1">
                <div className="flex items-center gap-2 text-sm font-semibold text-[#991B1B]">
                  <Trash2 className="h-4 w-4 text-[#DC2626]" />
                  <span>Reset database tables &amp; evidence vault</span>
                </div>
                <p className="text-xs text-[#991B1B]/80 max-w-xl">
                  Truncates both Version 2 tables (emails, indicators) and clears stored evidence files under the UUID storage root.
                </p>
              </div>

              <button
                id="truncate-all-btn"
                onClick={handleTruncate}
                disabled={isTruncating || isLoading}
                className="shrink-0 flex items-center gap-2 px-4 py-2.5 rounded-lg text-xs font-semibold bg-[#DC2626] hover:bg-red-700 text-white transition-colors disabled:opacity-50 cursor-pointer shadow-sm"
              >
                {isTruncating ? (
                  <>
                    <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                    <span>Truncating tables &amp; evidence...</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="h-3.5 w-3.5" />
                    <span>Truncate database tables &amp; evidence</span>
                  </>
                )}
              </button>
            </div>

            {truncateResult && (
              <div
                id="truncate-success-banner"
                className="mt-4 bg-[#F0FDF4] border border-[#DCFCE7] rounded-xl p-4 flex items-start gap-3"
              >
                <CheckCircle2 className="h-5 w-5 text-[#10B981] mt-0.5 shrink-0" />
                <div className="space-y-1">
                  <h4 className="text-sm font-semibold text-[#047857]">
                    Database &amp; evidence truncated successfully
                  </h4>
                  <p className="text-xs text-[#065F46]">
                    {truncateResult.message} Cleared tables: {truncateResult.truncatedTables?.join(', ')}.
                  </p>
                </div>
              </div>
            )}

            {truncateError && (
              <div
                id="truncate-error-banner"
                className="mt-4 bg-[#FEF2F2] border border-[#FEE2E2] rounded-xl p-4 flex items-start gap-3"
              >
                <AlertTriangle className="h-5 w-5 text-[#EF4444] mt-0.5 shrink-0" />
                <div className="space-y-1">
                  <h4 className="text-sm font-semibold text-[#B91C1C]">
                    Truncate failed
                  </h4>
                  <p className="text-xs text-[#B91C1C]">{truncateError}</p>
                </div>
              </div>
            )}
          </div>

          <div className="mt-8 pt-4 border-t border-[#E2E8F0]">
            <button
              onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
              className="flex items-center justify-between w-full text-xs font-medium text-[#64748B] hover:text-[#0F172A] cursor-pointer"
            >
              <span>Technical connection details</span>
              {showTechnicalDetails ? (
                <ChevronUp className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
            </button>

            {showTechnicalDetails && (
              <div className="mt-3 p-4 bg-[#F8FAFC] rounded-lg border border-[#E2E8F0] font-mono text-xs text-[#0F172A] space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[#64748B]">Endpoint:</span>
                  <span>GET /api/health</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-[#64748B]">Payload:</span>
                  <span>{JSON.stringify(healthData || { error })}</span>
                </div>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  )
}
