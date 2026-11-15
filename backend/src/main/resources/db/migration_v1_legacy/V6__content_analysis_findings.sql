CREATE TABLE content_analysis (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE RESTRICT UNIQUE,
    ml_model_version VARCHAR(80),
    probability DOUBLE PRECISION,
    predicted_label VARCHAR(80),
    ml_status VARCHAR(50),
    urgency BOOLEAN NOT NULL DEFAULT FALSE,
    credential_request BOOLEAN NOT NULL DEFAULT FALSE,
    payment_instruction BOOLEAN NOT NULL DEFAULT FALSE,
    impersonation BOOLEAN NOT NULL DEFAULT FALSE,
    secrecy BOOLEAN NOT NULL DEFAULT FALSE,
    prompt_injection_detected BOOLEAN NOT NULL DEFAULT FALSE,
    prompt_matches JSONB,
    social_engineering_matches JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_content_analysis_run ON content_analysis(analysis_run_id);
