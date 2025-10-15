package com.planmate.planmate_backend.notification.job;

import com.planmate.planmate_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupJob implements Job {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        LocalDateTime cutoff = LocalDateTime.now().minusWeeks(3);
        int deleted = notificationRepository.deleteOldNonReadyBefore(cutoff);

        log.info("🧹 [CleanupJob] {}개의 읽은 오래된 알림을 삭제 완료 ({} 이전 데이터)", deleted, cutoff);
    }
}
