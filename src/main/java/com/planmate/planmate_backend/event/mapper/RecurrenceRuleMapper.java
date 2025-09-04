package com.planmate.planmate_backend.event.mapper;

import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import org.springframework.stereotype.Component;

@Component
public class RecurrenceRuleMapper {
    public RecurrenceRuleDto toDto(RecurrenceRuleDto rule) {
        return new RecurrenceRuleDto(
                rule.getDaysOfMonth(),
                rule.getDaysOfWeek(),
                rule.getFrequency(),
                rule.getInterval(),
                rule.getEndDate()
        );
    }
}
