package com.bank.reconciliation.repository;

import com.bank.reconciliation.model.ReconciliationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReconciliationLogRepository extends JpaRepository<ReconciliationLog, Long> {
    List<ReconciliationLog> findByReconStatus(String reconStatus);
}
