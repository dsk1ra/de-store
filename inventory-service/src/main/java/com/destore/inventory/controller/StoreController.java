package com.destore.inventory.controller;

import com.destore.dto.ApiResponse;
import com.destore.inventory.dto.StoreCreateRequest;
import com.destore.inventory.dto.StoreUpdateRequest;
import com.destore.inventory.entity.Store;
import com.destore.inventory.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing stores.
 * Stores must exist before inventory can be assigned to them.
 */
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@Slf4j
public class StoreController {
    
    private final StoreService storeService;
    
    /**
     * Creates a new store.
     * 
     * @param request the store creation request
     * @return the created store
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Store>> createStore(@Valid @RequestBody StoreCreateRequest request) {
        try {
            Store store = storeService.createStore(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Store created successfully", store));
        } catch (Exception e) {
            log.error("Error creating store", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Gets a store by its store ID.
     * 
     * @param storeId the store ID
     * @return the store
     */
    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<Store>> getStore(@PathVariable String storeId) {
        try {
            Store store = storeService.getStore(storeId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Store retrieved successfully", store));
        } catch (Exception e) {
            log.error("Error retrieving store", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Checks if a store exists.
     * 
     * @param storeId the store ID
     * @return true if the store exists
     */
    @GetMapping("/{storeId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> storeExists(@PathVariable String storeId) {
        try {
            boolean exists = storeService.storeExists(storeId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Store existence checked", exists));
        } catch (Exception e) {
            log.error("Error checking store existence", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Gets all stores.
     * 
     * @return list of all stores
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Store>>> getAllStores() {
        try {
            List<Store> stores = storeService.getAllStores();
            return ResponseEntity.ok(new ApiResponse<>(true, "Stores retrieved successfully", stores));
        } catch (Exception e) {
            log.error("Error retrieving stores", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Gets all active stores.
     * 
     * @return list of active stores
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Store>>> getActiveStores() {
        try {
            List<Store> stores = storeService.getActiveStores();
            return ResponseEntity.ok(new ApiResponse<>(true, "Active stores retrieved successfully", stores));
        } catch (Exception e) {
            log.error("Error retrieving active stores", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Gets stores by city.
     * 
     * @param city the city name
     * @return list of stores in the city
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<List<Store>>> getStoresByCity(@PathVariable String city) {
        try {
            List<Store> stores = storeService.getStoresByCity(city);
            return ResponseEntity.ok(new ApiResponse<>(true, "Stores retrieved successfully", stores));
        } catch (Exception e) {
            log.error("Error retrieving stores by city", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Gets stores by region.
     * 
     * @param region the region name
     * @return list of stores in the region
     */
    @GetMapping("/region/{region}")
    public ResponseEntity<ApiResponse<List<Store>>> getStoresByRegion(@PathVariable String region) {
        try {
            List<Store> stores = storeService.getStoresByRegion(region);
            return ResponseEntity.ok(new ApiResponse<>(true, "Stores retrieved successfully", stores));
        } catch (Exception e) {
            log.error("Error retrieving stores by region", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Updates a store.
     * 
     * @param storeId the store ID
     * @param request the update request
     * @return the updated store
     */
    @PutMapping("/{storeId}")
    public ResponseEntity<ApiResponse<Store>> updateStore(
            @PathVariable String storeId,
            @Valid @RequestBody StoreUpdateRequest request) {
        try {
            Store store = storeService.updateStore(storeId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Store updated successfully", store));
        } catch (Exception e) {
            log.error("Error updating store", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Deactivates a store.
     * 
     * @param storeId the store ID
     * @return the deactivated store
     */
    @PostMapping("/{storeId}/deactivate")
    public ResponseEntity<ApiResponse<Store>> deactivateStore(@PathVariable String storeId) {
        try {
            Store store = storeService.deactivateStore(storeId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Store deactivated successfully", store));
        } catch (Exception e) {
            log.error("Error deactivating store", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Activates a store.
     * 
     * @param storeId the store ID
     * @return the activated store
     */
    @PostMapping("/{storeId}/activate")
    public ResponseEntity<ApiResponse<Store>> activateStore(@PathVariable String storeId) {
        try {
            Store store = storeService.activateStore(storeId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Store activated successfully", store));
        } catch (Exception e) {
            log.error("Error activating store", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
