package com.example.demo.Apartment;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.example.demo.Apartment.DTOs.CreateApartment;
import com.example.demo.Apartment.DTOs.UpdateApartment;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberRepository;
import com.example.demo.MemberApartment.DTOs.ApartmentMemberDTO;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentsService;
    private final ApartmentMemberRepository apartmentMemberRepository;

    public ApartmentController(ApartmentService apartmentsService,
            ApartmentMemberRepository apartmentMemberRepository) {
        this.apartmentsService = apartmentsService;
        this.apartmentMemberRepository = apartmentMemberRepository;
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

    @GetMapping("/{id}")
    public ResponseEntity<ApartmentEntity> getApartmentById(@PathVariable Integer id) {
        ApartmentEntity apartments = apartmentsService.findById(id);
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

    @PostMapping
    public ResponseEntity<ApartmentEntity> createApartment(@RequestBody CreateApartment apartments) {

        ApartmentEntity createdApartment = apartmentsService.save(CreateApartment.fromDTO(apartments));

        return new ResponseEntity<>(createdApartment, HttpStatus.CREATED);
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
