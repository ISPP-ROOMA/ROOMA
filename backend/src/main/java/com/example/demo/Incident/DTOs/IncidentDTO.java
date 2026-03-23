package com.example.demo.Incident.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.Incident.IncidentEntity;

public record IncidentDTO(
        Integer id,
        String incidentCode,
        Integer apartmentId,
        Integer tenantId,
        String tenantEmail,
        Integer landlordId,
        String landlordEmail,
        String title,
        String description,
        String category,
        String zone,
        String urgency,
        String status,
        List<String> photos,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        List<IncidentStatusHistoryDTO> statusHistory
) {
    public static IncidentDTO fromEntity(IncidentEntity incident, List<IncidentStatusHistoryDTO> statusHistory) {
        String code = incident.getId() == null ? "INC-NEW" : String.format("INC-%04d", incident.getId());
        return new IncidentDTO(
                incident.getId(),
                code,
                incident.getApartment().getId(),
                incident.getTenant().getId(),
                incident.getTenant().getEmail(),
                incident.getLandlord().getId(),
                incident.getLandlord().getEmail(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getCategory().name(),
                incident.getZone().name(),
                incident.getUrgency().name(),
                incident.getStatus().name(),
                incident.getPhotos(),
                incident.getRejectionReason(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getResolvedAt(),
                incident.getClosedAt(),
                statusHistory
        );
    }
}
