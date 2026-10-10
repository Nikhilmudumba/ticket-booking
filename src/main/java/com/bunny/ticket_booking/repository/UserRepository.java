package com.bunny.ticket_booking.repository;

import com.bunny.ticket_booking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}