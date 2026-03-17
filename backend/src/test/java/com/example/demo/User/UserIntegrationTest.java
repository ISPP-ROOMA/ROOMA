package com.example.demo.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.User.DTOs.CreateUser;
import com.example.demo.User.DTOs.UpdateProfileRequest;
import com.example.demo.User.DTOs.UpdateUser;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UserEntity testUser = new UserEntity();
        testUser.setEmail("testuser@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(Role.TENANT);
        userRepository.saveAndFlush(testUser);
    }

    // Test de Get /api/users/profile ==

    @Test
    @WithUserDetails("tenant1@test.com")
    void getUserProfile_returnsUserProfile() throws Exception {

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tenant1@test.com"))
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.hobbies").value("Videojuegos, Música"))
                .andExpect(jsonPath("$.schedule").value("Estudiante de tarde"))
                .andExpect(jsonPath("$.profession").value("Estudiante"));
    }

    @Test
    @WithMockUser(username = "nonexistent@test.com", roles = "TENANT")
    void getUserProfile_userNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not authenticated"));
    }

    // Test de Put /api/users/profile ==

    @Test
    @WithUserDetails("tenant1@test.com")
    void updateUserProfile_updatesFieldsSuccessfully() throws Exception {

        UpdateProfileRequest request = new UpdateProfileRequest(
                "NuevoNombre",
                "NuevoApellido",
                "tenant1@test.com",
                "newPassword123",
                "2000-01-01",
                "+34 600123456",
                "https://example.com/newpic.jpg",
                "Other",
                true,
                "Nuevos hobbies",
                "Nuevo schedule",
                "Nueva profesión"
        );

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.email").value("tenant1@test.com"))
               .andExpect(jsonPath("$.name").value("NuevoNombre"))
               .andExpect(jsonPath("$.surname").value("NuevoApellido"))
               .andExpect(jsonPath("$.profilePic").value("https://example.com/newpic.jpg"))
               .andExpect(jsonPath("$.gender").value("Other"))
               .andExpect(jsonPath("$.smoker").value(true))
               .andExpect(jsonPath("$.phone").value("+34 600123456"))
               .andExpect(jsonPath("$.hobbies").value("Nuevos hobbies"))
               .andExpect(jsonPath("$.schedule").value("Nuevo schedule"))
               .andExpect(jsonPath("$.profession").value("Nueva profesión"));
    }

    @Test
    @WithUserDetails("tenant1@test.com")
    void updateUserProfile_emailConflict_throwsConflict() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null,
                "tenant2@test.com", // email que ya existe
                null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    @WithUserDetails("tenant1@test.com")
    void updateUserProfile_blankPassword_notUpdated() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null, null,
                null,
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.email").value("tenant1@test.com"));
    }

    // Test de Delete /api/users/profile ==

    @Test
    @WithUserDetails("tenant7@test.com")
    void deleteUserProfile_withRelatedData_returnsConflict() throws Exception {
        mockMvc.perform(delete("/api/users/profile"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
            .value("User has related data and cannot be deleted"));
    }

    @Test
    @WithUserDetails("testuser@test.com")
    void deleteUserProfile_noRelatedData_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/profile"))
            .andExpect(status().isNoContent());
    }

    // Test de Get /api/users ==

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_returnsUserList() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(14))
            .andExpect(jsonPath("$[0].email").value("landlord1@test.com"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void getAllUsers_forbidden_throwsException() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());
    }

    // Test de Get /api/users/{id} ==

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_existingUser_returnsUser() throws Exception {
        mockMvc.perform(get("/api/users/6"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("tenant1@test.com"))
            .andExpect(jsonPath("$.role").value("TENANT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_nonexistentUser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found"));
    }

    // Test de Post /api/users ==

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_success_returnsCreatedUser() throws Exception {
        CreateUser request = new CreateUser(
            "newuser@test.com",
            "securePassword123",
            Role.TENANT
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("newuser@test.com"))
            .andExpect(jsonPath("$.role").value("TENANT"))
            .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void createUser_forbidden_throwsException() throws Exception {
        CreateUser request = new CreateUser(
            "newuser@test.com",
            "securePassword123",
            Role.TENANT
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // Test de Put /api/users/{id} ==

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_success_returnsUpdatedUser() throws Exception {
        UpdateUser request = new UpdateUser(
            "updateduser@test.com",
            "newSecurePassword456",
            Role.TENANT
        );

        mockMvc.perform(put("/api/users/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("updateduser@test.com"))
            .andExpect(jsonPath("$.role").value("TENANT"))
            .andExpect(jsonPath("$.id").value(6));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void updateUser_forbidden_throwsException() throws Exception {
        UpdateUser request = new UpdateUser(
            "updateduser@test.com",
            "newSecurePassword456",
            Role.TENANT
        );

        mockMvc.perform(put("/api/users/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // Test de Delete /api/users/{id} ==

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_existingUser_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/12"))
            .andExpect(status().isNoContent());

    }

    @Test
    @WithMockUser(roles = "TENANT")
    void deleteUser_forbidden_throwsException() throws Exception {
        mockMvc.perform(delete("/api/users/12"))
            .andExpect(status().isForbidden());
    }



    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_nonexistentUser_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/999"))
            .andExpect(status().isNotFound());
    }









    
}
