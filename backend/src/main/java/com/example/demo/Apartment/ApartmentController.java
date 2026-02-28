package com.example.demo.Apartment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.Apartment.DTOs.CreateApartment;
import com.example.demo.Apartment.DTOs.UpdateApartment;
import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberRepository;
import com.example.demo.MemberApartment.DTOs.ApartmentMemberDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentsService;
    private final ApartmentMemberRepository apartmentMemberRepository;
    private final ApartmentPhotoService apartmentPhotoService;

    public ApartmentController(ApartmentService apartmentsService, 
                               ApartmentMemberRepository apartmentMemberRepository,
                               ApartmentPhotoService apartmentPhotoService) {
        this.apartmentsService = apartmentsService;
        this.apartmentMemberRepository = apartmentMemberRepository;
        this.apartmentPhotoService = apartmentPhotoService;
    }

    private ApartmentDTO mapToDTOWithMembers(ApartmentEntity apartment) {
        List<ApartmentMemberEntity> members = apartmentMemberRepository.findByApartmentId(apartment.getId());
        List<ApartmentMemberDTO> memberDTOs = ApartmentMemberDTO.fromEntityList(members);
        return ApartmentDTO.fromApartmentEntityWithMembers(apartment, memberDTOs);
    }

    @GetMapping
    public ResponseEntity<List<ApartmentDTO>> getAllApartments() {
        List<ApartmentDTO> apartments = apartmentsService.findAll().stream()
                .map(this::mapToDTOWithMembers)
                .collect(Collectors.toList());
        return ResponseEntity.ok(apartments);
    }

    @PreAuthorize("hasRole('LANDLORD')")
    @GetMapping("/my")
    public ResponseEntity<List<ApartmentDTO>> getMyApartments() {
        List<ApartmentDTO> apartments = apartmentsService.findMyApartments().stream()
                .map(this::mapToDTOWithMembers)
                .collect(Collectors.toList());
        return ResponseEntity.ok(apartments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApartmentDTO> getApartmentById(@PathVariable Integer id) {
        ApartmentEntity apartment = apartmentsService.findById(id);
        return ResponseEntity.ok(mapToDTOWithMembers(apartment));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApartmentDTO> createApartment(
            @RequestPart("data") @Valid CreateApartment apartment,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        ApartmentEntity createdApartment = apartmentsService.createWithImages(apartment, images);
        return new ResponseEntity<>(ApartmentDTO.fromApartmentEntity(createdApartment), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApartmentEntity> updateApartment(@PathVariable Integer id,
            @RequestBody UpdateApartment apartment) {
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
        response.put("apartment", ApartmentDTO.fromApartmentEntity(apartment));
        response.put("images", images);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ApartmentDTO>> searchApartments(
            @RequestParam(required = false) String ubication,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) ApartmentState state) {

        List<ApartmentEntity> apartmentsEntityList = apartmentsService.search(ubication, minPrice, maxPrice, state);
        
        List<ApartmentDTO> apartmentDTOs = apartmentsEntityList.stream()
                .map(this::mapToDTOWithMembers)
                .collect(Collectors.toList());

        return ResponseEntity.ok(apartmentDTOs);
    }
}
