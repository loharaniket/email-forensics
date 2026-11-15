CREATE TABLE analysis_results (
    analysis_run_id UUID PRIMARY KEY REFERENCES analysis_runs(id) ON DELETE RESTRICT,

    -- Header result fields
    header_result_id UUID,
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
    header_created_at TIMESTAMPTZ,

    -- Content analysis fields
    content_result_id UUID,
    ml_model_version VARCHAR(80),
    probability DOUBLE PRECISION,
    predicted_label VARCHAR(80),
    ml_status VARCHAR(50),
    urgency BOOLEAN,
    credential_request BOOLEAN,
    payment_instruction BOOLEAN,
    impersonation BOOLEAN,
    secrecy BOOLEAN,
    prompt_injection_detected BOOLEAN,
    prompt_matches JSONB,
    social_engineering_matches JSONB,
    content_created_at TIMESTAMPTZ,

    -- Risk assessment fields
    risk_result_id UUID,
    deterministic_risk_score INTEGER CHECK (deterministic_risk_score IS NULL OR (deterministic_risk_score >= 0 AND deterministic_risk_score <= 100)),
    risk_level VARCHAR(20),
    evidence_confidence_score INTEGER CHECK (evidence_confidence_score IS NULL OR (evidence_confidence_score >= 0 AND evidence_confidence_score <= 100)),
    confidence_level VARCHAR(20),
    confidence_explanation TEXT,
    category_scores_json JSONB,
    total_uncapped_score INTEGER,
    engine_version VARCHAR(50),
    evaluated_at TIMESTAMPTZ,
    risk_created_at TIMESTAMPTZ
);

CREATE INDEX idx_analysis_results_from_domain ON analysis_results(from_domain);

INSERT INTO analysis_results (
    analysis_run_id,
    header_result_id,
    from_address,
    from_domain,
    reply_to,
    return_path,
    dkim_domain,
    reported_spf,
    reported_dkim,
    reported_dmarc,
    reported_arc,
    current_dns_observation,
    independent_verification,
    identity_mismatch_flags,
    raw_headers_json,
    header_created_at,
    content_result_id,
    ml_model_version,
    probability,
    predicted_label,
    ml_status,
    urgency,
    credential_request,
    payment_instruction,
    impersonation,
    secrecy,
    prompt_injection_detected,
    prompt_matches,
    social_engineering_matches,
    content_created_at,
    risk_result_id,
    deterministic_risk_score,
    risk_level,
    evidence_confidence_score,
    confidence_level,
    confidence_explanation,
    category_scores_json,
    total_uncapped_score,
    engine_version,
    evaluated_at,
    risk_created_at
)
SELECT
    runs.analysis_run_id,
    h.id AS header_result_id,
    h.from_address,
    h.from_domain,
    h.reply_to,
    h.return_path,
    h.dkim_domain,
    h.reported_spf,
    h.reported_dkim,
    h.reported_dmarc,
    h.reported_arc,
    h.current_dns_observation,
    h.independent_verification,
    h.identity_mismatch_flags,
    h.raw_headers_json,
    h.created_at AS header_created_at,
    c.id AS content_result_id,
    c.ml_model_version,
    c.probability,
    c.predicted_label,
    c.ml_status,
    c.urgency,
    c.credential_request,
    c.payment_instruction,
    c.impersonation,
    c.secrecy,
    c.prompt_injection_detected,
    c.prompt_matches,
    c.social_engineering_matches,
    c.created_at AS content_created_at,
    r.id AS risk_result_id,
    r.deterministic_risk_score,
    r.risk_level,
    r.evidence_confidence_score,
    r.confidence_level,
    r.confidence_explanation,
    r.category_scores_json,
    r.total_uncapped_score,
    r.engine_version,
    r.evaluated_at,
    r.created_at AS risk_created_at
FROM (
    SELECT analysis_run_id FROM header_analysis
    UNION
    SELECT analysis_run_id FROM content_analysis
    UNION
    SELECT analysis_run_id FROM risk_assessments
) runs
LEFT JOIN header_analysis h ON h.analysis_run_id = runs.analysis_run_id
LEFT JOIN content_analysis c ON c.analysis_run_id = runs.analysis_run_id
LEFT JOIN risk_assessments r ON r.analysis_run_id = runs.analysis_run_id;

DO $$
DECLARE
    v_old_header_count INTEGER;
    v_old_content_count INTEGER;
    v_old_risk_count INTEGER;
    v_new_header_count INTEGER;
    v_new_content_count INTEGER;
    v_new_risk_count INTEGER;
BEGIN
    SELECT count(*) INTO v_old_header_count FROM header_analysis;
    SELECT count(*) INTO v_old_content_count FROM content_analysis;
    SELECT count(*) INTO v_old_risk_count FROM risk_assessments;

    SELECT count(*) INTO v_new_header_count FROM analysis_results WHERE header_result_id IS NOT NULL;
    SELECT count(*) INTO v_new_content_count FROM analysis_results WHERE content_result_id IS NOT NULL;
    SELECT count(*) INTO v_new_risk_count FROM analysis_results WHERE risk_result_id IS NOT NULL;

    IF v_old_header_count <> v_new_header_count THEN
        RAISE EXCEPTION 'Header analysis record count mismatch: expected %, got %', v_old_header_count, v_new_header_count;
    END IF;

    IF v_old_content_count <> v_new_content_count THEN
        RAISE EXCEPTION 'Content analysis record count mismatch: expected %, got %', v_old_content_count, v_new_content_count;
    END IF;

    IF v_old_risk_count <> v_new_risk_count THEN
        RAISE EXCEPTION 'Risk assessment record count mismatch: expected %, got %', v_old_risk_count, v_new_risk_count;
    END IF;
END $$;

DROP TABLE IF EXISTS risk_assessments;
DROP TABLE IF EXISTS content_analysis;
DROP TABLE IF EXISTS header_analysis;

