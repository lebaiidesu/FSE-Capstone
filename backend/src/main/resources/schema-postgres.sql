CREATE TABLE ledger_mutation_audit (
    audit_id        BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT NOT NULL,
    account_id      BIGINT NOT NULL,
    operation       VARCHAR(10) NOT NULL CHECK (operation IN ('DEBIT','CREDIT')),
    amount          NUMERIC(18,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    before_balance  NUMERIC(18,4) NOT NULL,
    after_balance   NUMERIC(18,4) NOT NULL,
    created_date    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE reconciliation_log (
    recon_id        BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT NOT NULL,
    oracle_status   VARCHAR(20),
    postgres_status VARCHAR(20),
    recon_status    VARCHAR(20) NOT NULL CHECK (recon_status IN ('MATCHED','DRIFT_DETECTED')),
    recon_date      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE notification (
    notification_id BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    message         TEXT NOT NULL,
    status          VARCHAR(10) NOT NULL CHECK (status IN ('SENT','FAILED','RETRY')),
    created_date    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE audit_log (
    audit_id     BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL,
    action       VARCHAR(50) NOT NULL,
    entity       VARCHAR(50) NOT NULL,
    details      TEXT,
    timestamp    TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_transaction ON ledger_mutation_audit(transaction_id);
CREATE INDEX idx_audit_account     ON ledger_mutation_audit(account_id);
CREATE INDEX idx_recon_transaction ON reconciliation_log(transaction_id);
CREATE INDEX idx_recon_status      ON reconciliation_log(recon_status); -- your @Scheduled sweep will filter on this