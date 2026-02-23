package com.example.demo.Apartment;

import com.example.demo.Apartment.DTOs.UpdateApartment;
import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.Apartment.DTOs.CreateApartment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentsService;

    public ApartmentController(ApartmentService apartmentsService) {
        this.apartmentsService = apartmentsService;
    }

    @GetMapping
    public ResponseEntity<List<ApartmentDTO>> getAllApartments() {
        List<ApartmentDTO> apartments = ApartmentDTO.fromApartmentEntityList(apartmentsService.findAll());
        return ResponseEntity.ok(apartments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApartmentEntity> getApartmentById(@PathVariable Integer id) {
        ApartmentEntity apartments = apartmentsService.findById(id);
        return ResponseEntity.ok(apartments);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/my")
    public ResponseEntity<List<ApartmentDTO>> getMyApartments() {
        List<ApartmentDTO> apartments = ApartmentDTO.fromApartmentEntityList(apartmentsService.findMyApartments());
        return ResponseEntity.ok(apartments);
    }

    @PostMapping
    public ResponseEntity<ApartmentEntity> createApartment(@RequestBody CreateApartment apartments) {
        
        ApartmentEntity createdApartment = apartmentsService.save(CreateApartment.fromDTO(apartments));

        return new ResponseEntity<>(createdApartment, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApartmentEntity> updateApartment(@PathVariable Integer id, @RequestBody UpdateApartment apartment) {
        ApartmentEntity updatedApartment = apartmentsService.update(id, UpdateApartment.fromDTO(apartment));
        return ResponseEntity.ok(updatedApartment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApartment(@PathVariable Integer id) {
        try {
            apartmentsService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
