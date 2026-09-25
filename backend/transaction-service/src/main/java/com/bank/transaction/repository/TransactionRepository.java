package com.bank.transaction.repository;

import com.bank.transaction.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByReferenceNo(String referenceNo);
}
