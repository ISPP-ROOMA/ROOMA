package com.example.demo.Apartment.DTOs;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateApartment(
                @NotBlank @Size(max = 100) String title,
                @NotBlank @Size(max = 1000) String description,
                @NotNull @DecimalMin(value = "0.0", inclusive = false) Double price,
                @Size(max = 255) String bills,
                @NotBlank @Size(max = 255) String ubication,
                @NotNull ApartmentState state,
                @NotNull @Min(1) Integer maxTenants,
                @Size(max = 1000) String idealTenantProfile) {

        public static ApartmentEntity fromDTO(UpdateApartment updatamentApartment) {
                ApartmentEntity apartment = new ApartmentEntity(
                                updatamentApartment.title(),
                                updatamentApartment.description(),
                                updatamentApartment.price(),
                                updatamentApartment.bills(),
                                updatamentApartment.ubication(),
                                updatamentApartment.state(),
                                updatamentApartment.maxTenants(),
                                null);
                apartment.setIdealTenantProfile(updatamentApartment.idealTenantProfile());
                return apartment;
        }
}
