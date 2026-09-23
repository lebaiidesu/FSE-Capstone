package com.bank.ledger.model.postgres;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LEDGER_MUTATION_AUDIT")
public class LedgerMutationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation; // 'DEBIT', 'CREDIT'

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "PHP";

    @Column(name = "before_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal beforeBalance;

    @Column(name = "after_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal afterBalance;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    public LedgerMutationAudit() {}

    public LedgerMutationAudit(Long transactionId, Long accountId, String operation, BigDecimal amount,
                               String currency, BigDecimal beforeBalance, BigDecimal afterBalance) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.operation = operation;
        this.amount = amount;
        this.currency = currency;
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
        this.createdDate = LocalDateTime.now();
    }

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getBeforeBalance() { return beforeBalance; }
    public void setBeforeBalance(BigDecimal beforeBalance) { this.beforeBalance = beforeBalance; }

    public BigDecimal getAfterBalance() { return afterBalance; }
    public void setAfterBalance(BigDecimal afterBalance) { this.afterBalance = afterBalance; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
