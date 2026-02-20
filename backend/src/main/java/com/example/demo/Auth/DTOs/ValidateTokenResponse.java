package com.example.demo.Auth.DTOs;

public record ValidateTokenResponse(boolean authenticated, String message) {
}
