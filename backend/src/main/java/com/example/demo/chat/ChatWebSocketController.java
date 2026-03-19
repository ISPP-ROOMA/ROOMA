package com.example.demo.chat;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.example.demo.chat.DTOs.ChatMessageDTO;
import com.example.demo.chat.DTOs.SendMessageDTO;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat/{matchId}")
    @SendTo("/topic/chat/{matchId}")
    public ChatMessageDTO sendMessage(@DestinationVariable Integer matchId,
                                       SendMessageDTO message,
                                       Principal principal) {
        return chatService.sendMessage(matchId, message.content(), principal.getName());
    }
}
