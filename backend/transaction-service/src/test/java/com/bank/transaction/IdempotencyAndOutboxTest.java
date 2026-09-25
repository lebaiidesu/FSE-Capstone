package com.bank.transaction;

import com.bank.common.dto.MutationRequest;
import com.bank.common.dto.MutationResponse;
import com.bank.transaction.model.Account;
import com.bank.transaction.model.OutboxEvent;
import com.bank.transaction.repository.AccountRepository;
import com.bank.transaction.repository.OutboxEventRepository;
import com.bank.transaction.service.LedgerMutationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class IdempotencyAndOutboxTest {

    @Autowired
    private LedgerMutationService ledgerMutationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private Long testAccountId;

    @BeforeEach
    void setUp() {
        Account acc = new Account(1L, "ACC-TEST-IDEMPOTENCY-002", "SAVINGS", "PHP", new BigDecimal("1000.0000"));
        Account saved = accountRepository.save(acc);
        testAccountId = saved.getAccountId();
    }

    @Test
    @DisplayName("Idempotency Test: Sending identical request twice returns cached response with 0 extra balance deduction")
    void testIdempotentDuplicateRequest() {
        String key = "IDEM-KEY-UNIQUE-999";
        MutationRequest request1 = new MutationRequest(
                testAccountId, null, new BigDecimal("100.0000"), "PHP", "DEBIT", "DEBIT", key);

        MutationResponse resp1 = ledgerMutationService.mutateBalance(request1, "user1");
        assertFalse(resp1.isCachedIdempotentResponse(), "First request should be newly processed");
        assertEquals(0, new BigDecimal("900.0000").compareTo(resp1.getAfterBalance()));

        MutationRequest request2 = new MutationRequest(
                testAccountId, null, new BigDecimal("100.0000"), "PHP", "DEBIT", "DEBIT", key);

        MutationResponse resp2 = ledgerMutationService.mutateBalance(request2, "user1");
        assertTrue(resp2.isCachedIdempotentResponse(), "Second request must return cached idempotent response");
        assertEquals(resp1.getReferenceNo(), resp2.getReferenceNo(), "Reference numbers must match");

        Account finalAccount = accountRepository.findById(testAccountId).orElseThrow();
        assertEquals(0, new BigDecimal("900.0000").compareTo(finalAccount.getCurrentBalance()), "Balance should be deducted only once");
    }

    @Test
    @DisplayName("Transactional Outbox Test: Balance mutation creates a PENDING OUTBOX_EVENT record")
    void testOutboxEventCreatedOnMutation() {
        MutationRequest request = new MutationRequest(
                testAccountId, null, new BigDecimal("200.0000"), "PHP", "CREDIT", "CREDIT", "IDEM-OUTBOX-001");

        MutationResponse response = ledgerMutationService.mutateBalance(request, "user1");

        List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatusOrderByCreatedDateAsc("PENDING");
        assertFalse(outboxEvents.isEmpty(), "Pending outbox events must exist");
        assertTrue(outboxEvents.stream().anyMatch(e -> e.getTransactionId().equals(response.getTransactionId())));
    }
}
