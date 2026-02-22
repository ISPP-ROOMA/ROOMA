package com.example.demo.Vivienda.DTOs;

import com.example.demo.Vivienda.ViviendaEntity;

import java.math.BigDecimal;
import java.util.List;

public record ViviendaDTO(
        Integer id,
        String titulo,
        String descripcion,
        BigDecimal precio,
        String gastos,
        String ubicacion,
        String estado
) {
    public static ViviendaDTO fromEntity(ViviendaEntity vivienda) {
        return new ViviendaDTO(
                vivienda.getId(),
                vivienda.getTitulo(),
                vivienda.getDescripcion(),
                vivienda.getPrecio(),
                vivienda.getGastos(),
                vivienda.getUbicacion(),
                vivienda.getEstado()
        );
    }

    public static List<ViviendaDTO> fromEntityList(List<ViviendaEntity> viviendas) {
        return viviendas.stream().map(ViviendaDTO::fromEntity).toList();
    }
}




