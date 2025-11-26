package com.destore.delivery.repository;

import com.destore.delivery.entity.DeliveryZone;
import com.destore.delivery.entity.DeliveryZoneConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryZoneConfigRepository extends JpaRepository<DeliveryZoneConfig, Long> {
    
    Optional<DeliveryZoneConfig> findByZone(DeliveryZone zone);
    
    List<DeliveryZoneConfig> findByIsActiveTrue();
    
    @Query("SELECT d FROM DeliveryZoneConfig d WHERE d.isActive = true " +
           "AND :distance >= d.minDistance AND :distance <= d.maxDistance")
    Optional<DeliveryZoneConfig> findZoneByDistance(@Param("distance") BigDecimal distance);
}
