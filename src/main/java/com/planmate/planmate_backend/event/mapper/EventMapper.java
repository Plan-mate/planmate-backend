package com.planmate.planmate_backend.event.mapper;

import com.planmate.planmate_backend.event.dto.EventResDto;
import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import com.planmate.planmate_backend.event.entity.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final CategoryMapper categoryMapper;
    private final RecurrenceRuleMapper recurrenceRuleMapper;

    public EventResDto toDto(Event event, RecurrenceRuleDto rule) {
        return new EventResDto(
                event.getId(),
                categoryMapper.toDto(event.getCategory()),
                event.getTitle(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getIsRecurring(),
                event.getOriginalEventId(),
                rule != null ? recurrenceRuleMapper.toDto(rule) : null,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
