package com.bunny.ticket_booking.repository;

import com.bunny.ticket_booking.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}