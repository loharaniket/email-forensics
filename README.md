# Email Forensics Platform

An open-source, beginner-friendly cybersecurity platform for analyzing suspicious emails (`.eml` files). It helps you inspect email headers, verify sender authenticity, trace mail delivery routes, uncover deceptive links and hidden QR codes, detect phishing text, calculate a transparent risk score, and generate comprehensive forensic reports.

---

## Table of Contents

1. [What This Project Does](#what-this-project-does)
2. [Beginner's Guide to Email Security Terms](#beginners-guide-to-email-security-terms)
3. [Project Architecture](#project-architecture)
4. [Prerequisites](#prerequisites)
5. [Step-by-Step Setup Guide](#step-by-step-setup-guide)
   - [Step 1: Create the Database](#step-1-create-the-database)
   - [Step 2: Start the Backend Server](#step-2-start-the-backend-server)
   - [Step 3: Start the Web Dashboard](#step-3-start-the-web-dashboard)
   - [Step 4: Load the Chrome Extension (Optional)](#step-4-load-the-chrome-extension-optional)
6. [How to Get an .eml File](#how-to-get-an-eml-file)
7. [Testing with Sample Emails](#testing-with-sample-emails)
8. [Investigation Studio Views](#investigation-studio-views)
9. [Configuration Reference](#configuration-reference)
10. [REST API Reference](#rest-api-reference)
11. [Troubleshooting & FAQ](#troubleshooting--faq)
12. [License](#license)

---

## What This Project Does

When you receive a suspicious email, it can be dangerous to open links or download attachments. This platform lets you safely upload and analyze the raw email file in an isolated environment.

Key capabilities:

- **Sender Verification:** Validates SPF, DKIM, DMARC, and ARC authentication to check whether the email actually came from who claims to have sent it.
- **Delivery Route Tracking:** Reads all `Received:` mail server headers in order, measures transit delays between hops, and classifies server IP addresses.
- **Deceptive Link Detection:** Scans all web links in the email, flagging cases where the visible link text says one thing (e.g. `bank.com`) but the real destination leads somewhere else.
- **QR Code & Image Analysis (Quishing):** Automatically extracts embedded images and decodes hidden QR codes to reveal the destination web address.
- **Attachment Safety & Hashing:** Extracts file attachments and calculates their SHA-256 digital fingerprint so evidence can be safely audited.
- **Phishing & Scam Language Detection:** Scans message text for urgency cues, impersonation, credential theft attempts, and sneaky prompt injections designed to hijack AI systems.
- **Transparent Risk Score (0–100):** Combines all findings into a deterministic risk score (`LOW`, `MODERATE`, `HIGH`, `CRITICAL`) with an itemized breakdown explaining every point added.
- **Plain-English AI Summary:** Uses Groq AI (with an automatic offline fallback if no API key is provided) to explain the security verdict in simple terms.
- **Official Forensic Dossier:** Formats all technical findings into a clean report that can be reviewed online or printed/saved as a PDF.
- **Gmail Chrome Extension:** Adds an "Analyze Email" button directly into Gmail so you can investigate suspicious emails with a single click.

---

## Beginner's Guide to Email Security Terms

If you are new to email forensics, here is what the technical terms mean in everyday language:

| Technical Term | Everyday Meaning | How It Helps Protect You |
|---|---|---|
| **SPF** *(Sender Policy Framework)* | Approved Guest List | Checks if the mail server that sent the email is on the company's list of authorized servers. |
| **DKIM** *(DomainKeys Identified Mail)* | Tamper-Proof Wax Seal | A cryptographic digital signature proving that nobody altered the email after it was sent. |
| **DMARC** *(Domain-based Message Authentication)* | Company Security Policy | Tells email providers whether to block, quarantine, or deliver emails that fail SPF or DKIM checks. |
| **ARC** *(Authenticated Received Chain)* | Forwarding Stamp | Preserves authentication results when an email passes through intermediaries like mailing lists. |
| **MTA Hops** *(Mail Transfer Agent)* | Server Travel Stops | The list of mail servers the email hopped through on its journey from the sender to your inbox. |
| **Anchor Mismatch** | Deceptive Link | A phishing trick where the visible text says `paypal.com` but the link actually takes you to `evil-scam.com`. |
| **Quishing** | QR Code Phishing | A scam where attackers hide malicious website links inside a QR code to bypass link filters. |
| **Prompt Injection** | AI Hijack Instruction | Sneaky text hidden in an email that attempts to trick AI assistants into ignoring their safety rules. |
| **SHA-256 Hash** | Digital Fingerprint | A unique 64-character code proving the email file has remained untouched since you uploaded it. |

---

## Project Architecture

The repository is organized into three main modules:

```
email-forensics/
├── backend/                  # Java 21 & Spring Boot 4 Application
│   ├── src/main/java/        # Forensic pipeline services & REST controllers
│   ├── src/main/resources/   # Flyway database migrations (V1 to V15)
│   └── pom.xml               # Maven configuration
├── frontend/                 # React 19 & Vite 8 Web App
│   ├── src/pages/            # Dashboard, Investigation Studio, and Health pages
│   ├── src/components/       # Modular UI tabs, cards, and report viewer
│   └── package.json          # Node dependencies and scripts
├── extension/                # Chrome Extension (Manifest V3)
│   ├── background.js         # Service worker communicating with Gmail API and backend
│   ├── content.js            # Injects the "Analyze Email" button and side panel into Gmail
│   ├── popup.html            # Toolbar status popup
│   └── options/              # Extension settings page
└── test-fixtures/
    └── eml/                  # 8 realistic sample .eml files for testing
```

---

## Prerequisites

Before starting, ensure you have the following installed on your machine:

1. **Java Development Kit (JDK) 21** or higher:
   ```bash
   java -version
   ```
2. **Node.js 20** or higher (with npm):
   ```bash
   node -v
   npm -v
   ```
3. **PostgreSQL 16** (running locally on port `5432`):
   ```bash
   psql --version
   ```
4. **Google Chrome** or **Brave Browser** *(optional, only if using the Chrome extension)*.

---

## Step-by-Step Setup Guide

### Step 1: Create the Database

Open your PostgreSQL command line (`psql`) or a database management tool (like pgAdmin or DBeaver), and create a new database:

```sql
CREATE DATABASE email_forensics;
```

> **Note on Database Credentials:** By default, the backend connects to `localhost:5432` with username `postgres` and an empty password. If your local PostgreSQL has a password, configure it in `backend/.env` (see [Configuration Reference](#configuration-reference)).

---

### Step 2: Start the Backend Server

Open a terminal in the project root directory and navigate to the `backend` folder:

```bash
# On Windows:
cd backend
.\mvnw.cmd spring-boot:run

# On Linux or macOS:
cd backend
./mvnw spring-boot:run
```

When the server starts:
- Flyway automatically runs database migrations and creates all 11 required tables.
- The evidence vault directory (`./evidence_vault`) is automatically created.
- The server starts listening at: `http://localhost:8080`

To verify the backend is running, open:
`http://localhost:8080/api/v1/health`

You should receive the response: `{"status":"ok"}`.

---

### Step 3: Start the Web Dashboard

Open a **second terminal** window, navigate to the `frontend` folder, install dependencies, and start the development server:

```bash
cd frontend
npm install
npm run dev
```

The web dashboard is now live at:
`http://localhost:5173`

Open this URL in your web browser. You will see the main Dashboard where you can upload emails and review previous cases.

---

### Step 4: Load the Chrome Extension (Optional)

The Chrome extension lets you analyze emails directly inside Gmail without leaving your inbox.

1. Open **Google Chrome** or **Brave** and navigate to:
   `chrome://extensions` (or `brave://extensions`)
2. In the top-right corner, switch **Developer mode** to **ON**.
3. In the top-left corner, click **Load unpacked**.
4. In the file picker, select the `extension` folder inside this repository.
5. The extension is now installed. When you open an email in Gmail, an **Analyze Email** button will appear in the top action bar.

---

## How to Get an .eml File

To test the platform with your own emails, you need to download an `.eml` file from your email provider:

- **From Gmail:**
  1. Open the email message in your browser.
  2. Click the **three vertical dots** (`...`) next to the Reply button in the top right.
  3. Select **Download message**. An `.eml` file will be saved to your computer.

- **From Outlook (Web):**
  1. Open the email message.
  2. Click the **three dots** (`...`) at the top right of the message pane.
  3. Select **Save** or **Download original message**.

- **From Apple Mail (macOS):**
  1. Select the email in your inbox.
  2. Go to **File** > **Save As...**
  3. Choose **Raw Message Source** as the format and save.

---

## Testing with Sample Emails

The repository includes 8 sample `.eml` files in `test-fixtures/eml/` that cover real-world threats and edge cases. You can upload any of these via the web dashboard:

| File Name | Primary Scenario Tested | Expected Result |
|---|---|---|
| `01-plain-legitimate.eml` | Clean, standard business email with passing authentication. | **Low Risk** — SPF, DKIM, and DMARC all pass cleanly. |
| `02-html-anchor-mismatch.eml` | Deceptive link: text displays a trusted bank, but URL points to a fake domain. | **High Risk** — Link mismatch flagged and highlighted in red. |
| `03-multipart-attachment.eml` | Email containing binary file attachments. | Extracts files, computes SHA-256 hashes, and identifies MIME types. |
| `04-unicode-subject.eml` | Internationalized headers with RFC 2047 encoded subject text. | Verifies correct Unicode decoding without corrupted text. |
| `05-malformed-mime.eml` | Corrupt or invalid MIME boundaries and headers. | Verifies robust error handling without pipeline crashes. |
| `06-received-chain.eml` | Multi-hop email that passed through 4 different mail servers. | Parses all 4 transit hops, extracts server IPs, and calculates transit delay. |
| `07-inline-qr-code.eml` | Quishing attack hiding a phishing link inside an embedded image. | Detects the QR code, extracts the image, and decodes the embedded URL. |
| `08-prompt-injection.eml` | Adversarial text instructions designed to trick LLMs into ignoring security rules. | Flags prompt injection markers in the content signals view. |

---

## Investigation Studio Views

When you open any analyzed email, the **Investigation Studio** presents findings in an organized workspace with 11 dedicated views:

### 1. Overview
- **Summary:** Top-level verdict, risk score badge, evidence file metadata (size, upload date, SHA-256 checksum), sender identity, and threat indicators.
- **Risk Score:** Transparent 0–100 score breakdown showing each category's contribution (Authentication, Route, Links, Attachments, Content) and an itemized table of every triggered rule.
- **AI Assessment:** Plain-English threat analysis generated by the AI model, summarizing the primary danger and recommending concrete defensive actions.

### 2. Evidence Channels
- **Sender Checks:** SPF, DKIM, DMARC, and ARC verification statuses, plus identity alignment checks between the visible `From:` header and technical envelope addresses (`Return-Path`, `Reply-To`).
- **Mail Route:** Step-by-step chronology of every mail transfer agent (MTA) server the email touched, including hop order, hostnames, IP addresses, and transit latency in seconds.
- **Links:** Complete list of all URLs extracted from the email body, with immediate flagging of deceptive link text and dangerous destinations.
- **IP Addresses:** All public and private IP addresses identified across headers and body content, including geographical and network routing flags.
- **Attachments:** All attached files extracted from the email, showing file names, MIME content types, exact byte sizes, and SHA-256 checksums.
- **QR Codes:** Decoded QR codes discovered inside inline images or attachments, displaying the target URL and decoding confidence.
- **Content Signals:** Scans message text for urgency phrases, financial manipulation, credential requests, impersonation cues, and prompt injection attempts.
- **Message Text:** Clean, sanitized view of the raw text and HTML body content of the email.

### 3. Analysis & Report
- **Correlations:** Matches the current email against previously analyzed cases in your database (e.g. matching originating IPs, sender domains, or subject patterns).
- **Full Report:** Formal forensic dossier formatted for printing or exporting directly to PDF.

---

## Configuration Reference

The application runs out of the box with zero configuration required. If you wish to customize ports, database credentials, or AI settings, you can create a `.env` file in the `backend/` directory:

| Environment Variable | Default Value | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host address |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `email_forensics` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | *(empty)* | Database password |
| `EVIDENCE_VAULT_DIR` | `./evidence_vault` | Local directory where uploaded raw `.eml` files are stored |
| `GROQ_API_KEY` | *(empty)* | Optional Groq API key for live AI threat summaries |
| `GROQ_MODEL` | `openai/gpt-oss-20b` | Model used for AI assessments |
| `GROQ_BASE_URL` | `https://api.groq.com/openai/v1` | Base API URL for AI requests |
| `SPRING_AI_TIMEOUT_SECONDS` | `25` | Timeout in seconds for AI generation requests |

> **Offline Mode Note:** If no `GROQ_API_KEY` is provided, the application will automatically fall back to an offline rule-based threat summary. You do **not** need an API key to use any of the forensic features.

### Upload Limits
- **Maximum file upload size:** 25 MB
- **Maximum multipart request size:** 30 MB

---

## REST API Reference

For developers and automated workflows, the backend exposes standard REST endpoints under `/api/v1`:

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/evidence/upload` | Upload a new `.eml` file (`multipart/form-data`) |
| `GET` | `/api/v1/evidence` | Get a paginated list of all investigated email cases |
| `GET` | `/api/v1/evidence/{emailId}/status` | Get current pipeline processing status (`QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`) |
| `GET` | `/api/v1/evidence/{emailId}/investigation` | Get complete investigation details, metadata, and findings |
| `GET` | `/api/v1/evidence/{emailId}/headers` | Get SPF, DKIM, DMARC, and ARC authentication results |
| `GET` | `/api/v1/evidence/{emailId}/hops` | Get list of MTA transit relay hops with latency timings |
| `GET` | `/api/v1/evidence/{emailId}/urls` | Get all extracted URLs and deceptive anchor text mismatches |
| `GET` | `/api/v1/evidence/{emailId}/infrastructure`| Get extracted IP addresses and routing classifications |
| `GET` | `/api/v1/evidence/{emailId}/attachments` | Get metadata and SHA-256 hashes of attached files |
| `GET` | `/api/v1/evidence/{emailId}/qr` | Get decoded QR code payloads and images |
| `GET` | `/api/v1/evidence/{emailId}/content-analysis` | Get phishing keyword signals and prompt injection flags |
| `GET` | `/api/v1/evidence/{emailId}/risk-breakdown` | Get itemized 0–100 risk score calculation and triggered rules |
| `GET` | `/api/v1/evidence/{emailId}/correlations` | Get matching indicators from historical cases |
| `GET` | `/api/v1/evidence/{emailId}/integrity` | Verify evidence integrity against original stored SHA-256 hash |
| `GET` | `/api/v1/evidence/{emailId}/original` | Download the preserved RFC 5322 `.eml` file |
| `POST` | `/api/v1/evidence/{emailId}/retry` | Restart the forensic analysis pipeline for this email |
| `GET` | `/api/v1/evidence/{emailId}/ai-assessment` | Get the latest AI threat summary |
| `POST` | `/api/v1/evidence/{emailId}/ai-assessment/generate` | Generate or regenerate the AI threat summary |
| `GET` | `/api/v1/evidence/{emailId}/report` | Get structured forensic report data for printing / PDF |
| `GET` | `/api/v1/health` | Health check endpoint (returns `{"status":"ok"}`) |
| `POST` | `/api/v1/system/truncate-all` | Reset test data: safely clear database tables and evidence vault |

---

## Troubleshooting & FAQ

### 1. PostgreSQL connection error: "Connection refused" or "password authentication failed"
- Make sure your PostgreSQL service is running:
  - On Windows: check Windows Services (`services.msc`) for `postgresql-x64-16`.
  - On Linux/macOS: run `sudo systemctl status postgresql` or `brew services list`.
- Check if your PostgreSQL user has a password. If it does, create `backend/.env` and add:
  ```env
  DB_PASSWORD=your_actual_password
  ```

### 2. Port already in use (Port 8080 or Port 5173)
- If port `8080` is in use by another app, you can change the backend port in `backend/src/main/resources/application.properties` by setting `server.port=8081`.
- If port `5173` is in use, Vite will automatically offer the next available port (e.g. `5174`).

### 3. Do I need to buy an AI API key to use this tool?
No. All core forensics—including SPF, DKIM, DMARC, mail hops, link extraction, QR code decoding, phishing keywords, and the 0–100 risk score—run locally and deterministically. If you do not provide a `GROQ_API_KEY`, the application automatically uses a built-in offline summary generator.

### 4. How do I reset or wipe test cases to start fresh?
You can reset all data at any time:
- In the web app: navigate to `http://localhost:5173/health` and click **Reset All Data**.
- Via terminal / curl:
  ```bash
  curl -X POST http://localhost:8080/api/v1/system/truncate-all
  ```
This safely removes uploaded files from the evidence vault and truncates test tables while keeping your database schema intact.

---

## License

This project is open-source and licensed under the [MIT License](LICENSE).
