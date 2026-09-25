package com.bank.transaction;

import com.bank.common.dto.MutationRequest;
import com.bank.common.dto.MutationResponse;
import com.bank.transaction.model.Account;
import com.bank.transaction.repository.AccountRepository;
import com.bank.transaction.service.LedgerMutationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class PessimisticLockConcurrencyTest {

    @Autowired
    private LedgerMutationService ledgerMutationService;

    @Autowired
    private AccountRepository accountRepository;

    private Long testAccountId;

    @BeforeEach
    void setUp() {
        Account acc = new Account(1L, "ACC-TEST-CONCURRENCY-001", "SAVINGS", "PHP", new BigDecimal("60.0000"));
        Account saved = accountRepository.save(acc);
        testAccountId = saved.getAccountId();
    }

    @Test
    @DisplayName("Pessimistic Lock Guard: 10 parallel threads debiting ₱50 from ₱60 balance allows exactly 1 success and 9 failures (0% overdraft)")
    void testConcurrentDebitOverdraftPrevention() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    MutationRequest request = new MutationRequest(
                            testAccountId, null, new BigDecimal("50.0000"), "PHP", "DEBIT", "DEBIT", "KEY-" + index);
                    MutationResponse response = ledgerMutationService.mutateBalance(request, "thread-user-" + index);
                    if ("SUCCESS".equals(response.getStatus())) {
                        successCount.incrementAndGet();
                        return true;
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
                return false;
            }));
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Account finalAccount = accountRepository.findById(testAccountId).orElseThrow();

        assertEquals(1, successCount.get(), "Exactly 1 debit operation should succeed");
        assertEquals(9, failureCount.get(), "Exactly 9 debit operations should fail due to insufficient funds");
        assertEquals(0, new BigDecimal("10.0000").compareTo(finalAccount.getCurrentBalance()), "Final balance must be exactly ₱10.0000");
    }
}
