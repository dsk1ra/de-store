package com.destore.delivery.controller;

import com.destore.delivery.dto.DeliveryChargeRequest;
import com.destore.delivery.dto.DeliveryChargeResponse;
import com.destore.delivery.dto.DeliveryOrderResponse;
import com.destore.delivery.dto.DeliveryStatusUpdateRequest;
import com.destore.delivery.entity.DeliveryStatus;
import com.destore.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/calculate")
    public ResponseEntity<DeliveryChargeResponse> calculateDeliveryCharge(
            @Valid @RequestBody DeliveryChargeRequest request) {
        log.info("Received delivery charge calculation request for order: {}", request.getOrderId());
        try {
            DeliveryChargeResponse response = deliveryService.calculateDeliveryCharge(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating delivery charge: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<DeliveryOrderResponse> getDeliveryOrder(@PathVariable String orderId) {
        log.info("Fetching delivery order: {}", orderId);
        try {
            DeliveryOrderResponse response = deliveryService.getDeliveryOrder(orderId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Delivery order not found: {}", orderId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching delivery order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<DeliveryOrderResponse>> getCustomerDeliveries(@PathVariable String customerId) {
        log.info("Fetching deliveries for customer: {}", customerId);
        try {
            List<DeliveryOrderResponse> deliveries = deliveryService.getDeliveriesByCustomer(customerId);
            return ResponseEntity.ok(deliveries);
        } catch (Exception e) {
            log.error("Error fetching customer deliveries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DeliveryOrderResponse>> getDeliveriesByStatus(@PathVariable DeliveryStatus status) {
        log.info("Fetching deliveries with status: {}", status);
        try {
            List<DeliveryOrderResponse> deliveries = deliveryService.getDeliveriesByStatus(status);
            return ResponseEntity.ok(deliveries);
        } catch (Exception e) {
            log.error("Error fetching deliveries by status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/status")
    public ResponseEntity<DeliveryOrderResponse> updateDeliveryStatus(
            @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        log.info("Updating delivery status for order: {}", request.getOrderId());
        try {
            DeliveryOrderResponse response = deliveryService.updateDeliveryStatus(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Delivery order not found: {}", request.getOrderId());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating delivery status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Delivery Service is running");
    }
}
