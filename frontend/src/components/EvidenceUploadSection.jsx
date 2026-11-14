import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Upload,
  ShieldCheck,
  AlertTriangle,
  Download,
  RefreshCw,
  Hash,
  Clock,
  CheckCircle2,
  FileText,
  Eye,
  Search
} from 'lucide-react'
import {
  uploadEvidence,
  getEvidenceList,
  getEvidenceStatus,
  checkEvidenceIntegrity,
  downloadOriginalEvidence
} from '../api/evidence'

export default function EvidenceUploadSection() {
  const navigate = useNavigate()
  const [evidenceList, setEvidenceList] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [selectedFile, setSelectedFile] = useState(null)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadError, setUploadError] = useState(null)
  const [uploadSuccess, setUploadSuccess] = useState(null)
  const [integrityState, setIntegrityState] = useState({})
  const [isVerifying, setIsVerifying] = useState({})
  const [searchQuery, setSearchQuery] = useState('')
  const fileInputRef = useRef(null)

  const loadEvidence = useCallback(async () => {
    try {
      const data = await getEvidenceList(0, 50)
      setEvidenceList(data.content || [])
    } catch (err) {
      console.error(err)
    } finally {
      setIsLoading(false)
    }
  }, [])

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
      const response = await uploadEvidence(fileToUpload)
      setUploadSuccess({
        filename: response.originalFilename,
        hash: response.sha256Hash,
        status: response.status,
        size: response.fileSizeBytes,
        duplicate: Boolean(response.duplicate),
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
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-[#FFFBEB] text-[#B45309] border border-amber-200">
            In queue
          </span>
        )
      case 'RUNNING':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
            Analyzing
          </span>
        )
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold bg-[#F0FDF4] text-[#15803D] border border-green-200">
            Completed
          </span>
        )
      case 'FAILED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-[#FEF2F2] text-[#B42318] border border-red-200">
            Failed
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 text-[#202938]">
            {st}
          </span>
        )
    }
  }

  const filteredEvidence = useMemo(() => {
    if (!searchQuery.trim()) return evidenceList
    const q = searchQuery.toLowerCase()
    return evidenceList.filter(
      (item) =>
        (item.originalFilename && item.originalFilename.toLowerCase().includes(q)) ||
        (item.subject && item.subject.toLowerCase().includes(q)) ||
        (item.sha256Hash && item.sha256Hash.toLowerCase().includes(q)) ||
        (item.latestStatus && item.latestStatus.toLowerCase().includes(q))
    )
  }, [evidenceList, searchQuery])

  return (
    <div className="h-full flex flex-col lg:flex-row gap-4 min-h-0 overflow-hidden">
      <section className="w-full lg:w-[380px] bg-white border border-[#E4E7EC] rounded-xl p-4 sm:p-5 flex flex-col justify-between shrink-0 overflow-y-auto">
        <div className="space-y-4">
          <div className="pb-3 border-b border-[#E4E7EC]">
            <h2 className="text-sm font-bold text-[#202938]">
              Upload email evidence
            </h2>
            <p className="text-xs text-[#202938] mt-0.5">
              Select an .eml email file to start the forensic pipeline.
            </p>
          </div>

          <form onSubmit={handleUpload} className="space-y-3">
            {/* Simple File Selector Area */}
            <div className="border-2 border-dashed border-[#BFDBFE] bg-[#F8FAFC] rounded-lg p-5 text-center flex flex-col items-center justify-center gap-2">
              <Upload className="h-7 w-7 text-[#2563EB]" />
              <div className="text-xs font-semibold text-[#202938]">
                Select .eml email file
              </div>
              <p className="text-[11px] text-[#202938]">
                RFC 5322 format up to 25 MB
              </p>
              <input
                id="evidence-file-input"
                ref={fileInputRef}
                type="file"
                accept=".eml"
                onChange={handleFileChange}
                className="w-full text-xs text-[#202938] file:mr-2 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-medium file:bg-[#EFF6FF] file:text-[#2563EB] hover:file:bg-blue-100 bg-white border border-[#E4E7EC] rounded-md p-1.5 cursor-pointer focus:outline-none focus:border-[#2563EB]"
              />
            </div>

            {selectedFile && (
              <div className="flex items-center gap-2 text-xs text-[#202938] bg-[#F5F6F8] p-2.5 rounded-lg border border-[#E4E7EC]">
                <FileText className="h-4 w-4 text-[#2563EB] shrink-0" />
                <span className="truncate flex-1 font-medium">{selectedFile.name}</span>
                <span className="shrink-0 text-[11px]">({formatBytes(selectedFile.size)})</span>
              </div>
            )}

            {uploadError && (
              <div className="p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#B42318] flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-[#B42318] shrink-0" />
                <span>{uploadError}</span>
              </div>
            )}

            {uploadSuccess && (
              uploadSuccess.duplicate ? (
                <div id="duplicate-evidence-banner" className="p-3 rounded-lg bg-[#EFF6FF] border border-blue-200 text-xs text-[#202938] space-y-2">
                  <div className="flex items-center gap-1.5 font-semibold text-[#2563EB]">
                    <CheckCircle2 className="h-4 w-4" />
                    <span>Email already stored</span>
                  </div>
                  <p className="text-[11px] text-[#202938] leading-relaxed">
                    An identical file is already in the database.
                  </p>
                  <button
                    type="button"
                    id="view-duplicate-evidence-btn"
                    onClick={() => navigate(`/evidence/${uploadSuccess.evidenceId}`)}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer"
                  >
                    <Eye className="h-3.5 w-3.5" />
                    <span>View analysis results</span>
                  </button>
                </div>
              ) : (
                <div className="p-3 rounded-lg bg-[#F0FDF4] border border-[#DCFCE7] text-xs text-[#202938] space-y-2">
                  <div className="flex items-center gap-1.5 font-semibold text-[#15803D]">
                    <CheckCircle2 className="h-4 w-4" />
                    <span>Uploaded successfully</span>
                  </div>
                  <div className="text-[11px] text-[#202938] truncate">
                    {uploadSuccess.filename} ({formatBytes(uploadSuccess.size)})
                  </div>
                  {uploadSuccess.evidenceId && (
                    <button
                      id="inspect-uploaded-evidence-btn"
                      type="button"
                      onClick={() => navigate(`/evidence/${uploadSuccess.evidenceId}`)}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-[#15803D] hover:bg-green-800 text-white transition-colors cursor-pointer"
                    >
                      <Eye className="h-3.5 w-3.5" />
                      <span>Open analysis</span>
                    </button>
                  )}
                </div>
              )
            )}

            <button
              id="upload-evidence-btn"
              type="button"
              onClick={handleUpload}
              disabled={isUploading}
              className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {isUploading ? (
                <>
                  <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                  <span>Uploading &amp; analyzing email...</span>
                </>
              ) : (
                <>
                  <Upload className="h-3.5 w-3.5" />
                  <span>Upload &amp; Analyze</span>
                </>
              )}
            </button>
          </form>
        </div>


      </section>

      {/* Right Column: Clean & Minimal Recent Cases Table with Contained Scroll */}
      <section className="flex-1 min-w-0 bg-white border border-[#E4E7EC] rounded-xl p-4 sm:p-5 flex flex-col min-h-0">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#E4E7EC] shrink-0">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-bold text-[#202938]">
              Recent investigated cases
            </h2>
            <span className="px-1.5 py-0.2 rounded-md text-[11px] font-mono font-semibold bg-[#F5F6F8] text-[#202938] border border-[#E4E7EC]">
              {evidenceList.length}
            </span>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="h-3.5 w-3.5 absolute left-2.5 top-2 text-[#202938]" />
              <input
                type="text"
                placeholder="Search files..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8 pr-2.5 py-1 text-xs bg-[#F5F6F8] border border-[#E4E7EC] rounded-md outline-none focus:border-[#2563EB] text-[#202938] w-40 sm:w-52"
              />
            </div>
            <button
              onClick={loadEvidence}
              className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-[#202938] bg-white border border-[#E4E7EC] rounded-md hover:bg-slate-50 transition-colors cursor-pointer"
              title="Refresh case list"
            >
              <RefreshCw className="h-3 w-3" />
              <span>Refresh</span>
            </button>
          </div>
        </div>

        {/* Content list / table with contained internal scroll */}
        {isLoading ? (
          <div className="p-8 text-center text-xs text-[#202938]">
            Loading cases...
          </div>
        ) : filteredEvidence.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center p-8 text-center text-xs text-[#202938] border border-dashed border-[#E4E7EC] rounded-lg mt-3">
            <FileText className="h-8 w-8 text-[#202938] mb-2" />
            <h3 className="text-sm font-semibold text-[#202938]">
              No email cases found
            </h3>
            <p className="text-xs text-[#202938] mt-1">
              {searchQuery ? 'Try clearing your search query.' : 'Upload an .eml email file on the left to begin.'}
            </p>
          </div>
        ) : (
          <div className="flex-1 overflow-y-auto mt-2">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-[#E4E7EC] text-[11px] text-[#202938] bg-[#F8FAFC] sticky top-0 z-10">
                  <th className="p-2.5 font-semibold">File &amp; Subject</th>
                  <th className="p-2.5 font-semibold">Status</th>
                  <th className="p-2.5 font-semibold">Date</th>
                  <th className="p-2.5 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E4E7EC]">
                {filteredEvidence.map((item) => {
                  const integrity = integrityState[item.id]
                  const verifying = isVerifying[item.id]

                  return (
                    <tr key={item.id} className="hover:bg-slate-50 transition-colors">
                      <td className="p-2.5 max-w-[220px] sm:max-w-[320px]">
                        <div className="flex items-center gap-1.5">
                          <FileText className="h-3.5 w-3.5 text-[#2563EB] shrink-0" />
                          <span className="font-semibold text-[#202938] truncate" title={item.originalFilename}>
                            {item.originalFilename}
                          </span>
                          <span className="text-[10px] text-[#202938] shrink-0 font-mono">
                            ({formatBytes(item.fileSizeBytes)})
                          </span>
                        </div>
                        {item.subject && (
                          <div className="text-[11px] text-[#202938] truncate mt-0.5" title={item.subject}>
                            {item.subject}
                          </div>
                        )}
                      </td>

                      <td className="p-2.5 whitespace-nowrap">
                        {getStatusBadge(item.latestStatus)}
                      </td>

                      <td className="p-2.5 text-[#202938] whitespace-nowrap text-[11px]">
                        {new Date(item.uploadedAt).toLocaleDateString()}
                      </td>

                      <td className="p-2.5 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            id={`inspect-btn-${item.id}`}
                            onClick={() => navigate(`/evidence/${item.id}`)}
                            className="inspect-artifact-btn flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer"
                            title="Open investigation results"
                          >
                            <Eye className="h-3 w-3" />
                            <span>View</span>
                          </button>

                          <button
                            id={`verify-btn-${item.id}`}
                            onClick={() => handleVerifyIntegrity(item.id)}
                            disabled={verifying}
                            className={`flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium border border-[#E4E7EC] bg-white hover:bg-slate-50 text-[#202938] transition-colors cursor-pointer ${integrity?.intact ? 'border-green-300 text-[#15803D]' : ''
                              }`}
                            title="Verify SHA-256 evidence integrity"
                          >
                            <ShieldCheck className={`h-3 w-3 ${verifying ? 'animate-spin' : ''}`} />
                            <span className="hidden xl:inline">{verifying ? 'Checking' : integrity?.intact ? 'Verified' : 'Verify'}</span>
                          </button>

                          <button
                            id={`download-btn-${item.id}`}
                            onClick={() => handleDownload(item.id, item.originalFilename)}
                            className="p-1 rounded text-[#202938] hover:bg-slate-100 transition-colors cursor-pointer"
                            title="Download original .eml file"
                          >
                            <Download className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
