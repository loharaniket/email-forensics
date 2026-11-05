import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { getCasesApi, createCaseApi } from '../api/cases'
import {
  FolderKanban,
  Plus,
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
      setError(err.response?.data?.message || 'Failed to load cases.')
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
      setModalError('Case name is required.')
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
      setModalError(err.response?.data?.message || 'Failed to create case.')
    } finally {
      setIsCreating(false)
    }
  }

  const getStatusBadge = (status) => {
    switch (status) {
      case 'OPEN':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
            Open
          </span>
        )
      case 'IN_REVIEW':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-[#FFFBEB] text-[#B45309] border border-amber-200">
            In review
          </span>
        )
      case 'CLOSED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-[#667085] border border-[#E4E7EC]">
            Closed
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-[#667085]">
            {status}
          </span>
        )
    }
  }

  const filterOptions = [
    { key: 'ALL', label: 'All cases', id: 'filter-all-btn' },
    { key: 'OPEN', label: 'Open', id: 'filter-open-btn' },
    { key: 'IN_REVIEW', label: 'In review', id: 'filter-in_review-btn' },
    { key: 'CLOSED', label: 'Closed', id: 'filter-closed-btn' },
  ]

  return (
    <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#E4E7EC]">
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-2xl font-bold tracking-tight text-[#202938]">
                Cases
              </h1>
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-100 text-[#667085] border border-[#E4E7EC] font-medium">
                {totalElements} total
              </span>
            </div>
            <p className="text-sm text-[#667085] mt-1">
              Create cases and review suspicious emails.
            </p>
          </div>

          <button
            id="create-case-btn"
            onClick={() => {
              setModalError(null)
              setIsModalOpen(true)
            }}
            className="flex items-center justify-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer shrink-0"
          >
            <Plus className="h-4 w-4" />
            <span>New case</span>
          </button>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-1 bg-white p-1 rounded-lg border border-[#E4E7EC] w-full sm:w-auto">
            {filterOptions.map((opt) => (
              <button
                key={opt.key}
                id={opt.id}
                onClick={() => {
                  setStatusFilter(opt.key)
                  setPage(0)
                }}
                className={`flex-1 sm:flex-none px-3 py-1.5 rounded-md text-xs font-medium transition-colors cursor-pointer ${
                  statusFilter === opt.key
                    ? 'bg-[#EFF6FF] text-[#2563EB] font-semibold'
                    : 'text-[#667085] hover:text-[#202938] hover:bg-slate-50'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>

          <div className="text-xs text-[#667085]">
            Page {page + 1} of {Math.max(totalPages, 1)}
          </div>
        </div>

        {error && (
          <div className="p-4 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#B42318] flex items-center gap-2">
            <AlertCircle className="h-4 w-4 text-[#B42318] shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {isLoading ? (
          <div className="py-20 text-center text-[#667085] flex flex-col items-center gap-3">
            <FolderKanban className="h-8 w-8 text-[#2563EB] animate-pulse" />
            <span className="text-sm">Loading cases...</span>
          </div>
        ) : cases.length === 0 ? (
          <div className="py-16 text-center rounded-xl border border-[#E4E7EC] bg-white p-8 space-y-3">
            <FolderKanban className="h-10 w-10 text-[#98A2B3] mx-auto" />
            <h3 className="text-base font-semibold text-[#202938]">No cases yet.</h3>
            <p className="text-sm text-[#667085] max-w-sm mx-auto">
              Create a case to start investigating suspicious emails.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {cases.map((c) => (
              <div
                key={c.id}
                id={`case-card-${c.id}`}
                onClick={() => navigate(`/cases/${c.id}`)}
                className="bg-white hover:bg-slate-50 border border-[#E4E7EC] rounded-xl p-5 transition-all shadow-xs cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1.5 flex-1">
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-mono font-semibold text-[#2563EB]">
                      {c.caseNumber}
                    </span>
                    {getStatusBadge(c.status)}
                    <span className="text-xs text-[#667085] flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {new Date(c.createdAt).toLocaleDateString()}
                    </span>
                  </div>

                  <h3 className="text-base font-semibold text-[#202938]">
                    {c.title}
                  </h3>

                  {c.description && (
                    <p className="text-xs text-[#667085] line-clamp-1 max-w-2xl">
                      {c.description}
                    </p>
                  )}
                </div>

                <div className="flex items-center gap-4 pt-2 md:pt-0 border-t md:border-t-0 border-[#E4E7EC] justify-between md:justify-end">
                  <div className="flex items-center gap-1.5 text-xs text-[#667085]">
                    <User className="h-3.5 w-3.5 text-[#98A2B3]" />
                    <span>{c.createdByUsername || 'analyst'}</span>
                  </div>

                  <Link
                    to={`/cases/${c.id}`}
                    id={`inspect-case-btn-${c.id}`}
                    onClick={(e) => e.stopPropagation()}
                    className="flex items-center gap-1 text-xs text-[#2563EB] font-medium hover:text-blue-700 transition-colors"
                  >
                    <span>View case</span>
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
              className="p-2 rounded-lg bg-white border border-[#E4E7EC] text-[#202938] disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 cursor-pointer"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="text-xs text-[#667085] px-3">
              Page {page + 1} of {totalPages}
            </span>
            <button
              id="next-page-btn"
              onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
              disabled={page >= totalPages - 1}
              className="p-2 rounded-lg bg-white border border-[#E4E7EC] text-[#202938] disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 cursor-pointer"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        )}
      </main>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white border border-[#E4E7EC] rounded-xl max-w-lg w-full p-6 shadow-lg relative">
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute right-5 top-5 text-[#98A2B3] hover:text-[#202938] cursor-pointer"
            >
              <X className="h-4 w-4" />
            </button>

            <div className="mb-5">
              <h2 className="text-lg font-bold text-[#202938]">Create a case</h2>
              <p className="text-xs text-[#667085] mt-1">
                Enter a case name and optional details to begin investigating.
              </p>
            </div>

            {modalError && (
              <div className="mb-4 p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#B42318] flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-[#B42318] shrink-0" />
                <span>{modalError}</span>
              </div>
            )}

            <form onSubmit={handleCreateCase} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-[#202938] mb-1.5">
                  Case name <span className="text-[#B42318]">*</span>
                </label>
                <input
                  id="case-title-input"
                  type="text"
                  required
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. Suspicious invoice payment request"
                  className="w-full bg-white border border-[#E4E7EC] rounded-lg px-3.5 py-2.5 text-sm text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB]"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-[#202938] mb-1.5">
                  Description (optional)
                </label>
                <textarea
                  id="case-desc-input"
                  rows={4}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="Add any relevant notes or background information..."
                  className="w-full bg-white border border-[#E4E7EC] rounded-lg px-3.5 py-2.5 text-sm text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB] resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-lg text-sm font-medium text-[#667085] hover:text-[#202938] hover:bg-slate-50 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  id="create-case-submit-btn"
                  type="submit"
                  disabled={isCreating}
                  className="px-4 py-2 rounded-lg text-sm font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors disabled:opacity-50 cursor-pointer"
                >
                  {isCreating ? 'Creating case...' : 'Create case'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
