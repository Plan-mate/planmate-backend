package com.planmate.planmate_backend.summary.dto;

import com.planmate.planmate_backend.event.dto.EventReqDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendTemplate {
    private String category;
    private String title;
    private String description;
    private int durationHours;
    private int durationMinutes;

    public EventReqDto toEventDto(LocalDateTime startTime) {
        LocalDateTime end = safeEndTime(startTime, durationHours, durationMinutes);
        return new EventReqDto(category, description, startTime, end, title, false, null);
    }

    private LocalDateTime safeEndTime(LocalDateTime start, int plusHours, int plusMinutes) {
        LocalDateTime end = start.plusHours(plusHours).plusMinutes(plusMinutes);
        return end.toLocalDate().isEqual(start.toLocalDate()) ? end : start.withMinute(59);
    }
}
