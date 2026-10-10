package com.bunny.ticket_booking.dto;

import jakarta.validation.constraints.NotNull;

public record UserActionRequest(@NotNull Long userId) {
}