package com.destore.loyalty.controller;

import com.destore.dto.ApiResponse;
import com.destore.loyalty.dto.LoyaltyPromotionRequest;
import com.destore.loyalty.dto.LoyaltyPromotionResponse;
import com.destore.loyalty.entity.LoyaltyPromotion;
import com.destore.loyalty.service.LoyaltyPromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loyalty/promotions")
@RequiredArgsConstructor
@Slf4j
public class LoyaltyPromotionController {
    
    private final LoyaltyPromotionService promotionService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<LoyaltyPromotionResponse>> createPromotion(
            @Valid @RequestBody LoyaltyPromotionRequest request) {
        try {
            LoyaltyPromotion promotion = promotionService.createPromotion(request);
            LoyaltyPromotionResponse response = LoyaltyPromotionResponse.fromEntity(promotion);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Loyalty promotion created successfully", response));
        } catch (Exception e) {
            log.error("Error creating loyalty promotion", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/{promotionCode}")
    public ResponseEntity<ApiResponse<LoyaltyPromotionResponse>> getPromotion(@PathVariable String promotionCode) {
        try {
            LoyaltyPromotion promotion = promotionService.getPromotion(promotionCode);
            LoyaltyPromotionResponse response = LoyaltyPromotionResponse.fromEntity(promotion);
            return ResponseEntity.ok(new ApiResponse<>(true, "Promotion retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error retrieving promotion", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoyaltyPromotionResponse>>> getAllPromotions() {
        try {
            List<LoyaltyPromotion> promotions = promotionService.getAllPromotions();
            List<LoyaltyPromotionResponse> responses = promotions.stream()
                    .map(LoyaltyPromotionResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, "Promotions retrieved successfully", responses));
        } catch (Exception e) {
            log.error("Error retrieving promotions", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LoyaltyPromotionResponse>>> getActivePromotions() {
        try {
            List<LoyaltyPromotion> promotions = promotionService.getActivePromotions();
            List<LoyaltyPromotionResponse> responses = promotions.stream()
                    .map(LoyaltyPromotionResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, "Active promotions retrieved", responses));
        } catch (Exception e) {
            log.error("Error retrieving active promotions", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<LoyaltyPromotionResponse>>> getPromotionsForCustomer(
            @PathVariable String customerId) {
        try {
            List<LoyaltyPromotion> promotions = promotionService.getPromotionsForCustomer(customerId);
            List<LoyaltyPromotionResponse> responses = promotions.stream()
                    .map(LoyaltyPromotionResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, "Customer promotions retrieved", responses));
        } catch (Exception e) {
            log.error("Error retrieving customer promotions", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @DeleteMapping("/{promotionCode}")
    public ResponseEntity<ApiResponse<Void>> deactivatePromotion(@PathVariable String promotionCode) {
        try {
            promotionService.deactivatePromotion(promotionCode);
            return ResponseEntity.ok(new ApiResponse<>(true, "Promotion deactivated successfully", null));
        } catch (Exception e) {
            log.error("Error deactivating promotion", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Loyalty Service - Promotion Controller is running");
    }
}
