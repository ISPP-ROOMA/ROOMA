package com.example.demo.Apartment;

import com.example.demo.Apartment.DTOs.UpdateApartment;
import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;

import jakarta.validation.Valid;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.Apartment.DTOs.CreateApartment;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/my")
    public ResponseEntity<List<ApartmentDTO>> getMyApartments() {
        List<ApartmentDTO> apartments = ApartmentDTO.fromApartmentEntityList(apartmentsService.findMyApartments());
        return ResponseEntity.ok(apartments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApartmentEntity> getApartmentById(@PathVariable Integer id) {
        ApartmentEntity apartments = apartmentsService.findById(id);
        return ResponseEntity.ok(apartments);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApartmentDTO> createApartment(
        @RequestPart("data") @Valid CreateApartment apartment,
        @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        ApartmentEntity createdApartment = apartmentsService.createWithImages(apartment, images);
        return new ResponseEntity<>(ApartmentDTO.fromApartmentEntity(createdApartment), HttpStatus.CREATED);
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
