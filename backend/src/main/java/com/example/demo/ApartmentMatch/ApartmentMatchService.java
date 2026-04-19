package com.example.demo.ApartmentMatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.ApartmentMatch.DTOs.CandidateFilterDTO;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.MemberApartment.MemberRole;
import com.example.demo.Notification.EventType;
import com.example.demo.Notification.NotificationService;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@Service
public class ApartmentMatchService {

    private final ApartmentMatchRepository apartmentMatchRepository;
    private final ApartmentService apartmentService;
    private final ApartmentMemberService apartmentMemberService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public ApartmentMatchService(ApartmentMatchRepository apartmentMatchRepository, ApartmentService apartmentService,
            ApartmentMemberService apartmentMemberService, UserService userService,
            NotificationService notificationService) {
        this.apartmentMatchRepository = apartmentMatchRepository;
        this.apartmentService = apartmentService;
        this.apartmentMemberService = apartmentMemberService;
        this.userService = userService;
        this.notificationService = notificationService;
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Apartment match not found for the given candidate and apartment"));
    }

    public List<ApartmentMatchEntity> findAllApartmentMatches() {
        return apartmentMatchRepository.findAll();
    }

    public ApartmentMatchEntity getMatchById(Integer matchId) {
        return apartmentMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    }

    @Transactional
    public ApartmentMatchEntity saveApartmentMatch(ApartmentMatchEntity apartmentMatch) {
        return apartmentMatchRepository.save(apartmentMatch);
    }

    public com.example.demo.User.UserEntity getUserByEmail(String email) {
        return userService.findByEmail(email).orElse(null);
    }

    @Transactional
    public ApartmentMatchEntity processSwipe(Integer candidateId, Integer apartmentId, boolean isCandidateAction,
            boolean interest) {

        UserEntity candidate = userService.findById(candidateId);
        if (candidate == null) {
            throw new ResourceNotFoundException("Candidate not found");
        }
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        if (apartment.getState() != ApartmentState.ACTIVE) {
            throw new ConflictException("Cannot swipe on an apartment that is not active");
        }

        ApartmentMatchEntity apartmentMatch = apartmentMatchRepository
                .findByCandidateIdAndApartmentId(candidateId, apartmentId).orElse(null);
        if (apartmentMatch == null) {
            apartmentMatch = createFirstInteraction(candidate, apartment, isCandidateAction, interest);
            return apartmentMatchRepository.save(apartmentMatch);
        }

        checkNoDuplicateInteraction(apartmentMatch, isCandidateAction);
        if (apartmentMatch.getMatchStatus() == MatchStatus.MATCH
                || apartmentMatch.getMatchStatus() == MatchStatus.SUCCESSFUL
                || apartmentMatch.getMatchStatus() == MatchStatus.CANCELED) {
            throw new ConflictException(
                    "Cannot change interest on a match that is already matched, successful or canceled");
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

    public ApartmentMatchEntity createFirstInteraction(UserEntity candidate, ApartmentEntity apartment,
            boolean isCandidateAction, boolean interest) {

        ApartmentMatchEntity newMatch = new ApartmentMatchEntity();

        if (isCandidateAction) {
            newMatch.setCandidateInterest(interest);
            newMatch.setLandlordInterest(null);
        } else {
            newMatch.setLandlordInterest(interest);
            newMatch.setCandidateInterest(null);
        }
        newMatch.setCandidate(candidate);
        newMatch.setApartment(apartment);
        if (interest) {
            newMatch.setMatchStatus(MatchStatus.ACTIVE);
        } else {
            newMatch.setMatchStatus(MatchStatus.REJECTED);
        }
        newMatch.setTenantHasOpenedMatchDetails(false);
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
        if (match.getMatchStatus() == MatchStatus.SUCCESSFUL) {
            throw new ConflictException("Match is already finalized as successful");
        } else if (match.getMatchStatus() != MatchStatus.MATCH) {
            throw new ConflictException("Only matches with status MATCH can be finalized as successful");
        }
        match.setMatchStatus(MatchStatus.SUCCESSFUL);
        apartmentMatchRepository.save(match);
        return match;
    }

    @Transactional
    public ApartmentMatchEntity cancelMatch(Integer matchId) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentMatchEntity match = apartmentMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (match.getMatchStatus() == MatchStatus.SUCCESSFUL) {
            throw new ConflictException("Cannot cancel a match that has already been finalized as successful");
        } else if (match.getMatchStatus() == MatchStatus.CANCELED) {
            throw new ConflictException("Match is already canceled");
        } else if (match.getMatchStatus() == MatchStatus.REJECTED) {
            throw new ConflictException("Rejected matches cannot be canceled");
        }

        boolean isCandidate = match.getCandidate().getId().equals(currentUser.getId());
        boolean isLandlord = match.getApartment().getUser().getId().equals(currentUser.getId());

        if (!isCandidate && !isLandlord) {
            throw new AccessDeniedException("Only the users involved in the match can cancel it");
        }

        if (match.getMatchStatus() == MatchStatus.ACTIVE && !isCandidate) {
            throw new AccessDeniedException("Only the candidate can cancel an active request");
        }

        if (match.getMatchStatus() != MatchStatus.ACTIVE && match.getMatchStatus() != MatchStatus.MATCH) {
            throw new ConflictException("Only matches with status ACTIVE or MATCH can be canceled");
        }

        match.setMatchStatus(MatchStatus.CANCELED);
        apartmentMatchRepository.save(match);
        return match;
    }

    @Transactional
    public void finalizeMatchProcess(Integer apartmentId) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        if (apartment.getState() == ApartmentState.MATCHING) {
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

    @Transactional
    public ApartmentMatchEntity processSwipe(Integer apartmentId, boolean interest) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        if (apartment.getState() != ApartmentState.ACTIVE) {
            throw new ConflictException("Cannot swipe on an apartment that is not active");
        }
        if (apartment.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You cannot swipe on your own apartment");
        }

        ApartmentMatchEntity existingMatch = apartmentMatchRepository
                .findByCandidateIdAndApartmentId(currentUser.getId(), apartmentId)
                .orElse(null);

        if (hasCandidateSwipedSinceActivation(existingMatch, apartment)) {
            throw new ConflictException("You have already swiped on this apartment");
        }

        ApartmentMatchEntity apartmentMatch = createApartmentMatch(existingMatch, currentUser, apartment, interest);

        String description = "El usuario \"" + currentUser.getName() + " " + currentUser.getSurname()
                + "\" ha mostrado interés en tu apartamento \"" + apartment.getTitle() + "\" con localización: "
                + apartment.getUbication();
        String link = "/mis-solicitudes/recibidas";

        notificationService.createNotification(EventType.MATCH, description, link, apartment.getUser());

        return apartmentMatchRepository.save(apartmentMatch);
    }

    public ApartmentMatchEntity createApartmentMatch(UserEntity candidate, ApartmentEntity apartment,
            boolean interest) {
        return createApartmentMatch(null, candidate, apartment, interest);
    }

    public ApartmentMatchEntity createApartmentMatch(ApartmentMatchEntity existingMatch, UserEntity candidate,
            ApartmentEntity apartment, boolean interest) {
        ApartmentMatchEntity match = existingMatch != null ? existingMatch : new ApartmentMatchEntity();
        match.setCandidateInterest(interest);
        match.setLandlordInterest(null);
        match.setCandidate(candidate);
        match.setApartment(apartment);
        match.setMatchDate(LocalDateTime.now(ZoneId.of("Europe/Madrid")));

        if (interest) {
            match.setMatchStatus(MatchStatus.ACTIVE);
        } else {
            match.setMatchStatus(MatchStatus.REJECTED);
        }
        match.setTenantHasOpenedMatchDetails(false);
        return match;
    }

    private boolean hasCandidateSwipedSinceActivation(ApartmentMatchEntity existingMatch, ApartmentEntity apartment) {
        if (existingMatch == null) {
            return false;
        }

        LocalDateTime matchDate = existingMatch.getMatchDate();
        if (matchDate == null) {
            return true;
        }

        LocalDateTime activationDate = apartment.getActivationDate();
        if (activationDate == null) {
            return true;
        }

        return !matchDate.isBefore(activationDate);
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
            String description = "El arrendador \"" + match.getApartment().getUser().getName() + " "
                    + match.getApartment().getUser().getSurname() + "\" ha aceptado tu solicitud para el apartamento \""
                    + match.getApartment().getTitle() + "\" con localización: " + match.getApartment().getUbication()
                    + "\"\n Ahora tienes que hablar con el arrendador para concretar los detalles de la visita al apartamento y la posible firma del contrato.";
            String link = "/mis-solicitudes/enviadas";
            notificationService.createNotification(EventType.MATCH, description, link, match.getCandidate());
        } else {
            match.setMatchStatus(MatchStatus.REJECTED);
        }
        return apartmentMatchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public List<ApartmentMatchEntity> findInterestedCandidatesByApartmentIdAndStatus(Integer apartmentId,
            MatchStatus status) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (!apartment.getUser().getId().equals(currentUser.getId())) {
            throw new ConflictException("Only the landlord of the apartment can view the interested candidates");
        }
        return apartmentMatchRepository.findByApartmentIdAndMatchStatus(apartmentId, status);
    }

    @Transactional(readOnly = true)
    public List<ApartmentMatchEntity> getFilteredCandidates(Integer apartmentId, CandidateFilterDTO filter) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (!apartment.getUser().getId().equals(currentUser.getId())) {
            throw new ConflictException("Only the landlord of the apartment can view the filtered candidates");
        }
        List<ApartmentMatchEntity> candidates = apartmentMatchRepository.findByApartmentIdAndMatchStatus(apartmentId, MatchStatus.ACTIVE);
        
        if (filter == null) {
            return candidates;
        }

        Map<ApartmentMatchEntity, Integer> scores = new HashMap<>();
        int maxScore = 0;

        for (ApartmentMatchEntity match : candidates) {
            int score = 0;
            UserEntity user = match.getCandidate();
            
            if (filter.getMinAge() != null && user.getBirthDate() != null) {
                int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
                if (age >= filter.getMinAge()) score++;
            }
            if (filter.getMaxAge() != null && user.getBirthDate() != null) {
                int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
                if (age <= filter.getMaxAge()) score++;
            }
            if (filter.getRequiredProfession() != null && filter.getRequiredProfession().equalsIgnoreCase(user.getProfession())) {
                score++;
            }
            if (filter.getAllowedSmoker() != null) {
                if (Objects.equals(filter.getAllowedSmoker(), user.getSmoker())) {
                    score++;
                }
            }
            if (filter.getRequiredSchedule() != null && filter.getRequiredSchedule().equalsIgnoreCase(user.getSchedule())) {
                score++;
            }
            
            scores.put(match, score);
            if (score > maxScore) {
                maxScore = score;
            }
        }

        if (maxScore == 0) {
            return candidates;
        }

        candidates.sort((m1, m2) -> Integer.compare(scores.get(m2), scores.get(m1)));
        return candidates;
    }

    @Transactional
    public ApartmentMatchEntity processLandlordDecision(Integer matchId, String decision) {
        ApartmentMatchEntity match = apartmentMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        UserEntity currentUser = userService.findCurrentUserEntity();
        
        if (!match.getApartment().getUser().getId().equals(currentUser.getId())) {
            throw new ConflictException("Only the landlord of the apartment can process this decision");
        }
        
        if (match.getMatchStatus() != MatchStatus.ACTIVE && match.getMatchStatus() != MatchStatus.WAITING) {
            throw new ConflictException("Only matches with status ACTIVE or WAITING can be processed by the landlord decision");
        }

        switch (decision.toUpperCase()) {
            case "ACCEPT":
                match.setMatchStatus(MatchStatus.MATCH);
                match.setLandlordInterest(true);
                break;
            case "WAIT":
                if (match.getMatchStatus() != MatchStatus.ACTIVE) {
                    throw new ConflictException("Only matches with status ACTIVE can be moved to WAITING");
                }
                match.setMatchStatus(MatchStatus.WAITING);
                break;
            case "REJECT":
                match.setMatchStatus(MatchStatus.REJECTED);
                match.setLandlordInterest(false);
                break;
            default:
                throw new ConflictException("Invalid decision: " + decision);
        }
        return apartmentMatchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public List<ApartmentMatchEntity> findInterestedCandidatesByUserIdAndStatus(Integer userId, MatchStatus status) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (!currentUser.getId().equals(userId)) {
            throw new ConflictException("You can only view your own interested candidates");
        }
        return apartmentMatchRepository.findByUserIdAndMatchStatus(userId, status);
    }

    @Transactional(readOnly = true)
    List<ApartmentMatchEntity> findTenantRequestByUserIdAndStatus(MatchStatus status) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        return apartmentMatchRepository.findTenantRequestByUserIdAndStatus(currentUser.getId(), status);
    }

    @Transactional(readOnly = true)
    public ApartmentMatchEntity findMyMatchForTenant(Integer apartmentMatchId) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentMatchEntity match = apartmentMatchRepository.findById(apartmentMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (!match.getCandidate().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only view your own matches");
        }
        return match;
    }

    @Transactional
    public ApartmentMatchEntity markTenantMatchDetailsAsOpened(Integer apartmentMatchId) {
        ApartmentMatchEntity match = findMyMatchForTenant(apartmentMatchId);
        if (!Boolean.TRUE.equals(match.getTenantHasOpenedMatchDetails())) {
            match.setTenantHasOpenedMatchDetails(true);
            apartmentMatchRepository.save(match);
        }
        return match;
    }

    @Transactional(readOnly = true)
    public ApartmentMatchEntity findMyMatchForLandlord(Integer apartmentMatchId) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentMatchEntity match = apartmentMatchRepository.findById(apartmentMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (!match.getApartment().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only view matches for your own apartments");
        }
        return match;
    }

    @Transactional
    public ApartmentMatchEntity sendInvitation(Integer apartmentMatchId) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentMatchEntity match = apartmentMatchRepository.findById(apartmentMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (!match.getApartment().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the landlord of the apartment can send an invitation");
        }
        if (match.getMatchStatus() != MatchStatus.MATCH) {
            throw new ConflictException("Only matches with status MATCH can be invited");
        }
        if (!apartmentMemberService.findActiveMembershipsByUserId(match.getCandidate().getId()).isEmpty()) {
            throw new ConflictException("No puedes enviar esta invitación porque el candidato ya pertenece a un apartamento");
        }

        match.setMatchStatus(MatchStatus.INVITED);
        String description = "El arrendador \"" + match.getApartment().getUser().getName() + " "
                + match.getApartment().getUser().getSurname()
                + "\" te ha enviado una invitación para unirte al apartamento \"" + match.getApartment().getTitle()
                + "\" con localización: " + match.getApartment().getUbication()
                + "\"\n Por favor, responde a esta invitación lo antes posible para confirmar si estás interesado en unirte al apartamento.";
        String link = "/mis-solicitudes/recibidas/";
        notificationService.createNotification(EventType.INVITATION_SENT, description, link, match.getCandidate());
        return apartmentMatchRepository.save(match);
    }

    @Transactional
    public ApartmentMatchEntity respondToInvitation(Integer apartmentMatchId, boolean accepted) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentMatchEntity match = apartmentMatchRepository.findById(apartmentMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (!match.getCandidate().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the candidate can respond to the invitation");
        }
        if (match.getMatchStatus() != MatchStatus.INVITED) {
            throw new ConflictException("Only matches with status INVITED can be responded to");
        }
        if (accepted) {
            if (!apartmentMemberService.findActiveMembershipsByUserId(currentUser.getId()).isEmpty()) {
                throw new ConflictException("No puedes aceptar esta invitación porque ya perteneces a un apartamento");
            }

            if(!apartmentMemberService.existsByUserIdAndRole(match.getApartment().getUser().getId(), MemberRole.HOMEBODY)) {
                apartmentMemberService.addMember(match.getApartment().getId(), match.getApartment().getUser().getId(), null);
            }
            match.setMatchStatus(MatchStatus.SUCCESSFUL);
            apartmentMemberService.addMember(match.getApartment().getId(), currentUser.getId(), LocalDate.now());
            String description = "El inquilino \"" + currentUser.getName() + " " + currentUser.getSurname()
                    + "\" ha aceptado tu invitación para unirse al apartamento \"" + match.getApartment().getTitle()
                    + "\" con localización: " + match.getApartment().getUbication()
                    + "\"\n Ahora vivirá en su apartamento.";
            String link = "/my-home";
            notificationService.createNotification(EventType.INVITATION_ACCEPTED, description, link,
                    match.getApartment().getUser());
        } else {
            match.setMatchStatus(MatchStatus.REJECTED);
            String description = "El inquilino \"" + currentUser.getName() + " " + currentUser.getSurname()
                    + "\" ha rechazado tu invitación para unirse al apartamento \"" + match.getApartment().getTitle()
                    + "\" con localización: " + match.getApartment().getUbication()
                    + "\"\n Puedes seguir buscando candidatos interesados en tu apartamento.";
            String link = "/apartment/" + match.getApartment().getId() + "/interested-candidates/ACTIVE";
            notificationService.createNotification(EventType.INVITATION_REJECTED, description, link,
                    match.getApartment().getUser());
        }
        return apartmentMatchRepository.save(match);
    }

}
