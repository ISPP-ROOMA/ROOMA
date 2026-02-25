package com.example.demo.MemberApartment.DTOs;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import com.example.demo.MemberApartment.MemberRole;

public record CreateApartmentMember(
        @NotNull Integer userId,
        @NotNull MemberRole role,
        LocalDate joinDate
) {
}