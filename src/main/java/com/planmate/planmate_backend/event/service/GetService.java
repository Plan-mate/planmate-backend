package com.planmate.planmate_backend.event.service;

import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.event.dto.EventResDto;
import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import com.planmate.planmate_backend.event.dto.CategoryDto;
import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import com.planmate.planmate_backend.event.entity.RecurrenceException;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.event.mapper.CategoryMapper;
import com.planmate.planmate_backend.event.mapper.EventMapper;
import com.planmate.planmate_backend.event.repository.CategoryRepository;
import com.planmate.planmate_backend.event.repository.EventRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceRuleRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceExceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;

    private final EventMapper eventMapper;
    private final CategoryMapper categoryMapper;

    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public List<EventResDto> getEvents(Long userId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        List<Event> events = eventRepository.findByUserAndPeriod(userId, startDateTime, endDateTime);
        List<RecurrenceRule> rules = recurrenceRuleRepository.findRecurringEventsEndingInPeriod(userId, startDateTime, endDateTime);

        Map<Long, RecurrenceRuleDto> ruleMap = rules.stream()
                .collect(Collectors.toMap(
                        r -> r.getEvent().getId(),
                        r -> new RecurrenceRuleDto(
                                r.getDaysOfMonth() != null
                                        ? Arrays.stream(r.getDaysOfMonth().split(",")).map(Integer::parseInt).toList()
                                        : null,
                                r.getDaysOfWeek() != null
                                        ? Arrays.asList(r.getDaysOfWeek().split(","))
                                        : null,
                                r.getFrequency(),
                                r.getInterval(),
                                r.getEndDate()
                        )
                ));

        List<EventResDto> result = new ArrayList<>();
        List<Event> originalEvents = new ArrayList<>(events);

        for (Event e : events) {
            RecurrenceRuleDto ruleDto = e.getIsRecurring() ? ruleMap.get(e.getId()) : null;
            result.add(eventMapper.toDto(e, ruleDto));
        }

        for (RecurrenceRule rule : rules) {
            Event baseEvent = rule.getEvent();
            RecurrenceRuleDto ruleDto = ruleMap.get(baseEvent.getId());
            List<RecurrenceException> exceptions = recurrenceExceptionRepository.findByEventId(baseEvent.getId());

            List<Event> instances = generateRecurringInstancesWithExceptions(baseEvent, rule, startDateTime, endDateTime, exceptions);

            for (Event inst : instances) {
                boolean overlapsOriginal = originalEvents.stream().anyMatch(orig -> isOverlap(orig, inst));
                if (!overlapsOriginal) result.add(eventMapper.toDto(inst, ruleDto));
            }
        }

        return result;
    }

    private List<Event> generateRecurringInstancesWithExceptions(Event baseEvent,
                                                                 RecurrenceRule rule,
                                                                 LocalDateTime periodStart,
                                                                 LocalDateTime periodEnd,
                                                                 List<RecurrenceException> exceptions) {

        List<Event> instances = generateRecurringInstances(baseEvent, rule, periodStart, periodEnd);

        Set<LocalDate> skipDates = exceptions.stream()
                .map(RecurrenceException::getExceptionDate)
                .collect(Collectors.toSet());

        Map<LocalDate, Event> overrides = exceptions.stream()
                .filter(e -> e.getOverrideEvent() != null)
                .collect(Collectors.toMap(RecurrenceException::getExceptionDate, RecurrenceException::getOverrideEvent));

        return instances.stream()
                .filter(inst -> !skipDates.contains(inst.getStartTime().toLocalDate()))
                .map(inst -> overrides.getOrDefault(inst.getStartTime().toLocalDate(), inst))
                .toList();
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
                .map(s -> DayOfWeek.of((Integer.parseInt(s) == 0 ? 7 : Integer.parseInt(s))))
                .toList()
                : List.of();

        List<Integer> domList = rule.getDaysOfMonth() != null
                ? Arrays.stream(rule.getDaysOfMonth().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
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
                    for (LocalDate weekStart = firstWeek; !weekStart.atStartOfDay().isAfter(scanEnd); weekStart = weekStart.plusWeeks(rule.getInterval())) {
                        for (DayOfWeek dow : dowList) {
                            LocalDateTime cur = weekStart.atTime(seriesStart.toLocalTime()).with(TemporalAdjusters.nextOrSame(dow));
                            if (cur.isBefore(seriesStart) || cur.isAfter(scanEnd)) continue;
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
                            if (cur.isBefore(seriesStart) || cur.isAfter(scanEnd)) continue;
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
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "지원하지 않는 반복 주기입니다.");
        }

        return out;
    }

    private void addIfOverlap(List<Event> out, LocalDateTime s, LocalDateTime e, LocalDateTime periodStart, LocalDateTime periodEnd, Event baseEvent) {
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

    private boolean isOverlap(Event e1, Event e2) {
        return !e1.getEndTime().isBefore(e2.getStartTime()) && !e1.getStartTime().isAfter(e2.getEndTime());
    }

    private LocalDate startOfWeek(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDateTime alignDaily(LocalDateTime seriesStart, LocalDateTime periodStart, int intervalDays) {
        if (!periodStart.isAfter(seriesStart)) return seriesStart;
        long diff = Duration.between(seriesStart.toLocalDate().atStartOfDay(), periodStart.toLocalDate().atStartOfDay()).toDays();
        long steps = (diff / intervalDays) * intervalDays;
        LocalDateTime aligned = seriesStart.plusDays(steps);
        while (aligned.isBefore(periodStart)) aligned = aligned.plusDays(intervalDays);
        return aligned;
    }

    private LocalDateTime alignWeekly(LocalDateTime seriesStart, LocalDateTime periodStart, int intervalWeeks) {
        if (!periodStart.isAfter(seriesStart)) return seriesStart;
        long diffWeeks = ChronoUnit.WEEKS.between(startOfWeek(seriesStart.toLocalDate()), startOfWeek(periodStart.toLocalDate()));
        long steps = (diffWeeks / intervalWeeks) * intervalWeeks;
        LocalDateTime aligned = seriesStart.plusWeeks(steps);
        while (aligned.isBefore(periodStart)) aligned = aligned.plusWeeks(intervalWeeks);
        return aligned;
    }

    private LocalDateTime alignMonthly(LocalDateTime seriesStart, LocalDateTime periodStart, int intervalMonths) {
        if (!periodStart.isAfter(seriesStart)) return seriesStart;
        long diffMonths = ChronoUnit.MONTHS.between(YearMonth.from(seriesStart), YearMonth.from(periodStart));
        long steps = (diffMonths / intervalMonths) * intervalMonths;
        LocalDateTime aligned = seriesStart.plusMonths(steps);
        while (aligned.isBefore(periodStart)) aligned = aligned.plusMonths(intervalMonths);
        return aligned;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
