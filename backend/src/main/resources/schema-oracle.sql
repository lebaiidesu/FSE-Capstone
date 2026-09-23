<<<<<<< HEAD
CREATE TABLE customer (
    customer_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR2(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR2(255) NOT NULL,
    first_name      VARCHAR2(50),
    last_name       VARCHAR2(50),
    email           VARCHAR2(100) NOT NULL UNIQUE,
    contact_no      VARCHAR2(20),
    status          VARCHAR2(20)  DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED')),
    created_date    TIMESTAMP     DEFAULT SYSTIMESTAMP
);

CREATE TABLE account (
    account_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id     NUMBER NOT NULL REFERENCES customer(customer_id),
    account_number  VARCHAR2(30) NOT NULL UNIQUE,
    account_type    VARCHAR2(20),
    currency        VARCHAR2(3)  NOT NULL,
    current_balance NUMBER(18,4) DEFAULT 0 NOT NULL,
    status          VARCHAR2(20) DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    created_date    TIMESTAMP    DEFAULT SYSTIMESTAMP
);

CREATE TABLE transaction (
    transaction_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    from_account_id  NUMBER REFERENCES account(account_id),
    to_account_id    NUMBER REFERENCES account(account_id),
    amount           NUMBER(18,4) NOT NULL CHECK (amount > 0),
    source_currency  VARCHAR2(3),
    target_currency  VARCHAR2(3),
    transaction_type VARCHAR2(20),
    reference_no     VARCHAR2(50) NOT NULL UNIQUE,
    status           VARCHAR2(20) DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    failure_reason   VARCHAR2(100),
    transaction_date TIMESTAMP DEFAULT SYSTIMESTAMP
);

CREATE TABLE outbox_event (
    event_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id NUMBER NOT NULL REFERENCES transaction(transaction_id),
    event_type     VARCHAR2(30) NOT NULL, -- TRANSACTION_SUCCESS, TRANSACTION_FAILED
    payload        CLOB NOT NULL,
    status         VARCHAR2(20) DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','PROCESSED','FAILED')),
    created_date   TIMESTAMP DEFAULT SYSTIMESTAMP,
    processed_date TIMESTAMP
);

CREATE INDEX idx_account_customer   ON account(customer_id);
CREATE INDEX idx_txn_from_account   ON transaction(from_account_id);
CREATE INDEX idx_txn_to_account     ON transaction(to_account_id);
CREATE INDEX idx_outbox_status      ON outbox_event(status); -- the publisher polls WHERE status = 'PENDING'
=======
-- ============================================================================
-- CAPSTONE FSE: Core Retail Ledger & Balance Mutation Engine
-- Oracle XE 21c Database Schema (Master Source of Truth & Security Audit)
-- ============================================================================

-- Drop tables if existing (in reverse dependency order)
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE OUTBOX_EVENT CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE TRANSACTION CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE AUDIT_LOG CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE ACCOUNT CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE CUSTOMER CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- 1. CUSTOMER TABLE
CREATE TABLE CUSTOMER (
    customer_id      NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username         VARCHAR2(50) NOT NULL UNIQUE,
    password_hash    VARCHAR2(255) NOT NULL,
    first_name       VARCHAR2(100) NOT NULL,
    last_name        VARCHAR2(100) NOT NULL,
    email            VARCHAR2(150) NOT NULL UNIQUE,
    contact_no       VARCHAR2(30) NOT NULL,
    status           VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. ACCOUNT TABLE (customer_balance_master)
CREATE TABLE ACCOUNT (
    account_id       NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id      NUMBER(19) NOT NULL,
    account_number   VARCHAR2(30) NOT NULL UNIQUE,
    account_type     VARCHAR2(30) DEFAULT 'SAVINGS' NOT NULL,
    currency         VARCHAR2(10) DEFAULT 'PHP' NOT NULL,
    current_balance  NUMBER(18,4) DEFAULT 0.0000 NOT NULL,
    status           VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id),
    CONSTRAINT chk_account_balance_positive CHECK (current_balance >= 0.0000)
);

-- 3. AUDIT_LOG TABLE (Synchronous Security & Operational Audit)
CREATE TABLE AUDIT_LOG (
    audit_id         NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id      NUMBER(19) NOT NULL,
    action           VARCHAR2(100) NOT NULL,
    entity           VARCHAR2(50) NOT NULL,
    details          VARCHAR2(4000) NOT NULL,
    timestamp        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
);

-- 4. TRANSACTION TABLE
CREATE TABLE TRANSACTION (
    transaction_id   NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    from_account_id  NUMBER(19) NOT NULL,
    to_account_id    NUMBER(19),
    amount           NUMBER(18,4) NOT NULL,
    source_currency  VARCHAR2(10) DEFAULT 'PHP' NOT NULL,
    target_currency  VARCHAR2(10) DEFAULT 'PHP' NOT NULL,
    transaction_type VARCHAR2(30) NOT NULL, -- 'DEBIT', 'CREDIT', 'TRANSFER_INSTAPAY', 'TRANSFER_PESONET', 'TRANSFER_QRPH'
    reference_no     VARCHAR2(64) NOT NULL UNIQUE,
    status           VARCHAR2(20) NOT NULL, -- 'SUCCESS', 'FAILED'
    failure_reason   VARCHAR2(255),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_tx_from_account FOREIGN KEY (from_account_id) REFERENCES ACCOUNT(account_id),
    CONSTRAINT fk_tx_to_account FOREIGN KEY (to_account_id) REFERENCES ACCOUNT(account_id),
    CONSTRAINT chk_tx_amount_positive CHECK (amount > 0.0000)
);

-- 5. OUTBOX_EVENT TABLE (Transactional Outbox Pattern for Kafka Streaming)
CREATE TABLE OUTBOX_EVENT (
    event_id         NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id   NUMBER(19) NOT NULL,
    event_type       VARCHAR2(50) NOT NULL, -- 'TRANSACTION_SUCCESS', 'TRANSACTION_FAILED'
    payload          CLOB NOT NULL,
    status           VARCHAR2(20) DEFAULT 'PENDING' NOT NULL, -- 'PENDING', 'PROCESSED', 'FAILED'
    created_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    processed_date   TIMESTAMP,
    CONSTRAINT fk_outbox_transaction FOREIGN KEY (transaction_id) REFERENCES TRANSACTION(transaction_id)
);

-- Indexes for high-throughput concurrency
CREATE INDEX idx_account_cust_id ON ACCOUNT(customer_id);
CREATE INDEX idx_account_num ON ACCOUNT(account_number);
CREATE INDEX idx_tx_from_acc ON TRANSACTION(from_account_id);
CREATE INDEX idx_tx_ref_no ON TRANSACTION(reference_no);
CREATE INDEX idx_outbox_status ON OUTBOX_EVENT(status, created_date);
CREATE INDEX idx_audit_cust_id ON AUDIT_LOG(customer_id, timestamp);

-- Seed Initial Retail Banking Customer & Accounts (Philippine Context)
INSERT INTO CUSTOMER (username, password_hash, first_name, last_name, email, contact_no, status)
VALUES ('lviernes', '$2a$10$wN3WpZgJ4g7N8dC5lRzPfeYk4GqU1xL8e9m3K7b0yU6r5T1w9P8a2', 'Levi', 'Viernes', 'levi.viernes@paypink.ph', '+63 917 888 1234', 'ACTIVE');

INSERT INTO CUSTOMER (username, password_hash, first_name, last_name, email, contact_no, status)
VALUES ('arosales', '$2a$10$wN3WpZgJ4g7N8dC5lRzPfeYk4GqU1xL8e9m3K7b0yU6r5T1w9P8a2', 'Aly', 'Rosales', 'aly.rosales@paypink.ph', '+63 918 555 6789', 'ACTIVE');

INSERT INTO CUSTOMER (username, password_hash, first_name, last_name, email, contact_no, status)
VALUES ('glim', '$2a$10$wN3WpZgJ4g7N8dC5lRzPfeYk4GqU1xL8e9m3K7b0yU6r5T1w9P8a2', 'Gill', 'Lim', 'gill.lim@paypink.ph', '+63 920 333 4567', 'ACTIVE');

-- Accounts with initial balances (DECIMAL 18,4)
INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, current_balance, status)
VALUES (1, 'ACC-PH-1001-8842', 'SAVINGS_ACCOUNT', 'PHP', 125450.0000, 'ACTIVE');

INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, current_balance, status)
VALUES (1, 'ACC-PH-1001-9921', 'CHECKING_ACCOUNT', 'PHP', 50000.0000, 'ACTIVE');

INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, current_balance, status)
VALUES (1, 'ACC-PH-1001-7714', 'STRESS_TEST_ACCOUNT', 'PHP', 60.0000, 'ACTIVE'); -- For Double-Spend Race Condition Demo

INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, current_balance, status)
VALUES (2, 'ACC-PH-2002-3311', 'SAVINGS_ACCOUNT', 'PHP', 84320.5000, 'ACTIVE');

INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, current_balance, status)
VALUES (3, 'ACC-PH-3003-4422', 'TIME_DEPOSIT', 'PHP', 350000.0000, 'ACTIVE');
>>>>>>> bacc780 (feat: complete FSE Capstone - Core Retail Ledger, Dual-Store Outbox Engine & PayPink UI)
