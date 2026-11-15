# Email Forensics Version 2 — Authoritative Architecture & Technical Specification

This document serves as the single authoritative source of truth for the **Email Forensics Version 2** system architecture, data models, threat intelligence integrations, risk scoring methodology, and security guardrails. All agent rules, skills, and implementation plans must align with this specification.

---

## 1. System Overview and Architecture Style

Email Forensics Version 2 is an AI-assisted forensic web application designed for security analysts to investigate suspicious `.eml` files safely and thoroughly.

- **Architecture Style:** Modular Monolith.
- **Frontend:** React.js (v18+) with Vite, styled with modern CSS.
- **Backend:** Java 21 with Spring Boot 4.1.x.
- **Database:** PostgreSQL (`email_forensics_db`) with strictly **two** application tables: `emails` and `indicators`.
- **Email Authentication Helper:** Node.js 22+ with the `mailauth` library executed as an isolated local process.
- **Local OCR & Barcode Engines:** Local Tesseract OCR 5 engine for image text extraction and ZXing for QR code decoding.
- **AI Integration:** Spring AI with the official Google GenAI starter (Gemini), operating strictly on pattern-redacted text after explicit analyst approval.
- **Reporting Engine:** Apache PDFBox (v3.0.x) generating standalone, downloadable forensic summary reports.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        React + Vite Frontend                           │
│  UploadPage │ AnalysisPage │ ResultPage │ HistoryPage │ RedactionModal │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP / Multipart / JSON (Port 8080)
                                    v
┌────────────────────────────────────────────────────────────────────────┐
│                      Spring Boot 4.1.x Backend                         │
│                                                                        │
│  Controllers: EmailController, HealthController                        │
│  Orchestrator: AnalysisOrchestrator (ThreadPoolTaskExecutor)           │
│                                                                        │
│  Core Services:                                                        │
│  - EvidenceService: SHA-256 calculation & UUID private file storage   │
│  - EmailParserService: Jakarta Mail (Angus Mail) & Jsoup parsing      │
│  - AuthenticationService: Node.js mailauth process runner              │
│  - IndicatorExtractorService: URL, domain, IP, and text extraction     │
│  - ImageAnalysisService: Tesseract OCR & ZXing QR decoding            │
│  - ThreatIntelService: External provider API clients (RestClient)      │
│  - TextRedactorService: Regex PII masking & approval gating           │
│  - AiAnalysisService: Spring AI Gemini structured assessment          │
│  - CorrelationService: PostgreSQL historical indicator queries         │
│  - RiskScoreService: Rule-based explainable risk calculation           │
│  - ReportService: Apache PDFBox forensic report generator             │
└───────┬─────────────────────────┬──────────────────────────────┬───────┘
        │                         │                              │
        v                         v                              v
┌────────────────┐      ┌──────────────────┐           ┌─────────────────┐
│   PostgreSQL   │      │ Node.js mailauth │           │  Threat Intel   │
│ (emails and    │      │  Helper Process  │           │  & AI APIs      │
│  indicators)   │      │ (SPF/DKIM/DMARC) │           │ (SafeBrowsing,  │
└────────────────┘      └──────────────────┘           │  PhishTank, etc)│
                                                       └─────────────────┘
```

---

## 2. Complete List of 22 Approved Features

1. **EML Upload:** Upload one raw `.eml` file at a time via `multipart/form-data`.
2. **File Validation:** Server-side validation of file existence, `.eml` extension, 10 MB size limit, non-empty content, and email parseability.
3. **SHA-256 Fingerprint:** Compute SHA-256 immediately from original raw bytes using `java.security.MessageDigest`.
4. **Evidence Preservation:** Store original unedited bytes under a server-generated UUID path (`storage/emails/<uuid>/original.eml`).
5. **MIME & Header Extraction:** Parse all RFC 5322 headers, MIME parts, plain text, and HTML bodies using Jakarta Mail (Eclipse Angus) and Jsoup.
6. **Sender Identity Analysis:** Compare From, Reply-To, Return-Path, DKIM signing domain, and Received hops to detect spoofing or inconsistencies.
7. **Delivery Path Reconstruction:** Trace Received headers chronologically from newest to oldest, identifying relay hops and untrusted boundaries.
8. **Mail Authentication Verification:** Call Node.js `mailauth` helper to verify SPF, DKIM, DMARC, and ARC using original email bytes and DNS queries.
9. **Indicator Extraction & Normalization:** Extract URLs, domains, and IP addresses; normalize for duplicate matching while preserving original raw values.
10. **URL & Domain Reputation:** Check extracted URLs and domains against Google Safe Browsing, PhishTank, OpenPhish feed, URLhaus, and ThreatFox.
11. **IP Intelligence:** Check public IPs against AbuseIPDB for abuse reputation, IPinfo Lite for country and ASN, and VirusTotal/GreyNoise context.
12. **Image Extraction:** Extract inline images and image attachments from MIME parts (non-image attachments are safely ignored).
13. **Local OCR & QR Decoding:** Extract text from images using local Tesseract OCR; decode QR codes using ZXing.
14. **QR Destination Routing:** Treat decoded QR URLs as high-priority URL indicators and feed them through the URL reputation pipeline.
15. **PII Redaction:** Mask personal names (`[PERSON]`), email local parts (`[EMAIL]`), phone numbers (`[PHONE]`), account/card numbers (`[ACCOUNT]`, `[CARD]`), OTPs (`[OTP]`), and secrets (`[SECRET]`).
16. **Explicit AI Approval Gate:** Display the exact redacted preview in the UI; pause analysis at `WAITING_FOR_AI_APPROVAL` until the analyst explicitly approves or declines cloud AI processing.
17. **Threat Signal Classification:** Screen email text and OCR content for phishing patterns, Business Email Compromise (BEC), urgency/coercion, impersonation, and prompt injection attempts.
18. **Historical Correlation:** Query earlier `indicators` rows in PostgreSQL for exact normalized matches across past investigations.
19. **Related Emails Table:** Display related investigations in simple tabular format (no graph visualization).
20. **Explainable Risk Scoring:** Calculate a 0-100 score based on deterministic evidence points with an itemized breakdown of reasons.
21. **Separate Confidence Rating:** Display confidence (`HIGH`, `MEDIUM`, `LOW`) separately from the risk score to reflect evidence completeness.
22. **Forensic PDF Report:** Generate a comprehensive, downloadable PDF report using Apache PDFBox summarizing findings, evidence hashes, and limitations.

---

## 3. Explicitly Excluded Features (Strict MVP Guardrails)

The following enterprise technologies and architectural patterns are **strictly prohibited** in Version 2:

- **No User Management:** No user registration, login, JWT tokens, OAuth, session management, or user roles.
- **No Spring Security:** Backend REST APIs run unauthenticated on `localhost` or a private lab network.
- **No Extra Tables:** No tables for users, roles, cases, campaigns, graph nodes, or raw API response dumps.
- **No Graph Databases or Views:** No Neo4j, Amazon Neptune, or frontend network graphs. Historical matches use PostgreSQL queries and simple tables.
- **No Message Brokers:** No Apache Kafka, RabbitMQ, or ActiveMQ. Asynchronous tasks use Spring's in-memory `ThreadPoolTaskExecutor`.
- **No Distributed Caches:** No Redis, Memcached, or Hazelcast.
- **No Container Orchestration:** No Kubernetes, Helm charts, or multi-container microservice meshes during standard development.
- **No Non-Image Attachment Scanning:** No malware execution, sandboxing, or Office/archive deep scanning. Non-image attachments are ignored.
- **No Live URL Fetching:** Never visit, crawl, or fetch URLs found in emails. Lookups are strictly reputation-query-based.

---

## 4. External Intelligence Providers and Fallback Hierarchy

### URL and Domain Intelligence
- **Google Safe Browsing v5:** Checks URLs against Google's unsafe URL lists (max 50 URLs per request, non-commercial free tier).
- **PhishTank:** Checks community-verified phishing URLs via API (requires User-Agent and optional `app_key`).
- **OpenPhish Community Feed:** Downloaded every 12 hours and stored locally; extracted URLs are checked against the local feed.
- **URLhaus:** Checks URLs against abuse.ch malware distribution database (requires `Auth-Key`).
- **ThreatFox:** Searches for malware-related IOC matches (requires `Auth-Key`).
- **VirusTotal Public API:** Optional cross-check for URLs and domains (strictly subject to public API rate limits; never upload original emails).

### IP Intelligence & Geolocation
- **AbuseIPDB:** Checks IP abuse reports, confidence score, and report history (free tier allows 1,000 checks/day).
- **IPinfo Lite:** Provides country code, country name, ASN, and organization (free Lite API; does not provide city-level data).
- **MaxMind GeoLite2:** Local offline database fallback for IP country and ASN lookups when external network access is restricted.
- **GreyNoise Community:** Optional context for observed internet scanning activity (`noise=true` indicates scanning, not maliciousness).
- **VirusTotal IP:** Optional cross-check for public IPs within quota limits.

> [!IMPORTANT]
> **Provider Differentiation Rule:** Different providers perform fundamentally different tasks. IP geolocation (IPinfo, MaxMind) provides network context and **must never** be treated as a replacement for IP abuse reputation (AbuseIPDB, ThreatFox).

### Local Tools & AI Providers
- **OCR Engine:** Primary is local **Tesseract 5**. **OCR.space** is an optional cloud fallback only when local OCR fails and the user gives explicit consent (free tier capped at 500 req/day).
- **QR Decoder:** Local **ZXing** library.
- **AI Threat Evaluation:** Primary is **Google Gemini** via Spring AI (using redacted text with explicit user approval). **Ollama** (local LLM) is supported as an optional local fallback when cloud AI cannot be used.

### Provider Result Status Model
Every external provider result must normalize into one of four distinct statuses:
- `MATCH`: The provider found a matching threat or abuse record.
- `NO_MATCH`: The provider was queried successfully and had no matching threat record. **`NO_MATCH` does NOT prove that an indicator is safe.**
- `ERROR`: The query failed (e.g. timeout, HTTP 5xx, invalid API key).
- `UNKNOWN`: The result could not be determined due to missing evidence or provider unavailability.

Provider failures must never crash the pipeline; the investigation continues with remaining providers and marks status as `PARTIAL`.

---

## 5. PostgreSQL Database Architecture (Two Tables Only)

The database `email_forensics_db` contains exactly two application tables:

```sql
CREATE DATABASE email_forensics_db;

CREATE TABLE emails (
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

CREATE TABLE indicators (
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

CREATE INDEX idx_emails_created_at ON emails(created_at DESC);
CREATE INDEX idx_emails_sha256 ON emails(sha256);
CREATE INDEX idx_emails_status ON emails(status);
CREATE INDEX idx_indicators_email_id ON indicators(email_id);
CREATE INDEX idx_indicators_type_value ON indicators(type, normalized_value);
CREATE INDEX idx_indicators_normalized_value ON indicators(normalized_value);
CREATE INDEX idx_indicators_details_gin ON indicators USING GIN(details);
```

### Historical Correlation SQL Query
```sql
SELECT
    i.type,
    i.normalized_value,
    COUNT(DISTINCT i.email_id) AS email_count,
    ARRAY_AGG(DISTINCT i.email_id) AS related_email_ids
FROM indicators i
WHERE i.normalized_value = :normalizedValue
  AND i.type = :indicatorType
GROUP BY i.type, i.normalized_value
HAVING COUNT(DISTINCT i.email_id) > 1;
```

---

## 6. Risk Scoring and Evaluation Methodology

### Scoring Principles
- The backend `RiskScoreService` calculates the numeric score deterministically.
- AI never decides or alters the numeric risk score.
- Duplicate indicators are never double-counted.
- Historical matches increase risk only when the previous indicator was confirmed malicious.
- Shared cloud hosting IPs or ASN matches alone must never trigger campaign verdicts.

### Provisional Scoring Factors (Capped at 100 points):
1. **Verified Phishing URL or Domain Match:** `+40 points` (PhishTank or Safe Browsing verified match).
2. **Confirmed Malware-Distribution URL Match:** `+40 points` (URLhaus match; if already counted as phishing URL, take max, do not double-count).
3. **Known Malicious IP Evidence:** `+15 points` (ThreatFox IP match or AbuseIPDB confidence score > 80).
4. **Strong Email Authentication Inconsistency:** `+15 points` (DMARC failure with From/Reply-To mismatch, or DKIM failure with bad signature).
5. **Separate Malicious QR Destination:** `+10 points` (only if the QR destination is distinct from already scored URLs).
6. **AI-Supported Social Engineering / Fraud Signals:** `+5 points` (urgent payment instruction, credential harvesting, or executive impersonation).
7. **Historical Repeat of Confirmed Malicious Indicator:** `+10 points` (exact match with an indicator marked MALICIOUS in a previous investigation).

$$\text{Final Risk Score} = \min(\text{Raw Points}, 100)$$

### Provisional Nature of Weights
All scoring weights are **provisional starting heuristics**. They must be validated and tuned on a labelled dataset of legitimate and phishing emails using formal metrics:
- Precision
- Recall
- F1-Score
- False Positive Rate
- Confusion Matrix

No claim of research-grade accuracy (such as 99% accuracy) may be made without local benchmark validation.

### Confidence vs Risk
Risk score (0-100) and confidence (`HIGH`, `MEDIUM`, `LOW`) are separate dimensions:
- **`HIGH` Confidence:** Multiple independent, recent sources agree, and mail authentication context is fully verifiable.
- **`MEDIUM` Confidence:** Reliable evidence exists, but some provider checks failed or SMTP context was inferred.
- **`LOW` Confidence:** Findings depend primarily on heuristic text analysis, stale feeds, or failed external lookups.

---

## 7. Frontend Design Identity: Visual Reference Only

The existing Version 1 frontend located in `frontend/` serves as a **visual reference only**:
- **Design Inspiration:** Maintain the professional cybersecurity dark theme, Slate color palette (`#0f172a`, `#1e293b`, `#334155`), card borders, badge styles, and typography.
- **Strict Prohibition:** Agents must **NOT** copy, import, or reuse V1 JavaScript source code, legacy state management, or old API logic.
- **Fresh Implementation:** Build completely new React components using Vite, native `fetch()`, standard React hooks (`useEmailAnalysis`), and clean code without comments.

---

## 8. Git Safety and Manual Commit Policy

- **Developer Owns Git:** The human developer manually reviews, stages, commits, and pushes all code.
- **Absolute Prohibition for Agents:** No AI agent may execute `git commit`, `git push`, `git push --force`, or any GitHub write commands under any circumstance.
- **Branch Protection:** The `main` branch contains stable Version 1 and must **never** be modified. All work occurs on `development` (or feature branches cut from `development`).
- **Enforcement:** Enforced technically via Antigravity `PreToolUse` lifecycle hook in `.agents/hooks.json` and `.agents/scripts/block-git-commits.mjs`, backed by explicit agent rules.
