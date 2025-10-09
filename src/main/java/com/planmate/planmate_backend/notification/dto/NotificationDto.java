package com.planmate.planmate_backend.notification.dto;

import java.time.LocalDateTime;

import com.planmate.planmate_backend.common.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private String title;
    private String body;
    private boolean read;
    private LocalDateTime triggerTime;
    private LocalDateTime sentAt;
    private Status status;
}
