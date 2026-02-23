package com.example.demo.Apartment.DTOs;

import com.example.demo.Apartment.ApartmentState;

import jakarta.validation.constraints.NotNull;

public record UpdateApartment(
        @NotNull String title,
        @NotNull String description,
        @NotNull Double price,
        String bills,
        @NotNull String ubication,
        @NotNull ApartmentState state
) {
}
