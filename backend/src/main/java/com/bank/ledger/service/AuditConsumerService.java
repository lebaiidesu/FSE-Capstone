package com.bank.ledger.service;

import com.bank.ledger.model.postgres.LedgerMutationAudit;
import com.bank.ledger.repository.postgres.LedgerMutationAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuditConsumerService {

    private final LedgerMutationAuditRepository ledgerMutationAuditRepository;

    public AuditConsumerService(LedgerMutationAuditRepository ledgerMutationAuditRepository) {
        this.ledgerMutationAuditRepository = ledgerMutationAuditRepository;
    }

    /**
     * Kafka Consumer for Audit Topic:
     * Appends immutable audit entry to PostgreSQL LEDGER_MUTATION_AUDIT table.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LedgerMutationAudit consumeAuditEvent(Long transactionId, Long accountId, String operation,
                                                 BigDecimal amount, String currency, BigDecimal beforeBalance,
                                                 BigDecimal afterBalance) {
        LedgerMutationAudit audit = new LedgerMutationAudit(
                transactionId,
                accountId,
                operation,
                amount,
                currency,
                beforeBalance,
                afterBalance
        );
        return ledgerMutationAuditRepository.save(audit);
    }
}
