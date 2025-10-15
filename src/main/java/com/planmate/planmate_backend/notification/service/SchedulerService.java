package com.planmate.planmate_backend.notification.service;

import com.planmate.planmate_backend.notification.job.RecurringJob;
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

    public void scheduleNotification(Long notificationId, java.time.LocalDateTime triggerTime) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(com.planmate.planmate_backend.notification.job.SendJob.class)
                    .withIdentity("notification_" + notificationId)
                    .usingJobData("notificationId", notificationId)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger_" + notificationId)
                    .startAt(java.util.Date.from(triggerTime.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            log.error("🚨 예약 알림 등록 실패", e);
        }
    }

    public void scheduleRecurringJob(Long eventId, Long userId, String cronExpr,
                                     LocalDateTime startTime, LocalDateTime endDate) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(RecurringJob.class)
                    .withIdentity("recurring_event_" + eventId)
                    .usingJobData("eventId", eventId)
                    .usingJobData("userId", userId)
                    .build();

            TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger()
                    .withIdentity("recurring_trigger_" + eventId)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpr))
                    .startAt(Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant()));

            if (endDate != null) {
                triggerBuilder.endAt(Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant()));
            }

            scheduler.scheduleJob(jobDetail, triggerBuilder.build());
        } catch (SchedulerException e) {
            log.error("🚨 반복 알림 등록 실패 - eventId={}", eventId, e);
        }
    }

    public void cancelNotificationJob(Long notificationId) {
        try {
            JobKey jobKey = new JobKey("notification_" + notificationId);
            TriggerKey triggerKey = new TriggerKey("trigger_" + notificationId);

            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);

        } catch (SchedulerException e) {
            log.error("🚨 단일 알림 취소 실패: {}", notificationId, e);
        }
    }

    public void cancelRecurringJobByEventId(Long eventId) {
        try {
            JobKey jobKey = new JobKey("recurring_event_" + eventId);
            TriggerKey triggerKey = new TriggerKey("recurring_trigger_" + eventId);

            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.error("🚨 반복 알림 취소 실패 - eventId={}", eventId, e);
        }
    }
}
