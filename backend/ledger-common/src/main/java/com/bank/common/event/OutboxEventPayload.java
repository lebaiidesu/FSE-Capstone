package com.bank.common.event;

import java.math.BigDecimal;

public class OutboxEventPayload {

    private Long transactionId;
    private String referenceNo;
    private Long accountId;
    private Long customerId;
    private String operation;
    private BigDecimal amount;
    private String currency;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private String timestamp;

    public OutboxEventPayload() {}

    public OutboxEventPayload(Long transactionId, String referenceNo, Long accountId, Long customerId,
                             String operation, BigDecimal amount, String currency,
                             BigDecimal beforeBalance, BigDecimal afterBalance, String timestamp) {
        this.transactionId = transactionId;
        this.referenceNo = referenceNo;
        this.accountId = accountId;
        this.customerId = customerId;
        this.operation = operation;
        this.amount = amount;
        this.currency = currency;
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
        this.timestamp = timestamp;
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

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

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
