CREATE TABLE ai_grounded_assessments (
    id UUID PRIMARY KEY,
    email_evidence_id UUID NOT NULL REFERENCES email_evidence(id) ON DELETE RESTRICT,
    analysis_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    threat_classification VARCHAR(50) NOT NULL,
    confidence_assessment VARCHAR(20) NOT NULL,
    summary_explanation TEXT NOT NULL,
    key_indicators_json JSONB,
    evidence_references_json JSONB,
    unsupported_references_json JSONB,
    limitations_and_caveats TEXT NOT NULL,
    untrusted_injections_detected BOOLEAN NOT NULL DEFAULT FALSE,
    sanitized_context_summary TEXT,
    error_message TEXT,
    tokens_used INTEGER DEFAULT 0,
    generation_time_ms BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_assessments_email ON ai_grounded_assessments(email_evidence_id);
CREATE INDEX idx_ai_assessments_run ON ai_grounded_assessments(analysis_run_id);
CREATE INDEX idx_ai_assessments_created ON ai_grounded_assessments(created_at DESC);
