package com.planmate.planmate_backend.summary;

import com.planmate.planmate_backend.event.dto.EventReqDto;
import com.planmate.planmate_backend.summary.dto.LocationDataDto;
import com.planmate.planmate_backend.summary.dto.EventSummaryDto;
import com.planmate.planmate_backend.summary.dto.WeatherSummaryDto;
import com.planmate.planmate_backend.summary.service.EventService;
import com.planmate.planmate_backend.summary.service.RecommendService;
import com.planmate.planmate_backend.summary.service.WeatherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final EventService summaryService;
    private final WeatherService weatherService;
    private final RecommendService recommendService;

    @GetMapping("/today/weather")
    public WeatherSummaryDto getTodayWeatherSummary(@Valid LocationDataDto dto) {
        return weatherService.getWeatherSummary(dto.getNx(), dto.getNy(), dto.getLocationName());
    }

    @GetMapping("/today/event")
    public EventSummaryDto getTodayEventSummary(@AuthenticationPrincipal Long userId) {
        return summaryService.getTodayEventSummary(userId);
    }

    @GetMapping("/recommend")
    public List<EventReqDto> getTodayRecommend(@AuthenticationPrincipal Long userId, @Valid LocationDataDto dto) {
        System.out.println("여긴 추천" + dto);
        List<EventReqDto> as123df =  recommendService.getTodayRecommend(userId, dto);
        System.out.println(as123df);
        return as123df;
    }
}