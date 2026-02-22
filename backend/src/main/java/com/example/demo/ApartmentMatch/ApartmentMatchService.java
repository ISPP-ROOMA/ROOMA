package com.example.demo.ApartmentMatch;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@Service
public class ApartmentMatchService {

    private final ApartmentMatchRepository apartmentMatchRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;

    @Autowired
    public ApartmentMatchService(ApartmentMatchRepository apartmentMatchRepository, ApartmentService apartmentService, UserService userService) {
        this.apartmentMatchRepository = apartmentMatchRepository;
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    public ApartmentMatchEntity findApartmentMatchById(Integer id) {
        return apartmentMatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apartment match not found"));
    }

    public ApartmentMatchEntity findApartmentMatchByCandidateAndApartment(Integer candidateId, Integer apartmentId) {
        UserEntity candidate = userService.findById(candidateId);
        if (candidate == null) {
            throw new RuntimeException("Candidate not found");
        }

        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new RuntimeException("Apartment not found");
        }
        return apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment match not found for the given candidate and apartment"));
    }

    public List<ApartmentMatchEntity> findAllApartmentMatches() {
        return apartmentMatchRepository.findAll();
    }

    @Transactional
    public ApartmentMatchEntity saveApartmentMatch(ApartmentMatchEntity apartmentMatch) {
        return apartmentMatchRepository.save(apartmentMatch);
    }

    @Transactional
    public ApartmentMatchEntity processSwipe(Integer candidateId, Integer apartmentId, boolean isCandidateAction, boolean interest) {
        
        UserEntity candidate = userService.findById(candidateId);
        if (candidate == null) {
            throw new RuntimeException("Candidate not found");
        }
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new RuntimeException("Apartment not found");
        }
        if(apartment.getState() != ApartmentState.ACTIVE) {
            throw new RuntimeException("Cannot swipe on an apartment that is not active");
         }

        ApartmentMatchEntity apartmentMatch = apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId).orElse(null);
        if (apartmentMatch == null) {
            apartmentMatch = createFirstInteraction(candidate, apartment, isCandidateAction, interest);
            return apartmentMatchRepository.save(apartmentMatch);
        }
        
        checkNoDuplicateInteraction(apartmentMatch, isCandidateAction, interest);

        if(isCandidateAction){
            apartmentMatch.setCandidateInterest(interest);
        } else {
            apartmentMatch.setLandlordInterest(interest);
        }

        if(apartmentMatch.getMatchStatus() != MatchStatus.ACTIVE) {
            throw new RuntimeException("Match is not active");
        } else {
            if (Boolean.TRUE.equals(apartmentMatch.getCandidateInterest()) && 
                Boolean.TRUE.equals(apartmentMatch.getLandlordInterest())) {
                
                apartmentMatch.setMatchStatus(MatchStatus.MATCH);

            } else if (Boolean.FALSE.equals(apartmentMatch.getCandidateInterest()) || 
                    Boolean.FALSE.equals(apartmentMatch.getLandlordInterest())) {
                
                apartmentMatch.setMatchStatus(MatchStatus.REJECTED);
            }
        }

        return apartmentMatchRepository.save(apartmentMatch);
    }

    public ApartmentMatchEntity createFirstInteraction(UserEntity candidate, ApartmentEntity apartment, boolean isCandidateAction, boolean interest) {

                        
        ApartmentMatchEntity newMatch = new ApartmentMatchEntity();

        if(isCandidateAction) {
            newMatch.setCandidateInterest(interest);
            newMatch.setLandlordInterest(null);
        } else {
            newMatch.setLandlordInterest(interest);
            newMatch.setCandidateInterest(null);
        }
        newMatch.setCandidate(candidate);
        newMatch.setApartment(apartment);
        if(interest){
            newMatch.setMatchStatus(MatchStatus.ACTIVE);
        } else {
            newMatch.setMatchStatus(MatchStatus.REJECTED);
        }
        return newMatch;
    }

    public void checkNoDuplicateInteraction(ApartmentMatchEntity apartmentMatchEntity, boolean isCandidateAction, boolean interest) {
        if (apartmentMatchEntity.getCandidateInterest() != null && isCandidateAction) {
            throw new RuntimeException("Candidate has already swiped on this apartment");
        }
        if (apartmentMatchEntity.getLandlordInterest() != null && !isCandidateAction) {
            throw new RuntimeException("Landlord has already swiped on this candidate");
        }
    }

    @Transactional
    public void finalizeMatchProcess(Integer apartmentId) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new RuntimeException("Apartment not found");
        }
        if(apartment.getState() == ApartmentState.MATCHING) {
            throw new RuntimeException("Only apartments that are not matching can be finalized");
        }
        List<ApartmentMatchEntity> matches = apartmentMatchRepository.findByApartmentId(apartmentId);

        if (matches.isEmpty()) {
            throw new RuntimeException("No matches found for this apartment");
        }

        apartmentMatchRepository.deleteAll(matches);
        apartment.setState(ApartmentState.CLOSED);
        apartmentService.save(apartment);
    }

    public List<ApartmentMatchEntity> findMatchesByCandidateIdAndMatchStatus(Integer candidateId, MatchStatus status) {
        return apartmentMatchRepository.findByCandidateIdAndMatchStatus(candidateId, status);
    }

    public List<ApartmentMatchEntity> findMatchesByApartmentIdAndMatchStatus(Integer apartmentId, MatchStatus status) {
        return apartmentMatchRepository.findByApartmentIdAndMatchStatus(apartmentId, status);
    }

    @Transactional
    public ApartmentMatchEntity successfulMatch(Integer matchId) {
        ApartmentMatchEntity match = apartmentMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        if(match.getMatchStatus() == MatchStatus.SUCCESSFUL) {
            throw new RuntimeException("Match is already finalized as successful");
        }
        else if(match.getMatchStatus() != MatchStatus.MATCH) {
            throw new RuntimeException("Only matches with status MATCH can be finalized as successful");
        }
        match.setMatchStatus(MatchStatus.SUCCESSFUL);
        apartmentMatchRepository.save(match);
        return match;
    }

    @Transactional
    public ApartmentMatchEntity rejectMatch(Integer matchId) {
        ApartmentMatchEntity match = apartmentMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        if (match.getMatchStatus() == MatchStatus.SUCCESSFUL) {
            throw new RuntimeException("Cannot reject a match that has already been finalized as successful");
        } else if (match.getMatchStatus() == MatchStatus.REJECTED) {
            throw new RuntimeException("Match is already rejected");
        }
        match.setMatchStatus(MatchStatus.REJECTED);
        apartmentMatchRepository.save(match);
        return match;
    }
}
