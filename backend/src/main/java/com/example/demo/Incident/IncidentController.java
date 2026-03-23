package com.example.demo.Incident;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Incident.DTOs.CreateIncidentRequest;
import com.example.demo.Incident.DTOs.IncidentDTO;
import com.example.demo.Incident.DTOs.LandlordIncidentStatusUpdateRequest;
import com.example.demo.Incident.DTOs.TenantRejectResolutionRequest;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/apartments/{apartmentId}/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }


    @GetMapping()
    @PreAuthorize("hasAnyRole('TENANT','LANDLORD','ADMIN')")
    public ResponseEntity<List<IncidentDTO>> getIncidentsByApartmentId(
            @PathVariable Integer apartmentId,
            @RequestParam(required = false) IncidentBucket bucket
    ) {
        return ResponseEntity.ok(incidentService.findIncidentsByApartmentId(apartmentId, bucket));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT','LANDLORD','ADMIN')")
    public ResponseEntity<IncidentDTO> getIncidentById(@PathVariable Integer apartmentId, @PathVariable Integer id) {
        IncidentDTO incident = incidentService.findIncidentById(apartmentId, id);
        return ResponseEntity.ok(incident);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<IncidentDTO> createIncident(
            @PathVariable Integer apartmentId,
            @RequestPart("data") @Valid CreateIncidentRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        IncidentDTO createdIncident = incidentService.createIncident(apartmentId, request, images);
        return ResponseEntity.status(201).body(createdIncident);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<IncidentDTO> updateIncidentStatusByLandlord(
            @PathVariable Integer apartmentId,
            @PathVariable Integer id,
            @Valid @RequestBody LandlordIncidentStatusUpdateRequest request
    ) {
        IncidentDTO updatedIncident = incidentService.updateIncidentStatusByLandlord(apartmentId, id, request.status());
        return ResponseEntity.ok(updatedIncident);
    }

    @PatchMapping("/{id}/confirm-solution")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<IncidentDTO> confirmSolution(
            @PathVariable Integer apartmentId,
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(incidentService.confirmSolutionByTenant(apartmentId, id));
    }

    @PatchMapping("/{id}/reject-solution")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<IncidentDTO> rejectSolution(
            @PathVariable Integer apartmentId,
            @PathVariable Integer id,
            @Valid @RequestBody TenantRejectResolutionRequest request
    ) {
        return ResponseEntity.ok(incidentService.rejectSolutionByTenant(apartmentId, id, request.reason()));
    }
}