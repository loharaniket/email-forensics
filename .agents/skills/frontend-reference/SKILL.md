---
name: frontend-reference
description: Use this skill when building or styling React frontend pages, components, hooks, or API client code to match the Version 1 visual design and design tokens.
---

# Skill: Frontend Design Reference and UI Implementation

This skill provides step-by-step guidance on how to replicate and maintain the visual identity, dark theme, and component structure of the Version 1 frontend in the Version 2 application.

## When to Use This Skill

Activate this skill whenever:
- Creating or editing React components in `frontend/src/components/`.
- Creating or updating pages in `frontend/src/pages/`.
- Styling elements in `frontend/src/styles/`.
- Connecting React pages to backend REST endpoints in `frontend/src/api/emailApi.js`.

---

## 1. How to Inspect and Apply the Reference Design

Before creating or editing frontend code, review the [frontend-reference-guide.md](file:///c:/Users/Aniket%20Lohar/Documents/Web/github-repos/email-forensics/docs/ai/frontend-reference-guide.md).

Always verify:
1. **Color Palette:** Use established CSS custom properties or design constants:
   - Primary Accent: `#3b82f6` (blue-500)
   - Background Canvas: `#0f172a` (slate-900)
   - Card/Surface Background: `#1e293b` (slate-800)
   - Elevated Surface Background: `#334155` (slate-700)
   - Border Color: `#334155` (slate-700) or `#475569` (slate-600)
   - Text Primary: `#f8fafc` (slate-50)
   - Text Muted/Secondary: `#94a3b8` (slate-400)
   - Danger/Malicious: `#ef4444` (red-500)
   - Warning/Suspicious: `#f59e0b` (amber-500)
   - Success/Benign: `#10b981` (emerald-500)
2. **Typography:** Standard system sans-serif font stack (`Inter`, system-ui, -apple-system, sans-serif).
3. **Card Styling:** Rounded corners (`border-radius: 0.5rem` or `0.75rem`), subtle borders, dark background, padding of `1.25rem` to `1.5rem`.
4. **Button Styling:**
   - Primary: Blue background, white text, hover state with slight brightness increase, disabled state with reduced opacity and cursor not-allowed.
   - Danger: Red background for declining or stopping actions.
   - Secondary: Dark surface with border, light text.

---

## 2. Page Hierarchy and Layout Standards

The Version 2 frontend consists of four focused pages:

### 1. UploadPage (`UploadPage.jsx`)
- Header with project title and brief description.
- Main upload card with drag-and-drop file zone.
- File selection feedback displaying file name and human-readable size.
- Consent checkboxes:
  - `allowExternalLookups`: Checkbox for agreeing to send extracted indicators to external threat databases.
  - `requestAi`: Checkbox for requesting AI analysis (explaining that a redacted preview will be presented for approval).
- Large primary action button ("Analyse Email").

### 2. AnalysisPage (`AnalysisPage.jsx`)
- Active progress indicator tracking the 6 main pipeline stages:
  1. Saving original evidence
  2. Parsing MIME headers and content
  3. Verifying mail authentication (SPF, DKIM, DMARC, ARC)
  4. Performing external threat intelligence lookups
  5. Generating AI threat evaluation (if approved)
  6. Finalising investigation and score
- Polling mechanism calling `GET /api/emails/{id}` every 2 seconds.
- Modal or inline card displaying `RedactionPreview` when status reaches `WAITING_FOR_AI_APPROVAL`.

### 3. ResultPage (`ResultPage.jsx`)
- Header summary card: File name, SHA-256 hash, sender, subject, date.
- Risk Score Card: Numeric score (0-100), risk badge (LOW, GUARDED, ELEVATED, HIGH, CRITICAL), and confidence label (LOW, MEDIUM, HIGH).
- Tabbed or multi-section layout:
  - **Authentication Tab:** SPF, DKIM, DMARC, ARC verdicts and Received hop chain.
  - **Indicators Tab:** Table of extracted URLs, domains, and IPs with individual provider verdicts.
  - **Image & QR Tab:** OCR extracted text and decoded QR destination URLs.
  - **AI Assessment Tab:** Plain-English summary, classification, detected fraud signals, limitations, and recommendations.
  - **Risk Factors Tab:** Itemized list of scoring factors and points added.
  - **Related Emails Tab:** Table of historical emails sharing matched indicators.
- Download PDF report button.

### 4. HistoryPage (`HistoryPage.jsx`)
- Paginated table showing previous investigations: Email ID, file name, sender, date, risk score, verdict, and link to view full results.

---

## 3. API Communication Pattern

Centralize all API calls in `src/api/emailApi.js` using the browser's native `fetch()` API:

```javascript
export async function uploadEmail(file, options) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('allowExternalLookups', options.allowExternalLookups);
  formData.append('requestAi', options.requestAi);

  const response = await fetch('/api/emails/analyze', {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.message || 'Failed to upload email');
  }

  return response.json();
}

export async function getEmailStatus(emailId) {
  const response = await fetch(`/api/emails/${emailId}`);
  if (!response.ok) {
    throw new Error('Failed to fetch status');
  }
  return response.json();
}
```

---

## 4. State Management and Custom Hook Pattern

Manage state cleanly using the `useEmailAnalysis` hook without adding external state libraries:
- Manage `currentEmailId`, `status`, `stage`, `results`, `error`, and `loading`.
- Include safe cleanup logic to cancel pending `setInterval` polling when navigating away or unmounting.
