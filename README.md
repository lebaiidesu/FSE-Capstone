# PayPink: Core Retail Ledger & Microservices Balance Mutation Engine

**FSE Capstone Project (Unified Master Edition)**: High-Throughput Relational Balance Mutation, Distributed In-Memory Validation, Transactional Outbox Kafka Streaming, PostgreSQL Immutable Audit Trail, Full Telemetry Observability, and Clean Multi-Module Microservices Architecture.

---

## 🏛️ System Architecture Overview

```
1. Client Layer      ──> PayPink Banking SPA / JMeter (800+ TPS Load Generator)
2. Edge Layer        ──> Spring Cloud API Gateway (Port 8080) & JWT Perimeter Security
3. Business Layer    ──> Auth Service (8081), Account Service (8082), Transaction Service (8083), Outbox Publisher (8087), Analytics Service (8088)
4. Data Layer        ──> Oracle XE 21c (Master OLTP & ACID Ledger), PostgreSQL 15+ (Immutable Audit), Redis 7 (≤5ms Idempotency)
5. Event Layer       ──> Transactional Outbox Pattern + Apache Kafka Event Streaming (Audit 8085, Notification 8084, Reconciliation 8086)
6. Observability     ──> OpenTelemetry Collector, Prometheus (Port 9090), Loki (Port 3100), Tempo (Port 3200), Grafana (Port 3000)
```

---

## 📊 Database Topology

* **Oracle XE 21c (Master OLTP & Source of Truth)**:
  * `CUSTOMER` - Customer accounts & credentials.
  * `ACCOUNT` - Master balance state (`NUMBER(18,4)` with `@Lock(PESSIMISTIC_WRITE)`).
  * `TRANSACTION` - Master financial transaction ledger.
  * `OUTBOX_EVENT` - Transactional Outbox table for event-driven Kafka streaming.
  * `AUDIT_LOG` - Synchronous security & operational audit log.

* **PostgreSQL 15+ (Immutable Audit & Event Store)**:
  * `LEDGER_MUTATION_AUDIT` - Append-only double-entry financial audit trail.
  * `RECONCILIATION_LOG` - Cross-database drift detection logs (`MATCHED` / `DRIFT_DETECTED`).
  * `NOTIFICATION` - Customer alert delivery status history (`SENT`, `FAILED`, `RETRY`).

* **Redis 7 (In-Memory Matrix)**:
  * `idempotency:tx:{key}` - Token & key validation matrix achieving **≤5ms turnaround**.

---

## 🚀 Microservice Port & Module Topology

| Module | Port | Technology Stack | Responsibility |
|---|---|---|---|
| **`ledger-common`** | N/A | Java 17 / 21 | Shared DTOs, Outbox Event Models, RFC-7807 ProblemDetails |
| **`api-gateway`** | `8080` | Spring Cloud Gateway, Redis | Edge routing, JWT validation, rate limiting |
| **`auth-service`** | `8081` | Spring Boot, JJWT, Oracle XE | User authentication, token issuance |
| **`account-service`** | `8082` | Spring Boot, JPA, Oracle XE | Customer account details & balance inquiry |
| **`transaction-service`** | `8083` | Spring Boot, Redis, Oracle XE | Balance mutation engine, pessimistic row lock, outbox event generation |
| **`notification-service`** | `8084` | Spring Boot, Kafka, PostgreSQL | Asynchronous SMS/Email alerts |
| **`audit-service`** | `8085` | Spring Boot, Kafka, PostgreSQL | Consumes Kafka events, writes immutable financial audit logs |
| **`reconciliation-service`**| `8086` | Spring Boot, Kafka, Oracle + Postgres | Scheduled sweep (15-min) comparing Oracle vs PostgreSQL for drift detection |
| **`outbox-publisher`** | `8087` | Spring Boot, Kafka, Oracle XE | Polling worker scanning `OUTBOX_EVENT` and streaming to Kafka |
| **`analytics-service`** | `8088` | Spring Boot, Kafka | Real-time metric aggregation & ledger insights |

---

## 🧪 Automated Concurrency & Reliability Verification

Run automated Maven test suites:
```powershell
cd backend
mvn test
```

### Verified Scenarios:
1. **Pessimistic Lock Concurrency Test (`PessimisticLockConcurrencyTest.java`)**:
   Simulates 10 simultaneous threads debiting ₱50 from a ₱60 balance, proving `@Lock(PESSIMISTIC_WRITE)` prevents race conditions and double-spending (0% overdraft, exactly ₱10.0000 final balance).
2. **Validation & Boundary Test (`ValidationAndBoundaryTest.java`)**:
   Verifies JSR-380 `@Digits(integer=14, fraction=4)` and `@Positive` constraints returning RFC-7807 Problem Details.
3. **Idempotency & Outbox Test (`IdempotencyAndOutboxTest.java`)**:
   Validates Redis ≤5ms turnaround and Transactional Outbox $\rightarrow$ Kafka $\rightarrow$ PostgreSQL propagation.
4. **Reconciliation & Audit Test (`ReconciliationAndAuditTest.java`)**:
   Validates cross-database drift detection and append-only immutable audit entries.

---

## ⚡ Quick Start Guide

### 1. Build Multi-Module JARs
```powershell
cd backend
mvn clean package -DskipTests
```

### 2. Launch All Microservice Containers & Telemetry Stack
```powershell
cd docker
docker compose up -d --build
```

### 3. Verify Health & Topology
```powershell
cd scripts
.\verify_microservices.ps1
```

### 4. Launch PayPink Banking SPA UI
Open `frontend/index.html` in your browser.

---

## 📁 Repository Structure
```
FSE-Capstone-Unified/
├── backend/                  # Maven Multi-Module Spring Boot Microservices
│   ├── pom.xml               # Parent POM (Spring Boot 3.2.3, Spring Cloud 2023.0.1)
│   ├── ledger-common/        # Shared DTOs, Event Models, and RFC-7807 ProblemDetails
│   ├── api-gateway/          # Spring Cloud Gateway (Port 8080)
│   ├── auth-service/         # User Authentication & JWT Issuance (Port 8081)
│   ├── account-service/      # Customer Account & Balance Service (Port 8082)
│   ├── transaction-service/  # Balance Mutation Engine & Pessimistic Row Locking (Port 8083)
│   ├── notification-service/ # Customer Alerts Kafka Consumer (Port 8084)
│   ├── audit-service/        # Immutable Audit Store Kafka Consumer (Port 8085)
│   ├── reconciliation-service/# Cross-DB Reconciliation Sweep (Port 8086)
│   ├── outbox-publisher/     # Transactional Outbox Kafka Publisher Poller (Port 8087)
│   └── analytics-service/    # Real-Time Transaction Metrics & Insights (Port 8088)
├── frontend/                 # PayPink Banking SPA (Pink Theme)
├── docker/                   # Docker Compose, OTEL Collector, Prometheus, Loki, Tempo, Grafana
├── performance/              # JMeter 800+ TPS Load Generator script (.jmx)
├── scripts/                  # Automated verification & demo scripts (.ps1, .sql)
└── docs/                     # Architecture Specs, Backlog, and Quality Reports
```
