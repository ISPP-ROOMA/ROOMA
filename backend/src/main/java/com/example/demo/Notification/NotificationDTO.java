package com.example.demo.Notification;

import java.time.LocalDateTime;

public class NotificationDTO {

    private Integer id;
    private EventType eventType;
    private LocalDateTime timestamp;
    private String description;
    private Boolean isRead;
    private String link;
    private Integer userId;

    public NotificationDTO() {
    }

    public NotificationDTO(NotificationEntity entity) {
        this.id = entity.getId();
        this.eventType = entity.getEventType();
        this.timestamp = entity.getTimestamp();
        this.description = entity.getDescription();
        this.isRead = entity.getIsRead();
        this.link = entity.getLink();
        if (entity.getUser() != null) {
            this.userId = entity.getUser().getId();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
