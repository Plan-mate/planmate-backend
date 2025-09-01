package com.planmate.planmate_backend.event.repository;

import com.planmate.planmate_backend.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends  JpaRepository<Event, Long> {

}