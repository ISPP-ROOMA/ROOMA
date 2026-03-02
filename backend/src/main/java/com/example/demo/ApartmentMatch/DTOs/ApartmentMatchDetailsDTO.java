package com.example.demo.ApartmentMatch.DTOs;

import java.util.List;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.MatchStatus;
import com.example.demo.User.DTOs.UserDTO;

public record ApartmentMatchDetailsDTO(Integer id, UserDTO user, ApartmentDTO apartment, MatchStatus matchStatus) {

    public static ApartmentMatchDetailsDTO fromApartmentMatchEntity(ApartmentMatchEntity apartmentMatch) {
        return new ApartmentMatchDetailsDTO(apartmentMatch.getId(), UserDTO.fromUserEntity(apartmentMatch.getCandidate()), ApartmentDTO.fromApartmentEntity(apartmentMatch.getApartment()), apartmentMatch.getMatchStatus());
    }

    public static List<ApartmentMatchDetailsDTO> fromApartmentMatchEntityList(List<ApartmentMatchEntity> apartmentMatches) {

        return apartmentMatches.stream()
                .map(ApartmentMatchDetailsDTO::fromApartmentMatchEntity)
                .toList();
    }
}