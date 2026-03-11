package com.example.demo.Favorite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;
import com.example.demo.Favorite.DTOs.FavoriteSummaryDTO;
import com.example.demo.Favorite.DTOs.FavoriteToggleResponseDTO;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@ExtendWith(MockitoExtension.class)
public class FavoriteServiceTest {

    private FavoriteService favoriteService;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApartmentService apartmentService;

    @BeforeEach
    public void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, userService, apartmentService);
    }

    @Test
    @DisplayName("addFavorite: saves relation when favorite does not exist")
    public void addFavorite_SavesWhenNotExisting() {
        Integer apartmentId = 11;
        UserEntity currentUser = createUser(7);
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE);

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdAndApartmentId(currentUser.getId(), apartmentId)).thenReturn(Optional.empty());
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        FavoriteToggleResponseDTO response = favoriteService.addFavorite(apartmentId);

        ArgumentCaptor<FavoriteEntity> captor = ArgumentCaptor.forClass(FavoriteEntity.class);
        verify(favoriteRepository).save(captor.capture());
        assertEquals(currentUser.getId(), captor.getValue().getUser().getId());
        assertEquals(apartmentId, captor.getValue().getApartment().getId());
        assertTrue(response.isFavorite());
        assertEquals(apartmentId, response.apartmentId());
    }

    @Test
    @DisplayName("addFavorite: does not duplicate existing favorite")
    public void addFavorite_DoesNotDuplicate() {
        Integer apartmentId = 12;
        UserEntity currentUser = createUser(8);
        FavoriteEntity existingFavorite = new FavoriteEntity(currentUser, createApartment(apartmentId, ApartmentState.ACTIVE));

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdAndApartmentId(currentUser.getId(), apartmentId))
                .thenReturn(Optional.of(existingFavorite));

        FavoriteToggleResponseDTO response = favoriteService.addFavorite(apartmentId);

        verify(apartmentService, never()).findById(apartmentId);
        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any(FavoriteEntity.class));
        assertTrue(response.isFavorite());
        assertEquals(apartmentId, response.apartmentId());
    }

    @Test
    @DisplayName("removeFavorite: delete is idempotent")
    public void removeFavorite_IsIdempotent() {
        Integer apartmentId = 13;
        UserEntity currentUser = createUser(9);

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);

        FavoriteToggleResponseDTO response = favoriteService.removeFavorite(apartmentId);

        verify(favoriteRepository).deleteByUserIdAndApartmentId(currentUser.getId(), apartmentId);
        assertFalse(response.isFavorite());
        assertEquals(apartmentId, response.apartmentId());
    }

    @Test
    @DisplayName("getCurrentUserFavorites: returns own favorites and marks availability")
    public void getCurrentUserFavorites_ReturnsAvailabilityState() {
        UserEntity currentUser = createUser(10);

        ApartmentEntity activeApartment = createApartment(21, ApartmentState.ACTIVE);
        ApartmentPhotoEntity activePhoto = new ApartmentPhotoEntity();
        activePhoto.setOrden(1);
        activePhoto.setUrl("https://images.test/active-cover.jpg");
        activeApartment.setPhotos(List.of(activePhoto));

        FavoriteEntity activeFavorite = new FavoriteEntity(currentUser, activeApartment);
        activeFavorite.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        ApartmentEntity closedApartment = createApartment(22, ApartmentState.CLOSED);
        FavoriteEntity closedFavorite = new FavoriteEntity(currentUser, closedApartment);
        closedFavorite.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()))
                .thenReturn(List.of(closedFavorite, activeFavorite));

        List<FavoriteSummaryDTO> result = favoriteService.getCurrentUserFavorites();

        assertEquals(2, result.size());

        FavoriteSummaryDTO first = result.get(0);
        assertEquals(22, first.apartmentId());
        assertEquals(ApartmentState.CLOSED, first.state());
        assertFalse(first.available());
        assertFalse(first.canAccessDetail());
        assertEquals("CLOSED", first.availabilityStatus());
        assertFalse(first.detailAccessible());
        assertTrue(first.isFavorite());

        FavoriteSummaryDTO second = result.get(1);
        assertEquals(21, second.apartmentId());
        assertEquals("Madrid", second.city());
        assertEquals(ApartmentState.ACTIVE, second.state());
        assertTrue(second.available());
        assertTrue(second.canAccessDetail());
        assertEquals("AVAILABLE", second.availabilityStatus());
        assertTrue(second.detailAccessible());
        assertTrue(second.isFavorite());
        assertEquals("https://images.test/active-cover.jpg", second.mainImageUrl());
    }

    @Test
    @DisplayName("getFavoriteApartmentIds: returns subset of favorites for current user")
    public void getFavoriteApartmentIds_ReturnsSubset() {
        UserEntity currentUser = createUser(11);
        List<Integer> apartmentIds = List.of(1, 2, 3, 4);

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findFavoriteApartmentIdsByUserIdAndApartmentIds(currentUser.getId(), apartmentIds))
                .thenReturn(List.of(2, 4));

        List<Integer> result = favoriteService.getFavoriteApartmentIds(apartmentIds);

        assertEquals(List.of(2, 4), result);
        verify(favoriteRepository).findFavoriteApartmentIdsByUserIdAndApartmentIds(currentUser.getId(), apartmentIds);
    }

    private UserEntity createUser(Integer id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("user" + id + "@test.com");
        return user;
    }

    private ApartmentEntity createApartment(Integer id, ApartmentState state) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(id);
        apartment.setTitle("Apartment " + id);
        apartment.setUbication("Madrid");
        apartment.setPrice(500.0);
        apartment.setState(state);
        return apartment;
    }
}
