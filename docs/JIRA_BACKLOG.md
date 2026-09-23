# JIRA Sprint Backlog & Execution Blueprint (Days 31–37)

## Project: Capstone FSE - Core Retail Ledger & Balance Mutation Engine
**Team**: Full-Stack Engineering (FSE)  
**Methodology**: Agile Scrum & Microservices Architecture

---

### Sprint 1 (Days 31–34): Architecture, Core Engine & Perimeter Security

| Issue Key | Type | Summary & Acceptance Criteria | Story Points | Assignee | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **PAY-101** | Story | **API Schemas & Contract Engineering**: Define OpenAPI specifications, strict JSR-380 validation annotations (`@Digits(14,4)`, `@Positive`), and RFC-7807 Problem Details schemas. | 5 SP | Backend Team | **DONE** |
| **PAY-102** | Story | **Relational Schemas & HikariCP Tuning**: Formulate Oracle XE DDL (`CUSTOMER`, `ACCOUNT`, `TRANSACTION`, `OUTBOX_EVENT`, `AUDIT_LOG`) and PostgreSQL DDL (`LEDGER_MUTATION_AUDIT`, `RECONCILIATION_LOG`, `NOTIFICATION`). Configure HikariCP (`maximum-pool-size=30`, `minimum-idle=5`). | 8 SP | Database Team | **DONE** |
| **PAY-103** | Story | **Pessimistic Row Lock Mutation Engine**: Annotate JPA repository with `@Lock(LockModeType.PESSIMISTIC_WRITE)` executing `SELECT ... FOR UPDATE` to serialize simultaneous double-spend debits and prevent overdraft. | 13 SP | Core Engine Lead | **DONE** |
| **PAY-104** | Story | **Stateless Perimeter Security & JWT Filter**: Implement Spring Security stateless filter, decoding bearer tokens and assigning roles (`ROLE_CUSTOMER`, `ROLE_TELLER`, `ROLE_AUDITOR`). | 5 SP | Security Lead | **DONE** |
| **PAY-105** | Story | **In-Memory Idempotency Matrix (Redis)**: Implement interceptor checking idempotency keys with $\le 5\text{ms}$ turnaround time and zero duplicate state mutation. | 8 SP | Backend Team | **DONE** |

---

### Sprint 2 (Days 35–37): Outbox Streaming, Hardening, SPA UI & Live Assessment

| Issue Key | Type | Summary & Acceptance Criteria | Story Points | Assignee | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **PAY-201** | Story | **Transactional Outbox & Kafka Publisher**: Implement single local ACID commit on Oracle XE and background polling publisher streaming events to Kafka topics. | 8 SP | Event Stream Lead | **DONE** |
| **PAY-202** | Story | **PostgreSQL Immutable Audit Consumer**: Asynchronous consumer appending to `LEDGER_MUTATION_AUDIT` with before/after balance snapshots. | 5 SP | Backend Team | **DONE** |
| **PAY-203** | Story | **@Scheduled Cross-Database Reconciliation**: Implement 15-minute scheduled sweep comparing Oracle `TRANSACTION` vs PostgreSQL `LEDGER_MUTATION_AUDIT`, logging drift in `RECONCILIATION_LOG`. | 8 SP | Backend Team | **DONE** |
| **PAY-204** | Story | **Philippine FinTech Banking SPA (PayPink)**: Build responsive Rose/Pink themed interface with live double-spend race condition simulator, 11-step event visualizer, and telemetry gauges. | 13 SP | Frontend Lead | **DONE** |
| **PAY-205** | Story | **Stress Testing & JMeter Verification**: Configure Apache JMeter test plan for 800+ TPS and run JUnit concurrency verification suites. | 5 SP | QA / Perf Lead | **DONE** |
| **PAY-206** | Story | **Final Code Sweeps & Milestone 5 Assessment**: Eliminate runtime print statements, generate SonarQube quality report, and finalize live presentation deck. | 3 SP | FSE Team | **DONE** |

**Total Estimated Velocity**: 78 Story Points Completed
