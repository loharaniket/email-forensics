import React, { useState, useMemo } from 'react'
import {
  Brain,
  ShieldCheck,
  AlertOctagon,
  Info
} from 'lucide-react'

export default function MessageContent({
  mode = 'content', // 'content' or 'body'
  contentAnalysis,
  parsed = {}
}) {
  const [contentCategoryFilter, setContentCategoryFilter] = useState('ALL')
  const [contentChannelFilter, setContentChannelFilter] = useState('ALL')
  const [contentSearch, setContentSearch] = useState('')
  const [bodyMode, setBodyMode] = useState('extracted')

  const predictedLabel =
    contentAnalysis?.predictedLabel ||
    contentAnalysis?.mlPredictedLabel ||
    'INSUFFICIENT_DATA'
  const probability =
    contentAnalysis?.probability != null
      ? contentAnalysis.probability
      : contentAnalysis?.mlProbability != null
      ? contentAnalysis.mlProbability
      : 0
  const urgencyDetected = !!(
    contentAnalysis?.urgency || contentAnalysis?.urgencyDetected
  )
  const credentialRequestDetected = !!(
    contentAnalysis?.credentialRequest ||
    contentAnalysis?.credentialRequestDetected
  )
  const paymentInstructionDetected = !!(
    contentAnalysis?.paymentInstruction ||
    contentAnalysis?.paymentInstructionDetected
  )
  const impersonationDetected = !!(
    contentAnalysis?.impersonation || contentAnalysis?.impersonationDetected
  )
  const secrecyDetected = !!(
    contentAnalysis?.secrecy || contentAnalysis?.secrecyDetected
  )
  const promptMatches =
    contentAnalysis?.promptMatches ||
    contentAnalysis?.promptInjectionMatches ||
    []
  const socialMatches = contentAnalysis?.socialEngineeringMatches || []
  const promptInjectionDetected = !!(
    contentAnalysis?.promptInjectionDetected || promptMatches.length > 0
  )

  const filteredMatches = useMemo(() => {
    return socialMatches.filter((m) => {
      const matchCat =
        contentCategoryFilter === 'ALL' || m.category === contentCategoryFilter
      const matchChannel =
        contentChannelFilter === 'ALL' ||
        (m.sourceChannel || m.channel) === contentChannelFilter
      const matchText =
        !contentSearch.trim() ||
        (m.matchedText &&
          m.matchedText.toLowerCase().includes(contentSearch.toLowerCase())) ||
        (m.matchedPattern &&
          m.matchedPattern.toLowerCase().includes(contentSearch.toLowerCase())) ||
        (m.explanation &&
          m.explanation.toLowerCase().includes(contentSearch.toLowerCase()))
      return matchCat && matchChannel && matchText
    })
  }, [socialMatches, contentCategoryFilter, contentChannelFilter, contentSearch])

  if (mode === 'body') {
    return (
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-[#E4E7EC]">
          <div className="flex items-center gap-2 text-xs text-[#667085]">
            <Info className="h-4 w-4 text-[#2563EB]" />
            <span>Isolated message preview</span>
          </div>

          <div className="flex items-center gap-1.5 text-xs">
            <button
              id="body-mode-extracted-btn"
              onClick={() => setBodyMode('extracted')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
                bodyMode === 'extracted'
                  ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                  : 'text-[#667085] hover:text-[#202938] hover:bg-slate-50'
              }`}
            >
              Extracted text
            </button>
            <button
              id="body-mode-plain-btn"
              onClick={() => setBodyMode('plain')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
                bodyMode === 'plain'
                  ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                  : 'text-[#667085] hover:text-[#202938] hover:bg-slate-50'
              }`}
            >
              Plain text
            </button>
            <button
              id="body-mode-html-btn"
              onClick={() => setBodyMode('html')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
                bodyMode === 'html'
                  ? 'bg-[#EFF6FF] text-[#2563EB] border border-blue-100'
                  : 'text-[#667085] hover:text-[#202938] hover:bg-slate-50'
              }`}
            >
              HTML source
            </button>
          </div>
        </div>

        <div className="bg-white rounded-xl p-6 border border-[#E4E7EC] max-h-[600px] overflow-y-auto">
          <pre className="font-mono text-xs text-[#202938] whitespace-pre-wrap select-text leading-relaxed">
            {bodyMode === 'extracted'
              ? parsed?.extractedText || parsed?.plainTextBody || '(No text content extracted)'
              : bodyMode === 'plain'
              ? parsed?.plainTextBody || '(No plain text body found in message)'
              : parsed?.htmlBody || '(No HTML body found in message)'}
          </pre>
        </div>
      </div>
    )
  }

  // Content & ML Analysis Mode
  if (!contentAnalysis) {
    return (
      <div className="p-8 text-center text-xs text-[#667085] bg-white rounded-xl border border-[#E4E7EC]">
        No content analysis data available for this email.
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Machine Learning Classification Card */}
      <div
        id="ml-baseline-card"
        className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-4"
      >
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#E4E7EC]">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-[#EFF6FF] border border-blue-100 text-[#2563EB]">
              <Brain className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-[#202938]">
                Machine learning classification
              </h3>
              <p className="text-xs text-[#667085]">
                Model: {contentAnalysis.mlModelVersion || 'Logistic regression baseline'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className="text-xs text-[#667085]">
              Status: <span className="text-[#15803D] font-medium">{contentAnalysis.mlStatus}</span>
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-center pt-1">
          <div className="space-y-1">
            <span className="text-xs text-[#667085] block">
              Predicted classification
            </span>
            <div
              id="ml-predicted-label"
              className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-semibold ${
                predictedLabel === 'PHISHING'
                  ? 'bg-[#FEF2F2] border-red-200 text-[#B42318]'
                  : predictedLabel === 'BENIGN'
                  ? 'bg-[#F0FDF4] border-green-200 text-[#15803D]'
                  : 'bg-slate-100 border-[#E4E7EC] text-[#667085]'
              }`}
            >
              {predictedLabel === 'PHISHING' && (
                <AlertOctagon className="h-4 w-4 text-[#B42318]" />
              )}
              {predictedLabel === 'BENIGN' && (
                <ShieldCheck className="h-4 w-4 text-[#15803D]" />
              )}
              <span>{predictedLabel === 'PHISHING' ? 'Phishing' : predictedLabel === 'BENIGN' ? 'Legitimate' : predictedLabel}</span>
            </div>
          </div>

          <div className="md:col-span-2 space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-[#667085]">Phishing probability score</span>
              <span
                className={`font-mono font-bold ${
                  probability >= 0.7
                    ? 'text-[#B42318]'
                    : probability >= 0.4
                    ? 'text-[#B45309]'
                    : 'text-[#15803D]'
                }`}
              >
                {(probability * 100).toFixed(1)}%
              </span>
            </div>
            <div
              id="ml-probability-bar"
              className="w-full bg-[#F5F6F8] rounded-full h-2.5 overflow-hidden border border-[#E4E7EC]"
            >
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  probability >= 0.7
                    ? 'bg-[#B42318]'
                    : probability >= 0.4
                    ? 'bg-[#B45309]'
                    : 'bg-[#15803D]'
                }`}
                style={{
                  width: `${Math.min(100, Math.max(2, probability * 100))}%`
                }}
              />
            </div>
            <span className="text-[11px] text-[#667085] block">
              Estimated based on words and phrases in the subject and message text.
            </span>
          </div>
        </div>

      </div>

      {/* Prompt Injection Alert / Status */}
      <div>
        {promptInjectionDetected ? (
          <div
            id="prompt-injection-alert"
            className="p-5 rounded-xl bg-[#FEF2F2] border border-[#FEE2E2] space-y-3"
          >
            <div className="flex items-center gap-2 text-[#B42318] font-bold text-sm">
              <AlertOctagon className="h-5 w-5 shrink-0" />
              <span>Prompt injection detected</span>
            </div>
            <p className="text-xs text-[#B42318] leading-relaxed">
              This email contains instructions attempting to override or trick automated AI tools.
            </p>

            <div className="space-y-2 pt-1">
              {promptMatches.map((m, idx) => (
                <div
                  key={idx}
                  className="p-3 rounded-lg bg-white border border-red-200 space-y-1.5 text-xs"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-[#B42318]">{m.category || m.patternName}</span>
                    <span className="px-2 py-0.5 rounded text-[11px] bg-[#FEF2F2] text-[#B42318] border border-red-200">
                      Channel: {m.sourceChannel || m.channel}
                    </span>
                  </div>
                  <div className="p-2 rounded bg-[#F5F6F8] font-mono text-[11px] text-[#202938] select-all break-all">
                    "{m.matchedPattern || m.matchedText}"
                  </div>
                  {m.explanation && (
                    <p className="text-xs text-[#667085]">{m.explanation}</p>
                  )}
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div
            id="prompt-injection-clean"
            className="p-4 rounded-xl bg-[#F0FDF4] border border-[#DCFCE7] flex items-center gap-3 text-xs"
          >
            <ShieldCheck className="h-5 w-5 text-[#15803D] shrink-0" />
            <div>
              <span className="font-bold text-[#15803D] block">
                No prompt injection detected
              </span>
              <span className="text-[#667085]">
                Scanned message text and QR payloads without finding instructions targeting AI tools.
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Social Engineering Indicators */}
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-4">
        <div>
          <h2 className="text-sm font-bold text-[#202938]">
            Suspicious language indicators
          </h2>
          <p className="text-xs text-[#667085] mt-0.5">
            Indicators detected across subject lines, body text, and embedded QR codes.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {/* 1. Urgency */}
          <div
            id="social-card-urgency"
            className={`p-4 rounded-xl border space-y-2 transition-colors ${
              urgencyDetected
                ? 'bg-[#FEF2F2] border-red-200'
                : 'bg-[#F5F6F8] border-[#E4E7EC]'
            }`}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#202938]">Urgent language</span>
              <span
                className={`text-[11px] px-2 py-0.5 rounded font-medium ${
                  urgencyDetected
                    ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200 font-semibold'
                    : 'bg-white text-[#667085] border border-[#E4E7EC]'
                }`}
              >
                {urgencyDetected ? 'Detected' : 'Clear'}
              </span>
            </div>
            <p className="text-xs text-[#667085]">
              Urgent or threatening language, deadlines or account suspensions.
            </p>
          </div>

          {/* 2. Credentials */}
          <div
            id="social-card-credential"
            className={`p-4 rounded-xl border space-y-2 transition-colors ${
              credentialRequestDetected
                ? 'bg-[#FEF2F2] border-red-200'
                : 'bg-[#F5F6F8] border-[#E4E7EC]'
            }`}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#202938]">Password requests</span>
              <span
                className={`text-[11px] px-2 py-0.5 rounded font-medium ${
                  credentialRequestDetected
                    ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200 font-semibold'
                    : 'bg-white text-[#667085] border border-[#E4E7EC]'
                }`}
              >
                {credentialRequestDetected ? 'Detected' : 'Clear'}
              </span>
            </div>
            <p className="text-xs text-[#667085]">
              Requests for passwords, login details, or verification codes.
            </p>
          </div>

          {/* 3. Payment */}
          <div
            id="social-card-payment"
            className={`p-4 rounded-xl border space-y-2 transition-colors ${
              paymentInstructionDetected
                ? 'bg-[#FEF2F2] border-red-200'
                : 'bg-[#F5F6F8] border-[#E4E7EC]'
            }`}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#202938]">Payment requests</span>
              <span
                className={`text-[11px] px-2 py-0.5 rounded font-medium ${
                  paymentInstructionDetected
                    ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200 font-semibold'
                    : 'bg-white text-[#667085] border border-[#E4E7EC]'
                }`}
              >
                {paymentInstructionDetected ? 'Detected' : 'Clear'}
              </span>
            </div>
            <p className="text-xs text-[#667085]">
              Requests for money, invoice changes, or wire transfers.
            </p>
          </div>

          {/* 4. Impersonation */}
          <div
            id="social-card-impersonation"
            className={`p-4 rounded-xl border space-y-2 transition-colors ${
              impersonationDetected
                ? 'bg-[#FEF2F2] border-red-200'
                : 'bg-[#F5F6F8] border-[#E4E7EC]'
            }`}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#202938]">Impersonation</span>
              <span
                className={`text-[11px] px-2 py-0.5 rounded font-medium ${
                  impersonationDetected
                    ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200 font-semibold'
                    : 'bg-white text-[#667085] border border-[#E4E7EC]'
                }`}
              >
                {impersonationDetected ? 'Detected' : 'Clear'}
              </span>
            </div>
            <p className="text-xs text-[#667085]">
              Someone pretending to be another person or organisation.
            </p>
          </div>

          {/* 5. Secrecy */}
          <div
            id="social-card-secrecy"
            className={`p-4 rounded-xl border space-y-2 transition-colors ${
              secrecyDetected
                ? 'bg-[#FEF2F2] border-red-200'
                : 'bg-[#F5F6F8] border-[#E4E7EC]'
            }`}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#202938]">Secrecy / bypass</span>
              <span
                className={`text-[11px] px-2 py-0.5 rounded font-medium ${
                  secrecyDetected
                    ? 'bg-[#FEF2F2] text-[#B42318] border border-red-200 font-semibold'
                    : 'bg-white text-[#667085] border border-[#E4E7EC]'
                }`}
              >
                {secrecyDetected ? 'Detected' : 'Clear'}
              </span>
            </div>
            <p className="text-xs text-[#667085]">
              Requests to hide information or bypass normal procedures.
            </p>
          </div>
        </div>

        {/* Detailed matched phrases */}
        <div className="pt-4 border-t border-[#E4E7EC] space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <span className="text-xs font-semibold text-[#202938]">
              Matched phrases ({filteredMatches.length})
            </span>

            <div className="flex flex-wrap items-center gap-2">
              <select
                id="content-category-filter"
                value={contentCategoryFilter}
                onChange={(e) => setContentCategoryFilter(e.target.value)}
                className="bg-white border border-[#E4E7EC] rounded-lg px-2.5 py-1 text-xs text-[#202938]"
              >
                <option value="ALL">All categories</option>
                <option value="URGENCY">Urgent language</option>
                <option value="CREDENTIAL">Password requests</option>
                <option value="PAYMENT">Payment requests</option>
                <option value="IMPERSONATION">Impersonation</option>
                <option value="SECRECY">Secrecy / bypass</option>
              </select>

              <select
                id="content-channel-filter"
                value={contentChannelFilter}
                onChange={(e) => setContentChannelFilter(e.target.value)}
                className="bg-white border border-[#E4E7EC] rounded-lg px-2.5 py-1 text-xs text-[#202938]"
              >
                <option value="ALL">All channels</option>
                <option value="SUBJECT">Subject</option>
                <option value="BODY">Body</option>
                <option value="HTML">HTML</option>
                <option value="QR">QR Code</option>
              </select>

              <input
                id="content-search-input"
                type="text"
                placeholder="Search phrase..."
                value={contentSearch}
                onChange={(e) => setContentSearch(e.target.value)}
                className="bg-white border border-[#E4E7EC] rounded-lg px-2.5 py-1 text-xs text-[#202938] placeholder-[#98A2B3]"
              />
            </div>
          </div>

          {filteredMatches.length === 0 ? (
            <p className="text-xs text-[#667085] py-2">
              No matching phrases found for the selected filter.
            </p>
          ) : (
            <div className="space-y-2">
              {filteredMatches.map((m, idx) => (
                <div
                  key={idx}
                  className="p-3 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
                >
                  <div className="space-y-0.5 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-[#202938]">{m.category}</span>
                      <span className="text-[11px] text-[#667085] bg-white px-2 py-0.5 rounded border border-[#E4E7EC]">
                        {m.sourceChannel || m.channel}
                      </span>
                    </div>
                    <p className="font-mono text-[#B42318] break-all">
                      "{m.matchedText || m.matchedPattern}"
                    </p>
                    {m.explanation && (
                      <p className="text-[11px] text-[#667085]">{m.explanation}</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

