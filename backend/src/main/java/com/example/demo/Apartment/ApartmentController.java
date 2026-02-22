package com.example.demo.Apartment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.Apartment.DTOs.UpdateApartment;

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

    @PostMapping
    public ResponseEntity<ApartmentEntity> createApartment(@RequestBody ApartmentEntity apartments) {
        ApartmentEntity createdApartment = apartmentsService.save(apartments);

        return new ResponseEntity<>(createdApartment, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApartmentEntity> updateApartment(@PathVariable Integer id,
            @RequestBody UpdateApartment apartments) {
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

    @GetMapping("/search")
    public ResponseEntity<List<ApartmentDTO>> searchApartments(
            @RequestParam(required = false) String ubication,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String state) {

        List<ApartmentEntity> apartments = apartmentsService.search(ubication, minPrice, maxPrice, state);
        List<ApartmentDTO> apartmentDTOs = ApartmentDTO.fromApartmentEntityList(apartments);

        return ResponseEntity.ok(apartmentDTOs);
    }
}
