---
trigger: model_decision
description: Rules for React frontend development, preserving the Version 1 visual design and dark theme while building brand-new V2 components.
---

# Frontend Development Rules

These rules apply to all frontend development, styling, and UI components in the React application.

Consult the authoritative specification in [docs/architecture-v2.md](../../docs/architecture-v2.md) before implementing frontend features.

---

## 1. Version 1 is a Visual Reference Only (Do Not Copy V1 Code)

- The existing Version 1 frontend located in `frontend/` is a **VISUAL REFERENCE ONLY**.
- Take visual inspiration from Version 1 in these specific areas:
  - Overall visual theme and dark colour palette.
  - Colour combinations and contrast levels.
  - Typography and spacing scale.
  - Card designs, button designs, and border radiuses.
  - Navigation style and table appearance.
  - Overall professional cybersecurity dashboard appearance.
- **STRICT PROHIBITION ON COPYING V1 CODE:**
  - Agents must **NOT** copy V1 frontend source code or reuse its legacy components as they are.
  - Agents must design **BRAND-NEW** Version 2 React components with clean, simple, beginner-friendly code.
  - During future frontend development, agents may inspect V1 frontend files in read-only mode solely to understand visual styling and design choices.
  - Never copy old business logic, legacy API integrations, old state management, or obsolete V1 features.

---

## 2. Technology Stack and Dependencies

- **Framework:** React with Vite.
- **Styling:** Standard modern CSS matching the design tokens in [docs/ai/frontend-reference-guide.md](../../docs/ai/frontend-reference-guide.md).
- **Icons:** `lucide-react` for consistent interface icons.
- **HTTP Client:** Use the browser's native `fetch()` API. Do not install Axios or heavy HTTP wrappers.
- **State Management:** Use standard React component state (`useState`, `useEffect`) and lightweight custom hooks (such as `useEmailAnalysis`). Do not install Redux, Zustand, or large state stores.
- **Routing:** Standard `react-router-dom` for navigating between pages.

---

## 3. Preserving Component and Page Structure

Build new, clean components for the four primary Version 2 pages:
1. **UploadPage (`UploadPage.jsx`):** File upload card with `.eml` drag-and-drop or selection, file name and size display, external intelligence lookup consent checkbox, AI analysis consent checkbox, and an Analyse button.
2. **AnalysisPage (`AnalysisPage.jsx`):** Progress tracking screen showing step-by-step pipeline stages (Saving, Parsing, Authentication, Threat Checks, AI Analysis, Finalising), polling the backend every 2 seconds, and presenting the Redaction Preview modal when approval is requested.
3. **ResultPage (`ResultPage.jsx`):** Investigation dashboard displaying email summary, authentication verdict, URL/domain findings, IP reputation, OCR/QR results, AI evaluation summary, breakdown of risk score factors, and historical related emails table.
4. **HistoryPage (`HistoryPage.jsx`):** Paginated table of previously analyzed email records.

---

## 4. UI Component Design Principles

- Keep components small, focused, and single-purpose.
- Build clean, standalone V2 components: `FileUpload`, `ProgressSteps`, `EmailSummary`, `AuthenticationResults`, `ReceivedChain`, `UrlFindings`, `IpFindings`, `ImageFindings`, `AiAssessment`, `RiskScoreCard`, `RiskFactors`, `RelatedEmails`, `RedactionPreview`, and `ErrorNotice`.
- Handle all UI states cleanly:
  - **Loading state:** Clear spinners or progress badges.
  - **Empty state:** Meaningful empty state messages when no indicators or related emails are found.
  - **Error state:** User-friendly alert banners with actionable error descriptions.
  - **Unknown/Unchecked state:** Never display `UNKNOWN` or `NOT_CHECKED` as safe or benign. Always use neutral styling.

---

## 5. Security and API Integration Guardrails

- All backend API requests must be centralized in `src/api/emailApi.js`. Components must not construct API URLs directly.
- The frontend must NEVER communicate directly with external threat intelligence APIs. All provider calls and API keys remain on the Spring Boot backend.
- Never render raw unescaped HTML from email content directly in the browser DOM. Always render plain text or sanitize thoroughly inside a sandboxed iframe.
- Never write code comments inside JSX, JavaScript, or CSS files.
