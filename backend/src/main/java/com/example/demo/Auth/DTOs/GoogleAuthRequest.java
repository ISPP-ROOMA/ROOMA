package com.example.demo.Auth.DTOs;

import com.example.demo.User.Role;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String idToken,
        @NotBlank String deviceId,
        Role role
) {
}
