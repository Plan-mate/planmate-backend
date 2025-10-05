package com.planmate.planmate_backend.summary.service;

import com.planmate.planmate_backend.event.dto.EventResDto;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PersonalStatAnalyzer {

    public Map<String, Long> countByCategory(List<EventResDto> events) {
        return events.stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().getName(),
                        Collectors.mapping(
                                e -> e.getStartTime().toLocalDate(),
                                Collectors.toSet()
                        )
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (long) e.getValue().size()
                ));
    }

    public String findLeastCategory(Map<String, Long> categoryCount) {
        return categoryCount.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public boolean isBalanced(Map<String, Long> categoryCount) {
        if (categoryCount.isEmpty()) return true;

        long max = Collections.max(categoryCount.values());
        long min = Collections.min(categoryCount.values());
        return (max - min) <= 1;
    }
}
