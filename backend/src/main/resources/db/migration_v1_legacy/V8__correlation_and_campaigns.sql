CREATE TABLE historical_matches (
    id UUID PRIMARY KEY,
    source_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    matched_email_id UUID NOT NULL REFERENCES email_evidence(id) ON DELETE RESTRICT,
    indicator_type VARCHAR(50) NOT NULL,
    normalized_indicator_value TEXT NOT NULL,
    match_strength VARCHAR(20) NOT NULL,
    match_explanation TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_historical_matches UNIQUE (source_run_id, matched_email_id, indicator_type, normalized_indicator_value)
);

CREATE INDEX idx_historical_matches_source ON historical_matches(source_run_id);
CREATE INDEX idx_historical_matches_matched ON historical_matches(matched_email_id);
CREATE INDEX idx_historical_matches_type_val ON historical_matches(indicator_type, normalized_indicator_value);

CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    campaign_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    confidence_score INTEGER NOT NULL CHECK (confidence_score >= 0 AND confidence_score <= 100),
    summary TEXT NOT NULL,
    shared_indicators_json JSONB,
    first_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaigns_code ON campaigns(campaign_code);
CREATE INDEX idx_campaigns_created ON campaigns(created_at DESC);

CREATE TABLE campaign_members (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    email_id UUID NOT NULL REFERENCES email_evidence(id) ON DELETE RESTRICT,
    membership_reason TEXT NOT NULL,
    match_score INTEGER NOT NULL CHECK (match_score >= 0 AND match_score <= 100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_campaign_members UNIQUE (campaign_id, email_id)
);

CREATE INDEX idx_campaign_members_camp ON campaign_members(campaign_id);
CREATE INDEX idx_campaign_members_email ON campaign_members(email_id);
