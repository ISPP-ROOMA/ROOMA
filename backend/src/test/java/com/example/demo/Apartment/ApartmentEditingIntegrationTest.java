package com.example.demo.Apartment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.UserDetailsImpl;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class ApartmentEditingIntegrationTest {

    @Autowired
    private ApartmentService apartmentService;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity landlord;
    private UserEntity otherUser;
    private ApartmentEntity apartment;

    @BeforeEach
    void setUp() {
        apartmentRepository.deleteAll();
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();

        landlord = new UserEntity();
        landlord.setEmail("landlord@test.com");
        landlord.setPassword("pwd");
        landlord.setRole(Role.LANDLORD);
        landlord = userRepository.save(landlord);

        otherUser = new UserEntity();
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("pwd");
        otherUser.setRole(Role.TENANT);
        otherUser = userRepository.save(otherUser);

        apartment = new ApartmentEntity(
                "Titulo",
                "Descripcion original",
                300.0,
                "Gastos incluidos",
                "Sevilla",
                ApartmentState.ACTIVE,
                landlord);
        apartment.setIdealTenantProfile("Perfil original");
        apartment = apartmentRepository.save(apartment);
    }

    private void authenticate(UserEntity user) {
        UserDetailsImpl principal = UserDetailsImpl.fromUserEntity(user);
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                principal.getPassword(),
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void landlordCanEditOwnApartment() {
        authenticate(landlord);

        ApartmentEntity payload = new ApartmentEntity(
                "Nuevo titulo",
                "Nueva descripcion",
                450.0,
                "Nuevos gastos",
                "Madrid",
                ApartmentState.CLOSED,
                null);
        payload.setIdealTenantProfile("Nuevo perfil ideal");

        ApartmentEntity updated = apartmentService.update(apartment.getId(), payload);

        assertNotNull(updated);
        assertEquals("Nuevo titulo", updated.getTitle());
        assertEquals("Nueva descripcion", updated.getDescription());
        assertEquals(450.0, updated.getPrice());
        assertEquals("Nuevos gastos", updated.getBills());
        assertEquals("Madrid", updated.getUbication());
        assertEquals(ApartmentState.CLOSED, updated.getState());
        assertEquals("Nuevo perfil ideal", updated.getIdealTenantProfile());
        // el propietario no cambia
        assertEquals(landlord.getId(), updated.getUser().getId());
    }

    @Test
    void nonOwnerCannotEditApartment() {
        authenticate(otherUser);

        ApartmentEntity payload = new ApartmentEntity(
                "Titulo hackeado",
                "Descripcion hackeada",
                999.0,
                "Gastos hackeados",
                "Ciudad",
                ApartmentState.ACTIVE,
                null);

        assertThrows(ForbiddenException.class,
                () -> apartmentService.update(apartment.getId(), payload));
    }

    @Test
    void editingNonExistingApartmentThrowsNotFound() {
        authenticate(landlord);

        ApartmentEntity payload = new ApartmentEntity(
                "Titulo",
                "Descripcion",
                300.0,
                "Gastos",
                "Ubicacion",
                ApartmentState.ACTIVE,
                null);

        Integer nonExistingId = apartment.getId() + 9999;

        assertThrows(ResourceNotFoundException.class,
                () -> apartmentService.update(nonExistingId, payload));
    }
}

