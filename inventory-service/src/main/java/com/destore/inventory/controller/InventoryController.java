package com.destore.inventory.controller;

import com.destore.dto.ApiResponse;
import com.destore.inventory.dto.*;
import com.destore.inventory.entity.Inventory;
import com.destore.inventory.entity.ReservationStatus;
import com.destore.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Inventory>> createInventory(@Valid @RequestBody InventoryCreateRequest request) {
        try {
            Inventory inventory = inventoryService.createInventory(request);
            return ResponseEntity.ok(new ApiResponse<Inventory>(true, "Inventory created successfully", inventory));
        } catch (Exception e) {
            log.error("Error creating inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<Inventory>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/{productCode}")
    public ResponseEntity<?> getInventory(
            @PathVariable String productCode,
            @RequestParam(required = false) String storeId) {
        try {
            if (storeId != null && !storeId.isEmpty()) {
                // Return single inventory for specific store
                Inventory inventory = inventoryService.getInventory(productCode, storeId);
                return ResponseEntity.ok(new ApiResponse<Inventory>(true, "Inventory retrieved successfully", inventory));
            } else {
                // Return all inventory for this product across all stores
                List<Inventory> inventories = inventoryService.getAllInventoryByProduct(productCode);
                if (inventories.isEmpty()) {
                    return ResponseEntity.badRequest().body(new ApiResponse<List<Inventory>>(false, 
                        "Inventory not found for product: " + productCode, null));
                }
                return ResponseEntity.ok(new ApiResponse<List<Inventory>>(true, 
                    "Inventory retrieved for " + inventories.size() + " store(s)", inventories));
            }
        } catch (Exception e) {
            log.error("Error retrieving inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PutMapping("/{productCode}")
    public ResponseEntity<ApiResponse<Inventory>> updateInventoryDirect(
            @PathVariable String productCode,
            @RequestBody InventoryUpdateRequest request) {
        try {
            Inventory inventory = inventoryService.updateQuantity(productCode, request.getQuantity(),
                    request.getTransactionType(), request.getLowStockThreshold());
            return ResponseEntity.ok(new ApiResponse<Inventory>(true, "Inventory updated successfully", inventory));
        } catch (Exception e) {
            log.error("Error updating inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<Inventory>(false, e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Inventory>>> getAllInventory(
            @RequestParam(required = false) String storeId) {
        try {
            List<Inventory> inventories;
            if (storeId != null) {
                inventories = inventoryService.getAllInventoryByStore(storeId);
            } else {
                inventories = inventoryService.getAllInventory();
            }
            return ResponseEntity
                    .ok(new ApiResponse<List<Inventory>>(true, "All inventory retrieved successfully", inventories));
        } catch (Exception e) {
            log.error("Error retrieving all inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<List<Inventory>>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{productCode}/reserve")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveStock(
            @PathVariable String productCode,
            @Valid @RequestBody ReservationRequest request) {
        try {
            ReservationResponse response = inventoryService.reserveStock(productCode, request);
            return ResponseEntity
                    .ok(new ApiResponse<ReservationResponse>(true, "Stock reserved successfully", response));
        } catch (Exception e) {
            log.error("Error reserving stock", e);
            return ResponseEntity.badRequest().body(new ApiResponse<ReservationResponse>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{productCode}/deduct")
    public ResponseEntity<ApiResponse<Inventory>> deductStock(
            @PathVariable String productCode,
            @RequestParam Integer quantity) {
        try {
            Inventory inventory = inventoryService.deductStock(productCode, quantity);
            return ResponseEntity.ok(new ApiResponse<Inventory>(true, "Stock deducted successfully", inventory));
        } catch (Exception e) {
            log.error("Error deducting stock", e);
            return ResponseEntity.badRequest().body(new ApiResponse<Inventory>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{productCode}/restock")
    public ResponseEntity<ApiResponse<Inventory>> restockInventory(
            @PathVariable String productCode,
            @RequestParam Integer quantity) {
        try {
            Inventory inventory = inventoryService.restockInventory(productCode, quantity);
            return ResponseEntity.ok(new ApiResponse<Inventory>(true, "Inventory restocked successfully", inventory));
        } catch (Exception e) {
            log.error("Error restocking inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<Inventory>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<Inventory>>> getLowStockItems(
            @RequestParam(required = false) String storeId) {
        try {
            List<Inventory> lowStockItems = inventoryService.getLowStockItems(storeId);
            return ResponseEntity.ok(new ApiResponse<List<Inventory>>(true,
                    "Found " + lowStockItems.size() + " low stock item(s)", lowStockItems));
        } catch (Exception e) {
            log.error("Error retrieving low stock items", e);
            return ResponseEntity.badRequest().body(new ApiResponse<List<Inventory>>(false, e.getMessage(), null));
        }
    }

    // Reservation Management Endpoints

    @PostMapping("/reservations/confirm")
    public ResponseEntity<ApiResponse<ReservationDetailsResponse>> confirmReservation(
            @RequestBody ConfirmReservationRequest request) {
        try {
            ReservationDetailsResponse response = inventoryService.confirmReservation(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Reservation confirmed successfully", response));
        } catch (Exception e) {
            log.error("Error confirming reservation", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/reservations/cancel")
    public ResponseEntity<ApiResponse<ReservationDetailsResponse>> cancelReservation(
            @RequestBody CancelReservationRequest request) {
        try {
            ReservationDetailsResponse response = inventoryService.cancelReservation(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Reservation cancelled successfully", response));
        } catch (Exception e) {
            log.error("Error cancelling reservation", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationDetailsResponse>> getReservation(
            @PathVariable String reservationId) {
        try {
            ReservationDetailsResponse response = inventoryService.getReservation(reservationId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Reservation retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error retrieving reservation", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/reservations")
    public ResponseEntity<ApiResponse<List<ReservationDetailsResponse>>> getAllReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productCode) {
        try {
            List<ReservationDetailsResponse> reservations;

            if (status != null) {
                ReservationStatus reservationStatus = ReservationStatus.valueOf(status.toUpperCase());
                reservations = inventoryService.getReservationsByStatus(reservationStatus);
            } else if (productCode != null) {
                reservations = inventoryService.getReservationsByProduct(productCode);
            } else {
                reservations = inventoryService.getAllReservations();
            }

            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Found " + reservations.size() + " reservation(s)", reservations));
        } catch (Exception e) {
            log.error("Error retrieving reservations", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/reservations/release-expired")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> releaseExpiredReservations() {
        try {
            int count = inventoryService.releaseExpiredReservations();
            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Released " + count + " expired reservation(s)",
                    Map.of("releasedCount", count)));
        } catch (Exception e) {
            log.error("Error releasing expired reservations", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(new ApiResponse<String>(true, "Inventory service is running", "OK"));
    }
}
