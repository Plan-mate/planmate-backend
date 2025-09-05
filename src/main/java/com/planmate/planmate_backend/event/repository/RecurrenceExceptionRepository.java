package com.planmate.planmate_backend.event.repository;

import com.planmate.planmate_backend.event.entity.RecurrenceException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {

}