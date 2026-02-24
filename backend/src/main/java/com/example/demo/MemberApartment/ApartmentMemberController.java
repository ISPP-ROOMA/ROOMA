package com.example.demo.MemberApartment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.MemberApartment.DTOs.CreateApartmentMember;
import com.example.demo.MemberApartment.DTOs.ApartmentMemberDTO;
import com.example.demo.MemberApartment.DTOs.UpdateApartmentMember;

import java.util.List;

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
                request.role(),
                request.joinDate());

        return new ResponseEntity<>(ApartmentMemberDTO.fromEntity(member), HttpStatus.CREATED);
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<ApartmentMemberDTO> updateRole(
            @PathVariable Integer apartmentId,
            @PathVariable Integer memberId,
            @Valid @RequestBody UpdateApartmentMember request) {
        ApartmentMemberEntity member = apartmentMemberService.updateRole(
                apartmentId,
                memberId,
                request.role());

        return ResponseEntity.ok(ApartmentMemberDTO.fromEntity(member));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> remove(
            @PathVariable Integer apartmentId,
            @PathVariable Integer memberId) {
        apartmentMemberService.removeMember(apartmentId, memberId);
        return ResponseEntity.noContent().build();
    }
}