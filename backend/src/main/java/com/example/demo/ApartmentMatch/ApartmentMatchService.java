package com.example.demo.ApartmentMatch;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
    public ApartmentMatchEntity processSwipe(Integer apartmentId, boolean interest) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        if(apartment.getState() != ApartmentState.ACTIVE) {
            throw new ConflictException("Cannot swipe on an apartment that is not active");
        }
        if(apartment.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You cannot swipe on your own apartment");
        }
        if(apartmentMatchRepository.findByCandidateIdAndApartmentId(currentUser.getId(), apartmentId).isPresent()) {
            throw new ConflictException("You have already swiped on this apartment");
        }
        
        ApartmentMatchEntity apartmentMatch = createApartmentMatch(currentUser, apartment, interest);
        return apartmentMatchRepository.save(apartmentMatch);
    }

    public ApartmentMatchEntity createApartmentMatch(UserEntity candidate, ApartmentEntity apartment, boolean interest) { 
        ApartmentMatchEntity newMatch = new ApartmentMatchEntity();
        newMatch.setCandidateInterest(interest);
        newMatch.setLandlordInterest(null);
        newMatch.setCandidate(candidate);
        newMatch.setApartment(apartment);
        newMatch.setMatchDate(LocalDateTime.now(ZoneId.of("Europe/Madrid")));

        if(interest){
            newMatch.setMatchStatus(MatchStatus.ACTIVE);
        } else {
            newMatch.setMatchStatus(MatchStatus.REJECTED);
        }
        return newMatch;
    }

    @Transactional
    public ApartmentMatchEntity processLandlordAction(Integer matchId, boolean interest) {
        ApartmentMatchEntity match = apartmentMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (match.getMatchStatus() != MatchStatus.ACTIVE) {
            throw new ConflictException("Only matches with status ACTIVE can be processed by the landlord");
        }
        if (!match.getApartment().getUser().getId().equals(userService.findCurrentUserEntity().getId())) {
            throw new ConflictException("Only the landlord of the apartment can process this action");
        }
        match.setLandlordInterest(interest);
        if (interest) {
            match.setMatchStatus(MatchStatus.MATCH);
        } else {
            match.setMatchStatus(MatchStatus.REJECTED);
        }
        return apartmentMatchRepository.save(match);
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

    @Transactional
    public List<ApartmentMatchEntity> findInterestedCandidatesByApartmentId(Integer apartmentId) {
        String currentUser = userService.findCurrentUser();
        Optional<UserEntity> user = userService.findByEmail(currentUser);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        if (!apartment.getUser().getId().equals(user.get().getId())) {
            throw new ConflictException("Only the landlord of the apartment can view the interested candidates");
        }
        return apartmentMatchRepository.findByApartmentIdAndMatchStatus(apartmentId, MatchStatus.ACTIVE);
    }

    public List<ApartmentMatchEntity> findInterestedCandidatesByUserId(Integer userId) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (!currentUser.getId().equals(userId)) {
            throw new ConflictException("You can only view your own interested candidates");
        }
        return apartmentMatchRepository.findByUserIdAndMatchStatus(userId, MatchStatus.ACTIVE);
    }

    List<ApartmentMatchEntity> findTenantRequestByUserId(Integer id) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (!currentUser.getId().equals(id)) {
            throw new ConflictException("You can only view your own Request");
        }
        return apartmentMatchRepository.findTenantRequestByUserId(id);
    }    
}
