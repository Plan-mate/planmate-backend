package com.planmate.planmate_backend.notification.repository;

import com.planmate.planmate_backend.common.enums.Status;
import com.planmate.planmate_backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserIdAndStatusAndReadFalse(Long userId, Status status);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status <> 'READY' AND n.updatedAt < :cutoff")
    int deleteOldNonReadyBefore(LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE Notification n " +
            "SET n.read = true " +
            "WHERE n.user.id = :userId AND n.status = :status AND n.read = false")
    void markAllAsReadByUserIdAndStatus(Long userId, Status status);

    @Query("SELECT n FROM Notification n JOIN FETCH n.user WHERE n.id = :id")
    Optional<Notification> findByIdWithUser(Long id);

    List<Notification> findByUserIdAndStatusOrderBySentAtDesc(Long userId, Status status);

    List<Notification> findByEventIdAndUserIdAndStatus(Long eventId, Long userId, Status status);
}
