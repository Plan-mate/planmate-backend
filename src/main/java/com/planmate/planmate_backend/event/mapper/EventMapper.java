package com.planmate.planmate_backend.event.mapper;

import com.planmate.planmate_backend.event.dto.EventReqDto;
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

    public void updateEventFromDto(Event event, EventReqDto dto) {
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getStartTime() != null) event.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) event.setEndTime(dto.getEndTime());
        if (dto.getIsRecurring() != null) event.setIsRecurring(dto.getIsRecurring());

    }

    public Event createOverrideEvent(Event original, EventReqDto dto) {
        return Event.builder()
                .user(original.getUser())
                .category(original.getCategory())
                .title(dto.getTitle() != null ? dto.getTitle() : original.getTitle())
                .description(dto.getDescription() != null ? dto.getDescription() : original.getDescription())
                .startTime(dto.getStartTime() != null ? dto.getStartTime() : original.getStartTime())
                .endTime(dto.getEndTime() != null ? dto.getEndTime() : original.getEndTime())
                .isRecurring(Boolean.TRUE.equals(dto.getIsRecurring()))
                .originalEventId(original.getId())
                .build();
    }
}
