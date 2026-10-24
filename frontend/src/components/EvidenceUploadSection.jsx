import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Upload,
  FileCheck,
  ShieldCheck,
  AlertTriangle,
  Download,
  RefreshCw,
  Hash,
  Clock,
  CheckCircle2,
  FileText,
  ShieldAlert,
  HardDrive,
  Eye
} from 'lucide-react'
import {
  uploadEvidence,
  getCaseEvidence,
  getEvidenceStatus,
  checkEvidenceIntegrity,
  downloadOriginalEvidence
} from '../api/evidence'

export default function EvidenceUploadSection({ caseId }) {
  const navigate = useNavigate()
  const [evidenceList, setEvidenceList] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [selectedFile, setSelectedFile] = useState(null)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadError, setUploadError] = useState(null)
  const [uploadSuccess, setUploadSuccess] = useState(null)
  const [integrityState, setIntegrityState] = useState({})
  const [isVerifying, setIsVerifying] = useState({})
  const fileInputRef = useRef(null)

  const loadEvidence = useCallback(async () => {
    try {
      const data = await getCaseEvidence(caseId, 0, 50)
      setEvidenceList(data.content || [])
    } catch (err) {
      console.error(err)
    } finally {
      setIsLoading(false)
    }
  }, [caseId])

  useEffect(() => {
    loadEvidence()
  }, [loadEvidence])

  useEffect(() => {
    const activeRuns = evidenceList.filter(
      (ev) => ev.latestStatus === 'QUEUED' || ev.latestStatus === 'RUNNING'
    )

    if (activeRuns.length === 0) return

    const timer = setInterval(async () => {
      let anyUpdated = false
      const updatedList = await Promise.all(
        evidenceList.map(async (ev) => {
          if (ev.latestStatus === 'QUEUED' || ev.latestStatus === 'RUNNING') {
            try {
              const statusData = await getEvidenceStatus(ev.id)
              if (
                statusData.status !== ev.latestStatus ||
                statusData.currentStage !== ev.currentStage
              ) {
                anyUpdated = true
                return {
                  ...ev,
                  latestStatus: statusData.status,
                  currentStage: statusData.currentStage
                }
              }
            } catch (err) {
              console.error(err)
            }
          }
          return ev
        })
      )

      if (anyUpdated) {
        setEvidenceList(updatedList)
      }
    }, 4000)

    return () => clearInterval(timer)
  }, [evidenceList])

  const handleFileChange = (e) => {
    setUploadError(null)
    setUploadSuccess(null)
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.name.toLowerCase().endsWith('.eml')) {
      setUploadError('Only RFC 822/5322 .eml files are supported as email evidence.')
      setSelectedFile(null)
      return
    }

    if (file.size === 0) {
      setUploadError('Selected evidence file is empty (0 bytes).')
      setSelectedFile(null)
      return
    }

    if (file.size > 25 * 1024 * 1024) {
      setUploadError('Evidence file exceeds 25 MB maximum upload limit.')
      setSelectedFile(null)
      return
    }

    setSelectedFile(file)
  }

  const handleUpload = async (e) => {
    e.preventDefault()
    if (!selectedFile) return

    setIsUploading(true)
    setUploadError(null)
    setUploadSuccess(null)

    try {
      const response = await uploadEvidence(caseId, selectedFile)
      setUploadSuccess({
        filename: response.originalFilename,
        hash: response.sha256Hash,
        status: response.status,
        size: response.fileSizeBytes
      })
      setSelectedFile(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      await loadEvidence()
    } catch (err) {
      setUploadError(err.response?.data?.message || 'Failed to upload evidence file.')
    } finally {
      setIsUploading(false)
    }
  }

  const handleVerifyIntegrity = async (emailId) => {
    setIsVerifying((prev) => ({ ...prev, [emailId]: true }))
    try {
      const result = await checkEvidenceIntegrity(emailId)
      setIntegrityState((prev) => ({
        ...prev,
        [emailId]: {
          intact: result.intact,
          recalculatedSha256: result.recalculatedSha256,
          savedSha256: result.savedSha256,
          checkedAt: result.verifiedAt
        }
      }))
    } catch (err) {
      setIntegrityState((prev) => ({
        ...prev,
        [emailId]: {
          intact: false,
          error: err.response?.data?.message || 'Integrity check failed'
        }
      }))
    } finally {
      setIsVerifying((prev) => ({ ...prev, [emailId]: false }))
    }
  }

  const handleDownload = async (emailId, filename) => {
    try {
      await downloadOriginalEvidence(emailId, filename)
    } catch (err) {
      alert('Failed to download original evidence.')
    }
  }

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const getStatusBadge = (st) => {
    switch (st) {
      case 'QUEUED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-medium bg-amber-950/80 text-amber-400 border border-amber-800/80">
            QUEUED
          </span>
        )
      case 'RUNNING':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-medium bg-cyan-950/80 text-cyan-400 border border-cyan-800/80 animate-pulse">
            RUNNING
          </span>
        )
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-medium bg-emerald-950/80 text-emerald-400 border border-emerald-800/80">
            COMPLETED
          </span>
        )
      case 'FAILED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-medium bg-rose-950/80 text-rose-400 border border-rose-800/80">
            FAILED
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-medium bg-slate-800 text-slate-300">
            {st}
          </span>
        )
    }
  }

  return (
    <div className="space-y-6">
      <section className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 md:p-8 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-cyan-950/60 border border-cyan-800/60 text-cyan-400">
              <Upload className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white tracking-tight">
                Upload &amp; Preserve Email Evidence
              </h2>
              <p className="text-xs text-slate-400">
                Evidence vault immutability: computes SHA-256 before storage and enqueues forensic analysis.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 text-xs font-mono text-slate-400 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800">
            <HardDrive className="h-3.5 w-3.5 text-cyan-400" />
            <span>Vault: Private Storage</span>
          </div>
        </div>

        <form onSubmit={handleUpload} className="space-y-4">
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <div className="relative flex-1">
              <input
                id="evidence-file-input"
                ref={fileInputRef}
                type="file"
                accept=".eml"
                onChange={handleFileChange}
                className="w-full text-xs text-slate-300 file:mr-4 file:py-2.5 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-slate-800 file:text-cyan-400 hover:file:bg-slate-700 bg-slate-950 border border-slate-800 rounded-xl p-2 cursor-pointer focus:outline-none focus:border-cyan-500"
              />
            </div>

            <button
              id="upload-evidence-btn"
              type="submit"
              disabled={!selectedFile || isUploading}
              className="flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl text-xs font-semibold bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white shadow-lg shadow-cyan-600/20 transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shrink-0"
            >
              {isUploading ? (
                <>
                  <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                  <span>Preserving &amp; Hashing...</span>
                </>
              ) : (
                <>
                  <FileCheck className="h-3.5 w-3.5" />
                  <span>Upload &amp; Preserve (.eml)</span>
                </>
              )}
            </button>
          </div>

          {selectedFile && (
            <div className="flex items-center gap-3 text-xs text-slate-400 font-mono bg-slate-950/60 p-3 rounded-xl border border-slate-800/80">
              <FileText className="h-4 w-4 text-cyan-400" />
              <span>Selected: <strong className="text-slate-200">{selectedFile.name}</strong></span>
              <span>({formatBytes(selectedFile.size)})</span>
            </div>
          )}

          {uploadError && (
            <div className="p-3.5 rounded-xl bg-rose-950/40 border border-rose-800/80 text-xs text-rose-300 flex items-center gap-2.5">
              <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
              <span>{uploadError}</span>
            </div>
          )}

          {uploadSuccess && (
            <div className="p-4 rounded-xl bg-emerald-950/40 border border-emerald-800/80 text-xs text-emerald-200 space-y-2">
              <div className="flex items-center gap-2 font-semibold text-emerald-400">
                <CheckCircle2 className="h-4 w-4" />
                <span>Evidence Successfully Preserved in Vault</span>
                <span className="ml-auto font-mono text-[11px] bg-emerald-900/60 text-emerald-300 px-2 py-0.5 rounded">
                  HTTP 202 ACCEPTED
                </span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 font-mono text-[11px] text-slate-300">
                <div>
                  <span className="text-slate-400">Filename: </span>
                  {uploadSuccess.filename} ({formatBytes(uploadSuccess.size)})
                </div>
                <div>
                  <span className="text-slate-400">Initial Pipeline State: </span>
                  <span className="text-amber-400 font-semibold">{uploadSuccess.status}</span>
                </div>
                <div className="sm:col-span-2 break-all bg-slate-950/60 p-2 rounded border border-emerald-900/40">
                  <span className="text-cyan-400 font-semibold">SHA-256 Vault Hash: </span>
                  {uploadSuccess.hash}
                </div>
              </div>
            </div>
          )}
        </form>
      </section>

      <section className="bg-slate-900/40 border border-slate-800 rounded-2xl p-6 md:p-8 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-cyan-400" />
            <h2 className="text-sm font-bold text-white tracking-tight">
              Preserved Evidence Chain of Custody ({evidenceList.length})
            </h2>
          </div>
          <button
            onClick={loadEvidence}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
          >
            <RefreshCw className="h-3 w-3" />
            <span>Refresh</span>
          </button>
        </div>

        {isLoading ? (
          <div className="p-8 text-center text-xs text-slate-400 font-mono">
            Loading evidence inventory...
          </div>
        ) : evidenceList.length === 0 ? (
          <div className="border border-dashed border-slate-800 rounded-xl p-8 text-center bg-slate-950/40 space-y-2">
            <ShieldAlert className="h-8 w-8 text-slate-600 mx-auto" />
            <h3 className="text-xs font-semibold text-slate-300 font-mono">
              NO EVIDENCE ARTIFACTS ATTACHED YET
            </h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Select an .eml email file above to compute the SHA-256 cryptographic digest and safely preserve it in the local vault.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {evidenceList.map((item) => {
              const integrity = integrityState[item.id]
              const verifying = isVerifying[item.id]

              return (
                <div
                  key={item.id}
                  className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 md:p-5 space-y-3 transition-all hover:border-slate-700"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2 border-b border-slate-800/80">
                    <div className="flex items-center gap-2">
                      <FileText className="h-4 w-4 text-cyan-400 shrink-0" />
                      <span className="text-xs font-semibold text-slate-200 font-mono">
                        {item.originalFilename}
                      </span>
                      <span className="text-[11px] text-slate-500 font-mono">
                        ({formatBytes(item.fileSizeBytes)})
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-xs text-slate-400 font-mono">Status:</span>
                      {getStatusBadge(item.latestStatus)}
                      {item.currentStage && (
                        <span className="text-[11px] font-mono text-slate-400 bg-slate-900 px-2 py-0.5 rounded border border-slate-800">
                          {item.currentStage}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="space-y-2 text-xs font-mono">
                    <div className="flex items-start gap-2 bg-slate-900/60 p-2.5 rounded-lg border border-slate-800">
                      <Hash className="h-3.5 w-3.5 text-cyan-400 shrink-0 mt-0.5" />
                      <div className="flex-1 break-all">
                        <span className="text-slate-500">SHA-256: </span>
                        <span className="text-slate-300 select-all">{item.sha256Hash}</span>
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-400 pt-1">
                      <div className="flex items-center gap-4">
                        <div className="flex items-center gap-1">
                          <Clock className="h-3 w-3 text-slate-500" />
                          <span>Uploaded: {new Date(item.uploadedAt).toLocaleString()}</span>
                        </div>
                        <span>By: {item.uploadedByUsername || 'analyst'}</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          id={`inspect-btn-${item.id}`}
                          onClick={() => navigate(`/cases/${caseId}/evidence/${item.id}`)}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white shadow shadow-cyan-600/20 transition-all cursor-pointer"
                        >
                          <Eye className="h-3 w-3" />
                          <span>Inspect Artifact</span>
                        </button>

                        <button
                          id={`verify-btn-${item.id}`}
                          onClick={() => handleVerifyIntegrity(item.id)}
                          disabled={verifying}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 hover:bg-slate-700 text-cyan-300 border border-slate-700 transition-colors disabled:opacity-50 cursor-pointer"
                        >
                          <ShieldCheck className={`h-3 w-3 ${verifying ? 'animate-spin' : ''}`} />
                          <span>{verifying ? 'Verifying...' : 'Verify Vault Integrity'}</span>
                        </button>

                        <button
                          id={`download-btn-${item.id}`}
                          onClick={() => handleDownload(item.id, item.originalFilename)}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors cursor-pointer"
                        >
                          <Download className="h-3 w-3" />
                          <span>Download Original</span>
                        </button>
                      </div>
                    </div>

                    {integrity && (
                      <div
                        className={`p-3 rounded-lg text-xs flex items-center justify-between border ${
                          integrity.intact
                            ? 'bg-emerald-950/40 border-emerald-800/80 text-emerald-300'
                            : 'bg-rose-950/40 border-rose-800/80 text-rose-300'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          {integrity.intact ? (
                            <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                          ) : (
                            <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
                          )}
                          <span>
                            {integrity.intact
                              ? 'Vault Cryptographic Integrity Verified: Stored file hash matches registered record.'
                              : `Integrity Check Failed: ${integrity.error || 'Hash mismatch detected.'}`}
                          </span>
                        </div>
                        <span className="text-[10px] text-slate-400 font-mono">
                          {new Date(integrity.checkedAt).toLocaleTimeString()}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}
