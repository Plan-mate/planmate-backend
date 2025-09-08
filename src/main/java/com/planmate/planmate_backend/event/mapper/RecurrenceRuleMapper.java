package com.planmate.planmate_backend.event.mapper;

import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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

    public RecurrenceRuleDto convertRuleToDto(RecurrenceRule rule) {
        List<String> daysOfWeek = rule.getDaysOfWeek() != null
                ? Arrays.stream(rule.getDaysOfWeek().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList()
                : null;

        List<Integer> daysOfMonth = rule.getDaysOfMonth() != null
                ? Arrays.stream(rule.getDaysOfMonth().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList()
                : null;

        return new RecurrenceRuleDto(
                daysOfMonth,
                daysOfWeek,
                rule.getFrequency(),
                rule.getInterval(),
                rule.getEndDate()
        );
    }
}
