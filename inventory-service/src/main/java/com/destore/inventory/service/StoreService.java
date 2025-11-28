package com.destore.inventory.service;

import com.destore.inventory.dto.StoreCreateRequest;
import com.destore.inventory.dto.StoreUpdateRequest;
import com.destore.inventory.entity.Store;
import com.destore.inventory.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing stores.
 * Stores must exist before inventory can be assigned to them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {
    
    private final StoreRepository storeRepository;
    
    /**
     * Creates a new store.
     * 
     * @param request the store creation request
     * @return the created store
     * @throws com.destore.exception.DuplicateResourceException if store ID already exists
     */
    @Transactional
    public Store createStore(StoreCreateRequest request) {
        if (storeRepository.existsByStoreId(request.getStoreId())) {
            throw new com.destore.exception.DuplicateResourceException("Store", request.getStoreId());
        }
        
        Store store = Store.builder()
                .storeId(request.getStoreId())
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .region(request.getRegion())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .managerName(request.getManagerName())
                .active(true)
                .build();
        
        Store saved = storeRepository.save(store);
        log.info("Created new store: {} - {}", saved.getStoreId(), saved.getName());
        return saved;
    }
    
    /**
     * Gets a store by its store ID.
     * 
     * @param storeId the store ID
     * @return the store
     * @throws com.destore.exception.ResourceNotFoundException if store not found
     */
    public Store getStore(String storeId) {
        return storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new com.destore.exception.ResourceNotFoundException("Store", storeId));
    }
    
    /**
     * Checks if a store exists.
     * 
     * @param storeId the store ID
     * @return true if the store exists
     */
    public boolean storeExists(String storeId) {
        return storeRepository.existsByStoreId(storeId);
    }
    
    /**
     * Validates that a store exists.
     * 
     * @param storeId the store ID
     * @throws com.destore.exception.ResourceNotFoundException if store not found
     */
    public void validateStoreExists(String storeId) {
        if (!storeRepository.existsByStoreId(storeId)) {
            throw new com.destore.exception.ResourceNotFoundException("Store", storeId);
        }
    }
    
    /**
     * Gets all stores.
     * 
     * @return list of all stores
     */
    public List<Store> getAllStores() {
        return storeRepository.findAll();
    }
    
    /**
     * Gets all active stores.
     * 
     * @return list of active stores
     */
    public List<Store> getActiveStores() {
        return storeRepository.findByActiveTrue();
    }
    
    /**
     * Gets stores by city.
     * 
     * @param city the city name
     * @return list of stores in the city
     */
    public List<Store> getStoresByCity(String city) {
        return storeRepository.findByCity(city);
    }
    
    /**
     * Gets stores by region.
     * 
     * @param region the region name
     * @return list of stores in the region
     */
    public List<Store> getStoresByRegion(String region) {
        return storeRepository.findByRegion(region);
    }
    
    /**
     * Updates a store.
     * 
     * @param storeId the store ID
     * @param request the update request
     * @return the updated store
     * @throws com.destore.exception.ResourceNotFoundException if store not found
     */
    @Transactional
    public Store updateStore(String storeId, StoreUpdateRequest request) {
        Store store = getStore(storeId);
        
        if (request.getName() != null) {
            store.setName(request.getName());
        }
        if (request.getAddress() != null) {
            store.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            store.setCity(request.getCity());
        }
        if (request.getRegion() != null) {
            store.setRegion(request.getRegion());
        }
        if (request.getPostalCode() != null) {
            store.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            store.setCountry(request.getCountry());
        }
        if (request.getPhone() != null) {
            store.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            store.setEmail(request.getEmail());
        }
        if (request.getManagerName() != null) {
            store.setManagerName(request.getManagerName());
        }
        if (request.getActive() != null) {
            store.setActive(request.getActive());
        }
        
        Store updated = storeRepository.save(store);
        log.info("Updated store: {}", storeId);
        return updated;
    }
    
    /**
     * Deactivates a store.
     * 
     * @param storeId the store ID
     * @return the deactivated store
     * @throws com.destore.exception.ResourceNotFoundException if store not found
     */
    @Transactional
    public Store deactivateStore(String storeId) {
        Store store = getStore(storeId);
        store.setActive(false);
        Store updated = storeRepository.save(store);
        log.info("Deactivated store: {}", storeId);
        return updated;
    }
    
    /**
     * Activates a store.
     * 
     * @param storeId the store ID
     * @return the activated store
     * @throws com.destore.exception.ResourceNotFoundException if store not found
     */
    @Transactional
    public Store activateStore(String storeId) {
        Store store = getStore(storeId);
        store.setActive(true);
        Store updated = storeRepository.save(store);
        log.info("Activated store: {}", storeId);
        return updated;
    }
}
