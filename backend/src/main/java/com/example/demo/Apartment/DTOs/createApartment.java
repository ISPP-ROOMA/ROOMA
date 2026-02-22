package com.example.demo.Apartment.DTOs;

import jakarta.validation.constraints.NotNull;

public record createApartment(
        @NotNull String title,
        @NotNull String description,
        @NotNull Double price,
        String bills,
        @NotNull String ubication,
        @NotNull String state
        
) {
}
