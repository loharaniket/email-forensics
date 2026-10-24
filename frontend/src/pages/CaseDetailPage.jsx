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
  AlertCircle,
  FileText,
  Mail,
  ShieldAlert,
  Edit3
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
      setError(err.response?.data?.message || 'Failed to update case notes')
    } finally {
      setIsSaving(false)
    }
  }

  const getStatusBadge = (st) => {
    switch (st) {
      case 'OPEN':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-cyan-950 text-cyan-400 border border-cyan-800">
            OPEN
          </span>
        )
      case 'IN_REVIEW':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-950 text-amber-400 border border-amber-800">
            IN REVIEW
          </span>
        )
      case 'CLOSED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-800 text-slate-400 border border-slate-700">
            CLOSED
          </span>
        )
      default:
        return <span>{st}</span>
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center gap-3">
          <FolderKanban className="h-8 w-8 text-cyan-500 animate-pulse" />
          <span className="text-xs font-mono text-slate-400">Loading Case Dossier...</span>
        </div>
      </div>
    )
  }

  if (error && !caseData) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
        <Navbar />
        <main className="flex-1 max-w-4xl w-full mx-auto p-6 md:p-8">
          <div className="p-6 rounded-2xl bg-rose-950/30 border border-rose-900/60 text-center space-y-3">
            <AlertCircle className="h-10 w-10 text-rose-400 mx-auto" />
            <h2 className="text-base font-semibold text-rose-300">Case Dossier Unavailable</h2>
            <p className="text-xs text-rose-400/80">{error}</p>
            <Link
              to="/cases"
              className="inline-flex items-center gap-2 text-xs text-cyan-400 hover:text-cyan-300 font-medium pt-2"
            >
              <ArrowLeft className="h-3.5 w-3.5" />
              Return to Cases List
            </Link>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto p-6 md:p-8 space-y-6">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate('/cases')}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            Back to Case Directory
          </button>

          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-500 font-mono">Case ID: {caseData.id}</span>
          </div>
        </div>

        <section className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 md:p-8 shadow-xl space-y-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-slate-800">
            <div className="space-y-1.5">
              <div className="flex items-center gap-3">
                <span className="text-sm font-mono font-bold text-cyan-400">
                  {caseData.caseNumber}
                </span>
                {getStatusBadge(caseData.status)}
              </div>
              <h1 className="text-xl md:text-2xl font-bold tracking-tight text-white">
                {caseData.title}
              </h1>
            </div>

            <div className="flex items-center gap-6 text-xs text-slate-400 font-mono">
              <div className="flex items-center gap-1.5">
                <User className="h-4 w-4 text-slate-500" />
                <span>Analyst: {caseData.createdByUsername || 'analyst'}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Clock className="h-4 w-4 text-slate-500" />
                <span>Created: {new Date(caseData.createdAt).toLocaleDateString()}</span>
              </div>
            </div>
          </div>

          <div>
            <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2 font-mono">
              Case Brief / Threat Summary
            </h2>
            <div className="bg-slate-950/80 rounded-xl p-4 border border-slate-800/80 text-xs text-slate-300 leading-relaxed">
              {caseData.description || 'No description provided for this case.'}
            </div>
          </div>

          <form onSubmit={handleSaveNotes} className="space-y-4 pt-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Edit3 className="h-4 w-4 text-cyan-400" />
                <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400 font-mono">
                  Analyst Working Notes &amp; Status Workflow
                </h2>
              </div>

              <div className="flex items-center gap-2">
                <label className="text-xs text-slate-400 font-mono">Case Status:</label>
                <select
                  id="status-select"
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  className="bg-slate-950 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
                >
                  <option value="OPEN">OPEN</option>
                  <option value="IN_REVIEW">IN_REVIEW</option>
                  <option value="CLOSED">CLOSED</option>
                </select>
              </div>
            </div>

            <textarea
              id="analyst-notes-textarea"
              rows={6}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Record forensic observations, IOC discoveries, correlation notes, or next steps..."
              className="w-full bg-slate-950 border border-slate-800 rounded-xl p-4 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500 resize-none font-mono"
            />

            {saveSuccess && (
              <div className="p-3 rounded-xl bg-emerald-950/40 border border-emerald-800/80 text-xs text-emerald-300 flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                <span>Analyst notes and case status saved successfully.</span>
              </div>
            )}

            {error && (
              <div className="p-3 rounded-xl bg-rose-950/40 border border-rose-800/80 text-xs text-rose-300 flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-rose-400 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <div className="flex items-center justify-end">
              <button
                id="save-notes-btn"
                type="submit"
                disabled={isSaving}
                className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-medium bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white shadow-md shadow-cyan-600/20 transition-all disabled:opacity-50 cursor-pointer"
              >
                <Save className="h-3.5 w-3.5" />
                <span>{isSaving ? 'Saving Notes...' : 'Save Investigation Notes'}</span>
              </button>
            </div>
          </form>
        </section>

        <EvidenceUploadSection caseId={caseId} />
      </main>
    </div>
  )
}
