package com.example.demo.Chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.Chat.DTOs.ChatMessageDTO;
import com.example.demo.Chat.DTOs.SendMessageDTO;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatWebSocketControllerTest {

    @MockitoBean
    private ChatService chatService;

    private ChatWebSocketController controller;

    private final Integer matchId = 1;
    private final String username = "user@example.com";

    @BeforeEach
    void setup() {
        controller = new ChatWebSocketController(chatService);
    }

    @Test
    @DisplayName("sendMessage should return message DTO for valid input")
    void sendMessage_ReturnsMessageDTO() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO("Hello WebSocket");

        ChatMessageDTO returnedMessage = new ChatMessageDTO(
                1,
                matchId,
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
    @DisplayName("sendMessage should throw ResourceNotFoundException if match not found")
    void sendMessage_ThrowsNotFound() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO("Hello");

        when(chatService.sendMessage(eq(matchId), eq("Hello"), eq(username)))
            .thenThrow(new ResourceNotFoundException("Match not found"));

        Principal principal = () -> username;

        try {
            controller.sendMessage(matchId, sendMessageDTO, principal);
        } catch (ResourceNotFoundException ex) {
            assertThat(ex.getMessage()).isEqualTo("Match not found");
        }
    }

    @Test
    @DisplayName("sendMessage should throw AccessDeniedException if user not participant")
    void sendMessage_ThrowsForbidden() {
        Integer matchId = 1;
        String username = "user@example.com";
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
    @DisplayName("sendMessage should throw ConflictException if match status invalid")
    void sendMessage_ThrowsConflict() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO("Hello");

        when(chatService.sendMessage(eq(matchId), eq("Hello"), eq(username)))
            .thenThrow(new ConflictException("Invalid match status"));

        Principal principal = () -> username;

        try {
            controller.sendMessage(matchId, sendMessageDTO, principal);
        } catch (ConflictException ex) {
            assertThat(ex.getMessage()).isEqualTo("Invalid match status");
        }
    }

    // == CASO DE VALIDACIÓN DE MENSAJE VACÍO O NULO ==

    @Test
    @DisplayName("sendMessage should allow empty string if service handles it")
    void sendMessage_AllowsEmptyString() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO("");

        ChatMessageDTO returnedMessage = new ChatMessageDTO(
                1,
                matchId,
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
    @DisplayName("sendMessage should throw NullPointerException if content is null")
    void sendMessage_ThrowsIfNullContent() {
        SendMessageDTO sendMessageDTO = new SendMessageDTO(null);

        when(chatService.sendMessage(eq(matchId), eq(null), eq(username)))
            .thenThrow(new NullPointerException("Content cannot be null"));

        Principal principal = () -> username;

        try {
            controller.sendMessage(matchId, sendMessageDTO, principal);
        } catch (NullPointerException ex) {
            assertThat(ex.getMessage()).isEqualTo("Content cannot be null");
        }
    }
}