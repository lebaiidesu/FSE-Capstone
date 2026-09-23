package com.bank.account.controller;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.dto.UpdateStatusRequest;
import com.bank.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long accountId) {
        AccountResponse response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customers/{customerId}/accounts")
    public ResponseEntity<List<AccountResponse>> getCustomerAccounts(@PathVariable Long customerId) {
        List<AccountResponse> response = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/accounts/{accountId}/status")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable Long accountId,
            @RequestBody UpdateStatusRequest request) {
        AccountResponse response = accountService.updateAccountStatus(accountId, request.getStatus());
        return ResponseEntity.ok(response);
    }
}
