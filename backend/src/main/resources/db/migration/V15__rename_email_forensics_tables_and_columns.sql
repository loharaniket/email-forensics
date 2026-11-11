-- Migration V15: Rename email forensics tables and columns to standard convention and remove users/cases

-- 1. Remove Foreign Keys referencing fore_cases and users from email_evidence
ALTER TABLE email_evidence DROP CONSTRAINT IF EXISTS email_evidence_case_id_fkey;
ALTER TABLE email_evidence DROP CONSTRAINT IF EXISTS email_evidence_uploaded_by_fkey;

-- 2. Drop obsolete indexes on email_evidence
DROP INDEX IF EXISTS idx_evidence_case_uploaded;

-- 3. Remove case_id and uploaded_by columns from email_evidence
ALTER TABLE email_evidence DROP COLUMN IF EXISTS case_id;
ALTER TABLE email_evidence DROP COLUMN IF EXISTS uploaded_by;

-- 4. Safely drop obsolete tables
DROP TABLE IF EXISTS fore_cases CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 5. Rename tables to final approved naming convention
ALTER TABLE email_evidence RENAME TO emails;
ALTER TABLE analysis_runs RENAME TO email_analysis_runs;
ALTER TABLE analysis_results RENAME TO email_analysis_results;
ALTER TABLE received_hops RENAME TO email_hops;
ALTER TABLE extracted_urls RENAME TO email_urls;
ALTER TABLE ip_artifacts RENAME TO email_ip_addresses;
ALTER TABLE visual_artifacts RENAME TO email_files;
ALTER TABLE qr_artifacts RENAME TO qr_codes;
ALTER TABLE risk_factors RENAME TO risk_reasons;
ALTER TABLE historical_matches RENAME TO email_matches;
ALTER TABLE ai_grounded_assessments RENAME TO ai_assessments;

-- 6. Rename columns in Table 1: emails
ALTER TABLE emails RENAME COLUMN original_filename TO original_file_name;
ALTER TABLE emails RENAME COLUMN sent_date TO sent_at;

-- 7. Rename columns in Table 2: email_analysis_runs
ALTER TABLE email_analysis_runs RENAME COLUMN pipeline_version TO analysis_version;
ALTER TABLE email_analysis_runs RENAME COLUMN current_stage TO current_step;
ALTER TABLE email_analysis_runs RENAME COLUMN safe_error_message TO error_message;

-- 8. Rename columns in Table 3: email_analysis_results
ALTER TABLE email_analysis_results RENAME COLUMN from_address TO sender_email;
ALTER TABLE email_analysis_results RENAME COLUMN from_domain TO sender_domain;
ALTER TABLE email_analysis_results RENAME COLUMN reported_spf TO spf_result;
ALTER TABLE email_analysis_results RENAME COLUMN reported_dkim TO dkim_result;
ALTER TABLE email_analysis_results RENAME COLUMN reported_dmarc TO dmarc_result;
ALTER TABLE email_analysis_results RENAME COLUMN reported_arc TO arc_result;
ALTER TABLE email_analysis_results RENAME COLUMN current_dns_observation TO dns_observation;
ALTER TABLE email_analysis_results RENAME COLUMN independent_verification TO sender_verification;
ALTER TABLE email_analysis_results RENAME COLUMN identity_mismatch_flags TO sender_mismatch_flags;
ALTER TABLE email_analysis_results RENAME COLUMN probability TO ml_probability;
ALTER TABLE email_analysis_results RENAME COLUMN predicted_label TO ml_prediction;
ALTER TABLE email_analysis_results RENAME COLUMN urgency TO has_urgent_language;
ALTER TABLE email_analysis_results RENAME COLUMN credential_request TO asks_for_credentials;
ALTER TABLE email_analysis_results RENAME COLUMN payment_instruction TO has_payment_request;
ALTER TABLE email_analysis_results RENAME COLUMN impersonation TO detects_impersonation;
ALTER TABLE email_analysis_results RENAME COLUMN secrecy TO asks_for_secrecy;
ALTER TABLE email_analysis_results RENAME COLUMN prompt_injection_detected TO has_prompt_injection;
ALTER TABLE email_analysis_results RENAME COLUMN prompt_matches TO prompt_matches_json;
ALTER TABLE email_analysis_results RENAME COLUMN deterministic_risk_score TO risk_score;
ALTER TABLE email_analysis_results RENAME COLUMN category_scores_json TO risk_scores_by_category_json;
ALTER TABLE email_analysis_results RENAME COLUMN total_uncapped_score TO raw_risk_score;
ALTER TABLE email_analysis_results RENAME COLUMN engine_version TO risk_engine_version;
ALTER TABLE email_analysis_results RENAME COLUMN evaluated_at TO risk_checked_at;

-- 9. Rename columns in Table 4: email_hops
ALTER TABLE email_hops RENAME COLUMN hop_order TO hop_number;
ALTER TABLE email_hops RENAME COLUMN from_host TO from_server;
ALTER TABLE email_hops RENAME COLUMN by_host TO received_by_server;
ALTER TABLE email_hops RENAME COLUMN protocol TO mail_protocol;
ALTER TABLE email_hops RENAME COLUMN hop_ip TO server_ip;
ALTER TABLE email_hops RENAME COLUMN hop_timestamp TO received_at;
ALTER TABLE email_hops RENAME COLUMN raw_hop_line TO original_received_header;
ALTER TABLE email_hops RENAME COLUMN parsing_warning TO parse_warning;

-- 10. Rename columns in Table 5: email_urls
ALTER TABLE email_urls RENAME COLUMN raw_url TO original_url;
ALTER TABLE email_urls RENAME COLUMN normalized_url TO clean_url;
ALTER TABLE email_urls RENAME COLUMN source_type TO found_in;
ALTER TABLE email_urls RENAME COLUMN anchor_text TO link_text;
ALTER TABLE email_urls RENAME COLUMN anchor_mismatch TO link_text_mismatch;
ALTER TABLE email_urls RENAME COLUMN is_ip_url TO uses_ip_address;
ALTER TABLE email_urls RENAME COLUMN is_shortened TO is_short_url;
ALTER TABLE email_urls RENAME COLUMN unusual_port TO has_unusual_port;
ALTER TABLE email_urls RENAME COLUMN subdomain_excess TO has_many_subdomains;
ALTER TABLE email_urls RENAME COLUMN lookalike TO is_lookalike_domain;
ALTER TABLE email_urls RENAME COLUMN url_risk_score TO risk_score;
ALTER TABLE email_urls RENAME COLUMN data_mode TO data_status;

-- 11. Rename columns in Table 6: email_ip_addresses
ALTER TABLE email_ip_addresses RENAME COLUMN source_context TO found_in;
ALTER TABLE email_ip_addresses RENAME COLUMN is_private TO is_private_ip;
ALTER TABLE email_ip_addresses RENAME COLUMN data_mode TO data_status;

-- 12. Rename columns in Table 7: email_files
ALTER TABLE email_files RENAME COLUMN filename TO file_name;
ALTER TABLE email_files RENAME COLUMN content_type TO file_type;
ALTER TABLE email_files RENAME COLUMN data_mode TO data_status;

-- 13. Rename columns in Table 8: qr_codes
ALTER TABLE qr_codes RENAME COLUMN visual_artifact_id TO email_file_id;
ALTER TABLE qr_codes RENAME COLUMN decoded_payload TO qr_content;
ALTER TABLE qr_codes RENAME COLUMN payload_type TO qr_content_type;
ALTER TABLE qr_codes RENAME COLUMN normalized_url TO clean_url;

-- 14. Rename columns in Table 9: risk_reasons
ALTER TABLE risk_reasons RENAME COLUMN factor_code TO reason_code;
ALTER TABLE risk_reasons RENAME COLUMN weight_points TO risk_points;
ALTER TABLE risk_reasons RENAME COLUMN explanation TO reason;

-- 15. Rename columns in Table 10: email_matches
ALTER TABLE email_matches RENAME COLUMN source_run_id TO source_analysis_run_id;
ALTER TABLE email_matches RENAME COLUMN indicator_type TO match_type;
ALTER TABLE email_matches RENAME COLUMN normalized_indicator_value TO match_value;
ALTER TABLE email_matches RENAME COLUMN match_explanation TO explanation;

-- 16. Rename columns in Table 11: ai_assessments
ALTER TABLE ai_assessments RENAME COLUMN email_evidence_id TO email_id;
ALTER TABLE ai_assessments RENAME COLUMN provider TO ai_provider;
ALTER TABLE ai_assessments RENAME COLUMN threat_classification TO threat_type;
ALTER TABLE ai_assessments RENAME COLUMN confidence_assessment TO confidence_level;
ALTER TABLE ai_assessments RENAME COLUMN summary_explanation TO summary;
ALTER TABLE ai_assessments RENAME COLUMN limitations_and_caveats TO limitations;
ALTER TABLE ai_assessments RENAME COLUMN untrusted_injections_detected TO has_untrusted_injection;
ALTER TABLE ai_assessments RENAME COLUMN sanitized_context_summary TO safe_context_summary;

-- 17. Update unique constraint on email_matches
ALTER TABLE email_matches DROP CONSTRAINT IF EXISTS uq_historical_matches;
ALTER TABLE email_matches ADD CONSTRAINT uq_email_matches UNIQUE (source_analysis_run_id, matched_email_id, match_type, match_value);

-- 18. Recreate / maintain indexes with clear names
CREATE INDEX IF NOT EXISTS idx_emails_uploaded_at ON emails(uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_emails_sha256 ON emails(sha256_hash);
CREATE INDEX IF NOT EXISTS idx_email_analysis_runs_email ON email_analysis_runs(email_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_email_analysis_results_sender_domain ON email_analysis_results(sender_domain);
CREATE INDEX IF NOT EXISTS idx_email_hops_run_order ON email_hops(analysis_run_id, hop_number);
CREATE INDEX IF NOT EXISTS idx_email_urls_run ON email_urls(analysis_run_id);
CREATE INDEX IF NOT EXISTS idx_email_urls_clean ON email_urls(clean_url);
CREATE INDEX IF NOT EXISTS idx_email_urls_domain ON email_urls(domain);
CREATE INDEX IF NOT EXISTS idx_email_ip_addresses_run ON email_ip_addresses(analysis_run_id);
CREATE INDEX IF NOT EXISTS idx_email_ip_addresses_ip ON email_ip_addresses(ip_address);
CREATE INDEX IF NOT EXISTS idx_email_files_run ON email_files(analysis_run_id);
CREATE INDEX IF NOT EXISTS idx_email_files_sha256 ON email_files(sha256_hash);
CREATE INDEX IF NOT EXISTS idx_qr_codes_file ON qr_codes(email_file_id);
CREATE INDEX IF NOT EXISTS idx_risk_reasons_run ON risk_reasons(analysis_run_id);
CREATE INDEX IF NOT EXISTS idx_risk_reasons_code ON risk_reasons(reason_code);
CREATE INDEX IF NOT EXISTS idx_email_matches_source ON email_matches(source_analysis_run_id);
CREATE INDEX IF NOT EXISTS idx_email_matches_matched ON email_matches(matched_email_id);
CREATE INDEX IF NOT EXISTS idx_email_matches_type_val ON email_matches(match_type, match_value);
CREATE INDEX IF NOT EXISTS idx_ai_assessments_email ON ai_assessments(email_id);
CREATE INDEX IF NOT EXISTS idx_ai_assessments_run ON ai_assessments(analysis_run_id);
CREATE INDEX IF NOT EXISTS idx_ai_assessments_created ON ai_assessments(created_at DESC);

