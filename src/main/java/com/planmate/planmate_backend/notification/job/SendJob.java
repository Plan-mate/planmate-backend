package com.planmate.planmate_backend.notification.job;

import com.planmate.planmate_backend.notification.entity.Notification;
import com.planmate.planmate_backend.common.enums.Status;
import com.planmate.planmate_backend.notification.repository.NotificationRepository;
import com.planmate.planmate_backend.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendJob implements Job {

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;

    @Override
    public void execute(JobExecutionContext context) {
        Long notificationId = context.getJobDetail().getJobDataMap().getLong("notificationId");

        notificationRepository.findByIdWithUser(notificationId)
                .ifPresentOrElse(
                        this::processNotification,
                        () -> log.warn("알림(ID={})을(를) 찾을 수 없습니다.", notificationId)
                );
    }

    private void processNotification(Notification notification) {
        if (notification.getStatus() != Status.READY) { return; }

        try {
            String token = notification.getUser().getFcmToken();
            if (token == null || token.isBlank()) {
                markAsFailed(notification);
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("_link", notification.getDeepLink());

            fcmService.sendToToken(token, notification.getTitle(), notification.getBody(), data);
            markAsSent(notification);
        } catch (Exception e) {
            log.error("알림(ID={}) 전송 중 오류 발생: {}", notification.getId(), e.getMessage(), e);
            markAsFailed(notification);
        }
    }

    private void markAsSent(Notification notification) {
        notification.setStatus(Status.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private void markAsFailed(Notification notification) {
        notification.setStatus(Status.FAILED);
        notificationRepository.save(notification);
    }
}
