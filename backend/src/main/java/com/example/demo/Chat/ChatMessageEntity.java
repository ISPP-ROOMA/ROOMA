package com.example.demo.Chat;

import java.time.LocalDateTime;

import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.Incident.IncidentEntity;
import com.example.demo.User.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    @SequenceGenerator(name = "chat_messages_seq",
            sequenceName = "chat_messages_seq",
            initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_messages_seq")
    private Integer id;

    @JoinColumn(name = "match_id")
    @ManyToOne
    private ApartmentMatchEntity apartmentMatch;

    @JoinColumn(name = "incident_id")
    @ManyToOne
    private IncidentEntity incident;

    @JoinColumn(name = "sender_id", nullable = false)
    @ManyToOne
    private UserEntity sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column
    private String fileUrl;

    @Column
    private String filePublicId;

    @Column
    private String fileName;

    public ChatMessageEntity() {
    }

    @PrePersist
    public void prePersist() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
        if (status == null) {
            status = MessageStatus.SENT;
        }
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ApartmentMatchEntity getApartmentMatch() {
        return apartmentMatch;
    }

    public void setApartmentMatch(ApartmentMatchEntity apartmentMatch) {
        this.apartmentMatch = apartmentMatch;
    }

    public IncidentEntity getIncident() {
        return incident;
    }

    public void setIncident(IncidentEntity incident) {
        this.incident = incident;
    }

    public UserEntity getSender() {
        return sender;
    }

    public void setSender(UserEntity sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFilePublicId() {
        return filePublicId;
    }

    public void setFilePublicId(String filePublicId) {
        this.filePublicId = filePublicId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
