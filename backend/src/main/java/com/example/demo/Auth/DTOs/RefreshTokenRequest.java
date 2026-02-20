package com.example.demo.Auth.DTOs;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(@NotNull String deviceId) {
}
