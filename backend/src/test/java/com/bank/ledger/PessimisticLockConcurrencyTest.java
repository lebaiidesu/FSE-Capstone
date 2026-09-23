package com.bank.ledger;

import com.bank.ledger.dto.MutationRequest;
import com.bank.ledger.dto.MutationResponse;
import com.bank.ledger.exception.InsufficientFundsException;
import com.bank.ledger.model.oracle.Account;
import com.bank.ledger.repository.oracle.AccountRepository;
import com.bank.ledger.service.AccountService;
import com.bank.ledger.service.LedgerMutationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class PessimisticLockConcurrencyTest {

    @Autowired
    private LedgerMutationService ledgerMutationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Test
    @DisplayName("Requirement 1.B: Concurrency Control - Prevent Race Condition & Double Spend with @Lock(PESSIMISTIC_WRITE)")
    public void testPessimisticLockDoubleSpendPrevention() throws Exception {
        // Setup Account with 60.0000 PHP balance
        Account testAccount = accountRepository.findAll().get(0);
        Long accountId = testAccount.getAccountId();
        accountService.resetAccountBalance(accountId, new BigDecimal("60.0000"));

        int totalConcurrentThreads = 10;
        BigDecimal debitPerThread = new BigDecimal("50.0000");

        ExecutorService executor = Executors.newFixedThreadPool(totalConcurrentThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalConcurrentThreads);

        AtomicInteger successfulDebits = new AtomicInteger(0);
        AtomicInteger rejectedDebits = new AtomicInteger(0);
        Set<String> uniqueReferences = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < totalConcurrentThreads; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                try {
                    startGate.await(); // Synchronized release to hit database concurrently
                    MutationRequest req = new MutationRequest(accountId, debitPerThread, "DEBIT", "DEBIT", null, "PHP");
                    MutationResponse res = ledgerMutationService.mutateBalance(req, "thread_" + threadIdx);
                    successfulDebits.incrementAndGet();
                    uniqueReferences.add(res.getReferenceNo());
                } catch (InsufficientFundsException ex) {
                    rejectedDebits.incrementAndGet();
                } catch (Exception ex) {
                    // unexpected error
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent threads must finish within timeout window");

        // Verify that exactly 1 debit succeeded and 9 were safely rejected
        assertEquals(1, successfulDebits.get(), "Exactly one ₱50 debit must succeed against ₱60 starting balance");
        assertEquals(9, rejectedDebits.get(), "Remaining 9 attempts must be blocked with InsufficientFundsException");

        // Verify final balance in database is exactly ₱10.0000 (No -40 PHP invalid overdraft)
        Account finalAccountState = accountRepository.findById(accountId).orElseThrow();
        assertEquals(new BigDecimal("10.0000"), finalAccountState.getCurrentBalance(),
                "Final balance must be exactly ₱10.0000 with zero overdraft.");
    }
}
