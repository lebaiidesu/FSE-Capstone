package com.bank.account.dto;

import com.bank.account.entity.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {
    private Long accountId;
    private Long customerId;
    private String accountNumber;
    private String accountType;
    private String currency;
    private BigDecimal currentBalance;
    private String status;
    private LocalDateTime createdDate;

    public AccountResponse() {}

    public AccountResponse(Account account) {
        this.accountId = account.getAccountId();
        this.customerId = account.getCustomerId();
        this.accountNumber = account.getAccountNumber();
        this.accountType = account.getAccountType();
        this.currency = account.getCurrency();
        this.currentBalance = account.getCurrentBalance();
        this.status = account.getStatus();
        this.createdDate = account.getCreatedDate();
    }

    public Long getAccountId() { return accountId; }
    public Long getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
