package com.example.demo.Vivienda.DTOs;

import com.example.demo.Vivienda.MiembroRol;
import jakarta.validation.constraints.NotNull;

public record UpdateMiembroVivienda(
        @NotNull MiembroRol rol
) {
}




