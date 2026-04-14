package com.example.demo.Incident;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.Jwt.JwtService;

import java.util.List;

@WebMvcTest(IncidentController.class)
@Import(IncidentControllerTest.SecurityConfig.class)
public class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private JwtService jwtService;

    @TestConfiguration
    @EnableMethodSecurity
    static class SecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }

    @Test
    @WithMockUser(roles = "TENANT")
    public void getIncidentsByApartmentId_AsTenant() throws Exception {
        when(incidentService.findIncidentsByApartmentId(1, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/apartments/1/incidents"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    public void getIncidentsByApartmentId_AsLandlord() throws Exception {
        when(incidentService.findIncidentsByApartmentId(1, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/apartments/1/incidents"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getIncidentsByApartmentId_AsAdmin() throws Exception {
        when(incidentService.findIncidentsByApartmentId(1, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/apartments/1/incidents"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    public void getIncidentsByApartmentId_AsGuest() throws Exception {
        mockMvc.perform(get("/api/apartments/1/incidents"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getIncidentsByApartmentId_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/apartments/1/incidents"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    public void getIncidentById_AsTenant() throws Exception {
        when(incidentService.findIncidentById(1, 1)).thenReturn(null);

        mockMvc.perform(get("/api/apartments/1/incidents/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    public void getIncidentById_AsLandlord() throws Exception {
        when(incidentService.findIncidentById(1, 1)).thenReturn(null);

        mockMvc.perform(get("/api/apartments/1/incidents/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getIncidentById_AsAdmin() throws Exception {
        when(incidentService.findIncidentById(1, 1)).thenReturn(null);

        mockMvc.perform(get("/api/apartments/1/incidents/1"))
                .andExpect(status().isOk());
    }

    @Test
    public void getIncidentById_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/apartments/1/incidents/1"))
                .andExpect(status().isForbidden());
    } 

    @Test
    @WithMockUser(roles = "TENANT")
    public void createIncident_AsTenant() throws Exception {
        MockMultipartFile dataPart = new MockMultipartFile(
            "data",
            "data.json",
            "application/json",
            "{\"title\":\"Leaking Faucet\",\"description\":\"The faucet in the kitchen is leaking.\",\"category\":\"PLUMBING\",\"zone\":\"KITCHEN\",\"urgency\":\"MEDIUM\"}".getBytes()
        );

        when(incidentService.createIncident(org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(null);

        mockMvc.perform(multipart("/api/apartments/1/incidents")
            .file(dataPart))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    public void createIncident_AsLandlord() throws Exception {
        MockMultipartFile dataPart = new MockMultipartFile(
            "data",
            "data.json",
            "application/json",
            "{\"title\":\"Leaking Faucet\",\"description\":\"The faucet in the kitchen is leaking.\",\"category\":\"PLUMBING\",\"zone\":\"KITCHEN\",\"urgency\":\"MEDIUM\"}".getBytes()
        );

        mockMvc.perform(multipart("/api/apartments/1/incidents")
            .file(dataPart))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createIncident_WithoutAuthentication() throws Exception {
        MockMultipartFile dataPart = new MockMultipartFile(
            "data",
            "data.json",
            "application/json",
            "{\"title\":\"Leaking Faucet\",\"description\":\"The faucet in the kitchen is leaking.\",\"category\":\"PLUMBING\",\"zone\":\"KITCHEN\",\"urgency\":\"MEDIUM\"}".getBytes()
        );

        mockMvc.perform(multipart("/api/apartments/1/incidents")
            .file(dataPart))
                .andExpect(status().isForbidden());
    }   

    @Test
    @WithMockUser(roles = "TENANT")
    public void updateIncidentStatusByLandlord_AsTenant() throws Exception {
        mockMvc.perform(patch("/api/apartments/1/incidents/1/status")
                .contentType("application/json")
                .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    public void updateIncidentStatusByLandlord_AsLandlord() throws Exception {
        when(incidentService.updateIncidentStatusByLandlord(1, 1, IncidentStatus.IN_PROGRESS)).thenReturn(null);

        mockMvc.perform(patch("/api/apartments/1/incidents/1/status")
                .contentType("application/json")
                .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void updateIncidentStatusByLandlord_WithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/apartments/1/incidents/1/status")
                .contentType("application/json")
                .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    public void confirmSolution_AsTenant() throws Exception {
        when(incidentService.confirmSolutionByTenant(1, 1)).thenReturn(null);

        mockMvc.perform(patch("/api/apartments/1/incidents/1/confirm-solution"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    public void confirmSolution_AsLandlord() throws Exception {
        mockMvc.perform(patch("/api/apartments/1/incidents/1/confirm-solution"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void confirmSolution_WithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/apartments/1/incidents/1/confirm-solution"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    public void rejectSolution_AsTenant() throws Exception {
        when(incidentService.rejectSolutionByTenant(1, 1, "Still leaking")).thenReturn(null);

        mockMvc.perform(patch("/api/apartments/1/incidents/1/reject-solution")
            .contentType("application/json")
            .content("{\"reason\":\"Still leaking\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    public void rejectSolution_AsLandlord() throws Exception {
        mockMvc.perform(patch("/api/apartments/1/incidents/1/reject-solution")
            .contentType("application/json")
            .content("{\"reason\":\"Still leaking\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void rejectSolution_WithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/apartments/1/incidents/1/reject-solution")
            .contentType("application/json")
            .content("{\"reason\":\"Still leaking\"}"))
                .andExpect(status().isForbidden());
    }
}

