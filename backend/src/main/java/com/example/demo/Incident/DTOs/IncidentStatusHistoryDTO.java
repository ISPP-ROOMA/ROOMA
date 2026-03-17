package com.example.demo.Incident.DTOs;

import java.time.LocalDateTime;

import com.example.demo.Incident.IncidentStatusHistoryEntity;

public record IncidentStatusHistoryDTO(
        String status,
        LocalDateTime changedAt,
        Integer userId,
        String userEmail
) {
    public static IncidentStatusHistoryDTO fromEntity(IncidentStatusHistoryEntity entity) {
        return new IncidentStatusHistoryDTO(
                entity.getStatus().name(),
                entity.getChangedAt(),
                entity.getChangedByUserId(),
                entity.getChangedByEmail()
        );
    }
}
