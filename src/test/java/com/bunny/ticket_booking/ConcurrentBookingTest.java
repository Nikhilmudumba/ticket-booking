package com.bunny.ticket_booking;

import com.bunny.ticket_booking.dto.CreateEventRequest;
import com.bunny.ticket_booking.dto.EventResponse;
import com.bunny.ticket_booking.entity.User;
import com.bunny.ticket_booking.repository.SeatRepository;
import com.bunny.ticket_booking.repository.UserRepository;
import com.bunny.ticket_booking.service.BookingService;
import com.bunny.ticket_booking.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcurrentBookingTest {

    @Autowired
    BookingService bookingService;
    @Autowired
    EventService eventService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SeatRepository seatRepository;

    @Test
    void onlyOneUserCanHoldTheSameSeat() throws Exception {
        int threads = 100;

        EventResponse event = eventService.createEvent(new CreateEventRequest(
                "Race Test", "Test Hall", LocalDateTime.now().plusDays(10), 1, 1));
        Long seatId = seatRepository.findByEventIdOrderByIdAsc(event.id()).get(0).getId();

        String tag = String.valueOf(System.nanoTime());
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            User user = new User();
            user.setName("user" + i);
            user.setEmail("user" + i + "-" + tag + "@test.com");
            user.setPasswordHash("test");
            userIds.add(userRepository.save(user).getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (Long userId : userIds) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    bookingService.holdSeat(seatId, userId);
                    success.incrementAndGet();
                } catch (ResponseStatusException e) {
                    if (e.getStatusCode().value() == 409) {
                        conflict.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } catch (Exception e) {
                    other.incrementAndGet();
                }
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.SECONDS);

        System.out.println("SUCCESS=" + success + " CONFLICT=" + conflict + " OTHER=" + other);

        assertEquals(1, success.get());
        assertEquals(threads - 1, conflict.get());
        assertEquals(0, other.get());
    }
}