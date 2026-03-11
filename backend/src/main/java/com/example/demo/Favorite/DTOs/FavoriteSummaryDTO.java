package com.example.demo.Favorite.DTOs;

import java.time.LocalDateTime;

import com.example.demo.Apartment.ApartmentState;

public record FavoriteSummaryDTO(
        Integer apartmentId,
        String title,
        String ubication,
        String city,
        Double price,
        ApartmentState state,
        boolean available,
        String availabilityStatus,
        boolean canAccessDetail,
        boolean detailAccessible,
        String mainImageUrl,
        boolean isFavorite,
        String statusMessage,
        LocalDateTime favoritedAt
) {
}
