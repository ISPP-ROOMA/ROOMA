package com.example.demo.MemberApartment;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.DisplayName;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.MemberApartment.DTOs.CreateApartmentMember;
import com.example.demo.User.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@WebMvcTest(ApartmentMemberController.class)
@Import(ApartmentMemberControllerTest.TestConfig.class)
public class ApartmentMemberControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApartmentMemberService apartmentMemberService;

    @MockitoBean
    private JwtService jwtService;

        private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    @TestConfiguration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("list should return a list of apartment members DTO for a given apartment")
    void listShouldReturnAListOfApartmentMembersDTOForAGivenApartment() throws Exception {
        UserEntity user1 = new UserEntity();
        user1.setId(1);
        UserEntity user2 = new UserEntity();
        user2.setId(2);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(1);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setApartment(apartment);
        member1.setUser(user1);
        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);   
        member2.setApartment(apartment);
        member2.setUser(user2);

        when(apartmentMemberService.listMembers(apartment.getId())).thenReturn(List.of(member1, member2));

        mockMvc.perform(get("/api/apartments/{apartmentId}/members", apartment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(member1.getId()))
                .andExpect(jsonPath("$[0].userId").value(member1.getUser().getId()))
                .andExpect(jsonPath("$[1].id").value(member2.getId()))
                .andExpect(jsonPath("$[1].userId").value(member2.getUser().getId()));
       
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("list should throw ResourceNotFoundException if apartment does not exist")
    void listShouldThrowResourceNotFoundExceptionIfApartmentDoesNotExist() throws Exception {
        when(apartmentMemberService.listMembers(999)).thenThrow(new ResourceNotFoundException("Apartment not found"));

        mockMvc.perform(get("/api/apartments/{apartmentId}/members", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("list should throw ResourceNotFoundException if apartment has no members")
    void listShouldThrowResourceNotFoundExceptionIfApartmentHasNoMembers() throws Exception {
        when(apartmentMemberService.listMembers(1)).thenThrow(new ResourceNotFoundException("Apartment has no members"));

        mockMvc.perform(get("/api/apartments/{apartmentId}/members", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("add should add a new member to the apartment and return the created member DTO")
    void addShouldAddANewMemberToTheApartmentAndReturnTheCreatedMemberDTO() throws Exception {
        CreateApartmentMember request = new CreateApartmentMember(1, LocalDate.now().minusDays(1));
        UserEntity user = new UserEntity();
        user.setId(request.userId());

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(1);

        ApartmentMemberEntity created = new ApartmentMemberEntity();
        created.setId(1);
        created.setApartment(apartment);
        created.setUser(user);
        created.setJoinDate(request.joinDate());

        when(apartmentMemberService.addMember(apartment.getId(), request.userId(), request.joinDate())).thenReturn(created);

        mockMvc.perform(post("/api/apartments/{apartmentId}/members", apartment.getId())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.userId").value(created.getUser().getId()))
                .andExpect(jsonPath("$.joinDate").value(created.getJoinDate().toString()));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("add should throw ResourceNotFoundException if apartment does not exist")
    void addShouldThrowResourceNotFoundExceptionIfApartmentDoesNotExist() throws Exception {
        CreateApartmentMember request = new CreateApartmentMember(1, LocalDate.now().minusDays(1));

        when(apartmentMemberService.addMember(999, request.userId(), request.joinDate())).thenThrow(new ResourceNotFoundException("Apartment not found"));

        mockMvc.perform(post("/api/apartments/{apartmentId}/members", 999)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("add should throw ResourceNotFoundException if user does not exist")
    void addShouldThrowResourceNotFoundExceptionIfUserDoesNotExist() throws Exception { 
        CreateApartmentMember request = new CreateApartmentMember(999, LocalDate.now().minusDays(1));

        when(apartmentMemberService.addMember(1, request.userId(), request.joinDate())).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/apartments/{apartmentId}/members", 1)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("remove should remove the member from the apartment and return no content")
    void removeShouldRemoveTheMemberFromTheApartmentAndReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/apartments/{apartmentId}/members/{memberId}", 1, 1))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("remove should throw ResourceNotFoundException if apartment does not exist")
    void removeShouldThrowResourceNotFoundExceptionIfApartmentDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Apartment not found"))
            .when(apartmentMemberService).removeMember(999, 1);

        mockMvc.perform(delete("/api/apartments/{apartmentId}/members/{memberId}", 999, 1))
                .andExpect(status().isNotFound());
    }   

    @Test
    @WithMockUser(username = "testuser", roles = "TENANT")
    @DisplayName("remove should throw ResourceNotFoundException if member does not exist")
    void removeShouldThrowResourceNotFoundExceptionIfMemberDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Member not found"))
            .when(apartmentMemberService).removeMember(1, 999);

        mockMvc.perform(delete("/api/apartments/{apartmentId}/members/{memberId}", 1, 999))
                .andExpect(status().isNotFound());
    }

    
}