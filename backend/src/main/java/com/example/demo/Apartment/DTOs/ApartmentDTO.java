package com.example.demo.Apartment.DTOs;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.MemberApartment.DTOs.ApartmentMemberDTO;

public record ApartmentDTO(Integer id, String title, String description, Double price, String bills, String ubication,
        ApartmentState state, String imageUrl, List<ApartmentMemberDTO> members) {

    public static ApartmentDTO fromApartmentEntity(ApartmentEntity apartments) {
        return new ApartmentDTO(apartments.getId(), apartments.getTitle(), apartments.getDescription(),
                apartments.getPrice(), apartments.getBills(), apartments.getUbication(), apartments.getState(),
                apartments.getImageUrl(), new ArrayList<>());
    }

    public static ApartmentDTO fromApartmentEntityWithMembers(ApartmentEntity apartments,
            List<ApartmentMemberDTO> members) {
        return new ApartmentDTO(apartments.getId(), apartments.getTitle(), apartments.getDescription(),
                apartments.getPrice(), apartments.getBills(), apartments.getUbication(), apartments.getState(),
                apartments.getImageUrl(), members);
    }

    public static List<ApartmentDTO> fromApartmentEntityList(List<ApartmentEntity> apartments) {

        return apartments.stream()
                .map(ApartmentDTO::fromApartmentEntity)
                .toList();
    }
}
