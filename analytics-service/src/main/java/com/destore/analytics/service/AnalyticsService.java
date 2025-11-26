package com.destore.analytics.service;

import com.destore.analytics.dto.*;
import com.destore.analytics.entity.SalesTransaction;
import com.destore.analytics.entity.CustomerAnalytics;
import com.destore.analytics.entity.ProductSalesSummary;
import com.destore.analytics.entity.StorePerformance;
import com.destore.analytics.repository.SalesTransactionRepository;
import com.destore.analytics.repository.CustomerAnalyticsRepository;
import com.destore.analytics.repository.ProductSalesSummaryRepository;
import com.destore.analytics.repository.StorePerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final CustomerAnalyticsRepository customerAnalyticsRepository;
    private final ProductSalesSummaryRepository productSalesSummaryRepository;
    private final StorePerformanceRepository storePerformanceRepository;

    @Transactional
    public void trackTransaction(TransactionRequest request) {
        log.info("Tracking transaction: {}", request.getTransactionId());
        
        SalesTransaction transaction = SalesTransaction.builder()
                .transactionId(request.getTransactionId())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .storeId(request.getStoreId())
                .totalAmount(request.getTotalAmount())
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .netAmount(request.getNetAmount() != null ? request.getNetAmount() : request.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionStatus(request.getTransactionStatus())
                .items(request.getItems())
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
                .build();
        
        salesTransactionRepository.save(transaction);
        log.info("Transaction tracked successfully: {}", request.getTransactionId());
        
        // Trigger analytics updates
        updateCustomerAnalytics(request.getCustomerId());
        updateStorePerformance(request.getStoreId());
    }

    public SalesReportResponse generateSalesReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating sales report from {} to {}", startDate, endDate);
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        BigDecimal totalSales = salesTransactionRepository.getTotalSalesBetween(start, end);
        Long transactionCount = salesTransactionRepository.getTransactionCountBetween(start, end);
        BigDecimal avgTransaction = salesTransactionRepository.getAverageTransactionValue(start, end);
        
        List<SalesTransaction> transactions = salesTransactionRepository
                .findByTransactionDateBetween(start, end);
        
        BigDecimal totalDiscounts = transactions.stream()
                .map(SalesTransaction::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netRevenue = totalSales != null ? totalSales.subtract(totalDiscounts) : BigDecimal.ZERO;
        
        Integer uniqueCustomers = transactions.stream()
                .map(SalesTransaction::getCustomerId)
                .collect(Collectors.toSet())
                .size();
        
        return SalesReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalSales(totalSales != null ? totalSales : BigDecimal.ZERO)
                .totalDiscounts(totalDiscounts)
                .netRevenue(netRevenue)
                .transactionCount(transactionCount != null ? transactionCount : 0L)
                .averageTransactionValue(avgTransaction != null ? avgTransaction : BigDecimal.ZERO)
                .customerCount(uniqueCustomers)
                .build();
    }

    public List<PerformanceMetrics> getStorePerformance(LocalDate reportDate) {
        log.info("Getting store performance for date: {}", reportDate);
        
        List<StorePerformance> performances = storePerformanceRepository
                .findByReportDate(reportDate);
        
        return performances.stream()
                .map(this::convertToPerformanceMetrics)
                .collect(Collectors.toList());
    }

    public List<CustomerAnalyticsResponse> getTopCustomers(LocalDate reportDate, int limit) {
        log.info("Getting top {} customers for date: {}", limit, reportDate);
        
        List<CustomerAnalytics> topCustomers = customerAnalyticsRepository
                .findTopCustomers(reportDate);
        
        return topCustomers.stream()
                .limit(limit)
                .map(this::convertToCustomerAnalyticsResponse)
                .collect(Collectors.toList());
    }

    public List<ProductSalesResponse> getTopSellingProducts(LocalDate reportDate, int limit) {
        log.info("Getting top {} selling products for date: {}", limit, reportDate);
        
        List<ProductSalesSummary> topProducts = productSalesSummaryRepository
                .findTopSellingProducts(reportDate);
        
        return topProducts.stream()
                .limit(limit)
                .map(this::convertToProductSalesResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    private void updateCustomerAnalytics(String customerId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        List<SalesTransaction> customerTransactions = salesTransactionRepository
                .findByTransactionDateBetween(startOfDay, endOfDay)
                .stream()
                .filter(t -> t.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
        
        if (!customerTransactions.isEmpty()) {
            CustomerAnalytics analytics = customerAnalyticsRepository
                    .findByCustomerIdAndReportDate(customerId, today)
                    .orElse(CustomerAnalytics.builder()
                            .customerId(customerId)
                            .reportDate(today)
                            .build());
            
            analytics.setTotalPurchases(customerTransactions.size());
            analytics.setTotalSpent(customerTransactions.stream()
                    .map(SalesTransaction::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            
            if (analytics.getTotalPurchases() > 0) {
                analytics.setAverageOrderValue(
                        analytics.getTotalSpent().divide(
                                BigDecimal.valueOf(analytics.getTotalPurchases()),
                                2,
                                RoundingMode.HALF_UP
                        )
                );
            }
            
            customerAnalyticsRepository.save(analytics);
        }
    }

    @Transactional
    private void updateStorePerformance(String storeId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        List<SalesTransaction> storeTransactions = salesTransactionRepository
                .findByTransactionDateBetween(startOfDay, endOfDay)
                .stream()
                .filter(t -> t.getStoreId().equals(storeId))
                .collect(Collectors.toList());
        
        if (!storeTransactions.isEmpty()) {
            StorePerformance performance = storePerformanceRepository
                    .findByStoreIdAndReportDate(storeId, today)
                    .orElse(StorePerformance.builder()
                            .storeId(storeId)
                            .reportDate(today)
                            .build());
            
            performance.setTransactionCount(storeTransactions.size());
            performance.setTotalSales(storeTransactions.stream()
                    .map(SalesTransaction::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            
            BigDecimal totalDiscounts = storeTransactions.stream()
                    .map(SalesTransaction::getDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            performance.setNetRevenue(performance.getTotalSales().subtract(totalDiscounts));
            
            // Calculate unique customers
            Integer uniqueCustomers = storeTransactions.stream()
                    .map(SalesTransaction::getCustomerId)
                    .collect(Collectors.toSet())
                    .size();
            performance.setCustomerCount(uniqueCustomers);
            
            if (performance.getTransactionCount() > 0) {
                performance.setAverageTransactionValue(
                        performance.getTotalSales().divide(
                                BigDecimal.valueOf(performance.getTransactionCount()),
                                2,
                                RoundingMode.HALF_UP
                        )
                );
            }
            
            storePerformanceRepository.save(performance);
        }
    }

    private PerformanceMetrics convertToPerformanceMetrics(StorePerformance performance) {
        return PerformanceMetrics.builder()
                .storeId(performance.getStoreId())
                .period(performance.getReportDate().toString())
                .totalRevenue(performance.getTotalSales())
                .totalTransactions(performance.getTransactionCount())
                .averageOrderValue(performance.getAverageTransactionValue())
                .status("Active")
                .build();
    }

    private CustomerAnalyticsResponse convertToCustomerAnalyticsResponse(CustomerAnalytics analytics) {
        return CustomerAnalyticsResponse.builder()
                .customerId(analytics.getCustomerId())
                .totalPurchases(analytics.getTotalPurchases())
                .totalSpent(analytics.getTotalSpent())
                .averageOrderValue(analytics.getAverageOrderValue())
                .firstPurchaseDate(analytics.getFirstPurchaseDate())
                .lastPurchaseDate(analytics.getLastPurchaseDate())
                .daysSinceLastPurchase(
                        analytics.getLastPurchaseDate() != null ?
                                (int) ChronoUnit.DAYS.between(analytics.getLastPurchaseDate(), LocalDate.now()) :
                                null
                )
                .build();
    }

    private ProductSalesResponse convertToProductSalesResponse(ProductSalesSummary summary) {
        return ProductSalesResponse.builder()
                .productCode(summary.getProductCode())
                .reportDate(summary.getReportDate())
                .quantitySold(summary.getQuantitySold())
                .totalRevenue(summary.getTotalRevenue())
                .averagePrice(summary.getAveragePrice())
                .transactionCount(summary.getTransactionCount())
                .build();
    }
}
