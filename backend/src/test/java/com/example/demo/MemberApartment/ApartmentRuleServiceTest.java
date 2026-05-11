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
import com.example.demo.MemberApartment.DTOs.ApartmentRuleDTO;
import com.example.demo.MemberApartment.DTOs.UpdateApartmentRule;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@ExtendWith(MockitoExtension.class)
class ApartmentRuleServiceTest {

    private ApartmentRuleService apartmentRuleService;

    @Mock
    private ApartmentRuleRepository apartmentRuleRepository;

    @Mock
    private ApartmentService apartmentService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        apartmentRuleService = new ApartmentRuleService(apartmentRuleRepository, apartmentService, userService);
    }

    @Test
    @DisplayName("updateRules updates existing rules when current user is landlord")
    void updateRules_UpdatesExistingWhenLandlord() {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(10);

        UserEntity landlord = new UserEntity();
        landlord.setId(5);
        apartment.setUser(landlord);

        ApartmentRuleEntity existing = new ApartmentRuleEntity();
        existing.setApartment(apartment);

        when(apartmentService.findById(10)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentRuleRepository.findByApartmentId(10)).thenReturn(Optional.of(existing));
        when(apartmentRuleRepository.save(any(ApartmentRuleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateApartmentRule request = new UpdateApartmentRule(true, false, true);

        ApartmentRuleDTO result = apartmentRuleService.updateRules(10, request);

        assertEquals(10, result.apartmentId());
        assertTrue(result.allowsPets());
        assertFalse(result.allowsSmokers());
        assertTrue(result.partiesAllowed());
        verify(apartmentRuleRepository).save(existing);
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
        when(apartmentRuleRepository.findByApartmentId(20)).thenReturn(Optional.empty());
        when(apartmentRuleRepository.save(any(ApartmentRuleEntity.class))).thenAnswer(inv -> {
            ApartmentRuleEntity e = inv.getArgument(0);
            e.setId(99);
            return e;
        });

        UpdateApartmentRule request = new UpdateApartmentRule(true, true, false);

        ApartmentRuleDTO result = apartmentRuleService.updateRules(20, request);

        assertEquals(20, result.apartmentId());
        assertTrue(result.allowsPets());
        assertTrue(result.allowsSmokers());
        assertFalse(result.partiesAllowed());
        verify(apartmentRuleRepository).save(any(ApartmentRuleEntity.class));
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

        UpdateApartmentRule request = new UpdateApartmentRule(false, false, false);

        assertThrows(ForbiddenException.class, () -> apartmentRuleService.updateRules(30, request));
        verify(apartmentRuleRepository, never()).save(any(ApartmentRuleEntity.class));
    }
}

