package com.destore.delivery.service;

import com.destore.delivery.dto.DeliveryChargeRequest;
import com.destore.delivery.dto.DeliveryChargeResponse;
import com.destore.delivery.dto.DeliveryOrderResponse;
import com.destore.delivery.dto.DeliveryStatusUpdateRequest;
import com.destore.delivery.entity.*;
import com.destore.delivery.repository.DeliveryOrderRepository;
import com.destore.delivery.repository.DeliveryPricingRepository;
import com.destore.delivery.repository.DeliveryZoneConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryPricingRepository deliveryPricingRepository;
    private final DeliveryZoneConfigRepository deliveryZoneConfigRepository;

    @Transactional
    public DeliveryChargeResponse calculateDeliveryCharge(DeliveryChargeRequest request) {
        log.info("Calculating delivery charge for order: {}", request.getOrderId());
        
        // Get active pricing configuration
        DeliveryPricing pricing = deliveryPricingRepository.findActivePrice()
                .orElseThrow(() -> new RuntimeException("No active pricing configuration found"));
        
        // Determine delivery zone based on distance
        DeliveryZone zone = determineZone(request.getDistance());
        DeliveryZoneConfig zoneConfig = deliveryZoneConfigRepository.findByZone(zone)
                .orElseThrow(() -> new RuntimeException("Zone configuration not found for: " + zone));
        
        // Check if it's peak hour
        boolean isPeakHour = isPeakHour(LocalTime.now());
        
        // Calculate charges
        BigDecimal baseCharge = pricing.getBaseCharge();
        BigDecimal distanceCharge = calculateDistanceCharge(request.getDistance(), pricing);
        BigDecimal zoneCharge = calculateZoneCharge(distanceCharge, zoneConfig);
        BigDecimal expressCharge = request.getIsExpress() ? pricing.getExpressSurcharge() : BigDecimal.ZERO;
        BigDecimal peakHourCharge = isPeakHour ? 
                calculatePeakHourCharge(baseCharge.add(distanceCharge).add(zoneCharge), pricing) : 
                BigDecimal.ZERO;
        
        // Calculate total before discount
        BigDecimal subtotal = baseCharge.add(distanceCharge).add(zoneCharge)
                .add(expressCharge).add(peakHourCharge);
        
        // Apply discounts based on order value
        BigDecimal discount = calculateDiscount(request.getOrderValue(), subtotal, pricing);
        String discountReason = getDiscountReason(request.getOrderValue(), pricing);
        
        // Calculate final delivery charge
        BigDecimal totalDeliveryCharge = subtotal.subtract(discount).max(BigDecimal.ZERO);
        
        // Build charge breakdown
        String breakdown = buildChargeBreakdown(baseCharge, distanceCharge, zoneCharge, 
                expressCharge, peakHourCharge, discount);
        
        // Save delivery order
        DeliveryOrder order = saveDeliveryOrder(request, zone, isPeakHour, pricing, 
                totalDeliveryCharge, baseCharge, distanceCharge, zoneCharge, 
                expressCharge, peakHourCharge, discount);
        
        log.info("Delivery charge calculated for order {}: ${}", request.getOrderId(), totalDeliveryCharge);
        
        return DeliveryChargeResponse.builder()
                .orderId(request.getOrderId())
                .orderValue(request.getOrderValue())
                .distance(request.getDistance())
                .zone(zone)
                .isExpress(request.getIsExpress())
                .isPeakHour(isPeakHour)
                .baseCharge(baseCharge)
                .distanceCharge(distanceCharge)
                .zoneCharge(zoneCharge)
                .expressCharge(expressCharge)
                .peakHourCharge(peakHourCharge)
                .discount(discount)
                .totalDeliveryCharge(totalDeliveryCharge)
                .discountReason(discountReason)
                .chargeBreakdown(breakdown)
                .grandTotal(request.getOrderValue().add(totalDeliveryCharge))
                .build();
    }

    private DeliveryZone determineZone(BigDecimal distance) {
        if (distance.compareTo(BigDecimal.valueOf(10)) <= 0) {
            return DeliveryZone.CITY_CENTER;
        } else if (distance.compareTo(BigDecimal.valueOf(25)) <= 0) {
            return DeliveryZone.SUBURBAN;
        } else if (distance.compareTo(BigDecimal.valueOf(50)) <= 0) {
            return DeliveryZone.RURAL;
        } else {
            return DeliveryZone.REMOTE;
        }
    }

    private BigDecimal calculateDistanceCharge(BigDecimal distance, DeliveryPricing pricing) {
        // Free distance threshold
        BigDecimal chargeableDistance = distance.subtract(pricing.getFreeDistanceThreshold()).max(BigDecimal.ZERO);
        return chargeableDistance.multiply(pricing.getRatePerKm()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateZoneCharge(BigDecimal distanceCharge, DeliveryZoneConfig zoneConfig) {
        // Apply zone multiplier to distance charge
        BigDecimal zoneMultiplier = zoneConfig.getMultiplier().subtract(BigDecimal.ONE);
        return distanceCharge.multiply(zoneMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePeakHourCharge(BigDecimal baseAmount, DeliveryPricing pricing) {
        return baseAmount.multiply(pricing.getPeakHourSurchargePercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDiscount(BigDecimal orderValue, BigDecimal deliveryCharge, DeliveryPricing pricing) {
        // Free delivery for orders above threshold
        if (orderValue.compareTo(pricing.getFreeDeliveryThreshold()) >= 0) {
            return deliveryCharge;  // Full discount
        }
        
        // Reduced rate for orders above reduced threshold
        if (orderValue.compareTo(pricing.getReducedRateThreshold()) >= 0) {
            return deliveryCharge.multiply(pricing.getReducedRatePercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }

    private String getDiscountReason(BigDecimal orderValue, DeliveryPricing pricing) {
        if (orderValue.compareTo(pricing.getFreeDeliveryThreshold()) >= 0) {
            return "Free delivery for orders over $" + pricing.getFreeDeliveryThreshold();
        }
        if (orderValue.compareTo(pricing.getReducedRateThreshold()) >= 0) {
            return pricing.getReducedRatePercentage() + "% off delivery for orders over $" + pricing.getReducedRateThreshold();
        }
        return "No discount applied";
    }

    private String buildChargeBreakdown(BigDecimal base, BigDecimal distance, BigDecimal zone,
                                       BigDecimal express, BigDecimal peak, BigDecimal discount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Base Charge: $").append(base).append("\n");
        sb.append("Distance Charge: $").append(distance).append("\n");
        if (zone.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Zone Surcharge: $").append(zone).append("\n");
        }
        if (express.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Express Delivery: $").append(express).append("\n");
        }
        if (peak.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Peak Hour Surcharge: $").append(peak).append("\n");
        }
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: -$").append(discount).append("\n");
        }
        return sb.toString();
    }

    private boolean isPeakHour(LocalTime time) {
        // Peak hours: 11:00-13:00 and 18:00-20:00
        return (time.isAfter(LocalTime.of(11, 0)) && time.isBefore(LocalTime.of(13, 0))) ||
               (time.isAfter(LocalTime.of(18, 0)) && time.isBefore(LocalTime.of(20, 0)));
    }

    private DeliveryOrder saveDeliveryOrder(DeliveryChargeRequest request, DeliveryZone zone,
                                           boolean isPeakHour, DeliveryPricing pricing,
                                           BigDecimal totalCharge, BigDecimal baseCharge,
                                           BigDecimal distanceCharge, BigDecimal zoneCharge,
                                           BigDecimal expressCharge, BigDecimal peakHourCharge,
                                           BigDecimal discount) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime estimatedDelivery = request.getIsExpress() ? 
                now.plusHours(1) : now.plusHours(3);
        
        DeliveryOrder order = DeliveryOrder.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .storeId(request.getStoreId())
                .orderValue(request.getOrderValue())
                .deliveryCharge(totalCharge)
                .distance(request.getDistance())
                .zone(zone)
                .isExpress(request.getIsExpress())
                .isPeakHour(isPeakHour)
                .status(DeliveryStatus.PENDING)
                .deliveryAddress(request.getDeliveryAddress())
                .pickupAddress(request.getPickupAddress())
                .orderDateTime(now)
                .estimatedDeliveryTime(estimatedDelivery)
                .notes(request.getNotes())
                .baseCharge(baseCharge)
                .distanceCharge(distanceCharge)
                .zoneCharge(zoneCharge)
                .expressCharge(expressCharge)
                .peakHourCharge(peakHourCharge)
                .discount(discount)
                .build();
        
        return deliveryOrderRepository.save(order);
    }

    public DeliveryOrderResponse getDeliveryOrder(String orderId) {
        log.info("Fetching delivery order: {}", orderId);
        DeliveryOrder order = deliveryOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery order not found: " + orderId));
        return convertToResponse(order);
    }

    public List<DeliveryOrderResponse> getDeliveriesByCustomer(String customerId) {
        log.info("Fetching deliveries for customer: {}", customerId);
        return deliveryOrderRepository.findByCustomerId(customerId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<DeliveryOrderResponse> getDeliveriesByStatus(DeliveryStatus status) {
        log.info("Fetching deliveries with status: {}", status);
        return deliveryOrderRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryOrderResponse updateDeliveryStatus(DeliveryStatusUpdateRequest request) {
        log.info("Updating delivery status for order: {} to {}", request.getOrderId(), request.getStatus());
        
        DeliveryOrder order = deliveryOrderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Delivery order not found: " + request.getOrderId()));
        
        order.setStatus(request.getStatus());
        
        if (request.getDriverName() != null) {
            order.setDriverName(request.getDriverName());
        }
        if (request.getDriverPhone() != null) {
            order.setDriverPhone(request.getDriverPhone());
        }
        if (request.getVehicleNumber() != null) {
            order.setVehicleNumber(request.getVehicleNumber());
        }
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        
        // Update actual delivery time when delivered
        if (request.getStatus() == DeliveryStatus.DELIVERED) {
            order.setActualDeliveryTime(LocalDateTime.now());
        }
        
        DeliveryOrder updated = deliveryOrderRepository.save(order);
        log.info("Delivery status updated successfully for order: {}", request.getOrderId());
        
        return convertToResponse(updated);
    }

    private DeliveryOrderResponse convertToResponse(DeliveryOrder order) {
        return DeliveryOrderResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .storeId(order.getStoreId())
                .orderValue(order.getOrderValue())
                .deliveryCharge(order.getDeliveryCharge())
                .distance(order.getDistance())
                .zone(order.getZone().name())
                .isExpress(order.getIsExpress())
                .status(order.getStatus())
                .deliveryAddress(order.getDeliveryAddress())
                .orderDateTime(order.getOrderDateTime())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .actualDeliveryTime(order.getActualDeliveryTime())
                .driverName(order.getDriverName())
                .driverPhone(order.getDriverPhone())
                .vehicleNumber(order.getVehicleNumber())
                .notes(order.getNotes())
                .build();
    }
}
