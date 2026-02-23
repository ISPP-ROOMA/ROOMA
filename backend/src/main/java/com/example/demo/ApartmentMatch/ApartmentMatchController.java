package com.example.demo.ApartmentMatch;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ApartmentMatch.DTOs.ApartmentMatchDTO;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@RestController
@RequestMapping("/api/apartments-matches")
public class ApartmentMatchController {

    private final ApartmentMatchService apartmentMatchService;

    public ApartmentMatchController(ApartmentMatchService apartmentMatchService, UserService userService) {
        this.apartmentMatchService = apartmentMatchService;
    }

    @GetMapping
    public ResponseEntity<List<ApartmentMatchDTO>> getAllApartmentMatches() {
        List<ApartmentMatchDTO> apartmentMatches = ApartmentMatchDTO.fromApartmentMatchEntityList(apartmentMatchService.findAllApartmentMatches());
        return ResponseEntity.ok(apartmentMatches);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApartmentMatchDTO> getApartmentMatchById(@PathVariable Integer id) {
        ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.findApartmentMatchById(id));
        return ResponseEntity.ok(apartmentMatch);
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
        try {
            apartmentMatchService.finalizeMatchProcess(apartmentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/swipe/candidate/{candidateId}/apartment/{apartmentId}/action/{isCandidateAction}")
    public ResponseEntity<?> processSwipe(@PathVariable Integer candidateId, @PathVariable Integer apartmentId, 
        @PathVariable boolean isCandidateAction, @RequestBody boolean interest, @AuthenticationPrincipal UserEntity authenticatedUser) {
        try {
            if (authenticatedUser == null || !authenticatedUser.getId().equals(candidateId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only perform swipe actions for your own user");
            }

            // Hay que añadir tambien una verificacion para comprobar que la vivienda es del usuario que realiza la acción de swip


            // Si el usuario autenticado es el candidato y además es el arrendador, debe de devolver un 403 Forbidden, ya que no puede realizar acciones de swipe sobre un apartamento que él mismo ha publicado

            ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.processSwipe(candidateId, apartmentId, isCandidateAction, interest));
            return ResponseEntity.ok(apartmentMatch);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/apartment/{apartmentId}/status/successful")
    public ResponseEntity<ApartmentMatchDTO> updateApartmentMatchStatus(@PathVariable Integer apartmentMatchId) {
        try {
            ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.successfulMatch(apartmentMatchId));
            return ResponseEntity.ok(apartmentMatch);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/apartment/{apartmentId}/status/rejected")
    public ResponseEntity<ApartmentMatchDTO> rejectApartmentMatch(@PathVariable Integer apartmentMatchId) {
        try {
            ApartmentMatchDTO apartmentMatch = ApartmentMatchDTO.fromApartmentMatchEntity(apartmentMatchService.rejectMatch(apartmentMatchId));
            return ResponseEntity.ok(apartmentMatch);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    } 
}
