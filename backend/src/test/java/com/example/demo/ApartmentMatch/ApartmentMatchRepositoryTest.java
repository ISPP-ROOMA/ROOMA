package com.example.demo.ApartmentMatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;

import jakarta.persistence.EntityManager;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
public class ApartmentMatchRepositoryTest {

    @Autowired
    private ApartmentMatchRepository apartmentMatchRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void findByCandidateIdAndApartmentId_ReturnsMatchWhenPresent() {
        UserEntity landlord = persistUser("landlord-am-1@test.com", Role.LANDLORD);
        UserEntity candidate = persistUser("tenant-am-1@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("Apartment AM 1", landlord, ApartmentState.ACTIVE);
        ApartmentMatchEntity match = persistMatch(candidate, apartment, MatchStatus.ACTIVE);

        Optional<ApartmentMatchEntity> result = apartmentMatchRepository.findByCandidateIdAndApartmentId(
                candidate.getId(), apartment.getId());

        assertTrue(result.isPresent());
        assertEquals(match.getId(), result.get().getId());
    }

    @Test
    public void findByCandidateIdAndApartmentId_ReturnsEmptyWhenMissing() {
        UserEntity landlord = persistUser("landlord-am-2@test.com", Role.LANDLORD);
        UserEntity candidate = persistUser("tenant-am-2@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("Apartment AM 2", landlord, ApartmentState.ACTIVE);

        Optional<ApartmentMatchEntity> result = apartmentMatchRepository.findByCandidateIdAndApartmentId(
                candidate.getId(), apartment.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    public void findByApartmentIdAndMatchStatus_ReturnsOnlyMatchesForApartmentAndStatus() {
        UserEntity landlord = persistUser("landlord-am-3@test.com", Role.LANDLORD);
        UserEntity candidate1 = persistUser("tenant-am-3a@test.com", Role.TENANT);
        UserEntity candidate2 = persistUser("tenant-am-3b@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("Apartment AM 3", landlord, ApartmentState.ACTIVE);
        ApartmentEntity otherApartment = persistApartment("Apartment AM 3B", landlord, ApartmentState.ACTIVE);

        ApartmentMatchEntity expected = persistMatch(candidate1, apartment, MatchStatus.ACTIVE);
        persistMatch(candidate2, apartment, MatchStatus.REJECTED);
        persistMatch(candidate1, otherApartment, MatchStatus.ACTIVE);

        List<ApartmentMatchEntity> result = apartmentMatchRepository.findByApartmentIdAndMatchStatus(
                apartment.getId(), MatchStatus.ACTIVE);

        assertEquals(1, result.size());
        assertEquals(expected.getId(), result.get(0).getId());
    }

    @Test
    public void findByApartmentIdAndMatchStatus_ReturnsEmptyWhenNoMatches() {
        UserEntity landlord = persistUser("landlord-am-4@test.com", Role.LANDLORD);
        ApartmentEntity apartment = persistApartment("Apartment AM 4", landlord, ApartmentState.ACTIVE);

        List<ApartmentMatchEntity> result = apartmentMatchRepository.findByApartmentIdAndMatchStatus(
                apartment.getId(), MatchStatus.MATCH);

        assertTrue(result.isEmpty());
    }

    @Test
    public void findByCandidateIdAndMatchStatus_ReturnsOnlyMatchesForCandidateAndStatus() {
        UserEntity landlord = persistUser("landlord-am-5@test.com", Role.LANDLORD);
        UserEntity candidate = persistUser("tenant-am-5@test.com", Role.TENANT);
        UserEntity otherCandidate = persistUser("tenant-am-5-other@test.com", Role.TENANT);
        ApartmentEntity apartment1 = persistApartment("Apartment AM 5A", landlord, ApartmentState.ACTIVE);
        ApartmentEntity apartment2 = persistApartment("Apartment AM 5B", landlord, ApartmentState.ACTIVE);

        ApartmentMatchEntity expected = persistMatch(candidate, apartment1, MatchStatus.MATCH);
        persistMatch(candidate, apartment2, MatchStatus.ACTIVE);
        persistMatch(otherCandidate, apartment1, MatchStatus.MATCH);

        List<ApartmentMatchEntity> result = apartmentMatchRepository.findByCandidateIdAndMatchStatus(
                candidate.getId(), MatchStatus.MATCH);

        assertEquals(1, result.size());
        assertEquals(expected.getId(), result.get(0).getId());
    }

    @Test
    public void findByUserIdAndMatchStatus_ReturnsOnlyMatchesForLandlordOwnedApartmentsAndStatus() {
        UserEntity landlord = persistUser("landlord-am-6@test.com", Role.LANDLORD);
        UserEntity otherLandlord = persistUser("landlord-am-6-other@test.com", Role.LANDLORD);
        UserEntity candidate = persistUser("tenant-am-6@test.com", Role.TENANT);
        ApartmentEntity ownApartment = persistApartment("Apartment AM 6A", landlord, ApartmentState.ACTIVE);
        ApartmentEntity foreignApartment = persistApartment("Apartment AM 6B", otherLandlord, ApartmentState.ACTIVE);

        ApartmentMatchEntity expected = persistMatch(candidate, ownApartment, MatchStatus.MATCH);
        persistMatch(candidate, ownApartment, MatchStatus.ACTIVE, false);
        persistMatch(candidate, foreignApartment, MatchStatus.MATCH);

        List<ApartmentMatchEntity> result = apartmentMatchRepository.findByUserIdAndMatchStatus(
                landlord.getId(), MatchStatus.MATCH);

        assertEquals(1, result.size());
        assertEquals(expected.getId(), result.get(0).getId());
    }

    @Test
    public void findTenantRequestByUserIdAndStatus_ReturnsOnlyMatchesForCandidateAndStatus() {
        UserEntity landlord = persistUser("landlord-am-7@test.com", Role.LANDLORD);
        UserEntity candidate = persistUser("tenant-am-7@test.com", Role.TENANT);
        UserEntity otherCandidate = persistUser("tenant-am-7-other@test.com", Role.TENANT);
        ApartmentEntity apartment1 = persistApartment("Apartment AM 7A", landlord, ApartmentState.ACTIVE);
        ApartmentEntity apartment2 = persistApartment("Apartment AM 7B", landlord, ApartmentState.ACTIVE);

        ApartmentMatchEntity expected = persistMatch(candidate, apartment1, MatchStatus.ACTIVE);
        persistMatch(candidate, apartment2, MatchStatus.REJECTED);
        persistMatch(otherCandidate, apartment1, MatchStatus.ACTIVE);

        List<ApartmentMatchEntity> result = apartmentMatchRepository.findTenantRequestByUserIdAndStatus(
                candidate.getId(), MatchStatus.ACTIVE);

        assertEquals(1, result.size());
        assertEquals(expected.getId(), result.get(0).getId());
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
        apartment.setPrice(500.0);
        apartment.setBills("wifi");
        apartment.setUbication("Madrid");
        apartment.setState(state);
        apartment.setMaxTenants(2);
        apartment.setUser(owner);
        entityManager.persist(apartment);
        entityManager.flush();
        return apartment;
    }

    private ApartmentMatchEntity persistMatch(UserEntity candidate, ApartmentEntity apartment, MatchStatus status) {
        return persistMatch(candidate, apartment, status, true);
    }

    private ApartmentMatchEntity persistMatch(UserEntity candidate, ApartmentEntity apartment, MatchStatus status,
            boolean candidateInterest) {
        ApartmentMatchEntity match = new ApartmentMatchEntity();
        match.setCandidate(candidate);
        match.setApartment(apartment);
        match.setCandidateInterest(candidateInterest);
        match.setLandlordInterest(status == MatchStatus.ACTIVE ? null : Boolean.TRUE);
        match.setMatchStatus(status);
        match.setMatchDate(LocalDateTime.now());
        entityManager.persist(match);
        entityManager.flush();
        return match;
    }
}
