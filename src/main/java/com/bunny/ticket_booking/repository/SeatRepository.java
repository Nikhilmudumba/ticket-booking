package com.bunny.ticket_booking.repository;

import com.bunny.ticket_booking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventIdOrderByIdAsc(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
        update Seat s
        set s.status = com.bunny.ticket_booking.entity.SeatStatus.AVAILABLE,
            s.heldByUserId = null,
            s.heldUntil = null,
            s.version = s.version + 1
        where s.status = com.bunny.ticket_booking.entity.SeatStatus.HELD
          and s.heldUntil < :now
        """)
    int releaseExpiredHolds(@Param("now") LocalDateTime now);
}