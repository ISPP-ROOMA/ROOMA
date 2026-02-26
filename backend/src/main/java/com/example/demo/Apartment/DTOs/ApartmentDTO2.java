package com.example.demo.Apartment.DTOs;

import com.example.demo.Apartment.ApartmentEntity;

public record ApartmentDTO2(
    Integer id, 
    String title, 
    String description, 
    Double price, 
    String bills, 
    String ubication,
    String state, 
    String coverImageUrl
) {

    public static ApartmentDTO2 fromApartmentEntity(ApartmentEntity apartment) {
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

        return new ApartmentDTO2(
            apartment.getId(), 
            apartment.getTitle(), 
            apartment.getDescription(),
            apartment.getPrice(), 
            apartment.getBills(), 
            apartment.getUbication(), 
            apartment.getState() != null ? apartment.getState().toString() : null, 
            coverImageUrl
        );
    }
}