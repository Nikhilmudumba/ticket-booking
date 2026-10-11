package com.bunny.ticket_booking.service;

import com.bunny.ticket_booking.dto.BookingResponse;
import com.bunny.ticket_booking.dto.SeatResponse;
import com.bunny.ticket_booking.entity.*;
import com.bunny.ticket_booking.repository.BookingRepository;
import com.bunny.ticket_booking.repository.SeatRepository;
import com.bunny.ticket_booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock SeatRepository seatRepository;
    @Mock UserRepository userRepository;
    @Mock BookingRepository bookingRepository;
    @InjectMocks BookingService bookingService;

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setName("user" + id);
        u.setEmail("u" + id + "@test.com");
        return u;
    }

    private Seat seat(SeatStatus status) {
        Seat s = new Seat();
        s.setId(1L);
        s.setSeatNumber("A1");
        s.setStatus(status);
        return s;
    }

    private Booking booking(Long ownerId, Seat seat, BookingStatus status) {
        Booking b = new Booking();
        b.setId(1L);
        b.setUser(user(ownerId));
        b.setSeat(seat);
        b.setStatus(status);
        return b;
    }

    private void stubUserAndSeat(Long userId, Seat seat) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(seatRepository.findByIdForUpdate(seat.getId())).thenReturn(Optional.of(seat));
    }

    private void assertStatus(int expected, Runnable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expected, ex.getStatusCode().value());
    }

    @Test
    void holdAvailableSeat_marksItHeld() {
        Seat seat = seat(SeatStatus.AVAILABLE);
        stubUserAndSeat(1L, seat);

        SeatResponse result = bookingService.holdSeat(1L, 1L);

        assertEquals(SeatStatus.HELD, result.status());
        assertEquals(1L, seat.getHeldByUserId());
        assertTrue(seat.getHeldUntil().isAfter(LocalDateTime.now()));
    }

    @Test
    void holdBookedSeat_returnsConflict() {
        Seat seat = seat(SeatStatus.BOOKED);
        stubUserAndSeat(1L, seat);

        assertStatus(409, () -> bookingService.holdSeat(1L, 1L));
    }

    @Test
    void holdSeatWithActiveHold_returnsConflict() {
        Seat seat = seat(SeatStatus.HELD);
        seat.setHeldByUserId(2L);
        seat.setHeldUntil(LocalDateTime.now().plusMinutes(3));
        stubUserAndSeat(1L, seat);

        assertStatus(409, () -> bookingService.holdSeat(1L, 1L));
    }

    @Test
    void holdSeatWithExpiredHold_canBeTakenOver() {
        Seat seat = seat(SeatStatus.HELD);
        seat.setHeldByUserId(2L);
        seat.setHeldUntil(LocalDateTime.now().minusMinutes(1));
        stubUserAndSeat(1L, seat);

        SeatResponse result = bookingService.holdSeat(1L, 1L);

        assertEquals(SeatStatus.HELD, result.status());
        assertEquals(1L, seat.getHeldByUserId());
    }

    @Test
    void confirmByHolder_createsBooking() {
        Seat seat = seat(SeatStatus.HELD);
        seat.setHeldByUserId(1L);
        seat.setHeldUntil(LocalDateTime.now().plusMinutes(3));
        stubUserAndSeat(1L, seat);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = bookingService.confirm(1L, 1L);

        assertEquals(BookingStatus.CONFIRMED, result.status());
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        assertNull(seat.getHeldByUserId());
    }

    @Test
    void confirmByDifferentUser_returnsConflict() {
        Seat seat = seat(SeatStatus.HELD);
        seat.setHeldByUserId(2L);
        seat.setHeldUntil(LocalDateTime.now().plusMinutes(3));
        stubUserAndSeat(1L, seat);

        assertStatus(409, () -> bookingService.confirm(1L, 1L));
    }

    @Test
    void confirmAfterHoldExpired_returnsConflict() {
        Seat seat = seat(SeatStatus.HELD);
        seat.setHeldByUserId(1L);
        seat.setHeldUntil(LocalDateTime.now().minusMinutes(1));
        stubUserAndSeat(1L, seat);

        assertStatus(409, () -> bookingService.confirm(1L, 1L));
    }

    @Test
    void cancelOwnBooking_freesTheSeat() {
        Seat seat = seat(SeatStatus.BOOKED);
        Booking booking = booking(1L, seat, BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(seatRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(seat));

        BookingResponse result = bookingService.cancel(1L, 1L);

        assertEquals(BookingStatus.CANCELLED, result.status());
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void cancelAnotherUsersBooking_returns403AndTouchesNothing() {
        Seat seat = seat(SeatStatus.BOOKED);
        Booking booking = booking(1L, seat, BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertStatus(403, () -> bookingService.cancel(1L, 2L));

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        verifyNoInteractions(seatRepository);
    }

    @Test
    void cancelTwice_returnsConflict() {
        Booking booking = booking(1L, seat(SeatStatus.AVAILABLE), BookingStatus.CANCELLED);
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertStatus(409, () -> bookingService.cancel(1L, 1L));
    }

    @Test
    void cancelUnknownBooking_returns404() {
        when(bookingRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertStatus(404, () -> bookingService.cancel(99L, 1L));
    }
}