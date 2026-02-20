package com.example.demo.Auth.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank @Email         String email,
        @NotBlank @Size(min = 4) String password,
        @NotBlank                String deviceId
) {
}
