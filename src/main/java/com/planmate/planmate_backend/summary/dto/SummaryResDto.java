package com.planmate.planmate_backend.summary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResDto {
    private WeatherSummaryDto weather;
    private int totalEventCount;
    private List<CategoryCountDto> categoryCounts;
    private List<EventSummaryDto> mainEvents;
    private String message;
}
