package com.bunny.ticket_booking.controller;

import com.bunny.ticket_booking.dto.BookingResponse;
import com.bunny.ticket_booking.dto.SeatResponse;
import com.bunny.ticket_booking.dto.UserActionRequest;
import com.bunny.ticket_booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/seats/{seatId}/hold")
    public SeatResponse hold(@PathVariable Long seatId, @Valid @RequestBody UserActionRequest req) {
        return bookingService.holdSeat(seatId, req.userId());
    }

    @PostMapping("/seats/{seatId}/confirm")
    public BookingResponse confirm(@PathVariable Long seatId, @Valid @RequestBody UserActionRequest req) {
        return bookingService.confirm(seatId, req.userId());
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable Long bookingId) {
        return bookingService.cancel(bookingId);
    }
}