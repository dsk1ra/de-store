package com.destore.inventory.controller;

import com.destore.dto.ApiResponse;
import com.destore.inventory.dto.InventoryCreateRequest;
import com.destore.inventory.dto.InventoryUpdateRequest;
import com.destore.inventory.dto.ReservationRequest;
import com.destore.inventory.dto.ReservationResponse;
import com.destore.inventory.entity.Inventory;
import com.destore.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Inventory>> createInventory(@RequestBody InventoryCreateRequest request) {
        try {
            Inventory inventory = inventoryService.createInventory(request);
            return ResponseEntity.ok(new ApiResponse<Inventory>(true, "Inventory created successfully", inventory));
        } catch (Exception e) {
            log.error("Error creating inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<Inventory>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/{productCode}")
    public ResponseEntity<ApiResponse<Inventory>> getInventory(@PathVariable String productCode) {
        try {
            Inventory inventory = inventoryService.getInventory(productCode);
            return ResponseEntity.ok(new ApiResponse<Inventory>(true, "Inventory retrieved successfully", inventory));
        } catch (Exception e) {
            log.error("Error retrieving inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<Inventory>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Inventory>>> getAllInventory() {
        try {
            List<Inventory> inventories = inventoryService.getAllInventory();
            return ResponseEntity.ok(new ApiResponse<List<Inventory>>(true, "All inventory retrieved successfully", inventories));
        } catch (Exception e) {
            log.error("Error retrieving all inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<List<Inventory>>(false, e.getMessage(), null));
        }
    }
    
    @PutMapping("/{productCode}/update")
    public ResponseEntity<ApiResponse<Inventory>> updateInventory(
            @PathVariable String productCode,
            @RequestBody InventoryUpdateRequest request) {
        try {
            Inventory inventory = inventoryService.updateQuantity(productCode, request.getQuantity(), request.getTransactionType());
            return ResponseEntity.ok(new ApiResponse<Inventory>(true, "Inventory updated successfully", inventory));
        } catch (Exception e) {
            log.error("Error updating inventory", e);
            return ResponseEntity.badRequest().body(new ApiResponse<Inventory>(false, e.getMessage(), null));
        }
    }
    
    @PostMapping("/{productCode}/reserve")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveStock(
            @PathVariable String productCode,
            @RequestBody ReservationRequest request) {
        try {
            ReservationResponse response = inventoryService.reserveStock(productCode, request);
            return ResponseEntity.ok(new ApiResponse<ReservationResponse>(true, "Stock reserved successfully", response));
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
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(new ApiResponse<String>(true, "Inventory service is running", "OK"));
    }
}
