package com.ticketbooking.bookingservice.service;

import com.ticketbooking.bookingservice.dto.BookingRequest;
import com.ticketbooking.bookingservice.dto.BookingResponse;
import com.ticketbooking.bookingservice.model.Booking;
import com.ticketbooking.bookingservice.model.BookingStatus;
import com.ticketbooking.bookingservice.kafka.BookingEventProducer;
import com.ticketbooking.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingSagaService {

    private final BookingRepository bookingRepository;
    private final BookingEventProducer bookingEventProducer;

    // ── Step 1: User initiates booking ───────────────────────────────
    @Transactional
    public BookingResponse initiateBooking(BookingRequest request) {
        // Save booking in PENDING state first — DB is source of truth
        Booking booking = Booking.builder()
                .id(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .eventId(request.getEventId())
                .seatId(request.getSeatId())
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        bookingRepository.save(booking);
        log.info("Booking {} created in PENDING state", booking.getId());

        // Publish event → Seat-Lock service will consume this
        bookingEventProducer.publishSeatLockRequest(booking);

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .status(BookingStatus.PENDING)
                .message("Booking initiated. Seat is being reserved.")
                .build();
    }

    // ── Step 2a: Seat locked successfully → proceed to payment ───────
    @Transactional
    public void onSeatLockConfirmed(String bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        booking.setStatus(BookingStatus.SEAT_LOCKED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("Booking {} seat locked. Publishing to payment.", bookingId);

        // Publish event → Payment service will consume this
        bookingEventProducer.publishBookingCreated(booking);
    }

    // ── Step 2b: Seat lock failed → end saga, notify user ────────────
    @Transactional
    public void onSeatLockFailed(String bookingId, String reason) {
        Booking booking = findBookingOrThrow(bookingId);

        booking.setStatus(BookingStatus.FAILED);
        booking.setFailureReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.warn("Booking {} seat lock failed: {}", bookingId, reason);

        // Notify user — seat is unavailable, no payment taken
        bookingEventProducer.publishBookingFailed(booking, reason);
    }

    // ── Step 3a: Payment success → booking confirmed ──────────────────
    @Transactional
    public void onPaymentProcessed(String bookingId, String transactionId) {
        Booking booking = findBookingOrThrow(bookingId);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTransactionId(transactionId);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.info("Booking {} confirmed. Transaction: {}", bookingId, transactionId);

        // Notify user — booking confirmed
        bookingEventProducer.publishBookingConfirmed(booking);
    }

    // ── Step 3b: Payment failed → compensate, release seat ───────────
    @Transactional
    public void onPaymentFailed(String bookingId, String reason) {
        Booking booking = findBookingOrThrow(bookingId);

        booking.setStatus(BookingStatus.FAILED);
        booking.setFailureReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        log.warn("Booking {} payment failed: {}. Releasing seat.", bookingId, reason);

        // Compensation: release the seat so others can book it
        bookingEventProducer.publishSeatLockRelease(booking);

        // Notify user — payment failed
        bookingEventProducer.publishBookingFailed(booking, reason);
    }

    // ── Helper ────────────────────────────────────────────────────────
    private Booking findBookingOrThrow(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("Booking {} not found", bookingId);
                    return new RuntimeException("Booking not found: " + bookingId);
                });
    }
}