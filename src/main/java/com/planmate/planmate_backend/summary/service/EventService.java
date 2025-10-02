package com.planmate.planmate_backend.summary.service;

import com.planmate.planmate_backend.event.dto.EventResDto;
import com.planmate.planmate_backend.event.service.GetService;
import com.planmate.planmate_backend.summary.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final GetService getService;

    public EventSummaryDto getTodayEventSummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<EventResDto> events = Collections.emptyList();
        try {
            events = getService.getEvents(userId, today, today);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<EventResDto> todayEvents = events.stream()
                .filter(e -> {
                    LocalDateTime st = e.getStartTime();
                    LocalDateTime et = e.getEndTime() != null ? e.getEndTime() : st;
                    return st != null && !et.isBefore(startOfDay) && !st.isAfter(endOfDay);
                })
                .toList();

        int totalCount = todayEvents.size();

        Map<String, Long> categoryCountMap = todayEvents.stream()
                .collect(Collectors.groupingBy(this::safeCategoryName, Collectors.counting()));

        List<CategoryCountDto> categoryCounts = categoryCountMap.entrySet().stream()
                .map(en -> new CategoryCountDto(en.getKey(), en.getValue().intValue()))
                .sorted(Comparator.comparing(CategoryCountDto::getCount).reversed())
                .toList();

        List<EventDto> mainEvents = todayEvents.stream()
                .sorted(Comparator.comparing(EventResDto::getStartTime))
                .limit(5)
                .map(e -> new EventDto(
                        e.getTitle(),
                        e.getStartTime(),
                        e.getEndTime(),
                        Boolean.TRUE.equals(e.getIsRecurring()),
                        1,
                        safeCategoryName(e)
                ))
                .toList();

        String message = buildTodayMessage(totalCount, categoryCounts, mainEvents);

        return new EventSummaryDto(totalCount, categoryCounts, mainEvents, message);
    }

    private String safeCategoryName(EventResDto e) {
        try {
            if (e.getCategory() != null) {
                var cat = e.getCategory();
                return (String) cat.getClass().getMethod("getName").invoke(cat);
            }
        } catch (Exception ignored) { }
        return "기타";
    }

    private String buildTodayMessage(int totalCount,
                                     List<CategoryCountDto> categoryCounts,
                                     List<EventDto> mainEvents) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 오늘 일정 요약\n");
        sb.append(String.format("오늘은 총 %d개의 일정이 있습니다.", totalCount));
        sb.append("\n");

        if (!categoryCounts.isEmpty()) {
            String cats = categoryCounts.stream()
                    .map(c -> c.getCategoryName() + " " + c.getCount() + "개")
                    .collect(Collectors.joining(", "));
            sb.append("(카테고리: ").append(cats).append(")");
        }
        sb.append("\n\n");

        if (mainEvents.isEmpty()) {
            sb.append("오늘은 예정된 일정이 없습니다.");
        } else {
            sb.append("📌 주요 일정\n");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
            for (EventDto e : mainEvents) {
                String timeRange = "시간 미정";
                if (e.getStartTime() != null) {
                    String start = e.getStartTime().format(timeFmt);
                    if (e.getEndTime() != null) {
                        String end = e.getEndTime().format(timeFmt);
                        timeRange = start + " ~ " + end;
                    } else {
                        timeRange = start;
                    }
                }
                sb.append(String.format("- %s (%s)\n", e.getTitle(), timeRange));
            }
        }

        return sb.toString().trim();
    }
}
