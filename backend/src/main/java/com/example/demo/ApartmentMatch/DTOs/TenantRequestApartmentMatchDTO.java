package com.example.demo.ApartmentMatch.DTOs;

import java.util.List;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.MatchStatus;

public record TenantRequestApartmentMatchDTO(Integer id, ApartmentDTO apartment, MatchStatus matchStatus) {

    public static TenantRequestApartmentMatchDTO fromApartmentMatchEntity(ApartmentMatchEntity apartmentMatch) {
        return new TenantRequestApartmentMatchDTO(apartmentMatch.getId(), ApartmentDTO.fromApartmentEntity(apartmentMatch.getApartment()), apartmentMatch.getMatchStatus());
    }

    public static List<TenantRequestApartmentMatchDTO> fromApartmentMatchEntityList(List<ApartmentMatchEntity> apartmentMatches) {

        return apartmentMatches.stream()
                .map(TenantRequestApartmentMatchDTO::fromApartmentMatchEntity)
                .toList();
    }
}