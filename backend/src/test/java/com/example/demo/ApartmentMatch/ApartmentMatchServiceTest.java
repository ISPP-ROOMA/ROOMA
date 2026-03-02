package com.example.demo.ApartmentMatch;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@ExtendWith(MockitoExtension.class)
public class ApartmentMatchServiceTest {

    private ApartmentMatchService apartmentMatchService;

    @Mock
    private ApartmentMatchRepository apartmentMatchRepository;

    @Mock
    private ApartmentService apartmentService;

    @Mock
    private UserService userService;

    @BeforeEach
    public void setUp() {
        apartmentMatchService = new ApartmentMatchService(apartmentMatchRepository, apartmentService, userService);
    }

    @Test
    @DisplayName("processSwipe: interest=true creates ACTIVE match")
    public void processSwipe_InterestTrue_CreatesActiveMatch() {
        Integer apartmentId = 10;
        UserEntity tenant = createUser(1);
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE, 2);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(Optional.empty());
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(apartmentId, true);

        assertNotNull(result);
        assertEquals(tenant, result.getCandidate());
        assertEquals(apartment, result.getApartment());
        assertEquals(Boolean.TRUE, result.getCandidateInterest());
        assertNull(result.getLandlordInterest());
        assertEquals(MatchStatus.ACTIVE, result.getMatchStatus());
        assertNotNull(result.getMatchDate());
        verify(apartmentMatchRepository).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe: interest=false creates REJECTED match")
    public void processSwipe_InterestFalse_CreatesRejectedMatch() {
        Integer apartmentId = 11;
        UserEntity tenant = createUser(3);
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE, 4);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(Optional.empty());
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(apartmentId, false);

        assertNotNull(result);
        assertEquals(Boolean.FALSE, result.getCandidateInterest());
        assertNull(result.getLandlordInterest());
        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
        assertNotNull(result.getMatchDate());
        verify(apartmentMatchRepository).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe: throws when apartment does not exist")
    public void processSwipe_ApartmentNotFound_Throws() {
        Integer apartmentId = 12;
        UserEntity tenant = createUser(5);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> apartmentMatchService.processSwipe(apartmentId, true));
        assertNotNull(exception);
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe: throws when apartment is not ACTIVE")
    public void processSwipe_ApartmentNotActive_Throws() {
        Integer apartmentId = 13;
        UserEntity tenant = createUser(6);
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.CLOSED, 7);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> apartmentMatchService.processSwipe(apartmentId, true));
        assertNotNull(exception);
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe: throws when tenant swipes own apartment")
    public void processSwipe_OwnApartment_ThrowsAccessDenied() {
        Integer userId = 8;
        Integer apartmentId = 14;
        UserEntity tenant = createUser(userId);
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE, userId);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> apartmentMatchService.processSwipe(apartmentId, true));
        assertNotNull(exception);
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe: throws when tenant already swiped apartment")
    public void processSwipe_AlreadySwiped_Throws() {
        Integer apartmentId = 15;
        UserEntity tenant = createUser(9);
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE, 10);
        ApartmentMatchEntity existingMatch = new ApartmentMatchEntity();

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(tenant.getId(), apartmentId))
                .thenReturn(Optional.of(existingMatch));

        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> apartmentMatchService.processSwipe(apartmentId, false));
        assertNotNull(exception);
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    private UserEntity createUser(Integer id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        return user;
    }

    private ApartmentEntity createApartment(Integer apartmentId, ApartmentState state, Integer landlordId) {
        UserEntity landlord = createUser(landlordId);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setState(state);
        apartment.setUser(landlord);
        return apartment;
    }
}
