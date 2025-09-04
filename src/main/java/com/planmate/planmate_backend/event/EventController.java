package com.planmate.planmate_backend.event;

import com.planmate.planmate_backend.event.dto.CategoryDto;
import com.planmate.planmate_backend.event.dto.DateDto;
import com.planmate.planmate_backend.event.dto.EventDto;
import com.planmate.planmate_backend.event.dto.EventResDto;
import com.planmate.planmate_backend.event.service.CreateService;
import com.planmate.planmate_backend.event.service.GetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final CreateService createService;
    private final GetService getService;

    @GetMapping("/category")
    public List<CategoryDto> getCategories() {
        return getService.getCategories();
    }

    @GetMapping("/list")
    public List<EventResDto> getEvents(@AuthenticationPrincipal Long userId, @Valid DateDto dto) {
        LocalDate start = LocalDate.of(dto.getYear(), dto.getMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return getService.getEvents(userId, start, end);
    }

    @PostMapping()
    public List<EventResDto> createEvent(@AuthenticationPrincipal Long userId, @Valid @RequestBody EventDto dto) {
        return createService.createEvent(userId, dto);
    }



}