package com.example.demo.Favorite;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.Favorite.DTOs.FavoriteSummaryDTO;
import com.example.demo.Favorite.DTOs.FavoriteToggleResponseDTO;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserService userService;
    private final ApartmentService apartmentService;

    public FavoriteService(FavoriteRepository favoriteRepository, UserService userService,
            ApartmentService apartmentService) {
        this.favoriteRepository = favoriteRepository;
        this.userService = userService;
        this.apartmentService = apartmentService;
    }

    @Transactional
    public FavoriteToggleResponseDTO addFavorite(Integer apartmentId) {
        UserEntity currentUser = resolveCurrentUser();

        if (favoriteRepository.findByUserIdAndApartmentId(currentUser.getId(), apartmentId).isPresent()) {
            return new FavoriteToggleResponseDTO(apartmentId, true, "El apartamento ya está en favoritos");
        }

        ApartmentEntity apartment = resolveApartment(apartmentId);
        FavoriteEntity favorite = new FavoriteEntity(currentUser, apartment);
        favoriteRepository.save(favorite);
        return new FavoriteToggleResponseDTO(apartmentId, true, "El apartamento se ha añadido a favoritos");
    }

    @Transactional
    public FavoriteToggleResponseDTO removeFavorite(Integer apartmentId) {
        UserEntity currentUser = resolveCurrentUser();
        favoriteRepository.deleteByUserIdAndApartmentId(currentUser.getId(), apartmentId);
        return new FavoriteToggleResponseDTO(apartmentId, false, "El apartamento se ha eliminado de favoritos");
    }

    @Transactional(readOnly = true)
    public List<FavoriteSummaryDTO> getCurrentUserFavorites() {
        UserEntity currentUser = resolveCurrentUser();
        List<FavoriteEntity> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());

        return favorites.stream()
                .filter(favorite -> favorite.getApartment() != null)
                .map(this::toSummaryDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Integer> getFavoriteApartmentIds(List<Integer> apartmentIds) {
        if (apartmentIds == null || apartmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        UserEntity currentUser = resolveCurrentUser();
        return favoriteRepository.findFavoriteApartmentIdsByUserIdAndApartmentIds(currentUser.getId(), apartmentIds);
    }

    private UserEntity resolveCurrentUser() {
        return userService.findCurrentUserEntity();
    }

    private ApartmentEntity resolveApartment(Integer apartmentId) {
        return apartmentService.findById(apartmentId);
    }

    private FavoriteSummaryDTO toSummaryDTO(FavoriteEntity favorite) {
        ApartmentEntity apartment = favorite.getApartment();
        boolean available = isApartmentAvailable(apartment.getState());
        String availabilityStatus = available ? "AVAILABLE" : "CLOSED";
        String statusMessage = available ? null : "Este apartamento ya no está disponible";

        return new FavoriteSummaryDTO(
                apartment.getId(),
                apartment.getTitle(),
                apartment.getUbication(),
                apartment.getUbication(),
                apartment.getPrice(),
                apartment.getState(),
                available,
                availabilityStatus,
                available,
                available,
                apartment.getCoverImageUrl(),
                true,
                statusMessage,
                favorite.getCreatedAt());
    }

    private boolean isApartmentAvailable(ApartmentState state) {
        return state == ApartmentState.ACTIVE || state == ApartmentState.MATCHING;
    }
}
