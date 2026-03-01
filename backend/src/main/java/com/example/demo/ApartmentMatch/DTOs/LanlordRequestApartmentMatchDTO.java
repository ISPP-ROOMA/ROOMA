package com.example.demo.ApartmentMatch.DTOs;

import java.util.List;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.MatchStatus;
import com.example.demo.User.DTOs.UserDTO;

public record LanlordRequestApartmentMatchDTO(Integer id, UserDTO user, ApartmentDTO apartment, MatchStatus matchStatus) {

    public static LanlordRequestApartmentMatchDTO fromApartmentMatchEntity(ApartmentMatchEntity apartmentMatch) {
        return new LanlordRequestApartmentMatchDTO(apartmentMatch.getId(), UserDTO.fromUserEntity(apartmentMatch.getCandidate()), ApartmentDTO.fromApartmentEntity(apartmentMatch.getApartment()), apartmentMatch.getMatchStatus());
    }

    public static List<LanlordRequestApartmentMatchDTO> fromApartmentMatchEntityList(List<ApartmentMatchEntity> apartmentMatches) {

        return apartmentMatches.stream()
                .map(LanlordRequestApartmentMatchDTO::fromApartmentMatchEntity)
                .toList();
    }
}