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
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.MemberApartment.MemberRole;
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
    private ApartmentMemberService apartmentMemberService;

    @Mock
    private UserService userService;

    @BeforeEach
    public void setUp() {
        apartmentMatchService = new ApartmentMatchService(apartmentMatchRepository, apartmentService, apartmentMemberService, userService);
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
        when(apartmentService.findById(apartmentId)).thenThrow(new ResourceNotFoundException("Apartment not found"));

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> apartmentMatchService.processSwipe(apartmentId, true));
        assertNotNull(exception);
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processLandlordAction: interest=true sets MATCH")
    public void processLandlordAction_InterestTrue_SetsMatch() {
        Integer matchId = 16;
        UserEntity landlord = createUser(20);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.ACTIVE, 21, 20, ApartmentState.ACTIVE);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processLandlordAction(matchId, true);

        assertEquals(Boolean.TRUE, result.getLandlordInterest());
        assertEquals(MatchStatus.MATCH, result.getMatchStatus());
        verify(apartmentMatchRepository).save(match);
    }

    @Test
    @DisplayName("processLandlordAction: throws when current user is not landlord")
    public void processLandlordAction_NotLandlord_ThrowsConflict() {
        Integer matchId = 17;
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.ACTIVE, 22, 23, ApartmentState.ACTIVE);
        UserEntity anotherUser = createUser(24);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(userService.findCurrentUserEntity()).thenReturn(anotherUser);

        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> apartmentMatchService.processLandlordAction(matchId, true));

        assertNotNull(exception);
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("sendInvitation: sets INVITED for MATCH and active apartment")
    public void sendInvitation_ValidMatch_SetsInvited() {
        Integer matchId = 18;
        UserEntity landlord = createUser(30);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.MATCH, 31, 30, ApartmentState.ACTIVE);

        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.sendInvitation(matchId);

        assertEquals(MatchStatus.INVITED, result.getMatchStatus());
        verify(apartmentMatchRepository).save(match);
    }

    @Test
    @DisplayName("sendInvitation: throws when current user is not landlord")
    public void sendInvitation_NotLandlord_ThrowsAccessDenied() {
        Integer matchId = 19;
        UserEntity notLandlord = createUser(33);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.MATCH, 32, 34, ApartmentState.ACTIVE);

        when(userService.findCurrentUserEntity()).thenReturn(notLandlord);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> apartmentMatchService.sendInvitation(matchId));

        assertNotNull(exception);
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("respondToInvitation: accepted adds members and sets SUCCESSFUL")
    public void respondToInvitation_Accepted_AddsMembersAndSetsSuccessful() {
        Integer matchId = 20;
        Integer candidateId = 40;
        Integer landlordId = 41;
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED, candidateId, landlordId, ApartmentState.ACTIVE);
        UserEntity candidate = createUser(candidateId);

        when(userService.findCurrentUserEntity()).thenReturn(candidate);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(apartmentMemberService.existsByUserIdAndRole(landlordId, MemberRole.HOMEBODY)).thenReturn(false);
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.respondToInvitation(matchId, true);

        assertEquals(MatchStatus.SUCCESSFUL, result.getMatchStatus());
        verify(apartmentMemberService).addMember(match.getApartment().getId(), landlordId, MemberRole.HOMEBODY, null);
        verify(apartmentMemberService).addMember(match.getApartment().getId(), candidateId, MemberRole.RENTER, null);
        verify(apartmentMatchRepository).save(match);
    }

    @Test
    @DisplayName("respondToInvitation: rejected sets REJECTED and does not add members")
    public void respondToInvitation_Rejected_SetsRejectedWithoutMembers() {
        Integer matchId = 21;
        Integer candidateId = 50;
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED, candidateId, 51, ApartmentState.ACTIVE);
        UserEntity candidate = createUser(candidateId);

        when(userService.findCurrentUserEntity()).thenReturn(candidate);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.respondToInvitation(matchId, false);

        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
        verify(apartmentMemberService, never()).addMember(any(Integer.class), any(Integer.class), any(MemberRole.class), any());
        verify(apartmentMatchRepository).save(match);
    }

    @Test
    @DisplayName("respondToInvitation: throws when current user is not candidate")
    public void respondToInvitation_NotCandidate_ThrowsAccessDenied() {
        Integer matchId = 22;
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED, 60, 61, ApartmentState.ACTIVE);
        UserEntity otherUser = createUser(62);

        when(userService.findCurrentUserEntity()).thenReturn(otherUser);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> apartmentMatchService.respondToInvitation(matchId, true));

        assertNotNull(exception);
        verify(apartmentMemberService, never()).addMember(any(Integer.class), any(Integer.class), any(MemberRole.class), any());
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

    private ApartmentMatchEntity createMatch(Integer id, MatchStatus status, Integer candidateId, Integer landlordId, ApartmentState apartmentState) {
        ApartmentMatchEntity match = new ApartmentMatchEntity();
        match.setId(id);
        match.setCandidate(createUser(candidateId));
        match.setApartment(createApartment(100 + id, apartmentState, landlordId));
        match.setMatchStatus(status);
        return match;
    }
}
