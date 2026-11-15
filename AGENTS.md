# Antigravity AI Agent Instructions - Email Forensics Version 2

Welcome to the Email Forensics project repository. This file defines the permanent rules, guidelines, and operating principles for all AI assistants working on this codebase.

The single authoritative technical reference for Version 2 is [docs/architecture-v2.md](docs/architecture-v2.md). All agents must consult that document before designing or modifying code.

---

## 1. Project Identity and Purpose

- **Project Name:** Email Forensics
- **Version:** Version 2 (MVP)
- **Primary Goal:** Build an AI-assisted forensic web application that allows security analysts to upload `.eml` files, safely preserve original evidence, parse headers and MIME content, verify authentication (SPF, DKIM, DMARC, ARC), query threat intelligence feeds, perform OCR/QR analysis, run privacy-safe AI threat evaluation, identify historical correlations, and calculate an explainable risk score.
- **Architecture Style:** Modular Monolith consisting of:
  - Frontend: React + Vite (brand-new V2 components)
  - Backend: Java 21 + Spring Boot 4.1.x
  - Database: PostgreSQL (strictly two application tables: `emails` and `indicators`)
  - Mail Authentication Helper: Node.js 22.19.0 or later with `mailauth`
  - Local Image Tools: Tesseract OCR 5 (local text extraction) and ZXing (QR code decoding)
  - AI Integration: Spring AI with Gemini (using strictly redacted text after explicit user approval; Ollama as optional local fallback)
  - Reporting: Apache PDFBox for downloadable forensic summary reports

---

## 2. Protection of README.md (Manual Management by Owner)

- `README.md` is currently managed manually by the repository owner.
- AI agents must **NOT** read, open, modify, rewrite, or use `README.md` as the authoritative architecture reference until the owner explicitly updates it.
- The **ONLY** authoritative Version 2 technical reference is [docs/architecture-v2.md](docs/architecture-v2.md).

---

## 3. Git Branch and Absolute Manual Commit Policy

- **Main Branch (`main`):**
  - Contains the stable, preserved Version 1 of Email Forensics.
  - **NEVER** modify, commit to, rebase, or push to the `main` branch.
- **Development Branch (`development`):**
  - All Version 2 development must happen strictly on the `development` branch (or feature branches cut from `development`, such as `feature/email-forensics-v2`).
  - Always verify that the active branch is `development` before writing code.
- **No Automatic Branch Switching:**
  - AI agents must **NEVER** automatically switch branches (no `git switch` or `git checkout <branch>`).
  - If the active branch is `main` or anything other than `development`, the agent must **STOP immediately** and ask the developer to switch branches manually.
- **Absolute Manual Commit & Push Prohibition:**
  - AI agents are **STRICTLY PROHIBITED** from executing `git commit`, `git push`, `git push --force`, `git reset`, `git clean`, `git checkout .`, `git restore .`, or any GitHub write commands under any circumstance, even if explicitly instructed in a prompt.
  - The repository owner manually reviews, stages, commits, and pushes all code.
  - This policy is enforced technically via `.agents/hooks.json` PreToolUse command filters and requires continuous human verification.
  - Agents may only run read-only git operations (`git status`, `git diff`, `git log`, `git branch --show-current`, `git show`).

---

## 4. Beginner-Friendly Coding Requirements

The developer is learning Spring Boot while building this project. Every piece of code generated must be easy to read, understand, and maintain.

1. **Simple Names:** Use clear, obvious class names (`EmailController`, `EmailService`, `EmailRepository`), method names (`uploadEmail()`, `checkEmail()`, `calculateRiskScore()`), and variable names (`emailText`, `emailFile`, `fileHash`).
2. **Small Methods:** Keep methods short (under 25 lines where possible) with simple, single-purpose logic.
3. **Single Responsibility:** Each class must do one main job.
4. **No Unnecessary Abstractions:** Avoid abstract classes, unnecessary generic classes, complex design patterns, or extra interface layers unless strictly required.
5. **Simple Flow:** Use basic loops and conditions. Avoid deeply nested `if`/`else` blocks and complicated Java stream pipelines when simple loops are clearer.
6. **No Complicated Helper Classes:** Keep logic close to where it is used instead of creating dozens of abstract utility classes.

---

## 5. Strict No-Comments Rule in Source Code

Do NOT put comments inside source code files. This is a strict project requirement.

- No single-line comments (`//`).
- No multi-line comments (`/* ... */`).
- No JavaDoc comments (`/** ... */`).
- No TODO comments.
- No HTML comments (`<!-- ... -->`).
- No CSS comments (`/* ... */`).
- No explanatory comments inside methods.

Write clean, self-explanatory code with clear names instead of adding comments. When explanations are helpful, provide them in chat responses or external Markdown files in `docs/`.

---

## 6. Spring Boot Development Guidelines

- **Basic Annotations Only:** Use only necessary Spring Boot annotations:
  - `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`
  - `@Service`, `@Repository`
  - `@Entity`, `@Id`, `@GeneratedValue`, `@Column` (when required), `@Transactional` (when required)
- **STRICTLY NO LOMBOK:**
  - Do NOT use Lombok.
  - Do NOT use `@Data`, `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, or `@AllArgsConstructor`.
  - Write standard Java constructors, getters, and setters manually.
- **Constructor Injection:** Use constructor-based dependency injection with simple constructors. Do NOT use field injection (`@Autowired` on private fields).
- **Simple Layering:** Keep code organized in basic packages: `controller`, `service`, `repository`, `entity`, `dto`, `provider`, `model`, `exception`, and `config`.
- **No Complex Frameworks:** Use simple Spring Data JPA queries and Spring Framework `RestClient` for external HTTP requests.

---

## 7. MVP Development Rules

This project is an MVP (Minimum Viable Product) for educational and practical email forensic investigation.

- Do NOT introduce enterprise technologies unless explicitly requested:
  - No microservices.
  - No Kafka, RabbitMQ, or message brokers.
  - No Redis or distributed caching.
  - No Kubernetes or complex cloud infrastructure.
  - No user management, login, registration, JWT, OAuth, or Spring Security in Version 2.
  - No case management or campaign management tables.
  - No graph databases (Neo4j) or graph visualizations.
  - No attachment execution or non-image attachment scanning.
- **Minimal Required Validation:** Validate input existence, `.eml` extension, basic file size limit (10 MB), parseability, and required database fields. Do not build complex custom validation engines.

---

## 8. Frontend Visual Reference Only (Do Not Copy V1 Code)

- The existing Version 1 frontend located in `frontend/` is a **VISUAL REFERENCE ONLY**.
- Take design inspiration from V1 in:
  - Overall visual theme and dark colour palette (`#0f172a`, `#1e293b`, `#334155`).
  - Typography, spacing, borders, card styles, and button aesthetics.
  - Professional cybersecurity dashboard appearance.
- **STRICT PROHIBITION:**
  - Agents must **NOT** copy V1 frontend source code or reuse its components as they are.
  - Design **BRAND-NEW** V2 React components with clean, beginner-friendly code.
  - Never copy old business logic, legacy API integrations, state management, or obsolete features.
- Build frontend pages using React, Vite, standard CSS, and browser-native `fetch()`. No Axios, no Redux.
- Consult [docs/ai/frontend-reference-guide.md](docs/ai/frontend-reference-guide.md) for design tokens and component specifications.

---

## 9. Security and Trust Boundaries

1. **Untrusted Input:** Treat all uploaded emails, headers, body text, URLs, and attachments as untrusted.
2. **Never Execute Attachments:** Never execute or run scripts from uploaded files. Process inline/attached images for OCR and QR decoding only.
3. **Never Fetch URLs:** Never visit, open, or fetch URLs found in emails. Only query their reputation via external threat intelligence APIs.
4. **Evidence Preservation:**
   - Compute SHA-256 hash immediately upon receiving raw bytes.
   - Save the original file to private storage outside the web root under a generated UUID path.
   - Never overwrite or modify the original `.eml` file.
5. **PII Redaction & Explicit Approval:**
   - Redact personal information (names, emails, phones, tokens, credentials) before sending text to Gemini.
   - Show the redacted preview to the user and pause for explicit approval (`WAITING_FOR_AI_APPROVAL`) before contacting cloud AI.
6. **API Keys:** Store all threat provider keys and Gemini keys in environment variables. Never expose keys to the frontend or git.

---

## 10. Database Architecture (Two Tables Only)

- Database name: `email_forensics_db` (PostgreSQL)
- **Only Two Main Tables:**
  1. `emails`: Stores email metadata, file path, SHA-256 hash, status, risk score, verdict, and compact JSONB fields (`auth_data`, `ai_data`).
  2. `indicators`: Stores extracted indicators (`type IN ('URL', 'DOMAIN', 'IP', 'QR_URL', 'OCR_TEXT')`, `origin IN ('HEADER', 'BODY', 'HTML', 'DNS', 'IMAGE_OCR', 'QR')`) with foreign key `email_id` (`ON DELETE RESTRICT`) and compact provider results in JSONB `details`.
- Do NOT create additional tables for users, campaigns, cases, or graph nodes.
- Historical correlation is handled with standard PostgreSQL queries and displayed in normal tables.

---

## 11. Explanation and Mentoring Rules

The developer wants to learn while building. Whenever completing a task or phase, explain:
1. **What files were created or modified:** Clear list of paths.
2. **Why each file is needed:** Its single responsibility in simple English.
3. **How data flows through the feature:** Controller -> Service -> Provider/Repository -> Database.
4. **How to test the implementation:** Concrete commands and expected outputs.
5. **What the developer should understand before moving to the next phase.**

Keep explanations beginner-friendly without unnecessary jargon.

---

## 12. Rule and Skill Discovery

Before executing development work, Antigravity AI agents must consult the appropriate workspace rules and skills:

- **Authoritative Specification:**
  - [docs/architecture-v2.md](docs/architecture-v2.md) - Single source of truth for V2 architecture.

- **Rules (`.agents/rules/`):**
  - [development-rules.md](.agents/rules/development-rules.md) - Coding standards, naming, zero comments.
  - [git-rules.md](.agents/rules/git-rules.md) - Branch protection, absolute manual commit/push policy.
  - [mvp-rules.md](.agents/rules/mvp-rules.md) - MVP scope constraints, avoiding bloat.
  - [email-security-rules.md](.agents/rules/email-security-rules.md) - Evidence preservation, untrusted data handling.
  - [spring-boot-rules.md](.agents/rules/spring-boot-rules.md) - Backend rules, no Lombok, basic annotations.
  - [frontend-rules.md](.agents/rules/frontend-rules.md) - Frontend design rules, V1 visual reference only.

- **Skills (`.agents/skills/`):**
  - `project-understanding`: Step-by-step procedure to inspect project state before coding.
  - `spring-boot-beginner`: Practical guide to writing beginner-friendly Spring Boot code.
  - `frontend-reference`: Practical guide to creating new React components matching V1 visual design.
  - `email-forensics`: Technical reference for email parsing, auth checks, threat intel, and risk scoring.
  - `database-development`: Practical instructions for PostgreSQL entities, schema, and correlation queries.
  - `testing-and-debugging`: Procedures for running unit, integration, fixture, and mock tests.
  - `code-review`: Final pre-completion verification checklist.
