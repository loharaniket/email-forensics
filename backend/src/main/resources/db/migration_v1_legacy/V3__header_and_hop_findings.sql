CREATE TABLE header_analysis (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL UNIQUE REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    from_address VARCHAR(254),
    from_domain VARCHAR(255),
    reply_to VARCHAR(254),
    return_path VARCHAR(254),
    dkim_domain VARCHAR(255),
    reported_spf VARCHAR(40),
    reported_dkim VARCHAR(40),
    reported_dmarc VARCHAR(40),
    reported_arc VARCHAR(40),
    current_dns_observation TEXT,
    independent_verification TEXT,
    identity_mismatch_flags TEXT,
    raw_headers_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_header_analysis_run ON header_analysis(analysis_run_id);

CREATE TABLE received_hops (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    hop_order INTEGER NOT NULL,
    from_host VARCHAR(255),
    by_host VARCHAR(255),
    protocol VARCHAR(50),
    hop_ip VARCHAR(50),
    hop_timestamp TIMESTAMPTZ,
    delay_seconds BIGINT,
    raw_hop_line TEXT NOT NULL,
    parsing_warning VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_received_hops_run_order ON received_hops(analysis_run_id, hop_order);
