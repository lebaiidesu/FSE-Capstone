package com.bank.account.service;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.entity.Account;
import com.bank.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String generatedAccNumber = "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal balance = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;

        Account account = new Account(
                request.getCustomerId(),
                generatedAccNumber,
                request.getAccountType(),
                request.getCurrency(),
                balance,
                "ACTIVE"
        );

        Account saved = accountRepository.save(account);
        return new AccountResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));
        return new AccountResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(AccountResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, String newStatus) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));

        account.setStatus(newStatus.toUpperCase());
        Account updated = accountRepository.save(account);
        return new AccountResponse(updated);
    }
}
