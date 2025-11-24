package com.destore.inventory.scheduler;

import com.destore.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockScheduler {
    
    private final InventoryService inventoryService;
    
    // Run every hour
    @Scheduled(cron = "0 0 * * * *")
    public void checkLowStock() {
        log.info("Starting scheduled low stock check");
        inventoryService.checkLowStockProducts();
    }
}
