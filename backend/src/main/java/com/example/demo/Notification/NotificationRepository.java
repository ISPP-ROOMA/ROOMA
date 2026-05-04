package com.example.demo.Notification;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Integer> {

    @Query("""
        SELECT n FROM NotificationEntity n
        WHERE n.user.id = :userId
        AND (:isRead IS NULL OR n.isRead = :isRead)
        AND (:eventType IS NULL OR n.eventType = :eventType)
        AND (n.timestamp >= COALESCE(:startDate, n.timestamp))
        AND (n.timestamp <= COALESCE(:endDate, n.timestamp))
        ORDER BY n.timestamp DESC
    """)
    Page<NotificationEntity> searchNotifications(
        @Param("userId") Integer userId,
        @Param("isRead") Boolean isRead,
        @Param("eventType") EventType eventType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    long countByUserIdAndIsReadFalse(Integer userId);
}
