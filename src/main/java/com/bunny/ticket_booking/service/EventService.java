package com.bunny.ticket_booking.service;

import com.bunny.ticket_booking.dto.CreateEventRequest;
import com.bunny.ticket_booking.dto.EventResponse;
import com.bunny.ticket_booking.dto.SeatResponse;
import com.bunny.ticket_booking.entity.Event;
import com.bunny.ticket_booking.entity.Seat;
import com.bunny.ticket_booking.repository.EventRepository;
import com.bunny.ticket_booking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setVenue(request.venue());
        event.setShowTime(request.showTime());

        for (int r = 0; r < request.rows(); r++) {
            char rowLetter = (char) ('A' + r);
            for (int n = 1; n <= request.seatsPerRow(); n++) {
                Seat seat = new Seat();
                seat.setSeatNumber(rowLetter + String.valueOf(n));
                seat.setEvent(event);
                event.getSeats().add(seat);
            }
        }

        return toResponse(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + eventId);
        }
        return seatRepository.findByEventIdOrderByIdAsc(eventId).stream()
                .map(s -> new SeatResponse(s.getId(), s.getSeatNumber(), s.getStatus()))
                .toList();
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(e.getId(), e.getName(), e.getVenue(), e.getShowTime());
    }
}