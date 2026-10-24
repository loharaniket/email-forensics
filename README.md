# AI-Powered Email Forensic Investigation and Threat Correlation

An academic forensic analysis and threat intelligence platform designed to ingest `.eml` email files, preserve evidence integrity (SHA-256 hash validation), extract technical and content indicators, correlate findings across prior cases, compute explainable risk scores, and present structured forensic intelligence.

---

## Current Status: Phase 0 — Setup and Stable Skeleton

Phase 0 establishes a verified baseline development skeleton:
- **Backend:** Spring Boot 4.1.1 modular monolith with Java 21 LTS and Maven wrapper.
- **Frontend:** React 19 with plain JavaScript/JSX (no TypeScript), Vite 8, and Tailwind CSS v4.
- **Health Gateway:** `GET /api/v1/health` responding with `{ "status": "ok" }`.
- **Reverse Proxy:** Vite dev proxy mapping `/api` requests from `http://localhost:5173` to `http://localhost:8080`.
- **Resilient UI:** Analyst dashboard showing real-time connectivity status, round-trip latency, last checked timestamp, and connection error handling.

---

## Technology Stack Baseline

| Area | Coordinate / Version | Purpose |
|---|---|---|
| Runtime | Java 21 LTS | Backend runtime |
| Backend Framework | Spring Boot 4.1.1 | Modular monolith API |
| Build Tool | Apache Maven (Maven Wrapper) | Backend dependency management |
| Frontend Framework | React 19.3.x (JavaScript / JSX) | Analyst user interface |
| Frontend Bundler | Vite 8.x | Frontend build system & dev proxy |
| Styling | Tailwind CSS 4.x | Utility-first responsive design |
| HTTP Client | Axios 1.x | Client API requests |
| Database | PostgreSQL 16 (Phase 1+) | Structured forensic findings |
| Migration Tool | Flyway (Phase 1+) | Versioned database migrations |

---

## Prerequisites

Ensure the following tools are installed on your workstation:
- **Java Development Kit (JDK):** Version 21 or higher (`java -version`)
- **Node.js:** Current LTS (v20.19+ or v22.12+) (`node -v`, `npm -v`)
- **Git:** (`git --version`)
- **PostgreSQL:** Version 16 or compatible (`psql --version`)

---

## Quick Start Guide

### 1. Running the Backend

From the repository root:

```bash
cd backend
# Windows:
mvnw.cmd spring-boot:run

# Linux / macOS:
./mvnw spring-boot:run
```

The Spring Boot backend will start on **port 8080**.
Verify the health endpoint directly:
```bash
curl http://localhost:8080/api/v1/health
# Expected Output: {"status":"ok"}
```

Run backend unit and integration tests:
```bash
cd backend
mvnw.cmd test
```

### 2. Running the Frontend

From the repository root in a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server will start on **http://localhost:5173**.
Open your browser to `http://localhost:5173` to view the Analyst Health Dashboard.

Build frontend for production:
```bash
cd frontend
npm run build
```

---

## Phase 0 Completion Gate Verification

1. **Start Backend:** Run `mvnw.cmd spring-boot:run` in `backend/` (runs on `http://localhost:8080`).
2. **Start Frontend:** Run `npm run dev` in `frontend/` (runs on `http://localhost:5173`).
3. **Verify Successful Connection:**
   - Navigate to `http://localhost:5173` in a web browser.
   - The UI should display `GATEWAY: ONLINE` and `STATUS: OK` with response payload `{"status":"ok"}` and measured latency.
4. **Verify Error Handling:**
   - Stop or pause the backend process.
   - In the React frontend, trigger **Ping Endpoint** (or wait for auto-refresh).
   - The UI displays `GATEWAY: OFFLINE` with an alert card indicating the connection failure, rather than crashing or showing a blank page.
5. **Recover Connection:**
   - Restart the backend on port 8080 and click **Ping Endpoint**. The UI should seamlessly recover to the healthy green operational status.

---

## Project Structure

```text
email-forensics/
├── docs/
│   ├── AGENT.md
│   └── Implementation_plan.pdf
├── backend/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/
│       ├── main/
│       │   ├── java/com/emailforensics/backend/
│       │   │   ├── BackendApplication.java
│       │   │   ├── config/
│       │   │   │   └── CorsConfig.java
│       │   │   └── health/
│       │   │       ├── HealthController.java
│       │   │       └── HealthResponse.java
│       │   └── resources/
│       │       └── application.properties
│       └── test/java/com/emailforensics/backend/
│           ├── BackendApplicationTests.java
│           └── health/
│               └── HealthControllerTest.java
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── index.css
│       └── api/
│           ├── client.js
│           └── health.js
├── .env.example
├── .gitignore
└── README.md
```

---

## Implementation Roadmap

- [x] **Phase 0:** Setup and stable skeleton (Spring Boot 4.1.x + React 19 + Health API)
- [ ] **Phase 1:** Database, login, and case management (PostgreSQL 16, Flyway, JWT, Cases)
- [ ] **Phase 2:** Evidence upload and vault preservation (SHA-256 integrity, safe storage)
- [ ] **Phase 3:** MIME parsing and first full investigation slice
- [ ] **Phase 4:** Headers and Received hop chronology
- [ ] **Phase 5:** URL / DNS / IP / ASN / GeoIP intelligence
- [ ] **Phase 6:** OCR and QR code payload decoding
- [ ] **Phase 7:** Content rules, prompt injection alerts, and Tribuo baseline
- [ ] **Phase 8:** Explainable risk scoring and evidence confidence
- [ ] **Phase 9:** Historical correlation, graph visualization, and campaigns
- [ ] **Phase 10:** Spring AI grounded assessment
- [ ] **Phase 11:** Consolidated forensic reports and final regression
