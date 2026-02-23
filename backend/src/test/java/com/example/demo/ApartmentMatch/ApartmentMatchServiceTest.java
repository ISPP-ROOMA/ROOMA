package com.example.demo.ApartmentMatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Apartment.ApartmentService;
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
    public void testProcessSwipe_FirstInteraction_CandidateInterestedCreatesActiveMatch() {
        Integer candidateId = 1;
        Integer apartmentId = 2;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.empty());
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);

        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.getCandidateInterest());
        assertNull(result.getLandlordInterest());
        assertEquals(MatchStatus.ACTIVE, result.getMatchStatus());
    }

    @Test
    public void testProcessSwipe_FirstInteraction_LandlordInterestedCreatesActiveMatch() {
        Integer candidateId = 2;
        Integer apartmentId = 3;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.empty());
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidateId, apartmentId, false, false);
        assertNotNull(result);
        assertNull(result.getCandidateInterest());
        assertEquals(Boolean.FALSE, result.getLandlordInterest());
        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
    }

    @Test
    public void testProcessSwipe_FirstInteraction_Rejected() {
        Integer candidateId = 5;
        Integer apartmentId = 6;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.empty());
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidateId, apartmentId, true, false);

        assertNotNull(result);
        assertEquals(Boolean.FALSE, result.getCandidateInterest());
        assertNull(result.getLandlordInterest());
        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
    }

    @Test
    public void testProcessSwipe_SecondInteraction_BothInterested_MakesMatch() {
        Integer candidateId = 3;
        Integer apartmentId = 4;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity existing = new ApartmentMatchEntity();
        existing.setCandidateInterest(true);
        existing.setLandlordInterest(null);
        existing.setMatchStatus(MatchStatus.ACTIVE);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidateId, apartmentId, false, true);

        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.getCandidateInterest());
        assertEquals(Boolean.TRUE, result.getLandlordInterest());
        assertEquals(MatchStatus.MATCH, result.getMatchStatus());
    }

    @Test
    public void testProcessSwipe_SecondInteraction_LandlordRejects_MakesRejected() {
        Integer candidateId = 4;
        Integer apartmentId = 5;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity existing = new ApartmentMatchEntity();
        existing.setCandidateInterest(true);
        existing.setLandlordInterest(null);
        existing.setMatchStatus(MatchStatus.ACTIVE);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidateId, apartmentId, false, false);

        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.getCandidateInterest());
        assertEquals(Boolean.FALSE, result.getLandlordInterest());
        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
    }

    @Test
    public void testProcessSwipe_SecondInteraction_CandidateRejects_MakesRejected() {
        Integer candidateId = 4;
        Integer apartmentId = 5;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity existing = new ApartmentMatchEntity();
        existing.setCandidateInterest(null);
        existing.setLandlordInterest(true);
        existing.setMatchStatus(MatchStatus.ACTIVE);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.processSwipe(candidateId, apartmentId, true, false);

        assertNotNull(result);
        assertEquals(Boolean.FALSE, result.getCandidateInterest());
        assertEquals(Boolean.TRUE, result.getLandlordInterest());
        assertEquals(MatchStatus.REJECTED, result.getMatchStatus());
    }

    @Test
    public void testProcessSwipe_SecondInteraction_MatchAlreadyMatched_Throws() {
        Integer candidateId = 4;
        Integer apartmentId = 5;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity existing = new ApartmentMatchEntity();
        existing.setCandidateInterest(null);
        existing.setLandlordInterest(false);
        existing.setMatchStatus(MatchStatus.MATCH);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
    }

    @Test
    public void testProcessSwipe_SecondInteraction_MatchAlreadySuccessful_Throws() {
        Integer candidateId = 4;
        Integer apartmentId = 5;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity existing = new ApartmentMatchEntity();
        existing.setCandidateInterest(null);
        existing.setLandlordInterest(true);
        existing.setMatchStatus(MatchStatus.SUCCESSFUL);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, false);
        });
    }

    @Test
    public void testProcessSwipe_SecondInteraction_MatchAlreadyCanceled_Throws() {
        Integer candidateId = 4;
        Integer apartmentId = 5;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity existing = new ApartmentMatchEntity();
        existing.setCandidateInterest(null);
        existing.setLandlordInterest(false);
        existing.setMatchStatus(MatchStatus.CANCELED);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
    }

    @Test
    public void testProcessSwipe_DuplicateInteraction_Throws() {
        Integer candidateId = 5;
        Integer apartmentId = 6;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity existing = new ApartmentMatchEntity();
        existing.setCandidateInterest(true);
        existing.setLandlordInterest(null);
        existing.setMatchStatus(MatchStatus.ACTIVE);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
    }

    @Test
    public void testProcessSwipe_DuplicateInteraction2_Throws() {
        Integer candidateId = 5;
        Integer apartmentId = 6;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity existing = new ApartmentMatchEntity();
        existing.setCandidateInterest(null);
        existing.setLandlordInterest(true);
        existing.setMatchStatus(MatchStatus.ACTIVE);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, false, true);
        });
    }

    @Test
    public void testProcessSwipe_SwipeOnInactiveApartment_Throws() {
        Integer candidateId = 6;
        Integer apartmentId = 7;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.CLOSED);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
    }

    @Test
    public void testProcessSwipe_CandidateNotFound_Throws() {
        Integer candidateId = 7;
        Integer apartmentId = 8;

        when(userService.findById(candidateId)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
    }

    @Test
    public void testProcessSwipe_ApartmentNotFound_Throws() {
        Integer candidateId = 8;
        Integer apartmentId = 9;

        UserEntity user = new UserEntity();
        user.setId(candidateId);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
    }  

    @Test
    public void testFinalizeMatchProcess_DeletesMatchesAndClosesApartment() {
        Integer apartmentId = 7;
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setApartment(apt);

        List<ApartmentMatchEntity> matches = Arrays.asList(m);

        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByApartmentId(apartmentId)).thenReturn(matches);

        apartmentMatchService.finalizeMatchProcess(apartmentId);

        verify(apartmentMatchRepository).deleteAll(matches);
        assertEquals(ApartmentState.CLOSED, apt.getState());
        verify(apartmentService).save(apt);
    }

    @Test
    public void testFinalizeMatchProcess_ApartmentStateMatching_Throws() {
        Integer apartmentId = 8;
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.MATCHING);

        when(apartmentService.findById(apartmentId)).thenReturn(apt);

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.finalizeMatchProcess(apartmentId);
        });
    }

    @Test
    public void testFinalizeMatchProcess_ApartmentNotFound_Throws() {
        Integer apartmentId = 9;

        when(apartmentService.findById(apartmentId)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMatchService.finalizeMatchProcess(apartmentId);
        });
    }

    @Test
    public void testFinalizeMatchProcess_NoMatchesFound_Throws() {
        Integer apartmentId = 10;
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByApartmentId(apartmentId)).thenReturn(Arrays.asList());
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMatchService.finalizeMatchProcess(apartmentId);
        });
    }


    @Test
    public void testSuccessfulMatch_FinalizesMatch() {
        Integer matchId = 8;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.MATCH);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity res = apartmentMatchService.successfulMatch(matchId);

        assertEquals(MatchStatus.SUCCESSFUL, res.getMatchStatus());
    }

    @Test
    public void testSuccessfulMatch_AlreadySuccessful_Throws() {
        Integer matchId = 8;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.SUCCESSFUL);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.successfulMatch(matchId);
        });
    }

    @Test
    public void testSuccessfulMatch_NotInMatchStatus_Throws() {
        Integer matchId = 8;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.ACTIVE);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.successfulMatch(matchId);
        });
    }

    @Test
    public void testCancelMatch_CancelsMatch() {
        Integer matchId = 9;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.MATCH);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity res = apartmentMatchService.cancellMatch(matchId);

        assertEquals(MatchStatus.CANCELED, res.getMatchStatus());
    }

    @Test
    public void testCancelMatch_AlreadyCanceled_Throws() {
        Integer matchId = 9;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.CANCELED);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.cancellMatch(matchId);
        });
    }

    @Test
    public void testCancelMatch_AlreadyRejected_Throws() {
        Integer matchId = 9;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.REJECTED);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.cancellMatch(matchId);
        });
    }

    @Test
    public void testCancelMatch_AlreadyActive_Throws() {
        Integer matchId = 9;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.ACTIVE);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.cancellMatch(matchId);
        });
    }

    @Test
    public void testCancelMatch_AlreadySuccessful_Throws() {
        Integer matchId = 9;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.SUCCESSFUL);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        assertThrows(ConflictException.class, () -> {
            apartmentMatchService.cancellMatch(matchId);
        });
    }

    @Test
    public void testFindMatchesByCandidateIdAndMatchStatus() {
        Integer candidateId = 5;
        MatchStatus status = MatchStatus.MATCH;

        List<ApartmentMatchEntity> expectedMatches = Arrays.asList(
                new ApartmentMatchEntity(),
                new ApartmentMatchEntity()
        );

        when(apartmentMatchRepository.findByCandidateIdAndMatchStatus(candidateId, status)).thenReturn(expectedMatches);

        List<ApartmentMatchEntity> result = apartmentMatchService.findMatchesByCandidateIdAndMatchStatus(candidateId, status);

        assertEquals(expectedMatches, result);
    }

    @Test
    public void testFindMatchesByApartmentIdAndMatchStatus() {
        Integer apartmentId = 6;
        MatchStatus status = MatchStatus.ACTIVE;

        List<ApartmentMatchEntity> expectedMatches = Arrays.asList(
                new ApartmentMatchEntity(),
                new ApartmentMatchEntity(),
                new ApartmentMatchEntity()
        );

        when(apartmentMatchRepository.findByApartmentIdAndMatchStatus(apartmentId, status)).thenReturn(expectedMatches);

        List<ApartmentMatchEntity> result = apartmentMatchService.findMatchesByApartmentIdAndMatchStatus(apartmentId, status);

        assertEquals(expectedMatches, result); 
    }

    @Test
    public void testFindAparmentMatchById() {
        Integer matchId = 10;
        ApartmentMatchEntity expectedMatch = new ApartmentMatchEntity();
        expectedMatch.setId(matchId);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(expectedMatch));

        ApartmentMatchEntity result = apartmentMatchService.findApartmentMatchById(matchId);
        assertTrue(result != null);
        assertEquals(expectedMatch, result);   
    }

    @Test
    public void testFindAparmentMatchById_NotFound() {
        Integer matchId = 11;

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.empty());

       
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMatchService.findApartmentMatchById(matchId);
        });
    }

    @Test
    public void testFindApartmentMatchByCandidateAndApartment() {
        Integer candidateId = 12;
        Integer apartmentId = 13;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);

        ApartmentMatchEntity expectedMatch = new ApartmentMatchEntity();
        expectedMatch.setCandidate(user);
        expectedMatch.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);

        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(expectedMatch));
        ApartmentMatchEntity result = apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidateId, apartmentId);
        assertTrue(result != null);
        assertEquals(expectedMatch, result);
    
    }

    @Test
    public void testFindApartmentMatchByCandidateAndApartment_ApartmentNotFound() {
        Integer candidateId = 14;
        Integer apartmentId = 15;
        when(userService.findById(candidateId)).thenReturn(new UserEntity());
        when(apartmentService.findById(apartmentId)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidateId, apartmentId);
        });
    }

    @Test
    public void testFindApartmentMatchByCandidateAndApartment_CandidateNotFound() {
        Integer candidateId = 16;
        Integer apartmentId = 17;
        when(userService.findById(candidateId)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidateId, apartmentId);
        });
    }

    @Test
    public void testFindApartmentMatchByCandidateAndApartment_MatchNotFound() {
        Integer candidateId = 18;
        Integer apartmentId = 19;

        UserEntity user = new UserEntity();
        user.setId(candidateId);
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidateId, apartmentId);
        });
    }

    @Test
    public void testFindAllApartmentMatches() {
        List<ApartmentMatchEntity> expectedMatches = Arrays.asList(
                new ApartmentMatchEntity(),
                new ApartmentMatchEntity(),
                new ApartmentMatchEntity()
        );

        when(apartmentMatchRepository.findAll()).thenReturn(expectedMatches);

        List<ApartmentMatchEntity> result = apartmentMatchService.findAllApartmentMatches();

        assertEquals(expectedMatches, result);
    }

    @Test
    public void testSaveApartmentMatch() {
        ApartmentMatchEntity matchToSave = new ApartmentMatchEntity();
        matchToSave.setId(20);

        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity result = apartmentMatchService.saveApartmentMatch(matchToSave);

        assertNotNull(result);
        assertEquals(matchToSave, result);
    }
}
