package com.destore.delivery.repository;

import com.destore.delivery.entity.DeliveryOrder;
import com.destore.delivery.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {
    
    Optional<DeliveryOrder> findByOrderId(String orderId);
    
    List<DeliveryOrder> findByCustomerId(String customerId);
    
    List<DeliveryOrder> findByStatus(DeliveryStatus status);
    
    List<DeliveryOrder> findByStoreId(String storeId);
    
    @Query("SELECT d FROM DeliveryOrder d WHERE d.orderDateTime BETWEEN :start AND :end")
    List<DeliveryOrder> findByOrderDateTimeBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT d FROM DeliveryOrder d WHERE d.status = :status AND d.orderDateTime >= :since")
    List<DeliveryOrder> findByStatusAndOrderDateTimeAfter(
            @Param("status") DeliveryStatus status,
            @Param("since") LocalDateTime since
    );
    
    @Query("SELECT COUNT(d) FROM DeliveryOrder d WHERE d.status = :status")
    Long countByStatus(@Param("status") DeliveryStatus status);
    
    @Query("SELECT d FROM DeliveryOrder d WHERE d.driverName = :driverName AND d.status IN :statuses")
    List<DeliveryOrder> findActiveDeliveriesByDriver(
            @Param("driverName") String driverName,
            @Param("statuses") List<DeliveryStatus> statuses
    );
}
