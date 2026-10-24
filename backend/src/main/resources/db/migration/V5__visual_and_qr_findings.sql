CREATE TABLE visual_artifacts (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE RESTRICT,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes >= 0),
    sha256_hash CHAR(64) NOT NULL,
    ocr_text TEXT,
    ocr_status VARCHAR(50) NOT NULL,
    processing_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visual_artifacts_run ON visual_artifacts(analysis_run_id);
CREATE INDEX idx_visual_artifacts_sha256 ON visual_artifacts(sha256_hash);

CREATE TABLE qr_artifacts (
    id UUID PRIMARY KEY,
    visual_artifact_id UUID NOT NULL REFERENCES visual_artifacts(id) ON DELETE RESTRICT,
    decoded_payload TEXT NOT NULL,
    payload_type VARCHAR(50) NOT NULL,
    normalized_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qr_artifacts_visual ON qr_artifacts(visual_artifact_id);
