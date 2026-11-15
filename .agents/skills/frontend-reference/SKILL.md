---
name: frontend-reference
description: Use this skill when building or styling React frontend pages, components, hooks, or API client code to match the Version 1 visual design and design tokens.
---

# Skill: Frontend Design Reference and UI Implementation

This skill provides step-by-step guidance on how to replicate and maintain the visual identity, dark theme, and design tokens of the Version 1 frontend while writing clean, brand-new Version 2 components.

Consult the authoritative specification in [docs/architecture-v2.md](docs/architecture-v2.md) and design tokens in [docs/ai/frontend-reference-guide.md](docs/ai/frontend-reference-guide.md).

---

## 1. Version 1 is a Visual Reference Only (Do Not Copy Code)

The existing Version 1 frontend located in `frontend/` is a **VISUAL REFERENCE ONLY**.
- **Take Design Inspiration From:**
  - Dark theme and Slate color palette.
  - High-contrast typography and spacing hierarchy.
  - Border radius, button styling, card styling, and table layouts.
  - Cybersecurity dashboard look and feel.
- **STRICT PROHIBITION ON COPYING V1 CODE:**
  - Do **NOT** copy or paste V1 JSX, JavaScript, or CSS files.
  - Do **NOT** reuse V1 components as they are.
  - Build **BRAND-NEW** React components for Version 2 with simple, clean, beginner-friendly code.
  - Never copy legacy V1 business logic, state management, or old API clients.
  - Never write comments inside source code files.

---

## 2. Design Tokens and Colors

Use these standard colors from [docs/ai/frontend-reference-guide.md](docs/ai/frontend-reference-guide.md):
- Primary Accent: `#3b82f6` (blue-500)
- Background Canvas: `#0f172a` (slate-900)
- Card/Surface Background: `#1e293b` (slate-800)
- Elevated Surface: `#334155` (slate-700)
- Border Color: `#334155` (slate-700) or `#475569` (slate-600)
- Text Primary: `#f8fafc` (slate-50)
- Text Muted/Secondary: `#94a3b8` (slate-400)
- Danger/Malicious: `#ef4444` (red-500)
- Warning/Suspicious: `#f59e0b` (amber-500)
- Success/Benign: `#10b981` (emerald-500)
- Unknown/Unchecked: `#64748b` (slate-500) – **Never display Unknown as green/benign.**

---

## 3. Four Core Pages in Version 2

Build fresh, dedicated components for the four primary pages:
1. **UploadPage (`UploadPage.jsx`):** File upload card with `.eml` drag-and-drop, size display, external lookup consent checkbox, AI analysis consent checkbox, and an Analyse button.
2. **AnalysisPage (`AnalysisPage.jsx`):** Step-by-step progress tracking (Saving, Parsing, Authentication, Threat Checks, AI Analysis, Finalising), polling `/api/emails/{id}` every 2 seconds, and displaying the Redaction Preview modal when approval is requested.
3. **ResultPage (`ResultPage.jsx`):** Investigation dashboard displaying summary, authentication results, URL/domain findings, IP reputation, OCR/QR results, AI evaluation summary, breakdown of risk score factors, and historical related emails table.
4. **HistoryPage (`HistoryPage.jsx`):** Paginated table of previously analyzed email records.

---

## 4. API Communication Pattern

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

## 5. State Management

Manage state cleanly using standard React hooks and a dedicated custom hook (`useEmailAnalysis`):
- Manage `currentEmailId`, `status`, `stage`, `results`, `error`, and `loading`.
- Include cleanup logic to cancel pending `setInterval` polling when navigating away or unmounting.
- Do not install Redux, Zustand, or complex global stores.
- Never execute `git commit` or `git push`.
