package com.example.demo.Apartment.DTOs;

import java.util.ArrayList;
import java.util.List;
import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.MemberApartment.DTOs.ApartmentMemberDTO;

public record ApartmentDTO(
    Integer id, 
    String title, 
    String description, 
    Double price, 
    String bills, 
    String ubication,
    ApartmentState state, 
    String coverImageUrl, 
    List<ApartmentMemberDTO> members
) {

    public static ApartmentDTO fromApartmentEntity(ApartmentEntity apartment) {
        return new ApartmentDTO(
            apartment.getId(), 
            apartment.getTitle(), 
            apartment.getDescription(),
            apartment.getPrice(), 
            apartment.getBills(), 
            apartment.getUbication(), 
            apartment.getState(),
            apartment.getCoverImageUrl(), 
            new ArrayList<>()
        );
    }

    public static ApartmentDTO fromApartmentEntityWithMembers(ApartmentEntity apartment, List<ApartmentMemberDTO> members) {
        return new ApartmentDTO(
            apartment.getId(), 
            apartment.getTitle(), 
            apartment.getDescription(),
            apartment.getPrice(), 
            apartment.getBills(), 
            apartment.getUbication(), 
            apartment.getState(),
            apartment.getCoverImageUrl(), 
            members
        );
    }

    public static List<ApartmentDTO> fromApartmentEntityList(List<ApartmentEntity> apartments) {
        return apartments.stream()
                .map(ApartmentDTO::fromApartmentEntity)
                .toList();
    }
}