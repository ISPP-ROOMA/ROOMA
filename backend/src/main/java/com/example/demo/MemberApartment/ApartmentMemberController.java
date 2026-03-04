package com.example.demo.MemberApartment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.MemberApartment.DTOs.ApartmentMemberDTO;
import com.example.demo.MemberApartment.DTOs.CreateApartmentMember;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/apartments/{apartmentId}/members")
public class ApartmentMemberController {

    private final ApartmentMemberService apartmentMemberService;

    public ApartmentMemberController(ApartmentMemberService apartmentMemberService) {
        this.apartmentMemberService = apartmentMemberService;
    }

    @GetMapping
    public ResponseEntity<List<ApartmentMemberDTO>> list(@PathVariable Integer apartmentId) {
        return ResponseEntity.ok(
                ApartmentMemberDTO.fromEntityList(apartmentMemberService.listMembers(apartmentId)));
    }

    @PostMapping
    public ResponseEntity<ApartmentMemberDTO> add(
            @PathVariable Integer apartmentId,
            @Valid @RequestBody CreateApartmentMember request) {
        ApartmentMemberEntity member = apartmentMemberService.addMember(
                apartmentId,
                request.userId(),
                request.joinDate());

        return new ResponseEntity<>(ApartmentMemberDTO.fromEntity(member), HttpStatus.CREATED);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> remove(
            @PathVariable Integer apartmentId,
            @PathVariable Integer memberId) {
        apartmentMemberService.removeMember(apartmentId, memberId);
        return ResponseEntity.noContent().build();
    }
}