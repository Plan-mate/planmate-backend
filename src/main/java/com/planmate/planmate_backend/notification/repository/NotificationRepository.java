package com.planmate.planmate_backend.notification.repository;

import com.planmate.planmate_backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    int deleteByCreatedAtBefore(LocalDateTime cutoff);

    List<Notification> findByUserIdOrderByTriggerTimeDesc(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsReadByUserId(Long userId);

    boolean existsByUserIdAndReadFalse(Long userId);
}