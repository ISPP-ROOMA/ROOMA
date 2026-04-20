package com.example.demo.MemberApartment;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.MemberApartment.DTOs.ApartmentRuleDTO;
import com.example.demo.MemberApartment.DTOs.UpdateApartmentRule;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/apartments/{apartmentId}/rules")
public class ApartmentRuleController {

    private final ApartmentRuleService apartmentRuleService;

    public ApartmentRuleController(ApartmentRuleService apartmentRuleService) {
        this.apartmentRuleService = apartmentRuleService;
    }

    @PutMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ApartmentRuleDTO> updateRules(
            @PathVariable Integer apartmentId,
            @Valid @RequestBody UpdateApartmentRule request) {
        ApartmentRuleDTO dto = apartmentRuleService.updateRules(apartmentId, request);
        return ResponseEntity.ok(dto);
    }
}

