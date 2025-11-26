package com.destore.loyalty.repository;

import com.destore.loyalty.entity.PurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long> {
    List<PurchaseHistory> findByCustomerId(String customerId);
    List<PurchaseHistory> findByCustomerIdOrderByPurchaseDateDesc(String customerId);
    List<PurchaseHistory> findByPurchaseDateBetween(LocalDateTime start, LocalDateTime end);
    Long countByCustomerId(String customerId);
}
