package com.planmate.planmate_backend.summary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean recurring;
    private int occurrenceCount;
    private String categoryName;
}
