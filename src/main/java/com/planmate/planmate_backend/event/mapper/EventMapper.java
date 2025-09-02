package com.planmate.planmate_backend.event.mapper;

import com.planmate.planmate_backend.event.dto.ResEventDto;
import com.planmate.planmate_backend.event.dto.CategoryDto;
import com.planmate.planmate_backend.event.dto.CreateRecurrenceRuleDto;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class EventMapper {

    public ResEventDto toDto(Event event, CreateRecurrenceRuleDto rule) {
            return new ResEventDto(
                event.getId(),
                new CategoryDto(
                        event.getCategory().getId(),
                        event.getCategory().getName(),
                        event.getCategory().getColor()
                ),
                event.getTitle(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getIsRecurring(),
                event.getOriginalEventId(),
                rule != null
                    ? new CreateRecurrenceRuleDto(
                        rule.getDaysOfMonth(),
                        rule.getDaysOfWeek(),
                        rule.getFrequency(),
                        rule.getInterval(),
                        rule.getEndDate()
                      )
                    : null,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
