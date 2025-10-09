package com.planmate.planmate_backend.notification;

import com.planmate.planmate_backend.notification.dto.NotificationDto;
import com.planmate.planmate_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/has-unread")
    public boolean hasUnread(@AuthenticationPrincipal Long userId) {
        return notificationService.hasUnread(userId);
    }

    @GetMapping
    public List<NotificationDto> getMyNotifications(@AuthenticationPrincipal Long userId) {
        return notificationService.getUserNotifications(userId);
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);
    }
}
