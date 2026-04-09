package com.example.demo.User;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.User.DTOs.CreateUser;
import com.example.demo.User.DTOs.UpdateProfileRequest;
import com.example.demo.User.DTOs.UpdateUser;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@Import(UserControllerTests.SecurityTestConfig.class)
public class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @TestConfiguration
    @EnableMethodSecurity
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/users/profile").authenticated()
                    .requestMatchers("/api/users/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    // == Test de Get /api/users/profile ==

    @Test
    @WithMockUser(username = "tenant1@test.com", roles = "TENANT")
    @DisplayName("getUserProfile should return 200 for authenticated user")
    void getUserProfile_ReturnsOk() throws Exception {
        UserEntity user = new UserEntity();
        user.setEmail("tenant1@test.com");
        user.setRole(Role.TENANT);
        user.setHobbies("Videojuegos");

        when(userService.getUserProfile()).thenReturn(user);

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tenant1@test.com"))
                .andExpect(jsonPath("$.hobbies").value("Videojuegos"));
    }

    @Test
    @WithMockUser(username = "nonexistent@test.com")
    @DisplayName("getUserProfile should return 404 if user not found")
    void getUserProfile_UserNotFound() throws Exception {
        when(userService.getUserProfile()).thenThrow(new ResourceNotFoundException("User not authenticated"));

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isNotFound());
    }

    // == Test de Delete /api/users/profile ==

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("deleteUserProfile should return 204 for authenticated user")
    void deleteUserProfile_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/profile"))
                .andExpect(status().isNoContent());

        verify(userService).deleteCurrentUserProfile();
    }

    @Test
    @DisplayName("deleteUserProfile should return 401 when unauthenticated")
    void deleteUserProfile_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/profile"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).deleteCurrentUserProfile();
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("deleteUserProfile should return 409 when service throws conflict")
    void deleteUserProfile_Conflict_Returns409() throws Exception {
        doThrow(new ConflictException("User has related data and cannot be deleted"))
                .when(userService).deleteCurrentUserProfile();

        mockMvc.perform(delete("/api/users/profile"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User has related data and cannot be deleted"))
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    // == Test de Put /api/users/profile ==

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("updateUserProfile should return 200 on success")
    void updateUserProfile_UpdatesSuccessfully() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Nuevo", "Nombre", "email@test.com", "pass", null, null, null, null, null, null, null, null
        );
        UserEntity user = new UserEntity();
        user.setName("Nuevo");
        user.setEmail("email@test.com");
        user.setRole(Role.TENANT);

        when(userService.updateCurrentUserProfile(any(UpdateProfileRequest.class))).thenReturn(user);

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo"))
                .andExpect(jsonPath("$.email").value("email@test.com"))
                .andExpect(jsonPath("$.role").value("TENANT"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("updateUserProfile should return 409 if email exists")
    void updateUserProfile_EmailConflict() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "conflict@test.com", null, null, null, null, null, null, null, null, null);

        when(userService.updateCurrentUserProfile(any(UpdateProfileRequest.class)))
                .thenThrow(new ConflictException("Email already in use"));

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // == Test de Get /api/users (ADMIN) ==

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getAllUsers should return 200 for admin")
    void getAllUsers_ReturnsOkForAdmin() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1);
        user.setEmail("admin@test.com");
        user.setRole(Role.ADMIN);

        when(userService.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("admin@test.com"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getAllUsers should return 403 for tenant")
    void getAllUsers_ForbiddenForTenant() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
        
        verify(userService, never()).findAll();
    }

    // == Test de Post /api/users (ADMIN) ==

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("createUser should return 201 for admin")
    void createUser_ReturnsCreated() throws Exception {
        CreateUser request = new CreateUser("new@test.com", "pass", Role.TENANT);
        UserEntity created = new UserEntity();
        created.setEmail("new@test.com");
        created.setRole(Role.TENANT);

        when(userService.save(any(UserEntity.class))).thenReturn(created);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.role").value("TENANT"));
    }

    // == Test de Get /api/users/{id} (ADMIN) ==

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getUserById should return 200 and user for admin")
    void getUserById_ReturnsOkForAdmin() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(5);
        user.setEmail("user5@test.com");
        user.setRole(Role.TENANT);

        when(userService.findById(5)).thenReturn(user);

        mockMvc.perform(get("/api/users/{id}", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("user5@test.com"))
                .andExpect(jsonPath("$.role").value("TENANT"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getUserById should return 403 for non admin")
    void getUserById_ForbiddenForTenant() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 6))
                .andExpect(status().isForbidden());

        verify(userService, never()).findById(6);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getUserById should return 404 when service throws not found")
    void getUserById_NotFound_Returns404() throws Exception {
        when(userService.findById(7)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/{id}", 7))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // == Test de Put /api/users/{id} (ADMIN) ==

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateUser should return 200 and updated user for admin")
    void updateUser_ReturnsOkForAdmin() throws Exception {
        UpdateUser request = new UpdateUser("updated@test.com", "newpass", Role.TENANT);
        UserEntity updated = new UserEntity();
        updated.setId(8);
        updated.setEmail("updated@test.com");
        updated.setRole(Role.TENANT);

        when(userService.update(eq(8), any(UserEntity.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/{id}", 8)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.email").value("updated@test.com"))
                .andExpect(jsonPath("$.role").value("TENANT"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("updateUser should return 403 for non admin")
    void updateUser_ForbiddenForTenant() throws Exception {
        UpdateUser request = new UpdateUser("tenant@test.com", "pass", Role.TENANT);

        mockMvc.perform(put("/api/users/{id}", 9)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(userService, never()).update(eq(9), any(UserEntity.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updateUser should return 404 when service throws not found")
    void updateUser_NotFound_Returns404() throws Exception {
        UpdateUser request = new UpdateUser("missing@test.com", "pass", Role.TENANT);

        when(userService.update(eq(10), any(UserEntity.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/{id}", 10)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // == Test de Delete /api/users/{id} (ADMIN) ==

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("deleteUser should return 204 for admin")
    void deleteUser_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteById(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("deleteUser should return 404 when service throws RuntimeException")
    void deleteUser_ServiceThrows_ReturnsNotFound() throws Exception {
        doThrow(new RuntimeException("boom")).when(userService).deleteById(2);

        mockMvc.perform(delete("/api/users/2"))
                .andExpect(status().isNotFound());

        verify(userService).deleteById(2);
    }
}
