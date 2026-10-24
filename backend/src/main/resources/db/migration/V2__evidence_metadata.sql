CREATE TABLE email_evidence (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES fore_cases(id) ON DELETE RESTRICT,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes >= 0),
    sha256_hash CHAR(64) NOT NULL,
    subject TEXT,
    message_id VARCHAR(998),
    sent_date TIMESTAMPTZ,
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evidence_case_uploaded ON email_evidence(case_id, uploaded_at DESC);
CREATE INDEX idx_evidence_sha256 ON email_evidence(sha256_hash);

CREATE TABLE analysis_runs (
    id UUID PRIMARY KEY,
    email_id UUID NOT NULL REFERENCES email_evidence(id) ON DELETE RESTRICT,
    pipeline_version VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'PARTIAL', 'FAILED')),
    current_stage VARCHAR(80),
    attempt_number INTEGER NOT NULL DEFAULT 1 CHECK (attempt_number > 0),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_code VARCHAR(80),
    safe_error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analysis_run_email_created ON analysis_runs(email_id, created_at DESC);
