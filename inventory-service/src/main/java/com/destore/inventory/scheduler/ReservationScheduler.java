package com.destore.inventory.scheduler;

import com.destore.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {
    
    private final InventoryService inventoryService;
    
    /**
     * Automatically release expired reservations every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void releaseExpiredReservations() {
        log.info("Running scheduled task: Releasing expired reservations");
        try {
            int count = inventoryService.releaseExpiredReservations();
            if (count > 0) {
                log.info("Scheduled task completed: Released {} expired reservation(s)", count);
            }
        } catch (Exception e) {
            log.error("Error in scheduled task for releasing expired reservations", e);
        }
    }
}
