package com.example.demo.Incident;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/apartments/{apartmentId}/incidents")
public class IncidentController {
    
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }


    @GetMapping()
    public ResponseEntity<?> getIncidentsByApartmentId(@PathVariable Integer apartmentId) {
        return ResponseEntity.ok(incidentService.findIncidentsByApartmentId(apartmentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentEntity> getIncidentById(@PathVariable Integer id) {
        IncidentEntity incident = incidentService.findIncidentById(id);
        return ResponseEntity.ok(incident);
    }

    @PostMapping()
    public ResponseEntity<IncidentEntity> createIncident(@PathVariable Integer apartmentId, @RequestBody IncidentEntity incident) {
        IncidentEntity createdIncident = incidentService.createIncident(apartmentId, incident);
        return ResponseEntity.status(201).body(createdIncident);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentEntity> updateIncident(@PathVariable Integer id, @RequestBody IncidentEntity incident) {
        IncidentEntity updatedIncident = incidentService.updateIncident(id, incident);
        return ResponseEntity.ok(updatedIncident);
    }

}