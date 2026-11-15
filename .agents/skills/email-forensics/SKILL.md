---
name: email-forensics
description: Use this skill when implementing, verifying, or debugging the core email forensics pipeline, email parsing, authentication checks, threat intelligence lookups, OCR/QR decoding, or risk score calculation.
---

# Skill: Email Forensics Investigation and Pipeline Processing

This skill serves as the comprehensive technical guide for the 20-step forensic email analysis pipeline defined in Version 2.

## When to Use This Skill

Activate this skill when:
- Implementing or modifying the `AnalysisOrchestrator` workflow.
- Parsing MIME headers, Received chains, or bodies using Jakarta Mail and Jsoup.
- Calling the Node.js `mailauth` helper for SPF, DKIM, DMARC, and ARC verification.
- Integrating external threat intelligence client classes in `com.emailforensics.provider`.
- Implementing OCR (Tesseract) or QR decoding (ZXing).
- Redacting PII and integrating Gemini via Spring AI.
- Calculating risk scores and determining investigation verdicts.

---

## The 20-Step Forensic Pipeline

The backend processes uploaded emails across 20 distinct stages:

```
[Upload .eml] -> [Validate & Hash SHA-256] -> [Store Original File] -> [Create DB Record]
     │
     v
[Parse MIME & Headers] -> [Extract Received Chain] -> [Extract HTML/Text URLs]
     │
     v
[Node.js mailauth: SPF / DKIM / DMARC / ARC] -> [Extract Domains & Public IPs]
     │
     v
[Extract Images: Tesseract OCR & ZXing QR] -> [Query Threat Intel Providers]
     │
     v
[Normalize Provider Findings] -> [Redact PII] -> [Pause for User Approval]
     │
     v
[Spring AI: Gemini Structured Evaluation] -> [Historical Correlation Query]
     │
     v
[Calculate Explainable Risk Score] -> [Save Final Results] -> [Generate PDF Report]
```

---

## 1. Evidence Preservation and Hashing

1. Receive the file stream as raw bytes.
2. Calculate the SHA-256 hash using Java's built-in `MessageDigest`:
   ```java
   MessageDigest digest = MessageDigest.getInstance("SHA-256");
   byte[] hashBytes = digest.digest(rawBytes);
   String sha256Hex = HexFormat.of().formatHex(hashBytes);
   ```
3. Generate a secure server-side storage path using UUID: `storage/emails/<uuid>/original.eml`.
4. Never modify, overwrite, or re-encode the original `.eml` file.

---

## 2. Email Parsing (Jakarta Mail / Angus Mail)

Extract all relevant headers and body parts:
- `From`: Parsed display name and address.
- `Reply-To`: Check for mismatches with From address.
- `Return-Path`: Envelope sender address.
- `Date` and `Message-ID`.
- `Received` headers: Maintain the complete raw sequence from newest to oldest hop.
- `Authentication-Results` and `Received-SPF` headers.
- Plain text and HTML body parts (using Jsoup for HTML text and link extraction).

---

## 3. Email Authentication Verification (`mailauth` Helper)

Run the Node.js helper `tools/mailauth/verify.mjs` via `ProcessBuilder`:
- Check SPF, DKIM, DMARC, and ARC using the original email bytes.
- Standard protocol outcome states:
  - `PASS`: Signature or policy passed.
  - `FAIL`: Verification failed.
  - `NONE`: No record or header present.
  - `TEMPERROR`: Temporary DNS or network issue.
  - `PERMERROR`: Permanent protocol or syntax error.
  - `NOT_VERIFIABLE`: Required SMTP context is missing.
- **Never convert `NOT_VERIFIABLE` into `PASS` or `FAIL`.**

---

## 4. Threat Intelligence Provider Lookups

Query threat providers based on indicator type:

| Indicator Type | External Providers | Notes |
| :--- | :--- | :--- |
| **URLs** | Google Safe Browsing v5, PhishTank, OpenPhish feed, URLhaus, ThreatFox | De-duplicate URLs before querying. Never fetch URLs live. |
| **Domains** | ThreatFox, VirusTotal (quota permitting) | Extract from From, Reply-To, and URL hostnames. |
| **Public IPs** | AbuseIPDB, IPinfo Lite, GreyNoise Community | Skip private, loopback, and reserved IP ranges (RFC 1918). |

### Provider Response Normalization
Normalize each response into the standard internal `ProviderObservation` model:
- `provider`: Provider name string (e.g. `PHISHTANK`, `ABUSEIPDB`).
- `status`: `MATCH`, `NO_MATCH`, `ERROR`, or `UNKNOWN`.
- `verdict`: `PHISHING`, `MALWARE`, `ABUSE`, or `null`.
- `checkedAt`: Timestamp of check.
- `evidence`: Compact key-value details (never store raw HTML or huge payloads).

---

## 5. OCR and QR Code Processing

1. Extract supported image MIME types (`image/jpeg`, `image/png`, `image/gif`, `image/webp`).
2. Run local **Tesseract OCR** to extract visible text (store in temporary derived file, delete after analysis).
3. Run **ZXing** to decode QR codes.
4. If a QR code contains a URL, label it as indicator type `QR_URL` and route it through the standard URL threat intelligence pipeline.

---

## 6. PII Redaction and AI Assessment Contract

1. Redact personal identifiers using pattern replacement:
   - Names -> `[PERSON]`
   - Local-part of email -> `[EMAIL]` (keep domain)
   - Phone numbers -> `[PHONE]`
   - Account numbers -> `[ACCOUNT]`
   - Card numbers -> `[CARD]`
   - OTP codes -> `[OTP]`
   - Secrets/Tokens -> `[SECRET]`
2. Mask sensitive query parameters in extracted URLs.
3. Present preview in UI and set status to `WAITING_FOR_AI_APPROVAL`.
4. Upon user approval, send structured request to Gemini via Spring AI `ChatClient`.
5. Expected AI output contract:
   - `classification`: `PHISHING`, `SUSPICIOUS`, or `BENIGN`
   - `fraudIndicators`: List of detected fraud types
   - `impersonationSignals`: List of impersonation indicators
   - `socialEngineeringSignals`: List of social engineering techniques
   - `promptInjectionDetected`: Boolean
   - `confidenceLabel`: `LOW`, `MEDIUM`, or `HIGH`
   - `explanation`: Plain-English summary
   - `evidenceIds`: List of referenced indicator IDs
   - `limitations`: List of investigation caveats
   - `recommendations`: Plain text recommendations

---

## 7. Risk Score Calculation and Risk Levels

The risk score is calculated strictly by backend deterministic rules in `RiskScoreService`. The AI model does NOT determine the numeric score.

### Scoring Factors (Cap at 100 points maximum):
1. **Verified Phishing URL or Domain:** `+40 points` (counted once across all matching providers).
2. **Confirmed Malware URL:** `+40 points` (if URL already scored as phishing, do not double-count).
3. **Known Malicious IP:** `+15 points` (ThreatFox match or AbuseIPDB score > 80).
4. **Strong Authentication Inconsistency:** `+15 points` (DMARC fail with sender mismatch, or DKIM fail with bad signature).
5. **Separate Malicious QR Destination:** `+10 points` (only if distinct from already scored URLs).
6. **AI-Supported Social Engineering / Fraud:** `+5 points` (credential harvesting or urgent payment request).
7. **Historical Repeat of Known Malicious Indicator:** `+10 points` (exact match on malicious URL/domain/IP in database).

### Formula:
$$\text{Final Risk Score} = \min(\text{Sum of Unique Active Factors}, 100)$$

### Risk Levels:
- `0 - 19`: **LOW**
- `20 - 39`: **GUARDED**
- `40 - 59`: **ELEVATED**
- `60 - 79`: **HIGH**
- `80 - 100`: **CRITICAL**

### Final Verdict:
- `PHISHING`: Strong evidence of phishing or credential fraud.
- `SUSPICIOUS`: Noticeable anomalies or elevated risk.
- `NO_KNOWN_THREAT`: All checks completed and no threats were identified (does not guarantee safety).
- `INSUFFICIENT_DATA`: Critical providers failed or key evidence was unverifiable.
