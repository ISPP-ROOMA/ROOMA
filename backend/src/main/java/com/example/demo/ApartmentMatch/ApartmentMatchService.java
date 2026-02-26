package com.example.demo.ApartmentMatch;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Apartment match not found"));
    }

    public ApartmentMatchEntity findApartmentMatchByCandidateAndApartment(Integer candidateId, Integer apartmentId) {
        UserEntity candidate = userService.findById(candidateId);
        if (candidate == null) {
            throw new ResourceNotFoundException("Candidate not found");
        }

        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        return apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment match not found for the given candidate and apartment"));
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
            throw new ResourceNotFoundException("Candidate not found");
        }
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        if(apartment.getState() != ApartmentState.ACTIVE) {
            throw new ConflictException("Cannot swipe on an apartment that is not active");
         }

        ApartmentMatchEntity apartmentMatch = apartmentMatchRepository.findByCandidateIdAndApartmentId(candidateId, apartmentId).orElse(null);
        if (apartmentMatch == null) {
            apartmentMatch = createFirstInteraction(candidate, apartment, isCandidateAction, interest);
            return apartmentMatchRepository.save(apartmentMatch);
        }
        
        checkNoDuplicateInteraction(apartmentMatch, isCandidateAction);
        if (apartmentMatch.getMatchStatus() == MatchStatus.MATCH
                || apartmentMatch.getMatchStatus() == MatchStatus.SUCCESSFUL
                || apartmentMatch.getMatchStatus() == MatchStatus.CANCELED) {
            throw new ConflictException("Cannot change interest on a match that is already matched, successful or canceled");
        }
        if (isCandidateAction) {
            apartmentMatch.setCandidateInterest(interest);
        } else {
            apartmentMatch.setLandlordInterest(interest);
        }
        if (Boolean.TRUE.equals(apartmentMatch.getCandidateInterest()) && 
                Boolean.TRUE.equals(apartmentMatch.getLandlordInterest())) {
            
            apartmentMatch.setMatchStatus(MatchStatus.MATCH);
        } else {
            apartmentMatch.setMatchStatus(MatchStatus.REJECTED);
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

    public void checkNoDuplicateInteraction(ApartmentMatchEntity apartmentMatchEntity, boolean isCandidateAction) {
        if (apartmentMatchEntity.getCandidateInterest() != null && isCandidateAction) {
            throw new ConflictException("Candidate has already swiped on this apartment");
        }
        if (apartmentMatchEntity.getLandlordInterest() != null && !isCandidateAction) {
            throw new ConflictException("Landlord has already swiped on this candidate");
        }
    }

    @Transactional
    public void finalizeMatchProcess(Integer apartmentId) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        if(apartment.getState() == ApartmentState.MATCHING) {
            throw new ConflictException("Only apartments that are not matching can be finalized");
        }
        List<ApartmentMatchEntity> matches = apartmentMatchRepository.findByApartmentId(apartmentId);

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("No matches found for this apartment");
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
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if(match.getMatchStatus() == MatchStatus.SUCCESSFUL) {
            throw new ConflictException("Match is already finalized as successful");
        }
        else if(match.getMatchStatus() != MatchStatus.MATCH) {
            throw new ConflictException("Only matches with status MATCH can be finalized as successful");
        }
        match.setMatchStatus(MatchStatus.SUCCESSFUL);
        apartmentMatchRepository.save(match);
        return match;
    }

    @Transactional
    public ApartmentMatchEntity cancelMatch(Integer matchId) {
        ApartmentMatchEntity match = apartmentMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (match.getMatchStatus() == MatchStatus.SUCCESSFUL) {
            throw new ConflictException("Cannot cancel a match that has already been finalized as successful");
        } else if (match.getMatchStatus() == MatchStatus.CANCELED) {
            throw new ConflictException("Match is already canceled");
        } else if (match.getMatchStatus() == MatchStatus.REJECTED || match.getMatchStatus() == MatchStatus.ACTIVE) {
            throw new ConflictException("Only matches with status MATCH can be canceled");
        }
        match.setMatchStatus(MatchStatus.CANCELED);
        apartmentMatchRepository.save(match);
        return match;
    }
}
