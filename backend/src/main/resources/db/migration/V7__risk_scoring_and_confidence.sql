CREATE TABLE risk_factors (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    category VARCHAR(50) NOT NULL,
    factor_code VARCHAR(80) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    weight_points INTEGER NOT NULL CHECK (weight_points >= 0),
    explanation TEXT NOT NULL,
    evidence_reference VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_risk_factors_run ON risk_factors(analysis_run_id);
CREATE INDEX idx_risk_factors_code ON risk_factors(factor_code);

CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL UNIQUE REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    deterministic_risk_score INTEGER NOT NULL CHECK (deterministic_risk_score >= 0 AND deterministic_risk_score <= 100),
    risk_level VARCHAR(20) NOT NULL,
    evidence_confidence_score INTEGER NOT NULL CHECK (evidence_confidence_score >= 0 AND evidence_confidence_score <= 100),
    confidence_level VARCHAR(20) NOT NULL,
    confidence_explanation TEXT NOT NULL,
    category_scores_json JSONB,
    total_uncapped_score INTEGER NOT NULL DEFAULT 0,
    engine_version VARCHAR(50) NOT NULL DEFAULT 'v1.0.0',
    evaluated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_risk_assessments_run ON risk_assessments(analysis_run_id);
