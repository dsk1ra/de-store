package com.destore.delivery.entity;

public enum DeliveryStatus {
    PENDING,           // Order received, not yet assigned
    CONFIRMED,         // Delivery confirmed, calculating route
    ASSIGNED,          // Driver assigned
    PICKED_UP,         // Order picked up from store
    IN_TRANSIT,        // On the way to customer
    DELIVERED,         // Successfully delivered
    CANCELLED,         // Order cancelled
    FAILED,            // Delivery failed
    RETURNED           // Returned to store
}
