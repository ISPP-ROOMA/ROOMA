package com.example.demo.Auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.Auth.DTOs.AuthRequest;
import com.example.demo.Auth.DTOs.AuthResult;
import com.example.demo.Auth.DTOs.LoginRequest;
import com.example.demo.Auth.DTOs.RefreshTokenRequest;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.User.Role;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTests.SecurityTestConfig.class)
public class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @TestConfiguration
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated());
            return http.build();
        }
    }

    // == Test de /api/auth/register ==

    @Test
    @DisplayName("register should return 200 and tokens")
    void register_success() throws Exception {
        AuthRequest request = new AuthRequest("test@email.com", "password123", "device1", Role.LANDLORD);
        AuthResult result = new AuthResult("access-token", "refresh-token", "LANDLORD", 1);
        
        when(authService.register(anyString(), anyString(), anyString(), any(Role.class)))
            .thenReturn(result);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    @DisplayName("register should return 409 if user exists")
    void register_conflict() throws Exception {
        AuthRequest request = new AuthRequest("duplicate@email.com", "pass", "dev1", Role.LANDLORD);

        when(authService.register(anyString(), anyString(), anyString(), any(Role.class)))
            .thenThrow(new ConflictException("User already exists"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // == Test de /api/auth/login ==

    @Test
    @DisplayName("login should return 200 and tokens with userId")
    void login_success() throws Exception {
        LoginRequest login = new LoginRequest("login@email.com", "password", "device1");
        
        AuthResult result = new AuthResult(
            "access-token", 
            "refresh-token", 
            "LANDLORD", 
            1
        );

        when(authService.login(anyString(), anyString(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LANDLORD"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(cookie().exists("refresh_token"));
    }

    // == Test de /api/auth/refresh ==

    @Test
    @DisplayName("refresh should return new access token")
    void refresh_success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("device1");
        Cookie refreshCookie = new Cookie("refresh_token", "valid-refresh-token");
        AuthResult result = new AuthResult("new-access-token", "new-refresh-token", "TENANT", 2);

        when(authService.refreshToken(anyString(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(cookie().value("refresh_token", "new-refresh-token"));
    }

    // == Test de /api/auth/validate ==

    @Test
    @DisplayName("validate should return true for valid token")
    void validate_success() throws Exception {
        String token = "valid-token";
        
        when(authService.validateAccessToken(token)).thenReturn(true);

        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.message").value("Token valid"));
    }

    @Test
    @DisplayName("validate should return false for invalid token")
    void validate_invalid() throws Exception {
        String token = "invalid-token";
        
        when(authService.validateAccessToken(token)).thenReturn(false);

        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }
}