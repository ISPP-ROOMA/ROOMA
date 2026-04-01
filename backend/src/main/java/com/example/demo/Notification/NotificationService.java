package com.example.demo.Notification;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.UserEntity;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


@Service
public class NotificationService {

    @Value("${vapid.public-key}")
    private String publicKey;

    @Value("${vapid.private-key}")
    private String privateKey;

    @Value("${vapid.subject}")
    private String subject;

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private PushService pushService;

    public NotificationService(NotificationRepository notificationRepository, PushSubscriptionRepository pushSubscriptionRepository) {
        this.notificationRepository = notificationRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    @PostConstruct
    private void init() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
                logger.info("BouncyCastle provider registered.");
            }
            pushService = new PushService(publicKey, privateKey, subject);
            logger.info("PushService initialized successfully with VAPID.");
        } catch (Exception e) {
            logger.error("CRITICAL ERROR initializing PushService VAPID: {}", e.getMessage(), e);
        }
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
        if (notification.getIsRead()) {
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
        NotificationEntity saved = notificationRepository.save(notification);
        sendPushNotificationAsync(saved);
        return saved;
    }

    private void sendPushNotificationAsync(NotificationEntity notification) {
        if (pushService == null) {
            logger.warn("PushService is not initialized. Check VAPID keys.");
            return;
        }
        List<PushSubscriptionEntity> subscriptions = pushSubscriptionRepository.findByUserId(notification.getUser().getId());
        logger.info("Found {} push subscriptions for user {}", subscriptions.size(), notification.getUser().getId());
        for (PushSubscriptionEntity sub : subscriptions) {
            try {
                logger.info("Sending push notification to endpoint: {}", sub.getEndpoint());
                Subscription.Keys keys = new Subscription.Keys(sub.getP256dh(), sub.getAuth());
                Subscription subscription = new Subscription(sub.getEndpoint(), keys);
                // Creating a simple JSON payload manually for the push notification
                String payload = String.format("{\"title\":\"%s\", \"message\":\"%s\", \"url\":\"%s\"}",
                        notification.getEventType().name(),
                        notification.getDescription().replace("\"", "\\\""),
                        notification.getLink());
                Notification pushNotification = new Notification(subscription, payload);
                pushService.send(pushNotification);
                logger.info("Push notification sent successfully to {}", sub.getEndpoint());
            } catch (Exception e) {
                logger.error("Error sending push notification to {}: {}", sub.getEndpoint(), e.getMessage());
                // If it fails (e.g. 410 Gone), we could delete the subscription here
                if (e.getMessage() != null && e.getMessage().contains("410")) {
                    logger.info("Subscription expired (410 Gone), deleting: {}", sub.getEndpoint());
                    pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                }
            }
        }
    }

}
