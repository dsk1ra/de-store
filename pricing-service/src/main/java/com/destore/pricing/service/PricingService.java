package com.destore.pricing.service;

import com.destore.pricing.dto.*;
import com.destore.pricing.entity.Product;
import com.destore.pricing.entity.Promotion;
import com.destore.pricing.repository.ProductRepository;
import com.destore.pricing.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;

    @Transactional
    public Product createProduct(ProductRequest request) {
        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new RuntimeException("Product already exists with code: " + request.getProductCode());
        }

        Product product = Product.builder()
                .productCode(request.getProductCode())
                .productName(request.getProductName())
                .basePrice(request.getBasePrice())
                .build();

        @SuppressWarnings("null")
        Product saved = productRepository.save(product);
        log.info("Created product: {}", saved.getProductCode());
        return saved;
    }

    public Product getProduct(String productCode) {
        return productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product updateProduct(String productCode, ProductRequest request) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));

        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
        }
        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }

        Product updated = productRepository.save(product);
        log.info("Updated product: {}", updated.getProductCode());
        return updated;
    }

    @Transactional
    public void deleteProduct(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productCode));

        productRepository.delete(product);
        log.info("Deleted product: {}", productCode);
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }

        String searchTerm = query.toLowerCase();
        return productRepository.findAll().stream()
                .filter(p -> p.getProductName().toLowerCase().contains(searchTerm)
                || p.getProductCode().toLowerCase().contains(searchTerm))
                .toList();
    }

    @Transactional
    public Promotion createPromotion(PromotionRequest request) {
        // Check if promotion code already exists
        if (promotionRepository.findByPromotionCode(request.getPromotionCode()).isPresent()) {
            throw new RuntimeException("Promotion with code '" + request.getPromotionCode() + "' already exists");
        }
        
        // Validate dates
        if (request.getStartDate() != null && request.getEndDate() != null && 
            request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Start date cannot be after end date");
        }
        
        // Validate discountValue based on promotion type
        validatePromotionDiscount(request);
        
        String productsString = request.getApplicableProducts() != null
                ? String.join(",", request.getApplicableProducts()) : "";

        // Normalize: Set discountValue to null for BOGO and THREE_FOR_TWO
        BigDecimal normalizedDiscountValue = null;
        if (request.getPromotionType() == com.destore.pricing.entity.PromotionType.PERCENTAGE_DISCOUNT ||
            request.getPromotionType() == com.destore.pricing.entity.PromotionType.FIXED_AMOUNT) {
            normalizedDiscountValue = request.getDiscountValue();
        }

        Promotion promotion = Promotion.builder()
                .promotionCode(request.getPromotionCode())
                .promotionName(request.getPromotionName())
                .promotionType(request.getPromotionType())
                .discountValue(normalizedDiscountValue)
                .applicableProducts(productsString)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .build();

        @SuppressWarnings("null")
        Promotion saved = promotionRepository.save(promotion);
        log.info("Created promotion: {}", saved.getPromotionCode());
        return saved;
    }

    public List<Promotion> getActivePromotions() {
        LocalDate today = LocalDate.now();
        return promotionRepository.findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                today, today);
    }

    public PriceCalculationResponse calculatePrice(PriceCalculationRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<String> appliedPromotions = new ArrayList<>();
        List<PriceCalculationResponse.ItemBreakdown> itemBreakdowns = new ArrayList<>();

        LocalDate today = LocalDate.now();
        List<Promotion> activePromotions = promotionRepository
                .findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today);

        for (PriceCalculationRequest.Item item : request.getItems()) {
            Product product = getProduct(item.getProductCode());
            BigDecimal itemSubtotal = product.getBasePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            BigDecimal itemDiscount = BigDecimal.ZERO;

            // Apply promotions
            for (Promotion promo : activePromotions) {
                if (isProductApplicable(promo, item.getProductCode())) {
                    BigDecimal promoDiscount = calculatePromotionDiscount(
                            promo, item.getQuantity(), product.getBasePrice());
                    itemDiscount = itemDiscount.add(promoDiscount);

                    if (!appliedPromotions.contains(promo.getPromotionCode())) {
                        appliedPromotions.add(promo.getPromotionCode());
                    }
                }
            }

            totalDiscount = totalDiscount.add(itemDiscount);

            itemBreakdowns.add(PriceCalculationResponse.ItemBreakdown.builder()
                    .productCode(item.getProductCode())
                    .quantity(item.getQuantity())
                    .unitPrice(product.getBasePrice())
                    .subtotal(itemSubtotal)
                    .discount(itemDiscount)
                    .total(itemSubtotal.subtract(itemDiscount))
                    .build());
        }

        return PriceCalculationResponse.builder()
                .subtotal(subtotal)
                .promotionalDiscount(totalDiscount)
                .finalTotal(subtotal.subtract(totalDiscount))
                .appliedPromotions(appliedPromotions)
                .itemBreakdown(itemBreakdowns)
                .build();
    }

    private boolean isProductApplicable(Promotion promo, String productCode) {
        if (promo.getApplicableProducts() == null || promo.getApplicableProducts().isEmpty()) {
            return false;
        }
        List<String> products = Arrays.asList(promo.getApplicableProducts().split(","));
        return products.contains(productCode);
    }

    private BigDecimal calculatePromotionDiscount(Promotion promo, int quantity, BigDecimal unitPrice) {
        switch (promo.getPromotionType()) {
            case BOGO:
                int freeItems = quantity / 2;
                return unitPrice.multiply(BigDecimal.valueOf(freeItems));

            case THREE_FOR_TWO:
                int setsOf3 = quantity / 3;
                return unitPrice.multiply(BigDecimal.valueOf(setsOf3));

            case PERCENTAGE_DISCOUNT:
                return unitPrice.multiply(BigDecimal.valueOf(quantity))
                        .multiply(promo.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            case FIXED_AMOUNT:
                return promo.getDiscountValue().multiply(BigDecimal.valueOf(quantity));

            default:
                return BigDecimal.ZERO;
        }
    }

    private void validatePromotionDiscount(PromotionRequest request) {
        switch (request.getPromotionType()) {
            case PERCENTAGE_DISCOUNT:
                if (request.getDiscountValue() == null) {
                    throw new IllegalArgumentException(
                            "Discount value is required for PERCENTAGE_DISCOUNT promotions");
                }
                if (request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0 ||
                    request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException(
                            "Percentage discount must be between 0 and 100");
                }
                break;

            case FIXED_AMOUNT:
                if (request.getDiscountValue() == null) {
                    throw new IllegalArgumentException(
                            "Discount value is required for FIXED_AMOUNT promotions");
                }
                if (request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException(
                            "Fixed amount discount must be greater than 0");
                }
                break;

            case BOGO:
            case THREE_FOR_TWO:
                // These promotion types don't use discountValue
                // It will be set to null during creation
                break;

            default:
                throw new IllegalArgumentException("Invalid promotion type: " + request.getPromotionType());
        }
    }
}
