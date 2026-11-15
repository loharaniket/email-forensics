---
name: code-review
description: Use this skill before marking any task complete to run a thorough quality, architecture, simplicity, and compliance check on modified files.
---

# Skill: Code Review and Quality Verification

This skill provides a standardized pre-completion review checklist to ensure every pull request or feature addition complies with project rules.

Consult the authoritative specification in [docs/architecture-v2.md](docs/architecture-v2.md).

---

## Code Review Checklist

Review all changed files against these 12 criteria:

### 1. Correct Feature Behaviour
- Does the code implement exactly what was specified in the task description and [docs/architecture-v2.md](docs/architecture-v2.md)?
- Are edge cases (empty files, missing headers, timeouts) handled gracefully?

### 2. Beginner-Friendly Simplicity
- Is the code easy for a Spring Boot beginner to read and understand?
- Are method implementations concise (under 25 lines where possible) and focused on one task?
- Are loops and conditionals simple and flat rather than deeply nested?
- Has unnecessary generic typing or pattern complexity been avoided?

### 3. Naming Conventions
- Are class names clear PascalCase nouns (`EmailController`, `EvidenceService`)?
- Are method names clear camelCase verbs (`uploadEmail`, `calculateRiskScore`)?
- Are variable names simple and descriptive (`emailFile`, `fileHash`)?

### 4. Zero Code Comments Check (CRITICAL)
- Inspect the diff line by line.
- Verify there are **NO** comments:
  - No `//` single-line comments.
  - No `/* ... */` multi-line comments.
  - No `/** ... */` JavaDoc comments.
  - No `TODO` comments.
  - No commented-out code blocks.
  - No HTML (`<!-- -->`) or CSS comments in frontend files.
- If any comment is found, remove it immediately.

### 5. Annotation & Lombok Verification
- Check that Lombok was **NOT** used anywhere (`@Data`, `@Getter`, `@Setter`, etc.).
- Ensure getters, setters, and constructors are written in standard Java.
- Check that only essential Spring Boot annotations are present.
- Verify constructor injection is used instead of `@Autowired` on private fields.

### 6. V1 Code Copying Audit (Frontend)
- Confirm that the frontend developer wrote **BRAND-NEW** Version 2 components.
- Verify that no legacy Version 1 JavaScript files or obsolete components were copied as-is into the V2 application.
- Confirm that V1 was used strictly as a visual design reference.

### 7. MVP Scope Adherence
- Does the code avoid introducing microservices, Kafka, Redis, or distributed message queues?
- Has Spring Security or JWT been avoided (as required in Version 2 MVP)?
- Does the database interaction remain restricted to `emails` and `indicators`?

### 8. Security and Trust Boundaries
- Is uploaded file input treated as untrusted?
- Are file storage paths generated safely on the server (UUIDs) without path traversal risk?
- Are original email bytes preserved without modification?
- Are URLs evaluated purely via threat intelligence without live network fetching?
- Is PII redacted before sending text to Gemini?

### 9. Provider Normalization and Fallbacks
- Do provider clients return normalized statuses: `MATCH`, `NO_MATCH`, `ERROR`, `UNKNOWN`?
- Confirm that `NO_MATCH` is NOT treated as safe.
- Confirm that provider failures do not abort the overall pipeline.

### 10. Database Schema and Integrity
- Are queries using proper indexes (`type`, `normalized_value`, `sha256`)?
- Is `ON DELETE RESTRICT` preserved on the `indicators.email_id` foreign key?
- Are JSONB values structured cleanly without storing bloated raw provider payloads?

### 11. Test Execution Verification
- Were relevant unit or integration tests written for the new functionality?
- Were the tests actually executed and did they pass?

### 12. Git Safety and Manual Commit Policy
- Confirm that **NO** automated `git commit` or `git push` commands were executed.
- Confirm that `main` branch was not modified.
- Verify that working tree files are clean and ready for the human developer to inspect and commit manually.

---

## Review Report Format

Present review findings using this simple, clear structure:

1. **Files Reviewed:** List all reviewed files with repository-relative markdown links.
2. **Compliance Status:** Pass / Needs Minor Fix.
3. **Checklist Results:** Brief status on simplicity, no comments, no Lombok, no copied V1 code, and security.
4. **Action Items (if any):** Specific small fixes required before completion.
