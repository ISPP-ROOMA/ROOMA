package com.example.demo.ApartmentMatch.DTOs;

import java.util.List;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.MatchStatus;

public record ApartmentMatchSummaryDTO(Integer id, ApartmentDTO apartment, MatchStatus matchStatus) {

    public static ApartmentMatchSummaryDTO fromApartmentMatchEntity(ApartmentMatchEntity apartmentMatch) {
        return new ApartmentMatchSummaryDTO(apartmentMatch.getId(), ApartmentDTO.fromApartmentEntity(apartmentMatch.getApartment()), apartmentMatch.getMatchStatus());
    }

    public static List<ApartmentMatchSummaryDTO> fromApartmentMatchEntityList(List<ApartmentMatchEntity> apartmentMatches) {

        return apartmentMatches.stream()
                .map(ApartmentMatchSummaryDTO::fromApartmentMatchEntity)
                .toList();
    }
}