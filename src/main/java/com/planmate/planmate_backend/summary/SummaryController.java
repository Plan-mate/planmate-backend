package com.planmate.planmate_backend.summary;

import com.planmate.planmate_backend.summary.dto.LocationDataDto;
import com.planmate.planmate_backend.summary.dto.EventSummaryDto;
import com.planmate.planmate_backend.summary.dto.WeatherSummaryDto;
import com.planmate.planmate_backend.summary.service.EventService;
import com.planmate.planmate_backend.summary.service.WeatherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final EventService summaryService;
    private final WeatherService weatherService;

    @GetMapping("/today/weather")
    public WeatherSummaryDto getTodayWeatherSummary(@Valid LocationDataDto dto) {
        return weatherService.getWeatherSummary(dto.getNx(), dto.getNy(), dto.getLocationName());
    }

    @GetMapping("/today/event")
    public EventSummaryDto getTodayEventSummary(@AuthenticationPrincipal Long userId) {
        return summaryService.getTodayEventSummary(userId);
    }

    // 추천 API
    @GetMapping("/recommend")
    public void getRecommendations(@AuthenticationPrincipal Long userId) {
        System.out.println(userId);
    }
}