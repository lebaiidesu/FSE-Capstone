package com.bank.ledger.service;

import com.bank.ledger.model.oracle.TransactionRecord;
import com.bank.ledger.model.postgres.LedgerMutationAudit;
import com.bank.ledger.model.postgres.ReconciliationLog;
import com.bank.ledger.repository.oracle.TransactionRepository;
import com.bank.ledger.repository.postgres.LedgerMutationAuditRepository;
import com.bank.ledger.repository.postgres.ReconciliationLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final LedgerMutationAuditRepository ledgerMutationAuditRepository;
    private final ReconciliationLogRepository reconciliationLogRepository;

    public ReconciliationService(TransactionRepository transactionRepository,
                                 LedgerMutationAuditRepository ledgerMutationAuditRepository,
                                 ReconciliationLogRepository reconciliationLogRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerMutationAuditRepository = ledgerMutationAuditRepository;
        this.reconciliationLogRepository = reconciliationLogRepository;
    }

    /**
     * Requirement: @Scheduled Reconciliation Sweep every 15 minutes
     * Flow:
     * 1. Every 15 minutes
     * 2. Compare Oracle TRANSACTION vs PostgreSQL LEDGER_MUTATION_AUDIT
     * 3. Write RECONCILIATION_LOG
     */
    @Scheduled(cron = "${app.reconciliation.cron:0 */15 * * * *}")
    @Transactional
    public void runScheduledSystemWideReconciliation() {
        List<TransactionRecord> transactions = transactionRepository.findTop50ByOrderByTransactionDateDesc();
        for (TransactionRecord tx : transactions) {
            reconcileSingleTransaction(tx);
        }
    }

    /**
     * Near-Real-Time verification triggered by Kafka Event Stream
     */
    @Transactional
    public void validateEventRealTime(Long transactionId) {
        transactionRepository.findById(transactionId).ifPresent(this::reconcileSingleTransaction);
    }

    public ReconciliationLog reconcileSingleTransaction(TransactionRecord tx) {
        Optional<LedgerMutationAudit> auditOpt = ledgerMutationAuditRepository.findByTransactionId(tx.getTransactionId());
        
        String oracleStatus = tx.getStatus();
        String postgresStatus;
        String reconStatus;

        if (auditOpt.isPresent()) {
            LedgerMutationAudit audit = auditOpt.get();
            // Verify amounts match exactly
            if (audit.getAmount().compareTo(tx.getAmount()) == 0) {
                postgresStatus = "COMMITTED";
                reconStatus = "MATCHED";
            } else {
                postgresStatus = "AMOUNT_MISMATCH";
                reconStatus = "DRIFT_DETECTED";
            }
        } else {
            if ("SUCCESS".equalsIgnoreCase(oracleStatus)) {
                postgresStatus = "MISSING_AUDIT";
                reconStatus = "DRIFT_DETECTED";
            } else {
                postgresStatus = "NOT_APPLICABLE";
                reconStatus = "MATCHED";
            }
        }

        ReconciliationLog log = new ReconciliationLog(tx.getTransactionId(), oracleStatus, postgresStatus, reconStatus);
        return reconciliationLogRepository.save(log);
    }

    public List<ReconciliationLog> getRecentReconciliationLogs() {
        return reconciliationLogRepository.findTop50ByOrderByReconDateDesc();
    }
}
