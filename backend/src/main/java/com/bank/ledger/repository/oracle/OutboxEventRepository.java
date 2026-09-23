package com.bank.ledger.repository.oracle;

import com.bank.ledger.model.oracle.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusOrderByCreatedDateAsc(String status);
    List<OutboxEvent> findTop100ByOrderByCreatedDateDesc();
}
