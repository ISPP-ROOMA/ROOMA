package com.example.demo.Apartment;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Apartment.DTOs.CreateApartment;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Idempotency.ApartmentCreateIdempotencyEntity;
import com.example.demo.Idempotency.ApartmentCreateIdempotencyRepository;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@Service
public class ApartmentService {

    private static final String CREATE_APARTMENT_ENDPOINT = "POST:/api/apartments";

    private final ApartmentRepository apartmentsRepository;
    private final UserService userService;
    private final ApartmentPhotoService apartmentPhotoService;
    private final ApartmentCreateIdempotencyRepository apartmentCreateIdempotencyRepository;

    public ApartmentService(ApartmentRepository apartmentsRepository,
            UserService userService,
            ApartmentPhotoService apartmentPhotoService,
            ApartmentCreateIdempotencyRepository apartmentCreateIdempotencyRepository) {
        this.apartmentsRepository = apartmentsRepository;
        this.userService = userService;
        this.apartmentPhotoService = apartmentPhotoService;
        this.apartmentCreateIdempotencyRepository = apartmentCreateIdempotencyRepository;
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
    public ApartmentEntity createWithImages(CreateApartment dto, List<MultipartFile> images, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Missing Idempotency-Key header");
        }

        UserEntity currentUser = userService.findCurrentUserEntity();
        String normalizedKey = idempotencyKey.trim();
        String requestFingerprint = buildRequestFingerprint(dto, images);

        ApartmentCreateIdempotencyEntity idempotencyEntry = getOrCreateIdempotencyEntry(
                currentUser,
                normalizedKey,
                requestFingerprint);

        if (idempotencyEntry.getApartment() != null) {
            return idempotencyEntry.getApartment();
        }

        ApartmentEntity apartment = CreateApartment.fromDTO(dto);
        apartment.setUser(currentUser);

        ApartmentEntity savedApartment = apartmentsRepository.save(apartment);
        apartmentPhotoService.saveImages(savedApartment, images, false);

        idempotencyEntry.setApartment(savedApartment);
        apartmentCreateIdempotencyRepository.save(idempotencyEntry);
        return savedApartment;
    }

    private ApartmentCreateIdempotencyEntity getOrCreateIdempotencyEntry(
            UserEntity currentUser,
            String idempotencyKey,
            String requestFingerprint) {
        Optional<ApartmentCreateIdempotencyEntity> existingEntry = apartmentCreateIdempotencyRepository
                .findByUserIdAndEndpointAndIdempotencyKey(currentUser.getId(), CREATE_APARTMENT_ENDPOINT, idempotencyKey);

        if (existingEntry.isPresent()) {
            validateFingerprint(existingEntry.get(), requestFingerprint);
            return existingEntry.get();
        }

        ApartmentCreateIdempotencyEntity newEntry = new ApartmentCreateIdempotencyEntity(
                currentUser,
                CREATE_APARTMENT_ENDPOINT,
                idempotencyKey,
                requestFingerprint);

        try {
            return apartmentCreateIdempotencyRepository.saveAndFlush(newEntry);
        } catch (DataIntegrityViolationException ex) {
            ApartmentCreateIdempotencyEntity persistedEntry = apartmentCreateIdempotencyRepository
                    .findByUserIdAndEndpointAndIdempotencyKey(currentUser.getId(), CREATE_APARTMENT_ENDPOINT, idempotencyKey)
                    .orElseThrow(() -> ex);

            validateFingerprint(persistedEntry, requestFingerprint);
            return persistedEntry;
        }
    }

    private void validateFingerprint(ApartmentCreateIdempotencyEntity entry, String requestFingerprint) {
        if (!entry.getRequestFingerprint().equals(requestFingerprint)) {
            throw new BadRequestException("Idempotency-Key was already used with a different payload");
        }
    }

    private String buildRequestFingerprint(CreateApartment dto, List<MultipartFile> images) {
        int imageCount = images == null ? 0 : images.size();
        return normalize(dto.title())
                + "|" + normalize(dto.description())
                + "|" + dto.price()
                + "|" + normalize(dto.bills())
                + "|" + normalize(dto.ubication())
                + "|" + normalize(dto.state())
                + "|" + imageCount;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
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

        UserEntity currentUser = userService.findCurrentUserEntity();
        if (existingApartment.getUser() == null ||
                !existingApartment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the landlord of this apartment can edit it");
        }

        existingApartment.setTitle(apartments.getTitle());
        existingApartment.setDescription(apartments.getDescription());
        existingApartment.setPrice(apartments.getPrice());
        existingApartment.setBills(apartments.getBills());
        existingApartment.setUbication(apartments.getUbication());
        existingApartment.setState(apartments.getState());
        existingApartment.setIdealTenantProfile(apartments.getIdealTenantProfile());

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

    @Transactional(readOnly = true)
    public void checkUserIsLandlord(Integer apartmentId, Integer userId) {
        UserEntity landlord = apartmentsRepository.findLandlordByApartmentId(apartmentId).orElse(null);
        if (landlord == null || !landlord.getId().equals(userId)) {
            throw new BadRequestException("User is not the landlord of this apartment");
        }
    }

    @Transactional(readOnly = true)
    public UserEntity findLandlordByApartmentId(Integer apartmentId) {
        return apartmentsRepository.findLandlordByApartmentId(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found for this apartment"));
    }

    @Transactional(readOnly = true)
    public List<ApartmentEntity> findAllByUserId(Integer userId) {
        return apartmentsRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ApartmentEntity> getDeckForCandidate(Integer candidateId) {
        return apartmentsRepository.findDeckForCandidate(candidateId);
    }
}
