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
- Execute unauthorized `git commit` or `git push` commands.
- Copy legacy Version 1 frontend code instead of building clean Version 2 components.
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
│   ├── hooks.json                              # Command-blocking PreToolUse lifecycle hook (blocks git commit/push)
│   ├── scripts/
│   │   └── block-git-commits.mjs               # Node.js script enforcing the manual git policy
│   │
│   ├── rules/                                  # Project rule files with triggers
│   │   ├── development-rules.md                # Always-on: Beginner code, simple names, no comments
│   │   ├── git-rules.md                        # Always-on: Absolute manual commit policy, protect main
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

## 3. How the Coordinator Agent Orchestrates the Specialists

The main coordinator agent manages the end-to-end development process and delegates tasks to the four specialists:

```
[User Request]
      │
      v
[Coordinator Agent]
      │
      ├──> 1. [project-planner] (Read-Only)
      │       Reviews docs/architecture-v2.md, plans files & tests
      │
      ├──> 2. [spring-boot-developer]
      │       Implements backend features (no Lombok, no comments, constructor DI)
      │
      ├──> 3. [frontend-developer]
      │       Implements new V2 React components (V1 visual reference only)
      │
      └──> 4. [code-reviewer] (Read-Only)
              Audits changes against 12-point checklist before completion
```

### Coordinator Guardrails:
- The coordinator prevents parallel agents from modifying the same files simultaneously.
- The coordinator ensures that all backend and database choices strictly adhere to `docs/architecture-v2.md`.
- The coordinator ensures that `git commit` and `git push` are never executed by any agent.

---

## 4. Summary of the 6 Project Rules

| Rule File | Trigger | Purpose |
| :--- | :--- | :--- |
| **`development-rules.md`** | `always_on` | Enforces simple names, small methods, single responsibility, and **strictly zero comments** in code. |
| **`git-rules.md`** | `always_on` | Locks `main` branch, enforces all work on `development`, bans automated git commits/pushes. |
| **`mvp-rules.md`** | `always_on` | Prevents enterprise overengineering (no Kafka, Redis, microservices, or Spring Security). |
| **`email-security-rules.md`** | `always_on` | Enforces evidence preservation, SHA-256 hashing, safe UUID paths, and PII redaction. |
| **`spring-boot-rules.md`** | `model_decision` | Enforces standard Java without Lombok, constructor injection, and simple layers. |
| **`frontend-rules.md`** | `model_decision` | Enforces V1 as visual reference only; strictly prohibits copying V1 code. |

---

## 5. Absolute Git Commit and Push Protection

To guarantee that no agent creates automated commits or pushes:
1. **Rule Enforcement:** `AGENTS.md` and `git-rules.md` declare an absolute prohibition on git write operations.
2. **Technical Enforcement:** Configured via `.agents/hooks.json` using Antigravity's `PreToolUse` lifecycle hook:
   - Matcher: `run_command`
   - Command: `node scripts/block-git-commits.mjs`
   - Any terminal command containing `git commit`, `git push`, `git tag`, or `gh pr create` is automatically intercepted and denied before execution.
3. **Manual Human Ownership:** The developer runs `git status` and `git diff` to inspect changes, and manually executes git commits and pushes.

---

## 6. How to Build Version 2 Phase by Phase

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
  > "Implement SafeBrowsingClient and PhishTankClient in com.emailforensics.provider. Use Spring RestClient with connection and read timeouts. Normalize provider responses into ProviderObservation objects with MATCH, NO_MATCH, ERROR, or UNKNOWN. Mock provider calls with WireMock in tests."

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
  > "Implement Phase 10: RiskScoreService using the provisional weighting formula (+40, +40, +15, +15, +10, +5, +10). Build PDFBox report generator for downloading forensic summaries."

### Phase 11: End-to-End Testing and Verification
- **Goal:** Test full pipeline from upload to report download using sample fixtures.
- **Example Prompt:**
  > "Run end-to-end verification. Check all unit tests, run the frontend, test a full email analysis with a fixture EML file, verify the risk score breakdown, and inspect the generated PDF."
