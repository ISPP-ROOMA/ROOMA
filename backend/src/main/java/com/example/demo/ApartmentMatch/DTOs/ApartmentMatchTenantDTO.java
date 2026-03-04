package com.example.demo.ApartmentMatch.DTOs;

import java.util.List;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.MatchStatus;
import com.example.demo.User.DTOs.UserDTO;

public record ApartmentMatchTenantDTO(Integer id, UserDTO tenant, ApartmentDTO apartment, MatchStatus matchStatus) {

    public static ApartmentMatchTenantDTO fromApartmentMatchEntity(ApartmentMatchEntity apartmentMatch) {
        return new ApartmentMatchTenantDTO(apartmentMatch.getId(), UserDTO.fromUserEntity(apartmentMatch.getCandidate()), ApartmentDTO.fromApartmentEntity(apartmentMatch.getApartment()), apartmentMatch.getMatchStatus());
    }

    public static List<ApartmentMatchTenantDTO> fromApartmentMatchEntityList(List<ApartmentMatchEntity> apartmentMatches) {

        return apartmentMatches.stream()
                .map(ApartmentMatchTenantDTO::fromApartmentMatchEntity)
                .toList();
    }
}