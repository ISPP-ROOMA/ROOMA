package com.example.demo.Vivienda.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateVivienda(
        @NotBlank String titulo,
        @NotBlank String descripcion,
        @NotNull BigDecimal precio,
        @NotBlank String gastos,
        @NotBlank String ubicacion,
        @NotBlank String estado
) {
}




