---
name: email-forensics
description: Use this skill when implementing, verifying, or debugging the core email forensics pipeline, email parsing, authentication checks, threat intelligence lookups, OCR/QR decoding, or risk score calculation.
---

# Skill: Email Forensics Investigation and Pipeline Processing

This skill serves as the comprehensive technical guide for the 20-step forensic email analysis pipeline defined in Version 2.

Consult the authoritative specification in [docs/architecture-v2.md](docs/architecture-v2.md).

---

## 1. The 20-Step Forensic Pipeline

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

## 2. Evidence Preservation and Hashing

1. Receive the file stream as raw bytes.
2. Calculate the SHA-256 hash immediately using Java's built-in `MessageDigest`:
   ```java
   MessageDigest digest = MessageDigest.getInstance("SHA-256");
   byte[] hashBytes = digest.digest(rawBytes);
   String sha256Hex = HexFormat.of().formatHex(hashBytes);
   ```
3. Generate a secure server-side storage path using UUID: `storage/emails/<uuid>/original.eml`.
4. Never modify, overwrite, or re-encode the original `.eml` file.

---

## 3. Email Parsing (Jakarta Mail / Angus Mail)

Extract all relevant headers and body parts:
- `From`: Parsed display name and address.
- `Reply-To`: Check for mismatches with From address.
- `Return-Path`: Envelope sender address.
- `Date` and `Message-ID`.
- `Received` headers: Maintain the complete raw sequence from newest to oldest hop.
- `Authentication-Results` and `Received-SPF` headers.
- Plain text and HTML body parts (using Jsoup for HTML text and link extraction).

---

## 4. Email Authentication Verification (`mailauth` Helper)

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

## 5. Threat Intelligence Providers and Normalization

Query threat providers based on indicator type:

| Indicator Type | Primary Providers | Fallbacks / Context | Key Constraints |
| :--- | :--- | :--- | :--- |
| **URLs** | Safe Browsing v5, PhishTank, OpenPhish feed, URLhaus, ThreatFox | VirusTotal (quota limited) | De-duplicate URLs before querying. Never fetch URLs live. |
| **Domains** | ThreatFox, Safe Browsing, PhishTank | VirusTotal (quota limited) | Extract from From, Reply-To, and URL hostnames. |
| **Public IPs** | AbuseIPDB (abuse reputation) | ThreatFox, VirusTotal, GreyNoise (scanning context) | Skip private/reserved IPs (RFC 1918). Geolocation is NOT reputation! |
| **IP Geolocation** | IPinfo Lite (country/ASN) | MaxMind GeoLite2 (local fallback) | Provides network context only; never treat location as malicious verdict. |
| **Image OCR** | Tesseract 5 (local, default) | OCR.space (cloud, requires user consent) | Free cloud OCR limited to 500 req/day. Local OCR runs on server. |
| **AI Threat Eval** | Google Gemini via Spring AI | Ollama (local LLM fallback) | Requires PII redaction and explicit user approval before sending. |

### Normalized Result Statuses:
- `MATCH`: Provider found a confirmed threat/abuse record.
- `NO_MATCH`: Query succeeded, but provider had no record. **`NO_MATCH` does NOT prove the indicator is safe.**
- `ERROR`: Request failed (timeout, network error, invalid key). Does not abort the pipeline.
- `UNKNOWN`: Available evidence is insufficient to reach a verdict.

---

## 6. OCR and QR Code Processing

1. Extract supported image MIME types (`image/jpeg`, `image/png`, `image/gif`, `image/webp`).
2. Run local **Tesseract OCR** to extract visible text (store in temporary derived file, delete after analysis).
3. Run **ZXing** to decode QR codes.
4. If a QR code contains a URL, label it as indicator type `QR_URL` and route it through the standard URL threat intelligence pipeline.

---

## 7. PII Redaction and AI Assessment Contract

1. Redact personal identifiers using pattern replacement:
   - Names -> `[PERSON]`
   - Local-part of email -> `[EMAIL]` (retain domain)
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

## 8. Risk Score Calculation and Risk Levels

The risk score is calculated strictly by backend deterministic rules in `RiskScoreService`. The AI model does NOT determine the numeric score.

### Provisional Scoring Factors (Capped at 100 points maximum):
1. **Verified Phishing URL or Domain:** `+40 points` (PhishTank or Safe Browsing verified match).
2. **Confirmed Malware-Distribution URL:** `+40 points` (URLhaus match; if already counted as phishing URL, do not double-count).
3. **Known Malicious IP Evidence:** `+15 points` (ThreatFox match or AbuseIPDB score > 80).
4. **Strong Email Authentication Inconsistency:** `+15 points` (DMARC fail with sender mismatch, or DKIM fail with bad signature).
5. **Separate Malicious QR Destination:** `+10 points` (only if distinct from already scored URLs).
6. **AI-Supported Social Engineering / Fraud:** `+5 points` (credential harvesting, urgent payment, or executive impersonation).
7. **Historical Repeat of Confirmed Malicious Indicator:** `+10 points` (exact match with an indicator marked MALICIOUS in a previous investigation).

$$\text{Final Risk Score} = \min(\text{Raw Points}, 100)$$

### Provisional Weights & Calibration Requirements:
- All scoring weights are **provisional starting heuristics**.
- They must be evaluated and tuned on a labelled dataset of legitimate and phishing emails using formal metrics:
  - Precision
  - Recall
  - F1-Score
  - False Positive Rate
  - Confusion Matrix
- Do not claim research-paper accuracy without local benchmark testing.
- AI confidence labels (`HIGH`, `MEDIUM`, `LOW`) must not be presented as statistical probabilities.

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
