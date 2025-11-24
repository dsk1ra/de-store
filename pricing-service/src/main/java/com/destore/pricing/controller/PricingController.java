package com.destore.pricing.controller;

import com.destore.dto.ApiResponse;
import com.destore.pricing.dto.*;
import com.destore.pricing.entity.Product;
import com.destore.pricing.entity.Promotion;
import com.destore.pricing.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {
    
    private final PricingService pricingService;
    
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody ProductRequest request) {
        try {
            Product product = pricingService.createProduct(request);
            return ResponseEntity.ok(ApiResponse.success("Product created successfully", product));
        } catch (Exception e) {
            log.error("Failed to create product", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/products/{productCode}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable String productCode) {
        try {
            Product product = pricingService.getProduct(productCode);
            return ResponseEntity.ok(ApiResponse.success(product));
        } catch (Exception e) {
            log.error("Failed to get product", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/products/{productCode}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable String productCode,
            @RequestBody ProductRequest request) {
        try {
            Product product = pricingService.updateProduct(productCode, request);
            return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
        } catch (Exception e) {
            log.error("Failed to update product", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/products/{productCode}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String productCode) {
        try {
            pricingService.deleteProduct(productCode);
            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
        } catch (Exception e) {
            log.error("Failed to delete product", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        try {
            List<Product> products = pricingService.getAllProducts();
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            log.error("Failed to get products", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/products/search")
    public ResponseEntity<ApiResponse<List<Product>>> searchProducts(@RequestParam String q) {
        try {
            List<Product> products = pricingService.searchProducts(q);
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            log.error("Failed to search products", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<Promotion>> createPromotion(@RequestBody PromotionRequest request) {
        try {
            Promotion promotion = pricingService.createPromotion(request);
            return ResponseEntity.ok(ApiResponse.success("Promotion created successfully", promotion));
        } catch (Exception e) {
            log.error("Failed to create promotion", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse<List<Promotion>>> getActivePromotions() {
        try {
            List<Promotion> promotions = pricingService.getActivePromotions();
            return ResponseEntity.ok(ApiResponse.success(promotions));
        } catch (Exception e) {
            log.error("Failed to get promotions", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<PriceCalculationResponse>> calculatePrice(
            @RequestBody PriceCalculationRequest request) {
        try {
            PriceCalculationResponse response = pricingService.calculatePrice(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to calculate price", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pricing Service is healthy");
    }
}
