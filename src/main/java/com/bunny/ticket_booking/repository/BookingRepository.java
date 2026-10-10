package com.bunny.ticket_booking.repository;

import com.bunny.ticket_booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}