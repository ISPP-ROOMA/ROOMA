package com.example.demo.ApartmentMatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.MemberApartment.MemberRole;
import com.example.demo.User.Role;
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
        apartmentMatchService = new ApartmentMatchService(
                apartmentMatchRepository,
                apartmentService,
                apartmentMemberService,
                userService);
    }

    @Test
    @DisplayName("processSwipe tenant creates ACTIVE match when apartment is active and interest is true")
    public void processSwipe_InterestTrue_CreatesActiveMatch() {
        Integer apartmentId = 10;
        UserEntity tenant = createUser(1, Role.TENANT, "tenant1@test.com");
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE,
                createUser(2, Role.LANDLORD, "landlord@test.com"));

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(tenant.getId(), apartmentId))
                .thenReturn(Optional.empty());
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(apartmentId, true);

        assertEquals(tenant, result.getCandidate());
        assertEquals(apartment, result.getApartment());
        assertEquals(Boolean.TRUE, result.getCandidateInterest());
        assertNull(result.getLandlordInterest());
        assertEquals(MatchStatus.ACTIVE, result.getMatchStatus());
        assertNotNull(result.getMatchDate());
        verify(apartmentMatchRepository).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe tenant creates REJECTED match when interest is false")
    public void processSwipe_InterestFalse_CreatesRejectedMatch() {
        Integer apartmentId = 11;
        UserEntity tenant = createUser(3, Role.TENANT, "tenant2@test.com");
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE,
                createUser(4, Role.LANDLORD, "landlord2@test.com"));

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(tenant.getId(), apartmentId))
                .thenReturn(Optional.empty());
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(apartmentId, false);

        assertEquals(Boolean.FALSE, result.getCandidateInterest());
        assertNull(result.getLandlordInterest());
        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
        assertNotNull(result.getMatchDate());
    }

    @Test
    @DisplayName("processSwipe tenant throws when apartment does not exist")
    public void processSwipe_ApartmentNotFound_Throws() {
        Integer apartmentId = 12;
        UserEntity tenant = createUser(5, Role.TENANT, "tenant3@test.com");

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenThrow(new ResourceNotFoundException("Apartment not found"));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> apartmentMatchService.processSwipe(apartmentId, true));

        assertEquals("Apartment not found", exception.getMessage());
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe tenant throws when apartment is not ACTIVE")
    public void processSwipe_ApartmentNotActive_Throws() {
        Integer apartmentId = 13;
        UserEntity tenant = createUser(6, Role.TENANT, "tenant4@test.com");
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.CLOSED,
                createUser(7, Role.LANDLORD, "landlord3@test.com"));

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.processSwipe(apartmentId, true));

        assertEquals("Cannot swipe on an apartment that is not active", exception.getMessage());
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe tenant throws when swiping own apartment")
    public void processSwipe_OwnApartment_ThrowsAccessDenied() {
        Integer userId = 8;
        Integer apartmentId = 14;
        UserEntity tenant = createUser(userId, Role.TENANT, "tenant5@test.com");
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE, tenant);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> apartmentMatchService.processSwipe(apartmentId, true));

        assertEquals("You cannot swipe on your own apartment", exception.getMessage());
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processSwipe tenant throws when user already swiped apartment")
    public void processSwipe_AlreadySwiped_Throws() {
        Integer apartmentId = 15;
        UserEntity tenant = createUser(9, Role.TENANT, "tenant6@test.com");
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE,
                createUser(10, Role.LANDLORD, "landlord4@test.com"));
        ApartmentMatchEntity existingMatch = createMatch(100, MatchStatus.ACTIVE, tenant, apartment, true, null);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(tenant.getId(), apartmentId))
                .thenReturn(Optional.of(existingMatch));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.processSwipe(apartmentId, false));

        assertEquals("You have already swiped on this apartment", exception.getMessage());
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processLandlordAction sets MATCH when landlord is interested")
    public void processLandlordAction_InterestTrue_SetsMatch() {
        Integer matchId = 16;
        UserEntity landlord = createUser(20, Role.LANDLORD, "landlord5@test.com");
        UserEntity candidate = createUser(21, Role.TENANT, "tenant7@test.com");
        ApartmentEntity apartment = createApartment(116, ApartmentState.ACTIVE, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.ACTIVE, candidate, apartment, true, null);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.save(match)).thenReturn(match);

        ApartmentMatchEntity result = apartmentMatchService.processLandlordAction(matchId, true);

        assertEquals(Boolean.TRUE, result.getLandlordInterest());
        assertEquals(MatchStatus.MATCH, result.getMatchStatus());
        verify(apartmentMatchRepository).save(match);
    }

    @Test
    @DisplayName("processLandlordAction sets REJECTED when landlord is not interested")
    public void processLandlordAction_InterestFalse_SetsRejected() {
        Integer matchId = 17;
        UserEntity landlord = createUser(22, Role.LANDLORD, "landlord6@test.com");
        ApartmentEntity apartment = createApartment(117, ApartmentState.ACTIVE, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.ACTIVE,
                createUser(23, Role.TENANT, "tenant8@test.com"), apartment, true, null);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.save(match)).thenReturn(match);

        ApartmentMatchEntity result = apartmentMatchService.processLandlordAction(matchId, false);

        assertEquals(Boolean.FALSE, result.getLandlordInterest());
        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
    }

    @Test
    @DisplayName("processLandlordAction throws when match does not exist")
    public void processLandlordAction_MatchNotFound_Throws() {
        when(apartmentMatchRepository.findById(18)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> apartmentMatchService.processLandlordAction(18, true));

        assertEquals("Match not found", exception.getMessage());
    }

    @Test
    @DisplayName("processLandlordAction throws when current user is not apartment landlord")
    public void processLandlordAction_NotLandlord_ThrowsConflict() {
        Integer matchId = 19;
        ApartmentEntity apartment = createApartment(119, ApartmentState.ACTIVE,
                createUser(24, Role.LANDLORD, "landlord7@test.com"));
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.ACTIVE,
                createUser(25, Role.TENANT, "tenant9@test.com"), apartment, true, null);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(userService.findCurrentUserEntity()).thenReturn(createUser(26, Role.LANDLORD, "other@test.com"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.processLandlordAction(matchId, true));

        assertEquals("Only the landlord of the apartment can process this action", exception.getMessage());
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("processLandlordAction throws when match status is not ACTIVE")
    public void processLandlordAction_InvalidStatus_ThrowsConflict() {
        Integer matchId = 20;
        UserEntity landlord = createUser(27, Role.LANDLORD, "landlord8@test.com");
        ApartmentEntity apartment = createApartment(120, ApartmentState.ACTIVE, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.MATCH,
                createUser(28, Role.TENANT, "tenant10@test.com"), apartment, true, true);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.processLandlordAction(matchId, true));

        assertEquals("Only matches with status ACTIVE can be processed by the landlord", exception.getMessage());
    }

    @Test
    @DisplayName("processLandlordAction throws when apartment is not ACTIVE")
    public void processLandlordAction_ApartmentNotActive_ThrowsConflict() {
        Integer matchId = 21;
        UserEntity landlord = createUser(29, Role.LANDLORD, "landlord9@test.com");
        ApartmentEntity apartment = createApartment(121, ApartmentState.MATCHING, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.ACTIVE,
                createUser(30, Role.TENANT, "tenant11@test.com"), apartment, true, null);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(userService.findCurrentUserEntity()).thenReturn(landlord);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.processLandlordAction(matchId, true));

        assertEquals("Cannot process the match because the apartment is not active", exception.getMessage());
    }

    @Test
    @DisplayName("sendInvitation sets INVITED for landlord on MATCH and active apartment")
    public void sendInvitation_ValidMatch_SetsInvited() {
        Integer matchId = 22;
        UserEntity landlord = createUser(31, Role.LANDLORD, "landlord10@test.com");
        ApartmentEntity apartment = createApartment(122, ApartmentState.ACTIVE, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.MATCH,
                createUser(32, Role.TENANT, "tenant12@test.com"), apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(apartmentMatchRepository.save(match)).thenReturn(match);

        ApartmentMatchEntity result = apartmentMatchService.sendInvitation(matchId);

        assertEquals(MatchStatus.INVITED, result.getMatchStatus());
        verify(apartmentMatchRepository).save(match);
    }

    @Test
    @DisplayName("sendInvitation throws when current user is not landlord")
    public void sendInvitation_NotLandlord_ThrowsAccessDenied() {
        Integer matchId = 23;
        ApartmentEntity apartment = createApartment(123, ApartmentState.ACTIVE,
                createUser(33, Role.LANDLORD, "landlord11@test.com"));
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.MATCH,
                createUser(34, Role.TENANT, "tenant13@test.com"), apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(createUser(35, Role.LANDLORD, "other2@test.com"));
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> apartmentMatchService.sendInvitation(matchId));

        assertEquals("Only the landlord of the apartment can send an invitation", exception.getMessage());
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("sendInvitation throws when match status is not MATCH")
    public void sendInvitation_InvalidStatus_ThrowsConflict() {
        Integer matchId = 24;
        UserEntity landlord = createUser(36, Role.LANDLORD, "landlord12@test.com");
        ApartmentEntity apartment = createApartment(124, ApartmentState.ACTIVE, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.ACTIVE,
                createUser(37, Role.TENANT, "tenant14@test.com"), apartment, true, null);

        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.sendInvitation(matchId));

        assertEquals("Only matches with status MATCH can be invited", exception.getMessage());
    }

    @Test
    @DisplayName("sendInvitation throws when apartment is not ACTIVE")
    public void sendInvitation_ApartmentNotActive_ThrowsConflict() {
        Integer matchId = 25;
        UserEntity landlord = createUser(38, Role.LANDLORD, "landlord13@test.com");
        ApartmentEntity apartment = createApartment(125, ApartmentState.CLOSED, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.MATCH,
                createUser(39, Role.TENANT, "tenant15@test.com"), apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.sendInvitation(matchId));

        assertEquals("Cannot send an invitation because the apartment is not active", exception.getMessage());
    }

    @Test
    @DisplayName("respondToInvitation accepted adds landlord and tenant when landlord is not already HOMEBODY")
    public void respondToInvitation_Accepted_AddsMembersAndSetsSuccessful() {
        Integer matchId = 26;
        UserEntity landlord = createUser(40, Role.LANDLORD, "landlord14@test.com");
        UserEntity candidate = createUser(41, Role.TENANT, "tenant16@test.com");
        ApartmentEntity apartment = createApartment(126, ApartmentState.ACTIVE, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED, candidate, apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(candidate);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(apartmentMemberService.existsByUserIdAndRole(landlord.getId(), MemberRole.HOMEBODY)).thenReturn(false);
        when(apartmentMatchRepository.save(match)).thenReturn(match);

        ApartmentMatchEntity result = apartmentMatchService.respondToInvitation(matchId, true);

        assertEquals(MatchStatus.SUCCESSFUL, result.getMatchStatus());
        verify(apartmentMemberService).addMember(apartment.getId(), landlord.getId(), null);
        verify(apartmentMemberService).addMember(apartment.getId(), candidate.getId(), null);
        verify(apartmentMatchRepository).save(match);
    }

    @Test
    @DisplayName("respondToInvitation accepted skips landlord membership creation when already HOMEBODY")
    public void respondToInvitation_Accepted_LandlordAlreadyHomebody_AddsOnlyCandidate() {
        Integer matchId = 27;
        UserEntity landlord = createUser(42, Role.LANDLORD, "landlord15@test.com");
        UserEntity candidate = createUser(43, Role.TENANT, "tenant17@test.com");
        ApartmentEntity apartment = createApartment(127, ApartmentState.ACTIVE, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED, candidate, apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(candidate);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(apartmentMemberService.existsByUserIdAndRole(landlord.getId(), MemberRole.HOMEBODY)).thenReturn(true);
        when(apartmentMatchRepository.save(match)).thenReturn(match);

        ApartmentMatchEntity result = apartmentMatchService.respondToInvitation(matchId, true);

        assertEquals(MatchStatus.SUCCESSFUL, result.getMatchStatus());
        verify(apartmentMemberService, never()).addMember(apartment.getId(), landlord.getId(), null);
        verify(apartmentMemberService).addMember(apartment.getId(), candidate.getId(), null);
    }

    @Test
    @DisplayName("respondToInvitation rejected sets REJECTED and does not add members")
    public void respondToInvitation_Rejected_SetsRejectedWithoutMembers() {
        Integer matchId = 28;
        UserEntity candidate = createUser(44, Role.TENANT, "tenant18@test.com");
        ApartmentEntity apartment = createApartment(128, ApartmentState.ACTIVE,
                createUser(45, Role.LANDLORD, "landlord16@test.com"));
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED, candidate, apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(candidate);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(apartmentMatchRepository.save(match)).thenReturn(match);

        ApartmentMatchEntity result = apartmentMatchService.respondToInvitation(matchId, false);

        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
        verify(apartmentMemberService, never()).addMember(any(Integer.class), any(Integer.class), any());
    }

    @Test
    @DisplayName("respondToInvitation throws when current user is not candidate")
    public void respondToInvitation_NotCandidate_ThrowsAccessDenied() {
        Integer matchId = 29;
        ApartmentEntity apartment = createApartment(129, ApartmentState.ACTIVE,
                createUser(46, Role.LANDLORD, "landlord17@test.com"));
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED,
                createUser(47, Role.TENANT, "tenant19@test.com"), apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(createUser(48, Role.TENANT, "other3@test.com"));
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> apartmentMatchService.respondToInvitation(matchId, true));

        assertEquals("Only the candidate can respond to the invitation", exception.getMessage());
        verify(apartmentMemberService, never()).addMember(any(Integer.class), any(Integer.class), any());
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("respondToInvitation throws when match status is not INVITED")
    public void respondToInvitation_InvalidStatus_ThrowsConflict() {
        Integer matchId = 30;
        UserEntity candidate = createUser(49, Role.TENANT, "tenant20@test.com");
        ApartmentEntity apartment = createApartment(130, ApartmentState.ACTIVE,
                createUser(50, Role.LANDLORD, "landlord18@test.com"));
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.MATCH, candidate, apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(candidate);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.respondToInvitation(matchId, true));

        assertEquals("Only matches with status INVITED can be responded to", exception.getMessage());
    }

    @Test
    @DisplayName("respondToInvitation throws when apartment is not ACTIVE while accepting")
    public void respondToInvitation_Accepted_ApartmentNotActive_ThrowsConflict() {
        Integer matchId = 31;
        UserEntity candidate = createUser(51, Role.TENANT, "tenant21@test.com");
        ApartmentEntity apartment = createApartment(131, ApartmentState.CLOSED,
                createUser(52, Role.LANDLORD, "landlord19@test.com"));
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED, candidate, apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(candidate);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.respondToInvitation(matchId, true));

        assertEquals("Cannot accept the invitation because the apartment is not active", exception.getMessage());
    }

    @Test
    @DisplayName("respondToInvitation propagates ApartmentMemberService errors")
    public void respondToInvitation_AddMemberFails_PropagatesError() {
        Integer matchId = 32;
        UserEntity landlord = createUser(53, Role.LANDLORD, "landlord20@test.com");
        UserEntity candidate = createUser(54, Role.TENANT, "tenant22@test.com");
        ApartmentEntity apartment = createApartment(132, ApartmentState.ACTIVE, landlord);
        ApartmentMatchEntity match = createMatch(matchId, MatchStatus.INVITED, candidate, apartment, true, true);

        when(userService.findCurrentUserEntity()).thenReturn(candidate);
        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(apartmentMemberService.existsByUserIdAndRole(landlord.getId(), MemberRole.HOMEBODY)).thenReturn(false);
        when(apartmentMemberService.addMember(apartment.getId(), landlord.getId(), null))
                .thenThrow(new BadRequestException("User already belongs to this apartment"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> apartmentMatchService.respondToInvitation(matchId, true));

        assertEquals("User already belongs to this apartment", exception.getMessage());
        verify(apartmentMatchRepository, never()).save(any(ApartmentMatchEntity.class));
    }

    @Test
    @DisplayName("successfulMatch sets SUCCESSFUL when match is in MATCH state")
    public void successfulMatch_MatchState_SetsSuccessful() {
        ApartmentMatchEntity match = createMatch(33, MatchStatus.MATCH,
                createUser(55, Role.TENANT, "tenant23@test.com"),
                createApartment(133, ApartmentState.ACTIVE, createUser(56, Role.LANDLORD, "landlord21@test.com")),
                true, true);

        when(apartmentMatchRepository.findById(33)).thenReturn(Optional.of(match));
        when(apartmentMatchRepository.save(match)).thenReturn(match);

        ApartmentMatchEntity result = apartmentMatchService.successfulMatch(33);

        assertEquals(MatchStatus.SUCCESSFUL, result.getMatchStatus());
        verify(apartmentMatchRepository).save(match);
    }

    @Test
    @DisplayName("successfulMatch throws when status is not MATCH")
    public void successfulMatch_InvalidStatus_ThrowsConflict() {
        ApartmentMatchEntity match = createMatch(34, MatchStatus.ACTIVE,
                createUser(57, Role.TENANT, "tenant24@test.com"),
                createApartment(134, ApartmentState.ACTIVE, createUser(58, Role.LANDLORD, "landlord22@test.com")),
                true, null);

        when(apartmentMatchRepository.findById(34)).thenReturn(Optional.of(match));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.successfulMatch(34));

        assertEquals("Only matches with status MATCH can be finalized as successful", exception.getMessage());
    }

    @Test
    @DisplayName("cancelMatch sets CANCELED when match is in MATCH state")
    public void cancelMatch_MatchState_SetsCanceled() {
        ApartmentMatchEntity match = createMatch(35, MatchStatus.MATCH,
                createUser(59, Role.TENANT, "tenant25@test.com"),
                createApartment(135, ApartmentState.ACTIVE, createUser(60, Role.LANDLORD, "landlord23@test.com")),
                true, true);

        when(apartmentMatchRepository.findById(35)).thenReturn(Optional.of(match));
        when(apartmentMatchRepository.save(match)).thenReturn(match);

        ApartmentMatchEntity result = apartmentMatchService.cancelMatch(35);

        assertEquals(MatchStatus.CANCELED, result.getMatchStatus());
    }

    @Test
    @DisplayName("cancelMatch throws when status is ACTIVE")
    public void cancelMatch_InvalidStatus_ThrowsConflict() {
        ApartmentMatchEntity match = createMatch(36, MatchStatus.ACTIVE,
                createUser(61, Role.TENANT, "tenant26@test.com"),
                createApartment(136, ApartmentState.ACTIVE, createUser(62, Role.LANDLORD, "landlord24@test.com")),
                true, null);

        when(apartmentMatchRepository.findById(36)).thenReturn(Optional.of(match));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.cancelMatch(36));

        assertEquals("Only matches with status MATCH can be canceled", exception.getMessage());
    }

    @Test
    @DisplayName("findInterestedCandidatesByApartmentIdAndStatus returns matches for apartment landlord")
    public void findInterestedCandidatesByApartmentIdAndStatus_Landlord_ReturnsMatches() {
        UserEntity landlord = createUser(63, Role.LANDLORD, "landlord25@test.com");
        ApartmentEntity apartment = createApartment(137, ApartmentState.ACTIVE, landlord);
        List<ApartmentMatchEntity> matches = List.of(createMatch(37, MatchStatus.ACTIVE,
                createUser(64, Role.TENANT, "tenant27@test.com"), apartment, true, null));

        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);
        when(apartmentMatchRepository.findByApartmentIdAndMatchStatus(apartment.getId(), MatchStatus.ACTIVE))
                .thenReturn(matches);

        List<ApartmentMatchEntity> result = apartmentMatchService
                .findInterestedCandidatesByApartmentIdAndStatus(apartment.getId(), MatchStatus.ACTIVE);

        assertSame(matches, result);
    }

    @Test
    @DisplayName("findInterestedCandidatesByApartmentIdAndStatus throws for non landlord owner mismatch")
    public void findInterestedCandidatesByApartmentIdAndStatus_NotOwner_ThrowsConflict() {
        ApartmentEntity apartment = createApartment(138, ApartmentState.ACTIVE,
                createUser(65, Role.LANDLORD, "landlord26@test.com"));

        when(userService.findCurrentUserEntity()).thenReturn(createUser(66, Role.LANDLORD, "other4@test.com"));
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.findInterestedCandidatesByApartmentIdAndStatus(apartment.getId(),
                        MatchStatus.ACTIVE));

        assertEquals("Only the landlord of the apartment can view the interested candidates", exception.getMessage());
    }

    @Test
    @DisplayName("findInterestedCandidatesByUserIdAndStatus returns matches only for current user id")
    public void findInterestedCandidatesByUserIdAndStatus_SameUser_ReturnsMatches() {
        UserEntity landlord = createUser(67, Role.LANDLORD, "landlord27@test.com");
        List<ApartmentMatchEntity> matches = List.of(createMatch(38, MatchStatus.MATCH,
                createUser(68, Role.TENANT, "tenant28@test.com"),
                createApartment(139, ApartmentState.ACTIVE, landlord), true, true));

        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.findByUserIdAndMatchStatus(landlord.getId(), MatchStatus.MATCH))
                .thenReturn(matches);

        List<ApartmentMatchEntity> result = apartmentMatchService.findInterestedCandidatesByUserIdAndStatus(
                landlord.getId(), MatchStatus.MATCH);

        assertSame(matches, result);
    }

    @Test
    @DisplayName("findInterestedCandidatesByUserIdAndStatus throws for other user id")
    public void findInterestedCandidatesByUserIdAndStatus_OtherUser_ThrowsConflict() {
        when(userService.findCurrentUserEntity()).thenReturn(createUser(69, Role.LANDLORD, "landlord28@test.com"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.findInterestedCandidatesByUserIdAndStatus(70, MatchStatus.MATCH));

        assertEquals("You can only view your own interested candidates", exception.getMessage());
    }

    @Test
    @DisplayName("findTenantRequestByUserIdAndStatus returns requests for current user")
    public void findTenantRequestByUserIdAndStatus_ReturnsMatches() {
        UserEntity tenant = createUser(71, Role.TENANT, "tenant29@test.com");
        List<ApartmentMatchEntity> matches = List.of(createMatch(39, MatchStatus.ACTIVE, tenant,
                createApartment(140, ApartmentState.ACTIVE, createUser(72, Role.LANDLORD, "landlord29@test.com")),
                true, null));

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentMatchRepository.findTenantRequestByUserIdAndStatus(tenant.getId(), MatchStatus.ACTIVE))
                .thenReturn(matches);

        List<ApartmentMatchEntity> result = apartmentMatchService.findTenantRequestByUserIdAndStatus(MatchStatus.ACTIVE);

        assertSame(matches, result);
    }

    @Test
    @DisplayName("findMyMatchForTenant returns match when current user is candidate")
    public void findMyMatchForTenant_Candidate_ReturnsMatch() {
        UserEntity tenant = createUser(73, Role.TENANT, "tenant30@test.com");
        ApartmentMatchEntity match = createMatch(40, MatchStatus.INVITED, tenant,
                createApartment(141, ApartmentState.ACTIVE, createUser(74, Role.LANDLORD, "landlord30@test.com")),
                true, true);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentMatchRepository.findById(40)).thenReturn(Optional.of(match));

        ApartmentMatchEntity result = apartmentMatchService.findMyMatchForTenant(40);

        assertSame(match, result);
    }

    @Test
    @DisplayName("findMyMatchForTenant throws when current user is not candidate")
    public void findMyMatchForTenant_NotCandidate_ThrowsAccessDenied() {
        ApartmentMatchEntity match = createMatch(41, MatchStatus.INVITED,
                createUser(75, Role.TENANT, "tenant31@test.com"),
                createApartment(142, ApartmentState.ACTIVE, createUser(76, Role.LANDLORD, "landlord31@test.com")),
                true, true);

        when(userService.findCurrentUserEntity()).thenReturn(createUser(77, Role.TENANT, "other5@test.com"));
        when(apartmentMatchRepository.findById(41)).thenReturn(Optional.of(match));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> apartmentMatchService.findMyMatchForTenant(41));

        assertEquals("You can only view your own matches", exception.getMessage());
    }

    @Test
    @DisplayName("findMyMatchForLandlord returns match when current user owns apartment")
    public void findMyMatchForLandlord_Landlord_ReturnsMatch() {
        UserEntity landlord = createUser(78, Role.LANDLORD, "landlord32@test.com");
        ApartmentMatchEntity match = createMatch(42, MatchStatus.ACTIVE,
                createUser(79, Role.TENANT, "tenant32@test.com"),
                createApartment(143, ApartmentState.ACTIVE, landlord),
                true, null);

        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMatchRepository.findById(42)).thenReturn(Optional.of(match));

        ApartmentMatchEntity result = apartmentMatchService.findMyMatchForLandlord(42);

        assertSame(match, result);
    }

    @Test
    @DisplayName("findMyMatchForLandlord throws when current user does not own apartment")
    public void findMyMatchForLandlord_NotOwner_ThrowsAccessDenied() {
        ApartmentMatchEntity match = createMatch(43, MatchStatus.ACTIVE,
                createUser(80, Role.TENANT, "tenant33@test.com"),
                createApartment(144, ApartmentState.ACTIVE, createUser(81, Role.LANDLORD, "landlord33@test.com")),
                true, null);

        when(userService.findCurrentUserEntity()).thenReturn(createUser(82, Role.LANDLORD, "other6@test.com"));
        when(apartmentMatchRepository.findById(43)).thenReturn(Optional.of(match));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> apartmentMatchService.findMyMatchForLandlord(43));

        assertEquals("You can only view matches for your own apartments", exception.getMessage());
    }

    @Test
    @DisplayName("findApartmentMatchByCandidateAndApartment returns repository match when candidate and apartment exist")
    public void findApartmentMatchByCandidateAndApartment_ReturnsMatch() {
        UserEntity candidate = createUser(83, Role.TENANT, "tenant34@test.com");
        ApartmentEntity apartment = createApartment(145, ApartmentState.ACTIVE,
                createUser(84, Role.LANDLORD, "landlord34@test.com"));
        ApartmentMatchEntity match = createMatch(44, MatchStatus.ACTIVE, candidate, apartment, true, null);

        when(userService.findById(candidate.getId())).thenReturn(candidate);
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidate.getId(), apartment.getId()))
                .thenReturn(Optional.of(match));

        ApartmentMatchEntity result = apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidate.getId(),
                apartment.getId());

        assertSame(match, result);
    }

    @Test
    @DisplayName("findApartmentMatchByCandidateAndApartment throws when repository has no match")
    public void findApartmentMatchByCandidateAndApartment_NotFound_Throws() {
        UserEntity candidate = createUser(85, Role.TENANT, "tenant35@test.com");
        ApartmentEntity apartment = createApartment(146, ApartmentState.ACTIVE,
                createUser(86, Role.LANDLORD, "landlord35@test.com"));

        when(userService.findById(candidate.getId())).thenReturn(candidate);
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidate.getId(), apartment.getId()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidate.getId(),
                        apartment.getId()));

        assertEquals("Apartment match not found for the given candidate and apartment", exception.getMessage());
    }

    @Test
    @DisplayName("finalizeMatchProcess deletes matches and closes apartment when state is not MATCHING")
    public void finalizeMatchProcess_DeletesMatchesAndClosesApartment() {
        Integer apartmentId = 147;
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE,
                createUser(87, Role.LANDLORD, "landlord36@test.com"));
        List<ApartmentMatchEntity> matches = List.of(createMatch(45, MatchStatus.ACTIVE,
                createUser(88, Role.TENANT, "tenant36@test.com"), apartment, true, null));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMatchRepository.findByApartmentId(apartmentId)).thenReturn(matches);

        apartmentMatchService.finalizeMatchProcess(apartmentId);

        assertEquals(ApartmentState.CLOSED, apartment.getState());
        verify(apartmentMatchRepository).deleteAll(matches);
        verify(apartmentService).save(apartment);
    }

    @Test
    @DisplayName("finalizeMatchProcess throws when apartment is MATCHING")
    public void finalizeMatchProcess_MatchingApartment_ThrowsConflict() {
        Integer apartmentId = 148;
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.MATCHING,
                createUser(89, Role.LANDLORD, "landlord37@test.com"));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.finalizeMatchProcess(apartmentId));

        assertEquals("Only apartments that are not matching can be finalized", exception.getMessage());
        verify(apartmentMatchRepository, never()).deleteAll(any());
        verify(apartmentService, never()).save(any(ApartmentEntity.class));
    }

    @Test
    @DisplayName("finalizeMatchProcess throws when apartment has no matches")
    public void finalizeMatchProcess_NoMatches_ThrowsNotFound() {
        Integer apartmentId = 149;
        ApartmentEntity apartment = createApartment(apartmentId, ApartmentState.ACTIVE,
                createUser(90, Role.LANDLORD, "landlord38@test.com"));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMatchRepository.findByApartmentId(apartmentId)).thenReturn(List.of());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> apartmentMatchService.finalizeMatchProcess(apartmentId));

        assertEquals("No matches found for this apartment", exception.getMessage());
    }

    @Test
    @DisplayName("legacy processSwipe creates first interaction for candidate")
    public void legacyProcessSwipe_FirstCandidateInteraction_CreatesActiveMatch() {
        UserEntity candidate = createUser(91, Role.TENANT, "tenant37@test.com");
        ApartmentEntity apartment = createApartment(150, ApartmentState.ACTIVE,
                createUser(92, Role.LANDLORD, "landlord39@test.com"));

        when(userService.findById(candidate.getId())).thenReturn(candidate);
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidate.getId(), apartment.getId()))
                .thenReturn(Optional.empty());
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidate.getId(), apartment.getId(), true,
                true);

        assertEquals(Boolean.TRUE, result.getCandidateInterest());
        assertNull(result.getLandlordInterest());
        assertEquals(MatchStatus.ACTIVE, result.getMatchStatus());
    }

    @Test
    @DisplayName("legacy processSwipe throws on duplicate candidate interaction")
    public void legacyProcessSwipe_DuplicateCandidateInteraction_ThrowsConflict() {
        UserEntity candidate = createUser(93, Role.TENANT, "tenant38@test.com");
        ApartmentEntity apartment = createApartment(151, ApartmentState.ACTIVE,
                createUser(94, Role.LANDLORD, "landlord40@test.com"));
        ApartmentMatchEntity existingMatch = createMatch(46, MatchStatus.ACTIVE, candidate, apartment, true, null);

        when(userService.findById(candidate.getId())).thenReturn(candidate);
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidate.getId(), apartment.getId()))
                .thenReturn(Optional.of(existingMatch));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.processSwipe(candidate.getId(), apartment.getId(), true, false));

        assertEquals("Candidate has already swiped on this apartment", exception.getMessage());
    }

    @Test
    @DisplayName("legacy processSwipe sets MATCH when second positive interaction arrives")
    public void legacyProcessSwipe_SecondPositiveInteraction_SetsMatch() {
        UserEntity candidate = createUser(95, Role.TENANT, "tenant39@test.com");
        ApartmentEntity apartment = createApartment(152, ApartmentState.ACTIVE,
                createUser(96, Role.LANDLORD, "landlord41@test.com"));
        ApartmentMatchEntity existingMatch = createMatch(47, MatchStatus.ACTIVE, candidate, apartment, true, null);

        when(userService.findById(candidate.getId())).thenReturn(candidate);
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidate.getId(), apartment.getId()))
                .thenReturn(Optional.of(existingMatch));
        when(apartmentMatchRepository.save(existingMatch)).thenReturn(existingMatch);

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidate.getId(), apartment.getId(), false,
                true);

        assertEquals(Boolean.TRUE, result.getLandlordInterest());
        assertEquals(MatchStatus.MATCH, result.getMatchStatus());
    }

    @Test
    @DisplayName("legacy processSwipe sets REJECTED when second interaction is negative")
    public void legacyProcessSwipe_NegativeSecondInteraction_SetsRejected() {
        UserEntity candidate = createUser(97, Role.TENANT, "tenant40@test.com");
        ApartmentEntity apartment = createApartment(153, ApartmentState.ACTIVE,
                createUser(98, Role.LANDLORD, "landlord42@test.com"));
        ApartmentMatchEntity existingMatch = createMatch(48, MatchStatus.ACTIVE, candidate, apartment, true, null);

        when(userService.findById(candidate.getId())).thenReturn(candidate);
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidate.getId(), apartment.getId()))
                .thenReturn(Optional.of(existingMatch));
        when(apartmentMatchRepository.save(existingMatch)).thenReturn(existingMatch);

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidate.getId(), apartment.getId(), false,
                false);

        assertEquals(Boolean.FALSE, result.getLandlordInterest());
        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
    }

    @Test
    @DisplayName("legacy processSwipe blocks interaction on terminal states")
    public void legacyProcessSwipe_TerminalState_ThrowsConflict() {
        UserEntity candidate = createUser(99, Role.TENANT, "tenant41@test.com");
        ApartmentEntity apartment = createApartment(154, ApartmentState.ACTIVE,
                createUser(100, Role.LANDLORD, "landlord43@test.com"));
        ApartmentMatchEntity existingMatch = createMatch(49, MatchStatus.CANCELED, candidate, apartment, true, null);

        when(userService.findById(candidate.getId())).thenReturn(candidate);
        when(apartmentService.findById(apartment.getId())).thenReturn(apartment);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidate.getId(), apartment.getId()))
                .thenReturn(Optional.of(existingMatch));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> apartmentMatchService.processSwipe(candidate.getId(), apartment.getId(), false, true));

        assertEquals("Cannot change interest on a match that is already matched, successful or canceled",
                exception.getMessage());
    }

    private UserEntity createUser(Integer id, Role role, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRole(role);
        user.setEmail(email);
        user.setPassword("encoded-password");
        return user;
    }

    private ApartmentEntity createApartment(Integer apartmentId, ApartmentState state, UserEntity landlord) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setState(state);
        apartment.setUser(landlord);
        apartment.setTitle("Apartment " + apartmentId);
        apartment.setDescription("Description " + apartmentId);
        apartment.setPrice(500.0);
        apartment.setBills("wifi");
        apartment.setUbication("Madrid");
        return apartment;
    }

    private ApartmentMatchEntity createMatch(Integer id, MatchStatus status, UserEntity candidate,
            ApartmentEntity apartment, Boolean candidateInterest, Boolean landlordInterest) {
        ApartmentMatchEntity match = new ApartmentMatchEntity();
        match.setId(id);
        match.setCandidate(candidate);
        match.setApartment(apartment);
        match.setMatchStatus(status);
        match.setCandidateInterest(candidateInterest);
        match.setLandlordInterest(landlordInterest);
        return match;
    }
}
