import React, { useState } from 'react'
import {
  Sparkles,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  Tag,
  ShieldCheck,
  ChevronDown,
  ChevronRight
} from 'lucide-react'

export default function AutomatedAssessment({
  aiAssessment,
  isAiGenerating,
  onRegenerate
}) {
  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false)

  const isLiveAi = aiAssessment?.status === 'LIVE_AI'
  const isFailed = aiAssessment?.status === 'ASSESSMENT_FAILED'

  const methodLabel = isLiveAi
    ? 'AI-generated assessment'
    : isFailed
    ? 'Rule-based assessment (AI failed)'
    : 'Rule-based assessment'

  return (
    <div className="space-y-6">
      <div className="bg-white border border-[#E4E7EC] rounded-xl p-6 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 pb-4 border-b border-[#E4E7EC]">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#EFF6FF] text-[#2563EB] border border-blue-100">
                <Sparkles className="h-3.5 w-3.5" />
                {methodLabel}
              </span>

              {aiAssessment && (
                <span className="text-xs px-2.5 py-0.5 rounded bg-slate-100 text-[#667085] border border-[#E4E7EC]">
                  {aiAssessment.threatClassification || 'Assessed'}
                </span>
              )}
            </div>

            <h2 className="text-lg font-bold text-[#202938]">
              {aiAssessment ? aiAssessment.threatClassification : 'Generating assessment...'}
            </h2>
            <p className="text-xs text-[#667085]">
              Objective explanation based on extracted technical evidence and indicator findings.
            </p>
          </div>

          <button
            id="regenerate-ai-btn"
            onClick={onRegenerate}
            disabled={isAiGenerating}
            className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-xs font-medium bg-[#2563EB] hover:bg-blue-700 text-white transition-colors cursor-pointer disabled:opacity-50 shrink-0"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isAiGenerating ? 'animate-spin' : ''}`} />
            <span>{isAiGenerating ? 'Generating...' : 'Generate again'}</span>
          </button>
        </div>

        {/* Status notification */}
        {isLiveAi && (
          <div className="p-3.5 rounded-lg bg-[#F0FDF4] border border-[#DCFCE7] text-xs text-[#15803D] flex items-start gap-2.5">
            <CheckCircle2 className="h-4 w-4 shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold block">AI model assessment completed</span>
              <p className="text-[11px] text-[#667085] mt-0.5">
                Generated using {aiAssessment.provider} ({aiAssessment.modelName}). All findings were cross-referenced with extracted evidence.
              </p>
            </div>
          </div>
        )}

        {isFailed && (
          <div className="p-3.5 rounded-lg bg-[#FFFBEB] border border-amber-200 text-xs text-[#B42318] space-y-1">
            <div className="flex items-center gap-2 font-semibold">
              <AlertTriangle className="h-4 w-4 shrink-0 text-[#B45309]" />
              <span>AI service unavailable — rule-based fallback used</span>
            </div>
            <p className="text-[11px] text-[#667085]">
              The remote AI model call failed ({aiAssessment.errorMessage || 'Provider unreachable'}). A rule-based assessment was used instead.
            </p>
          </div>
        )}

        {/* Assessment summary narrative */}
        <div className="space-y-2 pt-1">
          <h3 className="text-xs font-bold uppercase tracking-wider text-[#667085]">
            Assessment summary
          </h3>
          <p className="text-sm text-[#202938] leading-relaxed bg-[#F5F6F8] p-4 rounded-lg border border-[#E4E7EC]">
            {aiAssessment?.assessmentSummary || aiAssessment?.summaryExplanation || 'No assessment narrative available.'}
          </p>
        </div>

        {/* Indicators and Evidence citations */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          {/* Key Indicators */}
          <div className="bg-[#F5F6F8] border border-[#E4E7EC] rounded-xl p-4 space-y-3">
            <div className="flex items-center gap-2 pb-2 border-b border-[#E4E7EC]">
              <ShieldCheck className="h-4 w-4 text-[#15803D]" />
              <h4 className="text-xs font-bold text-[#202938]">
                Key indicators cited ({aiAssessment?.keyIndicators?.length || 0})
              </h4>
            </div>

            {aiAssessment?.keyIndicators && aiAssessment.keyIndicators.length > 0 ? (
              <ul className="space-y-1.5 text-xs">
                {aiAssessment.keyIndicators.map((ind, idx) => (
                  <li key={idx} className="flex items-start gap-2 text-[#202938]">
                    <span className="h-1.5 w-1.5 rounded-full bg-[#2563EB] mt-1.5 shrink-0" />
                    <span>{ind}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-xs text-[#667085]">No specific key indicators listed.</p>
            )}
          </div>

          {/* Evidence Citations */}
          <div className="bg-[#F5F6F8] border border-[#E4E7EC] rounded-xl p-4 space-y-3">
            <div className="flex items-center gap-2 pb-2 border-b border-[#E4E7EC]">
              <Tag className="h-4 w-4 text-[#2563EB]" />
              <h4 className="text-xs font-bold text-[#202938]">
                Supporting evidence ({aiAssessment?.evidenceReferences?.length || 0})
              </h4>
            </div>

            {aiAssessment?.evidenceReferences && aiAssessment.evidenceReferences.length > 0 ? (
              <div className="flex flex-wrap gap-2">
                {aiAssessment.evidenceReferences.map((ref, idx) => (
                  <span
                    key={idx}
                    className="px-2.5 py-1 rounded-md text-xs font-mono bg-white text-[#202938] border border-[#E4E7EC]"
                  >
                    {ref}
                  </span>
                ))}
              </div>
            ) : (
              <p className="text-xs text-[#667085]">No specific evidence references cited.</p>
            )}
          </div>
        </div>

        {/* Limitations and caveats */}
        {aiAssessment?.limitationsAndCaveats && (
          <div className="p-4 rounded-lg bg-[#F5F6F8] border border-[#E4E7EC] text-xs space-y-1">
            <span className="font-semibold text-[#202938] block">Limitations and caveats</span>
            <p className="text-[#667085] leading-relaxed">
              {aiAssessment.limitationsAndCaveats}
            </p>
          </div>
        )}

        {/* Expandable Technical Model Details */}
        <div className="pt-3 border-t border-[#E4E7EC]">
          <button
            onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
            className="flex items-center justify-between w-full text-xs font-medium text-[#667085] hover:text-[#202938] cursor-pointer"
          >
            <span>Technical assessment details</span>
            {showTechnicalDetails ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
          </button>

          {showTechnicalDetails && aiAssessment && (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-3 p-4 bg-[#F5F6F8] rounded-lg border border-[#E4E7EC] text-xs">
              <div>
                <span className="text-[#667085] block text-[11px]">Provider / Model:</span>
                <span className="font-medium text-[#202938] mt-0.5 block truncate">
                  {aiAssessment.provider} {aiAssessment.modelName ? `(${aiAssessment.modelName})` : ''}
                </span>
              </div>
              <div>
                <span className="text-[#667085] block text-[11px]">Tokens used:</span>
                <span className="font-medium text-[#202938] mt-0.5 block">
                  {aiAssessment.tokensUsed != null ? aiAssessment.tokensUsed : 0}
                </span>
              </div>
              <div>
                <span className="text-[#667085] block text-[11px]">Generation latency:</span>
                <span className="font-medium text-[#202938] mt-0.5 block">
                  {aiAssessment.generationTimeMs != null ? `${aiAssessment.generationTimeMs} ms` : '—'}
                </span>
              </div>
              <div>
                <span className="text-[#667085] block text-[11px]">Prompt version:</span>
                <span className="font-medium text-[#202938] mt-0.5 block">
                  {aiAssessment.promptVersion || '1.0'}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

