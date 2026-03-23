package com.example.demo.Favorite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
public class FavoriteRepositoryTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void findByUserIdAndApartmentId_ReturnsFavoriteWhenPresent() {
        UserEntity user = persistUser("favorite-user-1@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("Favorite A", user, ApartmentState.ACTIVE);
        FavoriteEntity favorite = persistFavorite(user, apartment, LocalDateTime.now().minusMinutes(1));

        Optional<FavoriteEntity> result = favoriteRepository.findByUserIdAndApartmentId(user.getId(), apartment.getId());

        assertTrue(result.isPresent());
        assertEquals(favorite.getId(), result.get().getId());
    }

    @Test
    public void findByUserIdAndApartmentId_ReturnsEmptyWhenMissing() {
        UserEntity user = persistUser("favorite-user-2@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("Favorite B", user, ApartmentState.ACTIVE);

        Optional<FavoriteEntity> result = favoriteRepository.findByUserIdAndApartmentId(user.getId(), apartment.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    public void deleteByUserIdAndApartmentId_DeletesOnlyTargetFavorite() {
        UserEntity user = persistUser("favorite-user-3@test.com", Role.TENANT);
        ApartmentEntity apartment1 = persistApartment("Favorite C1", user, ApartmentState.ACTIVE);
        ApartmentEntity apartment2 = persistApartment("Favorite C2", user, ApartmentState.ACTIVE);

        FavoriteEntity target = persistFavorite(user, apartment1, LocalDateTime.now().minusMinutes(2));
        FavoriteEntity survivor = persistFavorite(user, apartment2, LocalDateTime.now().minusMinutes(1));

        favoriteRepository.deleteByUserIdAndApartmentId(user.getId(), apartment1.getId());
        entityManager.flush();
        entityManager.clear();

        assertTrue(favoriteRepository.findById(target.getId()).isEmpty());
        assertTrue(favoriteRepository.findById(survivor.getId()).isPresent());
    }

    @Test
    public void findByUserIdOrderByCreatedAtDesc_ReturnsOnlyUserFavoritesOrderedDescending() {
        UserEntity user = persistUser("favorite-user-4@test.com", Role.TENANT);
        UserEntity otherUser = persistUser("favorite-user-4-other@test.com", Role.TENANT);

        ApartmentEntity oldestApartment = persistApartment("Favorite D1", user, ApartmentState.ACTIVE);
        ApartmentEntity newestApartment = persistApartment("Favorite D2", user, ApartmentState.MATCHING);
        ApartmentEntity foreignApartment = persistApartment("Favorite D3", otherUser, ApartmentState.CLOSED);

        FavoriteEntity oldest = persistFavorite(user, oldestApartment, LocalDateTime.now().minusHours(2));
        FavoriteEntity newest = persistFavorite(user, newestApartment, LocalDateTime.now().minusMinutes(10));
        persistFavorite(otherUser, foreignApartment, LocalDateTime.now().minusMinutes(1));

        List<FavoriteEntity> result = favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertEquals(2, result.size());
        assertEquals(newest.getId(), result.get(0).getId());
        assertEquals(oldest.getId(), result.get(1).getId());
    }

    @Test
    public void findFavoriteApartmentIdsByUserIdAndApartmentIds_ReturnsIntersectionForCurrentUser() {
        UserEntity user = persistUser("favorite-user-5@test.com", Role.TENANT);
        UserEntity otherUser = persistUser("favorite-user-5-other@test.com", Role.TENANT);

        ApartmentEntity favorite1 = persistApartment("Favorite E1", user, ApartmentState.ACTIVE);
        ApartmentEntity favorite2 = persistApartment("Favorite E2", user, ApartmentState.ACTIVE);
        ApartmentEntity notFavorite = persistApartment("Favorite E3", user, ApartmentState.ACTIVE);
        ApartmentEntity foreignFavorite = persistApartment("Favorite E4", otherUser, ApartmentState.ACTIVE);

        persistFavorite(user, favorite1, LocalDateTime.now().minusMinutes(3));
        persistFavorite(user, favorite2, LocalDateTime.now().minusMinutes(2));
        persistFavorite(otherUser, foreignFavorite, LocalDateTime.now().minusMinutes(1));

        List<Integer> result = favoriteRepository.findFavoriteApartmentIdsByUserIdAndApartmentIds(
                user.getId(),
                List.of(favorite1.getId(), favorite2.getId(), notFavorite.getId(), foreignFavorite.getId())
        );

        assertEquals(2, result.size());
        assertTrue(result.contains(favorite1.getId()));
        assertTrue(result.contains(favorite2.getId()));
    }

    @Test
    public void findFavoriteApartmentIdsByUserIdAndApartmentIds_ReturnsEmptyWhenNoMatches() {
        UserEntity user = persistUser("favorite-user-6@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("Favorite F1", user, ApartmentState.ACTIVE);

        List<Integer> result = favoriteRepository.findFavoriteApartmentIdsByUserIdAndApartmentIds(
                user.getId(),
                List.of(apartment.getId())
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void findFavoriteApartmentIdsByUserIdAndApartmentIds_ReturnsEmptyWhenSubsetIsEmpty() {
        UserEntity user = persistUser("favorite-user-7@test.com", Role.TENANT);

        List<Integer> result = favoriteRepository.findFavoriteApartmentIdsByUserIdAndApartmentIds(user.getId(), List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    public void save_AssignsCreatedAtAutomatically() {
        UserEntity user = persistUser("favorite-user-8@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("Favorite G1", user, ApartmentState.ACTIVE);

        FavoriteEntity favorite = new FavoriteEntity(user, apartment);
        favoriteRepository.saveAndFlush(favorite);

        assertNotNull(favorite.getCreatedAt());
    }

    @Test
    public void save_RejectsDuplicateUserApartmentPairs() {
        UserEntity user = persistUser("favorite-user-9@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("Favorite H1", user, ApartmentState.ACTIVE);

        favoriteRepository.saveAndFlush(new FavoriteEntity(user, apartment));

        assertThrows(DataIntegrityViolationException.class, () -> {
            favoriteRepository.saveAndFlush(new FavoriteEntity(user, apartment));
        });
    }

    private UserEntity persistUser(String email, Role role) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private ApartmentEntity persistApartment(String title, UserEntity owner, ApartmentState state) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setTitle(title);
        apartment.setDescription("Description for " + title);
        apartment.setPrice(450.0);
        apartment.setBills("wifi");
        apartment.setUbication("Madrid");
        apartment.setState(state);
        apartment.setUser(owner);
        entityManager.persist(apartment);
        entityManager.flush();
        return apartment;
    }

    private FavoriteEntity persistFavorite(UserEntity user, ApartmentEntity apartment, LocalDateTime createdAt) {
        FavoriteEntity favorite = new FavoriteEntity(user, apartment);
        favorite.setCreatedAt(createdAt);
        entityManager.persist(favorite);
        entityManager.flush();
        return favorite;
    }
}
