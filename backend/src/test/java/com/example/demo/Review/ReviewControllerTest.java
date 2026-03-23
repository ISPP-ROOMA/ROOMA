package com.example.demo.Review;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ReviewController.class)
@Import(ReviewControllerTest.SecurityTestConfig.class)
@DisplayName("ReviewController Integration Tests")
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    @WithMockUser(roles = "LANDLORD")
    void createReviewByLandlord_created() throws Exception {
        ReviewEntity review = reviewEntity(1, 5, "Buen inquilino");
        when(reviewService.makeReviewByLandlord(2, 100, "Buen inquilino", 5)).thenReturn(review);

        mockMvc.perform(post("/api/reviews/landlord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewedUserId\":2,\"apartmentId\":100,\"rating\":5,\"comment\":\"Buen inquilino\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5));

        verify(reviewService).makeReviewByLandlord(2, 100, "Buen inquilino", 5);
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void createReviewByTenant_created() throws Exception {
        ReviewEntity review = reviewEntity(2, 4, "Buen casero");
        when(reviewService.makeReviewByTenant(1, 100, "Buen casero", 4)).thenReturn(review);

        mockMvc.perform(post("/api/reviews/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewedUserId\":1,\"apartmentId\":100,\"rating\":4,\"comment\":\"Buen casero\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.comment").value("Buen casero"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void createReview_validationError() throws Exception {
        mockMvc.perform(post("/api/reviews/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewedUserId\":1,\"apartmentId\":100,\"rating\":8,\"comment\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void getMadeReviews_ok() throws Exception {
        when(reviewService.findMadeReviewsByUserId()).thenReturn(List.of(reviewEntity(10, 5, "A")));

        mockMvc.perform(get("/api/reviews/made"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void getReceivedReviews_ok() throws Exception {
        when(reviewService.findReceivedReviewsByUserId()).thenReturn(List.of(reviewEntity(11, 4, "B")));

        mockMvc.perform(get("/api/reviews/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void getByApartment_ok() throws Exception {
        when(reviewService.findMadeReviewsByUserIdAndApartmentId(100)).thenReturn(List.of(reviewEntity(12, 4, "C")));
        when(reviewService.findReceivedReviewsByUserIdAndApartmentId(100)).thenReturn(List.of(reviewEntity(13, 5, "D")));

        mockMvc.perform(get("/api/reviews/made/apartment/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12));

        mockMvc.perform(get("/api/reviews/received/apartment/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(13));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void getReviewableAndPending_ok() throws Exception {
        UserEntity user = user(9, Role.TENANT, "flatmate@test.com");
        when(reviewService.getReviewableUsers(100)).thenReturn(List.of(user));

        ApartmentEntity apt = apartment(100);
        ReviewService.PendingUserInfo pendingUserInfo = new ReviewService.PendingUserInfo(user, true, false);
        ReviewService.PendingReviewApartment pendingReviewApartment = new ReviewService.PendingReviewApartment(apt, List.of(pendingUserInfo));
        when(reviewService.getPendingReviewApartments()).thenReturn(List.of(pendingReviewApartment));

        mockMvc.perform(get("/api/reviews/reviewable/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));

        mockMvc.perform(get("/api/reviews/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apartmentId").value(100))
                .andExpect(jsonPath("$[0].pendingUsers[0].hasReviewedYou").value(true));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void respondToReview_ok() throws Exception {
        ReviewEntity review = reviewEntity(20, 5, "Excelente");
        review.setResponse("Gracias");
        when(reviewService.respondToReview(20, "Gracias")).thenReturn(review);

        mockMvc.perform(put("/api/reviews/20/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResponseBody("Gracias"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Gracias"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    void respondToReview_badRequest() throws Exception {
        when(reviewService.respondToReview(20, "x")).thenThrow(new BadRequestException("No permitido"));

        mockMvc.perform(put("/api/reviews/20/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthorized_whenNoUser() throws Exception {
        mockMvc.perform(get("/api/reviews/made"))
                .andExpect(status().isUnauthorized());
    }

    private ReviewEntity reviewEntity(Integer id, Integer rating, String comment) {
        ReviewEntity review = new ReviewEntity();
        review.setId(id);
        review.setRating(rating);
        review.setComment(comment);
        review.setPublished(true);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewMember(user(1, Role.LANDLORD, "landlord@test.com"));
        review.setReviewedMember(user(2, Role.TENANT, "tenant@test.com"));
        review.setApartment(apartment(100));
        return review;
    }

    private UserEntity user(Integer id, Role role, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRole(role);
        user.setEmail(email);
        return user;
    }

    private ApartmentEntity apartment(Integer id) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(id);
        apartment.setTitle("Apt " + id);
        apartment.setUbication("Sevilla");
        return apartment;
    }

    private record ResponseBody(String response) {
    }
}
