---
name: project-understanding
description: Use this skill before starting any development or analysis task to understand the repository structure, active branch, feature requirements, and architectural rules.
---

# Skill: Project Understanding and Context Acquisition

This skill guides the AI agent through a structured 10-step process to thoroughly understand the Email Forensics project before modifying or creating any code.

Consult the authoritative specification in [docs/architecture-v2.md](../../../docs/architecture-v2.md).

---

## 10-Step Pre-Development Checklist

Follow these ten steps sequentially:

### Step 1: Read Master Instructions
Always read [AGENTS.md](../../../AGENTS.md) at the repository root. Ensure you are familiar with:
- The beginner-friendly coding style.
- The strict no-code-comments rule.
- The ban on Lombok and complex Spring annotations.
- The absolute prohibition on `git commit`, `git push`, and automatic branch switching.
- The two-table database restriction (`emails` and `indicators`).
- The protection of `README.md` (managed manually by owner; never read or edit).

### Step 2: Read the Technical Architecture Reference
Review the relevant sections of [docs/architecture-v2.md](../../../docs/architecture-v2.md):
- Backend class and package hierarchy.
- Frontend page and component organization.
- External threat intelligence provider specifications.
- Risk score weighting formula.

### Step 3: Verify the Active Git Branch
The repository must be on `development` or an approved feature branch cut from `development` (such as `feature/email-forensics-v2`):
- **Coordinator Responsibility:** The coordinator verifies the active branch using `git branch --show-current` before invoking subagents and provides the verified branch information to read-only agents.
- **Read-Only Agents (Project Planner & Code Reviewer):** These agents do not possess terminal execution tools (`run_command`) and have `commandExecutionPolicy: off`. They must **NOT** attempt terminal commands. They rely on the coordinator's verified branch status.
- **Developer Agents:** Developer agents may run read-only `git branch --show-current` if needed.
- **Approved Branches:** Work is permitted strictly on `development` or an approved feature branch cut from `development`. Never perform work on `main`.
- **NO AUTOMATIC BRANCH SWITCHING:** Agents must **NEVER** run `git switch` or `git checkout <branch>` to change branches. If the active branch is `main` or an unapproved branch, the agent must **STOP immediately** and ask the developer to switch branches manually.

### Step 4: Map the Modular Monolith Architecture
Understand how the three core layers interact:
- **Frontend (React + Vite):** Dispatches multipart uploads, polls status, renders findings, handles AI redaction approval. Built from scratch with V1 as visual reference only.
- **Backend (Spring Boot 4.1.x, Java 21):** Coordinates asynchronous analysis via `ThreadPoolTaskExecutor`, calls local tools, talks to PostgreSQL.
- **Mail Authentication Helper (Node.js 22.19.0 or later):** Executes SPF, DKIM, DMARC, ARC checks via a local process.

### Step 5: Locate Related Files
Identify which files are involved in the current task:
- Controllers in `backend/src/main/java/com/emailforensics/controller/`.
- Services in `backend/src/main/java/com/emailforensics/service/`.
- Repositories in `backend/src/main/java/com/emailforensics/repository/`.
- React pages in `frontend/src/pages/` and components in `frontend/src/components/`.

### Step 6: Verify Database Table Definitions
Ensure any data operations interact strictly with the two allowed tables:
- `emails`: Stores the primary record, hash, timestamps, status, risk score, verdict, and JSONB fields.
- `indicators`: Stores extracted URLs, domains, IPs, QR URLs, and OCR text with compact JSONB findings.
- **Indicator Origins:** Must strictly be one of: `HEADER`, `BODY`, `HTML`, `DNS`, `IMAGE_OCR`, `QR`.

### Step 7: Confirm API Contracts and Data Shapes
Review the API endpoint contracts in Section 11 of [docs/architecture-v2.md](../../../docs/architecture-v2.md) before adding or modifying REST controllers:
- `POST /api/emails/analyze`: Accepts multipart file, returns 202 Accepted.
- `GET /api/emails/{id}`: Returns status, summary, and authentication results.
- `GET /api/emails/{id}/indicators`: Returns extracted indicators and provider verdicts.
- `POST /api/emails/{id}/ai-approval`: Records explicit approval or rejection of AI processing.

### Step 8: Define the Expected Feature Behaviour
Clarify the exact input, processing stages, and output:
- What input data is expected?
- What validation checks are required?
- How should failures in external services be handled? (Always return `UNKNOWN` or `ERROR`, never false `SAFE`).

### Step 9: Identify Existing Reusable Patterns
Check for existing patterns before creating new ones:
- Reuse existing DTOs, response models, or exception handlers in the backend.
- In frontend, consult [docs/ai/frontend-reference-guide.md](../../../docs/ai/frontend-reference-guide.md) for design tokens. Remember: V1 is visual reference only; do not copy V1 code.

### Step 10: Identify Missing Requirements or Blockers
If any requirement is ambiguous or missing:
- Do not make assumptions or invent scoring formulas.
- Ask the developer for clarification before proceeding with implementation.

---

## Output of This Skill

Once these 10 steps are complete, formulate a concise, simple implementation plan outlining:
1. Target files to create or modify.
2. Step-by-step implementation order.
3. Verification and test plan.
