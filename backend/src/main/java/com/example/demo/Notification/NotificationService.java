package com.example.demo.Notification;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.UserEntity;


@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> searchNotifications(UserEntity user, Boolean isRead, EventType eventType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return notificationRepository.searchNotifications(user.getId(), isRead, eventType, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(UserEntity user) {
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional(readOnly = true)
    public NotificationEntity getNotificationById(Integer notificationId, UserEntity user) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized, you do not have permission to access this notification");
        }

        return notification;
    }

    @Transactional
    public void saveNotification(NotificationEntity notification) {
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(Integer notificationId, UserEntity user) {
        NotificationEntity notification = getNotificationById(notificationId, user);
        if(notification.getIsRead()) {
            return;
        }
        notification.setIsRead(true);
        saveNotification(notification);
    }

    @Transactional
    public void markAllAsRead(UserEntity user) {
        Page<NotificationEntity> unreadNotifications = searchNotifications(user, false, null, null, null, Pageable.unpaged());
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            saveNotification(notification);
        });
    }

    @Transactional
    public NotificationEntity createNotification(EventType eventType, String description, String link, UserEntity user) {
        NotificationEntity notification = new NotificationEntity();
        notification.setEventType(eventType);
        notification.setDescription(description);
        notification.setLink(link);
        notification.setIsRead(false);
        notification.setTimestamp(LocalDateTime.now());
        notification.setUser(user);
        return notificationRepository.save(notification);
    }

}
