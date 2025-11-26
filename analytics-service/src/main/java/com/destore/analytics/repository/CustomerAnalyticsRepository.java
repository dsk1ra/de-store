package com.destore.analytics.repository;

import com.destore.analytics.entity.CustomerAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAnalyticsRepository extends JpaRepository<CustomerAnalytics, Long> {
    Optional<CustomerAnalytics> findByCustomerIdAndReportDate(String customerId, LocalDate reportDate);
    List<CustomerAnalytics> findByCustomerId(String customerId);
    List<CustomerAnalytics> findByReportDate(LocalDate reportDate);
    
    @Query("SELECT c FROM CustomerAnalytics c WHERE c.reportDate = :date ORDER BY c.totalSpent DESC")
    List<CustomerAnalytics> findTopCustomers(LocalDate date);
}
