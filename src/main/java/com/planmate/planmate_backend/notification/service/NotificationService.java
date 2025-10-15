package com.planmate.planmate_backend.notification.service;

import com.planmate.planmate_backend.common.config.AppProperties;
import com.planmate.planmate_backend.common.enums.Status;
import com.planmate.planmate_backend.event.dto.RecurrenceRuleDto;
import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.notification.dto.NotificationDto;
import com.planmate.planmate_backend.notification.entity.Notification;
import com.planmate.planmate_backend.notification.repository.NotificationRepository;
import com.planmate.planmate_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final AppProperties appProperties;
    private final NotificationRepository notificationRepository;
    private final SchedulerService schedulerService;
    private final RecurringService recurringService;

    private String getDeepLink() {
        return appProperties.getNotification().getDeepLink();
    }

    @Transactional
    public void createEventNotification(User user, Event event, RecurrenceRuleDto ruleDto) {
        createSingleNotification(user, event, 30);
        if (ruleDto != null && Boolean.TRUE.equals(event.getIsRecurring())) {
            recurringService.registerRecurringNotification(user, event, ruleDto);
        }
    }

    @Transactional
    public void createSingleNotification(User user, Event event, long minutesBefore) {
        LocalDateTime startTime = event.getStartTime();
        LocalDateTime triggerTime = startTime.minusMinutes(minutesBefore);
        if (!triggerTime.isAfter(LocalDateTime.now())) return;

        Notification notification = Notification.builder()
                .user(user)
                .eventId(event.getId())
                .title("오늘의 일정 알림")
                .body(minutesBefore + "분 후 '" + event.getTitle() + "' 가 시작됩니다. 준비해주세요!")
                .deepLink(getDeepLink())
                .triggerTime(triggerTime)
                .status(Status.READY)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        schedulerService.scheduleNotification(saved.getId(), saved.getTriggerTime());
    }

    @Transactional
    public void cancelNotification(Long eventId, Long userId) {
        schedulerService.cancelRecurringJobByEventId(eventId);

        List<Notification> list =
                notificationRepository.findByEventIdAndUserIdAndStatus(eventId, userId, Status.READY);

        for (Notification n : list) {
            schedulerService.cancelNotificationJob(n.getId());
            n.setStatus(Status.CANCELED);
        }
        if (!list.isEmpty()) notificationRepository.saveAll(list);
    }


    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdAndStatusOrderBySentAtDesc(userId, Status.SENT)
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
        notificationRepository.markAllAsReadByUserIdAndStatus(userId, Status.SENT);
    }

    public boolean hasUnread(Long userId) {
        return notificationRepository.existsByUserIdAndStatusAndReadFalse(userId, Status.SENT);
    }
}
