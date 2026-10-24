CREATE TABLE extracted_urls (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    raw_url TEXT NOT NULL,
    normalized_url TEXT NOT NULL,
    domain VARCHAR(255),
    source_type VARCHAR(50) NOT NULL,
    anchor_text TEXT,
    anchor_mismatch BOOLEAN NOT NULL DEFAULT FALSE,
    is_ip_url BOOLEAN NOT NULL DEFAULT FALSE,
    is_shortened BOOLEAN NOT NULL DEFAULT FALSE,
    unusual_port BOOLEAN NOT NULL DEFAULT FALSE,
    subdomain_excess BOOLEAN NOT NULL DEFAULT FALSE,
    lookalike BOOLEAN NOT NULL DEFAULT FALSE,
    dns_status VARCHAR(50),
    url_risk_score INTEGER CHECK (url_risk_score >= 0 AND url_risk_score <= 100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_extracted_urls_run ON extracted_urls(analysis_run_id);
CREATE INDEX idx_extracted_urls_normalized ON extracted_urls(normalized_url);
CREATE INDEX idx_extracted_urls_domain ON extracted_urls(domain);

CREATE TABLE ip_artifacts (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    ip_address VARCHAR(50) NOT NULL,
    source_context VARCHAR(100) NOT NULL,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    lookup_status VARCHAR(50) NOT NULL,
    asn_number VARCHAR(50),
    organization VARCHAR(255),
    country VARCHAR(100),
    city VARCHAR(100),
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    data_source VARCHAR(100),
    observed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ip_artifacts_run ON ip_artifacts(analysis_run_id);
CREATE INDEX idx_ip_artifacts_address ON ip_artifacts(ip_address);
