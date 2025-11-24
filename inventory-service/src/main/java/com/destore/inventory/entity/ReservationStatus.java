package com.destore.inventory.entity;

public enum ReservationStatus {
    PENDING,     // Reservation created, stock reserved
    CONFIRMED,   // Reservation fulfilled, stock deducted
    CANCELLED,   // Reservation cancelled by user, stock released
    EXPIRED      // Reservation expired, stock released
}
