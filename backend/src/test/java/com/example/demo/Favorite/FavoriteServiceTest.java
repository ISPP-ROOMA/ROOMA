package com.example.demo.Favorite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Favorite.DTOs.FavoriteSummaryDTO;
import com.example.demo.Favorite.DTOs.FavoriteToggleResponseDTO;
import com.example.demo.User.Role;
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
    @DisplayName("addFavorite saves relation when favorite does not exist")
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
        assertEquals("El apartamento se ha añadido a favoritos", response.message());
        assertEquals(apartmentId, response.apartmentId());
    }

    @Test
    @DisplayName("addFavorite does not duplicate existing favorite")
    public void addFavorite_DoesNotDuplicate() {
        Integer apartmentId = 12;
        UserEntity currentUser = createUser(8);
        FavoriteEntity existingFavorite = createFavorite(currentUser, createApartment(apartmentId, ApartmentState.ACTIVE), LocalDateTime.now());

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdAndApartmentId(currentUser.getId(), apartmentId))
                .thenReturn(Optional.of(existingFavorite));

        FavoriteToggleResponseDTO response = favoriteService.addFavorite(apartmentId);

        verify(apartmentService, never()).findById(apartmentId);
        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any(FavoriteEntity.class));
        assertTrue(response.isFavorite());
        assertEquals("El apartamento ya está en favoritos", response.message());
        assertEquals(apartmentId, response.apartmentId());
    }

    @Test
    @DisplayName("addFavorite propagates error when apartment is missing")
    public void addFavorite_WhenApartmentLookupFails_PropagatesError() {
        Integer apartmentId = 44;
        UserEntity currentUser = createUser(14);
        ResourceNotFoundException error = new ResourceNotFoundException("Apartment not found");

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdAndApartmentId(currentUser.getId(), apartmentId)).thenReturn(Optional.empty());
        when(apartmentService.findById(apartmentId)).thenThrow(error);

        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> favoriteService.addFavorite(apartmentId));

        assertSame(error, thrown);
        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any(FavoriteEntity.class));
    }

    @Test
    @DisplayName("addFavorite propagates error when current user cannot be resolved")
    public void addFavorite_WhenCurrentUserFails_PropagatesError() {
        ResourceNotFoundException error = new ResourceNotFoundException("User not authenticated");

        when(userService.findCurrentUserEntity()).thenThrow(error);

        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> favoriteService.addFavorite(55));

        assertSame(error, thrown);
        verify(favoriteRepository, never()).findByUserIdAndApartmentId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("removeFavorite deletes favorite for current user")
    public void removeFavorite_DeletesForCurrentUser() {
        Integer apartmentId = 13;
        UserEntity currentUser = createUser(9);

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);

        FavoriteToggleResponseDTO response = favoriteService.removeFavorite(apartmentId);

        verify(favoriteRepository).deleteByUserIdAndApartmentId(currentUser.getId(), apartmentId);
        assertFalse(response.isFavorite());
        assertEquals("El apartamento se ha eliminado de favoritos", response.message());
        assertEquals(apartmentId, response.apartmentId());
    }

    @Test
    @DisplayName("removeFavorite is idempotent even when favorite does not exist")
    public void removeFavorite_IsIdempotent() {
        Integer apartmentId = 130;
        UserEntity currentUser = createUser(19);

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);

        FavoriteToggleResponseDTO response = favoriteService.removeFavorite(apartmentId);

        verify(favoriteRepository).deleteByUserIdAndApartmentId(currentUser.getId(), apartmentId);
        assertFalse(response.isFavorite());
    }

    @Test
    @DisplayName("removeFavorite propagates error when there is no authenticated user")
    public void removeFavorite_WhenCurrentUserFails_PropagatesError() {
        ResourceNotFoundException error = new ResourceNotFoundException("User not authenticated");

        when(userService.findCurrentUserEntity()).thenThrow(error);

        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> favoriteService.removeFavorite(77));

        assertSame(error, thrown);
        verify(favoriteRepository, never()).deleteByUserIdAndApartmentId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("getCurrentUserFavorites returns favorites for current user preserving repository order")
    public void getCurrentUserFavorites_ReturnsFavoritesPreservingOrder() {
        UserEntity currentUser = createUser(10);
        FavoriteEntity firstFavorite = createFavorite(
                currentUser,
                createApartment(22, ApartmentState.CLOSED),
                LocalDateTime.now().minusMinutes(2)
        );
        FavoriteEntity secondFavorite = createFavorite(
                currentUser,
                createApartment(21, ApartmentState.ACTIVE),
                LocalDateTime.now().minusMinutes(5)
        );

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()))
                .thenReturn(List.of(firstFavorite, secondFavorite));

        List<FavoriteSummaryDTO> result = favoriteService.getCurrentUserFavorites();

        assertEquals(2, result.size());
        assertEquals(22, result.get(0).apartmentId());
        assertEquals(21, result.get(1).apartmentId());
    }

    @Test
    @DisplayName("getCurrentUserFavorites filters favorites with null apartment")
    public void getCurrentUserFavorites_FiltersFavoritesWithNullApartment() {
        UserEntity currentUser = createUser(20);
        FavoriteEntity brokenFavorite = new FavoriteEntity();
        brokenFavorite.setUser(currentUser);
        brokenFavorite.setCreatedAt(LocalDateTime.now());

        FavoriteEntity validFavorite = createFavorite(
                currentUser,
                createApartment(201, ApartmentState.ACTIVE),
                LocalDateTime.now().minusMinutes(1)
        );

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()))
                .thenReturn(List.of(brokenFavorite, validFavorite));

        List<FavoriteSummaryDTO> result = favoriteService.getCurrentUserFavorites();

        assertEquals(1, result.size());
        assertEquals(201, result.get(0).apartmentId());
    }

    @Test
    @DisplayName("getCurrentUserFavorites maps active and closed availability correctly")
    public void getCurrentUserFavorites_MapsAvailabilityState() {
        UserEntity currentUser = createUser(21);

        ApartmentEntity activeApartment = createApartment(301, ApartmentState.ACTIVE);
        ApartmentPhotoEntity coverPhoto = createPhoto(1, "https://images.test/active-cover.jpg");
        activeApartment.setPhotos(List.of(coverPhoto));
        FavoriteEntity activeFavorite = createFavorite(currentUser, activeApartment, LocalDateTime.now().minusMinutes(5));

        ApartmentEntity closedApartment = createApartment(302, ApartmentState.CLOSED);
        FavoriteEntity closedFavorite = createFavorite(currentUser, closedApartment, LocalDateTime.now().minusMinutes(2));

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()))
                .thenReturn(List.of(closedFavorite, activeFavorite));

        List<FavoriteSummaryDTO> result = favoriteService.getCurrentUserFavorites();

        FavoriteSummaryDTO closed = result.get(0);
        assertEquals(ApartmentState.CLOSED, closed.state());
        assertFalse(closed.available());
        assertFalse(closed.canAccessDetail());
        assertEquals("CLOSED", closed.availabilityStatus());
        assertFalse(closed.detailAccessible());
        assertEquals("Este apartamento ya no está disponible", closed.statusMessage());

        FavoriteSummaryDTO active = result.get(1);
        assertEquals(ApartmentState.ACTIVE, active.state());
        assertEquals("Madrid", active.ubication());
        assertEquals("Madrid", active.city());
        assertTrue(active.available());
        assertEquals("AVAILABLE", active.availabilityStatus());
        assertTrue(active.canAccessDetail());
        assertTrue(active.detailAccessible());
        assertEquals("https://images.test/active-cover.jpg", active.mainImageUrl());
        assertEquals(activeFavorite.getCreatedAt(), active.favoritedAt());
    }

    @Test
    @DisplayName("getCurrentUserFavorites maps matching apartments as available")
    public void getCurrentUserFavorites_MapsMatchingAsAvailable() {
        UserEntity currentUser = createUser(22);
        FavoriteEntity matchingFavorite = createFavorite(
                currentUser,
                createApartment(401, ApartmentState.MATCHING),
                LocalDateTime.now()
        );

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()))
                .thenReturn(List.of(matchingFavorite));

        List<FavoriteSummaryDTO> result = favoriteService.getCurrentUserFavorites();

        assertEquals(1, result.size());
        assertTrue(result.get(0).available());
        assertTrue(result.get(0).canAccessDetail());
        assertNull(result.get(0).statusMessage());
    }

    @Test
    @DisplayName("getCurrentUserFavorites returns null main image when apartment has no cover image")
    public void getCurrentUserFavorites_ReturnsNullMainImageWhenNoCoverExists() {
        UserEntity currentUser = createUser(23);
        ApartmentEntity apartment = createApartment(501, ApartmentState.ACTIVE);
        apartment.setPhotos(List.of());
        FavoriteEntity favorite = createFavorite(currentUser, apartment, LocalDateTime.now());

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()))
                .thenReturn(List.of(favorite));

        List<FavoriteSummaryDTO> result = favoriteService.getCurrentUserFavorites();

        assertEquals(1, result.size());
        assertNull(result.get(0).mainImageUrl());
    }

    @Test
    @DisplayName("getCurrentUserFavorites falls back to first photo when no order 1 cover exists")
    public void getCurrentUserFavorites_UsesFirstPhotoWhenNoOrderOneExists() {
        UserEntity currentUser = createUser(24);
        ApartmentEntity apartment = createApartment(601, ApartmentState.ACTIVE);

        ApartmentPhotoEntity secondPhoto = createPhoto(2, "https://images.test/second-photo.jpg");
        ApartmentPhotoEntity thirdPhoto = createPhoto(3, "https://images.test/third-photo.jpg");
        apartment.setPhotos(List.of(secondPhoto, thirdPhoto));

        FavoriteEntity favorite = createFavorite(currentUser, apartment, LocalDateTime.now());

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()))
                .thenReturn(List.of(favorite));

        List<FavoriteSummaryDTO> result = favoriteService.getCurrentUserFavorites();

        assertEquals(1, result.size());
        assertEquals("https://images.test/second-photo.jpg", result.get(0).mainImageUrl());
    }

    @Test
    @DisplayName("getFavoriteApartmentIds returns empty list when input is null")
    public void getFavoriteApartmentIds_ReturnsEmptyListWhenInputIsNull() {
        List<Integer> result = favoriteService.getFavoriteApartmentIds(null);

        assertTrue(result.isEmpty());
        verify(favoriteRepository, never()).findFavoriteApartmentIdsByUserIdAndApartmentIds(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("getFavoriteApartmentIds returns empty list when input is empty")
    public void getFavoriteApartmentIds_ReturnsEmptyListWhenInputIsEmpty() {
        List<Integer> result = favoriteService.getFavoriteApartmentIds(List.of());

        assertTrue(result.isEmpty());
        verify(favoriteRepository, never()).findFavoriteApartmentIdsByUserIdAndApartmentIds(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("getFavoriteApartmentIds returns subset for current user")
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

    @Test
    @DisplayName("getFavoriteApartmentIds propagates error when current user cannot be resolved")
    public void getFavoriteApartmentIds_WhenCurrentUserFails_PropagatesError() {
        ResourceNotFoundException error = new ResourceNotFoundException("User not authenticated");

        when(userService.findCurrentUserEntity()).thenThrow(error);

        ResourceNotFoundException thrown = assertThrows(
                ResourceNotFoundException.class,
                () -> favoriteService.getFavoriteApartmentIds(List.of(10, 11))
        );

        assertSame(error, thrown);
        verify(favoriteRepository, never()).findFavoriteApartmentIdsByUserIdAndApartmentIds(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
    }

    private UserEntity createUser(Integer id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("user" + id + "@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.TENANT);
        return user;
    }

    private ApartmentEntity createApartment(Integer id, ApartmentState state) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(id);
        apartment.setTitle("Apartment " + id);
        apartment.setDescription("Description " + id);
        apartment.setUbication("Madrid");
        apartment.setPrice(500.0);
        apartment.setBills("wifi");
        apartment.setState(state);
        apartment.setMaxTenants(4);
        return apartment;
    }

    private FavoriteEntity createFavorite(UserEntity user, ApartmentEntity apartment, LocalDateTime createdAt) {
        FavoriteEntity favorite = new FavoriteEntity(user, apartment);
        favorite.setCreatedAt(createdAt);
        return favorite;
    }

    private ApartmentPhotoEntity createPhoto(Integer order, String url) {
        ApartmentPhotoEntity photo = new ApartmentPhotoEntity();
        photo.setPhoto_order(order);
        photo.setUrl(url);
        photo.setPublicId("public-" + order);
        photo.setCover(order == 1);
        return photo;
    }
}
