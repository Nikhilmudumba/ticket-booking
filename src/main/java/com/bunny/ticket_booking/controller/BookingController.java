package com.bunny.ticket_booking.controller;

import com.bunny.ticket_booking.dto.BookingResponse;
import com.bunny.ticket_booking.dto.SeatResponse;
import com.bunny.ticket_booking.security.AuthUser;
import com.bunny.ticket_booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/seats/{seatId}/hold")
    public SeatResponse hold(@PathVariable Long seatId, @AuthenticationPrincipal AuthUser user) {
        return bookingService.holdSeat(seatId, user.id());
    }

    @PostMapping("/seats/{seatId}/confirm")
    public BookingResponse confirm(@PathVariable Long seatId, @AuthenticationPrincipal AuthUser user) {
        return bookingService.confirm(seatId, user.id());
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable Long bookingId, @AuthenticationPrincipal AuthUser user) {
        return bookingService.cancel(bookingId, user.id());
    }

    @GetMapping("/bookings/me")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal AuthUser user) {
        return bookingService.myBookings(user.id());
    }
}