import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { getCasesApi, createCaseApi } from '../api/cases'
import {
  FolderKanban,
  Plus,
  Search,
  Filter,
  Clock,
  User,
  ArrowRight,
  ChevronLeft,
  ChevronRight,
  X,
  AlertCircle
} from 'lucide-react'

export default function CasesPage() {
  const [cases, setCases] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  const [isModalOpen, setIsModalOpen] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [newDescription, setNewDescription] = useState('')
  const [modalError, setModalError] = useState(null)
  const [isCreating, setIsCreating] = useState(false)

  const navigate = useNavigate()

  const fetchCases = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await getCasesApi(page, 10, statusFilter)
      setCases(data.content || [])
      setTotalPages(data.totalPages || 1)
      setTotalElements(data.totalElements || 0)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load investigation cases')
    } finally {
      setIsLoading(false)
    }
  }, [page, statusFilter])

  useEffect(() => {
    fetchCases()
  }, [fetchCases])

  const handleCreateCase = async (e) => {
    e.preventDefault()
    if (!newTitle.trim()) {
      setModalError('Case title is required')
      return
    }

    setIsCreating(true)
    setModalError(null)
    try {
      const created = await createCaseApi({
        title: newTitle.trim(),
        description: newDescription.trim() || null,
      })
      setIsModalOpen(false)
      setNewTitle('')
      setNewDescription('')
      fetchCases()
      navigate(`/cases/${created.id}`)
    } catch (err) {
      setModalError(err.response?.data?.message || 'Failed to create case')
    } finally {
      setIsCreating(false)
    }
  }

  const getStatusBadge = (status) => {
    switch (status) {
      case 'OPEN':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-cyan-950 text-cyan-400 border border-cyan-800">
            OPEN
          </span>
        )
      case 'IN_REVIEW':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-amber-950 text-amber-400 border border-amber-800">
            IN REVIEW
          </span>
        )
      case 'CLOSED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-slate-800 text-slate-400 border border-slate-700">
            CLOSED
          </span>
        )
      default:
        return <span>{status}</span>
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold tracking-tight text-white">
                Investigation Cases
              </h1>
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-900 text-slate-400 border border-slate-800 font-mono">
                {totalElements} total
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Active forensic case dossiers and correlated threat investigations
            </p>
          </div>

          <button
            id="create-case-btn"
            onClick={() => {
              setModalError(null)
              setIsModalOpen(true)
            }}
            className="flex items-center justify-center gap-2 px-4 py-2 rounded-xl text-xs font-medium bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white shadow-lg shadow-cyan-600/20 transition-all cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            <span>New Investigation Case</span>
          </button>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-1 bg-slate-900/80 p-1 rounded-xl border border-slate-800 w-full sm:w-auto">
            {['ALL', 'OPEN', 'IN_REVIEW', 'CLOSED'].map((st) => (
              <button
                key={st}
                id={`filter-${st.toLowerCase()}-btn`}
                onClick={() => {
                  setStatusFilter(st)
                  setPage(0)
                }}
                className={`flex-1 sm:flex-none px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
                  statusFilter === st
                    ? 'bg-slate-800 text-white shadow-sm border border-slate-700'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {st === 'ALL' ? 'All Cases' : st.replace('_', ' ')}
              </button>
            ))}
          </div>

          <div className="text-xs text-slate-500 font-mono">
            Showing Page {page + 1} of {Math.max(totalPages, 1)}
          </div>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800/80 text-xs text-rose-300 flex items-center gap-2">
            <AlertCircle className="h-4 w-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {isLoading ? (
          <div className="py-20 text-center text-slate-500 flex flex-col items-center gap-2">
            <FolderKanban className="h-8 w-8 text-cyan-500 animate-pulse" />
            <span className="text-xs font-mono">Loading cases...</span>
          </div>
        ) : cases.length === 0 ? (
          <div className="py-20 text-center rounded-2xl border border-dashed border-slate-800 bg-slate-900/20 p-8 space-y-3">
            <FolderKanban className="h-10 w-10 text-slate-600 mx-auto" />
            <h3 className="text-sm font-semibold text-slate-300">No Cases Found</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              {statusFilter !== 'ALL'
                ? `No investigation cases currently match status ${statusFilter}.`
                : 'No cases have been created yet. Click "New Investigation Case" to open your first investigation dossier.'}
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {cases.map((c) => (
              <div
                key={c.id}
                id={`case-card-${c.id}`}
                onClick={() => navigate(`/cases/${c.id}`)}
                className="group bg-slate-900/60 hover:bg-slate-900 border border-slate-800 hover:border-slate-700 rounded-xl p-5 transition-all shadow-md cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-2 flex-1">
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-mono font-semibold text-cyan-400">
                      {c.caseNumber}
                    </span>
                    {getStatusBadge(c.status)}
                    <span className="text-[11px] text-slate-500 flex items-center gap-1 font-mono">
                      <Clock className="h-3 w-3" />
                      {new Date(c.createdAt).toLocaleDateString()}
                    </span>
                  </div>

                  <h3 className="text-sm font-semibold text-white group-hover:text-cyan-300 transition-colors">
                    {c.title}
                  </h3>

                  {c.description && (
                    <p className="text-xs text-slate-400 line-clamp-1 max-w-2xl">
                      {c.description}
                    </p>
                  )}
                </div>

                <div className="flex items-center gap-4 pt-2 md:pt-0 border-t md:border-t-0 border-slate-800/60 justify-between md:justify-end">
                  <div className="flex items-center gap-1.5 text-xs text-slate-400 font-mono">
                    <User className="h-3.5 w-3.5 text-slate-500" />
                    <span>{c.createdByUsername || 'analyst'}</span>
                  </div>

                  <Link
                    to={`/cases/${c.id}`}
                    id={`inspect-case-btn-${c.id}`}
                    className="flex items-center gap-1 text-xs text-cyan-400 font-medium group-hover:translate-x-1 transition-transform"
                  >
                    <span>Inspect</span>
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-end gap-2 pt-4">
            <button
              id="prev-page-btn"
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-800"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="text-xs text-slate-400 px-3 font-mono">
              Page {page + 1} / {totalPages}
            </span>
            <button
              id="next-page-btn"
              onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
              disabled={page >= totalPages - 1}
              className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-800"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        )}
      </main>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl relative">
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute right-5 top-5 text-slate-500 hover:text-slate-300"
            >
              <X className="h-4 w-4" />
            </button>

            <div className="mb-5">
              <h2 className="text-lg font-bold text-white">Create Investigation Case</h2>
              <p className="text-xs text-slate-400 mt-1">
                Establish a new forensic dossier to correlate suspicious emails and technical artifacts.
              </p>
            </div>

            {modalError && (
              <div className="mb-4 p-3 rounded-xl bg-rose-950/40 border border-rose-800/80 text-xs text-rose-300 flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-rose-400 shrink-0" />
                <span>{modalError}</span>
              </div>
            )}

            <form onSubmit={handleCreateCase} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Case Title <span className="text-rose-400">*</span>
                </label>
                <input
                  id="case-title-input"
                  type="text"
                  required
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. Executive Payroll Spoofing Incident"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Investigation Description / Objectives
                </label>
                <textarea
                  id="case-desc-input"
                  rows={4}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="Provide incident context, affected departments, or threat details..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800"
                >
                  Cancel
                </button>
                <button
                  id="create-case-submit-btn"
                  type="submit"
                  disabled={isCreating}
                  className="px-4 py-2 rounded-xl text-xs font-medium bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white shadow-md shadow-cyan-600/20 disabled:opacity-50"
                >
                  {isCreating ? 'Creating Dossier...' : 'Create Case'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
