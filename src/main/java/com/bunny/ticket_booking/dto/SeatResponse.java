package com.bunny.ticket_booking.dto;

import com.bunny.ticket_booking.entity.SeatStatus;

public record SeatResponse(Long id, String seatNumber, SeatStatus status) {
}