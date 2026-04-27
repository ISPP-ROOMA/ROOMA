package com.example.demo.Chat;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Chat.DTOs.ChatMessageDTO;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    public record IncidentChatStatusDTO(boolean closed, boolean canParticipate, String incidentTenantName) {}

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/{matchId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessageHistory(@PathVariable Integer matchId) {
        List<ChatMessageDTO> messages = chatService.getMessageHistory(matchId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{matchId}/read")
    public ResponseEntity<List<ChatMessageDTO>> markAsRead(
            @PathVariable Integer matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ChatMessageDTO> messages = chatService.markMessagesAsRead(matchId, userDetails.getUsername());
        if (!messages.isEmpty()) {
            for (ChatMessageDTO msg : messages) {
                messagingTemplate.convertAndSend("/topic/chat/" + matchId, msg);
            }
        }
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/incidents/{incidentId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getIncidentMessageHistory(@PathVariable Integer incidentId) {
        List<ChatMessageDTO> messages = chatService.getIncidentMessageHistory(incidentId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/incidents/{incidentId}/read")
    public ResponseEntity<List<ChatMessageDTO>> markIncidentAsRead(
            @PathVariable Integer incidentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ChatMessageDTO> messages = chatService.markIncidentMessagesAsRead(incidentId, userDetails.getUsername());
        if (!messages.isEmpty()) {
            for (ChatMessageDTO msg : messages) {
                messagingTemplate.convertAndSend("/topic/chat/incident/" + incidentId, msg);
            }
        }
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/incidents/{incidentId}/status")
    public ResponseEntity<IncidentChatStatusDTO> getIncidentChatStatus(@PathVariable Integer incidentId) {
        ChatService.IncidentChatAccessInfo accessInfo = chatService.getIncidentChatAccessInfo(incidentId);
        return ResponseEntity.ok(
                new IncidentChatStatusDTO(accessInfo.closed(), accessInfo.canParticipate(), accessInfo.incidentTenantName())
        );
    }

    @PostMapping("/{matchId}/file")
    public ResponseEntity<ChatMessageDTO> uploadFile(
            @PathVariable Integer matchId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        ChatMessageDTO message = chatService.sendFileMessage(matchId, file, caption, userDetails.getUsername());
        messagingTemplate.convertAndSend("/topic/chat/" + matchId, message);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/incidents/{incidentId}/file")
    public ResponseEntity<ChatMessageDTO> uploadIncidentFile(
            @PathVariable Integer incidentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        ChatMessageDTO message = chatService.sendIncidentFileMessage(incidentId, file, caption, userDetails.getUsername());
        messagingTemplate.convertAndSend("/topic/chat/incident/" + incidentId, message);
        return ResponseEntity.ok(message);
    }
}
