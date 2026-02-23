package com.example.demo.Apartment.DTOs;

import com.example.demo.Apartment.ApartmentEntity;

import jakarta.validation.constraints.NotNull;

public record CreateApartment(
                @NotNull String title,
                @NotNull String description,
                @NotNull Double price,
                String bills,
                @NotNull String ubication,
                @NotNull String state

) {

        public static ApartmentEntity fromDTO(CreateApartment createApartment) {
                return new ApartmentEntity(
                                createApartment.title(),
                                createApartment.description(),
                                createApartment.price(),
                                createApartment.bills(),
                                createApartment.ubication(),
                                createApartment.state(),
                                null);
        }
}
