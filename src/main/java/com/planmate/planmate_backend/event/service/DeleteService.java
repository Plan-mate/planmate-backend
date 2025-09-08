package com.planmate.planmate_backend.event.service;

import com.planmate.planmate_backend.common.enums.Scope;
import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.event.dto.EventDelReqDto;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.event.entity.RecurrenceException;
import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import com.planmate.planmate_backend.event.repository.EventRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceExceptionRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeleteService {

    private final EventRepository eventRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;

    @Transactional
    public void deleteEvent(Long userId, Long eventId, EventDelReqDto dto) {
        Event originalEvent = eventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));

        Scope scope = dto.getScope();
        switch (scope) {
            case SINGLE -> handleSingleDelete(originalEvent);
            case ALL -> handleAllDelete(originalEvent);
            case THIS -> handleThisInstanceDelete(originalEvent, dto.getTargetTime());
            case THIS_AND_FUTURE -> handleThisAndFutureDelete(originalEvent, dto.getTargetTime());
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "지원하지 않는 스코프입니다.");
        }
    }

    private void handleSingleDelete(Event event) {
        if (Boolean.TRUE.equals(event.getIsRecurring())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "반복 일정은 SINGLE 삭제가 불가능합니다.");
        }

        List<RecurrenceException> exceptions = recurrenceExceptionRepository.findByOverrideEventId(event.getId());
        exceptions.forEach(ex -> ex.setOverrideEvent(null));
        recurrenceExceptionRepository.saveAll(exceptions);

        eventRepository.delete(event);
    }

    private void handleAllDelete(Event event) {
        List<Event> overrides = eventRepository.findByOriginalEventId(event.getId());
        overrides.forEach(override -> override.setOriginalEventId(null));
        eventRepository.saveAll(overrides);

        List<RecurrenceException> exceptions = recurrenceExceptionRepository.findByOverrideEventId(event.getId());
        exceptions.forEach(ex -> ex.setOverrideEvent(null));
        recurrenceExceptionRepository.saveAll(exceptions);

        recurrenceRuleRepository.deleteByEventId(event.getId());
        recurrenceExceptionRepository.deleteByEventId(event.getId());

        eventRepository.delete(event);
    }

    private void handleThisInstanceDelete(Event event, LocalDateTime targetTime) {
        if (targetTime == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "삭제할 반복 인스턴스의 날짜가 필요합니다.");
        }

        LocalDate instanceDate = targetTime.toLocalDate();

        RecurrenceException exception = RecurrenceException.builder()
                .event(event)
                .exceptionDate(instanceDate)
                .build();

        recurrenceExceptionRepository.save(exception);
    }

    private void handleThisAndFutureDelete(Event event, LocalDateTime targetTime) {
        if (targetTime == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "삭제할 시작 날짜가 필요합니다.");
        }

        LocalDate instanceDate = targetTime.toLocalDate();

        RecurrenceRule rule = recurrenceRuleRepository.findByEventId(event.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "반복 규칙을 찾지 못했습니다."));
        rule.setEndDate(instanceDate.atStartOfDay().minusSeconds(1));
        recurrenceRuleRepository.save(rule);

        List<Event> overrides = eventRepository.findByOriginalEventId(event.getId());
        overrides.forEach(override -> override.setOriginalEventId(null));
        eventRepository.saveAll(overrides);

        List<RecurrenceException> exceptions = recurrenceExceptionRepository.findByOverrideEventId(event.getId());
        exceptions.forEach(ex -> ex.setOverrideEvent(null));
        recurrenceExceptionRepository.saveAll(exceptions);
    }
}
