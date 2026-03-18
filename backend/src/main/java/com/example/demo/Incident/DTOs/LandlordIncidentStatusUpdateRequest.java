package com.example.demo.Incident.DTOs;

import com.example.demo.Incident.IncidentStatus;

import jakarta.validation.constraints.NotNull;

public record LandlordIncidentStatusUpdateRequest(@NotNull IncidentStatus status) {
}
