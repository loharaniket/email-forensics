import React, { useState, useMemo } from 'react'
import {
  Paperclip,
  Image as ImageIcon,
  QrCode,
  Search,
  Hash,
  Copy,
  Check,
  ExternalLink,
  Download,
  Link2,
  AlertTriangle
} from 'lucide-react'

export default function AttachmentsAndImages({
  mode = 'attachments', // 'attachments', 'visuals', or 'qr'
  emailId,
  attachments = [],
  inlineImages = [],
  visuals = [],
  qrCodes = [],
  copiedKey,
  copyValue,
  formatBytes = (b) => (b != null ? `${b} B` : '0 B'),
  onNavigateTab
}) {
  const [searchQuery, setSearchQuery] = useState('')

  // Flatten QR codes from dedicated prop or extract from visuals
  const allQrCodes = useMemo(() => {
    if (qrCodes && qrCodes.length > 0) {
      return qrCodes
    }
    const collected = []
    visuals.forEach((v) => {
      if (v.qrArtifacts && v.qrArtifacts.length > 0) {
        v.qrArtifacts.forEach((q) => {
          collected.push({
            ...q,
            imageId: q.imageId || v.id,
            imageFileName: q.imageFileName || v.filename
          })
        })
      }
    })
    return collected
  }, [qrCodes, visuals])

  // Filtered visuals
  const filteredVisuals = useMemo(() => {
    if (!searchQuery.trim()) return visuals
    const q = searchQuery.toLowerCase()
    return visuals.filter((v) => {
      const matchFile = v.filename && v.filename.toLowerCase().includes(q)
      const matchHash = v.sha256Hash && v.sha256Hash.toLowerCase().includes(q)
      const matchQr =
        v.qrArtifacts &&
        v.qrArtifacts.some(
          (qr) =>
            (qr.decodedPayload && qr.decodedPayload.toLowerCase().includes(q)) ||
            (qr.normalizedUrl && qr.normalizedUrl.toLowerCase().includes(q))
        )
      return matchFile || matchHash || matchQr
    })
  }, [visuals, searchQuery])

  // Filtered QR codes
  const filteredQrCodes = useMemo(() => {
    if (!searchQuery.trim()) return allQrCodes
    const q = searchQuery.toLowerCase()
    return allQrCodes.filter((qr) => {
      const matchPayload = qr.decodedPayload && qr.decodedPayload.toLowerCase().includes(q)
      const matchUrl = qr.normalizedUrl && qr.normalizedUrl.toLowerCase().includes(q)
      const matchFile = qr.imageFileName && qr.imageFileName.toLowerCase().includes(q)
      return matchPayload || matchUrl || matchFile
    })
  }, [allQrCodes, searchQuery])

  // Helper to build image content URL
  const getImageContentUrl = (visual) => {
    if (visual?.imageUrl) return visual.imageUrl
    if (emailId && visual?.id) {
      return `/api/v1/evidence/${emailId}/images/${visual.id}/content`
    }
    return null
  }

  // Helper for QR source image URL
  const getQrImageUrl = (qr) => {
    const imgId = qr.imageId
    if (emailId && imgId) {
      return `/api/v1/evidence/${emailId}/images/${imgId}/content`
    }
    return null
  }

  // 1. ATTACHMENTS MODE
  if (mode === 'attachments') {
    return (
      <div className="space-y-6">
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-4">
          <div className="pb-3 border-b border-[#E4E7EC]">
            <h2 className="text-sm font-bold text-[#202938]">
              Attached files ({attachments.length})
            </h2>
            <p className="text-xs text-[#202938] mt-0.5">
              Files attached to or embedded in the email message.
            </p>
          </div>

          {attachments.length === 0 ? (
            <div className="p-8 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] text-center text-xs text-[#202938]">
              No attachment files found in this message.
            </div>
          ) : (
            <div className="space-y-3">
              {attachments.map((att, idx) => {
                const isImage =
                  att.mimeType?.startsWith('image/') ||
                  (att.filename && (
                    att.filename.toLowerCase().endsWith('.png') ||
                    att.filename.toLowerCase().endsWith('.jpg') ||
                    att.filename.toLowerCase().endsWith('.jpeg') ||
                    att.filename.toLowerCase().endsWith('.gif') ||
                    att.filename.toLowerCase().endsWith('.webp')
                  ))

                const matchingVisual = visuals?.find(
                  (v) =>
                    (v.filename && att.filename && v.filename.toLowerCase() === att.filename.toLowerCase()) ||
                    (v.sha256Hash && att.sha256Hash && v.sha256Hash.toLowerCase() === att.sha256Hash.toLowerCase())
                )

                const matchingQr =
                  allQrCodes?.find(
                    (q) =>
                      (q.imageFileName && att.filename && q.imageFileName.toLowerCase() === att.filename.toLowerCase()) ||
                      (matchingVisual && q.imageId === matchingVisual.id)
                  ) ||
                  (att.qrDetected
                    ? {
                        decodedPayload: att.qrPayload,
                        payloadType: att.qrPayloadType,
                        normalizedUrl: att.qrNormalizedUrl
                      }
                    : null)

                const hasQr = Boolean(att.qrDetected || matchingQr)
                const imageUrl = matchingVisual ? getImageContentUrl(matchingVisual) : null

                return (
                  <div
                    key={idx}
                    className={`p-4 rounded-xl border transition-colors space-y-3 ${
                      hasQr
                        ? 'bg-[#F0FDF4] border-[#BBF7D0]'
                        : att.riskyExtension
                        ? 'bg-[#FEF2F2] border-red-200'
                        : 'bg-[#F5F6F8] border-[#E4E7EC]'
                    }`}
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-[#E4E7EC]">
                      <div className="flex items-center gap-2">
                        {isImage ? (
                          <ImageIcon className="h-4 w-4 text-[#2563EB] shrink-0" />
                        ) : (
                          <Paperclip className="h-4 w-4 text-[#2563EB] shrink-0" />
                        )}
                        <span className="text-xs font-bold font-mono text-[#202938]">
                          {att.filename}
                        </span>
                        <span className="text-xs text-[#202938]">
                          ({formatBytes(att.sizeBytes)})
                        </span>
                      </div>

                      <div className="flex flex-wrap items-center gap-2">
                        {hasQr && (
                          <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-[#EFF6FF] text-[#2563EB] border border-blue-200 flex items-center gap-1">
                            <QrCode className="h-3 w-3" />
                            <span>Attachment + QR</span>
                          </span>
                        )}
                        {isImage && (
                          <span className="px-2 py-0.5 rounded text-[11px] bg-slate-100 text-[#202938] border border-[#E4E7EC]">
                            Image
                          </span>
                        )}
                        {att.riskyExtension && (
                          <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-[#FEF2F2] text-[#B42318] border border-red-200">
                            Risky file extension
                          </span>
                        )}
                        {att.inline && (
                          <span className="px-2 py-0.5 rounded text-[11px] bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
                            Inline file
                          </span>
                        )}
                        <span className="px-2 py-0.5 rounded text-[11px] bg-white text-[#202938] border border-[#E4E7EC]">
                          {att.mimeType}
                        </span>
                      </div>
                    </div>

                    <div className="flex flex-col sm:flex-row gap-4 items-start">
                      {isImage && imageUrl && (
                        <div className="w-24 h-24 bg-white border border-[#E4E7EC] rounded-lg overflow-hidden flex items-center justify-center shrink-0">
                          <img
                            src={imageUrl}
                            alt={att.filename}
                            className="max-h-full max-w-full object-contain p-1"
                            onError={(e) => {
                              e.currentTarget.style.display = 'none'
                            }}
                          />
                        </div>
                      )}

                      <div className="flex-1 space-y-2 w-full">
                        <div className="flex items-start gap-2 bg-white p-2.5 rounded-lg border border-[#E4E7EC] font-mono text-xs">
                          <Hash className="h-3.5 w-3.5 text-[#202938] mt-0.5 shrink-0" />
                          <div className="break-all flex-1 text-[11px]">
                            <span className="text-[#202938]">SHA-256: </span>
                            <span className="text-[#202938] select-all">{att.sha256Hash}</span>
                          </div>
                        </div>

                        {att.contentId && (
                          <div className="text-[11px] text-[#202938]">
                            <span>Content-ID: &lt;{att.contentId}&gt;</span>
                          </div>
                        )}

                        {hasQr && (
                          <div className="p-3 bg-white rounded-lg border border-[#BBF7D0] space-y-2 text-xs">
                            <div className="flex items-center justify-between">
                              <span className="font-semibold text-[#15803D] flex items-center gap-1.5 text-xs">
                                <QrCode className="h-3.5 w-3.5" />
                                <span>Decoded QR payload ({matchingQr?.payloadType || 'PAYLOAD'}):</span>
                              </span>
                              {onNavigateTab && (
                                <button
                                  onClick={() => onNavigateTab('qr')}
                                  className="text-xs text-[#2563EB] hover:underline font-medium inline-flex items-center gap-1 cursor-pointer"
                                >
                                  <span>View in QR tab</span>
                                  <ExternalLink className="h-3 w-3" />
                                </button>
                              )}
                            </div>
                            <div className="p-2 bg-[#F5F6F8] rounded font-mono text-[11px] text-[#202938] break-all select-all">
                              {matchingQr?.decodedPayload || 'QR code payload detected'}
                            </div>
                            {matchingQr?.normalizedUrl && (
                              <div className="text-[11px] text-[#2563EB] font-mono break-all">
                                Destination: {matchingQr.normalizedUrl}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}

          {inlineImages.filter((img) => !attachments.some((a) => (a.sha256Hash && a.sha256Hash === img.sha256Hash) || (a.filename && a.filename === img.filename))).length > 0 && (
            <div className="space-y-3 pt-4 border-t border-[#E4E7EC]">
              <h3 className="text-xs font-bold text-[#202938] flex items-center gap-1.5">
                <ImageIcon className="h-4 w-4 text-[#2563EB]" />
                <span>Other embedded graphics ({inlineImages.filter((img) => !attachments.some((a) => (a.sha256Hash && a.sha256Hash === img.sha256Hash) || (a.filename && a.filename === img.filename))).length})</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {inlineImages
                  .filter((img) => !attachments.some((a) => (a.sha256Hash && a.sha256Hash === img.sha256Hash) || (a.filename && a.filename === img.filename)))
                  .map((img, idx) => (
                  <div
                    key={idx}
                    className="bg-[#F5F6F8] border border-[#E4E7EC] rounded-xl p-3.5 space-y-2 text-xs"
                  >
                    <div className="flex items-center justify-between text-[#202938] font-semibold truncate">
                      <span>{img.filename}</span>
                      <span className="text-[11px] text-[#202938] font-normal">
                        {formatBytes(img.sizeBytes)}
                      </span>
                    </div>
                    <div className="break-all text-[11px] font-mono bg-white p-2 rounded border border-[#E4E7EC]">
                      <span className="text-[#202938]">Hash: </span>
                      <span className="text-[#202938]">
                        {img.sha256Hash ? img.sha256Hash.substring(0, 16) + '...' : '—'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    )
  }

  // 2. DEDICATED QR CODES MODE
  if (mode === 'qr') {
    return (
      <div className="space-y-4">
        {/* QR Search Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-[#E4E7EC]">
          <div className="flex items-center gap-2">
            <QrCode className="h-4 w-4 text-[#2563EB]" />
            <span className="text-xs text-[#202938] font-semibold">
              QR Code Analysis ({filteredQrCodes.length} of {allQrCodes.length})
            </span>
          </div>

          <div className="relative w-full sm:w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-[#202938]" />
            <input
              id="qr-search-input"
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search QR decoded content or URLs..."
              className="w-full bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg pl-9 pr-3 py-1.5 text-xs text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB]"
            />
          </div>
        </div>

        {filteredQrCodes.length === 0 ? (
          <div className="p-12 text-center bg-white rounded-xl border border-[#E4E7EC]">
            <QrCode className="h-10 w-10 text-[#202938] mx-auto mb-3" />
            <p className="text-sm font-medium text-[#202938]">
              No QR codes detected
            </p>
            <p className="text-xs text-[#202938] mt-1 max-w-sm mx-auto">
              {searchQuery
                ? 'No QR codes match your search.'
                : 'No QR codes were found in any embedded images or attachments in this email.'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4">
            {filteredQrCodes.map((qr, idx) => {
              const qrImageUrl = getQrImageUrl(qr)
              return (
                <div
                  key={qr.id || idx}
                  className="bg-white p-5 rounded-xl border border-[#E4E7EC] space-y-4"
                >
                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pb-3 border-b border-[#E4E7EC]">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 rounded-lg bg-[#EFF6FF] border border-blue-100 text-[#2563EB]">
                        <QrCode className="h-4 w-4" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-[#202938]">
                            QR Code #{idx + 1}
                          </span>
                          <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
                            {qr.payloadType || 'PAYLOAD'}
                          </span>
                        </div>
                        {qr.imageFileName && (
                          <span className="text-[11px] text-[#202938]">
                            Source image: <span className="font-mono text-[#202938]">{qr.imageFileName}</span>
                          </span>
                        )}
                      </div>
                    </div>

                    {qr.normalizedUrl && onNavigateTab && (
                      <button
                        onClick={() => onNavigateTab('urls')}
                        className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-[#F5F6F8] hover:bg-slate-100 border border-[#E4E7EC] text-xs font-medium text-[#2563EB] cursor-pointer transition-colors"
                      >
                        <Link2 className="h-3.5 w-3.5" />
                        <span>Inspect in Links tab</span>
                      </button>
                    )}
                  </div>

                  <div className="flex flex-col md:flex-row gap-4 items-start">
                    {/* Source QR Image Thumbnail */}
                    {qrImageUrl && (
                      <div className="w-full sm:w-36 h-36 bg-[#F5F6F8] border border-[#E4E7EC] rounded-xl overflow-hidden flex items-center justify-center shrink-0 relative group">
                        <img
                          src={qrImageUrl}
                          alt="QR Code source"
                          className="max-h-full max-w-full object-contain p-2"
                          onError={(e) => {
                            e.currentTarget.style.display = 'none'
                          }}
                        />
                        <a
                          href={qrImageUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="absolute inset-0 bg-black/40 text-white opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center gap-1 text-[11px] font-medium"
                          title="Open full image"
                        >
                          <ExternalLink className="h-4 w-4" />
                          <span>View Image</span>
                        </a>
                      </div>
                    )}

                    {/* QR Payload Details */}
                    <div className="flex-1 space-y-3 w-full">
                      {/* Decoded Content */}
                      <div className="space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-semibold text-[#202938]">
                            Decoded payload content:
                          </span>
                          <button
                            id={`copy-qr-decoded-${idx}`}
                            onClick={() => copyValue(`qr-decoded-${idx}`, qr.decodedPayload)}
                            className="text-xs text-[#2563EB] hover:underline flex items-center gap-1 cursor-pointer font-medium"
                          >
                            {copiedKey === `qr-decoded-${idx}` ? (
                              <>
                                <Check className="h-3.5 w-3.5 text-[#15803D]" />
                                <span>Copied</span>
                              </>
                            ) : (
                              <>
                                <Copy className="h-3.5 w-3.5" />
                                <span>Copy</span>
                              </>
                            )}
                          </button>
                        </div>
                        <div className="p-3 bg-[#F5F6F8] rounded-lg border border-[#E4E7EC] font-mono text-xs text-[#202938] select-all break-all whitespace-pre-wrap max-h-32 overflow-y-auto">
                          {qr.decodedPayload}
                        </div>
                      </div>

                      {/* Normalized URL Destination */}
                      {qr.normalizedUrl && (
                        <div className="space-y-1 pt-2 border-t border-[#E4E7EC]">
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-semibold text-[#2563EB] flex items-center gap-1.5">
                              <Link2 className="h-3.5 w-3.5" />
                              <span>Normalized destination URL:</span>
                            </span>
                            <button
                              id={`copy-qr-url-${idx}`}
                              onClick={() => copyValue(`qr-url-${idx}`, qr.normalizedUrl)}
                              className="text-xs text-[#2563EB] hover:underline flex items-center gap-1 cursor-pointer font-medium"
                            >
                              {copiedKey === `qr-url-${idx}` ? (
                                <>
                                  <Check className="h-3.5 w-3.5 text-[#15803D]" />
                                  <span>Copied</span>
                                </>
                              ) : (
                                <>
                                  <Copy className="h-3.5 w-3.5" />
                                  <span>Copy</span>
                                </>
                              )}
                            </button>
                          </div>
                          <div className="p-2.5 bg-blue-50/50 rounded-lg border border-blue-100 text-xs font-mono text-[#202938] break-all select-all">
                            {qr.normalizedUrl}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    )
  }

  // 3. VISUALS / IMAGES MODE
  return (
    <div className="space-y-4">
      {/* Visual Search Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-[#E4E7EC]">
        <div className="flex items-center gap-2">
          <ImageIcon className="h-4 w-4 text-[#2563EB]" />
          <span className="text-xs text-[#202938] font-semibold">
            Extracted Images ({filteredVisuals.length} of {visuals.length})
          </span>
        </div>

        <div className="relative w-full sm:w-80">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-[#202938]" />
          <input
            id="visuals-search-input"
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search images or QR payloads..."
            className="w-full bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg pl-9 pr-3 py-1.5 text-xs text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB]"
          />
        </div>
      </div>

      {filteredVisuals.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-xl border border-[#E4E7EC]">
          <ImageIcon className="h-10 w-10 text-[#202938] mx-auto mb-3" />
          <p className="text-sm font-medium text-[#202938]">
            No image files found
          </p>
          <p className="text-xs text-[#202938] mt-1 max-w-sm mx-auto">
            {searchQuery
              ? 'No images match your search.'
              : 'No inline images or image attachments were detected in this email.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {filteredVisuals.map((v, idx) => {
            const imageUrl = getImageContentUrl(v)
            const hasQr = v.qrArtifacts && v.qrArtifacts.length > 0
            return (
              <div
                key={v.id || idx}
                className="bg-white p-5 rounded-xl border border-[#E4E7EC] space-y-4"
              >
                {/* Header row: image metadata + SHA-256 */}
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pb-3 border-b border-[#E4E7EC]">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-lg bg-[#EFF6FF] border border-blue-100 text-[#2563EB] shrink-0">
                      <ImageIcon className="h-5 w-5" />
                    </div>
                    <div className="space-y-1">
                      <span className="text-xs font-semibold text-[#202938] block break-all">
                        {v.filename || 'unnamed-image'}
                      </span>
                      <div className="flex flex-wrap items-center gap-2 text-[11px] text-[#202938]">
                        <span className="px-2 py-0.5 rounded bg-[#F5F6F8] border border-[#E4E7EC]">
                          {v.contentType || 'image/*'}
                        </span>
                        <span>{formatBytes(v.fileSizeBytes || v.byteSize || 0)}</span>
                        {hasQr && (
                          <span className="px-2 py-0.5 rounded font-semibold bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
                            {v.qrArtifacts.length} QR code{v.qrArtifacts.length > 1 ? 's' : ''} detected
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 bg-[#F5F6F8] px-3 py-1.5 rounded-lg border border-[#E4E7EC] font-mono text-[11px] shrink-0">
                    <Hash className="h-3 w-3 text-[#2563EB]" />
                    <span className="text-[#202938]">SHA-256:</span>
                    <span className="text-[#202938] select-all max-w-[140px] md:max-w-[200px] truncate">
                      {v.sha256Hash}
                    </span>
                    <button
                      id={`copy-visual-hash-${idx}`}
                      onClick={() => copyValue(`visual-hash-${idx}`, v.sha256Hash)}
                      className="p-1 hover:bg-white rounded text-[#202938] transition-colors cursor-pointer"
                      title="Copy image SHA-256"
                    >
                      {copiedKey === `visual-hash-${idx}` ? (
                        <Check className="h-3 w-3 text-[#15803D]" />
                      ) : (
                        <Copy className="h-3 w-3" />
                      )}
                    </button>
                  </div>
                </div>

                {/* Body row: Image Preview & Details */}
                <div className="flex flex-col sm:flex-row gap-5 items-start">
                  {/* Real Image Preview Container */}
                  <div className="w-full sm:w-48 h-48 bg-[#F5F6F8] border border-[#E4E7EC] rounded-xl overflow-hidden flex items-center justify-center relative shrink-0 group">
                    {imageUrl ? (
                      <>
                        <img
                          src={imageUrl}
                          alt={v.filename || 'Visual artifact'}
                          className="max-h-full max-w-full object-contain p-2"
                          onError={(e) => {
                            e.currentTarget.style.display = 'none'
                            if (e.currentTarget.nextSibling) {
                              e.currentTarget.nextSibling.style.display = 'flex'
                            }
                          }}
                        />
                        <div
                          style={{ display: 'none' }}
                          className="flex flex-col items-center justify-center p-4 text-[#202938]"
                        >
                          <ImageIcon className="h-8 w-8 mb-1" />
                          <span className="text-[11px]">Preview unavailable</span>
                        </div>
                      </>
                    ) : (
                      <div className="flex flex-col items-center justify-center p-4 text-[#202938]">
                        <ImageIcon className="h-8 w-8 mb-1" />
                        <span className="text-[11px]">No preview</span>
                      </div>
                    )}

                    {/* Hover actions overlay */}
                    {imageUrl && (
                      <div className="absolute inset-0 bg-black/40 text-white opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3">
                        <a
                          href={imageUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="p-2 rounded-lg bg-white/20 hover:bg-white/30 backdrop-blur-sm text-white transition-colors"
                          title="Open full image in new tab"
                        >
                          <ExternalLink className="h-4 w-4" />
                        </a>
                        <a
                          href={imageUrl}
                          download={v.filename || `image-${idx}.png`}
                          className="p-2 rounded-lg bg-white/20 hover:bg-white/30 backdrop-blur-sm text-white transition-colors"
                          title="Download image"
                        >
                          <Download className="h-4 w-4" />
                        </a>
                      </div>
                    )}
                  </div>

                  {/* Image Information & QR Detection details */}
                  <div className="flex-1 space-y-4 w-full">
                    <div className="flex flex-wrap items-center gap-2">
                      {imageUrl && (
                        <>
                          <a
                            href={imageUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#EFF6FF] hover:bg-blue-100 text-[#2563EB] text-xs font-medium border border-blue-100 transition-colors"
                          >
                            <ExternalLink className="h-3.5 w-3.5" />
                            <span>View original image</span>
                          </a>
                          <a
                            href={imageUrl}
                            download={v.filename || `image-${idx}.png`}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white hover:bg-[#F5F6F8] text-[#202938] text-xs font-medium border border-[#E4E7EC] transition-colors"
                          >
                            <Download className="h-3.5 w-3.5 text-[#202938]" />
                            <span>Download file</span>
                          </a>
                        </>
                      )}
                    </div>

                    {/* QR Code Detection Section inside Image card */}
                    <div className="space-y-3 bg-[#F5F6F8] p-4 rounded-xl border border-[#E4E7EC]">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-[#202938]">
                          <QrCode className="h-3.5 w-3.5 text-[#2563EB]" />
                          <span>QR code detection</span>
                        </div>
                        <span className="text-[11px] px-2 py-0.5 rounded-full bg-white border border-[#E4E7EC] text-[#202938]">
                          {hasQr ? `${v.qrArtifacts.length} detected` : 'None detected'}
                        </span>
                      </div>

                      {hasQr ? (
                        <div className="space-y-2.5">
                          {v.qrArtifacts.map((qr, qIdx) => (
                            <div
                              key={qr.id || qIdx}
                              className="p-3 bg-white rounded-lg border border-[#E4E7EC] space-y-2 text-xs"
                            >
                              <div className="flex items-center justify-between gap-2">
                                <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
                                  {qr.payloadType || 'PAYLOAD'}
                                </span>
                                {onNavigateTab && (
                                  <button
                                    onClick={() => onNavigateTab('qr')}
                                    className="text-[11px] text-[#2563EB] hover:underline font-medium cursor-pointer"
                                  >
                                    View in QR tab &rarr;
                                  </button>
                                )}
                              </div>

                              <div className="space-y-1">
                                <div className="flex items-center justify-between">
                                  <span className="text-[11px] text-[#202938]">
                                    Decoded payload:
                                  </span>
                                  <button
                                    id={`copy-visual-qr-${idx}-${qIdx}`}
                                    onClick={() =>
                                      copyValue(`visual-qr-${idx}-${qIdx}`, qr.decodedPayload)
                                    }
                                    className="text-[11px] text-[#2563EB] hover:underline flex items-center gap-1 cursor-pointer"
                                  >
                                    {copiedKey === `visual-qr-${idx}-${qIdx}` ? (
                                      <>
                                        <Check className="h-3 w-3 text-[#15803D]" />
                                        <span>Copied</span>
                                      </>
                                    ) : (
                                      <>
                                        <Copy className="h-3 w-3" />
                                        <span>Copy</span>
                                      </>
                                    )}
                                  </button>
                                </div>
                                <div className="p-2.5 bg-[#F5F6F8] rounded border border-[#E4E7EC] font-mono text-[11px] text-[#202938] select-all break-all whitespace-pre-wrap max-h-24 overflow-y-auto">
                                  {qr.decodedPayload}
                                </div>
                              </div>

                              {qr.normalizedUrl && (
                                <div className="space-y-1 pt-1 border-t border-[#E4E7EC]">
                                  <div className="flex items-center justify-between">
                                    <span className="text-[11px] text-[#2563EB]">
                                      Link in QR code:
                                    </span>
                                    <button
                                      id={`copy-visual-url-${idx}-${qIdx}`}
                                      onClick={() =>
                                        copyValue(`visual-url-${idx}-${qIdx}`, qr.normalizedUrl)
                                      }
                                      className="text-[11px] text-[#2563EB] hover:underline flex items-center gap-1 cursor-pointer"
                                    >
                                      {copiedKey === `visual-url-${idx}-${qIdx}` ? (
                                        <>
                                          <Check className="h-3 w-3 text-[#15803D]" />
                                          <span>Copied</span>
                                        </>
                                      ) : (
                                        <>
                                          <Copy className="h-3 w-3" />
                                          <span>Copy</span>
                                        </>
                                      )}
                                    </button>
                                  </div>
                                  <div className="text-[11px] font-mono text-[#202938] break-all select-all">
                                    {qr.normalizedUrl}
                                  </div>
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      ) : (
                        <div className="p-3 bg-white rounded-lg border border-[#E4E7EC] text-center">
                          <span className="text-xs text-[#202938]">
                            No QR codes were detected in this image.
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
