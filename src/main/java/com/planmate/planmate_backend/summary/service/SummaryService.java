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
public class SummaryService {

    private final WeatherService weatherService;
    private final GetService getService;

    public SummaryResDto getTodaySummary(Long userId, SummaryReqDto dto) {
        WeatherSummaryDto weather = weatherService.getWeatherSummary(dto.getNx(), dto.getNy(), dto.getLocationName());

        YearMonth ym = YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<EventResDto> events = Collections.emptyList();
        try {
            events = getService.getEvents(userId, start, end);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int totalCount = (int) events.stream()
                .filter(e -> {
                    LocalDate startDate = e.getStartTime() != null ? e.getStartTime().toLocalDate() : null;
                    LocalDate endDate = e.getEndTime() != null ? e.getEndTime().toLocalDate() : startDate;
                    if (Boolean.TRUE.equals(e.getIsRecurring())) return true;
                    if (startDate == null) return false;
                    return !endDate.isBefore(today);
                })
                .count();

        Map<String, Long> categoryCountMap = events.stream()
                .filter(e -> {
                    LocalDate startDate = e.getStartTime() != null ? e.getStartTime().toLocalDate() : null;
                    LocalDate endDate = e.getEndTime() != null ? e.getEndTime().toLocalDate() : startDate;
                    if (Boolean.TRUE.equals(e.getIsRecurring())) return true;
                    if (startDate == null) return false;
                    return !endDate.isBefore(today);
                })
                .collect(Collectors.groupingBy(this::safeCategoryName, Collectors.counting()));

        List<CategoryCountDto> categoryCounts = categoryCountMap.entrySet().stream()
                .map(en -> new CategoryCountDto(en.getKey(), en.getValue().intValue()))
                .sorted(Comparator.comparing(CategoryCountDto::getCount).reversed())
                .collect(Collectors.toList());

        Map<String, List<EventResDto>> recurringGroups = events.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsRecurring()))
                .collect(Collectors.groupingBy(EventResDto::getTitle));

        List<EventSummaryDto> mainEvents = new ArrayList<>();

        for (Map.Entry<String, List<EventResDto>> entry : recurringGroups.entrySet()) {
            List<EventResDto> group = entry.getValue();
            EventResDto representative = group.stream()
                    .min(Comparator.comparing(EventResDto::getStartTime))
                    .orElse(group.get(0));

            EventSummaryDto summary = new EventSummaryDto(
                    representative.getTitle(),
                    representative.getStartTime(),
                    representative.getEndTime(),
                    true,
                    group.size(),
                    safeCategoryName(representative)
            );
            mainEvents.add(summary);
        }

        List<EventResDto> oneOffs = events.stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsRecurring()))
                .filter(e -> {
                    LocalDate startDate = e.getStartTime() != null ? e.getStartTime().toLocalDate() : null;
                    LocalDate endDate = e.getEndTime() != null ? e.getEndTime().toLocalDate() : startDate;
                    if (startDate == null) return false;
                    return !endDate.isBefore(today);
                })
                .sorted(Comparator.comparing(EventResDto::getStartTime))
                .toList();

        int MAX_MAIN = 5;
        int remainingSlots = Math.max(0, MAX_MAIN - mainEvents.size());
        for (int i = 0; i < Math.min(remainingSlots, oneOffs.size()); i++) {
            EventResDto e = oneOffs.get(i);
            mainEvents.add(new EventSummaryDto(
                    e.getTitle(),
                    e.getStartTime(),
                    e.getEndTime(),
                    false,
                    1,
                    safeCategoryName(e)
            ));
        }

        mainEvents.sort(Comparator.comparing(es -> Optional.ofNullable(es.getStartTime()).orElse(LocalDateTime.MAX)));

        String message = buildCombinedMessage(totalCount, categoryCounts, mainEvents);

        return new SummaryResDto(weather, totalCount, categoryCounts, mainEvents, message);
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

    private String buildCombinedMessage(int totalCount,
                                        List<CategoryCountDto> categoryCounts,
                                        List<EventSummaryDto> mainEvents) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("🗓 %d월 일정\n", YearMonth.now().getMonthValue()));
        sb.append(String.format("총 %d개 일정", totalCount));
        if (!categoryCounts.isEmpty()) {
            String catSummary = categoryCounts.stream()
                    .map(c -> c.getCategoryName() + " " + c.getCount() + "개")
                    .collect(Collectors.joining(", "));
            sb.append(" (주요 카테고리: ").append(catSummary).append(")");
        }
        sb.append("\n\n");

        if (mainEvents.isEmpty()) {
            sb.append("이번 달 주요 일정이 없습니다.");
        } else {
            sb.append("📌 주요 일정\n");
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("M월 d일");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

            for (EventSummaryDto es : mainEvents) {
                if (es.isRecurring()) {
                    sb.append(String.format("- %s (반복 일정, 총 %d회)\n", es.getTitle(), es.getOccurrenceCount()));
                } else {
                    String when = "날짜 미정";
                    if (es.getStartTime() != null) {
                        String start = es.getStartTime().format(DateTimeFormatter.ofPattern("M월 d일 HH:mm"));
                        if (es.getEndTime() != null) {
                            String end = es.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                            when = start + " ~ " + end;
                        } else {
                            when = start;
                        }
                    }
                    sb.append(String.format("- %s (%s)\n", es.getTitle(), when));
                }
            }
        }

        return sb.toString().trim();
    }
}
