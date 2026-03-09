package com.example.demo.User.DTOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateProfileRequest(
        String name,
        String surname,
        @Email String email,
        @Size(min = 4) String password,
        String birthDate,
        String phone,
        String profilePic,
        String gender,
        Boolean smoker,
        String hobbies,
        String schedule,
        String profession
) {
}
