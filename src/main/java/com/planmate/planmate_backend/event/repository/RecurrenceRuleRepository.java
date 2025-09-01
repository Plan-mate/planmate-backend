package com.planmate.planmate_backend.event.repository;

import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {

}