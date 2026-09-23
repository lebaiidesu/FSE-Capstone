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