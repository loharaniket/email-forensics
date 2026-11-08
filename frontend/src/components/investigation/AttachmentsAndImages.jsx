import React, { useState, useMemo } from 'react'
import {
  Paperclip,
  Image as ImageIcon,
  QrCode,
  Search,
  Hash,
  Copy,
  Check
} from 'lucide-react'

export default function AttachmentsAndImages({
  mode = 'attachments', // 'attachments' or 'visuals'
  attachments = [],
  inlineImages = [],
  visuals = [],
  copiedKey,
  copyValue,
  formatBytes
}) {
  const [visualSearch, setVisualSearch] = useState('')

  const filteredVisuals = useMemo(() => {
    if (!visualSearch.trim()) return visuals
    const q = visualSearch.toLowerCase()
    return visuals.filter((v) => {
      const matchFile = v.filename && v.filename.toLowerCase().includes(q)
      const matchQr =
        v.qrArtifacts &&
        v.qrArtifacts.some(
          (qr) =>
            (qr.decodedPayload && qr.decodedPayload.toLowerCase().includes(q)) ||
            (qr.normalizedUrl && qr.normalizedUrl.toLowerCase().includes(q))
        )
      return matchFile || matchQr
    })
  }, [visuals, visualSearch])

  if (mode === 'attachments') {
    return (
      <div className="space-y-6">
        <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-4">
          <div className="pb-3 border-b border-[#E4E7EC]">
            <h2 className="text-sm font-bold text-[#202938]">
              Attached files ({attachments.length})
            </h2>
            <p className="text-xs text-[#667085] mt-0.5">
              Files attached to or embedded in the email message.
            </p>
          </div>

          {attachments.length === 0 ? (
            <div className="p-8 rounded-xl bg-[#F5F6F8] border border-[#E4E7EC] text-center text-xs text-[#667085]">
              No attachment files found in this message.
            </div>
          ) : (
            <div className="space-y-3">
              {attachments.map((att, idx) => (
                <div
                  key={idx}
                  className={`p-4 rounded-xl border transition-colors ${
                    att.riskyExtension
                      ? 'bg-[#FEF2F2] border-red-200'
                      : 'bg-[#F5F6F8] border-[#E4E7EC]'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-[#E4E7EC]">
                    <div className="flex items-center gap-2">
                      <Paperclip className="h-4 w-4 text-[#2563EB] shrink-0" />
                      <span className="text-xs font-bold font-mono text-[#202938]">
                        {att.filename}
                      </span>
                      <span className="text-xs text-[#667085]">
                        ({formatBytes(att.sizeBytes)})
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
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
                      <span className="px-2 py-0.5 rounded text-[11px] bg-white text-[#667085] border border-[#E4E7EC]">
                        {att.mimeType}
                      </span>
                    </div>
                  </div>

                  <div className="pt-2 text-xs font-mono space-y-1.5">
                    <div className="flex items-start gap-2 bg-white p-2.5 rounded-lg border border-[#E4E7EC]">
                      <Hash className="h-3.5 w-3.5 text-[#98A2B3] mt-0.5 shrink-0" />
                      <div className="break-all flex-1 text-[11px]">
                        <span className="text-[#667085]">SHA-256: </span>
                        <span className="text-[#202938] select-all">{att.sha256Hash}</span>
                      </div>
                    </div>

                    {att.contentId && (
                      <div className="text-[11px] text-[#667085] pt-0.5">
                        <span>Content-ID: &lt;{att.contentId}&gt;</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          {inlineImages.length > 0 && (
            <div className="space-y-3 pt-4 border-t border-[#E4E7EC]">
              <h3 className="text-xs font-bold text-[#667085] flex items-center gap-1.5">
                <ImageIcon className="h-4 w-4 text-[#2563EB]" />
                <span>Extracted inline images ({inlineImages.length})</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {inlineImages.map((img, idx) => (
                  <div
                    key={idx}
                    className="bg-[#F5F6F8] border border-[#E4E7EC] rounded-xl p-3.5 space-y-2 text-xs"
                  >
                    <div className="flex items-center justify-between text-[#202938] font-semibold truncate">
                      <span>{img.filename}</span>
                      <span className="text-[11px] text-[#667085] font-normal">
                        {formatBytes(img.sizeBytes)}
                      </span>
                    </div>
                    <div className="break-all text-[11px] font-mono bg-white p-2 rounded border border-[#E4E7EC]">
                      <span className="text-[#667085]">Hash: </span>
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

  // Visuals & QR codes mode
  return (
    <div className="space-y-4">
      {/* Visual search header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-[#E4E7EC]">
        <div className="flex items-center gap-2">
          <QrCode className="h-4 w-4 text-[#2563EB]" />
          <span className="text-xs text-[#202938] font-semibold">
            Images and QR codes ({filteredVisuals.length} of {visuals.length})
          </span>
        </div>

        <div className="relative w-full sm:w-80">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-[#98A2B3]" />
          <input
            id="visuals-search-input"
            type="text"
            value={visualSearch}
            onChange={(e) => setVisualSearch(e.target.value)}
            placeholder="Search images or QR payloads..."
            className="w-full bg-[#F5F6F8] border border-[#E4E7EC] rounded-lg pl-9 pr-3 py-1.5 text-xs text-[#202938] placeholder-[#98A2B3] focus:outline-none focus:border-[#2563EB]"
          />
        </div>
      </div>

      {filteredVisuals.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-xl border border-[#E4E7EC]">
          <ImageIcon className="h-10 w-10 text-[#98A2B3] mx-auto mb-3" />
          <p className="text-sm font-medium text-[#202938]">
            No image files found
          </p>
          <p className="text-xs text-[#667085] mt-1 max-w-sm mx-auto">
            {visualSearch
              ? 'No images match your search.'
              : 'No inline images or image attachments were detected in this email.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {filteredVisuals.map((v, idx) => (
            <div
              key={v.id || idx}
              className="bg-white p-5 rounded-xl border border-[#E4E7EC] space-y-4"
            >
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pb-3 border-b border-[#E4E7EC]">
                <div className="flex items-start gap-3">
                  <div className="p-2.5 rounded-lg bg-[#EFF6FF] border border-blue-100 text-[#2563EB] shrink-0">
                    <ImageIcon className="h-5 w-5" />
                  </div>
                  <div className="space-y-1">
                    <span className="text-xs font-semibold text-[#202938] block break-all">
                      {v.filename || 'unnamed-image'}
                    </span>
                    <div className="flex flex-wrap items-center gap-2 text-[11px] text-[#667085]">
                      <span className="px-2 py-0.5 rounded bg-[#F5F6F8] border border-[#E4E7EC]">
                        {v.contentType || 'image/*'}
                      </span>
                      <span>{formatBytes(v.fileSizeBytes || v.byteSize || 0)}</span>
                      {v.width && v.height && (
                        <span>
                          {v.width} &times; {v.height} px
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 bg-[#F5F6F8] px-3 py-1.5 rounded-lg border border-[#E4E7EC] font-mono text-[11px] shrink-0">
                  <Hash className="h-3 w-3 text-[#2563EB]" />
                  <span className="text-[#667085]">SHA-256:</span>
                  <span className="text-[#202938] select-all max-w-[140px] md:max-w-[200px] truncate">
                    {v.sha256Hash}
                  </span>
                  <button
                    id={`copy-visual-hash-${idx}`}
                    onClick={() => copyValue(`visual-hash-${idx}`, v.sha256Hash)}
                    className="p-1 hover:bg-white rounded text-[#667085] hover:text-[#202938] transition-colors cursor-pointer"
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

              {/* QR Code Detection Section */}
              <div className="space-y-3 bg-[#F5F6F8] p-4 rounded-xl border border-[#E4E7EC]">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-xs font-semibold text-[#202938]">
                    <QrCode className="h-3.5 w-3.5 text-[#2563EB]" />
                    <span>QR code detection</span>
                  </div>
                  <span className="text-[11px] px-2 py-0.5 rounded-full bg-white border border-[#E4E7EC] text-[#667085]">
                    {v.qrArtifacts && v.qrArtifacts.length > 0
                      ? `${v.qrArtifacts.length} detected`
                      : 'None detected'}
                  </span>
                </div>

                {v.qrArtifacts && v.qrArtifacts.length > 0 ? (
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
                          {qr.moduleCount && (
                            <span className="text-[11px] text-[#667085]">
                              Modules: {qr.moduleCount}
                            </span>
                          )}
                        </div>

                        <div className="space-y-1">
                          <div className="flex items-center justify-between">
                            <span className="text-[11px] text-[#667085]">
                              Decoded payload (safe preview):
                            </span>
                            <button
                              id={`copy-qr-payload-${idx}-${qIdx}`}
                              onClick={() =>
                                copyValue(`qr-payload-${idx}-${qIdx}`, qr.decodedPayload)
                              }
                              className="text-[11px] text-[#2563EB] hover:underline flex items-center gap-1 cursor-pointer"
                            >
                              {copiedKey === `qr-payload-${idx}-${qIdx}` ? (
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
                          <div className="p-2.5 bg-[#F5F6F8] rounded border border-[#E4E7EC] font-mono text-[11px] text-[#202938] select-all break-all whitespace-pre-wrap max-h-28 overflow-y-auto">
                            {qr.decodedPayload}
                          </div>
                        </div>

                        {qr.normalizedUrl && (
                          <div className="space-y-1 pt-1 border-t border-[#E4E7EC]">
                            <div className="flex items-center justify-between">
                              <span className="text-[11px] text-[#2563EB]">
                                Link address in QR code:
                              </span>
                              <button
                                id={`copy-qr-url-${idx}-${qIdx}`}
                                onClick={() =>
                                  copyValue(`qr-url-${idx}-${qIdx}`, qr.normalizedUrl)
                                }
                                className="text-[11px] text-[#2563EB] hover:underline flex items-center gap-1 cursor-pointer"
                              >
                                {copiedKey === `qr-url-${idx}-${qIdx}` ? (
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
                    <span className="text-xs text-[#667085]">
                      No QR codes were detected in this image.
                    </span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

