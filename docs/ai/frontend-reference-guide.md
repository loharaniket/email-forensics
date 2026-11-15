# Frontend Design Reference Guide - Email Forensics Version 2

This document records the visual identity, colors, typography, components, and page layouts of the Version 1 frontend. All Version 2 frontend development must follow this guide to preserve the application's clean, modern dark visual style.

All explanations in this guide use simple English.

---

## 1. Application Theme and Visual Identity

The Email Forensics frontend uses a professional cyber-security dark theme.
- **Style:** Clean, minimal, high-contrast dark dashboard.
- **Tone:** Technical, trustworthy, easy to read.
- **Primary Accent:** Vivid blue for primary actions, focus highlights, and active tabs.
- **Surface Elevation:** Subtle shifts from deep background slate to lighter slate surfaces for cards, modals, and tables.

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
| **Benign / Safe / Pass** | `#10b981` | Emerald-500 | Check passed, SPF pass, DKIM pass, benign indicator. |
| **Info / Processing / Primary** | `#3b82f6` | Blue-500 | Active polling, primary buttons, informational links. |
| **Unknown / Insufficient Data** | `#64748b` | Slate-500 | Unchecked, provider timeout, or missing SMTP context. |

> [!IMPORTANT]
> Never render `UNKNOWN` or `NOT_CHECKED` using green or benign styling. Unchecked items must always use neutral grey (`#64748b`).

---

## 3. Spacing, Borders, and Shadows

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

## 4. Reusable UI Components Catalog

Version 2 organizes the frontend into clean, reusable components located in `frontend/src/components/`:

### 1. `FileUpload.jsx`
- **Purpose:** Drag-and-drop file upload target.
- **States:**
  - Default: Dashed border (`#334155`), upload icon, file prompt text.
  - Drag-over: Solid blue border (`#3b82f6`), slightly lighter background.
  - File selected: Shows file name, human-readable size (e.g. `2.4 MB`), and remove button.
- **Controls:** Only allows `.eml` files; displays an inline warning if a user selects an unsupported file format or file over 10 MB.

### 2. `ProgressSteps.jsx`
- **Purpose:** Visual progress tracker during email processing.
- **Stages Displayed:**
  1. Saving original evidence
  2. Parsing MIME headers and content
  3. Verifying mail authentication (SPF, DKIM, DMARC, ARC)
  4. Querying threat intelligence providers
  5. Generating AI threat evaluation
  6. Finalising investigation report
- **Visuals:** Step circles connected by lines; active step has an animated pulse effect; completed steps show a green checkmark.

### 3. `EmailSummary.jsx`
- **Purpose:** Top overview banner on the result page.
- **Displays:**
  - Message Subject (bold, primary text).
  - From Address and Display Name.
  - Reply-To Address (highlights mismatch if different from From).
  - Date and Message-ID.
  - SHA-256 Hash of original file (monospace with one-click copy button).
  - Overall Investigation Verdict badge.

### 4. `RiskScoreCard.jsx`
- **Purpose:** Prominent visual summary of the calculated risk score.
- **Displays:**
  - Large numeric score (e.g. `75 / 100`).
  - Circular or linear progress meter with color keyed to risk level:
    - 0-19: Green (LOW)
    - 20-39: Yellow (GUARDED)
    - 40-59: Amber (ELEVATED)
    - 60-79: Orange (HIGH)
    - 80-100: Red (CRITICAL)
  - Confidence Badge: `HIGH`, `MEDIUM`, or `LOW`.

### 5. `AuthenticationResults.jsx`
- **Purpose:** Detailed summary of email authentication protocols.
- **Displays 4 Protocol Cards:**
  - **SPF:** Status badge (`PASS`, `FAIL`, `NONE`, `TEMPERROR`, `PERMERROR`, `NOT_VERIFIABLE`), client IP, MAIL FROM domain.
  - **DKIM:** Status badge, signing domain (`d=`), selector (`s=`).
  - **DMARC:** Status badge, policy (`none`, `quarantine`, `reject`), alignment check.
  - **ARC:** Status badge, chain validation result.

### 6. `ReceivedChain.jsx`
- **Purpose:** Interactive timeline of mail hops.
- **Displays:**
  - Chronological list of hops (from originating client to destination server).
  - Hop number, transmitting server, receiving server, timestamp, and delay between hops.
  - Visual indicator highlighting hops that crossed untrusted boundaries.

### 7. `UrlFindings.jsx`
- **Purpose:** Table of all URLs and domains extracted from the email body and HTML.
- **Table Columns:**
  - Original URL (truncated with hover tooltip, query parameters masked where sensitive).
  - Origin (`BODY`, `HTML`).
  - Threat Providers Checked (e.g. Safe Browsing, PhishTank, URLhaus).
  - Individual Provider Verdicts (`MATCH`, `NO_MATCH`, `ERROR`).
  - Consolidated Indicator Verdict badge (`MALICIOUS`, `SUSPICIOUS`, `NO_MATCH`, `UNKNOWN`).

### 8. `IpFindings.jsx`
- **Purpose:** Table of public IP addresses found in mail hops or URLs.
- **Table Columns:**
  - IP Address (monospace).
  - Origin (`MAIL_HOP`, `URL_DNS`).
  - Geolocation & ASN (Country code, ISP/Organization from IPinfo Lite).
  - Abuse Score (from AbuseIPDB, e.g. `82%`).
  - Verdict badge.

### 9. `ImageFindings.jsx`
- **Purpose:** Displays findings from inline images and image attachments.
- **Displays:**
  - Thumbnail preview of the image.
  - OCR Extracted Text block (from Tesseract OCR).
  - Decoded QR code URL (from ZXing) with reputation verdict badge.

### 10. `AiAssessment.jsx`
- **Purpose:** Privacy-safe AI threat evaluation summary from Gemini.
- **Displays:**
  - Classification banner (`PHISHING`, `SUSPICIOUS`, `BENIGN`).
  - Plain-English explanation paragraph explaining why the email is dangerous or safe.
  - Detected Signal Badges: `URGENCY`, `PAYMENT_REQUEST`, `CREDENTIAL_HARVESTING`, `EXECUTIVE_IMPERSONATION`.
  - Evidence Cross-References (tags linking to specific indicator IDs).
  - Limitations and Actionable Recommendations list.

### 11. `RiskFactors.jsx`
- **Purpose:** Transparent breakdown of how the risk score was calculated.
- **Displays:**
  - Itemized table of scoring rules triggered.
  - Factor description (e.g. "Verified Phishing URL match in PhishTank").
  - Points added (e.g. `+40`).
  - Capped total score calculation.

### 12. `RelatedEmails.jsx`
- **Purpose:** Historical correlation table of other emails in the database sharing indicators.
- **Displays:**
  - Email ID, File Name, Sender, Subject, Date, Verdict.
  - Matched Indicator (e.g. identical URL or same malicious IP).
  - Relation Strength badge (`STRONG`, `MEDIUM`, `WEAK`).

### 13. `RedactionPreview.jsx`
- **Purpose:** Modal displayed when status is `WAITING_FOR_AI_APPROVAL`.
- **Displays:**
  - Warning banner stating that text will be sent to the free Gemini API.
  - Read-only preview box showing the exact redacted text (with `[PERSON]`, `[EMAIL]`, `[PHONE]` tokens).
  - Action buttons: "Approve & Send to AI" (Primary Blue) and "Decline AI Analysis" (Secondary/Cancel).

### 14. `ErrorNotice.jsx`
- **Purpose:** Reusable alert banner for errors.
- **Displays:**
  - Red background (`#7f1d1d`), red border (`#ef4444`), alert icon, clear error message.

---

## 5. Page Layouts and Flows

### Page 1: Upload Page (`UploadPage.jsx`)
```
┌──────────────────────────────────────────────────────────┐
│                   EMAIL FORENSICS V2                     │
│        Investigate Suspicious Emails Safely              │
├──────────────────────────────────────────────────────────┤
│                                                          │
│   ┌──────────────────────────────────────────────────┐   │
│   │                                                  │   │
│   │           [ Upload .eml File Zone ]              │   │
│   │           Drag and drop or Browse                │   │
│   │                                                  │   │
│   └──────────────────────────────────────────────────┘   │
│                                                          │
│   [x] Allow External Threat Intelligence Lookups         │
│   [x] Request AI Threat Evaluation (Redacted Preview)    │
│                                                          │
│                 [  ANALYSE EMAIL  ]                      │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### Page 2: Analysis Page (`AnalysisPage.jsx`)
```
┌──────────────────────────────────────────────────────────┐
│ Investigation #1042: invoice.eml                         │
│ Status: PROCESSING                                       │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  (✓) Saving  ->  (✓) Parsing  ->  (*) Threat Checks  ->  ( ) AI  ->  ( ) Done
│                                                          │
│  Current Stage: Querying URLhaus and PhishTank...        │
│  [ Polling backend every 2 seconds ]                     │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### Page 3: Result Page (`ResultPage.jsx`)
```
┌──────────────────────────────────────────────────────────────────────────┐
│  [ EmailSummary Card: Sender, Subject, Date, SHA-256 Hash ]              │
├─────────────────────────────────────┬────────────────────────────────────┤
│  Risk Score: 75 / 100               │  AI Evaluation Summary:            │
│  Level: HIGH  | Confidence: MEDIUM  │  "Message requests urgent wire...  │
│  Verdict: SUSPICIOUS                │   Signals: Urgency, Payment"       │
├─────────────────────────────────────┴────────────────────────────────────┤
│  [ Tabs: Authentication | Indicators | Images/QR | Risk Factors | Related]│
├──────────────────────────────────────────────────────────────────────────┤
│  [ Active Tab Content ]                                                  │
├──────────────────────────────────────────────────────────────────────────┤
│  [ Download PDF Report Button ]                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Page 4: History Page (`HistoryPage.jsx`)
```
┌──────────────────────────────────────────────────────────────────────────┐
│ Previous Investigations                                                  │
├────────┬──────────────┬──────────────────┬───────────┬─────────┬─────────┤
│ ID     │ File Name    │ Sender           │ Date      │ Score   │ Verdict │
├────────┼──────────────┼──────────────────┼───────────┼─────────┼─────────┤
│ 1042   │ invoice.eml  │ billing@test.com │ Nov 15    │ 75/100  │ SUSP... │
│ 1041   │ verify.eml   │ security@acme.co │ Nov 14    │ 95/100  │ PHISH.. │
└────────┴──────────────┴──────────────────┴───────────┴─────────┴─────────┘
```

---

## 6. Frontend API Client Pattern (`src/api/emailApi.js`)

All API communication must be isolated in `src/api/emailApi.js`:

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
