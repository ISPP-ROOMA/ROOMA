package com.example.demo.Vivienda.DTOs;

import com.example.demo.Vivienda.MiembroRol;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateMiembroVivienda(
        @NotNull Integer usuarioId,
        @NotNull MiembroRol rol,
        LocalDate fechaIngreso
) {
}




