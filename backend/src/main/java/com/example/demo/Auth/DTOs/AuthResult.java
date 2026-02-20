package com.example.demo.Auth.DTOs;

public record AuthResult(
        String accessToken,
        String refreshToken,
        String role
) {
}
