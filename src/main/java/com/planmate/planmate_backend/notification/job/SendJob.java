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
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendJob implements Job {

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;

    @Override
    public void execute(JobExecutionContext context) {
        Long notificationId = context.getJobDetail().getJobDataMap().getLong("notificationId");
        log.info("🔔 [SendJob] 실행 - notificationId: {}", notificationId);

        Optional<Notification> optional = notificationRepository.findById(notificationId);
        if (optional.isEmpty()) {
            log.warn("❌ [SendJob] 알림을 찾을 수 없음 (id: {})", notificationId);
            return;
        }

        Notification notification = optional.get();

        // READY 상태만 발송
        if (notification.getStatus() != Status.READY) {
            return;
        }

        try {
            // ✅ FCM 발송 (유저 ID → FCM 토큰으로 수정)
            String targetToken = notification.getUser().getFcmToken();
            fcmService.sendToToken(targetToken, notification.getTitle(), notification.getBody(), null);

            // 상태 업데이트
            notification.setStatus(Status.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

        } catch (Exception e) {
            notification.setStatus(Status.FAILED);
            notificationRepository.save(notification);
        }
    }
}
