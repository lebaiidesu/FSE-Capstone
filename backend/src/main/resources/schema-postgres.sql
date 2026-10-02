-- =============================================================================
-- PostgreSQL Schema — Bank Ledger Audit DB (audit_user / ledger_audit_db)
-- Used by: audit-service, notification-service, reconciliation-service
-- =============================================================================

-- -----------------------------------------------------------------------------
-- LEDGER_MUTATION_AUDIT
-- Written by audit-service (Kafka consumer).
-- Read by reconciliation-service for cross-DB consistency checks.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS LEDGER_MUTATION_AUDIT (
    audit_id       BIGSERIAL     PRIMARY KEY,
    transaction_id BIGINT        NOT NULL,
    account_id     BIGINT        NOT NULL,
    operation      VARCHAR(20)   NOT NULL,
    amount         NUMERIC(18,4) NOT NULL,
    currency       VARCHAR(10)   NOT NULL DEFAULT 'PHP',
    before_balance NUMERIC(18,4) NOT NULL,
    after_balance  NUMERIC(18,4) NOT NULL,
    created_date   TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_lma_operation CHECK (operation IN ('DEBIT','CREDIT','REVERSAL'))
);

CREATE INDEX IF NOT EXISTS idx_lma_transaction_id ON LEDGER_MUTATION_AUDIT (transaction_id);
CREATE INDEX IF NOT EXISTS idx_lma_account_id     ON LEDGER_MUTATION_AUDIT (account_id);
CREATE INDEX IF NOT EXISTS idx_lma_created_date   ON LEDGER_MUTATION_AUDIT (created_date);

-- -----------------------------------------------------------------------------
-- NOTIFICATION
-- Written by notification-service (Kafka consumer).
-- Stores SMS/email delivery records per customer.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS NOTIFICATION (
    notification_id BIGSERIAL   PRIMARY KEY,
    customer_id     BIGINT      NOT NULL,
    message         TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_date    TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_notification_status CHECK (status IN ('PENDING','SENT','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_notification_customer_id  ON NOTIFICATION (customer_id);
CREATE INDEX IF NOT EXISTS idx_notification_status       ON NOTIFICATION (status);
CREATE INDEX IF NOT EXISTS idx_notification_created_date ON NOTIFICATION (created_date);

-- -----------------------------------------------------------------------------
-- RECONCILIATION_LOG
-- Written by reconciliation-service when it compares Oracle vs Postgres state.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS RECONCILIATION_LOG (
    recon_id        BIGSERIAL   PRIMARY KEY,
    transaction_id  BIGINT      NOT NULL,
    oracle_status   VARCHAR(30) NOT NULL,
    postgres_status VARCHAR(30) NOT NULL,
    recon_status    VARCHAR(30) NOT NULL,
    recon_date      TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_recon_status CHECK (recon_status IN ('MATCH','MISMATCH','PENDING','RESOLVED'))
);

CREATE INDEX IF NOT EXISTS idx_recon_transaction_id ON RECONCILIATION_LOG (transaction_id);
CREATE INDEX IF NOT EXISTS idx_recon_status         ON RECONCILIATION_LOG (recon_status);
CREATE INDEX IF NOT EXISTS idx_recon_date           ON RECONCILIATION_LOG (recon_date);

-- =============================================================================
-- End of PostgreSQL schema
-- =============================================================================
