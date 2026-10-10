package com.bunny.ticket_booking.service;

import com.bunny.ticket_booking.dto.BookingResponse;
import com.bunny.ticket_booking.dto.SeatResponse;
import com.bunny.ticket_booking.entity.*;
import com.bunny.ticket_booking.repository.BookingRepository;
import com.bunny.ticket_booking.repository.SeatRepository;
import com.bunny.ticket_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int HOLD_MINUTES = 5;

    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public SeatResponse holdSeat(Long seatId, Long userId) {
        findUser(userId);
        Seat seat = findSeat(seatId);
        LocalDateTime now = LocalDateTime.now();

        boolean holdExpired = seat.getStatus() == SeatStatus.HELD
                && seat.getHeldUntil().isBefore(now);

        if (seat.getStatus() != SeatStatus.AVAILABLE && !holdExpired) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is not available");
        }

        seat.setStatus(SeatStatus.HELD);
        seat.setHeldByUserId(userId);
        seat.setHeldUntil(now.plusMinutes(HOLD_MINUTES));
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getStatus());
    }

    @Transactional
    public BookingResponse confirm(Long seatId, Long userId) {
        User user = findUser(userId);
        Seat seat = findSeat(seatId);

        boolean heldByThisUser = seat.getStatus() == SeatStatus.HELD
                && userId.equals(seat.getHeldByUserId())
                && seat.getHeldUntil().isAfter(LocalDateTime.now());

        if (!heldByThisUser) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seat is not held by this user, or the hold expired");
        }

        seat.setStatus(SeatStatus.BOOKED);
        seat.setHeldByUserId(null);
        seat.setHeldUntil(null);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSeat(seat);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancel(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Seat seat = findSeat(booking.getSeat().getId());
        seat.setStatus(SeatStatus.AVAILABLE);
        return toResponse(booking);
    }

    private Seat findSeat(Long id) {
        return seatRepository.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Seat not found: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(b.getId(), b.getSeat().getId(), b.getSeat().getSeatNumber(),
                b.getUser().getId(), b.getStatus(), b.getCreatedAt());
    }
}