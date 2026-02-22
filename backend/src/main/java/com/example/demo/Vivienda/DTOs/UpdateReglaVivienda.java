package com.example.demo.Vivienda.DTOs;

import jakarta.validation.constraints.NotNull;

public record UpdateReglaVivienda(
        @NotNull Boolean permiteMascotas,
        @NotNull Boolean permiteFumadores,
        @NotNull Boolean fiestasPermitidas
) {
}




