package com.planmate.planmate_backend.summary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherBasicInfo {
    private Map<LocalDateTime, Map<String, String>> forecastMap;
    private String todaySky;
    private Integer tmx;
    private Integer tmn;
    private String wct;
}
