package com.ticketbooking.bookingservice.dto;
import com.ticketbooking.bookingservice.model.BookingStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private String bookingId;
    private String userId;
    private String eventId;
    private String seatId;
    private BookingStatus status;
    private BigDecimal amount;
    private String transactionId;
    private String message;
    private LocalDateTime createdAt;
}