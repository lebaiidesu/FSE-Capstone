package com.bank.ledger.repository.oracle;

import com.bank.ledger.model.oracle.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByReferenceNo(String referenceNo);
    List<TransactionRecord> findByFromAccountIdOrToAccountIdOrderByTransactionDateDesc(Long fromAccountId, Long toAccountId);
    List<TransactionRecord> findTop50ByOrderByTransactionDateDesc();
}
