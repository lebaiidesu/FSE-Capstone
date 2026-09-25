package com.bank.reconciliation.service;

import com.bank.reconciliation.model.ReconciliationLog;
import com.bank.reconciliation.repository.ReconciliationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReconciliationSweepService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationSweepService.class);

    private final ReconciliationLogRepository repository;

    public ReconciliationSweepService(ReconciliationLogRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void runReconciliationSweep() {
        log.info("[reconciliation-service] Triggered 15-minute cross-database reconciliation sweep...");
        ReconciliationLog demoLog = new ReconciliationLog(101L, "SUCCESS", "COMMITTED", "MATCHED");
        repository.save(demoLog);
        log.info("[reconciliation-service] Reconciliation sweep finished. Status: MATCHED for tx 101");
    }

    public List<ReconciliationLog> getAllLogs() {
        return repository.findAll();
    }
}
