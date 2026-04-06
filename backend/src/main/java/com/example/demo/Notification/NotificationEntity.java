package com.example.demo.Notification;

import java.time.LocalDateTime;

import com.example.demo.User.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @SequenceGenerator(name = "notification_seq", sequenceName = "notification_seq", initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
    private Integer id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @NotNull
    @Column(nullable = false,length = 500)
    private String description;

    @NotNull
    @Column(nullable = false)
    private Boolean isRead;

    @NotBlank
    @Column(nullable = false,length = 500)
    private String link;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(nullable = false)
    private UserEntity user;

    public NotificationEntity() {
    }

    public NotificationEntity(EventType eventType, LocalDateTime timestamp, String description, Boolean isRead, String link, UserEntity user) {
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.description = description;
        this.isRead = isRead;
        this.link = link;
        this.user = user;
    }

    public Integer getId() {
        return id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public String getLink() {
        return link;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

}
