package com.example.demo.Apartment.DTOs;

import com.example.demo.Apartment.ApartmentEntity;

import java.util.List;

public record ApartmentDTO(Integer id, String title, String description, Double price, String bills, String ubication,
    String state, String coverImageUrl) {

    public static ApartmentDTO fromApartmentEntity(ApartmentEntity apartment) {
    String coverImageUrl = apartment.getPhotos() == null
        ? null
        : apartment.getPhotos().stream()
            .filter(p -> p.getOrden() != null && p.getOrden().equals(1))
            .map(p -> p.getUrl())
            .findFirst()
            .orElseGet(() -> apartment.getPhotos().stream()
                .map(p -> p.getUrl())
                .findFirst()
                .orElse(null));

        return new ApartmentDTO(apartment.getId(), apartment.getTitle(), apartment.getDescription(),
        apartment.getPrice(), apartment.getBills(), apartment.getUbication(), apartment.getState(), coverImageUrl);
    }

    public static List<ApartmentDTO> fromApartmentEntityList(List<ApartmentEntity> apartments) {

        return apartments.stream()
                .map(ApartmentDTO::fromApartmentEntity)
                .toList();
    }
}
