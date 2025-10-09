package com.planmate.planmate_backend.notification.service;

import com.planmate.planmate_backend.notification.dto.NotificationDto;
import com.planmate.planmate_backend.notification.entity.Notification;
import com.planmate.planmate_backend.common.enums.Status;
import com.planmate.planmate_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SchedulerService schedulerService;

    @Transactional
    public Notification createNotification(Notification notification) {
        notification.setStatus(Status.READY);
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);
        // Quartz Job 등록
        schedulerService.scheduleNotification(saved.getId(), saved.getTriggerTime());
        return saved;
    }

    @Transactional
    public void cancelNotification(Long notificationId) {
        // Quartz Job 취소
        schedulerService.cancelNotification(notificationId);
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setStatus(Status.CANCELED);
            notificationRepository.save(n);
        });
    }

    public boolean hasUnread(Long userId) {
        return notificationRepository.existsByUserIdAndReadFalse(userId);
    }

    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByTriggerTimeDesc(userId)
                .stream()
                .map(n -> new NotificationDto(
                        n.getId(),
                        n.getTitle(),
                        n.getBody(),
                        n.isRead(),
                        n.getTriggerTime(),
                        n.getSentAt(),
                        n.getStatus()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
