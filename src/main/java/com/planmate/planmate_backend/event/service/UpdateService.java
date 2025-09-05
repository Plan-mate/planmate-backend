package com.planmate.planmate_backend.event.service;

import com.planmate.planmate_backend.event.dto.EventReqDto;
import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import com.planmate.planmate_backend.event.dto.EventResDto;
import com.planmate.planmate_backend.event.dto.EventUpdReqDto;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.event.entity.RecurrenceException;
import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import com.planmate.planmate_backend.event.mapper.EventMapper;
import com.planmate.planmate_backend.event.repository.CategoryRepository;
import com.planmate.planmate_backend.event.repository.EventRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceExceptionRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceRuleRepository;
import com.planmate.planmate_backend.common.enums.Scope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class UpdateService {

    private final EventRepository eventRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final GetService getService;

    @Transactional
    public List<EventResDto> updateEvent(Long userId, Long eventId, EventUpdReqDto dto) {
        Event originalEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        Event updated;
        Scope scope = dto.getScope();

        // 단일 이벤트
        if (!Boolean.TRUE.equals(originalEvent.getIsRecurring())) {
            if (scope != Scope.SINGLE) {
                throw new IllegalArgumentException("Non-recurring events must use SINGLE scope");
            }
            updated = handleSingleUpdate(originalEvent, dto.getEvent());
        } else { // 반복 이벤트
            updated = switch (scope) {
                case ALL -> handleAllUpdate(originalEvent, dto.getEvent());
                case THIS -> handleThisInstanceUpdate(originalEvent, dto.getEvent());
                case THIS_AND_FUTURE -> handleThisAndFutureUpdate(originalEvent, dto.getEvent());
                default -> throw new IllegalArgumentException("Unsupported scope: " + scope);
            };
        }

        return buildResultList(updated, dto.getEvent().getRecurrenceRule());
    }

    /* 단일 이벤트 수정 */
    private Event handleSingleUpdate(Event event, EventReqDto dto) {
        if (dto.getCategoryId() != null) {
            Long categoryId = Long.parseLong(dto.getCategoryId());
            categoryRepository.findById(categoryId).ifPresent(event::setCategory);
        }

        eventMapper.updateEventFromDto(event, dto);
        handleRecurrence(event, dto);

        return eventRepository.save(event);
    }


    /* 전체 반복 이벤트 수정 */
    private Event handleAllUpdate(Event event, EventReqDto dto) {
        eventMapper.updateEventFromDto(event, dto);
        handleRecurrence(event, dto);
        return eventRepository.save(event);
    }

    /* 특정 인스턴스만 수정 */
    private Event handleThisInstanceUpdate(Event originalEvent, EventReqDto dto) {
        LocalDate instanceDate = originalEvent.getStartTime().toLocalDate();

        // 기존 이벤트를 예외 처리
        RecurrenceException ex = RecurrenceException.builder()
                .event(originalEvent)
                .exceptionDate(instanceDate)
                .build();
        recurrenceExceptionRepository.save(ex);

        // Override Event 생성
        Event override = eventMapper.createOverrideEvent(originalEvent, dto);
        eventRepository.save(override);

        // 필요시 반복 규칙 처리
        handleRecurrence(override, dto);

        ex.setOverrideEvent(override);
        recurrenceExceptionRepository.save(ex);

        return override;
    }

    /* 현재 인스턴스 + 이후 이벤트 수정 */
    private Event handleThisAndFutureUpdate(Event originalEvent, EventReqDto dto) {
        LocalDate instanceDate = originalEvent.getStartTime().toLocalDate();

        // 기존 Rule 종료 날짜 조정
        RecurrenceRule oldRule = recurrenceRuleRepository.findByEventId(originalEvent.getId())
                .orElseThrow(() -> new RuntimeException("RecurrenceRule not found"));
        oldRule.setEndDate(instanceDate.atStartOfDay().minusSeconds(1));
        recurrenceRuleRepository.save(oldRule);

        // 새 이벤트 생성
        Event next = eventMapper.createOverrideEvent(originalEvent, dto);
        eventRepository.save(next);

        handleRecurrence(next, dto);

        return next;
    }

    /* 반복 여부 처리 */
    private void handleRecurrence(Event event, EventReqDto dto) {
        if (Boolean.TRUE.equals(dto.getIsRecurring()) && dto.getRecurrenceRule() != null) {
            event.setIsRecurring(true);
            createOrUpdateRecurrenceRule(event, dto.getRecurrenceRule());
        } else {
            event.setIsRecurring(false);
            recurrenceRuleRepository.deleteByEventId(event.getId());
        }
    }

    private void createOrUpdateRecurrenceRule(Event event, RecurrenceRuleDto rrDto) {
        RecurrenceRule rule = recurrenceRuleRepository.findByEventId(event.getId())
                .orElseGet(() -> RecurrenceRule.builder().event(event).build());

        rule.setFrequency(rrDto.getFrequency());
        rule.setInterval(rrDto.getInterval() == null ? 1 : rrDto.getInterval());
        rule.setDaysOfWeek(rrDto.getDaysOfWeek() != null
                ? rrDto.getDaysOfWeek().stream().map(String::valueOf).collect(Collectors.joining(","))
                : null);
        rule.setDaysOfMonth(rrDto.getDaysOfMonth() != null
                ? rrDto.getDaysOfMonth().stream().map(String::valueOf).collect(Collectors.joining(","))
                : null);
        rule.setEndDate(rrDto.getEndDate());

        recurrenceRuleRepository.save(rule);
    }

    private List<EventResDto> buildResultList(Event event, RecurrenceRuleDto ruleDto) {
        List<EventResDto> result = new ArrayList<>();
        result.add(eventMapper.toDto(event, ruleDto));

        if (Boolean.TRUE.equals(event.getIsRecurring())) {
            recurrenceRuleRepository.findByEventId(event.getId()).ifPresent(savedRule -> {
                List<Event> instances = getService.generateRecurringInstances(
                        event,
                        savedRule,
                        event.getStartTime(),
                        ruleDto != null && ruleDto.getEndDate() != null
                                ? ruleDto.getEndDate()
                                : event.getEndTime()
                );
                instances.forEach(inst -> result.add(eventMapper.toDto(inst, ruleDto)));
            });
        }
        return result;
    }
}
