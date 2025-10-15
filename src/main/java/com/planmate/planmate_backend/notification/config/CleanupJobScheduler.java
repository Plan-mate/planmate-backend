package com.planmate.planmate_backend.notification.config;

import com.planmate.planmate_backend.notification.job.CleanupJob;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupJobScheduler {

    private final Scheduler scheduler;

    @PostConstruct
    public void scheduleCleanupJob() {
        try {
            JobKey jobKey = new JobKey("cleanupJob", "notification");
            TriggerKey triggerKey = new TriggerKey("cleanupTrigger", "notification");

            if (scheduler.checkExists(jobKey)) {
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(CleanupJob.class)
                    .withIdentity(jobKey)
                    .storeDurably()
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 3 * * ?"))
                    .forJob(jobDetail)
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            log.error("🚨 [CleanupJobScheduler] CleanupJob 스케줄 등록 실패", e);
        }
    }
}
