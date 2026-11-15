---
trigger: always_on
---

# MVP Development Rules: Simplicity over Enterprise Bloat

The Email Forensics Version 2 application is an academic and functional MVP (Minimum Viable Product). These rules keep the project simple, achievable, and maintainable.

Consult the authoritative specification in [docs/architecture-v2.md](docs/architecture-v2.md).

---

## 1. Strictly Prohibited Technologies and Excluded Features

Do NOT introduce any of the following enterprise technologies or features under any circumstance:
- **No User Management:** No user registration, login, user accounts, roles, permissions, or session tracking.
- **No Security Frameworks in Version 2:** No Spring Security, OAuth2, JWT filters, or access control interceptors. Backend APIs run on `localhost` or a private lab network.
- **No Case Management:** No case files, analyst assignments, or investigation ownership tables.
- **No Campaign Management:** No campaign tables. Historical grouping is derived dynamically via PostgreSQL queries.
- **No Graph Databases or Visualizations:** No Neo4j, Amazon Neptune, or frontend network graphs. Historical correlation is displayed as clean tables.
- **No Microservices:** Maintain a single modular monolith for the backend.
- **No Message Brokers:** No Apache Kafka, RabbitMQ, ActiveMQ, or external queuing systems. Use Spring's in-memory `ThreadPoolTaskExecutor`.
- **No Distributed Caching:** No Redis, Memcached, or Hazelcast.
- **No Container Orchestration:** No Kubernetes, Helm charts, or multi-container meshes during standard development.
- **No Non-Image Attachment Scanning:** No malware execution, sandboxing, or Office/archive deep scanning. Non-image attachments are ignored in Version 2.
- **No Attachment SHA-256 Scanning:** Attachment files are not separately hashed or submitted to virus scanners in Version 2.
- **No Cloud Bloat:** Do not introduce cloud-managed services that prevent local execution. The entire project must run on a developer laptop with Java 21, Node.js 22+, and local PostgreSQL.

---

## 2. Exactly Two PostgreSQL Tables Only

Keep the relational schema strictly limited to the two approved tables:
1. `emails`: Contains the primary email investigation records, original file hashes, status, score, verdict, and JSONB fields for authentication and AI assessment.
2. `indicators`: Contains extracted indicators (URLs, domains, IPs, QR URLs, OCR text) and compact provider observations in JSONB.

Do not create additional tables for users, roles, sessions, campaigns, cases, organizations, or raw HTTP responses.

---

## 3. Minimal Validation Principles

Validation must remain practical and proportional to an MVP:
- Validate input existence (file attached, parameters present).
- Validate `.eml` extension and reject archives/executables.
- Enforce basic file size bounds (10 MB upload limit).
- Catch parsing exceptions gracefully.
- Do not build complex custom validation annotation processors or multi-layer validator pipelines.

---

## 4. Prioritize Working, Testable Code

- Implement features one small phase at a time following the roadmap.
- Every feature must be verifiable with unit tests or safe fixtures before moving to the next stage.
- Do not create placeholder files, stubs, or empty classes for future phases ahead of time.
- If a simpler approach works reliably, choose the simpler approach over theoretical "enterprise future-proofing".
