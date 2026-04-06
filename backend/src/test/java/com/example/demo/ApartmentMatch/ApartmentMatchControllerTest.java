package com.example.demo.ApartmentMatch;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;

@WebMvcTest(ApartmentMatchController.class)
@Import(ApartmentMatchControllerTest.SecurityTestConfig.class)
public class ApartmentMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApartmentMatchService apartmentMatchService;

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
    @DisplayName("tenant swipe endpoint returns 401 when unauthenticated")
    void processSwipeTenant_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/apartments-matches/swipe/apartment/{apartmentId}/tenant", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isUnauthorized());

        verify(apartmentMatchService, never()).processSwipe(eq(10), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("tenant swipe endpoint returns 403 for landlord role")
    void processSwipeTenant_WrongRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/apartments-matches/swipe/apartment/{apartmentId}/tenant", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Access denied")));

        verify(apartmentMatchService, never()).processSwipe(eq(10), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("tenant swipe endpoint returns 200 and dto body for tenant")
    void processSwipeTenant_ReturnsOk() throws Exception {
        ApartmentMatchEntity match = createMatch(1, 10, 20, MatchStatus.ACTIVE, Role.LANDLORD);
        when(apartmentMatchService.processSwipe(20, true)).thenReturn(match);

        mockMvc.perform(post("/api/apartments-matches/swipe/apartment/{apartmentId}/tenant", 20)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.candidateId").value(10))
                .andExpect(jsonPath("$.apartmentId").value(20))
                .andExpect(jsonPath("$.matchStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("tenant swipe endpoint returns 409 when service throws conflict")
    void processSwipeTenant_ServiceConflict_Returns409() throws Exception {
        when(apartmentMatchService.processSwipe(21, true))
                .thenThrow(new ConflictException("You have already swiped on this apartment"));

        mockMvc.perform(post("/api/apartments-matches/swipe/apartment/{apartmentId}/tenant", 21)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You have already swiped on this apartment"))
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("tenant swipe endpoint returns 400 for invalid apartment id")
    void processSwipeTenant_InvalidApartmentId_Returns400() throws Exception {
        mockMvc.perform(post("/api/apartments-matches/swipe/apartment/{apartmentId}/tenant", "bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter: apartmentId"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("tenant swipe endpoint returns 400 for malformed boolean body")
    void processSwipeTenant_InvalidBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/apartments-matches/swipe/apartment/{apartmentId}/tenant", 22)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interest\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid request body")));

        verify(apartmentMatchService, never()).processSwipe(eq(22), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("landlord action endpoint returns 200 for landlord")
    void processLandlordAction_ReturnsOk() throws Exception {
        ApartmentMatchEntity match = createMatch(2, 11, 23, MatchStatus.MATCH, Role.LANDLORD);
        when(apartmentMatchService.processLandlordAction(2, true)).thenReturn(match);

        mockMvc.perform(post("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/respond-request", 2)
                        .param("interest", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("MATCH"));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("landlord action endpoint returns 404 when service throws not found")
    void processLandlordAction_NotFound_Returns404() throws Exception {
        when(apartmentMatchService.processLandlordAction(3, true))
                .thenThrow(new ResourceNotFoundException("Match not found"));

        mockMvc.perform(post("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/respond-request", 3)
                        .param("interest", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Match not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("landlord action endpoint returns 403 for tenant role")
    void processLandlordAction_TenantRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/respond-request", 4)
                        .param("interest", "false"))
                .andExpect(status().isForbidden());

        verify(apartmentMatchService, never()).processLandlordAction(eq(4), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("sendInvitation returns 200 for landlord")
    void sendInvitation_ReturnsOk() throws Exception {
        ApartmentMatchEntity match = createMatch(5, 12, 24, MatchStatus.INVITED, Role.LANDLORD);
        when(apartmentMatchService.sendInvitation(5)).thenReturn(match);

        mockMvc.perform(post("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/send-invitation", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.matchStatus").value("INVITED"))
                .andExpect(jsonPath("$.landlord.id").value(25))
                .andExpect(jsonPath("$.apartment.id").value(24));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("sendInvitation returns 403 when service denies access")
    void sendInvitation_AccessDenied_Returns403() throws Exception {
        when(apartmentMatchService.sendInvitation(6))
                .thenThrow(new AccessDeniedException("Only the landlord of the apartment can send an invitation"));

        mockMvc.perform(post("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/send-invitation", 6))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only the landlord of the apartment can send an invitation")))
                .andExpect(jsonPath("$.statusCode").value(403));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("respondToInvitation returns 200 for tenant")
    void respondToInvitation_ReturnsOk() throws Exception {
        ApartmentMatchEntity match = createMatch(7, 13, 25, MatchStatus.SUCCESSFUL, Role.LANDLORD);
        when(apartmentMatchService.respondToInvitation(7, true)).thenReturn(match);

        mockMvc.perform(post("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/respond-invitation", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.matchStatus").value("SUCCESSFUL"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("respondToInvitation returns 409 when service throws conflict")
    void respondToInvitation_Conflict_Returns409() throws Exception {
        when(apartmentMatchService.respondToInvitation(8, false))
                .thenThrow(new ConflictException("Only matches with status INVITED can be responded to"));

        mockMvc.perform(post("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/respond-invitation", 8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("false"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only matches with status INVITED can be responded to"))
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("respondToInvitation returns 400 for malformed boolean body")
    void respondToInvitation_InvalidBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/respond-invitation", 9)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid request body")));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("getInterestedCandidates returns landlord dto list")
    void getInterestedCandidates_ReturnsOk() throws Exception {
        ApartmentMatchEntity match = createMatch(10, 14, 26, MatchStatus.ACTIVE, Role.LANDLORD);
        when(apartmentMatchService.findInterestedCandidatesByApartmentIdAndStatus(26, MatchStatus.ACTIVE))
                .thenReturn(List.of(match));

        mockMvc.perform(get("/api/apartments-matches/apartment/{apartmentId}/interested-candidates/{status}", 26, "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].matchStatus").value("ACTIVE"))
                .andExpect(jsonPath("$[0].landlord.id").value(27));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("getInterestedCandidates returns 400 for invalid MatchStatus")
    void getInterestedCandidates_InvalidStatus_Returns400() throws Exception {
        mockMvc.perform(get("/api/apartments-matches/apartment/{apartmentId}/interested-candidates/{status}", 27, "WRONG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter: status"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getAllTenantRequest returns summary dto list")
    void getAllTenantRequest_ReturnsOk() throws Exception {
        ApartmentMatchEntity match = createMatch(11, 15, 28, MatchStatus.ACTIVE, Role.LANDLORD);
        when(apartmentMatchService.findTenantRequestByUserIdAndStatus(MatchStatus.ACTIVE))
                .thenReturn(List.of(match));

        mockMvc.perform(get("/api/apartments-matches/my-requests/{status}", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].matchStatus").value("ACTIVE"))
                .andExpect(jsonPath("$[0].apartment.id").value(28));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("tenant detail endpoint returns 403 when service denies access")
    void getApartmentMatchDetailsForTenant_AccessDenied_Returns403() throws Exception {
        when(apartmentMatchService.findMyMatchForTenant(12))
                .thenThrow(new AccessDeniedException("You can only view your own matches"));

        mockMvc.perform(patch("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/tenant-match-details", 12))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("You can only view your own matches")))
                .andExpect(jsonPath("$.statusCode").value(403));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("landlord detail endpoint returns tenant dto")
    void getApartmentMatchDetailsForLandlord_ReturnsOk() throws Exception {
        ApartmentMatchEntity match = createMatch(13, 16, 29, MatchStatus.MATCH, Role.LANDLORD);
        when(apartmentMatchService.findMyMatchForLandlord(13)).thenReturn(match);

        mockMvc.perform(get("/api/apartments-matches/apartmentMatch/{apartmentMatchId}/landlord-match-details", 13))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.tenant.id").value(16))
                .andExpect(jsonPath("$.apartment.id").value(29));
    }

    @Test
    @WithMockUser(username = "tenant100@test.com", roles = "TENANT")
    @DisplayName("legacy swipe endpoint returns 403 when authenticated user id does not match candidate id")
    void legacySwipe_MismatchedCandidate_Returns403() throws Exception {
        when(apartmentMatchService.getUserByEmail("tenant100@test.com"))
                .thenReturn(createUser(200, Role.TENANT, "tenant100@test.com"));

        mockMvc.perform(post("/api/apartments-matches/swipe/candidate/{candidateId}/apartment/{apartmentId}/action/{isCandidateAction}",
                        201, 30, true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only perform swipe actions for your own user"));

        verify(apartmentMatchService, never()).processSwipe(eq(201), eq(30), eq(true), eq(true));
    }

    @Test
    @WithMockUser(username = "tenant101@test.com", roles = "TENANT")
    @DisplayName("legacy swipe endpoint returns 200 when authenticated user matches candidate id")
    void legacySwipe_AuthenticatedCandidate_ReturnsOk() throws Exception {
        when(apartmentMatchService.getUserByEmail("tenant101@test.com"))
                .thenReturn(createUser(202, Role.TENANT, "tenant101@test.com"));
        when(apartmentMatchService.processSwipe(202, 31, true, true))
                .thenReturn(createMatch(14, 202, 31, MatchStatus.ACTIVE, Role.LANDLORD));

        mockMvc.perform(post("/api/apartments-matches/swipe/candidate/{candidateId}/apartment/{apartmentId}/action/{isCandidateAction}",
                        202, 31, true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(14))
                .andExpect(jsonPath("$.candidateId").value(202))
                .andExpect(jsonPath("$.apartmentId").value(31));
    }

    private ApartmentMatchEntity createMatch(Integer matchId, Integer candidateId, Integer apartmentId, MatchStatus status,
            Role landlordRole) {
        UserEntity candidate = createUser(candidateId, Role.TENANT, "tenant" + candidateId + "@test.com");
        UserEntity landlord = createUser(apartmentId + 1, landlordRole, "landlord" + apartmentId + "@test.com");

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setTitle("Apartment " + apartmentId);
        apartment.setDescription("Description " + apartmentId);
        apartment.setPrice(500.0);
        apartment.setBills("wifi");
        apartment.setUbication("Madrid");
        apartment.setState(ApartmentState.ACTIVE);
        apartment.setUser(landlord);

        ApartmentMatchEntity match = new ApartmentMatchEntity();
        match.setId(matchId);
        match.setCandidate(candidate);
        match.setApartment(apartment);
        match.setMatchStatus(status);
        match.setCandidateInterest(true);
        match.setLandlordInterest(status == MatchStatus.ACTIVE ? null : true);
        return match;
    }

    private UserEntity createUser(Integer id, Role role, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRole(role);
        user.setEmail(email);
        user.setPassword("encoded-password");
        return user;
    }
}
