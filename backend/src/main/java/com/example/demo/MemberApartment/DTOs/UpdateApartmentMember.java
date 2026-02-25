package com.example.demo.MemberApartment.DTOs;

import com.example.demo.MemberApartment.MemberRole;

import jakarta.validation.constraints.NotNull;

public record UpdateApartmentMember(
        @NotNull MemberRole role
) {
}