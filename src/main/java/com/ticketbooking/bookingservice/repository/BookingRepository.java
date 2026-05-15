package com.ticketbooking.bookingservice.repository;

import com.ticketbooking.bookingservice.model.Booking;
import com.ticketbooking.bookingservice.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    List<Booking> findByEventId(String eventId);

    List<Booking> findByStatus(BookingStatus status);

    Optional<Booking> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndEventIdAndSeatIdAndStatusIn(
            String userId, String eventId, String seatId,
            List<BookingStatus> statuses);
}