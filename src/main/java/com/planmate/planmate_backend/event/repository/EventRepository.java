package com.planmate.planmate_backend.event.repository;

import com.planmate.planmate_backend.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends  JpaRepository<Event, Long> {

    Optional<Event> findByIdAndUserId(Long eventId, Long userId);

    List<Event> findByOriginalEventId(Long originalEventId);

    @Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.startTime < :end AND e.endTime >= :start")
    List<Event> findByUserAndPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT e.category.id, COUNT(e)
        FROM Event e
        WHERE e.createdAt >= :start
          AND e.createdAt <  :end
        GROUP BY e.category.id
        """)
    List<Object[]> countByCategoryCreatedBetween(LocalDateTime start, LocalDateTime end);
}