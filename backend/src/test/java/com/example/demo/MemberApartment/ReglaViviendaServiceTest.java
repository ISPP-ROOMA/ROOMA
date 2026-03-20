package com.example.demo.MemberApartment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.MemberApartment.DTOs.ReglaViviendaDTO;
import com.example.demo.MemberApartment.DTOs.UpdateReglaVivienda;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@ExtendWith(MockitoExtension.class)
class ReglaViviendaServiceTest {

    private ReglaViviendaService reglaViviendaService;

    @Mock
    private ReglaViviendaRepository reglaViviendaRepository;

    @Mock
    private ApartmentService apartmentService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        reglaViviendaService = new ReglaViviendaService(reglaViviendaRepository, apartmentService, userService);
    }

    @Test
    @DisplayName("updateRules updates existing rules when current user is landlord")
    void updateRules_UpdatesExistingWhenLandlord() {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(10);

        UserEntity landlord = new UserEntity();
        landlord.setId(5);
        apartment.setUser(landlord);

        ReglaViviendaEntity existing = new ReglaViviendaEntity();
        existing.setVivienda(apartment);

        when(apartmentService.findById(10)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(reglaViviendaRepository.findByViviendaId(10)).thenReturn(Optional.of(existing));
        when(reglaViviendaRepository.save(any(ReglaViviendaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateReglaVivienda request = new UpdateReglaVivienda(true, false, true);

        ReglaViviendaDTO result = reglaViviendaService.updateRules(10, request);

        assertEquals(10, result.viviendaId());
        assertTrue(result.permiteMascotas());
        assertFalse(result.permiteFumadores());
        assertTrue(result.fiestasPermitidas());
        verify(reglaViviendaRepository).save(existing);
    }

    @Test
    @DisplayName("updateRules creates rules when they do not exist yet")
    void updateRules_CreatesWhenMissing() {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(20);

        UserEntity landlord = new UserEntity();
        landlord.setId(8);
        apartment.setUser(landlord);

        when(apartmentService.findById(20)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(reglaViviendaRepository.findByViviendaId(20)).thenReturn(Optional.empty());
        when(reglaViviendaRepository.save(any(ReglaViviendaEntity.class))).thenAnswer(inv -> {
            ReglaViviendaEntity e = inv.getArgument(0);
            e.setId(99);
            return e;
        });

        UpdateReglaVivienda request = new UpdateReglaVivienda(true, true, false);

        ReglaViviendaDTO result = reglaViviendaService.updateRules(20, request);

        assertEquals(20, result.viviendaId());
        assertTrue(result.permiteMascotas());
        assertTrue(result.permiteFumadores());
        assertFalse(result.fiestasPermitidas());
        verify(reglaViviendaRepository).save(any(ReglaViviendaEntity.class));
    }

    @Test
    @DisplayName("updateRules throws ForbiddenException when current user is not landlord")
    void updateRules_NonLandlord_ThrowsForbidden() {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(30);

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        apartment.setUser(landlord);

        UserEntity otherUser = new UserEntity();
        otherUser.setId(2);

        when(apartmentService.findById(30)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(otherUser);

        UpdateReglaVivienda request = new UpdateReglaVivienda(false, false, false);

        assertThrows(ForbiddenException.class, () -> reglaViviendaService.updateRules(30, request));
        verify(reglaViviendaRepository, never()).save(any(ReglaViviendaEntity.class));
    }
}

