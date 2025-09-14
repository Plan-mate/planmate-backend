package com.planmate.planmate_backend.event;

import com.planmate.planmate_backend.event.dto.*;
import com.planmate.planmate_backend.event.service.CreateService;
import com.planmate.planmate_backend.event.service.DeleteService;
import com.planmate.planmate_backend.event.service.GetService;
import com.planmate.planmate_backend.event.service.UpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final CreateService createService;
    private final UpdateService updateService;
    private final DeleteService deleteService;
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
    public List<EventResDto> createEvent(@AuthenticationPrincipal Long userId, @Valid @RequestBody EventReqDto dto) {
        return createService.createEvent(userId, dto);
    }

    @PatchMapping("/{eventId}")
    public List<EventResDto> updateEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdReqDto dto
    ) {
        return updateService.updateEvent(userId, eventId, dto);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventDelReqDto dto
    ) {
        deleteService.deleteEvent(userId, eventId, dto);
        return ResponseEntity.noContent().build();
    }
}