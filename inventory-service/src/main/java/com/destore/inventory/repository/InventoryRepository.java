package com.destore.inventory.repository;

import com.destore.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    /**
     * Find inventory by product code and store ID - preferred method for specific inventory lookup
     */
    Optional<Inventory> findByProductCodeAndStoreId(String productCode, String storeId);
    
    /**
     * Check if inventory exists for a product (any store)
     */
    boolean existsByProductCode(String productCode);
    
    /**
     * Check if inventory exists for a product in a specific store
     */
    boolean existsByProductCodeAndStoreId(String productCode, String storeId);
    
    /**
     * Find all inventory items for a specific store
     */
    List<Inventory> findByStoreId(String storeId);
    
    /**
     * Find all inventory items for a specific product across all stores
     */
    List<Inventory> findAllByProductCode(String productCode);
    
    /**
     * Find low stock items using database query instead of in-memory filtering.
     * Low stock means available quantity (quantity - reservedQuantity) <= threshold.
     */
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= i.lowStockThreshold " +
           "AND (:storeId IS NULL OR :storeId = '' OR i.storeId = :storeId)")
    List<Inventory> findLowStockItems(@Param("storeId") String storeId);
    
    /**
     * Find all low stock items across all stores.
     */
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= i.lowStockThreshold")
    List<Inventory> findAllLowStockItems();
}
