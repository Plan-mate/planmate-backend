package com.planmate.planmate_backend.notification.service;

import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringService {

    private final SchedulerService schedulerService;

    @Transactional
    public void registerRecurringNotification(User user, Event event, RecurrenceRuleDto ruleDto) {
        if (ruleDto == null)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "반복 규칙이 존재하지 않습니다.");
        if (event == null || event.getStartTime() == null)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이벤트 시작 시간이 유효하지 않습니다.");

        String cronExpr = convertRuleToCron(ruleDto, event.getStartTime());
        LocalDateTime startTime = event.getStartTime();
        LocalDateTime endDate = ruleDto.getEndDate();

        schedulerService.scheduleRecurringJob(event.getId(), user.getId(), cronExpr, startTime, endDate);
    }

    private String convertRuleToCron(RecurrenceRuleDto rule, LocalDateTime startTime) {
        LocalDateTime notifyTime = startTime.minusMinutes(30);

        int minute = notifyTime.getMinute();
        int hour = notifyTime.getHour();
        int interval = (rule.getInterval() == null || rule.getInterval() < 1) ? 1 : rule.getInterval();

        return switch (rule.getFrequency()) {
            case DAILY -> String.format("0 %d %d */%d * ?", minute, hour, interval);

            case WEEKLY -> {
                String days = joinAsCronDays(normalizeDaysOfWeek(rule.getDaysOfWeek()));
                yield String.format("0 %d %d ? * %s", minute, hour, days);
            }

            case MONTHLY -> {
                String days = joinAsCsv(normalizeDaysOfMonth(rule.getDaysOfMonth()));
                yield String.format("0 %d %d %s * ?", minute, hour, days);
            }

            default -> throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "지원하지 않는 반복 주기입니다: " + rule.getFrequency());
        };
    }

    private List<Integer> normalizeDaysOfWeek(List<?> raw) {
        List<Integer> parsed = parseToIntegerList(raw, "요일", 0, 6);
        return parsed.isEmpty() ? List.of(1) : parsed;
    }

    private List<Integer> normalizeDaysOfMonth(List<?> raw) {
        List<Integer> parsed = parseToIntegerList(raw, "일(day)", 1, 31);
        return parsed.isEmpty() ? List.of(1) : parsed;
    }

    private List<Integer> parseToIntegerList(List<?> raw, String label, int min, int max) {
        if (raw == null || raw.isEmpty()) return List.of();

        try {
            return raw.stream()
                    .map(v -> {
                        if (v instanceof Integer i) return i;
                        if (v instanceof String s) return Integer.parseInt(s);
                        throw new BusinessException(HttpStatus.BAD_REQUEST, label + " 값 타입이 잘못되었습니다: " + v);
                    })
                    .peek(i -> {
                        if (i < min || i > max) {
                            throw new BusinessException(HttpStatus.BAD_REQUEST,
                                    String.format("%s 값 범위(%d~%d) 초과: %d", label, min, max, i));
                        }
                    })
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    label + " 값 파싱 오류: " + e.getMessage());
        }
    }

    private String joinAsCronDays(List<Integer> days) {
        if (days == null || days.isEmpty()) return "MON";
        return days.stream()
                .map(this::convertDayToCron)
                .collect(Collectors.joining(","));
    }

    private String joinAsCsv(Collection<Integer> values) {
        return values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String convertDayToCron(int day) {
        return switch (day) {
            case 0 -> "SUN";
            case 1 -> "MON";
            case 2 -> "TUE";
            case 3 -> "WED";
            case 4 -> "THU";
            case 5 -> "FRI";
            case 6 -> "SAT";
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "지원하지 않는 요일 값: " + day);
        };
    }
}
