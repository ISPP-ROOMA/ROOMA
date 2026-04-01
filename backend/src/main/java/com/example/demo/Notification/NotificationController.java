package com.example.demo.Notification;

import java.time.LocalDateTime;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserService userService;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    public NotificationController(NotificationService notificationService, UserService userService, PushSubscriptionRepository pushSubscriptionRepository) {
        this.notificationService = notificationService;
        this.userService = userService;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Integer> getUnreadNotificationCount() {
        UserEntity user = userService.findCurrentUserEntity();
        return ResponseEntity.ok((int) notificationService.countUnreadNotifications(user));
    }

    @PatchMapping("{id}/mark-as-read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Integer id) {
        UserEntity user = userService.findCurrentUserEntity();
        notificationService.markAsRead(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        UserEntity user = userService.findCurrentUserEntity();
        notificationService.markAllAsRead(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{id}")
    public ResponseEntity<NotificationDTO> getNotificationById(@PathVariable Integer id) {
        UserEntity user = userService.findCurrentUserEntity();
        NotificationEntity notification = notificationService.getNotificationById(id, user);
        return ResponseEntity.ok(new NotificationDTO(notification));
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<NotificationDTO>> searchNotifications(
        @RequestParam(required = false) Boolean isRead,
        @RequestParam(required = false) EventType eventType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @ParameterObject Pageable pageable
    ) {
        UserEntity user = userService.findCurrentUserEntity();
        return ResponseEntity.ok(notificationService.searchNotifications(user, isRead, eventType, startDate, endDate, pageable).map(NotificationDTO::new));
    }

    @PostMapping
    public ResponseEntity<NotificationDTO> createNotification(
        @RequestParam EventType eventType,
        @RequestParam String description,
        @RequestParam String link
    ) {
        UserEntity user = userService.findCurrentUserEntity();
        NotificationEntity notification = notificationService.createNotification(eventType, description, link, user);
        return ResponseEntity.ok(new NotificationDTO(notification));
    }
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionRequestDTO request) {
        UserEntity user = userService.findCurrentUserEntity();
        logger.info("Solicitud de suscripción push para usuario: {} (ID: {})", user.getEmail(), user.getId());
        
        if (!pushSubscriptionRepository.existsByEndpoint(request.getEndpoint())) {
            PushSubscriptionEntity subscription = new PushSubscriptionEntity(
                request.getEndpoint(),
                request.getKeys().getP256dh(),
                request.getKeys().getAuth(),
                user
            );
            pushSubscriptionRepository.save(subscription);
            logger.info("Nueva suscripción push guardada para endpoint: {}", request.getEndpoint());
        } else {
            logger.info("La suscripción push ya existía para este endpoint.");
        }
        
        return ResponseEntity.ok().build();
    }
}
