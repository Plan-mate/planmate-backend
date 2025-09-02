package com.planmate.planmate_backend.event.service;

import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.event.dto.CreateEventDto;
import com.planmate.planmate_backend.event.dto.CreateRecurrenceRuleDto;
import com.planmate.planmate_backend.event.dto.ResEventDto;
import com.planmate.planmate_backend.event.entity.RecurrenceRule;
import com.planmate.planmate_backend.event.mapper.EventMapper;
import com.planmate.planmate_backend.event.repository.EventRepository;
import com.planmate.planmate_backend.event.repository.RecurrenceRuleRepository;
import com.planmate.planmate_backend.event.repository.CategoryRepository;
import com.planmate.planmate_backend.event.entity.Category;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.user.entity.User;
import com.planmate.planmate_backend.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateService {

    private final EventRepository eventRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;
    private final EventMapper eventMapper;

    @Transactional
    public ResEventDto createEvent(Long userId, CreateEventDto dto) {

        User user = profileService.getUser(userId);

        Category category = categoryRepository.findById(Long.parseLong(dto.getCategoryId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."));

        Event event = Event.builder()
                .user(user)
                .category(category)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .isRecurring(dto.getIsRecurring())
                .build();

        eventRepository.save(event);

        if (Boolean.TRUE.equals(dto.getIsRecurring()) && dto.getRecurrenceRule() != null) {
            CreateRecurrenceRuleDto ruleDto = dto.getRecurrenceRule();

            RecurrenceRule rule = RecurrenceRule.builder()
                    .event(event)
                    .frequency(ruleDto.getFrequency())
                    .interval(ruleDto.getInterval() != null ? ruleDto.getInterval() : 1)
                    .daysOfWeek(ruleDto.getDaysOfWeek() != null ? String.join(",", ruleDto.getDaysOfWeek()) : null)
                    .daysOfMonth(ruleDto.getDaysOfMonth() != null ? ruleDto.getDaysOfMonth().stream()
                            .map(String::valueOf).collect(Collectors.joining(",")) : null)
                    .endDate(ruleDto.getEndDate())
                    .build();

            recurrenceRuleRepository.save(rule);

        }

        return eventMapper.toDto(event, dto.getRecurrenceRule());
    }
}
