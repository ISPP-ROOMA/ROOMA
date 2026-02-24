package com.example.demo.MemberApartment.DTOs;

import com.example.demo.MemberApartment.ReglaViviendaEntity;

public record ReglaViviendaDTO(
        Integer viviendaId,
        boolean permiteMascotas,
        boolean permiteFumadores,
        boolean fiestasPermitidas
) {
    public static ReglaViviendaDTO fromEntity(ReglaViviendaEntity regla) {
        return new ReglaViviendaDTO(
                regla.getVivienda().getId(),
                regla.isPermiteMascotas(),
                regla.isPermiteFumadores(),
                regla.isFiestasPermitidas()
        );
    }
}