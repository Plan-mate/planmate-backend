package com.planmate.planmate_backend.event.service;

import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.event.dto.EventReqDto;
import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import com.planmate.planmate_backend.event.dto.EventResDto;
import com.planmate.planmate_backend.event.dto.EventUpdReqDto;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.event.entity.RecurrenceException;
import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import com.planmate.planmate_backend.event.mapper.EventMapper;
import com.planmate.planmate_backend.event.mapper.RecurrenceRuleMapper;
import com.planmate.planmate_backend.event.repository.CategoryRepository;
import com.planmate.planmate_backend.event.repository.EventRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceExceptionRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceRuleRepository;
import com.planmate.planmate_backend.common.enums.Scope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final RecurrenceRuleMapper recurrenceRuleMapper;
    private final GetService getService;

    @Transactional
    public List<EventResDto> updateEvent(Long userId, Long eventId, EventUpdReqDto dto) {
        Event originalEvent = eventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));

        Event updated;
        Scope scope = dto.getScope();
        switch (scope) {
            case SINGLE -> updated = handleSingleUpdate(originalEvent, dto.getEvent());
            case ALL -> updated = handleAllUpdate(originalEvent, dto.getEvent());
            case THIS -> updated = handleThisInstanceUpdate(originalEvent, dto.getEvent());
            case THIS_AND_FUTURE -> updated = handleThisAndFutureUpdate(originalEvent, dto.getEvent());
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "지원하지 않는 스코프입니다.");
        }

        return buildResultList(updated, dto.getEvent().getRecurrenceRule());
    }

    private Event handleSingleUpdate(Event event, EventReqDto dto) {
        setCategoryIfPresent(event, dto);
        eventMapper.updateEventFromDto(event, dto);
        handleRecurrence(event, dto, Scope.SINGLE);
        return eventRepository.save(event);
    }

    private Event handleAllUpdate(Event event, EventReqDto dto) {
        recurrenceExceptionRepository.deleteByEventId(event.getId());
        setCategoryIfPresent(event, dto);

        dto.setStartTime(null);
        dto.setEndTime(null);

        eventMapper.updateEventFromDto(event, dto);
        handleRecurrence(event, dto, Scope.ALL);
        return eventRepository.save(event);
    }

    private Event handleThisInstanceUpdate(Event originalEvent, EventReqDto dto) {
        LocalDate instanceDate = dto.getStartTime().toLocalDate();
        Event override = eventMapper.createOverrideEvent(originalEvent, dto);

        setCategoryIfPresent(override, dto);

        Event savedOverride = saveAndHandleRecurrence(override, dto, Scope.THIS);

        RecurrenceException ex = RecurrenceException.builder()
                .event(originalEvent)
                .exceptionDate(instanceDate)
                .overrideEvent(savedOverride)
                .build();
        recurrenceExceptionRepository.save(ex);

        return savedOverride;
    }

    private Event handleThisAndFutureUpdate(Event originalEvent, EventReqDto dto) {
        LocalDate instanceDate = dto.getStartTime().toLocalDate();

        RecurrenceRule oldRule = recurrenceRuleRepository.findByEventId(originalEvent.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "반복 규칙을 찾지 못했습니다."));

        oldRule.setEndDate(instanceDate.atStartOfDay().minusSeconds(1));
        recurrenceRuleRepository.save(oldRule);

        Event next = eventMapper.createOverrideEvent(originalEvent, dto);
        setCategoryIfPresent(next, dto);

        return saveAndHandleRecurrence(next, dto, Scope.THIS_AND_FUTURE);
    }

    private void setCategoryIfPresent(Event event, EventReqDto dto) {
        if (dto.getCategoryId() != null) {
            Long categoryId = Long.parseLong(dto.getCategoryId());
            categoryRepository.findById(categoryId).ifPresent(event::setCategory);
        }
    }

    private Event saveAndHandleRecurrence(Event event, EventReqDto dto, Scope scope) {
        Event saved = eventRepository.save(event);
        handleRecurrence(saved, dto, scope);
        return saved;
    }

    private void handleRecurrence(Event event, EventReqDto dto, Scope scope) {
        if (Boolean.TRUE.equals(dto.getIsRecurring()) && dto.getRecurrenceRule() != null) {
            event.setIsRecurring(true);
            createOrUpdateRecurrenceRule(event, dto.getRecurrenceRule());
        } else if (scope == Scope.ALL && Boolean.FALSE.equals(dto.getIsRecurring())) {
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

        if (ruleDto == null && Boolean.TRUE.equals(event.getIsRecurring())) {
            ruleDto = recurrenceRuleRepository.findByEventId(event.getId())
                    .map(recurrenceRuleMapper::convertRuleToDto)
                    .orElse(null);
        }

        result.add(eventMapper.toDto(event, ruleDto));

        if (Boolean.TRUE.equals(event.getIsRecurring())) {
            RecurrenceRuleDto finalRuleDto = ruleDto;
            recurrenceRuleRepository.findByEventId(event.getId()).ifPresent(savedRule -> {
                List<Event> instances = getService.generateRecurringInstances(
                        event,
                        savedRule,
                        event.getStartTime(),
                        finalRuleDto != null && finalRuleDto.getEndDate() != null
                                ? finalRuleDto.getEndDate()
                                : event.getEndTime()
                );

                instances.stream()
                        .filter(inst -> !inst.getStartTime().toLocalDate().equals(event.getStartTime().toLocalDate()))
                        .forEach(inst -> result.add(eventMapper.toDto(inst, finalRuleDto)));
            });
        }

        return result;
    }
}
