package com.planmate.planmate_backend.event.repository;

import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {
    @Query("""
        SELECT r
        FROM RecurrenceRule r
        JOIN FETCH r.event e
        WHERE e.user.id = :userId
          AND r.event.startTime <= :end
          AND (r.endDate IS NULL OR r.endDate >= :start)
    """)
    List<RecurrenceRule> findRecurringEventsEndingInPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    Optional<RecurrenceRule> findByEventId(Long eventId);

    void deleteByEventId(Long eventId);
}