import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate, Link } from 'react-router-dom'
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
  Eye,
  FolderOpen,
  ExternalLink,
  ArrowRight
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
      setUploadError('Please select a valid .eml email file.')
      setSelectedFile(null)
      return
    }

    if (file.size === 0) {
      setUploadError('The selected file is empty (0 bytes).')
      setSelectedFile(null)
      return
    }

    if (file.size > 25 * 1024 * 1024) {
      setUploadError('The file exceeds the 25 MB maximum upload limit.')
      setSelectedFile(null)
      return
    }

    setSelectedFile(file)
  }

  const handleUpload = async (e) => {
    if (e && e.preventDefault) e.preventDefault()
    if (!selectedFile) {
      const inputEl = document.getElementById('evidence-file-input')
      if (inputEl && inputEl.files && inputEl.files[0]) {
        return proceedWithUpload(inputEl.files[0])
      }
      return
    }
    return proceedWithUpload(selectedFile)
  }

  const proceedWithUpload = async (fileToUpload) => {
    setIsUploading(true)
    setUploadError(null)
    setUploadSuccess(null)

    try {
      const response = await uploadEvidence(caseId, fileToUpload)
      setUploadSuccess({
        filename: response.originalFilename,
        hash: response.sha256Hash,
        status: response.status,
        size: response.fileSizeBytes,
        duplicate: Boolean(response.duplicate),
        existingCaseId: response.existingCaseId,
        existingCaseNumber: response.existingCaseNumber,
        existingCaseTitle: response.existingCaseTitle,
        evidenceId: response.emailId,
        message: response.message
      })
      setSelectedFile(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      await loadEvidence()
    } catch (err) {
      setUploadError(err.response?.data?.message || 'Failed to upload email file.')
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
          error: err.response?.data?.message || 'File integrity check failed.'
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
      alert('Failed to download original email.')
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
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#FFFBEB] text-[#B45309] border border-amber-200">
            In queue
          </span>
        )
      case 'RUNNING':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
            Analyzing
          </span>
        )
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#F0FDF4] text-[#15803D] border border-green-200">
            Completed
          </span>
        )
      case 'FAILED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#FEF2F2] text-[#B42318] border border-red-200">
            Failed
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-[#667085]">
            {st}
          </span>
        )
    }
  }

  return (
    <div className="space-y-6">
      {/* Upload email section */}
      <section className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-5">
        <div className="pb-4 border-b border-[#E4E7EC]">
          <h2 className="text-base font-bold text-[#202938]">
            Upload email
          </h2>
          <p className="text-sm text-[#667085] mt-0.5">
            Choose an .eml email file to start the analysis.
          </p>
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
                className="w-full text-xs text-[#202938] file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-medium file:bg-[#EFF6FF] file:text-[#2563EB] hover:file:bg-blue-100 bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg p-2 cursor-pointer focus:outline-none focus:border-[#2563EB]"
              />
            </div>

            <button
              id="upload-evidence-btn"
              type="button"
              onClick={handleUpload}
              disabled={isUploading}
              className="flex items-center justify-center gap-2 px-5 py-2.5 rounded-lg text-sm font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shrink-0"
            >
              {isUploading ? (
                <>
                  <RefreshCw className="h-4 w-4 animate-spin" />
                  <span>Uploading email...</span>
                </>
              ) : (
                <>
                  <Upload className="h-4 w-4" />
                  <span>Upload email</span>
                </>
              )}
            </button>
          </div>

          {selectedFile && (
            <div className="flex items-center gap-2.5 text-xs text-[#667085] bg-[#F5F6F8] p-3 rounded-lg border border-[#E4E7EC]">
              <FileText className="h-4 w-4 text-[#2563EB]" />
              <span>Selected file: <strong className="text-[#202938] font-medium">{selectedFile.name}</strong></span>
              <span>({formatBytes(selectedFile.size)})</span>
            </div>
          )}

          {uploadError && (
            <div className="p-3.5 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#B42318] flex items-center gap-2.5">
              <AlertTriangle className="h-4 w-4 text-[#B42318] shrink-0" />
              <span>{uploadError}</span>
            </div>
          )}

          {uploadSuccess && (
            uploadSuccess.duplicate ? (
              <div id="duplicate-evidence-banner" className="p-4 md:p-5 rounded-lg bg-[#EFF6FF] border border-blue-200 text-xs text-[#202938] space-y-3">
                <div className="flex items-center gap-2 font-semibold text-[#2563EB]">
                  <CheckCircle2 className="h-4 w-4" />
                  <span className="text-sm">This email was already uploaded</span>
                </div>

                <p className="text-xs text-[#667085] leading-relaxed">
                  An identical file is already stored. The application will use the existing copy.
                </p>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 text-xs text-[#667085] bg-white p-3 rounded-lg border border-blue-100">
                  <div>
                    <span className="font-medium text-[#202938]">Filename: </span>
                    {uploadSuccess.filename} ({formatBytes(uploadSuccess.size)})
                  </div>
                  <div>
                    <span className="font-medium text-[#202938]">Status: </span>
                    {uploadSuccess.status}
                  </div>

                  {uploadSuccess.existingCaseId && (
                    <div className="sm:col-span-2 flex flex-wrap items-center gap-1.5 pt-1 border-t border-slate-100">
                      <span className="font-medium text-[#202938]">Existing case: </span>
                      <Link
                        id="actual-case-link"
                        to={`/cases/${uploadSuccess.existingCaseId}`}
                        className="inline-flex items-center gap-1 text-[#2563EB] hover:underline font-medium"
                      >
                        <span>{uploadSuccess.existingCaseNumber || 'Case'}</span>
                        {uploadSuccess.existingCaseTitle && (
                          <span className="text-[#667085] font-normal"> — {uploadSuccess.existingCaseTitle}</span>
                        )}
                        <ExternalLink className="h-3 w-3 ml-0.5" />
                      </Link>
                    </div>
                  )}

                  <div className="sm:col-span-2 break-all text-[11px] pt-1">
                    <span className="font-medium text-[#202938]">SHA-256: </span>
                    <span className="font-mono text-[#667085]">{uploadSuccess.hash}</span>
                  </div>
                </div>

                <div className="pt-2 flex flex-wrap items-center gap-3">
                  {uploadSuccess.existingCaseId && (
                    <button
                      type="button"
                      id="go-to-actual-case-btn"
                      onClick={() => navigate(`/cases/${uploadSuccess.existingCaseId}`)}
                      className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer"
                    >
                      <FolderOpen className="h-3.5 w-3.5" />
                      <span>Open existing case</span>
                    </button>
                  )}

                  <button
                    type="button"
                    id="view-duplicate-evidence-btn"
                    onClick={() => navigate(`/cases/${uploadSuccess.existingCaseId || caseId}/evidence/${uploadSuccess.evidenceId}`)}
                    className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer"
                  >
                    <Eye className="h-3.5 w-3.5 text-[#667085]" />
                    <span>View results</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-lg bg-[#F0FDF4] border border-[#DCFCE7] text-xs text-[#202938] space-y-2">
                <div className="flex items-center gap-2 font-semibold text-[#15803D]">
                  <CheckCircle2 className="h-4 w-4" />
                  <span className="text-sm">Email uploaded successfully</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 text-xs text-[#667085]">
                  <div>
                    <span className="font-medium text-[#202938]">Filename: </span>
                    {uploadSuccess.filename} ({formatBytes(uploadSuccess.size)})
                  </div>
                  <div>
                    <span className="font-medium text-[#202938]">Status: </span>
                    {uploadSuccess.status}
                  </div>
                  <div className="sm:col-span-2 break-all bg-white p-2.5 rounded-lg border border-green-100 font-mono text-[11px]">
                    <span className="font-medium text-[#202938]">SHA-256: </span>
                    {uploadSuccess.hash}
                  </div>
                </div>
                {uploadSuccess.evidenceId && (
                  <div className="pt-2">
                    <button
                      id="inspect-uploaded-evidence-btn"
                      type="button"
                      onClick={() => navigate(`/cases/${caseId}/evidence/${uploadSuccess.evidenceId}`)}
                      className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium bg-[#15803D] hover:bg-green-800 text-white transition-colors cursor-pointer"
                    >
                      <Eye className="h-3.5 w-3.5" />
                      <span>View analysis</span>
                    </button>
                  </div>
                )}
              </div>
            )
          )}
        </form>
      </section>

      {/* Uploaded files section */}
      <section className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-[#E4E7EC]">
          <div className="flex items-center gap-2">
            <h2 className="text-base font-bold text-[#202938]">
              Uploaded files ({evidenceList.length})
            </h2>
          </div>
          <button
            onClick={loadEvidence}
            className="flex items-center gap-1.5 text-xs text-[#667085] hover:text-[#202938] transition-colors cursor-pointer"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            <span>Refresh</span>
          </button>
        </div>

        {isLoading ? (
          <div className="p-8 text-center text-xs text-[#667085]">
            Loading files...
          </div>
        ) : evidenceList.length === 0 ? (
          <div className="border border-dashed border-[#E4E7EC] rounded-xl p-8 text-center bg-[#F5F6F8] space-y-2">
            <FileText className="h-8 w-8 text-[#98A2B3] mx-auto" />
            <h3 className="text-sm font-semibold text-[#202938]">
              No email files uploaded yet.
            </h3>
            <p className="text-xs text-[#667085] max-w-sm mx-auto">
              Upload an .eml file to begin.
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
                  className="bg-[#F5F6F8] border border-[#E4E7EC] rounded-xl p-4 md:p-5 space-y-3 transition-colors hover:border-slate-300"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2 border-b border-[#E4E7EC]">
                    <div className="flex items-center gap-2">
                      <FileText className="h-4 w-4 text-[#2563EB] shrink-0" />
                      <span className="text-sm font-semibold text-[#202938]">
                        {item.originalFilename}
                      </span>
                      <span className="text-xs text-[#667085]">
                        ({formatBytes(item.fileSizeBytes)})
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-xs text-[#667085]">Status:</span>
                      {getStatusBadge(item.latestStatus)}
                    </div>
                  </div>

                  {item.subject && (
                    <div className="text-xs text-[#202938] font-medium truncate">
                      Subject: {item.subject}
                    </div>
                  )}

                  <div className="space-y-2 text-xs">
                    <div className="flex items-start gap-2 bg-white p-2.5 rounded-lg border border-[#E4E7EC]">
                      <Hash className="h-3.5 w-3.5 text-[#98A2B3] shrink-0 mt-0.5" />
                      <div className="flex-1 break-all font-mono text-[11px]">
                        <span className="text-[#667085]">SHA-256: </span>
                        <span className="text-[#202938] select-all">{item.sha256Hash}</span>
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-[#667085] pt-1">
                      <div className="flex flex-wrap items-center gap-4">
                        <div className="flex items-center gap-1">
                          <Clock className="h-3.5 w-3.5 text-[#98A2B3]" />
                          <span>Uploaded: {new Date(item.uploadedAt).toLocaleString()}</span>
                        </div>
                        {item.sentDate && (
                          <span>Sent: {new Date(item.sentDate).toLocaleString()}</span>
                        )}
                        <span>By: {item.uploadedByUsername || 'analyst'}</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          id={`inspect-btn-${item.id}`}
                          onClick={() => navigate(`/cases/${caseId}/evidence/${item.id}`)}
                          className="inspect-artifact-btn flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer"
                        >
                          <Eye className="h-3.5 w-3.5" />
                          <span>View results</span>
                        </button>

                        <button
                          id={`verify-btn-${item.id}`}
                          onClick={() => handleVerifyIntegrity(item.id)}
                          disabled={verifying}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors disabled:opacity-50 cursor-pointer"
                        >
                          <ShieldCheck className={`h-3.5 w-3.5 text-[#667085] ${verifying ? 'animate-spin' : ''}`} />
                          <span>{verifying ? 'Checking...' : 'Check file integrity'}</span>
                        </button>

                        <button
                          id={`download-btn-${item.id}`}
                          onClick={() => handleDownload(item.id, item.originalFilename)}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white hover:bg-slate-50 text-[#202938] border border-[#E4E7EC] transition-colors cursor-pointer"
                        >
                          <Download className="h-3.5 w-3.5 text-[#667085]" />
                          <span>Download original</span>
                        </button>
                      </div>
                    </div>

                    {integrity && (
                      <div
                        className={`p-3 rounded-lg text-xs flex items-center justify-between border ${
                          integrity.intact
                            ? 'bg-[#F0FDF4] border-[#DCFCE7] text-[#15803D]'
                            : 'bg-[#FEF2F2] border-[#FEE2E2] text-[#B42318]'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          {integrity.intact ? (
                            <CheckCircle2 className="h-4 w-4 text-[#15803D] shrink-0" />
                          ) : (
                            <AlertTriangle className="h-4 w-4 text-[#B42318] shrink-0" />
                          )}
                          <span>
                            {integrity.intact
                              ? 'File integrity verified: The stored file matches the original record.'
                              : `File integrity check failed: ${integrity.error || 'Hash mismatch detected.'}`}
                          </span>
                        </div>
                        <span className="text-[11px] text-[#667085] font-mono">
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
