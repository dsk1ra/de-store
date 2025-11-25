package com.destore.inventory.service;

import com.destore.dto.LowStockEvent;
import com.destore.inventory.dto.InventoryCreateRequest;
import com.destore.inventory.dto.ReservationRequest;
import com.destore.inventory.dto.ReservationResponse;
import com.destore.inventory.dto.ReservationDetailsResponse;
import com.destore.inventory.dto.ConfirmReservationRequest;
import com.destore.inventory.dto.CancelReservationRequest;
import com.destore.inventory.entity.Inventory;
import com.destore.inventory.entity.InventoryTransaction;
import com.destore.inventory.entity.TransactionType;
import com.destore.inventory.entity.Reservation;
import com.destore.inventory.entity.ReservationStatus;
import com.destore.inventory.repository.InventoryRepository;
import com.destore.inventory.repository.InventoryTransactionRepository;
import com.destore.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.low-stock}")
    private String lowStockExchange;

    @Value("${rabbitmq.routing-key.low-stock}")
    private String lowStockRoutingKey;

    @Transactional
    public Inventory createInventory(InventoryCreateRequest request) {
        if (inventoryRepository.existsByProductCode(request.getProductCode())) {
            throw new com.destore.exception.DuplicateResourceException("Inventory", request.getProductCode());
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
        logTransaction(saved.getProductCode(), TransactionType.RESTOCK, saved.getQuantity(), 0, saved.getQuantity(),
                null);

        return saved;
    }

    public Inventory getInventory(String productCode) {
        return inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new com.destore.exception.ResourceNotFoundException("Inventory", productCode));
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

        logTransaction(productCode, transactionType, Math.abs(quantity - previousQuantity), previousQuantity, quantity,
                null);

        // Check if low stock threshold crossed
        if (updated.getAvailableQuantity() <= updated.getLowStockThreshold()) {
            publishLowStockEvent(updated);
        }

        return updated;
    }

    @Transactional
    public Inventory updateQuantity(String productCode, Integer quantity, TransactionType transactionType,
            Integer lowStockThreshold) {
        Inventory inventory = getInventory(productCode);
        Integer previousQuantity = inventory.getQuantity();

        // Update quantity if provided
        if (quantity != null) {
            inventory.setQuantity(quantity);
        }

        // Update low stock threshold if provided (including 0)
        if (lowStockThreshold != null) {
            inventory.setLowStockThreshold(
                    lowStockThreshold >= 0 ? lowStockThreshold : inventory.getLowStockThreshold());
        }

        Inventory updated = inventoryRepository.save(inventory);

        if (quantity != null) {
            logTransaction(productCode, transactionType, Math.abs(quantity - previousQuantity), previousQuantity,
                    quantity, null);
        }

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
            throw new com.destore.exception.InsufficientStockException(inventory.getAvailableQuantity(),
                    request.getQuantity());
        }

        Integer previousReserved = inventory.getReservedQuantity();
        inventory.setReservedQuantity(previousReserved + request.getQuantity());
        inventoryRepository.save(inventory);

        String reservationId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // Create reservation record
        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .productCode(productCode)
                .quantity(request.getQuantity())
                .initialNotes(request.getNotes())
                .status(ReservationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        reservationRepository.save(reservation);

        logTransaction(productCode, TransactionType.RESERVATION, request.getQuantity(),
                previousReserved, inventory.getReservedQuantity(), reservationId);

        // Check if low stock threshold crossed after reservation
        if (inventory.getAvailableQuantity() <= inventory.getLowStockThreshold()) {
            publishLowStockEvent(inventory);
        }

        log.info("Created reservation {} for product {} with quantity {}", reservationId, productCode,
                request.getQuantity());

        return ReservationResponse.builder()
                .reservationId(reservationId)
                .productCode(productCode)
                .reservedQuantity(request.getQuantity())
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public ReservationDetailsResponse confirmReservation(ConfirmReservationRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Reservation not found: " + request.getReservationId()));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Reservation is not in PENDING status. Current status: " + reservation.getStatus());
        }

        if (LocalDateTime.now().isAfter(reservation.getExpiresAt())) {
            throw new IllegalArgumentException("Reservation has expired");
        }

        Inventory inventory = getInventory(reservation.getProductCode());

        Integer previousQuantity = inventory.getQuantity();
        Integer previousReserved = inventory.getReservedQuantity();

        // Deduct from both total and reserved
        inventory.setQuantity(previousQuantity - reservation.getQuantity());
        inventory.setReservedQuantity(previousReserved - reservation.getQuantity());
        inventoryRepository.save(inventory);

        // Update reservation status
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setProcessedAt(LocalDateTime.now());
        if (request.getNotes() != null) {
            reservation.setNotes(request.getNotes());
        }
        reservationRepository.save(reservation);

        logTransaction(reservation.getProductCode(), TransactionType.SALE, reservation.getQuantity(),
                previousQuantity, inventory.getQuantity(), reservation.getReservationId());

        log.info("Confirmed reservation {} for product {}", reservation.getReservationId(),
                reservation.getProductCode());

        return mapToDetailsResponse(reservation);
    }

    @Transactional
    public ReservationDetailsResponse cancelReservation(CancelReservationRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Reservation not found: " + request.getReservationId()));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING reservations can be cancelled. Current status: " + reservation.getStatus());
        }

        Inventory inventory = getInventory(reservation.getProductCode());

        // Release reserved stock
        Integer previousReserved = inventory.getReservedQuantity();
        inventory.setReservedQuantity(previousReserved - reservation.getQuantity());
        inventoryRepository.save(inventory);

        // Update reservation status
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setProcessedAt(LocalDateTime.now());
        reservation
                .setNotes("Cancelled: " + (request.getReason() != null ? request.getReason() : "No reason provided"));
        reservationRepository.save(reservation);

        logTransaction(reservation.getProductCode(), TransactionType.ADJUSTMENT, reservation.getQuantity(),
                previousReserved, inventory.getReservedQuantity(), reservation.getReservationId());

        log.info("Cancelled reservation {} for product {}", reservation.getReservationId(),
                reservation.getProductCode());

        return mapToDetailsResponse(reservation);
    }

    @Transactional
    public int releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(ReservationStatus.PENDING,
                now);

        int count = 0;
        for (Reservation reservation : expiredReservations) {
            try {
                Inventory inventory = getInventory(reservation.getProductCode());

                // Release reserved stock
                Integer previousReserved = inventory.getReservedQuantity();
                inventory.setReservedQuantity(Math.max(0, previousReserved - reservation.getQuantity()));
                inventoryRepository.save(inventory);

                // Update reservation status
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservation.setProcessedAt(now);
                reservation.setNotes("Auto-expired by system");
                reservationRepository.save(reservation);

                logTransaction(reservation.getProductCode(), TransactionType.ADJUSTMENT, reservation.getQuantity(),
                        previousReserved, inventory.getReservedQuantity(), reservation.getReservationId());

                count++;
            } catch (Exception e) {
                log.error("Error releasing expired reservation {}: {}", reservation.getReservationId(), e.getMessage());
            }
        }

        if (count > 0) {
            log.info("Released {} expired reservations", count);
        }

        return count;
    }

    public ReservationDetailsResponse getReservation(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        return mapToDetailsResponse(reservation);
    }

    public List<ReservationDetailsResponse> getReservationsByProduct(String productCode) {
        List<Reservation> reservations = reservationRepository.findByProductCode(productCode);
        return reservations.stream()
                .map(this::mapToDetailsResponse)
                .collect(Collectors.toList());
    }

    public List<ReservationDetailsResponse> getReservationsByStatus(ReservationStatus status) {
        List<Reservation> reservations = reservationRepository.findByStatus(status);
        return reservations.stream()
                .map(this::mapToDetailsResponse)
                .collect(Collectors.toList());
    }

    public List<ReservationDetailsResponse> getAllReservations() {
        List<Reservation> reservations = reservationRepository.findAll();
        return reservations.stream()
                .map(this::mapToDetailsResponse)
                .collect(Collectors.toList());
    }

    private ReservationDetailsResponse mapToDetailsResponse(Reservation reservation) {
        Long minutesToExpiry = null;
        if (reservation.getStatus() == ReservationStatus.PENDING && reservation.getExpiresAt() != null) {
            Duration duration = Duration.between(LocalDateTime.now(), reservation.getExpiresAt());
            minutesToExpiry = duration.toMinutes();
        }

        return ReservationDetailsResponse.builder()
                .reservationId(reservation.getReservationId())
                .productCode(reservation.getProductCode())
                .quantity(reservation.getQuantity())
                .initialNotes(reservation.getInitialNotes())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .expiresAt(reservation.getExpiresAt())
                .processedAt(reservation.getProcessedAt())
                .notes(reservation.getNotes())
                .timeToExpiryMinutes(minutesToExpiry)
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
