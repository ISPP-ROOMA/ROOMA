package com.example.demo.ApartmentMatch.DTOs;

import java.util.List;

import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.MatchStatus;

public record ApartmentMatchDTO(Integer id, Integer candidateId, Integer apartmentId, MatchStatus matchStatus) {

    public static ApartmentMatchDTO fromApartmentMatchEntity(ApartmentMatchEntity apartmentMatch) {
        return new ApartmentMatchDTO(apartmentMatch.getId(), apartmentMatch.getCandidate().getId(), apartmentMatch.getApartment().getId(), apartmentMatch.getMatchStatus());
    }

    public static List<ApartmentMatchDTO> fromApartmentMatchEntityList(List<ApartmentMatchEntity> apartmentMatches) {

        return apartmentMatches.stream()
                .map(ApartmentMatchDTO::fromApartmentMatchEntity)
                .toList();
    }
}