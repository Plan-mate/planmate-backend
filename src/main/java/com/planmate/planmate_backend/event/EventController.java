package com.planmate.planmate_backend.event;

import com.planmate.planmate_backend.event.dto.CategoryDto;
import com.planmate.planmate_backend.event.dto.CreateEventDto;
import com.planmate.planmate_backend.event.dto.ResEventDto;
import com.planmate.planmate_backend.event.service.CreateService;
import com.planmate.planmate_backend.event.service.GetService;
import com.planmate.planmate_backend.user.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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

    @PostMapping()
    public ResEventDto createEvent(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateEventDto dto) {
        return createService.createEvent(userId, dto);
    }



}