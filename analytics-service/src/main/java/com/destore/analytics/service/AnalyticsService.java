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
        log.info("Tracking transaction for order: {}", request.getOrderId());
        
        SalesTransaction transaction = SalesTransaction.builder()
                .transactionId("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .orderId(request.getOrderId())
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
        log.info("Transaction tracked successfully for order: {}", request.getOrderId());
        
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
        
        // If no aggregated data, compute from raw transactions
        if (performances.isEmpty()) {
            log.info("No aggregated data found, computing from raw transactions");
            return computeStorePerformanceFromTransactions(reportDate);
        }
        
        return performances.stream()
                .map(this::convertToPerformanceMetrics)
                .collect(Collectors.toList());
    }

    public List<CustomerAnalyticsResponse> getTopCustomers(LocalDate reportDate, int limit) {
        log.info("Getting top {} customers for date: {}", limit, reportDate);
        
        List<CustomerAnalytics> topCustomers = customerAnalyticsRepository
                .findTopCustomers(reportDate);
        
        // If no aggregated data, compute from raw transactions
        if (topCustomers.isEmpty()) {
            log.info("No aggregated data found, computing from raw transactions");
            return computeTopCustomersFromTransactions(reportDate, limit);
        }
        
        return topCustomers.stream()
                .limit(limit)
                .map(this::convertToCustomerAnalyticsResponse)
                .collect(Collectors.toList());
    }

    public List<ProductSalesResponse> getTopSellingProducts(LocalDate reportDate, int limit) {
        log.info("Getting top {} selling products for date: {}", limit, reportDate);
        
        List<ProductSalesSummary> topProducts = productSalesSummaryRepository
                .findTopSellingProducts(reportDate);
        
        // If no aggregated data, compute from raw transactions
        if (topProducts.isEmpty()) {
            log.info("No aggregated data found, computing from raw transactions");
            return computeTopProductsFromTransactions(reportDate, limit);
        }
        
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
        
        // Use database query to filter by customer ID instead of fetching all and filtering in memory
        List<SalesTransaction> customerTransactions = salesTransactionRepository
                .findByCustomerIdAndTransactionDateBetween(customerId, startOfDay, endOfDay);
        
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
        
        // Use database query to filter by store ID instead of fetching all and filtering in memory
        List<SalesTransaction> storeTransactions = salesTransactionRepository
                .findByStoreIdAndTransactionDateBetween(storeId, startOfDay, endOfDay);
        
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

    private List<PerformanceMetrics> computeStorePerformanceFromTransactions(LocalDate reportDate) {
        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = reportDate.atTime(23, 59, 59);
        
        List<SalesTransaction> transactions = salesTransactionRepository
                .findByTransactionDateBetween(startOfDay, endOfDay);
        
        // Group transactions by store
        return transactions.stream()
                .collect(Collectors.groupingBy(SalesTransaction::getStoreId))
                .entrySet().stream()
                .map(entry -> {
                    String storeId = entry.getKey();
                    List<SalesTransaction> storeTransactions = entry.getValue();
                    
                    BigDecimal totalRevenue = storeTransactions.stream()
                            .map(SalesTransaction::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    int uniqueCustomers = storeTransactions.stream()
                            .map(SalesTransaction::getCustomerId)
                            .collect(Collectors.toSet())
                            .size();
                    
                    BigDecimal avgOrderValue = storeTransactions.isEmpty() ? BigDecimal.ZERO :
                            totalRevenue.divide(BigDecimal.valueOf(storeTransactions.size()), 2, RoundingMode.HALF_UP);
                    
                    return PerformanceMetrics.builder()
                            .storeId(storeId)
                            .period(reportDate.toString())
                            .totalRevenue(totalRevenue)
                            .totalTransactions(storeTransactions.size())
                            .uniqueCustomers(uniqueCustomers)
                            .averageOrderValue(avgOrderValue)
                            .status("Active")
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<CustomerAnalyticsResponse> computeTopCustomersFromTransactions(LocalDate reportDate, int limit) {
        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = reportDate.atTime(23, 59, 59);
        
        List<SalesTransaction> transactions = salesTransactionRepository
                .findByTransactionDateBetween(startOfDay, endOfDay);
        
        // Group transactions by customer and calculate aggregates
        return transactions.stream()
                .collect(Collectors.groupingBy(SalesTransaction::getCustomerId))
                .entrySet().stream()
                .map(entry -> {
                    String customerId = entry.getKey();
                    List<SalesTransaction> customerTransactions = entry.getValue();
                    
                    BigDecimal totalSpent = customerTransactions.stream()
                            .map(SalesTransaction::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal avgOrderValue = totalSpent.divide(
                            BigDecimal.valueOf(customerTransactions.size()), 2, RoundingMode.HALF_UP);
                    
                    return CustomerAnalyticsResponse.builder()
                            .customerId(customerId)
                            .customerName(customerTransactions.get(0).getCustomerName())
                            .totalPurchases(customerTransactions.size())
                            .totalSpent(totalSpent)
                            .averageOrderValue(avgOrderValue)
                            .build();
                })
                .sorted((c1, c2) -> c2.getTotalSpent().compareTo(c1.getTotalSpent()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<ProductSalesResponse> computeTopProductsFromTransactions(LocalDate reportDate, int limit) {
        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = reportDate.atTime(23, 59, 59);
        
        List<SalesTransaction> transactions = salesTransactionRepository
                .findByTransactionDateBetween(startOfDay, endOfDay);
        
        // Parse items and aggregate by product using regex
        java.util.Map<String, ProductAggregation> productMap = new java.util.HashMap<>();
        
        for (SalesTransaction transaction : transactions) {
            if (transaction.getItems() != null && !transaction.getItems().trim().isEmpty()) {
                try {
                    String items = transaction.getItems();
                    // Use regex to extract product information
                    java.util.regex.Pattern productPattern = java.util.regex.Pattern.compile(
                            "\"productCode\":\\s*\"([^\"]+)\".*?\"productName\":\\s*\"([^\"]+)\".*?\"quantity\":\\s*(\\d+).*?\"totalPrice\":\\s*([\\d.]+)"
                    );
                    java.util.regex.Matcher matcher = productPattern.matcher(items);
                    
                    while (matcher.find()) {
                        String productCode = matcher.group(1);
                        String productName = matcher.group(2);
                        int quantity = Integer.parseInt(matcher.group(3));
                        BigDecimal totalPrice = new BigDecimal(matcher.group(4));
                        
                        ProductAggregation agg = productMap.getOrDefault(productCode, new ProductAggregation());
                        if (agg.productCode == null) {
                            agg.productCode = productCode;
                            agg.productName = productName;
                        }
                        agg.quantity += quantity;
                        agg.revenue = agg.revenue.add(totalPrice);
                        agg.transactionCount++;
                        productMap.put(productCode, agg);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse items JSON: {}", e.getMessage());
                }
            }
        }
        
        return productMap.values().stream()
                .map(agg -> ProductSalesResponse.builder()
                        .productCode(agg.productCode)
                        .productName(agg.productName)
                        .reportDate(reportDate)
                        .quantitySold(agg.quantity)
                        .totalRevenue(agg.revenue)
                        .averagePrice(agg.quantity > 0 ? 
                                agg.revenue.divide(BigDecimal.valueOf(agg.quantity), 2, RoundingMode.HALF_UP) : 
                                BigDecimal.ZERO)
                        .transactionCount(agg.transactionCount)
                        .build())
                .sorted((p1, p2) -> p2.getTotalRevenue().compareTo(p1.getTotalRevenue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static class ProductAggregation {
        String productCode;
        String productName;
        int quantity = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        int transactionCount = 0;
    }
}
