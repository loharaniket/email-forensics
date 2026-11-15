# Antigravity AI Agent Setup Guide - Email Forensics Version 2

This guide explains the complete AI agent instruction system configured for the Email Forensics Version 2 project.

Consult the authoritative specification in [docs/architecture-v2.md](../architecture-v2.md).

All explanations use very simple English so that any beginner developer can easily follow and use this setup.

---

## 1. Why Was This Setup Created?

When developing with an AI assistant in Antigravity, the AI needs clear instructions so it does not:
- Write overly complicated or confusing code.
- Add code comments that clutter the source files.
- Use advanced Spring Boot features (like Lombok or complex generic classes) that make learning harder.
- Accidentally modify or break the stable Version 1 code on the `main` branch.
- Execute unauthorized `git commit`, `git push`, or automatic branch-switch commands.
- Copy legacy Version 1 frontend code instead of building clean Version 2 components.
- Introduce bloated enterprise systems (like Kafka, Redis, or microservices) into a simple MVP.
- Compromise security by visiting malicious links or sending confidential email text to cloud AI.

This setup teaches Antigravity your project rules, your coding preferences, your architecture, and your design standards before a single line of application code is written.

---

## 2. Protection of README.md

`README.md` is currently managed manually by the repository owner.
- AI agents must **NOT** read, open, modify, rewrite, or use `README.md` as the authoritative architecture reference until the owner explicitly updates it.
- The **ONLY** authoritative Version 2 technical reference is [docs/architecture-v2.md](../architecture-v2.md).

---

## 3. Complete File and Folder Structure

```
email-forensics/
│
├── AGENTS.md                                   # Root master instructions (always loaded)
│
├── .agents/
│   ├── hooks.json                              # Command-blocking PreToolUse lifecycle hook (blocks git write/switch/push)
│   ├── scripts/
│   │   ├── block-git-commits.mjs               # Node.js script enforcing the manual git policy
│   │   └── test-git-hook.mjs                   # Internal automated test suite for the git safety gate
│   │
│   ├── rules/                                  # Project rule files with triggers
│   │   ├── development-rules.md                # Always-on: Beginner code, simple names, no comments
│   │   ├── git-rules.md                        # Always-on: Absolute manual commit policy, no auto branch switch
│   │   ├── mvp-rules.md                        # Always-on: Avoid enterprise bloat, simple MVP only
│   │   ├── email-security-rules.md             # Always-on: Untrusted inputs, hash integrity, PII redaction
│   │   ├── spring-boot-rules.md                # Model-decision: Spring Boot rules, no Lombok, basic annotations
│   │   └── frontend-rules.md                   # Model-decision: V1 visual reference only, React + Vite
│   │
│   ├── skills/                                 # Progressive disclosure runbooks
│   │   ├── project-understanding/SKILL.md      # 10-step pre-development project check
│   │   ├── spring-boot-beginner/SKILL.md       # Spring Boot class templates and beginner patterns
│   │   ├── frontend-reference/SKILL.md         # Replicating V1 UI styling with new V2 components
│   │   ├── email-forensics/SKILL.md            # The 20-step forensic pipeline and risk formula
│   │   ├── database-development/SKILL.md       # PostgreSQL schema (2 tables), JSONB, correlation SQL
│   │   ├── testing-and-debugging/SKILL.md      # Testing guides, safe test fixtures, mocking providers
│   │   └── code-review/SKILL.md                # 12-point pre-completion review checklist
│   │
│   └── agents/                                 # Specialized agent profiles
│       ├── project-planner/agent.md            # Plans features and checks architecture (read-only)
│       ├── spring-boot-developer/agent.md      # Implements backend Java code and tests
│       ├── frontend-developer/agent.md         # Implements React UI and components
│       └── code-reviewer/agent.md              # Audits code for simplicity and compliance (read-only)
│
└── docs/
    ├── architecture-v2.md                      # Single authoritative Version 2 architecture document
    └── ai/
        ├── agent-setup-guide.md                # This setup and operating guide
        └── frontend-reference-guide.md         # Visual design tokens and UI component catalog
```

---

## 4. Specialized Agents and Supported Tool Configurations

All custom agent files use supported Antigravity tool names, explicit permissions, and `inheritCustomizations: true`:

| Agent | Model | Tools | Read/Write Role |
| :--- | :--- | :--- | :--- |
| **`project-planner`** | `flash` | `[view_file, grep_search, find_by_name, list_dir, search_web, send_message]` | **Read-Only:** Inspects architecture and prepares implementation plans. No file-editing tools. |
| **`spring-boot-developer`** | `flash` | `[view_file, write_to_file, replace_file_content, multi_replace_file_content, grep_search, find_by_name, list_dir, run_command, manage_task, search_web, send_message]` | **Read/Write:** Implements simple Java backend features, runs tests, explains data flow. |
| **`frontend-developer`** | `flash` | `[view_file, write_to_file, replace_file_content, multi_replace_file_content, grep_search, find_by_name, list_dir, run_command, manage_task, search_web, send_message]` | **Read/Write:** Builds brand-new React components using V1 visual design tokens. |
| **`code-reviewer`** | `flash` | `[view_file, grep_search, find_by_name, list_dir, search_web, send_message]` | **Read-Only:** Audits code against beginner simplicity, zero code comments, no Lombok. Cannot claim tests passed without execution logs. |

---

## 5. Absolute Git Safety Gate

The repository owner manually reviews, stages, commits, and pushes all code.

### Technical Hook Enforcement
Configured via `.agents/hooks.json` and executed via `.agents/scripts/block-git-commits.mjs`:
- Intercepts `run_command` and `github_*` MCP tool calls before execution.
- Automatically denies:
  - `git commit`
  - `git push` (including `--force`)
  - `git reset` (including `--hard`)
  - `git clean`
  - `git checkout .` or `git restore .`
  - `git switch` or `git checkout <branch>` (agents must never switch branches automatically)
  - GitHub CLI write commands (`gh pr create`, `gh repo`, etc.)
  - GitHub MCP write tools (`github_create_pull_request`, `github_create_commit`, etc.)
- Safely permits read-only inspection commands:
  - `git status`
  - `git diff`
  - `git log`
  - `git branch --show-current`
  - `git show`
- Normal project commands (`npm test`, `mvn test`, `dir`, `node`) run without restriction.

---

## 6. Standard Indicator Origins Across Backend and Database

The PostgreSQL database `indicators.origin` column enforces:
```sql
CONSTRAINT chk_indicator_origin
    CHECK (origin IN ('HEADER', 'BODY', 'HTML', 'DNS', 'IMAGE_OCR', 'QR'))
```
- `HEADER`: Extracted from RFC 5322 headers (From/Reply-To domains, Received hops, relay IPs).
- `BODY`: Extracted from plain-text email bodies.
- `HTML`: Extracted from HTML anchor hrefs, image tags, or form actions.
- `DNS`: Discovered through domain DNS lookups (A/AAAA/MX records).
- `IMAGE_OCR`: Extracted as visible text from images by local Tesseract OCR.
- `QR`: Decoded destination URLs from QR codes by ZXing.

All frontend components, backend entities, and DTOs use this exact origin vocabulary.

---

## 7. How to Build Version 2 Phase by Phase

Build the application one phase at a time following the roadmap:

### Phase 1: Project Foundation
- **Goal:** Set up Spring Boot backend, React/Vite frontend, connect to PostgreSQL, and verify health endpoint.
- **Example Prompt:**
  > "Read docs/architecture-v2.md and use the project-understanding skill. Work strictly on the development branch. Create the Spring Boot backend and React/Vite frontend foundation. Configure the two PostgreSQL tables (emails and indicators). Add a GET /api/health endpoint. Do not add Spring Security, JWT, or extra tables. Show me the created files and test results."

### Phase 2: EML Upload and Evidence Preservation
- **Goal:** Build file upload card, server-side `.eml` validation, SHA-256 hashing, and saving to `storage/emails/<uuid>/original.eml`.
- **Example Prompt:**
  > "Read docs/architecture-v2.md and use spring-boot-beginner and email-forensics skills. Implement Phase 2: EML upload endpoint (POST /api/emails/analyze), SHA-256 calculation with Java MessageDigest, and safe storage under a UUID path. Save a row to the emails table with status RECEIVED. Add unit tests verifying the hash. Do not add external APIs yet."

### Phase 3: Email Parser
- **Goal:** Integrate Jakarta Mail / Angus Mail and Jsoup to parse headers, Received hops, body text, HTML, and image parts.
- **Example Prompt:**
  > "Implement Phase 3: Email parsing with Angus Mail and Jsoup. Extract From, Reply-To, Subject, Date, Message-ID, Received headers, plain text, and HTML links. Test against plain text and HTML fixtures. Remember: zero code comments and no Lombok."

### Phase 4: Mail Authentication (SPF, DKIM, DMARC, ARC)
- **Goal:** Integrate Node.js 22.19.0+ `mailauth` helper and execute via `ProcessBuilder`.
- **Example Prompt:**
  > "Implement Phase 4: Node.js mailauth helper in tools/mailauth/verify.mjs using Node.js 22.19.0+. Call the helper from AuthenticationService in Java with a process timeout. Store normalized SPF, DKIM, DMARC, and ARC results in emails.auth_data. Never convert NOT_VERIFIABLE into PASS or FAIL."

### Phase 5: URL, Domain, and IP Extraction
- **Goal:** Extract unique URLs, hostnames, and public IPs, storing them in the `indicators` table.
- **Example Prompt:**
  > "Implement Phase 5: Extract indicators from parsed email headers, text, and HTML. Normalize URLs and domains. Extract public IPs and skip private RFC 1918 ranges. Save rows to the indicators table using standard origins (HEADER, BODY, HTML, DNS)."

### Phase 6: Threat Intelligence Providers
- **Goal:** Implement one provider at a time (Safe Browsing v5, PhishTank, OpenPhish feed, URLhaus, ThreatFox, AbuseIPDB, IPinfo Lite).
- **Example Prompt:**
  > "Implement SafeBrowsingClient and PhishTankClient in com.emailforensics.provider. Use Spring RestClient with connection and read timeouts. Normalize provider responses into ProviderObservation objects with MATCH, NO_MATCH, ERROR, or UNKNOWN. Mock provider calls with WireMock in tests."

### Phase 7: Image OCR and QR Decoding
- **Goal:** Run local Tesseract OCR on images and decode QR codes with ZXing.
- **Example Prompt:**
  > "Implement Phase 7: Extract inline/attached images. Run local Tesseract to extract text and ZXing to decode QR URLs. Feed decoded QR URLs into the URL threat pipeline with origin QR."

### Phase 8: PII Redaction and Gemini AI Analysis
- **Goal:** Redact sensitive data, show preview to user, and send approved text to Gemini using Spring AI.
- **Example Prompt:**
  > "Implement Phase 8: PII redaction for names, email usernames, phone numbers, and credentials. Present the redaction preview in the UI and pause for approval (WAITING_FOR_AI_APPROVAL). Upon approval, call Gemini via Spring AI ChatClient to receive the structured AiAssessment record."

### Phase 9: Historical Correlation
- **Goal:** Query the `indicators` table to find past emails sharing matching indicators.
- **Example Prompt:**
  > "Implement Phase 9: Historical correlation using the indicator grouping SQL query. Return matching emails in a clean table. Exclude the current email from its own history."

### Phase 10: Risk Scoring and PDF Report
- **Goal:** Calculate explainable risk score (0-100) and generate PDF report with Apache PDFBox.
- **Example Prompt:**
  > "Implement Phase 10: RiskScoreService using the provisional weighting formula (+40, +40, +15, +15, +10, +5, +10). Build PDFBox report generator for downloading forensic summaries."

### Phase 11: End-to-End Testing and Verification
- **Goal:** Test full pipeline from upload to report download using sample fixtures.
- **Example Prompt:**
  > "Run end-to-end verification. Check all unit tests, run the frontend, test a full email analysis with a fixture EML file, verify the risk score breakdown, and inspect the generated PDF."
