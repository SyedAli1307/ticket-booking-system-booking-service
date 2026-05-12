package com.ticketbooking.bookingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ticketbooking.bookingservice.model.BookingStatus;
import lombok.*;

@Data
@Builder
public class BookingSummaryResponse {
    private String bookingId;
    private String eventName;   // joined from Event table
    private String seatId;
    private BookingStatus status;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
