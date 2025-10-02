package com.planmate.planmate_backend.summary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryDto {
    private int totalEventCount;
    private List<CategoryCountDto> categoryCounts;
    private List<EventDto> mainEvents;
    private String message;
}
