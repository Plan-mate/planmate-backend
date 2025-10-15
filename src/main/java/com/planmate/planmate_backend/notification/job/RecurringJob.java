package com.planmate.planmate_backend.notification.job;

import com.planmate.planmate_backend.event.entity.Event;
import com.planmate.planmate_backend.event.service.GetService;
import com.planmate.planmate_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringJob implements Job {

    private final GetService getService;
    private final NotificationService notificationService;

    @Override
    public void execute(JobExecutionContext context) {
        Long eventId = context.getMergedJobDataMap().getLong("eventId");

        try {
            boolean isException = getService.isExceptionDate(eventId, LocalDate.now());
            if (isException) { return; }

            Optional<Event> eventOpt = getService.getEventById(eventId);
            if (eventOpt.isEmpty()) { return; }

            Event event = eventOpt.get();
            notificationService.createSingleNotification(event.getUser(), event, 30);
        } catch (Exception e) {
            log.error("❌ [RecurringJob] 실행 중 오류 - eventId={}", eventId, e);
        }
    }
}
