package com.example.demo.Auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Auth.DTOs.AuthRequest;
import com.example.demo.Auth.DTOs.LoginRequest;
import com.example.demo.Auth.DTOs.RefreshTokenRequest;
import com.example.demo.User.Role;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        private ObjectMapper objectMapper = new ObjectMapper();

        // == Test de /api/auth/register ==

        @Test
        void register_shouldCreateUserAndReturnToken() throws Exception {

                AuthRequest request = new AuthRequest(
                        "test@email.com",
                        "password123",
                        "device1",
                        Role.LANDLORD
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").exists())
                        .andExpect(cookie().exists("refresh_token"));
        }

        @Test
        void register_shouldFailWhenUserExists() throws Exception {

                AuthRequest request = new AuthRequest(
                        "duplicate@email.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );
                //Primero registramos el usuario
                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

                //Intentamos registrarlo de nuevo, lo que debería fallar
                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isConflict());
        }

        //== Test de /api/auth/login ==

        @Test
        void login_shouldReturnAccessToken() throws Exception {

                AuthRequest register = new AuthRequest(
                        "login@email.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)));

                LoginRequest login = new LoginRequest(
                        "login@email.com",
                        "password",
                        "device1"

                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").exists())
                        .andExpect(cookie().exists("refresh_token"));
        }

        @Test
        void login_shouldFailWithWrongPassword() throws Exception {

                // primero registrar
                AuthRequest register = new AuthRequest(
                        "login@email.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)));

                LoginRequest login = new LoginRequest(
                        "login@email.com",
                        "wrongpassword",
                        "device1"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isUnauthorized());
        }

        @Test
        void login_shouldFailWithUnregisteredUser() throws Exception {

                LoginRequest login = new LoginRequest(
                        "unregistered@email.com",
                        "password",
                        "device1"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isNotFound());
        }

        @Test
        void login_shouldFailWhenRequestInvalid() throws Exception {

                LoginRequest login = new LoginRequest(
                        "",
                        "",
                        "device1"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isBadRequest());
        }

        @Test
        void login_shouldSetRefreshTokenCookieCorrectly() throws Exception {

                AuthRequest register = new AuthRequest(
                        "cookie@email.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)));

                LoginRequest login = new LoginRequest(
                        "cookie@email.com",
                        "password",
                        "device1"
                );

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                        .andExpect(status().isOk())
                        .andExpect(cookie().exists("refresh_token"))
                        .andExpect(cookie().httpOnly("refresh_token", true))
                        .andExpect(cookie().path("refresh_token", "/"));
        }

        //== Test de /api/auth/refresh ==

        @Test
        void refresh_shouldReturnNewAccessToken() throws Exception {

                AuthRequest register = new AuthRequest(
                        "refresh@email.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );

                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                        .andExpect(status().isOk())
                        .andReturn();

                Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");

                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh")
                                .cookie(refreshCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").exists())
                        .andExpect(cookie().exists("refresh_token"));
        }

        @Test
        void refresh_shouldFailWhenCookieMissing() throws Exception {

                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
        }

        @Test
        void refresh_shouldFailWhenTokenInvalid() throws Exception {

                Cookie fakeCookie = new Cookie("refresh_token", "invalidToken");

                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh")
                                .cookie(fakeCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
        }

        @Test
        void refresh_shouldFailWhenDeviceIdDoesNotMatch() throws Exception {

                AuthRequest register = new AuthRequest(
                        "device@test.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );

                MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                        .andReturn();

                Cookie refreshCookie = result.getResponse().getCookie("refresh_token");

                RefreshTokenRequest request = new RefreshTokenRequest("wrongDevice");

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh")
                                .cookie(refreshCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
        }

        // == Test de /api/auth/logout ==

        @Test
        void logout_shouldClearRefreshTokenCookie() throws Exception {

                //Registrar usuario y obtener cookie refresh
                AuthRequest register = new AuthRequest(
                        "logout@email.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );

                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                        .andExpect(status().isOk())
                        .andReturn();

                Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");
                assertNotNull(refreshCookie);

                // Obtener el access token del registro
                String responseBody = registerResult.getResponse().getContentAsString();

                String accessToken = objectMapper.readTree(responseBody).get("token").asText();

                //Preparar request logout
                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                //Llamada al endpoint /logout con Authorization
                MvcResult logoutResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
                        .cookie(refreshCookie)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andReturn();

                //Verificar que la cookie se ha borrado
                Cookie deletedCookie = logoutResult.getResponse().getCookie("refresh_token");
                assertNotNull(deletedCookie);
                assertEquals(0, deletedCookie.getMaxAge(), "La cookie debe eliminarse (MaxAge = 0)");
                assertNull(deletedCookie.getValue(), "El valor de la cookie debe ser nulo tras logout");
        }

        @Test
        void logout_shouldFailWithInvalidRefreshToken() throws Exception {

                // Creamos una cookie con un token falso
                Cookie fakeCookie = new Cookie("refresh_token", "invalidToken");

                // DeviceId cualquiera
                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
                                .cookie(fakeCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
        }

        @Test
        void logout_shouldFailWhenDeviceIdDoesNotMatch() throws Exception {

                //Registrar usuario y obtener refresh token real
                AuthRequest register = new AuthRequest(
                        "logout2@email.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );

                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                        .andExpect(status().isOk())
                        .andReturn();

                Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");
                assertNotNull(refreshCookie);

                //Hacemos logout con deviceId incorrecto
                RefreshTokenRequest wrongDeviceRequest = new RefreshTokenRequest("wrongDevice");

                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
                                .cookie(refreshCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongDeviceRequest)))
                        .andExpect(status().isUnauthorized());
        }

        // == Test de /api/auth/validate ==

        @Test
        void validateToken_shouldReturnBadRequestWhenMissingHeader() throws Exception {
                mockMvc.perform(get("/api/auth/validate"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.authenticated").value(false))
                        .andExpect(jsonPath("$.message").value("Missing token"));
        }

        @Test
        void validateToken_shouldReturnBadRequestWhenHeaderMalformed() throws Exception {
                mockMvc.perform(get("/api/auth/validate")
                                .header("Authorization", "Token xyz"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.authenticated").value(false))
                        .andExpect(jsonPath("$.message").value("Missing token"));
        }

        @Test
        void validateToken_shouldReturnInvalidForInvalidToken() throws Exception {
                String fakeToken = "this.is.not.valid";

                mockMvc.perform(get("/api/auth/validate")
                                .header("Authorization", "Bearer " + fakeToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated").value(false))
                        .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        void validateToken_shouldReturnValidForValidToken() throws Exception {

                //Registrar usuario y obtener access token
                AuthRequest register = new AuthRequest(
                        "validate@email.com",
                        "password",
                        "device1",
                        Role.LANDLORD
                );

                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(register)))
                        .andExpect(status().isOk())
                        .andReturn();

                String responseBody = registerResult.getResponse().getContentAsString();
                String accessToken = objectMapper.readTree(responseBody).get("token").asText();

                //Validar el access token
                mockMvc.perform(get("/api/auth/validate")
                                .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated").value(true))
                        .andExpect(jsonPath("$.message").value("Token valid"));
        }

}
