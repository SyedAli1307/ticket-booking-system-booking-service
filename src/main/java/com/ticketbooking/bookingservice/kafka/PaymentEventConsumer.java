package com.ticketbooking.bookingservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.bookingservice.service.BookingSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final BookingSagaService bookingSagaService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.processed",
                   groupId = "booking-service-group")
    public void onPaymentProcessed(String message) {
        try {
            JsonNode node       = objectMapper.readTree(message);
            String bookingId    = node.get("bookingId").asText();
            String transactionId = node.get("transactionId").asText();
            bookingSagaService.onPaymentProcessed(bookingId, transactionId);
        } catch (Exception e) {
            log.error("Error processing payment.processed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment.failed",
                   groupId = "booking-service-group")
    public void onPaymentFailed(String message) {
        try {
            JsonNode node    = objectMapper.readTree(message);
            String bookingId = node.get("bookingId").asText();
            String reason    = node.get("reason").asText();
            bookingSagaService.onPaymentFailed(bookingId, reason);
        } catch (Exception e) {
            log.error("Error processing payment.failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}