# Frontend Design Reference Guide - Email Forensics Version 2

This document records the visual identity, colors, typography, components, and page layouts of the Version 1 frontend. All Version 2 frontend development must follow this guide to preserve the application's clean, modern dark visual style.

Consult the authoritative specification in [docs/architecture-v2.md](../architecture-v2.md).

All explanations in this guide use simple English.

---

## 1. Version 1 is a Visual Reference Only (Do Not Copy Code)

The existing Version 1 frontend located in `frontend/` is a **VISUAL REFERENCE ONLY**.

- **Take Design Inspiration From:**
  - Overall visual theme and dark colour palette.
  - Colour combinations and contrast levels.
  - Typography and spacing scale.
  - Card design, button design, and border radius.
  - Navigation style and table appearance.
  - Overall professional cybersecurity dashboard appearance.
- **STRICT PROHIBITION ON COPYING V1 CODE:**
  - Agents must **NOT** copy V1 frontend source code or reuse its components as they are.
  - Agents must design **BRAND-NEW** Version 2 React components with clean, simple, understandable code.
  - During future frontend development, agents may inspect V1 frontend files in read-only mode solely to understand visual styling and design decisions.
  - Never copy old business logic, API integrations, state management, component implementations, or legacy features.
  - Never add comments inside source code files.

---

## 2. Design Tokens and Color System

Use these exact color values across all CSS and components. Do not invent new random colors.

### Base Backgrounds and Surfaces
- **App Canvas Background:** `#0f172a` (Tailwind Slate-900) - Deep background for the entire page.
- **Card and Panel Background:** `#1e293b` (Tailwind Slate-800) - Main surface for cards, widgets, and tables.
- **Elevated Surface / Hover State:** `#334155` (Tailwind Slate-700) - Used for table headers, dropdowns, hovered rows, and active tab highlights.
- **Subtle Surface Highlight:** `#475569` (Tailwind Slate-600) - Used for component borders and dividers.

### Borders and Dividers
- **Standard Border:** `#334155` (1px solid).
- **Subtle Border:** `#1e293b` (1px solid).
- **Focus Border:** `#3b82f6` (2px solid blue).

### Typography and Text Colors
- **Font Family:** System sans-serif stack: `Inter`, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif.
- **Monospace Font (for hashes, IPs, and code):** `ui-monospace`, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace.
- **Primary Text:** `#f8fafc` (Tailwind Slate-50) - Clean off-white for headings, card titles, and high-priority data.
- **Secondary / Body Text:** `#cbd5e1` (Tailwind Slate-300) - Standard readable text for descriptions and table cells.
- **Muted / Hint Text:** `#94a3b8` (Tailwind Slate-400) - Timestamps, table labels, subtitles, and placeholder text.
- **Disabled Text:** `#64748b` (Tailwind Slate-500).

### Status and Forensic Verdict Colors
Use these colors consistently across badges, score meters, and status indicators:

| Status / Verdict | Color Hex | Color Name | Meaning |
| :--- | :--- | :--- | :--- |
| **Critical / Phishing / Malicious** | `#ef4444` | Red-500 | Confirmed threat, malicious URL, high abuse score. |
| **High Risk / Impersonation** | `#f97316` | Orange-500 | Serious anomalies, SPF/DMARC failure with mismatch. |
| **Elevated / Suspicious** | `#f59e0b` | Amber-500 | Suspicious language, unverified sender, warning flags. |
| **Guarded / Low Risk** | `#eab308` | Yellow-500 | Minor observations, low risk factors. |
| **Cryptographic Pass** | `#10b981` | Emerald-500 | Strict cryptographic verification passed (SPF pass, DKIM pass, DMARC pass). |
| **No Known Threat / Neutral** | `#64748b` | Slate-500 | No provider matches found in feeds. Render in neutral grey; absence of a known threat is NOT proof of safety. |
| **Info / Processing / Primary** | `#3b82f6` | Blue-500 | Active polling, primary buttons, informational links. |
| **Unknown / Insufficient Data** | `#64748b` | Slate-500 | Unchecked, provider timeout, or missing SMTP context. |

> [!IMPORTANT]
> Never render `NO_KNOWN_THREAT`, `UNKNOWN`, or `NOT_CHECKED` using green or benign/safe styling. Absence of a known threat in reputation feeds is never proof of safety (threat feeds only know about already-discovered attacks). These items must always use neutral grey (`#64748b`). Emerald green (`#10b981`) is reserved strictly for affirmative cryptographic verification passes (such as SPF/DKIM/DMARC PASS).

---

## 3. Standard Indicator Origin Labels

Frontend tables, cards, and tooltips must use the exact database origin values defined in [docs/architecture-v2.md](../architecture-v2.md):

| Origin Code | Display Label | Description |
| :--- | :--- | :--- |
| `HEADER` | Header Hop / Header Domain | Extracted from RFC 5322 email headers (From, Reply-To, Received hops, DKIM d=). |
| `BODY` | Plain Text Body | Extracted from the plain-text body of the message. |
| `HTML` | HTML Link / Element | Extracted from HTML anchor hrefs, image tags, or form actions. |
| `DNS` | DNS Lookup | Public IP discovered by resolving domain DNS records (A, AAAA, MX). |
| `IMAGE_OCR` | Image OCR Text | Extracted as visible text from an image by local Tesseract OCR. |
| `QR` | QR Destination | Decoded as a destination URL from a QR code by ZXing. |

Never invent custom origin strings like `MAIL_HOP` or `URL_DNS`. Always use the standard 6 origin values.

---

## 4. Spacing, Borders, and Shadows

- **Spacing Scale:** Standard 4px grid (multiples of 4):
  - Small gap: `0.5rem` (8px)
  - Standard gap: `1rem` (16px)
  - Card padding: `1.5rem` (24px)
  - Section margin: `2rem` (32px)
- **Border Radius:**
  - Small elements (tags, badges): `0.375rem` (6px)
  - Medium elements (buttons, inputs): `0.5rem` (8px)
  - Large elements (cards, modals): `0.75rem` (12px)
- **Box Shadows:**
  - Standard card shadow: `0 4px 6px -1px rgba(0, 0, 0, 0.3), 0 2px 4px -2px rgba(0, 0, 0, 0.3)`
  - Elevated modal shadow: `0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5)`

---

## 5. Reusable UI Components Catalog (Brand-New V2 Components)

Version 2 organizes the frontend into clean, brand-new components located in `frontend/src/components/`:

### 1. `FileUpload.jsx`
- Drag-and-drop file upload target.
- Accepts `.eml` only; displays file name and size.
- Shows validation error for unsupported files or files > 10 MB.

### 2. `ProgressSteps.jsx`
- Visual progress tracker during email processing.
- Stages: Saving -> Parsing -> Authentication -> Threat Checks -> AI Analysis -> Finalising.

### 3. `EmailSummary.jsx`
- Displays subject, From, Reply-To, Date, Message-ID, SHA-256 hash (with copy button), and overall verdict badge.

### 4. `RiskScoreCard.jsx`
- Large numeric score (0-100), risk badge (LOW, GUARDED, ELEVATED, HIGH, CRITICAL), and confidence badge (HIGH, MEDIUM, LOW).

### 5. `AuthenticationResults.jsx`
- 4 cards for SPF, DKIM, DMARC, and ARC with status badges and policy details.

### 6. `ReceivedChain.jsx`
- Chronological timeline of mail hops from sender to receiver, highlighting untrusted hops.

### 7. `UrlFindings.jsx`
- Table of extracted URLs/domains with origin (`BODY`, `HTML`), provider check statuses, and consolidated verdicts.

### 8. `IpFindings.jsx`
- Table of public IPs with origin (`HEADER`, `DNS`, `BODY`), geolocation (IPinfo/MaxMind), abuse score (AbuseIPDB), and verdict.

### 9. `ImageFindings.jsx`
- Displays inline/attached image previews, local Tesseract OCR text (`IMAGE_OCR`), and decoded ZXing QR destination URLs (`QR`).

### 10. `AiAssessment.jsx`
- Displays Gemini's structured evaluation: classification, plain-English summary, detected signals, evidence tags, limitations, and recommendations.

### 11. `RiskFactors.jsx`
- Itemized breakdown of scoring rules triggered, points added, and capped total score.

### 12. `RelatedEmails.jsx`
- Table of past investigations that share matching indicators (file name, sender, date, verdict, relation strength).

### 13. `RedactionPreview.jsx`
- Modal displayed during `WAITING_FOR_AI_APPROVAL` showing the redacted text preview with Approve and Decline action buttons.

### 14. `ErrorNotice.jsx`
- Reusable red alert banner for error display.

---

## 6. Frontend API Client Pattern (`src/api/emailApi.js`)

All API communication must be isolated in `src/api/emailApi.js` using native `fetch()`:

```javascript
const BASE_URL = '/api';

export async function uploadEmail(file, allowExternalLookups, requestAi) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('allowExternalLookups', allowExternalLookups);
  formData.append('requestAi', requestAi);

  const res = await fetch(`${BASE_URL}/emails/analyze`, {
    method: 'POST',
    body: formData
  });
  if (!res.ok) throw new Error((await res.json()).message || 'Upload failed');
  return res.json();
}

export async function getEmailById(id) {
  const res = await fetch(`${BASE_URL}/emails/${id}`);
  if (!res.ok) throw new Error('Failed to load email record');
  return res.json();
}

export async function getRedactionPreview(id) {
  const res = await fetch(`${BASE_URL}/emails/${id}/redaction-preview`);
  if (!res.ok) throw new Error('Failed to load redaction preview');
  return res.json();
}

export async function sendAiApproval(id, approved) {
  const res = await fetch(`${BASE_URL}/emails/${id}/ai-approval`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ approved })
  });
  if (!res.ok) throw new Error('Failed to submit AI approval');
  return res.json();
}

export async function getIndicators(id) {
  const res = await fetch(`${BASE_URL}/emails/${id}/indicators`);
  if (!res.ok) throw new Error('Failed to load indicators');
  return res.json();
}

export async function getCorrelations(id) {
  const res = await fetch(`${BASE_URL}/emails/${id}/correlations`);
  if (!res.ok) throw new Error('Failed to load correlations');
  return res.json();
}

export async function getHistory(page = 0, size = 20) {
  const res = await fetch(`${BASE_URL}/emails?page=${page}&size=${size}`);
  if (!res.ok) throw new Error('Failed to load history');
  return res.json();
}
```
