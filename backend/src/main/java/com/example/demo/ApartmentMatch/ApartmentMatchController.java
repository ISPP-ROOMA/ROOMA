package com.example.demo.ApartmentMatch;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ApartmentMatch.DTOs.ApartmentMatchDTO;
import com.example.demo.ApartmentMatch.DTOs.ApartmentMatchLandlordDTO;
import com.example.demo.ApartmentMatch.DTOs.ApartmentMatchSummaryDTO;
import com.example.demo.ApartmentMatch.DTOs.ApartmentMatchTenantDTO;

@RestController
@RequestMapping("/api/apartments-matches")
public class ApartmentMatchController {

    private final ApartmentMatchService apartmentMatchService;

    public ApartmentMatchController(ApartmentMatchService apartmentMatchService) {
        this.apartmentMatchService = apartmentMatchService;
    }

    @GetMapping
    public ResponseEntity<List<ApartmentMatchDTO>> getAllApartmentMatches() {
        List<ApartmentMatchDTO> apartmentMatches = ApartmentMatchDTO
                .fromApartmentMatchEntityList(apartmentMatchService.findAllApartmentMatches());
        return ResponseEntity.ok(apartmentMatches);
    }

    @GetMapping("/candidate/{candidateId}/apartment/{apartmentId}")
    public ResponseEntity<ApartmentMatchDTO> getApartmentMatchByCandidateAndApartment(@PathVariable Integer candidateId,
            @PathVariable Integer apartmentId) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(
                apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidateId, apartmentId));
        return ResponseEntity.ok(apartmentMatch);
    }

    @GetMapping("/candidate/{candidateId}/status/{matchStatus}")
    public ResponseEntity<List<ApartmentMatchDTO>> getApartmentMatchesByCandidateId(@PathVariable Integer candidateId,
            @PathVariable MatchStatus matchStatus) {
        List<ApartmentMatchDTO> apartmentMatches = ApartmentMatchDTO.fromApartmentMatchEntityList(
                apartmentMatchService.findMatchesByCandidateIdAndMatchStatus(candidateId, matchStatus));
        return ResponseEntity.ok(apartmentMatches);
    }

    @GetMapping("/apartment/{apartmentId}/status/{matchStatus}")
    public ResponseEntity<List<ApartmentMatchDTO>> getApartmentMatchesByApartmentId(@PathVariable Integer apartmentId,
            @PathVariable MatchStatus matchStatus) {
        List<ApartmentMatchDTO> apartmentMatches = ApartmentMatchDTO.fromApartmentMatchEntityList(
                apartmentMatchService.findMatchesByApartmentIdAndMatchStatus(apartmentId, matchStatus));
        return ResponseEntity.ok(apartmentMatches);
    }

    @DeleteMapping("/apartment/{apartmentId}")
    public ResponseEntity<Void> finalizeMatchProcess(@PathVariable Integer apartmentId) {
        apartmentMatchService.finalizeMatchProcess(apartmentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/swipe/candidate/{candidateId}/apartment/{apartmentId}/action/{isCandidateAction}")
    public ResponseEntity<?> processSwipe(@PathVariable Integer candidateId, @PathVariable Integer apartmentId,
            @PathVariable boolean isCandidateAction, @RequestBody boolean interest,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You must be authenticated to perform swipe actions");
        }

        com.example.demo.User.UserEntity authenticatedUser = apartmentMatchService
                .getUserByEmail(userDetails.getUsername());
        if (authenticatedUser == null || !authenticatedUser.getId().equals(candidateId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only perform swipe actions for your own user");
        }

        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(
                apartmentMatchService.processSwipe(candidateId, apartmentId, isCandidateAction, interest));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PatchMapping("/apartmentMatch/{apartmentMatchId}/status/successful")
    public ResponseEntity<ApartmentMatchDTO> updateApartmentMatchStatus(@PathVariable Integer apartmentMatchId) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO
                .fromApartmentMatchEntity(apartmentMatchService.successfulMatch(apartmentMatchId));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PatchMapping("/apartmentMatch/{apartmentMatchId}/status/canceled")
    public ResponseEntity<ApartmentMatchDTO> cancelApartmentMatch(@PathVariable Integer apartmentMatchId) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO
                .fromApartmentMatchEntity(apartmentMatchService.cancelMatch(apartmentMatchId));
        return ResponseEntity.ok(apartmentMatch);
    } 

    @PreAuthorize("hasRole('TENANT')")
    @PostMapping("/swipe/apartment/{apartmentId}/tenant")
    public ResponseEntity<?> processSwipe(@PathVariable Integer apartmentId, @RequestBody boolean interest) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.processSwipe(apartmentId, interest));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @PostMapping("/apartmentMatch/{apartmentMatchId}/respond-request")
    public ResponseEntity<ApartmentMatchDTO> processLandlordAction(@PathVariable Integer apartmentMatchId, @RequestParam boolean interest) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.processLandlordAction(apartmentMatchId, interest));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/apartment/{apartmentId}/interested-candidates/{status}")
    public ResponseEntity<List<ApartmentMatchLandlordDTO>> getInterestedCandidates(@PathVariable Integer apartmentId, @PathVariable MatchStatus status) {
        List<ApartmentMatchLandlordDTO> apartmentMatches = ApartmentMatchLandlordDTO.fromApartmentMatchEntityList(apartmentMatchService.findInterestedCandidatesByApartmentIdAndStatus(apartmentId, status));
        return ResponseEntity.ok(apartmentMatches);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/{userId}/interested-candidates/{status}")
    public ResponseEntity<List<ApartmentMatchLandlordDTO>> getInterestedCandidatesByUserId(@PathVariable Integer userId, @PathVariable MatchStatus status) {
        List<ApartmentMatchLandlordDTO> apartmentMatches = ApartmentMatchLandlordDTO.fromApartmentMatchEntityList(apartmentMatchService.findInterestedCandidatesByUserIdAndStatus(userId, status));
        return ResponseEntity.ok(apartmentMatches);
    }

    @PreAuthorize("hasRole('TENANT')")
    @GetMapping("/my-requests/{status}")
    public ResponseEntity<List<ApartmentMatchSummaryDTO>> getAllTenantRequest(@PathVariable MatchStatus status) {
        List<ApartmentMatchEntity> matches = apartmentMatchService.findTenantRequestByUserIdAndStatus(status);
        List<ApartmentMatchSummaryDTO> apartmentMatches = ApartmentMatchSummaryDTO.fromApartmentMatchEntityList(matches);
        return ResponseEntity.ok(apartmentMatches);
    }

    @PreAuthorize("hasRole('TENANT')")
    @PatchMapping("/apartmentMatch/{apartmentMatchId}/tenant-match-details")
    public ResponseEntity<ApartmentMatchLandlordDTO> getApartmentMatchDetailsForTenant(@PathVariable Integer apartmentMatchId) {
        ApartmentMatchLandlordDTO apartmentMatch = ApartmentMatchLandlordDTO.fromApartmentMatchEntity(apartmentMatchService.findMyMatchForTenant(apartmentMatchId));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/apartmentMatch/{apartmentMatchId}/landlord-match-details")
    public ResponseEntity<ApartmentMatchTenantDTO> getApartmentMatchDetailsForLandlord(@PathVariable Integer apartmentMatchId) {
        ApartmentMatchTenantDTO apartmentMatch = ApartmentMatchTenantDTO.fromApartmentMatchEntity(apartmentMatchService.findMyMatchForLandlord(apartmentMatchId));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @PostMapping("/apartmentMatch/{apartmentMatchId}/send-invitation")
    public ResponseEntity<ApartmentMatchLandlordDTO> sendInvitation(@PathVariable Integer apartmentMatchId) {
        ApartmentMatchLandlordDTO apartmentMatch = ApartmentMatchLandlordDTO.fromApartmentMatchEntity(apartmentMatchService.sendInvitation(apartmentMatchId));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PreAuthorize("hasRole('TENANT')")
    @PostMapping("/apartmentMatch/{apartmentMatchId}/respond-invitation")
    public ResponseEntity<ApartmentMatchDTO> respondToInvitation(@PathVariable Integer apartmentMatchId, @RequestBody boolean accepted) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.respondToInvitation(apartmentMatchId, accepted));
        return ResponseEntity.ok(apartmentMatch);
    }

}
