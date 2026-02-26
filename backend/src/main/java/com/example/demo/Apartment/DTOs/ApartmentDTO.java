package com.example.demo.Apartment.DTOs;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.MemberApartment.DTOs.ApartmentMemberDTO;

/**
 * DTO principal de la rama TRUNK
 */
public record ApartmentDTO(
    Integer id, 
    String title, 
    String description, 
    Double price, 
    String bills, 
    String ubication,
    ApartmentState state, 
    String imageUrl, 
    List<ApartmentMemberDTO> members
) {

    public static ApartmentDTO fromApartmentEntity(ApartmentEntity apartments) {
        return new ApartmentDTO(
            apartments.getId(), 
            apartments.getTitle(), 
            apartments.getDescription(),
            apartments.getPrice(), 
            apartments.getBills(), 
            apartments.getUbication(), 
            apartments.getState(),
            apartments.getImageUrl(), 
            new ArrayList<>()
        );
    }

    public static ApartmentDTO fromApartmentEntityWithMembers(ApartmentEntity apartments, List<ApartmentMemberDTO> members) {
        return new ApartmentDTO(
            apartments.getId(), 
            apartments.getTitle(), 
            apartments.getDescription(),
            apartments.getPrice(), 
            apartments.getBills(), 
            apartments.getUbication(), 
            apartments.getState(),
            apartments.getImageUrl(), 
            members
        );
    }

    public static List<ApartmentDTO> fromApartmentEntityList(List<ApartmentEntity> apartments) {
        return apartments.stream()
                .map(ApartmentDTO::fromApartmentEntity)
                .toList();
    }
}

/**
 * DTO temporal de la rama TRUNK-2
 * Sin "public" para que coexista en este archivo durante el merge
 */
record ApartmentDTO2(
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