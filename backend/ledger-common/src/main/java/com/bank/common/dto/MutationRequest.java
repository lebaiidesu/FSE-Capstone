package com.bank.common.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class MutationRequest {

    @NotNull(message = "Account ID is mandatory")
    private Long accountId;

    private Long targetAccountId;

    @NotNull(message = "Mutation amount is mandatory")
    @Positive(message = "Mutation amount must be strictly greater than 0")
    @Digits(integer = 14, fraction = 4, message = "Amount must have up to 14 integer digits and 4 decimal places")
    private BigDecimal mutationAmount;

    @NotNull(message = "Currency is mandatory")
    private String currency;

    @NotNull(message = "Operation is mandatory (DEBIT or CREDIT)")
    private String operation;

    private String transactionType = "DEBIT";

    private String idempotencyKey;

    public MutationRequest() {}

    public MutationRequest(Long accountId, Long targetAccountId, BigDecimal mutationAmount,
                           String currency, String operation, String transactionType, String idempotencyKey) {
        this.accountId = accountId;
        this.targetAccountId = targetAccountId;
        this.mutationAmount = mutationAmount;
        this.currency = currency;
        this.operation = operation;
        this.transactionType = transactionType;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getTargetAccountId() { return targetAccountId; }
    public void setTargetAccountId(Long targetAccountId) { this.targetAccountId = targetAccountId; }

    public BigDecimal getMutationAmount() { return mutationAmount; }
    public void setMutationAmount(BigDecimal mutationAmount) { this.mutationAmount = mutationAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
