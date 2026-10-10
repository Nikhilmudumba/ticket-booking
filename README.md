# Ticket Booking API

Backend for booking seats at events. The core problem it solves: when many users try to book the same seat at once, exactly one succeeds and everyone else gets a clean 409.

**Stack:** Java 21, Spring Boot 4, Spring Data JPA, MySQL 8, JUnit 5, Mockito

## How it works

1. **Hold:** a user holds a seat for 5 minutes (AVAILABLE to HELD).
2. **Confirm:** the same user confirms before the hold expires (HELD to BOOKED, booking created).
3. **Cancel:** the booking is cancelled and the seat becomes AVAILABLE again.
4. **Expiry:** a scheduled job releases holds that were never confirmed.

## The hard problem: double booking

The seat row is loaded with a pessimistic lock (`SELECT ... FOR UPDATE`) inside a transaction. Concurrent requests for the same seat wait for each other, then see the updated status and get a 409.

**Why pessimistic, not optimistic:** everyone wants the same hot seat at the same time, so conflicts are the normal case. Optimistic locking would make most requests fail and retry. `@Version` stays on the seat as a second layer in case a code path skips the lock.

**Proof:** a test fires 100 simultaneous hold requests at one seat.

```
SUCCESS=1 CONFLICT=99 OTHER=0
```

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/users | Create a user |
| POST | /api/events | Create an event with a seat grid |
| GET | /api/events | List events |
| GET | /api/events/{id}/seats | List seats and their status |
| POST | /api/seats/{id}/hold | Hold a seat for 5 minutes |
| POST | /api/seats/{id}/confirm | Confirm a held seat into a booking |
| POST | /api/bookings/{id}/cancel | Cancel a booking |

Interactive docs: `http://localhost:8080/swagger-ui.html`

## Run it

1. Create a MySQL database: `CREATE DATABASE ticket_booking;`
2. Copy `application.properties.example` to `application.properties` and set your password.
3. Run `TicketBookingApplication`.

## Tests

- `BookingServiceTest`: 9 unit tests for the booking rules (Mockito)
- `ConcurrentBookingTest`: 100-thread race test against a real database

## Known limitations and next steps

- Users are identified by `userId` in the request body; JWT login is the next step.
- Payments are not implemented.