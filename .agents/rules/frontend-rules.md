---
trigger: model_decision
description: Rules for React frontend development, preserving the Version 1 visual design, dark theme, styling, and reusable components.
---

# Frontend Development Rules

These rules apply to all frontend development, styling, and UI components in the React application.

## 1. Primary Design Reference: Version 1 Frontend

- The existing Version 1 frontend located in `frontend/` is the authoritative visual and UX reference for Version 2.
- Always inspect the existing frontend components and styling before designing or writing new UI code.
- Maintain the exact same visual identity: dark theme, color palette, typography, border radius, button styling, card structures, and spacing hierarchy.
- Do not introduce random new color schemes, alternative CSS frameworks, or redesign the user interface without explicit approval.

## 2. Technology Stack and Dependencies

- **Framework:** React with Vite.
- **Styling:** Standard CSS or CSS modules matching the existing style conventions.
- **Icons:** `lucide-react` for consistent system icons where needed.
- **HTTP Client:** Use the browser's native `fetch()` API. Do not install Axios or heavy HTTP wrapper libraries.
- **State Management:** Use standard React component state (`useState`, `useEffect`) and lightweight custom hooks (such as `useEmailAnalysis`). Do not install Redux, Zustand, or large state stores.
- **Routing:** Standard `react-router-dom` for navigating between pages.

## 3. Preserving Component and Page Structure

The application consists of four primary pages:
1. **UploadPage (`UploadPage.jsx`):** File upload card with `.eml` drag-and-drop or selection, file name and size display, external intelligence lookup consent checkbox, AI analysis consent checkbox, and an Analyse button.
2. **AnalysisPage (`AnalysisPage.jsx`):** Progress tracking screen showing step-by-step pipeline stages (Saving, Parsing, Authentication, Threat Checks, AI Analysis, Finalising), polling the backend every 2 seconds, and presenting the Redaction Preview modal when approval is requested.
3. **ResultPage (`ResultPage.jsx`):** Investigation dashboard displaying email summary, authentication verdict, URL/domain findings, IP reputation, OCR/QR results, AI evaluation summary, breakdown of risk score factors, and historical related emails table.
4. **HistoryPage (`HistoryPage.jsx`):** Paginated table of previously analyzed email records.

## 4. UI Component Design Principles

- Keep components small, focused, and reusable.
- Extract common UI elements into dedicated components: `FileUpload`, `ProgressSteps`, `EmailSummary`, `AuthenticationResults`, `ReceivedChain`, `UrlFindings`, `IpFindings`, `ImageFindings`, `AiAssessment`, `RiskScoreCard`, `RiskFactors`, `RelatedEmails`, `RedactionPreview`, and `ErrorNotice`.
- Always handle all UI states cleanly:
  - **Loading state:** Clear spinners or progress badges.
  - **Empty state:** Meaningful empty state messages when no indicators or related emails are found.
  - **Error state:** User-friendly alert banners with actionable error descriptions.
  - **Unknown/Unchecked state:** Never display `UNKNOWN` or `NOT_CHECKED` as safe or benign.

## 5. Security and API Integration Guardrails

- All backend API requests must be centralized in `src/api/emailApi.js`. Components must not construct API URLs directly.
- The frontend must NEVER communicate directly with external threat intelligence APIs. All provider calls and API keys remain on the Spring Boot backend.
- Never render raw unescaped HTML from email content directly in the browser. Always render plain text or sanitize thoroughly inside a sandboxed iframe.
- Never write code comments inside JSX, JavaScript, or CSS files.
