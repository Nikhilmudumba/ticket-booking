package com.bunny.ticket_booking.controller;

import com.bunny.ticket_booking.dto.CreateEventRequest;
import com.bunny.ticket_booking.dto.EventResponse;
import com.bunny.ticket_booking.dto.SeatResponse;
import com.bunny.ticket_booking.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping
    public List<EventResponse> list() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}/seats")
    public List<SeatResponse> seats(@PathVariable Long id) {
        return eventService.getSeats(id);
    }
}