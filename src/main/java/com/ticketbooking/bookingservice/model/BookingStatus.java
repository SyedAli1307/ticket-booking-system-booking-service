package com.ticketbooking.bookingservice.model;

public enum BookingStatus {
    PENDING,       // user clicked "Book" — saga started
    SEAT_LOCKED,   // Redis lock acquired — waiting for payment
    CONFIRMED,     // payment success — ticket issued
    FAILED,        // seat taken OR payment failed
    CANCELLED      // user cancelled after confirmation
}
