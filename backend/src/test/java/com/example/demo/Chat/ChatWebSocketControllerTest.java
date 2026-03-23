package com.example.demo.Chat;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.security.access.AccessDeniedException;

import com.example.demo.Chat.DTOs.ChatMessageDTO;
import com.example.demo.Chat.DTOs.SendMessageDTO;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;

public class ChatWebSocketControllerTest {

    private ChatService chatService;
    private ChatWebSocketController controller;

    private final Integer matchId = 1;
    private final Integer incidentId = 10;
    private final String username = "user@example.com";

    @BeforeEach
    void setup() {
        chatService = mock(ChatService.class);
        controller = new ChatWebSocketController(chatService);
    }

    @Test
    @DisplayName("sendMessage should return message DTO for valid input")
    void sendMessage_ReturnsMessageDTO() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO("Hello WebSocket");

        ChatMessageDTO returnedMessage = new ChatMessageDTO(
                1,
                matchId,
                null,
                2,
                "John Doe",
                "Hello WebSocket",
                LocalDateTime.now(),
                MessageStatus.SENT,
                MessageType.TEXT,
                null,
                null
        );

        when(chatService.sendMessage(eq(matchId), eq("Hello WebSocket"), eq(username)))
            .thenReturn(returnedMessage);

        Principal principal = () -> username;

        ChatMessageDTO response = controller.sendMessage(matchId, sendMessageDTO, principal);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Hello WebSocket");
        assertThat(response.matchId()).isEqualTo(matchId);
    }

    // == CASOS DE ERROR ==

    @Test
    @DisplayName("sendMessage throws NotFound")
    void sendMessage_ThrowsNotFound() {
        SendMessageDTO dto = new SendMessageDTO("Hello");
        when(chatService.sendMessage(eq(matchId), eq("Hello"), eq(username)))
                .thenThrow(new ResourceNotFoundException("Match not found"));

        Principal principal = () -> username;

        ResourceNotFoundException ex = Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> controller.sendMessage(matchId, dto, principal)
        );
        assertThat(ex.getMessage()).isEqualTo("Match not found");
    }

    @Test
    @DisplayName("sendMessage should throw AccessDeniedException if user not participant")
    void sendMessage_ThrowsForbidden() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO("Hello");

        when(chatService.sendMessage(eq(matchId), eq("Hello"), eq(username)))
            .thenThrow(new AccessDeniedException("Not participant"));

        Principal principal = () -> username;

        AccessDeniedException ex = Assertions.assertThrows(
            AccessDeniedException.class,
            () -> controller.sendMessage(matchId, sendMessageDTO, principal)
        );

        assertThat(ex.getMessage()).isEqualTo("Not participant");
    }

    @Test
    @DisplayName("sendMessage throws Conflict")
    void sendMessage_ThrowsConflict() {
        SendMessageDTO dto = new SendMessageDTO("Hello");
        when(chatService.sendMessage(eq(matchId), eq("Hello"), eq(username)))
                .thenThrow(new ConflictException("Invalid match status"));

        Principal principal = () -> username;

        ConflictException ex = Assertions.assertThrows(
                ConflictException.class,
                () -> controller.sendMessage(matchId, dto, principal)
        );
        assertThat(ex.getMessage()).isEqualTo("Invalid match status");
    }

    // == CASO DE VALIDACIÓN DE MENSAJE VACÍO O NULO ==

    @Test
    @DisplayName("sendMessage should allow empty string if service handles it")
    void sendMessage_AllowsEmptyString() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO("");

        ChatMessageDTO returnedMessage = new ChatMessageDTO(
                1,
                matchId,
                null,
                2,
                "John Doe",
                "",
                LocalDateTime.now(),
                MessageStatus.SENT,
                MessageType.TEXT,
                null,
                null
        );

        when(chatService.sendMessage(eq(matchId), eq(""), eq(username)))
            .thenReturn(returnedMessage);

        Principal principal = () -> username;

        ChatMessageDTO response = controller.sendMessage(matchId, sendMessageDTO, principal);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("sendMessage throws NullPointerException if content null")
    void sendMessage_ThrowsIfNullContent() {
        SendMessageDTO dto = new SendMessageDTO(null);
        when(chatService.sendMessage(eq(matchId), eq(null), eq(username)))
                .thenThrow(new NullPointerException("Content cannot be null"));

        Principal principal = () -> username;

        NullPointerException ex = Assertions.assertThrows(
                NullPointerException.class,
                () -> controller.sendMessage(matchId, dto, principal)
        );
        assertThat(ex.getMessage()).isEqualTo("Content cannot be null");
    }

    @Test
    @DisplayName("sendIncidentMessage should return message DTO for valid input")
    void sendIncidentMessage_ReturnsMessageDTO() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO("Hola incidencia");

        ChatMessageDTO returnedMessage = new ChatMessageDTO(
                7,
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

        when(chatService.sendIncidentMessage(eq(incidentId), eq("Hola incidencia"), eq(username)))
                .thenReturn(returnedMessage);

        Principal principal = () -> username;

        ChatMessageDTO response = controller.sendIncidentMessage(incidentId, sendMessageDTO, principal);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Hola incidencia");
        assertThat(response.incidentId()).isEqualTo(incidentId);
        assertThat(response.matchId()).isNull();
    }

    @Test
    @DisplayName("sendIncidentMessage throws AccessDeniedException when user is not participant")
    void sendIncidentMessage_ThrowsForbidden() {
        SendMessageDTO dto = new SendMessageDTO("Hola");
        when(chatService.sendIncidentMessage(eq(incidentId), eq("Hola"), eq(username)))
                .thenThrow(new AccessDeniedException("Not participant"));

        Principal principal = () -> username;

        AccessDeniedException ex = Assertions.assertThrows(
                AccessDeniedException.class,
                () -> controller.sendIncidentMessage(incidentId, dto, principal)
        );
        assertThat(ex.getMessage()).isEqualTo("Not participant");
    }
}