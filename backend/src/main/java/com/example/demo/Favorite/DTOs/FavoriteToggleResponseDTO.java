package com.example.demo.Favorite.DTOs;

public record FavoriteToggleResponseDTO(
        Integer apartmentId,
        boolean isFavorite,
        String message
) {
}
