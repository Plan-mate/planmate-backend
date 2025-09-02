package com.planmate.planmate_backend.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResEventDto {
    private Long id;
    private CategoryDto category;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isRecurring;
    private Long originalEventId;
    private CreateRecurrenceRuleDto recurrenceRule;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
