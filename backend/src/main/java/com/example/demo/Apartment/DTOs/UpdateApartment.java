package com.example.demo.Apartment.DTOs;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;

import jakarta.validation.constraints.NotNull;

public record UpdateApartment(
                @NotNull String title,
                @NotNull String description,
                @NotNull Double price,
                String bills,
                @NotNull String ubication,
                @NotNull ApartmentState state) {

        public static ApartmentEntity fromDTO(UpdateApartment updatamentApartment) {
                return new ApartmentEntity(
                                updatamentApartment.title(),
                                updatamentApartment.description(),
                                updatamentApartment.price(),
                                updatamentApartment.bills(),
                                updatamentApartment.ubication(),
                                updatamentApartment.state(),
                                null);
        }
}
