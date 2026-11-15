---
name: database-development
description: Use this skill when writing PostgreSQL queries, creating or modifying Spring Data JPA entities, updating database schema, or optimizing correlation queries.
---

# Skill: PostgreSQL Database Development and Correlation

This skill outlines the database schema, entity mapping, JSONB handling, and query design for the Email Forensics application.

Consult the authoritative specification in [docs/architecture-v2.md](../../../docs/architecture-v2.md).

---

## 1. Database Architecture: Two Tables Only

The system uses strictly two tables in PostgreSQL (`email_forensics_db`):

```
┌─────────────────────────────────┐
│             emails              │
├─────────────────────────────────┤
│ id (BIGSERIAL, PK)              │
│ file_name (VARCHAR)             │
│ file_path (TEXT)                │
│ sha256 (CHAR(64))               │
│ sender (TEXT)                   │
│ reply_to (TEXT)                 │
│ subject (TEXT)                  │
│ sent_at (TIMESTAMPTZ)           │
│ auth_data (JSONB)               │
│ ai_data (JSONB)                 │
│ status (VARCHAR(24))            │
│ stage (VARCHAR(32))             │
│ risk_score (SMALLINT)           │
│ risk_level (VARCHAR(20))        │
│ verdict (VARCHAR(30))           │
│ error_message (TEXT)            │
│ created_at (TIMESTAMPTZ)        │
│ completed_at (TIMESTAMPTZ)      │
└────────────────┬────────────────┘
                 │ 1
                 │
                 │ has many (ON DELETE RESTRICT)
                 │
                 │ *
┌────────────────┴────────────────┐
│           indicators            │
├─────────────────────────────────┤
│ id (BIGSERIAL, PK)              │
│ email_id (BIGINT, FK)           │
│ type (VARCHAR(24))              │
│ value (TEXT)                    │
│ normalized_value (TEXT)         │
│ origin (VARCHAR(24))            │
│ verdict (VARCHAR(24))           │
│ details (JSONB)                 │
│ checked_at (TIMESTAMPTZ)        │
└─────────────────────────────────┘
```

Do not add additional tables for cases, campaigns, users, sessions, or threat provider dumps.

---

## 2. Complete SQL Schema Definition

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

---

## 3. Indicator Origin Mapping Standards

The `origin` column uses strictly one of the six allowed values:
- `HEADER`: Extracted from RFC 5322 headers (From/Reply-To domains, Received hops, relay IPs).
- `BODY`: Extracted from plain-text email bodies.
- `HTML`: Extracted from HTML anchor hrefs, image tags, or form actions.
- `DNS`: Discovered through domain DNS lookups (A/AAAA/MX records).
- `IMAGE_OCR`: Extracted as visible text from an image by local Tesseract OCR.
- `QR`: Decoded destination URLs from QR codes by ZXing.

---

## 4. Why JSONB is Used

External threat intelligence providers return different response schemas. Rather than adding dozens of nullable columns, store compact normalized observations in JSONB columns:
- `emails.auth_data`: Holds parsed SPF, DKIM, DMARC, ARC evaluation results and Received hop records.
- `emails.ai_data`: Holds structured AI classification, summary, fraud indicators, and limitations.
- `indicators.details`: Holds compact provider findings and IP reputation metadata.

---

## 5. Historical Correlation SQL Query

Historical correlation identifies past investigations that encountered the same normalized indicator:

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

To display related emails for a specific investigation:
1. Extract all normalized indicators for the target email.
2. Query other emails containing those same normalized indicators (excluding the current `email_id`).
3. Return the matching email summary fields: `id`, `file_name`, `sender`, `subject`, `risk_score`, `verdict`, and `created_at`.
