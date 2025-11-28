package com.destore.analytics.repository;

import com.destore.analytics.entity.SalesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {
    Optional<SalesTransaction> findByOrderId(String orderId);
    List<SalesTransaction> findByCustomerId(String customerId);
    List<SalesTransaction> findByStoreId(String storeId);
    List<SalesTransaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
    List<SalesTransaction> findByStoreIdAndTransactionDateBetween(
            String storeId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Find transactions by customer ID within a date range.
     * Avoids fetching all transactions and filtering in memory.
     */
    List<SalesTransaction> findByCustomerIdAndTransactionDateBetween(
            String customerId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT SUM(t.totalAmount) FROM SalesTransaction t WHERE t.transactionDate BETWEEN :start AND :end")
    BigDecimal getTotalSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(t) FROM SalesTransaction t WHERE t.transactionDate BETWEEN :start AND :end")
    Long getTransactionCountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT AVG(t.totalAmount) FROM SalesTransaction t WHERE t.transactionDate BETWEEN :start AND :end")
    BigDecimal getAverageTransactionValue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
