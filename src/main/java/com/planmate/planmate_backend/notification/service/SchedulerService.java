package com.planmate.planmate_backend.notification.service;

import com.planmate.planmate_backend.notification.job.SendJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final Scheduler scheduler;

    public void scheduleNotification(Long notificationId, LocalDateTime triggerTime) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(SendJob.class)
                    .withIdentity("notification_" + notificationId)
                    .usingJobData("notificationId", notificationId)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger_" + notificationId)
                    .startAt(Date.from(triggerTime.atZone(ZoneId.systemDefault()).toInstant()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("✅ 예약 알림 등록 완료: {}", notificationId);
        } catch (SchedulerException e) {
            log.error("🚨 예약 알림 등록 실패", e);
        }
    }

    public void cancelNotification(Long notificationId) {
        try {
            scheduler.deleteJob(new JobKey("notification_" + notificationId));
            log.info("🛑 예약 알림 취소 완료: {}", notificationId);
        } catch (SchedulerException e) {
            log.error("🚨 예약 알림 취소 실패", e);
        }
    }
}
