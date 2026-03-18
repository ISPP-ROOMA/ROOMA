package com.example.demo.User;

import static org.mockito.ArgumentMatchers.any;
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

    // == Test de Delete /api/users/{id} (ADMIN) ==

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("deleteUser should return 204 for admin")
    void deleteUser_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteById(1);
    }
}