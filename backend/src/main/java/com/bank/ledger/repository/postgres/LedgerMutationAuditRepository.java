package com.bank.ledger.repository.postgres;

import com.bank.ledger.model.postgres.LedgerMutationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerMutationAuditRepository extends JpaRepository<LedgerMutationAudit, Long> {
    Optional<LedgerMutationAudit> findByTransactionId(Long transactionId);
    List<LedgerMutationAudit> findByAccountIdOrderByCreatedDateDesc(Long accountId);
    List<LedgerMutationAudit> findTop50ByOrderByCreatedDateDesc();
}
