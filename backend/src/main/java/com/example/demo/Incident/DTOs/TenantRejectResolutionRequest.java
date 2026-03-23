package com.example.demo.Incident.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantRejectResolutionRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
