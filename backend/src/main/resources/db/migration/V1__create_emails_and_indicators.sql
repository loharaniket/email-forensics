CREATE TABLE IF NOT EXISTS emails (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    sender TEXT,
    reply_to TEXT,
    subject TEXT,
    sent_at TIMESTAMPTZ,
    auth_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    ai_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
    stage VARCHAR(32),
    risk_score SMALLINT,
    risk_level VARCHAR(20),
    verdict VARCHAR(30),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_email_risk_score
        CHECK (risk_score IS NULL OR risk_score BETWEEN 0 AND 100),
    CONSTRAINT chk_email_status
        CHECK (status IN (
            'RECEIVED', 'PROCESSING', 'WAITING_FOR_AI_APPROVAL',
            'COMPLETED', 'PARTIAL', 'FAILED', 'FAILED_RETRYABLE'
        )),
    CONSTRAINT chk_email_risk_level
        CHECK (risk_level IS NULL OR risk_level IN (
            'LOW', 'GUARDED', 'ELEVATED', 'HIGH', 'CRITICAL'
        )),
    CONSTRAINT chk_email_verdict
        CHECK (verdict IS NULL OR verdict IN (
            'PHISHING', 'SUSPICIOUS', 'NO_KNOWN_THREAT', 'INSUFFICIENT_DATA'
        ))
);

CREATE TABLE IF NOT EXISTS indicators (
    id BIGSERIAL PRIMARY KEY,
    email_id BIGINT NOT NULL REFERENCES emails(id) ON DELETE RESTRICT,
    type VARCHAR(24) NOT NULL,
    value TEXT NOT NULL,
    normalized_value TEXT,
    origin VARCHAR(24) NOT NULL,
    verdict VARCHAR(24) NOT NULL DEFAULT 'UNKNOWN',
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    checked_at TIMESTAMPTZ,
    CONSTRAINT chk_indicator_type
        CHECK (type IN ('URL', 'DOMAIN', 'IP', 'QR_URL', 'OCR_TEXT')),
    CONSTRAINT chk_indicator_origin
        CHECK (origin IN ('HEADER', 'BODY', 'HTML', 'DNS', 'IMAGE_OCR', 'QR')),
    CONSTRAINT chk_indicator_verdict
        CHECK (verdict IN (
            'MALICIOUS', 'SUSPICIOUS', 'NO_MATCH', 'UNKNOWN', 'NOT_CHECKED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_emails_created_at ON emails(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_emails_sha256 ON emails(sha256);
CREATE INDEX IF NOT EXISTS idx_emails_status ON emails(status);
CREATE INDEX IF NOT EXISTS idx_indicators_email_id ON indicators(email_id);
CREATE INDEX IF NOT EXISTS idx_indicators_type_value ON indicators(type, normalized_value);
CREATE INDEX IF NOT EXISTS idx_indicators_normalized_value ON indicators(normalized_value);
CREATE INDEX IF NOT EXISTS idx_indicators_details_gin ON indicators USING GIN(details);
