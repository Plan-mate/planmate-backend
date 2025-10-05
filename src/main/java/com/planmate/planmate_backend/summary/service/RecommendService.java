package com.planmate.planmate_backend.summary.service;

import com.planmate.planmate_backend.event.dto.EventReqDto;
import com.planmate.planmate_backend.event.dto.EventResDto;
import com.planmate.planmate_backend.event.service.GetService;
import com.planmate.planmate_backend.summary.dto.RecommendTemplate;
import com.planmate.planmate_backend.summary.dto.WeatherBasicInfo;
import com.planmate.planmate_backend.summary.loader.GlobalRecommendLoader;
import com.planmate.planmate_backend.summary.loader.PersonalRecommendLoader;
import com.planmate.planmate_backend.summary.loader.WeatherKeyBuilder;
import com.planmate.planmate_backend.summary.loader.WeatherRecommendLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final WeatherService weatherService;
    private final WeatherRecommendLoader weatherLoader;
    private final PersonalRecommendLoader personalLoader;
    private final GlobalRecommendLoader globalLoader;
    private final PersonalStatAnalyzer personalStatAnalyzer;
    private final GlobalStatAnalyzer globalStatAnalyzer;
    private final GetService getService;

    public List<EventReqDto> getTodayRecommend(Long userId, int nx, int ny) {
        List<EventReqDto> out = new ArrayList<>(3);
        addIfNotNull(out, getWeatherBasedRecommendation(nx, ny));
        addIfNotNull(out, getPersonalBasedRecommendation(userId));
        addIfNotNull(out, getGlobalBasedRecommendation());
        return out;
    }

    private void addIfNotNull(List<EventReqDto> list, EventReqDto e) {
        if (e != null) list.add(e);
    }

    private int randIndex(int size) {
        return ThreadLocalRandom.current().nextInt(size);
    }

    private LocalDateTime topOfHourPlus(int hours) {
        return LocalDateTime.now().plusHours(hours).withMinute(0).withSecond(0).withNano(0);
    }

    private EventReqDto pickFromTemplates(List<RecommendTemplate> templates, LocalDateTime start) {
        if (templates == null || templates.isEmpty()) return null;
        return templates.get(randIndex(templates.size())).toEventDto(start);
    }

    private EventReqDto getWeatherBasedRecommendation(int nx, int ny) {
        String baseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        WeatherBasicInfo weather = weatherService.fetchVilageWeatherData(nx, ny, baseDate);
        String key = WeatherKeyBuilder.build(weather);
        List<RecommendTemplate> templates = weatherLoader.getRecommendations(key);
        return pickFromTemplates(templates, topOfHourPlus(1));
    }

    private EventReqDto getPersonalBasedRecommendation(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        List<EventResDto> recent = getService.getEvents(userId, startDate, today);

        LocalDateTime start = topOfHourPlus(1);

        if (recent.size() < 3) {
            return pickFromTemplates(personalLoader.getRecommendations("콜드스타트"), start);
        }

        Map<String, Long> categoryCount = personalStatAnalyzer.countByCategory(recent);

        if (personalStatAnalyzer.isBalanced(categoryCount)) {
            return pickFromTemplates(personalLoader.getRecommendations("콜드스타트"), start);
        }

        String least = personalStatAnalyzer.findLeastCategory(categoryCount);
        return pickFromTemplates(personalLoader.getRecommendations(least), start);
    }

    private EventReqDto getGlobalBasedRecommendation() {
        LocalDateTime now = LocalDateTime.now();
        int WINDOW_DAYS = 14;

        Optional<Long> hotCategoryOpt = globalStatAnalyzer.getHotCategory(now, WINDOW_DAYS);

        LocalDateTime start = topOfHourPlus(3);

        if (hotCategoryOpt.isEmpty()) {
            return pickFromTemplates(globalLoader.getRecommendations("콜드스타트"), start);
        }

        String key = String.valueOf(hotCategoryOpt.get());
        List<RecommendTemplate> templates = globalLoader.getRecommendations(key);

        if (templates == null || templates.isEmpty()) {
            return pickFromTemplates(globalLoader.getRecommendations("콜드스타트"), start);
        }

        return pickFromTemplates(templates, start);
    }
}
