package com.ticketbooking.bookingservice.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*; 
import lombok.Data;

@Data
public class BookingRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "seatId is required")
    private String seatId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;
}