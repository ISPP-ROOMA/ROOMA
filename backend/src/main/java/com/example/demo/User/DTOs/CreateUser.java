package com.example.demo.User.DTOs;

import com.example.demo.User.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUser(
        @NotNull @Email         String email,
        @NotNull @Size(min = 4) String password,
        @NotNull                Role   role
) {
}
