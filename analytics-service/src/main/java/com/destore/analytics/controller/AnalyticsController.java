package com.destore.analytics.controller;

import com.destore.analytics.dto.*;
import com.destore.analytics.service.AnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/transactions")
    public ResponseEntity<String> trackTransaction(@Valid @RequestBody TransactionRequest request) {
        log.info("Received transaction tracking request: {}", request.getTransactionId());
        try {
            analyticsService.trackTransaction(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Transaction tracked successfully");
        } catch (Exception e) {
            log.error("Error tracking transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error tracking transaction: " + e.getMessage());
        }
    }

    @GetMapping("/reports/sales")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Generating sales report from {} to {}", startDate, endDate);
        try {
            SalesReportResponse report = analyticsService.generateSalesReport(startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating sales report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports/performance")
    public ResponseEntity<List<PerformanceMetrics>> getStorePerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        log.info("Getting store performance for date: {}", reportDate);
        try {
            List<PerformanceMetrics> metrics = analyticsService.getStorePerformance(reportDate);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting store performance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/customers/top")
    public ResponseEntity<List<CustomerAnalyticsResponse>> getTopCustomers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting top {} customers for date: {}", limit, reportDate);
        try {
            List<CustomerAnalyticsResponse> topCustomers = analyticsService.getTopCustomers(reportDate, limit);
            return ResponseEntity.ok(topCustomers);
        } catch (Exception e) {
            log.error("Error getting top customers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/products/top")
    public ResponseEntity<List<ProductSalesResponse>> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting top {} products for date: {}", limit, reportDate);
        try {
            List<ProductSalesResponse> topProducts = analyticsService.getTopSellingProducts(reportDate, limit);
            return ResponseEntity.ok(topProducts);
        } catch (Exception e) {
            log.error("Error getting top products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Analytics Service is running");
    }
}
