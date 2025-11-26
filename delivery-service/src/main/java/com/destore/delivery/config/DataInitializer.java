package com.destore.delivery.config;

import com.destore.delivery.entity.DeliveryPricing;
import com.destore.delivery.entity.DeliveryZone;
import com.destore.delivery.entity.DeliveryZoneConfig;
import com.destore.delivery.repository.DeliveryPricingRepository;
import com.destore.delivery.repository.DeliveryZoneConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DeliveryPricingRepository deliveryPricingRepository;
    private final DeliveryZoneConfigRepository deliveryZoneConfigRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing delivery service data...");
        
        // Initialize pricing if not exists
        if (deliveryPricingRepository.count() == 0) {
            DeliveryPricing pricing = DeliveryPricing.builder()
                    .baseCharge(BigDecimal.valueOf(5.00))
                    .ratePerKm(BigDecimal.valueOf(1.50))
                    .freeDistanceThreshold(BigDecimal.valueOf(5.0))
                    .freeDeliveryThreshold(BigDecimal.valueOf(100.00))
                    .reducedRateThreshold(BigDecimal.valueOf(50.00))
                    .reducedRatePercentage(BigDecimal.valueOf(50))
                    .expressSurcharge(BigDecimal.valueOf(10.00))
                    .peakHourSurchargePercentage(BigDecimal.valueOf(20))
                    .isActive(true)
                    .build();
            deliveryPricingRepository.save(pricing);
            log.info("Default pricing configuration created");
        }
        
        // Initialize zones if not exists
        if (deliveryZoneConfigRepository.count() == 0) {
            DeliveryZoneConfig cityCenter = DeliveryZoneConfig.builder()
                    .zone(DeliveryZone.CITY_CENTER)
                    .multiplier(BigDecimal.valueOf(1.0))
                    .minDistance(BigDecimal.ZERO)
                    .maxDistance(BigDecimal.valueOf(10.0))
                    .description("City center - within 10km")
                    .isActive(true)
                    .build();
            
            DeliveryZoneConfig suburban = DeliveryZoneConfig.builder()
                    .zone(DeliveryZone.SUBURBAN)
                    .multiplier(BigDecimal.valueOf(1.2))
                    .minDistance(BigDecimal.valueOf(10.0))
                    .maxDistance(BigDecimal.valueOf(25.0))
                    .description("Suburban area - 10-25km")
                    .isActive(true)
                    .build();
            
            DeliveryZoneConfig rural = DeliveryZoneConfig.builder()
                    .zone(DeliveryZone.RURAL)
                    .multiplier(BigDecimal.valueOf(1.5))
                    .minDistance(BigDecimal.valueOf(25.0))
                    .maxDistance(BigDecimal.valueOf(50.0))
                    .description("Rural area - 25-50km")
                    .isActive(true)
                    .build();
            
            DeliveryZoneConfig remote = DeliveryZoneConfig.builder()
                    .zone(DeliveryZone.REMOTE)
                    .multiplier(BigDecimal.valueOf(2.0))
                    .minDistance(BigDecimal.valueOf(50.0))
                    .maxDistance(BigDecimal.valueOf(100.0))
                    .description("Remote area - 50-100km")
                    .isActive(true)
                    .build();
            
            deliveryZoneConfigRepository.save(cityCenter);
            deliveryZoneConfigRepository.save(suburban);
            deliveryZoneConfigRepository.save(rural);
            deliveryZoneConfigRepository.save(remote);
            
            log.info("Delivery zones configured");
        }
        
        log.info("Delivery service data initialization completed");
    }
}
