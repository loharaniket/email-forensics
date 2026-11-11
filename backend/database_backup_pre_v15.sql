--
-- PostgreSQL database dump
--

\restrict FsJH7NayScUsTQKAyZFU9Z6zrgpCjFEv8Herh2SQR6u9hTwSPAjPLcl7gbQTSRm

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ai_grounded_assessments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ai_grounded_assessments (
    id uuid NOT NULL,
    email_evidence_id uuid NOT NULL,
    analysis_run_id uuid NOT NULL,
    provider character varying(50) NOT NULL,
    model_name character varying(100) NOT NULL,
    prompt_version character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    threat_classification character varying(50) NOT NULL,
    confidence_assessment character varying(20) NOT NULL,
    summary_explanation text NOT NULL,
    key_indicators_json jsonb,
    evidence_references_json jsonb,
    unsupported_references_json jsonb,
    limitations_and_caveats text NOT NULL,
    untrusted_injections_detected boolean DEFAULT false NOT NULL,
    sanitized_context_summary text,
    error_message text,
    tokens_used integer DEFAULT 0,
    generation_time_ms bigint DEFAULT 0,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.ai_grounded_assessments OWNER TO postgres;

--
-- Name: analysis_results; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis_results (
    analysis_run_id uuid NOT NULL,
    header_result_id uuid,
    from_address character varying(254),
    from_domain character varying(255),
    reply_to character varying(254),
    return_path character varying(254),
    dkim_domain character varying(255),
    reported_spf character varying(40),
    reported_dkim character varying(40),
    reported_dmarc character varying(40),
    reported_arc character varying(40),
    current_dns_observation text,
    independent_verification text,
    identity_mismatch_flags text,
    raw_headers_json text,
    header_created_at timestamp with time zone,
    content_result_id uuid,
    ml_model_version character varying(80),
    probability double precision,
    predicted_label character varying(80),
    ml_status character varying(50),
    urgency boolean,
    credential_request boolean,
    payment_instruction boolean,
    impersonation boolean,
    secrecy boolean,
    prompt_injection_detected boolean,
    prompt_matches jsonb,
    social_engineering_matches jsonb,
    content_created_at timestamp with time zone,
    risk_result_id uuid,
    deterministic_risk_score integer,
    risk_level character varying(20),
    evidence_confidence_score integer,
    confidence_level character varying(20),
    confidence_explanation text,
    category_scores_json jsonb,
    total_uncapped_score integer,
    engine_version character varying(50),
    evaluated_at timestamp with time zone,
    risk_created_at timestamp with time zone,
    CONSTRAINT analysis_results_deterministic_risk_score_check CHECK (((deterministic_risk_score IS NULL) OR ((deterministic_risk_score >= 0) AND (deterministic_risk_score <= 100)))),
    CONSTRAINT analysis_results_evidence_confidence_score_check CHECK (((evidence_confidence_score IS NULL) OR ((evidence_confidence_score >= 0) AND (evidence_confidence_score <= 100))))
);


ALTER TABLE public.analysis_results OWNER TO postgres;

--
-- Name: analysis_runs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis_runs (
    id uuid NOT NULL,
    email_id uuid NOT NULL,
    pipeline_version character varying(40) NOT NULL,
    status character varying(20) NOT NULL,
    current_stage character varying(80),
    attempt_number integer DEFAULT 1 NOT NULL,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    error_code character varying(80),
    safe_error_message text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT analysis_runs_attempt_number_check CHECK ((attempt_number > 0)),
    CONSTRAINT analysis_runs_status_check CHECK (((status)::text = ANY ((ARRAY['QUEUED'::character varying, 'RUNNING'::character varying, 'COMPLETED'::character varying, 'PARTIAL'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.analysis_runs OWNER TO postgres;

--
-- Name: email_evidence; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.email_evidence (
    id uuid NOT NULL,
    case_id uuid NOT NULL,
    original_filename character varying(255) NOT NULL,
    storage_key character varying(500) NOT NULL,
    file_size_bytes bigint NOT NULL,
    sha256_hash character(64) NOT NULL,
    subject text,
    message_id character varying(998),
    sent_date timestamp with time zone,
    uploaded_by uuid NOT NULL,
    uploaded_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT email_evidence_file_size_bytes_check CHECK ((file_size_bytes >= 0))
);


ALTER TABLE public.email_evidence OWNER TO postgres;

--
-- Name: extracted_urls; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.extracted_urls (
    id uuid NOT NULL,
    analysis_run_id uuid NOT NULL,
    raw_url text NOT NULL,
    normalized_url text NOT NULL,
    domain character varying(255),
    source_type character varying(50) NOT NULL,
    anchor_text text,
    anchor_mismatch boolean DEFAULT false NOT NULL,
    is_ip_url boolean DEFAULT false NOT NULL,
    is_shortened boolean DEFAULT false NOT NULL,
    unusual_port boolean DEFAULT false NOT NULL,
    subdomain_excess boolean DEFAULT false NOT NULL,
    lookalike boolean DEFAULT false NOT NULL,
    dns_status character varying(50),
    url_risk_score integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    data_mode character varying(50) DEFAULT 'NOT_CHECKED'::character varying NOT NULL,
    CONSTRAINT extracted_urls_url_risk_score_check CHECK (((url_risk_score >= 0) AND (url_risk_score <= 100)))
);


ALTER TABLE public.extracted_urls OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: fore_cases; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fore_cases (
    id uuid NOT NULL,
    case_number character varying(40) NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    analyst_notes text,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT fore_cases_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'IN_REVIEW'::character varying, 'CLOSED'::character varying])::text[])))
);


ALTER TABLE public.fore_cases OWNER TO postgres;

--
-- Name: historical_matches; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.historical_matches (
    id uuid NOT NULL,
    source_run_id uuid NOT NULL,
    matched_email_id uuid NOT NULL,
    indicator_type character varying(50) NOT NULL,
    normalized_indicator_value text NOT NULL,
    match_strength character varying(20) NOT NULL,
    match_explanation text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.historical_matches OWNER TO postgres;

--
-- Name: ip_artifacts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ip_artifacts (
    id uuid NOT NULL,
    analysis_run_id uuid NOT NULL,
    ip_address character varying(50) NOT NULL,
    source_context character varying(100) NOT NULL,
    is_private boolean DEFAULT false NOT NULL,
    lookup_status character varying(50) NOT NULL,
    observed_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    data_mode character varying(50) DEFAULT 'UNAVAILABLE'::character varying NOT NULL
);


ALTER TABLE public.ip_artifacts OWNER TO postgres;

--
-- Name: qr_artifacts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.qr_artifacts (
    id uuid NOT NULL,
    visual_artifact_id uuid NOT NULL,
    decoded_payload text NOT NULL,
    payload_type character varying(50) NOT NULL,
    normalized_url text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.qr_artifacts OWNER TO postgres;

--
-- Name: received_hops; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.received_hops (
    id uuid NOT NULL,
    analysis_run_id uuid NOT NULL,
    hop_order integer NOT NULL,
    from_host character varying(255),
    by_host character varying(255),
    protocol character varying(50),
    hop_ip character varying(50),
    hop_timestamp timestamp with time zone,
    delay_seconds bigint,
    raw_hop_line text NOT NULL,
    parsing_warning character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.received_hops OWNER TO postgres;

--
-- Name: risk_factors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.risk_factors (
    id uuid NOT NULL,
    analysis_run_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    factor_code character varying(80) NOT NULL,
    severity character varying(20) NOT NULL,
    weight_points integer NOT NULL,
    explanation text NOT NULL,
    evidence_reference character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT risk_factors_weight_points_check CHECK ((weight_points >= 0))
);


ALTER TABLE public.risk_factors OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    username character varying(80) NOT NULL,
    email character varying(254) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(30) DEFAULT 'ANALYST'::character varying NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'ANALYST'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: visual_artifacts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.visual_artifacts (
    id uuid NOT NULL,
    analysis_run_id uuid NOT NULL,
    filename character varying(255) NOT NULL,
    content_type character varying(100) NOT NULL,
    file_size_bytes bigint NOT NULL,
    sha256_hash character(64) NOT NULL,
    processing_error text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    data_mode character varying(50) DEFAULT 'NOT_CHECKED'::character varying NOT NULL,
    CONSTRAINT visual_artifacts_file_size_bytes_check CHECK ((file_size_bytes >= 0))
);


ALTER TABLE public.visual_artifacts OWNER TO postgres;

--
-- Data for Name: ai_grounded_assessments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ai_grounded_assessments (id, email_evidence_id, analysis_run_id, provider, model_name, prompt_version, status, threat_classification, confidence_assessment, summary_explanation, key_indicators_json, evidence_references_json, unsupported_references_json, limitations_and_caveats, untrusted_injections_detected, sanitized_context_summary, error_message, tokens_used, generation_time_ms, created_at) FROM stdin;
\.


--
-- Data for Name: analysis_results; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.analysis_results (analysis_run_id, header_result_id, from_address, from_domain, reply_to, return_path, dkim_domain, reported_spf, reported_dkim, reported_dmarc, reported_arc, current_dns_observation, independent_verification, identity_mismatch_flags, raw_headers_json, header_created_at, content_result_id, ml_model_version, probability, predicted_label, ml_status, urgency, credential_request, payment_instruction, impersonation, secrecy, prompt_injection_detected, prompt_matches, social_engineering_matches, content_created_at, risk_result_id, deterministic_risk_score, risk_level, evidence_confidence_score, confidence_level, confidence_explanation, category_scores_json, total_uncapped_score, engine_version, evaluated_at, risk_created_at) FROM stdin;
\.


--
-- Data for Name: analysis_runs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.analysis_runs (id, email_id, pipeline_version, status, current_stage, attempt_number, started_at, completed_at, error_code, safe_error_message, created_at) FROM stdin;
\.


--
-- Data for Name: email_evidence; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.email_evidence (id, case_id, original_filename, storage_key, file_size_bytes, sha256_hash, subject, message_id, sent_date, uploaded_by, uploaded_at) FROM stdin;
\.


--
-- Data for Name: extracted_urls; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.extracted_urls (id, analysis_run_id, raw_url, normalized_url, domain, source_type, anchor_text, anchor_mismatch, is_ip_url, is_shortened, unusual_port, subdomain_excess, lookalike, dns_status, url_risk_score, created_at, data_mode) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	core schema	SQL	V1__core_schema.sql	-299175826	postgres	2026-10-24 11:32:44.565844	51	t
2	2	evidence metadata	SQL	V2__evidence_metadata.sql	2072958282	postgres	2026-10-24 11:55:55.500759	125	t
3	3	header and hop findings	SQL	V3__header_and_hop_findings.sql	1074009799	postgres	2026-10-24 13:16:43.846717	72	t
4	4	url and infrastructure findings	SQL	V4__url_and_infrastructure_findings.sql	749527883	postgres	2026-10-24 18:00:48.608963	85	t
5	5	visual and qr findings	SQL	V5__visual_and_qr_findings.sql	1753643686	postgres	2026-10-24 18:52:12.891254	114	t
6	6	content analysis findings	SQL	V6__content_analysis_findings.sql	227340754	postgres	2026-10-24 20:05:19.179387	52	t
7	7	risk scoring and confidence	SQL	V7__risk_scoring_and_confidence.sql	-2077020358	postgres	2026-10-25 13:48:10.041406	143	t
8	8	correlation and campaigns	SQL	V8__correlation_and_campaigns.sql	-1849481128	postgres	2026-10-25 14:41:11.760787	100	t
9	9	ai grounded assessments	SQL	V9__ai_grounded_assessments.sql	1641706848	postgres	2026-10-25 15:32:32.800223	84	t
10	10	add data mode	SQL	V10__add_data_mode.sql	1202158687	postgres	2026-10-27 18:33:14.543835	34	t
11	11	remove ocr columns	SQL	V11__remove_ocr_columns.sql	1054641942	postgres	2026-10-29 21:09:32.262659	79	t
12	12	remove geoip asn columns	SQL	V12__remove_geoip_asn_columns.sql	173384694	postgres	2026-10-29 22:57:15.348334	28	t
13	13	remove campaigns	SQL	V13__remove_campaigns.sql	394140359	postgres	2026-10-29 23:16:51.438766	39	t
14	14	consolidate analysis results	SQL	V14__consolidate_analysis_results.sql	-1826201158	postgres	2026-11-06 09:08:31.195998	92	t
\.


--
-- Data for Name: fore_cases; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.fore_cases (id, case_number, title, description, status, analyst_notes, created_by, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: historical_matches; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.historical_matches (id, source_run_id, matched_email_id, indicator_type, normalized_indicator_value, match_strength, match_explanation, created_at) FROM stdin;
\.


--
-- Data for Name: ip_artifacts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ip_artifacts (id, analysis_run_id, ip_address, source_context, is_private, lookup_status, observed_at, created_at, data_mode) FROM stdin;
\.


--
-- Data for Name: qr_artifacts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.qr_artifacts (id, visual_artifact_id, decoded_payload, payload_type, normalized_url, created_at) FROM stdin;
\.


--
-- Data for Name: received_hops; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.received_hops (id, analysis_run_id, hop_order, from_host, by_host, protocol, hop_ip, hop_timestamp, delay_seconds, raw_hop_line, parsing_warning, created_at) FROM stdin;
\.


--
-- Data for Name: risk_factors; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.risk_factors (id, analysis_run_id, category, factor_code, severity, weight_points, explanation, evidence_reference, created_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, email, password_hash, role, enabled, created_at) FROM stdin;
3d0658fa-dead-47ea-b7d3-2d82e9918a95	analyst	analyst@emailforensics.local	$2a$10$gQHYWFqeq3P4p9cuAi5CfeOxRb9CfVJyGoaPlb5A53LWMfBgc3RbK	ANALYST	t	2026-11-09 15:30:32.093587+05:30
6c8eec53-5430-475f-91e1-a08faa3bae52	admin	admin@emailforensics.local	$2a$10$tTRGbtiM5k/IqatQk21UWOFyOBfax4neU/zx.s2N42NizJF2pznI6	ADMIN	t	2026-11-09 15:30:32.197929+05:30
\.


--
-- Data for Name: visual_artifacts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.visual_artifacts (id, analysis_run_id, filename, content_type, file_size_bytes, sha256_hash, processing_error, created_at, data_mode) FROM stdin;
\.


--
-- Name: ai_grounded_assessments ai_grounded_assessments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_grounded_assessments
    ADD CONSTRAINT ai_grounded_assessments_pkey PRIMARY KEY (id);


--
-- Name: analysis_results analysis_results_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_results
    ADD CONSTRAINT analysis_results_pkey PRIMARY KEY (analysis_run_id);


--
-- Name: analysis_runs analysis_runs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_runs
    ADD CONSTRAINT analysis_runs_pkey PRIMARY KEY (id);


--
-- Name: email_evidence email_evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_evidence
    ADD CONSTRAINT email_evidence_pkey PRIMARY KEY (id);


--
-- Name: email_evidence email_evidence_storage_key_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_evidence
    ADD CONSTRAINT email_evidence_storage_key_key UNIQUE (storage_key);


--
-- Name: extracted_urls extracted_urls_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.extracted_urls
    ADD CONSTRAINT extracted_urls_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: fore_cases fore_cases_case_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fore_cases
    ADD CONSTRAINT fore_cases_case_number_key UNIQUE (case_number);


--
-- Name: fore_cases fore_cases_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fore_cases
    ADD CONSTRAINT fore_cases_pkey PRIMARY KEY (id);


--
-- Name: historical_matches historical_matches_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.historical_matches
    ADD CONSTRAINT historical_matches_pkey PRIMARY KEY (id);


--
-- Name: ip_artifacts ip_artifacts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ip_artifacts
    ADD CONSTRAINT ip_artifacts_pkey PRIMARY KEY (id);


--
-- Name: qr_artifacts qr_artifacts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.qr_artifacts
    ADD CONSTRAINT qr_artifacts_pkey PRIMARY KEY (id);


--
-- Name: received_hops received_hops_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.received_hops
    ADD CONSTRAINT received_hops_pkey PRIMARY KEY (id);


--
-- Name: risk_factors risk_factors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.risk_factors
    ADD CONSTRAINT risk_factors_pkey PRIMARY KEY (id);


--
-- Name: historical_matches uq_historical_matches; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.historical_matches
    ADD CONSTRAINT uq_historical_matches UNIQUE (source_run_id, matched_email_id, indicator_type, normalized_indicator_value);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: visual_artifacts visual_artifacts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.visual_artifacts
    ADD CONSTRAINT visual_artifacts_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_ai_assessments_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_assessments_created ON public.ai_grounded_assessments USING btree (created_at DESC);


--
-- Name: idx_ai_assessments_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_assessments_email ON public.ai_grounded_assessments USING btree (email_evidence_id);


--
-- Name: idx_ai_assessments_run; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_assessments_run ON public.ai_grounded_assessments USING btree (analysis_run_id);


--
-- Name: idx_analysis_results_from_domain; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_analysis_results_from_domain ON public.analysis_results USING btree (from_domain);


--
-- Name: idx_analysis_run_email_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_analysis_run_email_created ON public.analysis_runs USING btree (email_id, created_at DESC);


--
-- Name: idx_evidence_case_uploaded; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_evidence_case_uploaded ON public.email_evidence USING btree (case_id, uploaded_at DESC);


--
-- Name: idx_evidence_sha256; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_evidence_sha256 ON public.email_evidence USING btree (sha256_hash);


--
-- Name: idx_extracted_urls_domain; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_extracted_urls_domain ON public.extracted_urls USING btree (domain);


--
-- Name: idx_extracted_urls_normalized; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_extracted_urls_normalized ON public.extracted_urls USING btree (normalized_url);


--
-- Name: idx_extracted_urls_run; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_extracted_urls_run ON public.extracted_urls USING btree (analysis_run_id);


--
-- Name: idx_fore_cases_created_by; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_fore_cases_created_by ON public.fore_cases USING btree (created_by);


--
-- Name: idx_fore_cases_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_fore_cases_status ON public.fore_cases USING btree (status);


--
-- Name: idx_historical_matches_matched; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_historical_matches_matched ON public.historical_matches USING btree (matched_email_id);


--
-- Name: idx_historical_matches_source; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_historical_matches_source ON public.historical_matches USING btree (source_run_id);


--
-- Name: idx_historical_matches_type_val; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_historical_matches_type_val ON public.historical_matches USING btree (indicator_type, normalized_indicator_value);


--
-- Name: idx_ip_artifacts_address; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ip_artifacts_address ON public.ip_artifacts USING btree (ip_address);


--
-- Name: idx_ip_artifacts_run; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ip_artifacts_run ON public.ip_artifacts USING btree (analysis_run_id);


--
-- Name: idx_qr_artifacts_visual; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_qr_artifacts_visual ON public.qr_artifacts USING btree (visual_artifact_id);


--
-- Name: idx_received_hops_run_order; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_received_hops_run_order ON public.received_hops USING btree (analysis_run_id, hop_order);


--
-- Name: idx_risk_factors_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_risk_factors_code ON public.risk_factors USING btree (factor_code);


--
-- Name: idx_risk_factors_run; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_risk_factors_run ON public.risk_factors USING btree (analysis_run_id);


--
-- Name: idx_visual_artifacts_run; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_visual_artifacts_run ON public.visual_artifacts USING btree (analysis_run_id);


--
-- Name: idx_visual_artifacts_sha256; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_visual_artifacts_sha256 ON public.visual_artifacts USING btree (sha256_hash);


--
-- Name: ai_grounded_assessments ai_grounded_assessments_analysis_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_grounded_assessments
    ADD CONSTRAINT ai_grounded_assessments_analysis_run_id_fkey FOREIGN KEY (analysis_run_id) REFERENCES public.analysis_runs(id) ON DELETE RESTRICT;


--
-- Name: ai_grounded_assessments ai_grounded_assessments_email_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_grounded_assessments
    ADD CONSTRAINT ai_grounded_assessments_email_evidence_id_fkey FOREIGN KEY (email_evidence_id) REFERENCES public.email_evidence(id) ON DELETE RESTRICT;


--
-- Name: analysis_results analysis_results_analysis_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_results
    ADD CONSTRAINT analysis_results_analysis_run_id_fkey FOREIGN KEY (analysis_run_id) REFERENCES public.analysis_runs(id) ON DELETE RESTRICT;


--
-- Name: analysis_runs analysis_runs_email_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_runs
    ADD CONSTRAINT analysis_runs_email_id_fkey FOREIGN KEY (email_id) REFERENCES public.email_evidence(id) ON DELETE RESTRICT;


--
-- Name: email_evidence email_evidence_case_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_evidence
    ADD CONSTRAINT email_evidence_case_id_fkey FOREIGN KEY (case_id) REFERENCES public.fore_cases(id) ON DELETE RESTRICT;


--
-- Name: email_evidence email_evidence_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_evidence
    ADD CONSTRAINT email_evidence_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: extracted_urls extracted_urls_analysis_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.extracted_urls
    ADD CONSTRAINT extracted_urls_analysis_run_id_fkey FOREIGN KEY (analysis_run_id) REFERENCES public.analysis_runs(id) ON DELETE RESTRICT;


--
-- Name: fore_cases fore_cases_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fore_cases
    ADD CONSTRAINT fore_cases_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: historical_matches historical_matches_matched_email_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.historical_matches
    ADD CONSTRAINT historical_matches_matched_email_id_fkey FOREIGN KEY (matched_email_id) REFERENCES public.email_evidence(id) ON DELETE RESTRICT;


--
-- Name: historical_matches historical_matches_source_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.historical_matches
    ADD CONSTRAINT historical_matches_source_run_id_fkey FOREIGN KEY (source_run_id) REFERENCES public.analysis_runs(id) ON DELETE RESTRICT;


--
-- Name: ip_artifacts ip_artifacts_analysis_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ip_artifacts
    ADD CONSTRAINT ip_artifacts_analysis_run_id_fkey FOREIGN KEY (analysis_run_id) REFERENCES public.analysis_runs(id) ON DELETE RESTRICT;


--
-- Name: qr_artifacts qr_artifacts_visual_artifact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.qr_artifacts
    ADD CONSTRAINT qr_artifacts_visual_artifact_id_fkey FOREIGN KEY (visual_artifact_id) REFERENCES public.visual_artifacts(id) ON DELETE RESTRICT;


--
-- Name: received_hops received_hops_analysis_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.received_hops
    ADD CONSTRAINT received_hops_analysis_run_id_fkey FOREIGN KEY (analysis_run_id) REFERENCES public.analysis_runs(id) ON DELETE RESTRICT;


--
-- Name: risk_factors risk_factors_analysis_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.risk_factors
    ADD CONSTRAINT risk_factors_analysis_run_id_fkey FOREIGN KEY (analysis_run_id) REFERENCES public.analysis_runs(id) ON DELETE RESTRICT;


--
-- Name: visual_artifacts visual_artifacts_analysis_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.visual_artifacts
    ADD CONSTRAINT visual_artifacts_analysis_run_id_fkey FOREIGN KEY (analysis_run_id) REFERENCES public.analysis_runs(id) ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--

\unrestrict FsJH7NayScUsTQKAyZFU9Z6zrgpCjFEv8Herh2SQR6u9hTwSPAjPLcl7gbQTSRm

