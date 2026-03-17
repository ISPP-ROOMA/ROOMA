package com.example.demo.Incident.DTOs;

import com.example.demo.Incident.IncidentCategory;
import com.example.demo.Incident.IncidentUrgency;
import com.example.demo.Incident.IncidentZone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 1000) String description,
        @NotNull IncidentCategory category,
        @NotNull IncidentZone zone,
        @NotNull IncidentUrgency urgency
) {
}
