package com.example.demo.Apartment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.MatchStatus;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;

import jakarta.persistence.EntityManager;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
public class ApartmentRepositoryTest {

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void search_ReturnsExpectedWithCombinedFilters() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);

        ApartmentEntity expected = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        persistApartment("A2", "Madrid Centro", 250.0, ApartmentState.ACTIVE, landlord);
        persistApartment("A3", "Valencia", 500.0, ApartmentState.ACTIVE, landlord);
        persistApartment("A4", "Madrid Centro", 500.0, ApartmentState.CLOSED, landlord);

        List<ApartmentEntity> result = apartmentRepository.search("Madrid", 400.0, 600.0, ApartmentState.ACTIVE);

        assertEquals(1, result.size());
        assertEquals(expected.getId(), result.get(0).getId());
    }

    @Test
    public void findLandlordByApartmentId_ReturnsOwnerAndEmptyWhenMissing() {
        UserEntity landlord = persistUser("landlord-b@test.com", Role.LANDLORD);
        ApartmentEntity apartment = persistApartment("B1", "Bilbao", 600.0, ApartmentState.ACTIVE, landlord);

        Optional<UserEntity> found = apartmentRepository.findLandlordByApartmentId(apartment.getId());
        Optional<UserEntity> missing = apartmentRepository.findLandlordByApartmentId(999999);

        assertTrue(found.isPresent());
        assertEquals(landlord.getId(), found.get().getId());
        assertTrue(missing.isEmpty());
    }

    @Test
    public void findAllByUserId_ReturnsOnlyOwnerApartments() {
        UserEntity landlord1 = persistUser("landlord-c1@test.com", Role.LANDLORD);
        UserEntity landlord2 = persistUser("landlord-c2@test.com", Role.LANDLORD);

        ApartmentEntity own1 = persistApartment("C1", "Madrid", 500.0, ApartmentState.ACTIVE, landlord1);
        ApartmentEntity own2 = persistApartment("C2", "Madrid", 650.0, ApartmentState.CLOSED, landlord1);
        persistApartment("C3", "Madrid", 700.0, ApartmentState.ACTIVE, landlord2);

        List<ApartmentEntity> result = apartmentRepository.findAllByUserId(landlord1.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> a.getId().equals(own1.getId())));
        assertTrue(result.stream().anyMatch(a -> a.getId().equals(own2.getId())));
    }

    @Test
    public void findDeckForCandidate_ReturnsOnlyActiveNotOwnedAndNotSwiped() {
        UserEntity landlord1 = persistUser("landlord-d1@test.com", Role.LANDLORD);
        UserEntity landlord2 = persistUser("landlord-d2@test.com", Role.LANDLORD);
        UserEntity candidate = persistUser("tenant-d@test.com", Role.TENANT);

        ApartmentEntity expected = persistApartment("D1", "Madrid", 500.0, ApartmentState.ACTIVE, landlord1);
        persistApartment("D2", "Madrid", 500.0, ApartmentState.MATCHING, landlord1);
        persistApartment("D3", "Madrid", 500.0, ApartmentState.CLOSED, landlord2);
        persistApartment("D4", "Madrid", 500.0, ApartmentState.ACTIVE, candidate);
        ApartmentEntity swiped = persistApartment("D5", "Madrid", 500.0, ApartmentState.ACTIVE, landlord2);

        persistMatch(candidate, swiped, MatchStatus.ACTIVE,LocalDateTime.now());

        List<ApartmentEntity> deck = apartmentRepository.findDeckForCandidate(
            candidate.getId(),
            ApartmentState.ACTIVE);

        assertEquals(1, deck.size());
        assertEquals(expected.getId(), deck.get(0).getId());
    }

    @Test
    public void findDeckForCandidate_ExcludesAnyApartmentWithPreviousInteractionUnlessReactivatedAfterMatch() {
        UserEntity landlord = persistUser("landlord-e1@test.com", Role.LANDLORD);
        UserEntity candidate = persistUser("tenant-e@test.com", Role.TENANT);

        ApartmentEntity rejectedApartment = persistApartment("E1", "Sevilla", 550.0, ApartmentState.ACTIVE, landlord);
        ApartmentEntity reactivatedApartment = persistApartment("E2", "Sevilla", 650.0, ApartmentState.ACTIVE, landlord);

        LocalDateTime oldMatchDate = LocalDateTime.now().minusDays(5);
        reactivatedApartment.setActivationDate(LocalDateTime.now().minusDays(1));
        entityManager.flush();

        persistMatch(candidate, rejectedApartment, MatchStatus.REJECTED, LocalDateTime.now().minusDays(2));
        persistMatch(candidate, reactivatedApartment, MatchStatus.CANCELED, oldMatchDate);

        List<ApartmentEntity> deck = apartmentRepository.findDeckForCandidate(
            candidate.getId(),
            ApartmentState.ACTIVE);

        assertEquals(1, deck.size());
        assertTrue(deck.stream().anyMatch(a -> a.getId().equals(reactivatedApartment.getId())));
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

    private ApartmentEntity persistApartment(String title, String ubication, Double price, ApartmentState state, UserEntity owner) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setTitle(title);
        apartment.setDescription("desc " + title);
        apartment.setPrice(price);
        apartment.setBills("bills");
        apartment.setUbication(ubication);
        apartment.setState(state);
        apartment.setMaxTenants(2);
        apartment.setUser(owner);
        entityManager.persist(apartment);
        entityManager.flush();
        return apartment;
    }

    private void persistMatch(UserEntity candidate, ApartmentEntity apartment, MatchStatus status, LocalDateTime matchDate) {
        ApartmentMatchEntity match = new ApartmentMatchEntity();
        match.setCandidate(candidate);
        match.setApartment(apartment);
        match.setCandidateInterest(true);
        match.setMatchStatus(status);
        match.setMatchDate(matchDate);
        entityManager.persist(match);
        entityManager.flush();
    }
}
