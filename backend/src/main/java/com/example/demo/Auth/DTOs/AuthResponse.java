package com.example.demo.Auth.DTOs;

public record AuthResponse(
        String token,
        String role
) {
}
