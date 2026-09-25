package com.bank.audit.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LEDGER_MUTATION_AUDIT")
public class LedgerMutationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long auditId;

    @Column(name = "TRANSACTION_ID", nullable = false)
    private Long transactionId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "OPERATION", nullable = false)
    private String operation;

    @Column(name = "AMOUNT", nullable = false)
    private BigDecimal amount;

    @Column(name = "CURRENCY", nullable = false)
    private String currency = "PHP";

    @Column(name = "BEFORE_BALANCE", nullable = false)
    private BigDecimal beforeBalance;

    @Column(name = "AFTER_BALANCE", nullable = false)
    private BigDecimal afterBalance;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate = LocalDateTime.now();

    public LedgerMutationAudit() {}

    public LedgerMutationAudit(Long transactionId, Long accountId, String operation, BigDecimal amount, String currency, BigDecimal beforeBalance, BigDecimal afterBalance) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.operation = operation;
        this.amount = amount;
        this.currency = currency;
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
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
