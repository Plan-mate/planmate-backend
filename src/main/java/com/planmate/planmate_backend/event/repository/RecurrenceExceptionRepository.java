package com.planmate.planmate_backend.event.repository;

import com.planmate.planmate_backend.event.entity.RecurrenceException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {

    List<RecurrenceException> findByEventId(Long eventId);

    void deleteByEventId(Long eventId);

    void deleteByEventIdAndExceptionDateAfter(Long eventId, LocalDate date);
}