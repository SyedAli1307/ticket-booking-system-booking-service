package com.ticketbooking.bookingservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLockEvent {
    private String bookingId;
    private String eventId;
    private String seatId;
    private String userId;
    private LocalDateTime timestamp;
}
