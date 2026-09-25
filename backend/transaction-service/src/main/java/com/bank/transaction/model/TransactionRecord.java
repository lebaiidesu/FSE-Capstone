package com.bank.transaction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSACTION")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

    @Column(name = "FROM_ACCOUNT_ID", nullable = false)
    private Long fromAccountId;

    @Column(name = "TO_ACCOUNT_ID")
    private Long toAccountId;

    @Column(name = "AMOUNT", nullable = false)
    private BigDecimal amount;

    @Column(name = "SOURCE_CURRENCY", nullable = false)
    private String sourceCurrency = "PHP";

    @Column(name = "TARGET_CURRENCY", nullable = false)
    private String targetCurrency = "PHP";

    @Column(name = "TRANSACTION_TYPE", nullable = false)
    private String transactionType;

    @Column(name = "REFERENCE_NO", nullable = false, unique = true)
    private String referenceNo;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "FAILURE_REASON")
    private String failureReason;

    @Column(name = "TRANSACTION_DATE")
    private LocalDateTime transactionDate = LocalDateTime.now();

    public TransactionRecord() {}

    public TransactionRecord(Long fromAccountId, Long toAccountId, BigDecimal amount, String sourceCurrency, String targetCurrency, String transactionType, String referenceNo, String status, String failureReason) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.transactionType = transactionType;
        this.referenceNo = referenceNo;
        this.status = status;
        this.failureReason = failureReason;
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Long getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }

    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getSourceCurrency() { return sourceCurrency; }
    public void setSourceCurrency(String sourceCurrency) { this.sourceCurrency = sourceCurrency; }

    public String getTargetCurrency() { return targetCurrency; }
    public void setTargetCurrency(String targetCurrency) { this.targetCurrency = targetCurrency; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
}
