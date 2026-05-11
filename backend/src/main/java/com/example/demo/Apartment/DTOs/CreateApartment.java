package com.example.demo.Apartment.DTOs;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateApartment(
                @NotNull String title,
                @NotNull String description,
                @NotNull Double price,
                String bills,
                @NotNull String ubication,
                @NotNull String state,
                @NotNull @Min(1) Integer maxTenants

) {
        public static ApartmentEntity fromDTO(CreateApartment createApartment) {
                return new ApartmentEntity(
                                createApartment.title(),
                                createApartment.description(),
                                createApartment.price(),
                                createApartment.bills(),
                                createApartment.ubication(),
                                ApartmentState.valueOf(createApartment.state()),
                                createApartment.maxTenants(),
                                null);
        }
}
