import React from 'react'
import {
  Sparkles,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  Tag,
  ShieldCheck
} from 'lucide-react'

export default function AutomatedAssessment({
  aiAssessment,
  isAiGenerating,
  onRegenerate
}) {
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
                <span className="text-xs px-2.5 py-0.5 rounded bg-slate-100 text-[#202938] border border-[#E4E7EC]">
                  {aiAssessment.threatClassification || 'Assessed'}
                </span>
              )}
            </div>

            <h2 className="text-lg font-bold text-[#202938]">
              {aiAssessment ? aiAssessment.threatClassification : 'Generating assessment...'}
            </h2>
            <p className="text-xs text-[#202938]">
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
              <span className="font-semibold block">Automated assessment completed</span>
              <p className="text-[11px] text-[#202938] mt-0.5">
                All findings were cross-referenced with extracted evidence.
              </p>
            </div>
          </div>
        )}

        {isFailed && (
          <div className="p-3.5 rounded-lg bg-[#FFFBEB] border border-amber-200 text-xs text-[#B42318] space-y-1">
            <div className="flex items-center gap-2 font-semibold">
              <AlertTriangle className="h-4 w-4 shrink-0 text-[#B45309]" />
              <span>Assessment service unavailable — rule-based fallback used</span>
            </div>
            <p className="text-[11px] text-[#202938]">
              A deterministic rule-based assessment was used instead.
            </p>
          </div>
        )}

        {/* Assessment summary narrative */}
        <div className="space-y-2 pt-1">
          <h3 className="text-xs font-bold uppercase tracking-wider text-[#202938]">
            Assessment summary
          </h3>
          <p className="text-sm text-[#202938] leading-relaxed bg-[#F5F6F8] p-4 rounded-lg border border-[#E4E7EC]">
            {aiAssessment?.summaryExplanation || 'No assessment narrative available.'}
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
              <p className="text-xs text-[#202938]">No specific key indicators listed.</p>
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
              <p className="text-xs text-[#202938]">No specific evidence references cited.</p>
            )}
          </div>
        </div>



      </div>
    </div>
  )
}

