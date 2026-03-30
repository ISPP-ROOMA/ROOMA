package com.example.demo.Notification;

import java.time.LocalDateTime;

import org.checkerframework.checker.units.qual.N;
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

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
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
    public ResponseEntity<NotificationEntity> getNotificationById(@PathVariable Integer id) {
        UserEntity user = userService.findCurrentUserEntity();
        return ResponseEntity.ok(notificationService.getNotificationById(id, user));
    }

    @GetMapping
    public ResponseEntity<?> searchNotifications(
        @RequestParam(required = false) Boolean isRead,
        @RequestParam(required = false) EventType eventType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @ParameterObject Pageable pageable
    ) {
        UserEntity user = userService.findCurrentUserEntity();
        return ResponseEntity.ok(notificationService.searchNotifications(user, isRead, eventType, startDate, endDate, pageable));
    }

    @PostMapping
    public ResponseEntity<NotificationEntity> createNotification(
        @RequestParam EventType eventType,
        @RequestParam String description,
        @RequestParam String link
    ) {
        UserEntity user = userService.findCurrentUserEntity();
        NotificationEntity notification = notificationService.createNotification(eventType, description, link, user);
        return ResponseEntity.ok(notification);
    }

}
