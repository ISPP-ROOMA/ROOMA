package com.example.demo.MemberApartment.DTOs;

import com.example.demo.MemberApartment.ApartmentRuleEntity;

public record ApartmentRuleDTO(
        Integer apartmentId,
        boolean allowsPets,
        boolean allowsSmokers,
        boolean partiesAllowed
) {
    public static ApartmentRuleDTO fromEntity(ApartmentRuleEntity rule) {
        return new ApartmentRuleDTO(
                rule.getApartment().getId(),
                rule.isAllowsPets(),
                rule.isAllowsSmokers(),
                rule.isPartiesAllowed()
        );
    }
}