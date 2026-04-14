package com.example.demo.Incident;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;


import com.example.demo.Apartment.ApartmentService;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.Cloudinary.CloudinaryService;
import com.example.demo.User.UserService;
import com.example.demo.Incident.DTOs.IncidentDTO;
import com.example.demo.Incident.DTOs.CreateIncidentRequest;
import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.User.UserEntity;
import com.example.demo.User.Role;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.Exceptions.BadRequestException;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.argThat;


@ExtendWith(MockitoExtension.class)
public class IncidentServiceTest {
    private IncidentService incidentService;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentStatusHistoryRepository incidentStatusHistoryRepository;

    @Mock
    private ApartmentService apartmentService;

    @Mock
    private UserService userService;

    @Mock
    private ApartmentMemberService apartmentMemberService;

    @Mock
    private CloudinaryService cloudinaryService;

    @BeforeEach
    public void setUp() {
        incidentService = new IncidentService(incidentRepository, incidentStatusHistoryRepository, userService, apartmentService, apartmentMemberService, cloudinaryService);
    }

    @Test
    @DisplayName("findIncidentById should return incident when found")
    public void findIncidentByIdShouldReturnIncidentWhenFound() {
        Integer incidentId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(1);        
        user.setRole(Role.LANDLORD);
        UserEntity user2 = new UserEntity();
        user2.setId(2);
        user2.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(user);
        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setTenant(user2);
        incident.setLandlord(user);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setTitle("Test Incident");
        incident.setDescription("This is a test incident.");
        incident.setCategory(IncidentCategory.PLUMBING);
        incident.setZone(IncidentZone.KITCHEN);
        incident.setUrgency(IncidentUrgency.HIGH);


        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        IncidentDTO result = incidentService.findIncidentById(apartmentId, incidentId);

        assertNotNull(result);
        assertEquals(incidentId, result.id());
    }

    @Test
    @DisplayName("findIncidentById should throw ResourceNotFoundException when incident not found")
    public void findIncidentByIdShouldThrowResourceNotFoundExceptionWhenIncidentNotFound() {
        Integer incidentId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(1);
        user.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(user);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.findIncidentById(apartmentId, incidentId));
    }

    @Test
    @DisplayName("findIncidentById should throw ResourceNotFoundException when apartment not found")
    public void findIncidentByIdShouldThrowResourceNotFoundExceptionWhenApartmentNotFound() {
        Integer incidentId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(1);
        user.setRole(Role.LANDLORD);

        when(apartmentService.findById(apartmentId)).thenReturn(null);
        when(userService.findCurrentUserEntity()).thenReturn(user);

        assertThrows(ResourceNotFoundException.class, () -> incidentService.findIncidentById(apartmentId, incidentId));
    }

    @Test
    @DisplayName("findIncidentById should throw ForbiddenException when landlord tries to access incident of another apartment")
    public void findIncidentByIdShouldThrowForbiddenExceptionWhenLandlordTriesToAccessIncidentOfAnotherApartment() {
        Integer incidentId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(1);
        user.setRole(Role.LANDLORD);
        UserEntity user2 = new UserEntity();
        user2.setId(2);
        user2.setRole(Role.LANDLORD);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(user2);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(user);

        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.findIncidentById(apartmentId, incidentId));
    }

    @Test
    @DisplayName("findIncidentById should throw ForbiddenException when tenant membership is inactive")
    public void findIncidentByIdShouldThrowForbiddenExceptionWhenTenantMembershipIsInactive() {
        Integer incidentId = 1;
        Integer apartmentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        ApartmentMemberEntity inactiveMembership = new ApartmentMemberEntity();
        inactiveMembership.setApartment(apartment);
        inactiveMembership.setUser(tenant);
        inactiveMembership.setEndDate(LocalDate.now());

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(inactiveMembership);

        assertThrows(com.example.demo.Exceptions.ForbiddenException.class,
                () -> incidentService.findIncidentById(apartmentId, incidentId));
    }

    @Test
    @DisplayName("findIncidentById should throw ResourceNotFoundException when tenant requests incident from another apartment")
    public void findIncidentByIdShouldThrowResourceNotFoundExceptionWhenTenantRequestsIncidentFromAnotherApartment() {
        Integer incidentId = 1;
        Integer apartmentId = 1;
        Integer apartmentId2 = 2;

        UserEntity user = new UserEntity();
        user.setId(1);
        user.setRole(Role.TENANT);
        UserEntity user2 = new UserEntity();
        user2.setId(2);
        user2.setRole(Role.LANDLORD);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(user2);
        ApartmentEntity apartment2 = new ApartmentEntity();
        apartment2.setId(apartmentId2);
        apartment2.setUser(user2);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment2);

        ApartmentMemberEntity memberApartment = new ApartmentMemberEntity();
        memberApartment.setApartment(apartment);
        memberApartment.setUser(user);
        memberApartment.setEndDate(LocalDate.now().plusDays(1));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(apartmentMemberService.findByUserIdAndApartmentId(user.getId(), apartmentId)).thenReturn(memberApartment);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(ResourceNotFoundException.class, () -> incidentService.findIncidentById(apartmentId, incidentId));
    }

    @Test
    @DisplayName("findIncidentsByApartmentId should return list of active incidents for an apartment")
    public void findIncidentsByApartmentIdShouldReturnListOfIncidentsForLandlord() {
        Integer apartmentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);
        landlord.setEmail("landlord@test.com");

        UserEntity tenant = new UserEntity();
        tenant.setId(2);
        tenant.setRole(Role.TENANT);
        tenant.setEmail("tenant@test.com");

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        IncidentEntity incident1 = new IncidentEntity();
        incident1.setId(1);
        incident1.setApartment(apartment);
        incident1.setTenant(tenant);
        incident1.setLandlord(landlord);
        incident1.setTitle("Incident 1");
        incident1.setDescription("Description 1");
        incident1.setCategory(IncidentCategory.PLUMBING);
        incident1.setZone(IncidentZone.KITCHEN);
        incident1.setUrgency(IncidentUrgency.HIGH);
        incident1.setStatus(IncidentStatus.OPEN);

        IncidentEntity incident2 = new IncidentEntity();
        incident2.setId(2);
        incident2.setApartment(apartment);
        incident2.setTenant(tenant);
        incident2.setLandlord(landlord);
        incident2.setTitle("Incident 2");
        incident2.setDescription("Description 2");
        incident2.setCategory(IncidentCategory.PLUMBING);
        incident2.setZone(IncidentZone.KITCHEN);
        incident2.setUrgency(IncidentUrgency.MEDIUM);
        incident2.setStatus(IncidentStatus.IN_PROGRESS);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findByApartmentIdAndStatusInOrderByCreatedAtDesc(
            apartmentId,
            List.of(
                IncidentStatus.OPEN,
                IncidentStatus.RECEIVED,
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.TECHNICIAN_NOTIFIED,
                IncidentStatus.RESOLVED)))
            .thenReturn(List.of(incident1, incident2));

        var result = incidentService.findIncidentsByApartmentId(apartmentId, IncidentBucket.ACTIVE);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findIncidentsByApartmentId should return list of closed incidents for an apartment")
    public void findIncidentsByApartmentIdShouldReturnListOfClosedIncidentsForLandlord() {
        Integer apartmentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);
        landlord.setEmail("landlord@test.com");

        UserEntity tenant = new UserEntity();
        tenant.setId(2);
        tenant.setRole(Role.TENANT);
        tenant.setEmail("tenant@test.com");

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);
        IncidentEntity incident1 = new IncidentEntity();
        incident1.setId(1);
        incident1.setApartment(apartment);
        incident1.setTenant(tenant);
        incident1.setLandlord(landlord);
        incident1.setTitle("Incident 1");
        incident1.setDescription("Description 1");
        incident1.setCategory(IncidentCategory.PLUMBING);
        incident1.setZone(IncidentZone.KITCHEN);
        incident1.setUrgency(IncidentUrgency.HIGH);
        incident1.setStatus(IncidentStatus.CLOSED);
        IncidentEntity incident2 = new IncidentEntity();
        incident2.setId(2);
        incident2.setApartment(apartment);
        incident2.setTenant(tenant);
        incident2.setLandlord(landlord);
        incident2.setTitle("Incident 2");
        incident2.setDescription("Description 2");
        incident2.setCategory(IncidentCategory.ELECTRICITY);
        incident2.setZone(IncidentZone.LIVING_ROOM);
        incident2.setUrgency(IncidentUrgency.LOW);
        incident2.setStatus(IncidentStatus.CLOSED);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findByApartmentIdAndStatusInOrderByCreatedAtDesc(
            apartmentId,
            List.of(IncidentStatus.CLOSED, IncidentStatus.CLOSED_INACTIVITY)))
            .thenReturn(List.of(incident1, incident2));
        
        var result = incidentService.findIncidentsByApartmentId(apartmentId, IncidentBucket.CLOSED);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findIncidentsByApartmentId should return all incidents for an apartment when bucket is null")
    public void findIncidentsByApartmentIdShouldReturnAllIncidentsForApartmentWhenBucketIsNull() {
        Integer apartmentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);
        landlord.setEmail("landlord@test.com");
        UserEntity tenant = new UserEntity();
        tenant.setId(2);
        tenant.setRole(Role.TENANT);
        tenant.setEmail("tenant@test.com");

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);
        IncidentEntity incident1 = new IncidentEntity();
        incident1.setId(1);
        incident1.setApartment(apartment);
        incident1.setTenant(tenant);
        incident1.setLandlord(landlord);
        incident1.setTitle("Incident 1");
        incident1.setDescription("Description 1");
        incident1.setCategory(IncidentCategory.PLUMBING);
        incident1.setZone(IncidentZone.KITCHEN);
        incident1.setUrgency(IncidentUrgency.HIGH);
        incident1.setStatus(IncidentStatus.RECEIVED);
        IncidentEntity incident2 = new IncidentEntity();
        incident2.setId(2);
        incident2.setApartment(apartment);
        incident2.setTenant(tenant);
        incident2.setLandlord(landlord);
        incident2.setTitle("Incident 2");
        incident2.setDescription("Description 2");
        incident2.setCategory(IncidentCategory.ELECTRICITY);
        incident2.setZone(IncidentZone.LIVING_ROOM);
        incident2.setUrgency(IncidentUrgency.LOW);
        incident2.setStatus(IncidentStatus.CLOSED);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findByApartmentIdOrderByCreatedAtDesc(apartmentId)).thenReturn(List.of(incident1, incident2));

        var result = incidentService.findIncidentsByApartmentId(apartmentId, null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findIncidentsByApartmentId should throw ResourceNotFoundException when apartment not found")
    public void findIncidentsByApartmentIdShouldThrowResourceNotFoundExceptionWhenApartmentNotFound() {
        Integer apartmentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        when(apartmentService.findById(apartmentId)).thenReturn(null);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);

        assertThrows(ResourceNotFoundException.class, () -> incidentService.findIncidentsByApartmentId(apartmentId, IncidentBucket.ACTIVE));    
    }

    @Test
    @DisplayName("findIncidentsByApartmentId should throw ForbiddenException when current user tries to access incidents of another apartment")
    public void findIncidentsByApartmentIdShouldThrowForbiddenExceptionWhenCurrentUserTriesToAccessIncidentsOfAnotherApartment() {
        Integer apartmentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        UserEntity landlord2 = new UserEntity();
        landlord2.setId(2);
        landlord2.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord2);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);

        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.findIncidentsByApartmentId(apartmentId, IncidentBucket.ACTIVE));
    }

    @Test
    @DisplayName("findIncidentsByApartmentId should throw ForbiddenException when tenant membership is inactive")
    public void findIncidentsByApartmentIdShouldThrowForbiddenExceptionWhenTenantMembershipIsInactive() {
        Integer apartmentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        ApartmentMemberEntity inactiveMembership = new ApartmentMemberEntity();
        inactiveMembership.setApartment(apartment);
        inactiveMembership.setUser(tenant);
        inactiveMembership.setEndDate(LocalDate.now());

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(inactiveMembership);

        assertThrows(com.example.demo.Exceptions.ForbiddenException.class,
                () -> incidentService.findIncidentsByApartmentId(apartmentId, IncidentBucket.ACTIVE));
    }

    @Test
    @DisplayName("createIncident should create and return new incident")
    public void createIncidentShouldCreateAndReturnNewIncident() {
        Integer apartmentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);
        tenant.setEmail("tenant@example.com");

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setRole(Role.LANDLORD);
        landlord.setEmail("landlord@example.com");

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);

        CreateIncidentRequest request = new CreateIncidentRequest(
                "Test Incident",
                "This is a test incident.",
                IncidentCategory.PLUMBING,
                IncidentZone.KITCHEN,
                IncidentUrgency.HIGH);
        IncidentEntity savedIncident = new IncidentEntity();
        savedIncident.setId(1);
        savedIncident.setApartment(apartment);
        savedIncident.setTenant(tenant);
        savedIncident.setLandlord(apartment.getUser());
        savedIncident.setTitle(request.title());
        savedIncident.setDescription(request.description());
        savedIncident.setCategory(request.category());
        savedIncident.setZone(request.zone());
        savedIncident.setUrgency(request.urgency());
        savedIncident.setStatus(IncidentStatus.OPEN);
        savedIncident.setPhotos(List.of("photo1.jpg", "photo2.jpg"));

        when(incidentRepository.save(org.mockito.ArgumentMatchers.any(IncidentEntity.class))).thenReturn(savedIncident);
        IncidentDTO result = incidentService.createIncident(apartmentId, request, null);

        assertNotNull(result);
        assertEquals(savedIncident.getId(), result.id());
        assertEquals(request.title(), result.title());
    }

    @Test
    @DisplayName("createIncident should throw ResourceNotFoundException when apartment not found")
    public void createIncidentShouldThrowResourceNotFoundExceptionWhenApartmentNotFound() {
        Integer apartmentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        when(apartmentService.findById(apartmentId)).thenReturn(null);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);

        CreateIncidentRequest request = new CreateIncidentRequest(
            "Test Incident",
            "This is a test incident.",
            IncidentCategory.PLUMBING,
            IncidentZone.KITCHEN,
            IncidentUrgency.HIGH);

        assertThrows(ResourceNotFoundException.class, () -> incidentService.createIncident(apartmentId, request, null));
    }

    @Test
    @DisplayName("createIncident should throw ForbiddenException when tenant tries to create incident for another apartment")
    public void createIncidentShouldThrowForbiddenExceptionWhenTenantTriesToCreateIncidentForAnotherApartment() {
        Integer apartmentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(null);

        CreateIncidentRequest request = new CreateIncidentRequest(
            "Test Incident",
            "This is a test incident.",
            IncidentCategory.PLUMBING,
            IncidentZone.KITCHEN,
            IncidentUrgency.HIGH);
        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.createIncident(apartmentId, request, null)); 
    }

    @Test
    @DisplayName("createIncident should throw ForbiddenException when tenant membership is inactive")
    public void createIncidentShouldThrowForbiddenExceptionWhenTenantMembershipIsInactive() {
        Integer apartmentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        ApartmentMemberEntity inactiveMembership = new ApartmentMemberEntity();
        inactiveMembership.setApartment(apartment);
        inactiveMembership.setUser(tenant);
        inactiveMembership.setEndDate(LocalDate.now());

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(inactiveMembership);

        CreateIncidentRequest request = new CreateIncidentRequest(
            "Test Incident",
            "This is a test incident.",
            IncidentCategory.PLUMBING,
            IncidentZone.KITCHEN,
            IncidentUrgency.HIGH);
        assertThrows(com.example.demo.Exceptions.ForbiddenException.class,
            () -> incidentService.createIncident(apartmentId, request, null));
    }

    @Test
    @DisplayName("createIncident should  throw ForbiddenException usere role is not tenant")
    public void createIncidentShouldThrowForbiddenExceptionUserRoleIsNotTenant() {
        Integer apartmentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        CreateIncidentRequest request = new CreateIncidentRequest(
            "Test Incident",
            "This is a test incident.",
            IncidentCategory.PLUMBING,
            IncidentZone.KITCHEN,
            IncidentUrgency.HIGH);

        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.createIncident(apartmentId, request, null));
    }

    @Test
    @DisplayName("createIncident should throw BadRequestException when there are more than 5 photos in the request")
    public void createIncidentShouldThrowBadRequestExceptionWhenThereAreMoreThan5PhotosInTheRequest() {
        Integer apartmentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);

        CreateIncidentRequest request = new CreateIncidentRequest(
            "Test Incident",
            "This is a test incident.",
            IncidentCategory.PLUMBING,
            IncidentZone.KITCHEN,
            IncidentUrgency.HIGH);

        MultipartFile photo1 = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile photo2 = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile photo3 = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile photo4 = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile photo5 = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile photo6 = org.mockito.Mockito.mock(MultipartFile.class);
        List<MultipartFile> photos = List.of(photo1, photo2, photo3, photo4, photo5, photo6);

        assertThrows(com.example.demo.Exceptions.BadRequestException.class, () -> incidentService.createIncident(apartmentId, request, photos));
    }

    @Test
    @DisplayName("createIncident should throw BadRequestException when there is a photo that fails to upload")
    public void createIncidentShouldThrowBadRequestExceptionWhenThereIsAPhotoThatFailsToUpload() throws IOException {
        Integer apartmentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);

        CreateIncidentRequest request = new CreateIncidentRequest(
            "Test Incident",
            "This is a test incident.",
            IncidentCategory.PLUMBING,
            IncidentZone.KITCHEN,
            IncidentUrgency.HIGH);

        MultipartFile photo1 = org.mockito.Mockito.mock(MultipartFile.class);
        MultipartFile photo2 = org.mockito.Mockito.mock(MultipartFile.class);
        List<MultipartFile> photos = List.of(photo1, photo2);
        when(cloudinaryService.upload(photo1, "incidents")).thenReturn(Map.of("secure_url", "http://cloudinary.com/photo1.jpg"));
        when(cloudinaryService.upload(photo2, "incidents")).thenThrow(new IOException("Upload failed"));

        assertThrows(com.example.demo.Exceptions.BadRequestException.class, () -> incidentService.createIncident(apartmentId, request, photos));
    }

    @Test
    @DisplayName("updateIncidentStatus should update the status of an incident")
    public void updateIncidentStatusShouldUpdateTheStatusOfAnIncident() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);
        UserEntity tenant = new UserEntity();
        tenant.setId(2);
        tenant.setRole(Role.TENANT);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);
        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setTenant(tenant);
        incident.setLandlord(landlord);
        incident.setTitle("Incident");
        incident.setDescription("Description");
        incident.setCategory(IncidentCategory.PLUMBING);
        incident.setZone(IncidentZone.KITCHEN);
        incident.setUrgency(IncidentUrgency.HIGH);
        incident.setStatus(IncidentStatus.OPEN);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        when(incidentRepository.save(incident)).thenReturn(incident);
        when(incidentStatusHistoryRepository.findByIncidentIdOrderByChangedAtAsc(incidentId)).thenReturn(List.of());

        IncidentDTO result = incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.IN_PROGRESS);

        assertNotNull(result);
        assertEquals(incidentId, result.id());
        assertEquals(IncidentStatus.IN_PROGRESS.name(), result.status());
    }

    @Test
    @DisplayName("updateIncidentStatus should throw ResourceNotFoundException when incident not found")
    public void updateIncidentStatusShouldThrowResourceNotFoundExceptionWhenIncidentNotFound() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("updateIncidentStatus should throw ResourceNotFoundException when apartment not found")
    public void updateIncidentStatusShouldThrowResourceNotFoundExceptionWhenApartmentNotFound() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        when(apartmentService.findById(apartmentId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.IN_PROGRESS));
    }  

    @Test
    @DisplayName("updateIncidentStatus should throw ForbiddenException when current user tries to update incident of another apartment")
    public void updateIncidentStatusShouldThrowForbiddenExceptionWhenCurrentUserTriesToUpdateIncidentOfAnotherApartment() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        UserEntity landlord2 = new UserEntity();
        landlord2.setId(2);
        landlord2.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord2);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("updateIncidentStatus should throw ForbiddenException when current user is tenant")
    public void updateIncidentStatusShouldThrowForbiddenExceptionWhenCurrentUserIsTenant() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("updateIncidentStatus should throw BadRequestException when trying to update status to closed")
    public void updateIncidentStatusShouldThrowBadRequestExceptionWhenTryingToUpdateStatusToClosed() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(com.example.demo.Exceptions.BadRequestException.class, () -> incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.CLOSED));
    }

    @Test
    @DisplayName("updateIncidentStatus should throw BadRequestException when trying to update status to closed inactivity")
    public void updateIncidentStatusShouldThrowBadRequestExceptionWhenTryingToUpdateStatusToClosedInactivity() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(com.example.demo.Exceptions.BadRequestException.class, () -> incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.CLOSED_INACTIVITY));
    }

    @Test
    @DisplayName("updateIncidentStatus should throw BadRequestException when trying to update status bad status transition")
    public void updateIncidentStatusShouldThrowBadRequestExceptionWhenTryingToUpdateStatusBadStatusTransition() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setStatus(IncidentStatus.CLOSED);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(com.example.demo.Exceptions.BadRequestException.class, () -> incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("updateIncidentStatus should throw BadRequestException when trying to update status to same status")
    public void updateIncidentStatusShouldThrowBadRequestExceptionWhenTryingToUpdateStatusToSameStatus() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setStatus(IncidentStatus.OPEN);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(com.example.demo.Exceptions.BadRequestException.class, () -> incidentService.updateIncidentStatusByLandlord(apartmentId, incidentId, IncidentStatus.OPEN));
    }

    @Test
    @DisplayName("confirmSolutionByTenant should update the status of an incident to closed")
    public void confirmSolutionByTenantShouldUpdateTheStatusOfAnIncidentToClosed() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setEmail("landlord@test.com");

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setTenant(tenant);
        incident.setLandlord(landlord);
        incident.setTitle("Incident");
        incident.setDescription("Description");
        incident.setCategory(IncidentCategory.PLUMBING);
        incident.setZone(IncidentZone.KITCHEN);
        incident.setUrgency(IncidentUrgency.HIGH);
        incident.setStatus(IncidentStatus.RESOLVED);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);
        when(incidentRepository.save(incident)).thenReturn(incident);
        when(incidentStatusHistoryRepository.findByIncidentIdOrderByChangedAtAsc(incidentId)).thenReturn(List.of());

        incidentService.confirmSolutionByTenant(apartmentId, incidentId);

        verify(incidentRepository).save(argThat(savedIncident -> savedIncident.getStatus() == IncidentStatus.CLOSED));
    }

    @Test
    @DisplayName("confirmSolutionByTenant should throw ResourceNotFoundException when incident not found")
    public void confirmSolutionByTenantShouldThrowResourceNotFoundExceptionWhenIncidentNotFound() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.confirmSolutionByTenant(apartmentId, incidentId));
    }

    @Test
    @DisplayName("confirmSolutionByTenant should throw ResourceNotFoundException when apartment not found")
    public void confirmSolutionByTenantShouldThrowResourceNotFoundExceptionWhenApartmentNotFound() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.confirmSolutionByTenant(apartmentId, incidentId));
    }

    @Test
    @DisplayName("confirmoSolutionByTenant should throw ForbiddenException when current user is not tenant")
    public void confirmSolutionByTenantShouldThrowForbiddenExceptionWhenCurrentUserIsNotTenant() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        when(userService.findCurrentUserEntity()).thenReturn(landlord);

        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.confirmSolutionByTenant(apartmentId, incidentId));
    }

    @Test
    @DisplayName("confirmSolutionByTenant should throw ResourceNotFoundException when incident does not belong to the apartment")
    public void confirmSolutionByTenantShouldThrowResourceNotFoundExceptionWhenIncidentDoesNotBelongToTheApartment() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        ApartmentEntity anotherApartment = new ApartmentEntity();
        anotherApartment.setId(2);
        incident.setApartment(anotherApartment);
        incident.setTenant(tenant);
        incident.setStatus(IncidentStatus.OPEN);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(ResourceNotFoundException.class, () -> incidentService.confirmSolutionByTenant(apartmentId, incidentId));
    }

    @Test
    @DisplayName("confirmSolutionByTenant should throw ForbiddenException when user membership is inactive")
    public void confirmSolutionByTenantShouldThrowForbiddenExceptionWhenUserMembershipIsInactive() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setTenant(tenant);
        incident.setStatus(IncidentStatus.OPEN);

        ApartmentMemberEntity inactiveMembership = new ApartmentMemberEntity();
        inactiveMembership.setApartment(apartment);
        inactiveMembership.setUser(tenant);
        inactiveMembership.setEndDate(LocalDate.now());

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(inactiveMembership);

        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.confirmSolutionByTenant(apartmentId, incidentId));
    }

    @Test
    @DisplayName("confirmSolutionByTenant should throw ForbiddenException when tenant is not the one who created the incident")
    public void confirmSolutionByTenantShouldThrowForbiddenExceptionWhenTenantIsNotTheOneWhoCreatedTheIncident() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        UserEntity anotherTenant = new UserEntity();
        anotherTenant.setId(2);
        incident.setTenant(anotherTenant);

        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);

        assertThrows(com.example.demo.Exceptions.ForbiddenException.class, () -> incidentService.confirmSolutionByTenant(apartmentId, incidentId));
    }

    @Test
    @DisplayName("confirmSolutionByTenant should throw BadRequestException when trying to confirm solution of an incident that is not in RESOLVED status")
    public void confirmSolutionByTenantShouldThrowBadRequestExceptionWhenTryingToConfirmSolutionOfAnIncidentThatIsNotInResolvedStatus() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setTenant(tenant);
        incident.setStatus(IncidentStatus.IN_PROGRESS);

        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);

        assertThrows(com.example.demo.Exceptions.BadRequestException.class, () -> incidentService.confirmSolutionByTenant(apartmentId, incidentId));
    }

    @Test
    @DisplayName("rejectSolutionByTenant should update the status of an incident to in progress")
    public void rejectSolutionByTenantShouldUpdateTheStatusOfAnIncidentToInProgress() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setTenant(tenant);
        incident.setStatus(IncidentStatus.RESOLVED);

        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));

        UserEntity landlord = new UserEntity();
        landlord.setId(2);
        landlord.setEmail("landlord@test.com");
        incident.setLandlord(landlord);
        incident.setTitle("Incident");
        incident.setDescription("Description");
        incident.setCategory(IncidentCategory.PLUMBING);
        incident.setZone(IncidentZone.KITCHEN);
        incident.setUrgency(IncidentUrgency.HIGH);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);
        when(incidentRepository.save(incident)).thenReturn(incident);
        when(incidentStatusHistoryRepository.findByIncidentIdOrderByChangedAtAsc(incidentId)).thenReturn(List.of());

        incidentService.rejectSolutionByTenant(apartmentId, incidentId, "Reason");

        verify(incidentRepository).save(argThat(savedIncident -> savedIncident.getStatus() == IncidentStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("rejectSolutionByTenant should throw ResourceNotFoundException when incident not found")
    public void rejectSolutionByTenantShouldThrowResourceNotFoundExceptionWhenIncidentNotFound() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.rejectSolutionByTenant(apartmentId, incidentId, "Reason"));
    }

    @Test
    @DisplayName("rejectSolutionByTenant should throw ResourceNotFoundException when apartment not found")
    public void rejectSolutionByTenantShouldThrowResourceNotFoundExceptionWhenApartmentNotFound() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.rejectSolutionByTenant(apartmentId, incidentId, "Reason"));
    }

    @Test
    @DisplayName("rejectSolutionByTenant should throw ForbiddenException when current user is not tenant")
    public void rejectSolutionByTenantShouldThrowForbiddenExceptionWhenCurrentUserIsNotTenant() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity landlord = new UserEntity();
        landlord.setId(1);
        landlord.setRole(Role.LANDLORD);

        when(userService.findCurrentUserEntity()).thenReturn(landlord);

        assertThrows(ForbiddenException.class, () -> incidentService.rejectSolutionByTenant(apartmentId, incidentId, "Reason"));
    }

    @Test
    @DisplayName("rejectSolutionByTenant should throw ResourceNotFoundException when incident does not belong to the apartment")
    public void rejectSolutionByTenantShouldThrowResourceNotFoundExceptionWhenIncidentDoesNotBelongToTheApartment() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        ApartmentEntity anotherApartment = new ApartmentEntity();
        anotherApartment.setId(2);
        incident.setApartment(anotherApartment);
        incident.setTenant(tenant);
        incident.setStatus(IncidentStatus.RESOLVED);

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(ResourceNotFoundException.class, () -> incidentService.rejectSolutionByTenant(apartmentId, incidentId, "Reason"));
    }

    @Test
    @DisplayName("rejectSolutionByTenant should throw ForbiddenException when user membership is inactive")
    public void rejectSolutionByTenantShouldThrowForbiddenExceptionWhenUserMembershipIsInactive() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setTenant(tenant);
        incident.setStatus(IncidentStatus.RESOLVED);

        ApartmentMemberEntity inactiveMembership = new ApartmentMemberEntity();
        inactiveMembership.setApartment(apartment);
        inactiveMembership.setUser(tenant);
        inactiveMembership.setEndDate(LocalDate.now());

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(inactiveMembership);

        assertThrows(ForbiddenException.class, () -> incidentService.rejectSolutionByTenant(apartmentId, incidentId, "Reason"));
    }

    @Test
    @DisplayName("rejectSolutionByTenant should throw ForbiddenException when tenant is not the one who created the incident")
    public void rejectSolutionByTenantShouldThrowForbiddenExceptionWhenTenantIsNotTheOneWhoCreatedTheIncident() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        UserEntity anotherTenant = new UserEntity();
        anotherTenant.setId(2);
        incident.setTenant(anotherTenant);
        incident.setStatus(IncidentStatus.RESOLVED);

        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);

        assertThrows(ForbiddenException.class, () -> incidentService.rejectSolutionByTenant(apartmentId, incidentId, "Reason"));
    }

    @Test
    @DisplayName("rejectSolutionByTenant should throw BadRequestException when trying to reject solution of an incident that is not in RESOLVED status")
    public void rejectSolutionByTenantShouldThrowBadRequestExceptionWhenTryingToRejectSolutionOfAnIncidentThatIsNotInResolvedStatus() {
        Integer apartmentId = 1;
        Integer incidentId = 1;

        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setRole(Role.TENANT);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(tenant);

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setApartment(apartment);
        incident.setTenant(tenant);
        incident.setStatus(IncidentStatus.IN_PROGRESS);

        ApartmentMemberEntity activeMembership = new ApartmentMemberEntity();
        activeMembership.setApartment(apartment);
        activeMembership.setUser(tenant);
        activeMembership.setEndDate(LocalDate.now().plusDays(1));

        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId)).thenReturn(activeMembership);

        assertThrows(BadRequestException.class, () -> incidentService.rejectSolutionByTenant(apartmentId, incidentId, "Reason"));
    }

    @Test
    @DisplayName("autoCloseResolvedIncidents should update the status of resolved incidents to closed inactivity")
    public void autoCloseResolvedIncidentsShouldUpdateTheStatusOfResolvedIncidentsToClosedInactivity() {
        Integer incidentId = 1;

        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setStatus(IncidentStatus.RESOLVED);
        UserEntity tenant = new UserEntity();
        tenant.setId(1);
        tenant.setEmail("tenant@test.com");
        incident.setTenant(tenant);

        when(incidentRepository.findByStatusAndUpdatedAtBefore(org.mockito.ArgumentMatchers.eq(IncidentStatus.RESOLVED), org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class))).thenReturn(List.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.autoCloseResolvedIncidents();

        verify(incidentRepository).save(argThat(savedIncident -> savedIncident.getStatus() == IncidentStatus.CLOSED_INACTIVITY));
    }

    @Test
    @DisplayName("autoCloseResolvedIncidents should do nothing when there are no resolved incidents")
    public void autoCloseResolvedIncidentsShouldDoNothingWhenThereAreNoResolvedIncidents() {
        when(incidentRepository.findByStatusAndUpdatedAtBefore(org.mockito.ArgumentMatchers.eq(IncidentStatus.RESOLVED), org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class))).thenReturn(List.of());

        incidentService.autoCloseResolvedIncidents();

        verify(incidentRepository).findByStatusAndUpdatedAtBefore(org.mockito.ArgumentMatchers.eq(IncidentStatus.RESOLVED), org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class));
    }

}