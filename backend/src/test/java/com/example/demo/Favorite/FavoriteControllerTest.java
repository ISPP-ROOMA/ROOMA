package com.example.demo.Favorite;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.Apartment.ApartmentState;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Favorite.DTOs.FavoriteSummaryDTO;
import com.example.demo.Favorite.DTOs.FavoriteToggleResponseDTO;
import com.example.demo.Jwt.JwtService;

@WebMvcTest(FavoriteController.class)
@Import(FavoriteControllerTest.SecurityTestConfig.class)
public class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    // Required because the production JWT filter is still part of the web slice context.
    @MockitoBean
    private JwtService jwtService;

    @TestConfiguration
    @EnableMethodSecurity
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getCurrentUserFavorites returns 200 and favorite summaries for authenticated user")
    public void getCurrentUserFavorites_ReturnsOk() throws Exception {
        FavoriteSummaryDTO favorite = new FavoriteSummaryDTO(
                10,
                "Apartment 10",
                "Madrid Centro",
                "Madrid Centro",
                550.0,
                ApartmentState.ACTIVE,
                true,
                "AVAILABLE",
                true,
                true,
                "https://images.test/favorite-cover.jpg",
                true,
                null,
                LocalDateTime.of(2026, 3, 17, 12, 30)
        );

        when(favoriteService.getCurrentUserFavorites()).thenReturn(List.of(favorite));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apartmentId").value(10))
                .andExpect(jsonPath("$[0].title").value("Apartment 10"))
                .andExpect(jsonPath("$[0].availabilityStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].mainImageUrl").value("https://images.test/favorite-cover.jpg"))
                .andExpect(jsonPath("$[0].isFavorite").value(true));
    }

    @Test
    @DisplayName("getCurrentUserFavorites returns 401 when unauthenticated")
    public void getCurrentUserFavorites_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).getCurrentUserFavorites();
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getCurrentUserFavorites returns 404 when service throws not found")
    public void getCurrentUserFavorites_WhenServiceThrowsNotFound_Returns404() throws Exception {
        when(favoriteService.getCurrentUserFavorites()).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("addFavorite returns 200 and toggle response for authenticated user")
    public void addFavorite_ReturnsOk() throws Exception {
        when(favoriteService.addFavorite(25)).thenReturn(new FavoriteToggleResponseDTO(25, true, "Apartment added to favorites"));

        mockMvc.perform(put("/api/favorites/{apartmentId}", 25))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartmentId").value(25))
                .andExpect(jsonPath("$.isFavorite").value(true))
                .andExpect(jsonPath("$.message").value("Apartment added to favorites"));
    }

    @Test
    @DisplayName("addFavorite returns 401 when unauthenticated")
    public void addFavorite_Unauthenticated() throws Exception {
        mockMvc.perform(put("/api/favorites/{apartmentId}", 25))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).addFavorite(25);
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("addFavorite returns 404 when service throws not found")
    public void addFavorite_WhenServiceThrowsNotFound_Returns404() throws Exception {
        when(favoriteService.addFavorite(25)).thenThrow(new ResourceNotFoundException("Apartment not found"));

        mockMvc.perform(put("/api/favorites/{apartmentId}", 25))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("addFavorite returns 400 when apartment id is not numeric")
    public void addFavorite_WithInvalidApartmentId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/favorites/{apartmentId}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(favoriteService, never()).addFavorite(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("removeFavorite returns 200 and toggle response for authenticated user")
    public void removeFavorite_ReturnsOk() throws Exception {
        when(favoriteService.removeFavorite(30)).thenReturn(new FavoriteToggleResponseDTO(30, false, "Apartment removed from favorites"));

        mockMvc.perform(delete("/api/favorites/{apartmentId}", 30))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartmentId").value(30))
                .andExpect(jsonPath("$.isFavorite").value(false))
                .andExpect(jsonPath("$.message").value("Apartment removed from favorites"));
    }

    @Test
    @DisplayName("removeFavorite returns 401 when unauthenticated")
    public void removeFavorite_Unauthenticated() throws Exception {
        mockMvc.perform(delete("/api/favorites/{apartmentId}", 30))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).removeFavorite(30);
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("removeFavorite returns 404 when service throws not found")
    public void removeFavorite_WhenServiceThrowsNotFound_Returns404() throws Exception {
        when(favoriteService.removeFavorite(30)).thenThrow(new ResourceNotFoundException("Apartment not found"));

        mockMvc.perform(delete("/api/favorites/{apartmentId}", 30))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("removeFavorite returns 400 when apartment id is not numeric")
    public void removeFavorite_WithInvalidApartmentId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/favorites/{apartmentId}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(favoriteService, never()).removeFavorite(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getFavoriteApartmentIds returns 200 and wrapper json for authenticated user")
    public void getFavoriteApartmentIds_ReturnsOk() throws Exception {
        when(favoriteService.getFavoriteApartmentIds(List.of(1, 2, 3))).thenReturn(List.of(1, 3));

        mockMvc.perform(get("/api/favorites/ids")
                        .param("apartmentIds", "1", "2", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteApartmentIds[0]").value(1))
                .andExpect(jsonPath("$.favoriteApartmentIds[1]").value(3));
    }

    @Test
    @DisplayName("getFavoriteApartmentIds returns 401 when unauthenticated")
    public void getFavoriteApartmentIds_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/favorites/ids").param("apartmentIds", "1", "2"))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).getFavoriteApartmentIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getFavoriteApartmentIds returns 400 when apartmentIds is malformed")
    public void getFavoriteApartmentIds_WithMalformedParameter_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/favorites/ids").param("apartmentIds", "1", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter: apartmentIds"))
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(favoriteService, never()).getFavoriteApartmentIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getFavoriteApartmentIds returns 404 when service throws not found")
    public void getFavoriteApartmentIds_WhenServiceThrowsNotFound_Returns404() throws Exception {
        when(favoriteService.getFavoriteApartmentIds(List.of(1, 2))).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/favorites/ids")
                        .param("apartmentIds", "1", "2"))
                .andExpect(status().isNotFound());
    }
}
