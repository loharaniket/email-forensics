import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import EvidenceUploadSection from '../components/EvidenceUploadSection'
import { getCaseByIdApi, updateCaseNotesApi } from '../api/cases'
import {
  ArrowLeft,
  FolderKanban,
  Clock,
  User,
  Save,
  CheckCircle2,
  AlertCircle
} from 'lucide-react'

export default function CaseDetailPage() {
  const { caseId } = useParams()
  const navigate = useNavigate()

  const [caseData, setCaseData] = useState(null)
  const [notes, setNotes] = useState('')
  const [status, setStatus] = useState('OPEN')
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState(null)
  const [saveSuccess, setSaveSuccess] = useState(false)

  const loadCase = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await getCaseByIdApi(caseId)
      setCaseData(data)
      setNotes(data.analystNotes || '')
      setStatus(data.status || 'OPEN')
    } catch (err) {
      setError(err.response?.data?.message || 'Case not found or failed to load.')
    } finally {
      setIsLoading(false)
    }
  }, [caseId])

  useEffect(() => {
    loadCase()
  }, [loadCase])

  const handleSaveNotes = async (e) => {
    e.preventDefault()
    setIsSaving(true)
    setSaveSuccess(false)
    setError(null)

    try {
      const updated = await updateCaseNotesApi(caseId, {
        analystNotes: notes,
        status: status,
      })
      setCaseData(updated)
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 4000)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update case notes.')
    } finally {
      setIsSaving(false)
    }
  }

  const getStatusBadge = (st) => {
    switch (st) {
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
            {st}
          </span>
        )
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center gap-3">
          <FolderKanban className="h-8 w-8 text-[#2563EB] animate-pulse" />
          <span className="text-sm text-[#667085]">Loading case...</span>
        </div>
      </div>
    )
  }

  if (error && !caseData) {
    return (
      <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
        <Navbar />
        <main className="flex-1 max-w-4xl w-full mx-auto p-6 md:p-8">
          <div className="p-8 rounded-xl bg-white border border-[#E4E7EC] text-center space-y-3">
            <AlertCircle className="h-10 w-10 text-[#B42318] mx-auto" />
            <h2 className="text-base font-semibold text-[#202938]">Case unavailable</h2>
            <p className="text-sm text-[#667085]">{error}</p>
            <div className="pt-2">
              <Link
                to="/cases"
                className="inline-flex items-center gap-2 text-sm text-[#2563EB] hover:text-blue-700 font-medium"
              >
                <ArrowLeft className="h-4 w-4" />
                <span>Back to cases</span>
              </Link>
            </div>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[#F5F6F8] text-[#202938] flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate('/cases')}
            className="flex items-center gap-1.5 text-sm text-[#667085] hover:text-[#202938] transition-colors cursor-pointer"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back to cases</span>
          </button>

          <span className="text-xs text-[#98A2B3] font-mono">Case ID: {caseData.id}</span>
        </div>

        {/* Section 1: Case Information */}
        <section className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-[#E4E7EC]">
            <div className="space-y-1.5">
              <div className="flex items-center gap-3">
                <span className="text-sm font-mono font-semibold text-[#2563EB]">
                  {caseData.caseNumber}
                </span>
                {getStatusBadge(caseData.status)}
              </div>
              <h1 className="text-xl md:text-2xl font-bold tracking-tight text-[#202938]">
                {caseData.title}
              </h1>
            </div>

            <div className="flex items-center gap-6 text-xs text-[#667085]">
              <div className="flex items-center gap-1.5">
                <User className="h-4 w-4 text-[#98A2B3]" />
                <span>Created by: {caseData.createdByUsername || 'analyst'}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Clock className="h-4 w-4 text-[#98A2B3]" />
                <span>Created: {new Date(caseData.createdAt).toLocaleDateString()}</span>
              </div>
            </div>
          </div>

          {/* Section 2: Description */}
          <div>
            <h2 className="text-xs font-semibold uppercase tracking-wider text-[#667085] mb-2">
              Description
            </h2>
            <div className="bg-[#F5F6F8] rounded-lg p-4 border border-[#E4E7EC] text-sm text-[#202938] leading-relaxed">
              {caseData.description || 'No description provided.'}
            </div>
          </div>

          {/* Section 3: Notes and Status */}
          <form onSubmit={handleSaveNotes} className="space-y-4 pt-2">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <h2 className="text-xs font-semibold uppercase tracking-wider text-[#667085]">
                Notes and status
              </h2>

              <div className="flex items-center gap-2">
                <label className="text-xs text-[#667085] font-medium">Status:</label>
                <select
                  id="status-select"
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  className="bg-white border border-[#E4E7EC] rounded-lg px-3 py-1.5 text-xs text-[#202938] focus:outline-none focus:border-[#2563EB]"
                >
                  <option value="OPEN">Open</option>
                  <option value="IN_REVIEW">In review</option>
                  <option value="CLOSED">Closed</option>
                </select>
              </div>
            </div>

            <textarea
              id="analyst-notes-textarea"
              rows={5}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Write your observations and next steps..."
              className="w-full bg-white border border-[#E4E7EC] rounded-lg p-3.5 text-sm text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB] resize-none"
            />

            {saveSuccess && (
              <div className="p-3 rounded-lg bg-[#F0FDF4] border border-[#DCFCE7] text-xs text-[#15803D] flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-[#15803D] shrink-0" />
                <span>Changes saved.</span>
              </div>
            )}

            {error && (
              <div className="p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#B42318] flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-[#B42318] shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <div className="flex items-center justify-end">
              <button
                id="save-notes-btn"
                type="submit"
                disabled={isSaving}
                className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors disabled:opacity-50 cursor-pointer"
              >
                <Save className="h-4 w-4" />
                <span>{isSaving ? 'Saving changes...' : 'Save changes'}</span>
              </button>
            </div>
          </form>
        </section>

        {/* Section 4: Uploaded Email Files */}
        <EvidenceUploadSection caseId={caseId} />
      </main>
    </div>
  )
}
