package com.planmate.planmate_backend.event.repository;

import com.planmate.planmate_backend.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends  JpaRepository<Event, Long> {


    @Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.startTime < :end AND e.endTime >= :start")
    List<Event> findByUserAndPeriod(Long userId, LocalDateTime start, LocalDateTime end);
}