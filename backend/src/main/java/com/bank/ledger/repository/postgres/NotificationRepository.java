package com.bank.ledger.repository.postgres;

import com.bank.ledger.model.postgres.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCustomerIdOrderByCreatedDateDesc(Long customerId);
    List<Notification> findTop50ByOrderByCreatedDateDesc();
}
