# Email Forensics Platform

An open-source digital forensic application for analyzing suspicious emails (`.eml` files). It helps security analysts inspect email headers, trace network transit hops, extract and check URLs and IP addresses, decode QR codes, detect social engineering and prompt injections, calculate an explainable risk score, and generate court-ready investigation reports.

---

## What This Project Does

When an analyst receives a suspicious or malicious email, they upload the raw `.eml` file into a case. The platform automatically:

1. **Protects Evidence Integrity:** Stores the original file in a local evidence vault and computes a SHA-256 hash immediately to ensure the file has not been tampered with.
2. **Parses Headers and Authentication:** Extracts sender details (`From`, `Reply-To`, `Return-Path`), checks for display name spoofing, and reads reported SPF, DKIM, DMARC, and ARC authentication results.
3. **Reconstructs Transit Hops:** Reads all `Received:` headers in chronological order, measures delivery delays between mail servers, and identifies public and private IP addresses.
4. **Extracts Links and IP Addresses:** Extracts all URLs from HTML and plain text bodies, detects anchor text mismatches (for example, text saying `paypal.com` but linking to an external site), and classifies server IPs.
5. **Inspects Images and Decodes QR Codes:** Extracts inline images and attachments, and automatically decodes QR codes (quishing detection) using ZXing.
6. **Analyzes Email Content:** Uses an embedded Tribuo machine learning model to classify spam/phishing text, scans for social engineering cues (urgency, credential harvesting, wire transfers, impersonation), and checks for hidden prompt injection attacks against LLMs.
7. **Calculates an Explainable Risk Score:** Computes a transparent 0–100 risk score based on weighted forensic rules. Every score comes with a confidence rating and an itemized breakdown of contributing factors.
8. **Correlates Across Past Cases:** Checks if sender domains, subject patterns, IP addresses, or attachment hashes match evidence from other investigations in the database.
9. **Generates AI Threat Summaries:** Provides a grounded technical summary and maps findings to MITRE ATT&CK techniques (supports Groq/OpenAI compatible models with an automatic local fallback).
10. **Produces Multi-Format Reports:** Generates an interactive web dossier, a printer-friendly PDF layout with analyst sign-off blocks, and a structured JSON export.

---

## Architecture and Pipeline

```
[ Raw .eml Upload ]
        │
        ▼
[ Evidence Vault ] ──► Compute SHA-256 Checksum
        │
        ▼
[ MIME Parser (Jakarta Mail) ]
        │
        ├─► Header Analysis (SPF, DKIM, DMARC, Return-Path alignment)
        ├─► Transit Analysis (Received hops, delays, IP classification)
        ├─► URL & Network Intel (Anchor mismatches, suspicious domains)
        ├─► Visual & QR Engine (Image extraction, ZXing QR decoding)
        ├─► Content Engine (Tribuo ML, social engineering rules, prompt injection)
        ├─► Risk Scoring Engine (Deterministic 0-100 score + confidence)
        └─► Historical Correlation (Matches across existing cases)
        │
        ▼
[ Database Storage ] ──► Stored in 13 PostgreSQL tables
        │
        ▼
[ Real-time Updates ] ──► Server-Sent Events (SSE) stream to React frontend
        │
        ▼
[ Dossier & Reports ] ──► Web UI, Printable PDF, JSON Export
```

---

## Tech Stack

### Backend
- **Java 21**
- **Spring Boot 4.1** (REST APIs, Security, Data JPA)
- **PostgreSQL 16** (Primary relational database)
- **Flyway** (Automated database migrations, V1 through V14)
- **Jakarta Mail & Angus Mail** (RFC 5322 / MIME parsing)
- **Oracle Tribuo 4.3** (Embedded machine learning for content classification)
- **ZXing 3.5** (QR code reading and decoding)
- **JJWT 0.12** (Stateless JWT authentication)

### Frontend
- **React 19**
- **Vite 8**
- **Tailwind CSS 4**
- **Axios** (API communication)
- **Lucide React** (Icons)

---

## Database Structure

The database uses PostgreSQL with 13 consolidated tables managed through Flyway migrations:

| Table | What It Stores |
|---|---|
| `users` | User accounts, credentials, and roles (`ANALYST`, `ADMIN`). |
| `fore_cases` | Investigation cases (case number, title, description, severity, status). |
| `email_evidence` | Uploaded email files, file sizes, SHA-256 hashes, and vault paths. |
| `analysis_runs` | Individual analysis attempts for an evidence file, tracking status and progress. |
| `analysis_results` | Consolidated results for a run: header checks, ML content analysis, and risk scoring. |
| `received_hops` | Chronological mail transit hops parsed from `Received:` headers. |
| `extracted_urls` | Extracted links, anchor text mismatches, and domain flags. |
| `ip_artifacts` | Extracted IP addresses and private/public classification. |
| `visual_artifacts` | Images extracted from email bodies and attachments. |
| `qr_artifacts` | QR codes found in images, along with their decoded target URLs. |
| `risk_factors` | Individual risk rules triggered during scoring and their weight values. |
| `historical_matches` | Connections found between the current email and past cases. |
| `ai_grounded_assessments` | AI-generated technical narratives and MITRE ATT&CK technique tags. |

---

## Project Structure

```
email-forensics/
├── backend/
│   ├── src/main/java/com/emailforensics/backend/
│   │   ├── aiassessment/     # LLM summaries and MITRE ATT&CK mapping
│   │   ├── analysis/         # Consolidated analysis results entity and repository
│   │   ├── auth/             # Authentication endpoints and JWT logic
│   │   ├── casefile/         # Case management (CRUD operations)
│   │   ├── content/          # Tribuo ML, social engineering rules, prompt injection
│   │   ├── correlation/      # Cross-case matching engine
│   │   ├── email/            # Jakarta Mail MIME parser
│   │   ├── evidence/         # Upload handling, evidence vault, orchestration, SSE
│   │   ├── headers/          # Header checks, SPF/DKIM/DMARC, received hops
│   │   ├── health/           # Health checks and system reset/truncate tool
│   │   ├── infrastructure/   # IP classification and DNS lookups
│   │   ├── report/           # Dossier report builder and JSON export
│   │   ├── risk/             # Deterministic risk scoring and confidence rating
│   │   ├── security/         # Spring Security filters and UserDetailsService
│   │   ├── urlintel/         # URL extraction, heuristics, and normalization
│   │   └── visual/           # Image handling and ZXing QR decoding
│   └── src/main/resources/
│       ├── db/migration/     # Flyway SQL migrations (V1 through V14)
│       └── application.properties
├── frontend/
│   ├── src/
│   │   ├── api/              # Axios client functions organized by domain
│   │   ├── auth/             # AuthContext and login state handling
│   │   ├── components/       # Investigation tabs, report view, upload modal, navbar
│   │   └── pages/            # Login, Cases, Case Detail, Investigation, Health
│   ├── package.json
│   └── vite.config.js
└── test-fixtures/
    └── eml/                  # Sample .eml files for testing different forensic scenarios
```

---

## Prerequisites

Before running the application, make sure you have:

- **Java Development Kit (JDK) 21** or higher (`java -version`)
- **Node.js 20** or higher (`node -v` and `npm -v`)
- **PostgreSQL 16** running locally on port 5432 (`psql -U postgres`)

---

## Quick Start Guide

### 1. Create the Database

Open your PostgreSQL client (psql, pgAdmin, or DBeaver) and create the database:

```sql
CREATE DATABASE email_forensics;
```

*Note: The backend defaults to connecting with user `postgres` and an empty password on `localhost:5432`. If your local PostgreSQL uses a password, set `DB_PASSWORD` as shown in the configuration section below.*

### 2. Start the Backend

Open a terminal in the project directory:

```bash
# On Windows:
cd backend
.\mvnw.cmd spring-boot:run

# On Linux or macOS:
cd backend
./mvnw spring-boot:run
```

When started, Flyway automatically runs the database migrations (V1 to V14) and creates default user accounts. The API runs at `http://localhost:8080`.

**Default Login Credentials:**
- **Analyst:** `analyst` / `analyst123` (Role: `ANALYST`)
- **Admin:** `admin` / `admin123` (Role: `ADMIN`)

### 3. Start the Frontend

Open a second terminal window:

```bash
cd frontend
npm install
npm run dev
```

The frontend will run at `http://localhost:5173` and automatically proxies `/api` calls to the backend on port 8080.

---

## Using the Application

1. **Log In:** Go to `http://localhost:5173` and sign in with `analyst` / `analyst123`.
2. **Create a Case:** Click **"New Case"**, enter a case number (such as `CASE-2026-001`), add a title and description, choose a severity level, and submit.
3. **Upload an Evidence File:** Open your case, click **"Upload Evidence"**, and select any `.eml` file (you can pick one from the `test-fixtures/eml/` folder).
4. **Inspect the Findings:** Click **"Investigate"** on the uploaded email. The tabs provide details on:
   - **Overview:** SHA-256 checksum, file size, sender summary, and pipeline status.
   - **Headers:** `From`, `Reply-To`, `Return-Path`, and SPF/DKIM/DMARC alignment.
   - **Mail Route:** Chronological relay servers, transit latency, and hop IPs.
   - **URLs & Network:** Extracted links, anchor mismatches, and IP classifications.
   - **Visuals & QR:** Embedded images and decoded QR code targets.
   - **Content & ML:** Spam/phishing classification, social engineering cues, and prompt injection defense.
   - **Risk Scoring:** Transparent 0–100 score with an itemized table of risk reasons and weights.
   - **Correlations:** Links to any matching indicators found in earlier cases.
   - **AI Assessment:** High-level narrative summary and MITRE ATT&CK technique tags.
5. **Generate a Report:** Open the **"Forensic Dossier Report"** tab to view the formatted report, export it as a clean JSON file, or print/save it as a PDF.

---

## Sample Test Fixtures

The `test-fixtures/eml/` directory contains sample emails designed to test specific forensic checks:

| File | Scenario Tested | What to Expect |
|---|---|---|
| `01-plain-legitimate.eml` | Standard corporate email | Clean headers, passing authentication, Low Risk (0–10). |
| `02-html-anchor-mismatch.eml` | Phishing with spoofed link | Display text shows a trusted bank, but href links to a phishing site. Triggers High Risk factor. |
| `03-multipart-attachment.eml` | Multipart email with attachments | Extracts attachment metadata and SHA-256 payload hashes. |
| `04-unicode-subject.eml` | RFC 2047 internationalized subject | Verifies Unicode decoding and header normalization. |
| `05-malformed-mime.eml` | Corrupted header boundaries | Checks that the parser handles broken structures gracefully without crashing. |
| `06-received-chain.eml` | Multi-hop email route | Reconstructs 4 relay server hops with transit delay calculations. |
| `07-inline-qr-code.eml` | Quishing (QR code phishing) | Extracts the embedded image and decodes the QR URL. |
| `08-prompt-injection.eml` | LLM prompt injection attempt | Detects adversarial instructions designed to hijack LLM behavior and flags the attack. |

---

## System Maintenance and Testing Tools

### Service Status and Database Reset
When testing locally, you can reset test data at any time:
1. Open the **"Service Status"** page in the top navigation bar (`/health`).
2. Review backend services, database connectivity, and pipeline health.
3. Click the **"Reset All Data"** button to safely clear all uploaded evidence files from disk and truncate test tables while preserving user accounts and database structure.

### Running Tests

**Backend Test Suite:**
```bash
# Windows
cd backend
.\mvnw.cmd test

# Linux / macOS
cd backend
./mvnw test
```
All 108 unit and integration tests run against an in-memory/mocked setup without affecting your live database.

**Frontend Production Build Check:**
```bash
cd frontend
npm run build
```

---

## Configuration Reference

The application works out of the box with default settings. You can override properties using environment variables or a `.env` file in the `backend/` directory:

| Environment Variable | Default Value | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `email_forensics` | PostgreSQL database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | *(empty)* | Database password |
| `JWT_SECRET` | *(auto-generated)* | 256-bit secret key used to sign JWT tokens |
| `JWT_EXPIRATION_MS` | `86400000` (24 hours) | JWT token lifespan in milliseconds |
| `EVIDENCE_VAULT_DIR` | `./evidence_vault` | Directory where uploaded `.eml` files are saved |
| `GROQ_API_KEY` | *(empty)* | Optional Groq API key for AI summaries (falls back to local mock if empty) |
| `GROQ_MODEL` | `openai/gpt-oss-20b` | Model name for AI threat assessment |

---

## Contributing and License

This project is licensed under the MIT License. Contributions, bug reports, and suggestions are welcome via GitHub issues and pull requests.
