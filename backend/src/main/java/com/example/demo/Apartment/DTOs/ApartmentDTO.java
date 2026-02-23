package com.example.demo.Apartment.DTOs;

import com.example.demo.Apartment.ApartmentEntity;

import java.util.List;

public record ApartmentDTO(Integer id, String title, String description, Double price, String bills, String ubication,
        String state) {

    public static ApartmentDTO fromApartmentEntity(ApartmentEntity apartments) {
        return new ApartmentDTO(apartments.getId(), apartments.getTitle(), apartments.getDescription(),
                apartments.getPrice(), apartments.getBills(), apartments.getUbication(), apartments.getState());
    }

    public static List<ApartmentDTO> fromApartmentEntityList(List<ApartmentEntity> apartments) {

        return apartments.stream()
                .map(ApartmentDTO::fromApartmentEntity)
                .toList();
    }
}
