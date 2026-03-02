package com.example.demo.ApartmentMatch.DTOs;

import java.util.List;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.MatchStatus;
import com.example.demo.User.DTOs.UserDTO;

public record ApartmentMatchLandlordDTO(Integer id, UserDTO landlord, ApartmentDTO apartment, MatchStatus matchStatus) {

    public static ApartmentMatchLandlordDTO fromApartmentMatchEntity(ApartmentMatchEntity apartmentMatch) {
        return new ApartmentMatchLandlordDTO(apartmentMatch.getId(), UserDTO.fromUserEntity(apartmentMatch.getApartment().getUser()), ApartmentDTO.fromApartmentEntity(apartmentMatch.getApartment()), apartmentMatch.getMatchStatus());
    }

    public static List<ApartmentMatchLandlordDTO> fromApartmentMatchEntityList(List<ApartmentMatchEntity> apartmentMatches) {

        return apartmentMatches.stream()
                .map(ApartmentMatchLandlordDTO::fromApartmentMatchEntity)
                .toList();
    }
}