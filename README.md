# PayPink: Core Retail Ledger & Balance Mutation Engine

**FSE Capstone Project**: High-Throughput Relational Balance Mutation, Distributed In-Memory Validation, Transactional Outbox Kafka Streaming, and PostgreSQL Immutable Audit Trail.

---

## System Architecture Overview

```
1. Client Layer      ──> PayPink SPA / JMeter (800+ TPS Load Generator)
2. Edge Layer        ──> API Gateway & Stateless JWT Perimeter Filter
3. Business Layer    ──> Transaction Service, Account Service, Reconciliation Service (@Scheduled 15-Min), Notification Service
4. Data Layer        ──> Oracle XE 21c (Master OLTP & Security Audit), PostgreSQL 15+ (Immutable Audit), Redis (≤5ms Idempotency)
5. Event Layer       ──> Transactional Outbox Pattern + Apache Kafka Event Streaming
6. Observability     ──> OpenTelemetry SDK, Prometheus (Port 9090), Grafana Dashboards (Port 3000)
```

---

## Database Schema & ERD Topology

* **Oracle XE 21c (Master OLTP & Security Engine)**:
  * `CUSTOMER` - User accounts & credentials.
  * `ACCOUNT` - Master balance state (`NUMBER(18,4)` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`).
  * `TRANSACTION` - Master financial transaction records.
  * `OUTBOX_EVENT` - Transactional Outbox table for event-driven streaming.
  * `AUDIT_LOG` - **Synchronous Security & Operational Audit** for immediate local ACID non-repudiation.
* **PostgreSQL 15+ (Immutable Audit & Downstream Store)**:
  * `LEDGER_MUTATION_AUDIT` - Append-only double-entry financial audit entries (`before_balance`, `after_balance`, `DEBIT`/`CREDIT`).
  * `RECONCILIATION_LOG` - Cross-database drift detection logs (`MATCHED` / `DRIFT_DETECTED`).
  * `NOTIFICATION` - Customer alert delivery status history (`SENT`, `FAILED`, `RETRY`).
* **Redis In-Memory**:
  * `idempotency:{key}` - Token & key validation matrix achieving **≤5ms** turnaround.

---

## Quick Start Guide

### 1. Run via Docker Compose (All 8 Containers)
```bash
cd docker
docker compose up -d
```

### 2. Run Backend Locally with Maven
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
cd backend
mvn spring-boot:run
```

### 3. Open Banking Web Application (SPA UI)
Open `frontend/index.html` in your browser.

---

## Automated Testing & Concurrency Verification
```powershell
cd backend
mvn test
```

* **Validation & Boundary Tests**: Verifies JSR-380 `@Digits(integer=14, fraction=4)` and `@Positive` constraints returning RFC-7807 Problem Details.
* **Pessimistic Lock Concurrency Test**: Simulates 10 simultaneous threads debiting ₱50 from a ₱60 balance, proving `@Lock(PESSIMISTIC_WRITE)` prevents race conditions and overdrafts (0% overdraft, exactly ₱10.0000 final balance).
* **Idempotency & Outbox Tests**: Validates Redis ≤5ms turnaround and Transactional Outbox $\rightarrow$ Kafka $\rightarrow$ PostgreSQL propagation.

---

##  Repository Structure
```
core-retail-ledger-fse/
├── backend/          # Spring Boot 3.2 Microservices & Core Mutation Engine
├── frontend/         # PayPink Philippine FinTech Banking SPA (Pink Theme)
├── docker/           # Docker Compose, Prometheus config, and JMeter stress test
└── docs/             # Architecture Specifications, JIRA Sprint Backlog, and SonarQube Report
```
