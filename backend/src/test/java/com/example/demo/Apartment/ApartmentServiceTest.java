package com.example.demo.Apartment;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Apartment.DTOs.CreateApartment;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@ExtendWith(MockitoExtension.class)
public class ApartmentServiceTest {

    private ApartmentService apartmentService;

    @Mock
    private ApartmentRepository apartmentRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApartmentPhotoService apartmentPhotoService;

    @BeforeEach
    void setUp() {
        apartmentService = new ApartmentService(apartmentRepository, userService, apartmentPhotoService);
    }

    @Test
    @DisplayName("save assigns current user and persists apartment")
    public void save_AssignsCurrentUserAndPersists() {
        ApartmentEntity apartment = baseApartment();
        UserEntity landlord = user(10, "landlord@test.com", Role.LANDLORD);

        when(userService.findCurrentUser()).thenReturn("landlord@test.com");
        when(userService.findByEmail("landlord@test.com")).thenReturn(Optional.of(landlord));
        when(apartmentRepository.save(any(ApartmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ApartmentEntity saved = apartmentService.save(apartment);

        assertNotNull(saved);
        assertEquals(landlord.getId(), saved.getUser().getId());
        verify(apartmentRepository).save(apartment);
    }

    @Test
    @DisplayName("save throws when current user is not found")
    public void save_WhenCurrentUserNotFound_Throws() {
        ApartmentEntity apartment = baseApartment();

        when(userService.findCurrentUser()).thenReturn("missing@test.com");
        when(userService.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> apartmentService.save(apartment));
    }

    @Test
    @DisplayName("createWithImages converts dto, saves and delegates photo save with replace false")
    public void createWithImages_SavesAndDelegatesPhotoSave() {
        CreateApartment dto = new CreateApartment("title", "desc", 500.0, "wifi", "Madrid", "ACTIVE");
        UserEntity landlord = user(11, "owner@test.com", Role.LANDLORD);
        ApartmentEntity persisted = baseApartment();
        persisted.setId(100);

        when(userService.findCurrentUser()).thenReturn("owner@test.com");
        when(userService.findByEmail("owner@test.com")).thenReturn(Optional.of(landlord));
        when(apartmentRepository.save(any(ApartmentEntity.class))).thenReturn(persisted);

        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        List<MultipartFile> images = List.of(image);

        ApartmentEntity result = apartmentService.createWithImages(dto, images);

        assertEquals(100, result.getId());
        verify(apartmentPhotoService).saveImages(eq(persisted), eq(images), eq(false));
    }

    @Test
    @DisplayName("findById returns apartment when present")
    public void findById_ReturnsApartmentWhenPresent() {
        ApartmentEntity apartment = baseApartment();
        apartment.setId(200);

        when(apartmentRepository.findById(200)).thenReturn(Optional.of(apartment));

        ApartmentEntity found = apartmentService.findById(200);

        assertEquals(200, found.getId());
    }

    @Test
    @DisplayName("findById throws when apartment is missing")
    public void findById_WhenMissing_Throws() {
        when(apartmentRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> apartmentService.findById(999));
    }

    @Test
    @DisplayName("findMyApartments returns current user apartments")
    public void findMyApartments_ReturnsCurrentUserApartments() {
        UserEntity landlord = user(12, "landlord2@test.com", Role.LANDLORD);
        ApartmentEntity apartment = baseApartment();
        apartment.setId(300);

        when(userService.findCurrentUser()).thenReturn("landlord2@test.com");
        when(userService.findByEmail("landlord2@test.com")).thenReturn(Optional.of(landlord));
        when(apartmentRepository.findAllByUserId(12)).thenReturn(List.of(apartment));

        List<ApartmentEntity> result = apartmentService.findMyApartments();

        assertEquals(1, result.size());
        assertEquals(300, result.get(0).getId());
    }

    @Test
    @DisplayName("findMyApartments throws when current user does not exist")
    public void findMyApartments_WhenUserMissing_Throws() {
        when(userService.findCurrentUser()).thenReturn("no-user@test.com");
        when(userService.findByEmail("no-user@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> apartmentService.findMyApartments());
    }

    @Test
    @DisplayName("update mutates target fields and saves existing apartment")
    public void update_MutatesAndSavesExistingApartment() {
        ApartmentEntity existing = baseApartment();
        existing.setId(400);
        UserEntity owner = user(50, "owner50@test.com", Role.LANDLORD);
        existing.setUser(owner);

        ApartmentEntity updates = new ApartmentEntity(
                "new title",
                "new desc",
                999.0,
                "new bills",
                "Barcelona",
                ApartmentState.MATCHING,
                null
        );

        // El servicio ahora verifica que el usuario actual sea el propietario
        when(userService.findCurrentUserEntity()).thenReturn(owner);
        when(apartmentRepository.findById(400)).thenReturn(Optional.of(existing));
        when(apartmentRepository.save(any(ApartmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ApartmentEntity updated = apartmentService.update(400, updates);

        assertEquals("new title", updated.getTitle());
        assertEquals("new desc", updated.getDescription());
        assertEquals(999.0, updated.getPrice());
        assertEquals("new bills", updated.getBills());
        assertEquals("Barcelona", updated.getUbication());
        assertEquals(ApartmentState.MATCHING, updated.getState());
        assertEquals(50, updated.getUser().getId());
        verify(apartmentRepository).save(existing);
    }

    @Test
    @DisplayName("deleteById deletes existing apartment")
    public void deleteById_WhenExists_Deletes() {
        when(apartmentRepository.existsById(500)).thenReturn(true);

        apartmentService.deleteById(500);

        verify(apartmentRepository).deleteById(500);
    }

    @Test
    @DisplayName("deleteById throws when apartment does not exist")
    public void deleteById_WhenMissing_Throws() {
        when(apartmentRepository.existsById(501)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> apartmentService.deleteById(501));
    }

    @Test
    @DisplayName("checkUserIsLandlord passes when landlord matches user")
    public void checkUserIsLandlord_WhenMatches_Passes() {
        UserEntity landlord = user(22, "ll@test.com", Role.LANDLORD);
        when(apartmentRepository.findLandlordByApartmentId(600)).thenReturn(Optional.of(landlord));

        apartmentService.checkUserIsLandlord(600, 22);
    }

    @Test
    @DisplayName("checkUserIsLandlord throws when landlord does not match user")
    public void checkUserIsLandlord_WhenMismatch_Throws() {
        UserEntity landlord = user(23, "ll23@test.com", Role.LANDLORD);
        when(apartmentRepository.findLandlordByApartmentId(602)).thenReturn(Optional.of(landlord));

        assertThrows(BadRequestException.class, () -> apartmentService.checkUserIsLandlord(602, 99));
    }

    @Test
    @DisplayName("findLandlordByApartmentId returns landlord when found")
    public void findLandlordByApartmentId_ReturnsLandlord() {
        UserEntity landlord = user(24, "ll24@test.com", Role.LANDLORD);
        when(apartmentRepository.findLandlordByApartmentId(700)).thenReturn(Optional.of(landlord));

        UserEntity found = apartmentService.findLandlordByApartmentId(700);

        assertEquals(24, found.getId());
    }

    private ApartmentEntity baseApartment() {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setTitle("Apartment title");
        apartment.setDescription("Apartment description");
        apartment.setPrice(500.0);
        apartment.setBills("bills");
        apartment.setUbication("Madrid");
        apartment.setState(ApartmentState.ACTIVE);
        return apartment;
    }

    private UserEntity user(Integer id, String email, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRole(role);
        return user;
    }
}
