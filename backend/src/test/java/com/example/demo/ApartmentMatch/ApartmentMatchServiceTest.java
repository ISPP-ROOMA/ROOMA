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
    public void testProcessSwipe_SecondInteraction_MatchAlreadyRejected_Throws() {
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
        existing.setMatchStatus(MatchStatus.REJECTED);
        existing.setCandidate(user);
        existing.setApartment(apt);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("match is not active"));
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

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("already swiped") || ex.getMessage().toLowerCase().contains("already"));
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
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("not active") || ex.getMessage().toLowerCase().contains("not active"));

    }

    @Test
    public void testProcessSwipe_CandidateNotFound_Throws() {
        Integer candidateId = 7;
        Integer apartmentId = 8;

        when(userService.findById(candidateId)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("candidate not found"));
    }

    @Test
    public void testProcessSwipe_ApartmentNotFound_Throws() {
        Integer candidateId = 8;
        Integer apartmentId = 9;

        UserEntity user = new UserEntity();
        user.setId(candidateId);

        when(userService.findById(candidateId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.processSwipe(candidateId, apartmentId, true, true);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("apartment not found"));
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

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.finalizeMatchProcess(apartmentId);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("only apartments that are not matching can be finalized"));
    }

    @Test
    public void testFinalizeMatchProcess_ApartmentNotFound_Throws() {
        Integer apartmentId = 9;

        when(apartmentService.findById(apartmentId)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.finalizeMatchProcess(apartmentId);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("apartment not found"));
    }

    @Test
    public void testFinalizeMatchProcess_NoMatchesFound_Throws() {
        Integer apartmentId = 10;
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId(apartmentId);
        apt.setState(ApartmentState.ACTIVE);

        when(apartmentService.findById(apartmentId)).thenReturn(apt);
        when(apartmentMatchRepository.findByApartmentId(apartmentId)).thenReturn(Arrays.asList());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.finalizeMatchProcess(apartmentId);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("no matches found"));
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

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.successfulMatch(matchId);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("already finalized as successful"));
    }

    @Test
    public void testSuccessfulMatch_NotInMatchStatus_Throws() {
        Integer matchId = 8;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.ACTIVE);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.successfulMatch(matchId);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("only matches with status match can be finalized as successful"));
    }

    @Test
    public void testRejectMatch_RejectsMatch() {
        Integer matchId = 9;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.ACTIVE);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));
        when(apartmentMatchRepository.save(any(ApartmentMatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApartmentMatchEntity res = apartmentMatchService.rejectMatch(matchId);

        assertEquals(MatchStatus.REJECTED, res.getMatchStatus());
    }

    @Test
    public void testRejectMatch_AlreadySuccessful_Throws() {
        Integer matchId = 9;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.SUCCESSFUL);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.rejectMatch(matchId);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("cannot reject a match that has already been finalized as successful"));
    }

    @Test
    public void testRejectMatch_AlreadyRejected_Throws() {
        Integer matchId = 9;
        ApartmentMatchEntity m = new ApartmentMatchEntity();
        m.setMatchStatus(MatchStatus.REJECTED);

        when(apartmentMatchRepository.findById(matchId)).thenReturn(Optional.of(m));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.rejectMatch(matchId);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("match is already rejected"));
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

       
        assertThrows(RuntimeException.class, () -> {
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
        assertThrows(RuntimeException.class, () -> {
            apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidateId, apartmentId);
        });
    }

    @Test
    public void testFindApartmentMatchByCandidateAndApartment_CandidateNotFound() {
        Integer candidateId = 16;
        Integer apartmentId = 17;
        when(userService.findById(candidateId)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> {
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
        assertThrows(RuntimeException.class, () -> {
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
