package com.example.demo.chat.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.chat.ChatMessageEntity;
import com.example.demo.chat.MessageStatus;
import com.example.demo.chat.MessageType;

public record ChatMessageDTO(
        Integer id,
        Integer matchId,
        Integer senderId,
        String senderName,
        String content,
        LocalDateTime sentAt,
        MessageStatus status,
        MessageType messageType,
        String fileUrl,
        String fileName) {

    public static ChatMessageDTO fromEntity(ChatMessageEntity entity) {
        String name = entity.getSender().getName();
        if (entity.getSender().getSurname() != null) {
            name += " " + entity.getSender().getSurname();
        }
        return new ChatMessageDTO(
                entity.getId(),
                entity.getApartmentMatch().getId(),
                entity.getSender().getId(),
                name,
                entity.getContent(),
                entity.getSentAt(),
                entity.getStatus(),
                entity.getMessageType(),
                entity.getFileUrl(),
                entity.getFileName());
    }

    public static List<ChatMessageDTO> fromEntityList(List<ChatMessageEntity> entities) {
        return entities.stream()
                .map(ChatMessageDTO::fromEntity)
                .toList();
    }
}
