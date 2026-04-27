package com.example.demo.Apartment;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.Apartment.DTOs.ApartmentHomeDTO;
import com.example.demo.Apartment.DTOs.CreateApartment;
import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;

@WebMvcTest(ApartmentController.class)
@Import(ApartmentControllerTest.SecurityTestConfig.class)
public class ApartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApartmentService apartmentService;

    @MockitoBean
    private ApartmentMemberService apartmentMemberService;

    @MockitoBean
    private ApartmentPhotoService apartmentPhotoService;

    @MockitoBean
    private ApartmentHomeService apartmentHomeService;

    @MockitoBean
    private JwtService jwtService;

    @TestConfiguration
    @EnableMethodSecurity
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.GET, "/api/apartments/**").authenticated()
                            .requestMatchers(HttpMethod.POST, "/api/apartments/**").hasRole("LANDLORD")
                            .requestMatchers(HttpMethod.PUT, "/api/apartments/**").hasRole("LANDLORD")
                            .requestMatchers(HttpMethod.DELETE, "/api/apartments/**").hasRole("LANDLORD")
                            .anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Test
    @DisplayName("getAllApartments should return 401 when unauthenticated")
    public void getAllApartments_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/apartments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getAllApartments should return apartment list for authenticated user")
    public void getAllApartments_ReturnsOk() throws Exception {
        ApartmentEntity apartment = apartment(1);
        ApartmentMemberEntity member = member(100, apartment, 200);

        when(apartmentService.findAll()).thenReturn(List.of(apartment));
        when(apartmentMemberService.findCurrentMembers(1)).thenReturn(List.of(member));

        mockMvc.perform(get("/api/apartments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].members[0].id").value(100));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getMyHomeSnapshot should return 200 for tenant")
    public void getMyHomeSnapshot_Tenant() throws Exception {
        ApartmentHomeDTO home = new ApartmentHomeDTO(ApartmentDTO.fromApartmentEntity(apartment(10)), List.of(), List.of(), null, 1);
        when(apartmentHomeService.getCurrentUserHome()).thenReturn(home);

        mockMvc.perform(get("/api/apartments/me/home"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("getMyHomeSnapshot should return 403 for landlord")
    public void getMyHomeSnapshot_LandlordForbidden() throws Exception {
        mockMvc.perform(get("/api/apartments/me/home"))
                .andExpect(status().isForbidden());

        verify(apartmentHomeService, never()).getCurrentUserHome();
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("getMyApartments should return 200 for landlord")
    public void getMyApartments_Landlord() throws Exception {
        ApartmentEntity apartment = apartment(2);
        when(apartmentService.findMyApartments()).thenReturn(List.of(apartment));
        when(apartmentMemberService.findCurrentMembers(2)).thenReturn(List.of());

        mockMvc.perform(get("/api/apartments/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("createApartment should return 403 for tenant")
    public void createApartment_TenantForbidden() throws Exception {
        mockMvc.perform(multipart("/api/apartments")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "data",
                                "data",
                                MediaType.APPLICATION_JSON_VALUE,
                                "{\"title\":\"Flat C\",\"description\":\"Desc\",\"price\":700.0,\"bills\":\"wifi\",\"ubication\":\"Madrid\",\"state\":\"ACTIVE\"}"
                                        .getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("deleteApartment should return 204 when service succeeds")
    public void deleteApartment_Success() throws Exception {
        mockMvc.perform(delete("/api/apartments/{id}", 4))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("deleteApartment should return 404 when service throws")
    public void deleteApartment_NotFound() throws Exception {
        doThrow(new RuntimeException("boom")).when(apartmentService).deleteById(5);

        mockMvc.perform(delete("/api/apartments/{id}", 5))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getApartmentAndPhotos should return apartment and image list")
    public void getApartmentAndPhotos_ReturnsData() throws Exception {
        ApartmentEntity apartment = apartment(6);
        ApartmentPhotoEntity photo = new ApartmentPhotoEntity();
        photo.setId(66);
        photo.setUrl("https://img/test.jpg");

        when(apartmentService.findByIdForCurrentUser(6)).thenReturn(apartment);
        when(apartmentPhotoService.findPhotosByApartmentId(6)).thenReturn(List.of(photo));

        mockMvc.perform(get("/api/apartments/{id}/photos", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartment.id").value(6))
                .andExpect(jsonPath("$.images[0].id").value(66));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("searchApartments should return mapped list")
    public void searchApartments_ReturnsMappedList() throws Exception {
        ApartmentEntity apartment = apartment(7);
        when(apartmentService.search("Madrid", 300.0, 600.0, ApartmentState.ACTIVE)).thenReturn(List.of(apartment));
        when(apartmentMemberService.findCurrentMembers(7)).thenReturn(List.of());

        mockMvc.perform(get("/api/apartments/search")
                        .param("ubication", "Madrid")
                        .param("minPrice", "300")
                        .param("maxPrice", "600")
                        .param("state", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getDeckForCandidate should return mapped list")
    public void getDeckForCandidate_ReturnsMappedList() throws Exception {
        ApartmentEntity apartment = apartment(8);
        when(apartmentService.getDeckForCandidate(9)).thenReturn(List.of(apartment));
        when(apartmentMemberService.findCurrentMembers(8)).thenReturn(List.of());

        mockMvc.perform(get("/api/apartments/deck/{candidateId}", 9))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getApartmentById should return 200 with apartment data")
    public void getApartmentById_ReturnsOk() throws Exception {
        ApartmentEntity apartment = apartment(3);
        when(apartmentService.findByIdForCurrentUser(3)).thenReturn(apartment);
        when(apartmentMemberService.findCurrentMembers(3)).thenReturn(List.of());

        mockMvc.perform(get("/api/apartments/{id}", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.title").value("Apartment 3"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getApartmentById should return 404 when apartment does not exist")
    public void getApartmentById_WhenNotFound_Returns404() throws Exception {
        when(apartmentService.findByIdForCurrentUser(998))
                .thenThrow(new ResourceNotFoundException("Apartment not found"));

        mockMvc.perform(get("/api/apartments/{id}", 998))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Apartment not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("createApartment should return 201 with created apartment for landlord")
    public void createApartment_LandlordSuccess() throws Exception {
        ApartmentEntity created = apartment(5);
        when(apartmentService.createWithImages(any(CreateApartment.class), any()))
                .thenReturn(created);

        mockMvc.perform(multipart("/api/apartments")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "data",
                                "data",
                                MediaType.APPLICATION_JSON_VALUE,
                                "{\"title\":\"Flat A\",\"description\":\"Desc\",\"price\":700.0,\"bills\":\"wifi\",\"ubication\":\"Madrid\",\"state\":\"ACTIVE\"}"
                                        .getBytes(StandardCharsets.UTF_8)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Apartment 5"));

        verify(apartmentService).createWithImages(any(CreateApartment.class), any());
    }

    @Test
    @DisplayName("updateApartment should return 401 when unauthenticated")
    public void updateApartment_Unauthenticated() throws Exception {
        String body = """
            {
              "title": "Nuevo titulo",
              "description": "Nueva descripcion",
              "price": 750.0,
              "bills": "incluido",
              "ubication": "Madrid",
              "state": "ACTIVE",
              "idealTenantProfile": "Perfil"
            }
            """;

        mockMvc.perform(put("/api/apartments/{id}", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("updateApartment should return 403 for non-landlord roles")
    public void updateApartment_ForbiddenForTenant() throws Exception {
        String body = """
            {
              "title": "Nuevo titulo",
              "description": "Nueva descripcion",
              "price": 750.0,
              "bills": "incluido",
              "ubication": "Madrid",
              "state": "ACTIVE",
              "idealTenantProfile": "Perfil"
            }
            """;

        mockMvc.perform(put("/api/apartments/{id}", 11)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        verify(apartmentService, never()).update(any(Integer.class), any(ApartmentEntity.class));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("updateApartment should return 200 and updated entity for landlord")
    public void updateApartment_SuccessForLandlord() throws Exception {
        ApartmentEntity existing = apartment(20);
        existing.setId(20);
        existing.setUbication("Sevilla");

        when(apartmentService.update(any(Integer.class), any(ApartmentEntity.class))).thenReturn(existing);

        String body = """
            {
              "title": "Nuevo titulo",
              "description": "Nueva descripcion",
              "price": 800.0,
              "bills": "incluido",
              "ubication": "Sevilla",
              "state": "ACTIVE",
              "idealTenantProfile": "Perfil ideal"
            }
            """;

        mockMvc.perform(put("/api/apartments/{id}", 20)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.ubication").value("Sevilla"));

        verify(apartmentService).update(any(Integer.class), any(ApartmentEntity.class));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("updateApartment should return 404 when apartment is not found")
    public void updateApartment_NotFound() throws Exception {
        when(apartmentService.update(any(Integer.class), any(ApartmentEntity.class)))
                .thenThrow(new ResourceNotFoundException("Apartment not found"));

        String body = """
            {
              "title": "Nuevo titulo",
              "description": "Nueva descripcion",
              "price": 800.0,
              "bills": "incluido",
              "ubication": "Madrid",
              "state": "ACTIVE",
              "idealTenantProfile": "Perfil ideal"
            }
            """;

        mockMvc.perform(put("/api/apartments/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Apartment not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("updateApartment should return 400 when payload is invalid")
    public void updateApartment_InvalidPayload() throws Exception {
        String body = """
            {
              "title": "",
              "description": "",
              "price": -10.0,
              "bills": "incluido",
              "ubication": "",
              "state": "ACTIVE"
            }
            """;

        mockMvc.perform(put("/api/apartments/{id}", 30)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(apartmentService, never()).update(any(Integer.class), any(ApartmentEntity.class));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("updateApartment should return 403 when service throws ForbiddenException")
    public void updateApartment_ForbiddenWhenNotOwner() throws Exception {
        when(apartmentService.update(any(Integer.class), any(ApartmentEntity.class)))
                .thenThrow(new ForbiddenException("Only the landlord of this apartment can edit it"));

        String body = """
            {
              "title": "Nuevo titulo",
              "description": "Nueva descripcion",
              "price": 800.0,
              "bills": "incluido",
              "ubication": "Madrid",
              "state": "ACTIVE",
              "idealTenantProfile": "Perfil ideal"
            }
            """;

        mockMvc.perform(put("/api/apartments/{id}", 21)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the landlord of this apartment can edit it"))
                .andExpect(jsonPath("$.statusCode").value(403));
    }

    private ApartmentEntity apartment(Integer id) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(id);
        apartment.setTitle("Apartment " + id);
        apartment.setDescription("desc " + id);
        apartment.setPrice(500.0);
        apartment.setBills("wifi");
        apartment.setUbication("Madrid");
        apartment.setState(ApartmentState.ACTIVE);
        return apartment;
    }

    private ApartmentMemberEntity member(Integer id, ApartmentEntity apartment, Integer userId) {
        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(id);
        member.setApartment(apartment);

        com.example.demo.User.UserEntity user = new com.example.demo.User.UserEntity();
        user.setId(userId);
        member.setUser(user);
        return member;
    }
}
