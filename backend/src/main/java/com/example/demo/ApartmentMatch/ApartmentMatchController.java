package com.example.demo.ApartmentMatch;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ApartmentMatch.DTOs.ApartmentMatchDTO;
import com.example.demo.ApartmentMatch.DTOs.ApartmentMatchLandlordDTO;
import com.example.demo.ApartmentMatch.DTOs.ApartmentMatchSummaryDTO;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@RestController
@RequestMapping("/api/apartments-matches")
public class ApartmentMatchController {

    private final ApartmentMatchService apartmentMatchService;
    private final UserService userService;

    public ApartmentMatchController(ApartmentMatchService apartmentMatchService, UserService userService) {
        this.apartmentMatchService = apartmentMatchService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<ApartmentMatchDTO>> getAllApartmentMatches() {
        List<ApartmentMatchDTO> apartmentMatches = ApartmentMatchDTO.fromApartmentMatchEntityList(apartmentMatchService.findAllApartmentMatches());
        return ResponseEntity.ok(apartmentMatches);
    }

    @GetMapping("/candidate/{candidateId}/apartment/{apartmentId}")
    public ResponseEntity<ApartmentMatchDTO> getApartmentMatchByCandidateAndApartment(@PathVariable Integer candidateId, @PathVariable Integer apartmentId) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.findApartmentMatchByCandidateAndApartment(candidateId, apartmentId));
        return ResponseEntity.ok(apartmentMatch);
    }

    @GetMapping("/candidate/{candidateId}/status/{matchStatus}")
    public ResponseEntity<List<ApartmentMatchDTO>> getApartmentMatchesByCandidateId(@PathVariable Integer candidateId, @PathVariable MatchStatus matchStatus) {
        List<ApartmentMatchDTO> apartmentMatches = ApartmentMatchDTO.fromApartmentMatchEntityList(apartmentMatchService.findMatchesByCandidateIdAndMatchStatus(candidateId,matchStatus));
        return ResponseEntity.ok(apartmentMatches);
    }

    @GetMapping("/apartment/{apartmentId}/status/{matchStatus}")
    public ResponseEntity<List<ApartmentMatchDTO>> getApartmentMatchesByApartmentId(@PathVariable Integer apartmentId, @PathVariable MatchStatus matchStatus) {
        List<ApartmentMatchDTO> apartmentMatches = ApartmentMatchDTO.fromApartmentMatchEntityList(apartmentMatchService.findMatchesByApartmentIdAndMatchStatus(apartmentId,matchStatus));
        return ResponseEntity.ok(apartmentMatches);
    }

    @DeleteMapping("/apartment/{apartmentId}")
    public ResponseEntity<Void> finalizeMatchProcess(@PathVariable Integer apartmentId) {
        apartmentMatchService.finalizeMatchProcess(apartmentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('TENANT')")
    @PostMapping("/swipe/apartment/{apartmentId}/action")
    public ResponseEntity<?> processSwipe(@PathVariable Integer apartmentId, @RequestBody boolean interest) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.processSwipe(apartmentId, interest));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @PostMapping("/apartmentMatch/{apartmentMatchId}/landlord-action")
    public ResponseEntity<ApartmentMatchDTO> processLandlordAction(@PathVariable Integer apartmentMatchId, @RequestBody boolean interest) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.processLandlordAction(apartmentMatchId, interest));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PreAuthorize("hasRole('TENANT')")
    @PatchMapping("/apartmentMatch/{apartmentMatchId}/match")
    public ResponseEntity<ApartmentMatchLandlordDTO> getApartmentMatchDetails(@PathVariable Integer apartmentMatchId) {
        ApartmentMatchLandlordDTO apartmentMatch = ApartmentMatchLandlordDTO.fromApartmentMatchEntity(apartmentMatchService.findMyMatch(apartmentMatchId));
        return ResponseEntity.ok(apartmentMatch);
    }


    @PatchMapping("/apartmentMatch/{apartmentMatchId}/status/successful")
    public ResponseEntity<ApartmentMatchDTO> updateApartmentMatchStatus(@PathVariable Integer apartmentMatchId) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.successfulMatch(apartmentMatchId));
        return ResponseEntity.ok(apartmentMatch);
    }

    @PatchMapping("/apartmentMatch/{apartmentMatchId}/status/canceled")
    public ResponseEntity<ApartmentMatchDTO> cancelApartmentMatch(@PathVariable Integer apartmentMatchId) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.cancelMatch(apartmentMatchId));
        return ResponseEntity.ok(apartmentMatch);
    } 

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/apartment/{apartmentId}/interested-candidates")
    public ResponseEntity<List<ApartmentMatchLandlordDTO>> getInterestedCandidates(@PathVariable Integer apartmentId) {
        List<ApartmentMatchLandlordDTO> apartmentMatches = ApartmentMatchLandlordDTO.fromApartmentMatchEntityList(apartmentMatchService.findInterestedCandidatesByApartmentId(apartmentId));
        return ResponseEntity.ok(apartmentMatches);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/{userId}/interested-candidates")
    public ResponseEntity<List<ApartmentMatchLandlordDTO>> getInterestedCandidatesByUserId(@PathVariable Integer userId) {
        List<ApartmentMatchLandlordDTO> apartmentMatches = ApartmentMatchLandlordDTO.fromApartmentMatchEntityList(apartmentMatchService.findInterestedCandidatesByUserId(userId));
        return ResponseEntity.ok(apartmentMatches);
    }

    @PreAuthorize("hasRole('TENANT')")
    @GetMapping("/my-matches")
    public ResponseEntity<List<ApartmentMatchSummaryDTO>> getAllTenantRequest() {
        UserEntity authenticatedUser = userService.findCurrentUserEntity();
        List<ApartmentMatchEntity> matches = apartmentMatchService.findTenantRequestByUserId(authenticatedUser.getId());
        List<ApartmentMatchSummaryDTO> apartmentMatches = ApartmentMatchSummaryDTO.fromApartmentMatchEntityList(matches);
        return ResponseEntity.ok(apartmentMatches);
    }

}
