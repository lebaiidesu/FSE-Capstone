package com.bank.ledger.config;

import com.bank.ledger.model.oracle.Account;
import com.bank.ledger.model.oracle.AuditLog;
import com.bank.ledger.model.oracle.Customer;
import com.bank.ledger.model.oracle.TransactionRecord;
import com.bank.ledger.model.postgres.LedgerMutationAudit;
import com.bank.ledger.model.postgres.Notification;
import com.bank.ledger.model.postgres.ReconciliationLog;
import com.bank.ledger.repository.oracle.AccountRepository;
import com.bank.ledger.repository.oracle.AuditLogRepository;
import com.bank.ledger.repository.oracle.CustomerRepository;
import com.bank.ledger.repository.oracle.TransactionRepository;
import com.bank.ledger.repository.postgres.LedgerMutationAuditRepository;
import com.bank.ledger.repository.postgres.NotificationRepository;
import com.bank.ledger.repository.postgres.ReconciliationLogRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final LedgerMutationAuditRepository ledgerMutationAuditRepository;
    private final ReconciliationLogRepository reconciliationLogRepository;
    private final NotificationRepository notificationRepository;

    public DataInitializer(CustomerRepository customerRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           AuditLogRepository auditLogRepository,
                           LedgerMutationAuditRepository ledgerMutationAuditRepository,
                           ReconciliationLogRepository reconciliationLogRepository,
                           NotificationRepository notificationRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.ledgerMutationAuditRepository = ledgerMutationAuditRepository;
        this.reconciliationLogRepository = reconciliationLogRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            // Seed Filipino Retail Customer Personas
            Customer c1 = customerRepository.save(new Customer(
                    "jdelacruz",
                    "$2a$10$wN3WpZgJ4g7N8dC5lRzPfeYk4GqU1xL8e9m3K7b0yU6r5T1w9P8a2",
                    "Juan",
                    "Dela Cruz",
                    "juan.delacruz@paypink.ph",
                    "+63 917 888 1234"
            ));

            Customer c2 = customerRepository.save(new Customer(
                    "msantos",
                    "$2a$10$wN3WpZgJ4g7N8dC5lRzPfeYk4GqU1xL8e9m3K7b0yU6r5T1w9P8a2",
                    "Maria",
                    "Santos",
                    "maria.santos@paypink.ph",
                    "+63 918 555 6789"
            ));

            Customer c3 = customerRepository.save(new Customer(
                    "jreyes",
                    "$2a$10$wN3WpZgJ4g7N8dC5lRzPfeYk4GqU1xL8e9m3K7b0yU6r5T1w9P8a2",
                    "Joshua",
                    "Reyes",
                    "joshua.reyes@paypink.ph",
                    "+63 920 333 4567"
            ));

            // Seed Accounts with 4-decimal precision balances (DECIMAL 18,4)
            Account a1 = accountRepository.save(new Account(c1.getCustomerId(), "ACC-PH-1001-8842", "SAVINGS_ACCOUNT", "PHP", new BigDecimal("125450.0000")));
            Account a2 = accountRepository.save(new Account(c1.getCustomerId(), "ACC-PH-1001-9921", "CHECKING_ACCOUNT", "PHP", new BigDecimal("50000.0000")));
            Account a3 = accountRepository.save(new Account(c1.getCustomerId(), "ACC-PH-1001-7714", "STRESS_TEST_ACCOUNT", "PHP", new BigDecimal("60.0000")));
            Account a4 = accountRepository.save(new Account(c2.getCustomerId(), "ACC-PH-2002-3311", "SAVINGS_ACCOUNT", "PHP", new BigDecimal("84320.5000")));
            Account a5 = accountRepository.save(new Account(c3.getCustomerId(), "ACC-PH-3003-4422", "TIME_DEPOSIT", "PHP", new BigDecimal("350000.0000")));

            // Seed Initial Transactions
            TransactionRecord tx1 = transactionRepository.save(new TransactionRecord(
                    a1.getAccountId(),
                    a4.getAccountId(),
                    new BigDecimal("15000.0000"),
                    "PHP",
                    "PHP",
                    "TRANSFER_INSTAPAY",
                    "TX-PH-INIT-001",
                    "SUCCESS",
                    null
            ));

            // Seed Synchronous Security Audit Log in Oracle XE
            auditLogRepository.save(new AuditLog(
                    c1.getCustomerId(),
                    "USER_LOGIN",
                    "AUTH",
                    "User [jdelacruz] successfully authenticated from BGC Taguig Branch (IP: 192.168.1.104, Device: Chrome/Windows)."
            ));

            auditLogRepository.save(new AuditLog(
                    c1.getCustomerId(),
                    "TRANSFER_INITIATED",
                    "ACCOUNT",
                    "Initiated InstaPay transfer of ₱15,000.0000 from ACC-PH-1001-8842 to Maria Santos (ACC-PH-2002-3311)."
            ));

            // Seed Immutable Audit Entry in PostgreSQL
            ledgerMutationAuditRepository.save(new LedgerMutationAudit(
                    tx1.getTransactionId(),
                    a1.getAccountId(),
                    "DEBIT",
                    new BigDecimal("15000.0000"),
                    "PHP",
                    new BigDecimal("140450.0000"),
                    new BigDecimal("125450.0000")
            ));

            // Seed Reconciliation Log in PostgreSQL
            reconciliationLogRepository.save(new ReconciliationLog(
                    tx1.getTransactionId(),
                    "SUCCESS",
                    "COMMITTED",
                    "MATCHED"
            ));

            // Seed Notification in PostgreSQL
            notificationRepository.save(new Notification(
                    c1.getCustomerId(),
                    "PayPink Alert: ₱15,000.0000 successfully debited via InstaPay to Maria Santos. Ref: TX-PH-INIT-001.",
                    "SENT"
            ));
        }
    }
}
