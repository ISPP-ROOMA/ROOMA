package com.example.demo.Chat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Chat.DTOs.ChatMessageDTO;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.JwtService;

@WebMvcTest(ChatController.class)
@Import(ChatControllerTest.SecurityTestConfig.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

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

    // == Test for GET /api/chat/{matchId}/messages ==

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getMessageHistory should return 200 for tenant")
    void getMessageHistory_ReturnsOkForTenant() throws Exception {
        Integer matchId = 1;
        ChatMessageDTO message = new ChatMessageDTO(
            1,
            10,
            null,
            2,
            "John Doe",
            "Hello",
            LocalDateTime.now(),
            MessageStatus.SENT,
            MessageType.TEXT,
            null,
            null
        );
        when(chatService.getMessageHistory(matchId)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/chat/{matchId}/messages", matchId))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("getMessageHistory should return 200 for landlord")
    void getMessageHistory_ReturnsOkForLandlord() throws Exception {
        Integer matchId = 1;
        ChatMessageDTO message = new ChatMessageDTO(
            1,
            10,
            null,
            2,
            "John Doe",
            "Hello",
            LocalDateTime.now(),
            MessageStatus.SENT,
            MessageType.TEXT,
            null,
            null
        );
        when(chatService.getMessageHistory(matchId)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/chat/{matchId}/messages", matchId))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getMessageHistory should return 404 if match not found")
    void getMessageHistory_ReturnsNotFoundIfMatchNotFound() throws Exception {
        Integer matchId = 999;
        when(chatService.getMessageHistory(matchId))
                .thenThrow(new ResourceNotFoundException("Match not found"));

        mockMvc.perform(get("/api/chat/{matchId}/messages", matchId))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getMessageHistory should return 403 if user not participant")
    void getMessageHistory_ReturnsForbiddenIfUserNotParticipant() throws Exception {
        Integer matchId = 1;
        when(chatService.getMessageHistory(matchId))
                .thenThrow(new AccessDeniedException("Not participant"));

        mockMvc.perform(get("/api/chat/{matchId}/messages", matchId))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getMessageHistory should return 409 if match status invalid")
    void getMessageHistory_ReturnsConflictIfStatusInvalid() throws Exception {
        Integer matchId = 1;
        when(chatService.getMessageHistory(matchId))
                .thenThrow(new ConflictException("Invalid match status"));

        mockMvc.perform(get("/api/chat/{matchId}/messages", matchId))
               .andExpect(status().isConflict());
    }

    // == Test for PUT /api/chat/{matchId}/read ==

    @Test
    @WithMockUser(username = "tenant@example.com", roles = "TENANT")
    @DisplayName("markAsRead should return 200 and the updated messages")
    void markAsRead_ReturnsOkAndMessages() throws Exception {
        Integer matchId = 1;

        ChatMessageDTO message = new ChatMessageDTO(
            1,
            matchId,
            null,
            2,
            "John Doe",
            "Hello",
            LocalDateTime.now(),
            MessageStatus.SENT,
            MessageType.TEXT,
            null,
            null
        );

        when(chatService.markMessagesAsRead(matchId, "tenant@example.com"))
            .thenReturn(List.of(message));

        mockMvc.perform(
                put("/api/chat/{matchId}/read", matchId)
            )
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "tenant@example.com", roles = "TENANT")
    @DisplayName("markAsRead should return 404 if match not found")
    void markAsRead_ReturnsNotFoundIfMatchNotFound() throws Exception {
        Integer matchId = 999;

        when(chatService.markMessagesAsRead(matchId, "tenant@example.com"))
            .thenThrow(new ResourceNotFoundException("Match not found"));

        mockMvc.perform(put("/api/chat/{matchId}/read", matchId))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "tenant@example.com", roles = "TENANT")
    @DisplayName("markAsRead should return 403 if user not participant")
    void markAsRead_ReturnsForbiddenIfUserNotParticipant() throws Exception {
        Integer matchId = 1;

        when(chatService.markMessagesAsRead(matchId, "tenant@example.com"))
            .thenThrow(new AccessDeniedException("Not participant"));

        mockMvc.perform(put("/api/chat/{matchId}/read", matchId))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tenant@example.com", roles = "TENANT")
    @DisplayName("markAsRead should return 409 if match status invalid")
    void markAsRead_ReturnsConflictIfStatusInvalid() throws Exception {
        Integer matchId = 1;

        when(chatService.markMessagesAsRead(matchId, "tenant@example.com"))
            .thenThrow(new ConflictException("Invalid match status"));

        mockMvc.perform(put("/api/chat/{matchId}/read", matchId))
            .andExpect(status().isConflict());
    }

    // == Test for POST /api/chat/{matchId}/file ==

    @Test
    @WithMockUser(username = "tenant@example.com", roles = "TENANT")
    @DisplayName("uploadFile should return 200 and send message to topic")
    void uploadFile_ReturnsOkAndSendsMessage() throws Exception {
        Integer matchId = 1;

        ChatMessageDTO message = new ChatMessageDTO(
            1,
            matchId,
            null,
            2,
            "John Doe",
            "File uploaded",
            LocalDateTime.now(),
            MessageStatus.SENT,
            MessageType.FILE,
            "https://example.com/file.png",
            "file.png"
        );

        when(chatService.sendFileMessage(
            eq(matchId),
            any(MultipartFile.class),
            eq("File uploaded"),
            eq("tenant@example.com")
        )).thenReturn(message);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "file.png",
            "image/png",
            "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/chat/{matchId}/file", matchId)
                .file(file)
                .param("caption", "File uploaded"))
            .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("uploadFile should return 409 if file is empty")
    void uploadFile_ReturnsConflictIfFileEmpty() throws Exception {
        Integer matchId = 1;
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);

        when(chatService.sendFileMessage(eq(matchId), any(), any(), any()))
                .thenThrow(new ConflictException("File cannot be empty"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/chat/{matchId}/file", matchId)
                .file(file)
                .with(request -> { request.setUserPrincipal(() -> "user@example.com"); return request; })
        ).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("uploadFile should return 404 if match not found")
    void uploadFile_ReturnsNotFoundIfMatchNotFound() throws Exception {
        Integer matchId = 999;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello".getBytes()
        );

        when(chatService.sendFileMessage(eq(matchId), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Match not found"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/chat/{matchId}/file", matchId)
                .file(file)
                .with(request -> { request.setUserPrincipal(() -> "user@example.com"); return request; })
        ).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("uploadFile should return 403 if user not participant")
    void uploadFile_ReturnsForbiddenIfUserNotParticipant() throws Exception {
        Integer matchId = 1;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello".getBytes()
        );

        when(chatService.sendFileMessage(eq(matchId), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Not participant"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/chat/{matchId}/file", matchId)
                .file(file)
                .with(request -> { request.setUserPrincipal(() -> "user@example.com"); return request; })
        ).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("getIncidentMessageHistory should return 200")
    void getIncidentMessageHistory_ReturnsOk() throws Exception {
        Integer incidentId = 55;
        ChatMessageDTO message = new ChatMessageDTO(
            1,
            null,
            incidentId,
            2,
            "John Doe",
            "Hola incidencia",
            LocalDateTime.now(),
            MessageStatus.SENT,
            MessageType.TEXT,
            null,
            null
        );

        when(chatService.getIncidentMessageHistory(incidentId)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/chat/incidents/{incidentId}/messages", incidentId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "tenant@example.com", roles = "TENANT")
    @DisplayName("markIncidentAsRead should return 200")
    void markIncidentAsRead_ReturnsOk() throws Exception {
        Integer incidentId = 55;
        ChatMessageDTO message = new ChatMessageDTO(
            1,
            null,
            incidentId,
            2,
            "John Doe",
            "Hola incidencia",
            LocalDateTime.now(),
            MessageStatus.READ,
            MessageType.TEXT,
            null,
            null
        );

        when(chatService.markIncidentMessagesAsRead(incidentId, "tenant@example.com"))
                .thenReturn(List.of(message));

        mockMvc.perform(put("/api/chat/incidents/{incidentId}/read", incidentId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "tenant@example.com", roles = "TENANT")
    @DisplayName("uploadIncidentFile should return 200")
    void uploadIncidentFile_ReturnsOk() throws Exception {
        Integer incidentId = 55;
        ChatMessageDTO message = new ChatMessageDTO(
            1,
            null,
            incidentId,
            2,
            "John Doe",
            "Adjunto incidencia",
            LocalDateTime.now(),
            MessageStatus.SENT,
            MessageType.FILE,
            "https://example.com/file.png",
            "file.png"
        );

        when(chatService.sendIncidentFileMessage(
                eq(incidentId),
                any(MultipartFile.class),
                eq("Adjunto incidencia"),
                eq("tenant@example.com")
        )).thenReturn(message);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "file.png",
            "image/png",
            "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/chat/incidents/{incidentId}/file", incidentId)
                .file(file)
                .param("caption", "Adjunto incidencia"))
            .andExpect(status().isOk());
    }

    

}
