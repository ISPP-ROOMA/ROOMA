package com.example.demo.Apartment;

import com.example.demo.Apartment.DTOs.UpdateApartment;
import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.Apartment.DTOs.ApartmentDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentsService;

    private final ApartmentPhotoService apartmentPhotoService;

    public ApartmentController(ApartmentService apartmentsService, ApartmentPhotoService apartmentPhotoService) {
        this.apartmentsService = apartmentsService;
        this.apartmentPhotoService = apartmentPhotoService;
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

    @PostMapping
    public ResponseEntity<ApartmentEntity> createApartment(@RequestBody ApartmentEntity apartments) {
        ApartmentEntity createdApartment = apartmentsService.save(apartments);

        return new ResponseEntity<>(createdApartment, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApartmentEntity> updateApartment(@PathVariable Integer id, @RequestBody UpdateApartment apartments) {
        ApartmentEntity apartmentsToUpdate = new ApartmentEntity();
        apartmentsToUpdate.setTitle(apartments.title());
        apartmentsToUpdate.setDescription(apartments.description());
        apartmentsToUpdate.setPrice(apartments.price());
        apartmentsToUpdate.setBills(apartments.bills());
        apartmentsToUpdate.setUbication(apartments.ubication());
        apartmentsToUpdate.setState(apartments.state());
        ApartmentEntity updatedApartment = apartmentsService.update(id, apartmentsToUpdate);
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

    @GetMapping("/{id}/photos")
    public ResponseEntity<?> getApartmentAndPhotos(@PathVariable Integer id) {
        ApartmentEntity apartment = apartmentsService.findById(id);
        List<ApartmentPhotoEntity> images = apartmentPhotoService.findPhotosByApartmentId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("apartment", apartment);
        response.put("images", images);

        return ResponseEntity.ok(response);
    }
}
