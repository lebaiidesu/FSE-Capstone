package com.bank.audit.repository;

import com.bank.audit.model.LedgerMutationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerMutationAuditRepository extends JpaRepository<LedgerMutationAudit, Long> {
}
