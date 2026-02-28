package com.example.demo.MemberApartment.DTOs;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CreateApartmentMember(
        @NotNull Integer userId,
        LocalDate joinDate
) {
}