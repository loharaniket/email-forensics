---
trigger: always_on
---

# MVP Development Rules: Simplicity over Enterprise Bloat

The Email Forensics Version 2 application is an academic and functional MVP (Minimum Viable Product). These rules keep the project simple, achievable, and maintainable.

## 1. Strictly Prohibited Technologies

Do NOT introduce any of the following enterprise technologies unless explicitly requested and approved:
- **No Microservices:** Maintain a single modular monolith for the backend.
- **No Message Brokers:** No Apache Kafka, RabbitMQ, ActiveMQ, or external queuing systems. Use Spring's built-in in-memory `ThreadPoolTaskExecutor`.
- **No Distributed Caching:** No Redis, Memcached, or Hazelcast. Use simple in-memory maps or database lookups if caching is needed.
- **No Container Orchestration:** No Kubernetes, Helm charts, or complex multi-container meshes during standard development.
- **No Security Frameworks in Version 2:** No Spring Security, OAuth2, JWT filters, user accounts, login portals, or role-based access control. The application is run locally on `localhost` or in a private lab network.
- **No Graph Databases:** No Neo4j or Amazon Neptune. Historical correlation is handled with standard PostgreSQL relational queries and displayed in normal tables.
- **No Cloud Bloat:** Do not introduce AWS/GCP managed services that prevent local execution. The entire project must run on a developer laptop with Java, Node.js, and local PostgreSQL.

## 2. Two PostgreSQL Tables Only

Keep the relational schema strictly limited to the two approved tables:
1. `emails`: Contains the primary email investigation records, original file hashes, status, score, verdict, and JSONB fields for authentication and AI assessment.
2. `indicators`: Contains extracted indicators (URLs, domains, IPs, QR URLs, OCR text) and compact provider observations in JSONB.

Do not create additional tables for users, roles, sessions, campaigns, cases, organizations, or raw HTTP responses.

## 3. Minimal Validation Principles

Validation must remain practical and proportional to an MVP:
- Validate input existence (file attached, parameters present).
- Validate `.eml` extension and reject archives/executables.
- Enforce basic file size bounds (10 MB upload limit).
- Catch parsing exceptions gracefully.
- Do not build complex custom validation annotation processors or multi-layer validator pipelines.

## 4. Prioritize Working, Testable Code

- Implement features one small phase at a time following the roadmap.
- Every feature must be verifiable with unit tests or safe fixtures before moving to the next stage.
- Do not create placeholder files, stubs, or empty classes for future phases ahead of time.
- If a simpler approach works reliably, choose the simpler approach over theoretical "enterprise future-proofing".
