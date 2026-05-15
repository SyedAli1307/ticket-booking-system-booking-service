package com.ticketbooking.bookingservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.bookingservice.model.Booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // ── Topic constants ───────────────────────────────────────────────
    private static final String TOPIC_SEAT_LOCK_REQUEST  = "seat.lock.requested";
    private static final String TOPIC_BOOKING_CREATED    = "ticket.booking.created";
    private static final String TOPIC_BOOKING_CONFIRMED  = "ticket.booking.confirmed";
    private static final String TOPIC_BOOKING_FAILED     = "ticket.booking.failed";
    private static final String TOPIC_SEAT_LOCK_RELEASE  = "seat.lock.released";

    // ── Publish: seat lock request ────────────────────────────────────
    public void publishSeatLockRequest(Booking booking) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("eventId",   booking.getEventId());
        payload.put("seatId",    booking.getSeatId());
        payload.put("userId",    booking.getUserId());

        // Key = seatId — Kafka partitions by key, so all events for the
        // same seat go to the same partition → ordered processing
        send(TOPIC_SEAT_LOCK_REQUEST, booking.getSeatId(), payload);
    }

    // ── Publish: booking created (seat confirmed, trigger payment) ────
    public void publishBookingCreated(Booking booking) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId",     booking.getId());
        payload.put("userId",        booking.getUserId());
        payload.put("eventId",       booking.getEventId());
        payload.put("seatId",        booking.getSeatId());
        payload.put("amount",        booking.getAmount());
        payload.put("idempotencyKey", booking.getId()); // payment uses this to prevent double charge

        send(TOPIC_BOOKING_CREATED, booking.getId(), payload);
    }

    // ── Publish: booking confirmed (payment done) ─────────────────────
    public void publishBookingConfirmed(Booking booking) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId",     booking.getId());
        payload.put("userId",        booking.getUserId());
        payload.put("eventId",       booking.getEventId());
        payload.put("seatId",        booking.getSeatId());
        payload.put("transactionId", booking.getTransactionId());

        send(TOPIC_BOOKING_CONFIRMED, booking.getId(), payload);
    }

    // ── Publish: booking failed (seat lock OR payment failed) ─────────
    public void publishBookingFailed(Booking booking, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("userId",    booking.getUserId());
        payload.put("eventId",   booking.getEventId());
        payload.put("seatId",    booking.getSeatId());
        payload.put("reason",    reason);

        send(TOPIC_BOOKING_FAILED, booking.getId(), payload);
    }

    // ── Publish: release seat lock (compensation on payment failure) ──
    public void publishSeatLockRelease(Booking booking) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("eventId",   booking.getEventId());
        payload.put("seatId",    booking.getSeatId());

        // Same key as the lock request — same partition, ordered processing
        send(TOPIC_SEAT_LOCK_RELEASE, booking.getSeatId(), payload);
    }

    // ── Internal send with callback logging ───────────────────────────
    private void send(String topic, String key, Map<String, Object> payload) {
        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for topic {}: {}", topic, e.getMessage());
            throw new RuntimeException("Kafka payload serialization failed", e);
        }

        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, key, message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to topic={} key={} error={}",
                        topic, key, ex.getMessage());
            } else {
                log.info("Published to topic={} key={} partition={} offset={}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
