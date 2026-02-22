package com.example.demo.Vivienda.DTOs;

import com.example.demo.Vivienda.MiembroViviendaEntity;

import java.time.LocalDate;
import java.util.List;

public record MiembroViviendaDTO(
        Integer id,
        Integer viviendaId,
        Integer usuarioId,
        String rol,
        LocalDate fechaIngreso
) {
    public static MiembroViviendaDTO fromEntity(MiembroViviendaEntity miembro) {
        return new MiembroViviendaDTO(
                miembro.getId(),
                miembro.getVivienda().getId(),
                miembro.getUsuario().getId(),
                miembro.getRol().name(),
                miembro.getFechaIngreso()
        );
    }

    public static List<MiembroViviendaDTO> fromEntityList(List<MiembroViviendaEntity> miembros) {
        return miembros.stream().map(MiembroViviendaDTO::fromEntity).toList();
    }
}




