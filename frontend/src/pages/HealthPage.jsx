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
  Trash2
} from 'lucide-react'

export default function HealthPage() {
  const [healthData, setHealthData] = useState(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [latency, setLatency] = useState(null)
  const [lastChecked, setLastChecked] = useState(null)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false)

  // Database truncation states
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
      setTruncateError(
        err.response?.data?.message || err.message || 'Failed to truncate database and evidence.'
      )
    } finally {
      setIsTruncating(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6">
        <section className="bg-white rounded-xl border border-[#E4E7EC] p-6 md:p-8">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-[#E4E7EC]">
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-[#202938]">
                Service status
              </h1>
              <p className="text-sm text-[#202938] mt-1 max-w-xl">
                Check whether the application can connect to its backend service.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <button
                id="toggle-refresh-btn"
                onClick={() => setAutoRefresh(!autoRefresh)}
                className={`px-3.5 py-2 rounded-lg text-xs font-medium border transition-colors cursor-pointer ${
                  autoRefresh
                    ? 'bg-[#EFF6FF] border-blue-200 text-[#2563EB]'
                    : 'bg-white border-[#E4E7EC] text-[#202938] hover:bg-slate-50'
                }`}
              >
                Auto-refresh: {autoRefresh ? 'On (5s)' : 'Off'}
              </button>

              <button
                id="refresh-health-btn"
                onClick={checkStatus}
                disabled={isLoading}
                className="flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors disabled:opacity-50 cursor-pointer"
              >
                <RefreshCw
                  className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`}
                />
                <span>Check connection</span>
              </button>

              <button
                id="truncate-db-action-btn"
                onClick={handleTruncate}
                disabled={isTruncating || isLoading}
                className="flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-medium bg-[#DC2626] hover:bg-red-700 text-white transition-colors disabled:opacity-50 cursor-pointer"
                title="Truncate all database tables and all vaulted evidence files in one click"
              >
                {isTruncating ? (
                  <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                ) : (
                  <Trash2 className="h-3.5 w-3.5" />
                )}
                <span>{isTruncating ? 'Truncating...' : 'Truncate all data'}</span>
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
            <div className="bg-[#F5F6F8] p-5 rounded-xl border border-[#E4E7EC]">
              <div className="flex items-center justify-between text-xs text-[#202938]">
                <span>Connection status</span>
                <Server className="h-4 w-4 text-[#202938]" />
              </div>
              <div className="mt-3 flex items-center gap-2">
                {healthData ? (
                  <>
                    <span className="inline-flex rounded-full h-2.5 w-2.5 bg-[#15803D]"></span>
                    <span className="font-semibold text-[#15803D] text-sm">
                      Connected ({healthData.status?.toUpperCase() || 'OK'})
                    </span>
                  </>
                ) : (
                  <>
                    <span className="h-2.5 w-2.5 rounded-full bg-[#B42318]"></span>
                    <span className="font-semibold text-[#B42318] text-sm">
                      Disconnected
                    </span>
                  </>
                )}
              </div>
            </div>

            <div className="bg-[#F5F6F8] p-5 rounded-xl border border-[#E4E7EC]">
              <div className="flex items-center justify-between text-xs text-[#202938]">
                <span>Response time</span>
                <Clock className="h-4 w-4 text-[#202938]" />
              </div>
              <div className="mt-3">
                <span className="text-xl font-bold font-mono text-[#202938]">
                  {latency !== null ? `${latency} ms` : '—'}
                </span>
              </div>
            </div>

            <div className="bg-[#F5F6F8] p-5 rounded-xl border border-[#E4E7EC]">
              <div className="flex items-center justify-between text-xs text-[#202938]">
                <span>Last checked</span>
                <Activity className="h-4 w-4 text-[#202938]" />
              </div>
              <div className="mt-3">
                <span className="text-sm font-medium text-[#202938]">
                  {lastChecked || 'Checking...'}
                </span>
              </div>
            </div>
          </div>

          <div className="mt-6">
            {healthData ? (
              <div
                id="health-success-banner"
                className="bg-[#F0FDF4] border border-[#DCFCE7] rounded-xl p-5"
              >
                <div className="flex items-start gap-3">
                  <CheckCircle2 className="h-5 w-5 text-[#15803D] mt-0.5 shrink-0" />
                  <div className="space-y-1 flex-1">
                    <h3 className="text-sm font-semibold text-[#15803D]">
                      Backend service connected successfully
                    </h3>
                    <p className="text-xs text-[#202938]">
                      All backend services are responding normally.
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
                  <AlertTriangle className="h-5 w-5 text-[#B42318] mt-0.5 shrink-0" />
                  <div className="space-y-1 flex-1">
                    <h3 className="text-sm font-semibold text-[#B42318]">
                      Cannot connect to backend service
                    </h3>
                    <p className="text-xs text-[#B42318]">{error}</p>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Dedicated Truncate Database & Evidence Section */}
          <div className="mt-8 pt-6 border-t border-[#E4E7EC]">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 bg-[#FEF2F2] border border-[#FEE2E2] rounded-xl">
              <div className="space-y-1">
                <div className="flex items-center gap-2 text-sm font-semibold text-[#991B1B]">
                  <Trash2 className="h-4 w-4 text-[#DC2626]" />
                  <span>Reset database & vaulted evidence</span>
                </div>
                <p className="text-xs text-[#991B1B]/80 max-w-xl">
                  Truncates all 13 database tables (cases, emails, analysis runs, results) and completely wipes all stored evidence files from the vault in one click. Default analyst and admin accounts are immediately preserved.
                </p>
              </div>

              <button
                id="truncate-all-btn"
                onClick={handleTruncate}
                disabled={isTruncating || isLoading}
                className="shrink-0 flex items-center gap-2 px-4 py-2.5 rounded-lg text-xs font-semibold bg-[#DC2626] hover:bg-red-700 text-white transition-colors disabled:opacity-50 cursor-pointer"
              >
                {isTruncating ? (
                  <>
                    <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                    <span>Truncating all tables & evidence...</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="h-3.5 w-3.5" />
                    <span>Truncate all database tables & evidence</span>
                  </>
                )}
              </button>
            </div>

            {truncateResult && (
              <div
                id="truncate-success-banner"
                className="mt-4 bg-[#F0FDF4] border border-[#DCFCE7] rounded-xl p-4 flex items-start gap-3"
              >
                <CheckCircle2 className="h-5 w-5 text-[#15803D] mt-0.5 shrink-0" />
                <div className="space-y-1">
                  <h4 className="text-sm font-semibold text-[#15803D]">
                    Database & evidence truncated successfully
                  </h4>
                  <p className="text-xs text-[#166534]">
                    {truncateResult.message} Cleared {truncateResult.truncatedTables?.length || 13} database tables and removed {truncateResult.evidenceFilesDeleted} vault evidence files.
                  </p>
                </div>
              </div>
            )}

            {truncateError && (
              <div
                id="truncate-error-banner"
                className="mt-4 bg-[#FEF2F2] border border-[#FEE2E2] rounded-xl p-4 flex items-start gap-3"
              >
                <AlertTriangle className="h-5 w-5 text-[#B42318] mt-0.5 shrink-0" />
                <div className="space-y-1">
                  <h4 className="text-sm font-semibold text-[#B42318]">
                    Truncate failed
                  </h4>
                  <p className="text-xs text-[#B42318]">{truncateError}</p>
                </div>
              </div>
            )}
          </div>

          {/* Expandable technical details */}
          <div className="mt-8 pt-4 border-t border-[#E4E7EC]">
            <button
              onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
              className="flex items-center justify-between w-full text-xs font-medium text-[#202938] cursor-pointer"
            >
              <span>Technical connection details</span>
              {showTechnicalDetails ? (
                <ChevronUp className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
            </button>

            {showTechnicalDetails && (
              <div className="mt-3 p-4 bg-[#F5F6F8] rounded-lg border border-[#E4E7EC] font-mono text-xs text-[#202938] space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[#202938]">Endpoint:</span>
                  <span>GET /api/v1/health</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-[#202938]">Payload:</span>
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
