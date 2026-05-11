package com.example.demo.MemberApartment.DTOs;

import jakarta.validation.constraints.NotNull;

public record UpdateApartmentRule(
        @NotNull Boolean allowsPets,
        @NotNull Boolean allowsSmokers,
        @NotNull Boolean partiesAllowed
) {
}