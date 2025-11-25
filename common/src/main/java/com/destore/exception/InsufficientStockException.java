package com.destore.exception;

public class InsufficientStockException extends RuntimeException {
    private final int available;
    private final int requested;

    public InsufficientStockException(int available, int requested) {
        super(String.format("Insufficient stock available. Requested: %d, Available: %d", requested, available));
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
