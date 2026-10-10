package com.bunny.ticket_booking.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank String name,
        @NotBlank String venue,
        @NotNull @Future LocalDateTime showTime,
        @Min(1) @Max(26) int rows,
        @Min(1) @Max(50) int seatsPerRow
) {
}