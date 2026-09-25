-- Seed Script for PayPink Accounts
INSERT INTO CUSTOMER (username, password_hash, first_name, last_name, email, contact_no, status)
VALUES ('demo_user', '$2a$10$wN3WpZgJ4g7N8dC5lRzPfeYk4GqU1xL8e9m3K7b0yU6r5T1w9P8a2', 'Demo', 'User', 'demo.user@paypink.ph', '+63 999 000 1111', 'ACTIVE');

INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, current_balance, status)
VALUES (1, 'ACC-PH-1001-9999', 'SAVINGS_ACCOUNT', 'PHP', 100000.0000, 'ACTIVE');
