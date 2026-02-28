package com.example.demo.Apartment;

import com.example.demo.Apartment.DTOs.CreateApartment;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ApartmentService {

    private final ApartmentRepository apartmentsRepository;
    private final UserService userService;
    private final ApartmentPhotoService apartmentPhotoService;

    public ApartmentService(ApartmentRepository apartmentsRepository,
                            UserService userService,
                            ApartmentPhotoService apartmentPhotoService) {
        this.apartmentsRepository = apartmentsRepository;
        this.userService = userService;
        this.apartmentPhotoService = apartmentPhotoService;
    }

    @Transactional
    public ApartmentEntity save(ApartmentEntity newApartment) {
        String username = userService.findCurrentUser();
        Optional<UserEntity> user = userService.findByEmail(username);

        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        newApartment.setUser(user.get());

        return apartmentsRepository.save(newApartment);
    }

    @Transactional
    public ApartmentEntity createWithImages(CreateApartment dto, List<MultipartFile> images) {
        ApartmentEntity apartment = save(CreateApartment.fromDTO(dto));
        apartmentPhotoService.saveImages(apartment, images, false);
        return apartment;
    }

    @Transactional(readOnly = true)
    public List<ApartmentEntity> findAll() {
        return apartmentsRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ApartmentEntity findById(Integer id) {
        return apartmentsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found"));
    }

    public List<ApartmentEntity> findMyApartments() {
        String username = userService.findCurrentUser();
        Optional<UserEntity> user = userService.findByEmail(username);

        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        return apartmentsRepository.findAllByUserId(user.get().getId());
    }

    @Transactional
    public ApartmentEntity update(Integer id, ApartmentEntity apartments) {
        ApartmentEntity existingApartment = findById(id);

        existingApartment.setTitle(apartments.getTitle());
        existingApartment.setDescription(apartments.getDescription());
        existingApartment.setPrice(apartments.getPrice());
        existingApartment.setBills(apartments.getBills());
        existingApartment.setUbication(apartments.getUbication());
        existingApartment.setState(apartments.getState());

        return apartmentsRepository.save(existingApartment);
    }

    @Transactional
    public void deleteById(Integer id) {
        if (!apartmentsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        apartmentsRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ApartmentEntity> search(String ubication, Double minPrice, Double maxPrice, ApartmentState state) {
        return apartmentsRepository.search(ubication, minPrice, maxPrice, state);
    }

}
