---
name: testing-and-debugging
description: Use this skill when executing unit tests, integration tests, mock provider tests, frontend tests, or diagnosing and fixing build or runtime errors.
---

# Skill: Testing and Debugging Guidelines

This skill provides testing procedures, test fixture standards, mock provider configurations, and debugging workflows for the Email Forensics application.

Consult the authoritative specification in [docs/architecture-v2.md](../../../docs/architecture-v2.md).

---

## 1. Golden Testing Rules

1. **Never Claim Tests Passed Without Running Them:** Always execute tests via command line (`./mvnw test` or `npm test`) and inspect the output.
2. **Never Use Real Malware or Live Malicious URLs in Test Fixtures:** Always use reserved domains (such as `example.test`, `test.invalid`) and RFC 5737 test IP addresses (`192.0.2.1`, `198.51.100.1`, `203.0.113.1`).
3. **Never Burn API Quota in Automated Tests:** Always use WireMock or MockWebServer to simulate provider responses during automated testing. Run live provider calls only during deliberate manual smoke tests.
4. **Never Execute Git Commit or Push:** Tests should be run to verify code, but agents must never commit or push test results or code changes.

---

## 2. Backend Testing Strategy

### Unit Tests
Test individual service classes in isolation with Mockito:
- `EvidenceServiceTest`: Verify that SHA-256 calculation produces the exact expected 64-character hash for known byte arrays.
- `EmailParserServiceTest`: Verify header extraction, MIME traversal, and Jsoup HTML link extraction across various email formats.
- `RiskScoreServiceTest`: Verify that each active factor adds points correctly, duplicates are not double-counted, and the score caps at 100.
- `TextRedactorServiceTest`: Verify that names, email local parts, phone numbers, and secrets are replaced with expected tokens while domains and threat indicators remain intact.

### Provider Integration Tests (Mocked)
Use WireMock to simulate external threat provider endpoints:
- Test HTTP 200 with threat found (`MATCH`).
- Test HTTP 200 with no record found (`NO_MATCH`).
- Test HTTP 429 rate limit with `Retry-After` header.
- Test HTTP 500 server error and connection timeouts.
- Confirm that a provider failure sets that provider's status to `ERROR` or `UNKNOWN` without crashing the overall analysis pipeline.

### Database Integration Tests (Testcontainers)
- Use the PostgreSQL Testcontainers module to run repository and correlation tests against a real PostgreSQL instance:
  - Verify `EmailRepository` and `IndicatorRepository` CRUD operations.
  - Verify foreign key enforcement (`ON DELETE RESTRICT`).
  - Verify JSONB querying and indexing.
  - Verify the historical correlation query groups matching indicators properly.

---

## 3. Safe Parser Test Fixtures

Store sample `.eml` files in `test-fixtures/` covering common forensic scenarios:
- `plain-text-benign.eml`: Simple plain text email with standard headers.
- `html-links.eml`: HTML email containing visible text, styled links, and form actions.
- `multiple-hops.eml`: Email containing realistic Received header chains from mail relays.
- `dkim-signed.eml`: Email with standard DKIM-Signature header.
- `inline-image-qr.eml`: Multipart email containing an inline image with a test QR code.
- `malformed-headers.eml`: Email with missing or duplicate From headers to test parser resilience.

---

## 4. Frontend Testing Strategy

Use Vitest and React Testing Library:
- Test that file upload component accepts only `.eml` files and rejects files over 10 MB.
- Test polling behavior: ensures component polls status every 2 seconds until `COMPLETED`, `PARTIAL`, or `FAILED`.
- Test redaction modal: confirms approval or decline triggers the correct API POST request.
- Test that `UNKNOWN` or `NOT_CHECKED` statuses are never rendered as green/safe.

---

## 5. Debugging and Error Remediation Workflow

When a bug or test failure occurs:
1. **Isolate the Error:** Identify the exact file, class, method, or line causing the issue.
2. **Minimal Fix:** Apply the simplest drop-in fix that addresses the root cause without rewriting unrelated code or refactoring architectures.
3. **Re-Test:** Run the specific failing test to confirm resolution.
4. **No Code Comments:** Ensure no debug comments or commented-out code blocks remain in the codebase.
