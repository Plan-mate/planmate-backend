package com.planmate.planmate_backend.event.service;

import com.planmate.planmate_backend.event.dto.EventResDto;
import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import com.planmate.planmate_backend.event.dto.CategoryDto;
import com.planmate.planmate_backend.event.entity.Category;
import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.event.mapper.CategoryMapper;
import com.planmate.planmate_backend.event.mapper.EventMapper;
import com.planmate.planmate_backend.event.mapper.RecurrenceRuleMapper;
import com.planmate.planmate_backend.event.repository.CategoryRepository;
import com.planmate.planmate_backend.event.repository.EventRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;

    private final EventMapper eventMapper;
    private final CategoryMapper categoryMapper;
    private final RecurrenceRuleMapper recurrenceRuleMapper;

    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public List<EventResDto> getEvents(Long userId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        // 원본 이벤트 조회
        List<Event> events = eventRepository.findByUserAndPeriod(userId, startDateTime, endDateTime);

        // 반복 규칙 조회
        List<RecurrenceRule> rules =
                recurrenceRuleRepository.findRecurringEventsEndingInPeriod(userId, startDateTime, endDateTime);

        // 반복 규칙 DTO Map
        Map<Long, RecurrenceRuleDto> ruleMap = rules.stream()
                .collect(Collectors.toMap(
                        r -> r.getEvent().getId(),
                        r -> new RecurrenceRuleDto(
                                r.getDaysOfMonth() != null
                                        ? Arrays.stream(r.getDaysOfMonth().split(","))
                                        .map(Integer::parseInt).toList()
                                        : null,
                                r.getDaysOfWeek() != null
                                        ? Arrays.asList(r.getDaysOfWeek().split(","))
                                        : null,
                                r.getFrequency(),
                                r.getInterval(),
                                r.getEndDate()
                        )
                ));

        // 원본 이벤트 슬롯 체크
        List<EventResDto> result = new ArrayList<>();
        List<Event> originalEvents = new ArrayList<>();
        for (Event e : events) {
            originalEvents.add(e);
            RecurrenceRuleDto ruleDto = e.getIsRecurring() ? ruleMap.get(e.getId()) : null;
            result.add(eventMapper.toDto(e, ruleDto));
        }

        // 반복 인스턴스 생성
        for (RecurrenceRule rule : rules) {
            Event baseEvent = rule.getEvent();
            RecurrenceRuleDto ruleDto = ruleMap.get(baseEvent.getId());

            List<Event> instances = generateRecurringInstances(baseEvent, rule, startDateTime, endDateTime);

            for (Event inst : instances) {
                // 원본 이벤트와만 겹침 체크
                boolean overlapsOriginal = originalEvents.stream()
                        .anyMatch(orig -> isOverlap(orig, inst));
                if (!overlapsOriginal) {
                    result.add(eventMapper.toDto(inst, ruleDto));
                }
            }
        }

        return result;
    }

    // 동일 시간 범위 이벤트 판단
    private boolean isOverlap(Event e1, Event e2) {
        return !e1.getEndTime().isBefore(e2.getStartTime()) &&
                !e1.getStartTime().isAfter(e2.getEndTime());
    }

    private String slotKey(LocalDateTime start, LocalDateTime end) {
        return start.toString() + "|" + end.toString();
    }

    public List<Event> generateRecurringInstances(Event baseEvent,
                                                  RecurrenceRule rule,
                                                  LocalDateTime periodStart,
                                                  LocalDateTime periodEnd) {
        List<Event> out = new ArrayList<>();

        Duration duration = Duration.between(baseEvent.getStartTime(), baseEvent.getEndTime());
        LocalDateTime seriesStart = baseEvent.getStartTime();
        LocalDateTime seriesEnd = (rule.getEndDate() != null) ? rule.getEndDate() : periodEnd;
        LocalDateTime scanEnd = min(seriesEnd, periodEnd);

        List<DayOfWeek> dowList = rule.getDaysOfWeek() != null
                ? Arrays.stream(rule.getDaysOfWeek().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    int val = Integer.parseInt(s);
                    return DayOfWeek.of((val == 0 ? 7 : val));
                })
                .toList()
                : List.of();

        List<Integer> domList = rule.getDaysOfMonth() != null
                ? Arrays.stream(rule.getDaysOfMonth().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .toList()
                : List.of();

        switch (rule.getFrequency()) {
            case DAILY -> {
                LocalDateTime cur = alignDaily(seriesStart, periodStart, rule.getInterval());
                while (!cur.isAfter(scanEnd)) {
                    addIfOverlap(out, cur, cur.plus(duration), periodStart, periodEnd, baseEvent);
                    cur = cur.plusDays(rule.getInterval());
                }
            }
            case WEEKLY -> {
                if (!dowList.isEmpty()) {
                    LocalDate firstWeek = startOfWeek(periodStart.toLocalDate());
                    for (LocalDate weekStart = firstWeek;
                         !weekStart.atStartOfDay().isAfter(scanEnd);
                         weekStart = weekStart.plusWeeks(rule.getInterval())) {

                        for (DayOfWeek dow : dowList) {
                            LocalDateTime cur = weekStart.atTime(seriesStart.toLocalTime())
                                    .with(TemporalAdjusters.nextOrSame(dow));
                            if (cur.isBefore(seriesStart)) continue;
                            if (cur.isAfter(scanEnd)) continue;
                            addIfOverlap(out, cur, cur.plus(duration), periodStart, periodEnd, baseEvent);
                        }
                    }
                } else {
                    LocalDateTime cur = alignWeekly(seriesStart, periodStart, rule.getInterval());
                    while (!cur.isAfter(scanEnd)) {
                        addIfOverlap(out, cur, cur.plus(duration), periodStart, periodEnd, baseEvent);
                        cur = cur.plusWeeks(rule.getInterval());
                    }
                }
            }
            case MONTHLY -> {
                if (!domList.isEmpty()) {
                    LocalDate monthCursor = LocalDate.of(periodStart.getYear(), periodStart.getMonth(), 1);
                    while (!monthCursor.atStartOfDay().isAfter(scanEnd)) {
                        for (Integer dom : domList) {
                            if (dom < 1 || dom > monthCursor.lengthOfMonth()) continue;
                            LocalDateTime cur = monthCursor.withDayOfMonth(dom).atTime(seriesStart.toLocalTime());
                            if (cur.isBefore(seriesStart)) continue;
                            if (cur.isAfter(scanEnd)) continue;
                            addIfOverlap(out, cur, cur.plus(duration), periodStart, periodEnd, baseEvent);
                        }
                        monthCursor = monthCursor.plusMonths(rule.getInterval());
                    }
                } else {
                    LocalDateTime cur = alignMonthly(seriesStart, periodStart, rule.getInterval());
                    while (!cur.isAfter(scanEnd)) {
                        addIfOverlap(out, cur, cur.plus(duration), periodStart, periodEnd, baseEvent);
                        cur = cur.plusMonths(rule.getInterval());
                    }
                }
            }
            default -> throw new IllegalArgumentException("Unsupported frequency: " + rule.getFrequency());
        }

        return out;
    }

    private void addIfOverlap(List<Event> out,
                              LocalDateTime s,
                              LocalDateTime e,
                              LocalDateTime periodStart,
                              LocalDateTime periodEnd,
                              Event baseEvent) {
        if (e.isAfter(periodStart) && s.isBefore(periodEnd.plusSeconds(1))) {
            Event inst = new Event();
            inst.setId(null);
            inst.setUser(baseEvent.getUser());
            inst.setCategory(baseEvent.getCategory());
            inst.setTitle(baseEvent.getTitle());
            inst.setDescription(baseEvent.getDescription());
            inst.setStartTime(s);
            inst.setEndTime(e);
            inst.setIsRecurring(true);
            inst.setOriginalEventId(baseEvent.getId());
            inst.setCreatedAt(baseEvent.getCreatedAt());
            inst.setUpdatedAt(baseEvent.getUpdatedAt());
            out.add(inst);
        }
    }

    private LocalDate startOfWeek(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDateTime alignDaily(LocalDateTime seriesStart, LocalDateTime periodStart, int intervalDays) {
        if (!periodStart.isAfter(seriesStart)) return seriesStart;
        long diff = Duration.between(seriesStart.toLocalDate().atStartOfDay(),
                periodStart.toLocalDate().atStartOfDay()).toDays();
        long steps = (diff / intervalDays) * intervalDays;
        LocalDateTime aligned = seriesStart.plusDays(steps);
        while (aligned.isBefore(periodStart)) aligned = aligned.plusDays(intervalDays);
        return aligned;
    }

    private LocalDateTime alignWeekly(LocalDateTime seriesStart, LocalDateTime periodStart, int intervalWeeks) {
        if (!periodStart.isAfter(seriesStart)) return seriesStart;
        long diffWeeks = ChronoUnit.WEEKS.between(
                startOfWeek(seriesStart.toLocalDate()),
                startOfWeek(periodStart.toLocalDate())
        );
        long steps = (diffWeeks / intervalWeeks) * intervalWeeks;
        LocalDateTime aligned = seriesStart.plusWeeks(steps);
        while (aligned.isBefore(periodStart)) aligned = aligned.plusWeeks(intervalWeeks);
        return aligned;
    }

    private LocalDateTime alignMonthly(LocalDateTime seriesStart, LocalDateTime periodStart, int intervalMonths) {
        if (!periodStart.isAfter(seriesStart)) return seriesStart;
        long diffMonths = ChronoUnit.MONTHS.between(
                YearMonth.from(seriesStart),
                YearMonth.from(periodStart)
        );
        long steps = (diffMonths / intervalMonths) * intervalMonths;
        LocalDateTime aligned = seriesStart.plusMonths(steps);
        while (aligned.isBefore(periodStart)) aligned = aligned.plusMonths(intervalMonths);
        return aligned;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return (a.isBefore(b)) ? a : b;
    }
}
