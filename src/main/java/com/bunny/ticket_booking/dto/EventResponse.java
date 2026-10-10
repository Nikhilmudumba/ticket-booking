package com.bunny.ticket_booking.dto;

import java.time.LocalDateTime;

public record EventResponse(Long id, String name, String venue, LocalDateTime showTime) {
}