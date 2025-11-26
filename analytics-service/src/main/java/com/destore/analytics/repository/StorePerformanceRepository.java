package com.destore.analytics.repository;

import com.destore.analytics.entity.StorePerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StorePerformanceRepository extends JpaRepository<StorePerformance, Long> {
    Optional<StorePerformance> findByStoreIdAndReportDate(String storeId, LocalDate reportDate);
    List<StorePerformance> findByStoreId(String storeId);
    List<StorePerformance> findByReportDate(LocalDate reportDate);
    List<StorePerformance> findByReportDateBetween(LocalDate start, LocalDate end);
    List<StorePerformance> findByStoreIdAndReportDateBetween(
            String storeId, LocalDate start, LocalDate end);
}
