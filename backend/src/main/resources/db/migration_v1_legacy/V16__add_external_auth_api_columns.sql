-- Migration V16: Add external email header authentication API audit columns

ALTER TABLE email_analysis_results
ADD COLUMN IF NOT EXISTS auth_api_provider VARCHAR(50),
ADD COLUMN IF NOT EXISTS auth_api_status VARCHAR(50),
ADD COLUMN IF NOT EXISTS auth_api_response_json TEXT,
ADD COLUMN IF NOT EXISTS auth_api_error_details TEXT,
ADD COLUMN IF NOT EXISTS auth_api_updated_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_email_analysis_results_auth_api_provider 
ON email_analysis_results(auth_api_provider);
