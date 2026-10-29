# AI-Powered Email Forensic Investigation & Threat Intelligence Platform

An enterprise-grade, academic-caliber digital forensics and threat intelligence platform designed to ingest raw `.eml` email files, preserve cryptographic evidence integrity (SHA-256 hash verification), extract deep header/transit/content indicators, detect adversarial prompt injection and quishing, correlate threats across historical cases, compute explainable risk scores, and produce court-admissible forensic dossier reports.

---

## Key Features & Capabilities

- **Cryptographic Chain of Custody:** Strict SHA-256 evidence hashing upon ingestion with continuous tamper-evident verification.
- **Deep Header Forensics & Spoofing Defense:** Full RFC 5322/2045/2047 MIME parsing, SPF / DKIM / DMARC authentication alignment, Return-Path / Reply-To mismatch detection, and display name spoofing analysis.
- **Mail Transit Route & Hop Reconstruction:** Chronological Received header traversal with hop-by-hop latency calculation, TLS cipher inspection, and delay anomaly flags.
- **URL & Web Infrastructure Intelligence:** Anchor text vs target mismatch detection, IP-based URLs, URL shortener resolution, lookalike/punycode homograph analysis, and defanged URL exports.
- **Network Telemetry & GeoIP Resolution:** Server IP extraction, private/bogon (RFC 1918) detection, MaxMind GeoIP resolution, ASN routing classification, and organization ownership.
- **Visual Artifacts & Quishing Defense:** Embedded image extraction and QR code detection/payload decoding via ZXing.
- **Content NLP & Prompt Injection Firewall:** Tribuo ML machine learning baseline classification, rule-based social engineering cue detection (urgency, credential harvesting, extortion, impersonation), and proactive prompt injection defense against LLM hijacking.
- **Explainable Deterministic Risk Scoring:** Rule-based scoring engine (0–100) with confidence calibration, weighted severity multipliers, and clear audit breakdowns.
- **Historical Threat Correlation & Campaign Engine:** Graph-based indicator linking (senders, domains, hashes, IPs), interactive force-directed Threat Graph canvas, and automated threat campaign clustering.
- **Grounded AI Forensic Intelligence:** Hallucination-shielded LLM synthesis generating grounded technical narratives, Mitre ATT&CK technique mapping, and strict citation validation.
- **Court-Ready Forensic Dossier & Reporting:** Multi-format report generation (interactive UI, clean JSON API, and high-contrast `@media print` PDF layout with analyst sign-off and chain of custody certification).

---

## Technology Stack

| Layer | Component | Version / Coordinate | Purpose |
|---|---|---|---|
| **Runtime** | OpenJDK | 21 LTS | High-performance backend runtime |
| **Backend Framework** | Spring Boot | 4.1.x / Spring 7 | Modular monolith architecture |
| **Security** | Spring Security & JJWT | 0.12.x | Stateless JWT authentication & RBAC |
| **Database** | PostgreSQL | 16 | Relational data store for cases and forensic telemetry |
| **Database Migrations** | Flyway | 10.x | Version-controlled database schema migrations (V1–V11) |
| **Email Parsing** | Jakarta Mail | 2.1.x | Robust RFC 5322 MIME stream parser |
| **Machine Learning** | Oracle Tribuo | 4.3.x | Embedded Naive Bayes NLP classification |
| **Visual Decoding** | ZXing | 3.5.x | QR barcode matrix decoding and payload routing |
| **Frontend Framework** | React | 19.x (JSX, No TS) | Reactive component UI |
| **Build & Bundler** | Vite | 8.x | Lightning-fast development & production bundling |
| **Styling** | Tailwind CSS | 4.x & Vanilla CSS | Slate/cyan dark theme & print stylesheets |
| **Icons & Graphs** | Lucide React & Canvas | Latest | Responsive iconography & custom HTML5 canvas graph |

---

## System Requirements & Prerequisites

Ensure the following tools are installed:
- **Java Development Kit (JDK):** Version 21 or higher (`java -version`)
- **Node.js:** Current LTS (v20.x or v22.x) (`node -v`, `npm -v`)
- **PostgreSQL:** Version 16 or compatible running on `localhost:5432` (`psql -version`)
- **Maven:** Bundled via `./mvnw` / `mvnw.cmd` wrapper (no standalone installation required)

---

## Installation & Setup Guide

### 1. Database Provisioning

Create the PostgreSQL database and dedicated user:

```sql
CREATE DATABASE email_forensics;
CREATE USER email_user WITH ENCRYPTED PASSWORD 'email_password';
GRANT ALL PRIVILEGES ON DATABASE email_forensics TO email_user;
```

*(Note: The database configuration defaults to `jdbc:postgresql://localhost:5432/email_forensics` with username `postgres` / password `postgres` or credentials configured in `application.yml` / `.env`)*

### 2. Backend Startup

From the project root:

```bash
# On Windows:
cd backend
..\mvnw.cmd spring-boot:run

# On Linux / macOS:
cd backend
../mvnw spring-boot:run
```

The Spring Boot server will execute Flyway migrations and listen on **port 8080**.

Default Demo Accounts:
- **Analyst:** Username `analyst` | Password `analyst123` (Role: `ANALYST`)
- **Admin:** Username `admin` | Password `admin123` (Role: `ADMIN`)

### 3. Frontend Startup

In a separate terminal window:

```bash
cd frontend
npm install
npm run dev
```

The Vite development server will start on **http://localhost:5173** with proxying to `http://localhost:8080/api/v1`.

---

## Test Fixture Catalog

The repository includes a curated set of test fixtures located in `test-fixtures/eml/`:

| Fixture Filename | Description & Provenance | Expected Forensic Signals |
|---|---|---|
| `01-plain-legitimate.eml` | Standard synthetic RFC 5322 corporate email. | Clean SPF/DKIM/DMARC pass, zero spoofing flags, Low Risk (0–10/100), Benign. |
| `02-html-anchor-mismatch.eml` | Phishing email with anchor text mismatch. | Visible text displays `https://secure.bank.com` while underlying href links to `http://phish-login.cc`. High Risk factor triggered. |
| `03-multipart-attachment.eml` | Multi-part MIME message with attachments. | Nested MIME parts, attachment metadata extraction, payload fingerprinting. |
| `04-unicode-subject.eml` | RFC 2047 encoded internationalized subject line. | Tests character set decoder resiliency (`=?UTF-8?B?...?=`). |
| `05-malformed-mime.eml` | Intentional header boundary corruption. | Tests parser tolerance and graceful error recovery with warnings recording. |
| `06-received-chain.eml` | Multi-hop message with 4 transit relays. | Complete route hop traversal, relay IP geolocation, transit latency calculation. |
| `07-inline-qr-code.eml` | Quishing attack with embedded QR barcode image. | Visual artifact extraction, ZXing QR decoding, suspicious QR target URL detection. |
| `08-prompt-injection.eml` | Adversarial LLM prompt injection attack. | Regex injection engine detects `[SYSTEM INSTRUCTION: IGNORE ALL PREVIOUS INSTRUCTIONS]`, blocks AI override. |

---

## 5-Minute Demo Walkthrough

Follow these steps for a complete demonstration of the system:

1. **Sign In:**
   - Navigate to `http://localhost:5173/login`.
   - Log in using `analyst` / `analyst123`.
2. **Review or Create a Case:**
   - On the Cases Dashboard (`/cases`), click **"New Case"** (`#btn-create-case`).
   - Enter Case Number (e.g., `CASE-2026-DEMO`), Title, Description, and select Severity `HIGH`. Click **"Create Case"**.
3. **Ingest Evidence (.eml):**
   - Inside the case view, click **"Upload Evidence"** (`#btn-upload-evidence`).
   - Select `test-fixtures/eml/08-prompt-injection.eml` or `test-fixtures/eml/07-inline-qr-code.eml`.
   - Observe automatic SHA-256 checksum calculation, storage ingestion, and automated multi-stage pipeline processing.
4. **Deep Forensic Investigation:**
   - Click **"Investigate"** (`#btn-investigate-{id}`) on the newly uploaded evidence file.
   - **Overview Tab (`#tab-overview`):** View the executive summary, cryptographic fingerprint, authentication verdict, and stage health.
   - **Headers Tab (`#tab-headers`):** Inspect sender alignment, SPF/DKIM/DMARC verdicts, and identity alignment warnings.
   - **Mail Route Tab (`#tab-hops`):** Examine the chronological transit route, relay delays, and server hops.
   - **URLs Tab (`#tab-urls`):** Inspect extracted links, anchor text mismatches, and domain flags.
   - **Visuals & QR Tab (`#tab-visuals`):** View extracted visual artifacts and decoded QR payloads.
   - **Content & ML Tab (`#tab-content`):** Review Tribuo ML classification, social engineering indicators, and Adversarial Injection Defense alerts.
   - **Risk Scoring Tab (`#tab-risk`):** Audit the explainable deterministic risk factors table with weight contributions.
   - **Correlations & Graph Tab (`#tab-correlations`):** Review cross-case correlations and click nodes on the interactive Threat Graph canvas.
   - **AI Grounded Assessment Tab (`#tab-ai`):** View the hallucination-shielded technical narrative and verified Mitre ATT&CK citations.
5. **Generate & Export Court-Ready Dossier:**
   - Click the **"Forensic Dossier Report"** tab (`#tab-report`).
   - Review the complete multi-page document: Cryptographic Chain of Custody, Executive Verdict, Stage Health, All Technical Findings, and Formal Examiner Sign-Off Block.
   - Click **"Export JSON Dossier"** (`#btn-report-tab-export-json`) to download the machine-readable forensic report.
   - Click **"Print / PDF Report"** (`#btn-report-tab-print`) to preview the court-ready, high-contrast printable dossier.

---

## Running Verification Tests

### Backend Unit & Integration Tests (98 Tests)

```bash
cd backend
..\mvnw.cmd test
```

Expected result:
```
[INFO] Results:
[INFO] Tests run: 98, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Frontend Production Build

```bash
cd frontend
npm run build
```

Expected result:
```
✓ built in ~1.0s
```

---

## Architectural Principles & Compliance

- **ISO/IEC 27037:2012 Evidentiary Standards:** Immutable raw artifact storage and SHA-256 integrity verification across the entire lifecycle.
- **Explainable Cyber Intelligence:** Zero black-box decisions; all deterministic scores are backed by weighted rules and audit citations.
- **Adversarial Resilience:** Defenses against prompt injection, Unicode obfuscation, and malformed MIME streams.
- **Zero-Dependency Export Serialization:** Forensic report JSON export operates with zero external dependencies to prevent parser vulnerabilities.
