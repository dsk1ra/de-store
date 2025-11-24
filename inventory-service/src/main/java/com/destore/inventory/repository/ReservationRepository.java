package com.destore.inventory.repository;

import com.destore.inventory.entity.Reservation;
import com.destore.inventory.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {
    
    List<Reservation> findByProductCode(String productCode);
    
    List<Reservation> findByStatus(ReservationStatus status);
    
    List<Reservation> findByProductCodeAndStatus(String productCode, ReservationStatus status);
    
    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.expiresAt < :currentTime")
    List<Reservation> findExpiredReservations(@Param("status") ReservationStatus status, 
                                               @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT r FROM Reservation r WHERE r.expiresAt < :expiryTime AND r.status = 'PENDING'")
    List<Reservation> findPendingReservationsExpiringSoon(@Param("expiryTime") LocalDateTime expiryTime);
}
