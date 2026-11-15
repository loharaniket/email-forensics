# Antigravity AI Agent Setup Guide - Email Forensics Version 2

This guide explains the complete AI agent instruction system configured for the Email Forensics Version 2 project.

All explanations use very simple English so that any beginner developer can easily follow and use this setup.

---

## 1. Why Was This Setup Created?

When developing with an AI assistant in Antigravity, the AI needs clear instructions so it does not:
- Write overly complicated or confusing code.
- Add code comments that clutter the source files.
- Use advanced Spring Boot features (like Lombok or complex generic classes) that make learning harder.
- Accidentally modify or break the stable Version 1 code on the `main` branch.
- Introduce bloated enterprise systems (like Kafka, Redis, or microservices) into a simple MVP.
- Compromise security by visiting malicious links or sending confidential email text to cloud AI.

This setup teaches Antigravity your project rules, your coding preferences, your architecture, and your design standards before a single line of application code is written.

---

## 2. Complete File and Folder Structure

```
email-forensics/
│
├── AGENTS.md                                   # Root master instructions (always loaded)
│
├── .agents/
│   │
│   ├── rules/                                  # Project rule files with triggers
│   │   ├── development-rules.md                # Always-on: Beginner code, simple names, no comments
│   │   ├── git-rules.md                        # Always-on: Never touch main, work on development
│   │   ├── mvp-rules.md                        # Always-on: Avoid enterprise bloat, simple MVP only
│   │   ├── email-security-rules.md             # Always-on: Untrusted inputs, hash integrity, PII redaction
│   │   ├── spring-boot-rules.md                # Model-decision: Spring Boot rules, no Lombok, basic annotations
│   │   └── frontend-rules.md                   # Model-decision: V1 visual identity, React + Vite, native fetch
│   │
│   ├── skills/                                 # Progressive disclosure runbooks
│   │   ├── project-understanding/SKILL.md      # 10-step pre-development project check
│   │   ├── spring-boot-beginner/SKILL.md       # Spring Boot class templates and beginner patterns
│   │   ├── frontend-reference/SKILL.md         # Replicating V1 UI, layout, and API client patterns
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
    └── ai/
        ├── agent-setup-guide.md                # This setup and operating guide
        └── frontend-reference-guide.md         # Visual design tokens and UI component catalog
```

---

## 3. How Antigravity Discovers and Uses These Files

Antigravity uses a system called **hierarchical discovery** and **progressive disclosure**:

1. **Root `AGENTS.md`:** Loaded automatically on every turn. It provides permanent project-wide constraints.
2. **Rules (`.agents/rules/`):**
   - **`always_on`:** Rules marked with `trigger: always_on` (`development-rules.md`, `git-rules.md`, `mvp-rules.md`, `email-security-rules.md`) are automatically included in the agent's context budget.
   - **`model_decision`:** Rules marked with `trigger: model_decision` (`spring-boot-rules.md`, `frontend-rules.md`) are automatically brought into the conversation when the user or task mentions backend or frontend development.
3. **Skills (`.agents/skills/`):**
   - The agent only sees the skill name and description initially.
   - When a specific task matches a skill's description (for example, parsing an email or writing a database query), Antigravity loads the full `SKILL.md` file on demand. This saves context tokens while providing deep, exact instructions.
4. **Agents (`.agents/agents/`):**
   - Defines four distinct specialist roles with clear permissions, model tiers (`flash`), and tool grants.
   - The coordinator agent can delegate subtasks to these roles or use their system prompts during feature development.

---

## 4. Summary of the 6 Project Rules

| Rule File | Trigger | Purpose |
| :--- | :--- | :--- |
| **`development-rules.md`** | `always_on` | Enforces simple names, small methods, single responsibility, and **strictly zero comments** in code. |
| **`git-rules.md`** | `always_on` | Locks `main` branch, enforces all work on `development`, bans destructive git commands. |
| **`mvp-rules.md`** | `always_on` | Prevents enterprise overengineering (no Kafka, Redis, microservices, or Spring Security). |
| **`email-security-rules.md`** | `always_on` | Enforces evidence preservation, SHA-256 hashing, safe UUID paths, and PII redaction. |
| **`spring-boot-rules.md`** | `model_decision` | Enforces standard Java without Lombok, constructor injection, and simple layers. |
| **`frontend-rules.md`** | `model_decision` | Enforces copying the Version 1 visual style, dark theme, and using native `fetch()`. |

---

## 5. Summary of the 7 Available Skills

1. **`project-understanding`:** Use before every task to verify the git branch, read documentation, and locate existing files.
2. **`spring-boot-beginner`:** Provides standard Java entity, repository, service, and controller templates written for a beginner.
3. **`frontend-reference`:** Details how to construct React pages and components matching Version 1 design tokens.
4. **`email-forensics`:** Outlines the 20-step forensic analysis pipeline, threat intelligence lookups, and risk scoring formula.
5. **`database-development`:** Documents the exact two-table schema (`emails` and `indicators`), JSONB usage, and historical correlation query.
6. **`testing-and-debugging`:** Explains how to run tests, write safe `.eml` fixtures without real malware, and mock external APIs.
7. **`code-review`:** 12-point quality checklist to run before declaring any task complete.

---

## 6. How to Build Version 2 Phase by Phase

Do not ask Antigravity to build the entire system in one prompt. Build the application one phase at a time following the roadmap:

### Phase 1: Project Foundation
- **Goal:** Set up Spring Boot backend, React/Vite frontend, connect to PostgreSQL, and verify health endpoint.
- **Example Prompt:**
  > "Read AGENTS.md and use the project-understanding skill. Work strictly on the development branch. Create the Spring Boot backend and React/Vite frontend foundation. Configure the two PostgreSQL tables (emails and indicators). Add a GET /api/health endpoint. Do not add Spring Security, JWT, or extra tables. Show me the created files and test results."

### Phase 2: EML Upload and Evidence Preservation
- **Goal:** Build file upload card, server-side `.eml` validation, SHA-256 hashing, and saving to `storage/emails/<uuid>/original.eml`.
- **Example Prompt:**
  > "Read AGENTS.md and use spring-boot-beginner and email-forensics skills. Implement Phase 2: EML upload endpoint (POST /api/emails/analyze), SHA-256 calculation with Java MessageDigest, and safe storage under a UUID path. Save a row to the emails table with status RECEIVED. Add unit tests verifying the hash. Do not add external APIs yet."

### Phase 3: Email Parser
- **Goal:** Integrate Jakarta Mail / Angus Mail and Jsoup to parse headers, Received hops, body text, HTML, and image parts.
- **Example Prompt:**
  > "Implement Phase 3: Email parsing with Angus Mail and Jsoup. Extract From, Reply-To, Subject, Date, Message-ID, Received headers, plain text, and HTML links. Test against plain text and HTML fixtures. Remember: zero code comments and no Lombok."

### Phase 4: Mail Authentication (SPF, DKIM, DMARC, ARC)
- **Goal:** Integrate Node.js 22+ `mailauth` helper and execute via `ProcessBuilder`.
- **Example Prompt:**
  > "Implement Phase 4: Node.js mailauth helper in tools/mailauth/verify.mjs. Call the helper from AuthenticationService in Java with a process timeout. Store normalized SPF, DKIM, DMARC, and ARC results in emails.auth_data. Never convert NOT_VERIFIABLE into PASS or FAIL."

### Phase 5: URL, Domain, and IP Extraction
- **Goal:** Extract unique URLs, hostnames, and public IPs, storing them in the `indicators` table.
- **Example Prompt:**
  > "Implement Phase 5: Extract indicators from parsed email headers, text, and HTML. Normalize URLs and domains. Extract public IPs and skip private RFC 1918 ranges. Save rows to the indicators table."

### Phase 6: Threat Intelligence Providers
- **Goal:** Implement one provider at a time (Safe Browsing v5, PhishTank, OpenPhish feed, URLhaus, ThreatFox, AbuseIPDB, IPinfo Lite).
- **Example Prompt:**
  > "Implement SafeBrowsingClient and PhishTankClient in com.emailforensics.provider. Use Spring RestClient with connection and read timeouts. Normalize provider responses into ProviderObservation objects. Mock provider calls with WireMock in tests."

### Phase 7: Image OCR and QR Decoding
- **Goal:** Run local Tesseract OCR on images and decode QR codes with ZXing.
- **Example Prompt:**
  > "Implement Phase 7: Extract inline/attached images. Run local Tesseract to extract text and ZXing to decode QR URLs. Feed decoded QR URLs into the URL threat pipeline."

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
  > "Implement Phase 10: RiskScoreService using the approved weighting formula (+40, +40, +15, +15, +10, +5, +10). Build PDFBox report generator for downloading forensic summaries."

### Phase 11: End-to-End Testing and Verification
- **Goal:** Test full pipeline from upload to report download using sample fixtures.
- **Example Prompt:**
  > "Run end-to-end verification. Check all unit tests, run the frontend, test a full email analysis with a fixture EML file, verify the risk score breakdown, and inspect the generated PDF."

---

## 7. How to Verify That Antigravity Has Discovered the Setup

You can confirm that Antigravity recognizes your configuration at any time:
1. In the chat interface, type `@` to see that rules and workspace files are discoverable.
2. Ask Antigravity: *"What are the project rules for code comments and Lombok?"* - Antigravity will immediately quote `AGENTS.md` and `development-rules.md`.
3. Ask Antigravity: *"What tables exist in the database?"* - Antigravity will refer to `database-development` skill and confirm only `emails` and `indicators` exist.
