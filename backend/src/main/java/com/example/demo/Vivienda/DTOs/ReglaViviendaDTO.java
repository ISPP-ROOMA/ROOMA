package com.example.demo.Vivienda.DTOs;

import com.example.demo.Vivienda.ReglaViviendaEntity;

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




