package com.bunny.ticket_booking.dto;

import com.bunny.ticket_booking.entity.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(Long id, Long seatId, String seatNumber, Long userId,
                              BookingStatus status, LocalDateTime createdAt) {
}