package com.planmate.planmate_backend.summary.service;

import com.planmate.planmate_backend.summary.dto.SummaryReqDto;
import com.planmate.planmate_backend.summary.dto.WeatherSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final WeatherService weatherService;

    public String getTodaySummary(SummaryReqDto dto) {
        // 날씨 api
        WeatherSummaryDto weather =  weatherService.getWeatherSummary(dto.getNx(), dto.getNy(), dto.getLocationName());
        System.out.println(weather);

        // 운세 api


        return "aasdfa";
    }
}
