package com.destore.analytics.repository;

import com.destore.analytics.entity.ProductSalesSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSalesSummaryRepository extends JpaRepository<ProductSalesSummary, Long> {
    Optional<ProductSalesSummary> findByProductCodeAndReportDate(String productCode, LocalDate reportDate);
    List<ProductSalesSummary> findByReportDate(LocalDate reportDate);
    List<ProductSalesSummary> findByReportDateBetween(LocalDate start, LocalDate end);
    List<ProductSalesSummary> findByProductCode(String productCode);
    
    @Query("SELECT p FROM ProductSalesSummary p WHERE p.reportDate = :date ORDER BY p.totalRevenue DESC")
    List<ProductSalesSummary> findTopSellingProducts(LocalDate date);
}
