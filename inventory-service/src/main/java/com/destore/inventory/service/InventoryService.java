package com.destore.inventory.service;

import com.destore.dto.LowStockEvent;
import com.destore.inventory.dto.InventoryCreateRequest;
import com.destore.inventory.dto.ReservationRequest;
import com.destore.inventory.dto.ReservationResponse;
import com.destore.inventory.entity.Inventory;
import com.destore.inventory.entity.InventoryTransaction;
import com.destore.inventory.entity.TransactionType;
import com.destore.inventory.repository.InventoryRepository;
import com.destore.inventory.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.low-stock}")
    private String lowStockExchange;
    
    @Value("${rabbitmq.routing-key.low-stock}")
    private String lowStockRoutingKey;
    
    @Transactional
    public Inventory createInventory(InventoryCreateRequest request) {
        if (inventoryRepository.existsByProductCode(request.getProductCode())) {
            throw new IllegalArgumentException("Inventory already exists for product: " + request.getProductCode());
        }
        
        Inventory inventory = Inventory.builder()
                .productCode(request.getProductCode())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 0)
                .reservedQuantity(0)
                .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10)
                .storeId(request.getStoreId())
                .build();
        
        @SuppressWarnings("null")
        Inventory saved = inventoryRepository.save(inventory);
        
        // Log transaction
        logTransaction(saved.getProductCode(), TransactionType.RESTOCK, saved.getQuantity(), 0, saved.getQuantity(), null);
        
        return saved;
    }
    
    public Inventory getInventory(String productCode) {
        return inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product: " + productCode));
    }
    
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }
    
    @Transactional
    public Inventory updateQuantity(String productCode, Integer quantity, TransactionType transactionType) {
        Inventory inventory = getInventory(productCode);
        Integer previousQuantity = inventory.getQuantity();
        
        inventory.setQuantity(quantity);
        Inventory updated = inventoryRepository.save(inventory);
        
        logTransaction(productCode, transactionType, Math.abs(quantity - previousQuantity), previousQuantity, quantity, null);
        
        // Check if low stock threshold crossed
        if (updated.getAvailableQuantity() <= updated.getLowStockThreshold()) {
            publishLowStockEvent(updated);
        }
        
        return updated;
    }
    
    @Transactional
    public ReservationResponse reserveStock(String productCode, ReservationRequest request) {
        Inventory inventory = getInventory(productCode);
        
        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock available. Available: " + inventory.getAvailableQuantity());
        }
        
        Integer previousReserved = inventory.getReservedQuantity();
        inventory.setReservedQuantity(previousReserved + request.getQuantity());
        inventoryRepository.save(inventory);
        
        String reservationId = UUID.randomUUID().toString();
        logTransaction(productCode, TransactionType.RESERVATION, request.getQuantity(), 
                previousReserved, inventory.getReservedQuantity(), reservationId);
        
        // Check if low stock threshold crossed after reservation
        if (inventory.getAvailableQuantity() <= inventory.getLowStockThreshold()) {
            publishLowStockEvent(inventory);
        }
        
        return ReservationResponse.builder()
                .reservationId(reservationId)
                .productCode(productCode)
                .reservedQuantity(request.getQuantity())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }
    
    @Transactional
    public Inventory deductStock(String productCode, Integer quantity) {
        Inventory inventory = getInventory(productCode);
        
        Integer previousQuantity = inventory.getQuantity();
        Integer previousReserved = inventory.getReservedQuantity();
        
        if (inventory.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + inventory.getQuantity());
        }
        
        inventory.setQuantity(previousQuantity - quantity);
        
        // Deduct from reserved if applicable
        if (previousReserved >= quantity) {
            inventory.setReservedQuantity(previousReserved - quantity);
        } else if (previousReserved > 0) {
            inventory.setReservedQuantity(0);
        }
        
        Inventory updated = inventoryRepository.save(inventory);
        
        logTransaction(productCode, TransactionType.SALE, quantity, previousQuantity, updated.getQuantity(), null);
        
        // Check if low stock threshold crossed
        if (updated.getAvailableQuantity() <= updated.getLowStockThreshold()) {
            publishLowStockEvent(updated);
        }
        
        return updated;
    }
    
    @Transactional
    public Inventory restockInventory(String productCode, Integer quantity) {
        Inventory inventory = getInventory(productCode);
        
        Integer previousQuantity = inventory.getQuantity();
        inventory.setQuantity(previousQuantity + quantity);
        
        Inventory updated = inventoryRepository.save(inventory);
        
        logTransaction(productCode, TransactionType.RESTOCK, quantity, previousQuantity, updated.getQuantity(), null);
        
        return updated;
    }
    
    private void logTransaction(String productCode, TransactionType type, Integer quantity, 
                                Integer previousQuantity, Integer newQuantity, String referenceId) {
        InventoryTransaction transaction = InventoryTransaction.builder()
                .productCode(productCode)
                .transactionType(type)
                .quantity(quantity)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .referenceId(referenceId)
                .build();
        
        @SuppressWarnings("null")
        InventoryTransaction savedTransaction = transactionRepository.save(transaction);
        log.debug("Logged transaction: {}", savedTransaction.getId());
    }
    
    private void publishLowStockEvent(Inventory inventory) {
        LowStockEvent event = LowStockEvent.builder()
                .productCode(inventory.getProductCode())
                .currentQuantity(inventory.getAvailableQuantity())
                .threshold(inventory.getLowStockThreshold())
                .storeId(inventory.getStoreId())
                .timestamp(LocalDateTime.now())
                .build();
        
        rabbitTemplate.convertAndSend(lowStockExchange, lowStockRoutingKey, event);
        log.info("Published low stock event for product: {}", inventory.getProductCode());
    }
    
    public void checkLowStockProducts() {
        List<Inventory> allInventory = inventoryRepository.findAll();
        
        for (Inventory inventory : allInventory) {
            if (inventory.getAvailableQuantity() <= inventory.getLowStockThreshold()) {
                publishLowStockEvent(inventory);
            }
        }
        
        log.info("Low stock check completed. Checked {} products", allInventory.size());
    }
    
    public List<Inventory> getLowStockItems(String storeId) {
        List<Inventory> allInventory = inventoryRepository.findAll();
        
        return allInventory.stream()
                .filter(inventory -> {
                    // Filter by storeId if provided
                    boolean storeMatch = (storeId == null || storeId.isEmpty() || 
                                        storeId.equals(inventory.getStoreId()));
                    // Check if available quantity is at or below threshold
                    boolean lowStock = inventory.getAvailableQuantity() <= inventory.getLowStockThreshold();
                    return storeMatch && lowStock;
                })
                .toList();
    }
}
