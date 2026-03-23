package com.example.demo.MemberApartment;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.MemberApartment.DTOs.ReglaViviendaDTO;
import com.example.demo.MemberApartment.DTOs.UpdateReglaVivienda;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/apartments/{apartmentId}/rules")
public class ReglaViviendaController {

    private final ReglaViviendaService reglaViviendaService;

    public ReglaViviendaController(ReglaViviendaService reglaViviendaService) {
        this.reglaViviendaService = reglaViviendaService;
    }

    @PutMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ReglaViviendaDTO> updateRules(
            @PathVariable Integer apartmentId,
            @Valid @RequestBody UpdateReglaVivienda request) {
        ReglaViviendaDTO dto = reglaViviendaService.updateRules(apartmentId, request);
        return ResponseEntity.ok(dto);
    }
}

