package com.example.demo.chat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.ApartmentMatchRepository;
import com.example.demo.ApartmentMatch.MatchStatus;
import com.example.demo.Cloudinary.CloudinaryService;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import com.example.demo.chat.DTOs.ChatMessageDTO;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ApartmentMatchRepository apartmentMatchRepository;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       ApartmentMatchRepository apartmentMatchRepository,
                       UserService userService,
                       CloudinaryService cloudinaryService) {
        this.chatMessageRepository = chatMessageRepository;
        this.apartmentMatchRepository = apartmentMatchRepository;
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getMessageHistory(Integer matchId) {
        ApartmentMatchEntity match = findMatchOrThrow(matchId);
        UserEntity currentUser = userService.findCurrentUserEntity();
        validateParticipant(match, currentUser);
        validateChatAllowed(match);

        List<ChatMessageEntity> messages = chatMessageRepository
                .findByApartmentMatchIdOrderBySentAtAsc(matchId);
        return ChatMessageDTO.fromEntityList(messages);
    }

    @Transactional
    public ChatMessageDTO sendMessage(Integer matchId, String content, String senderEmail) {
        ApartmentMatchEntity match = findMatchOrThrow(matchId);
        UserEntity sender = userService.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        validateParticipant(match, sender);
        validateChatAllowed(match);

        if (content == null || content.isBlank()) {
            throw new ConflictException("Message content cannot be empty");
        }

        ChatMessageEntity message = new ChatMessageEntity();
        message.setApartmentMatch(match);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setMessageType(MessageType.TEXT);

        ChatMessageEntity saved = chatMessageRepository.save(message);
        return ChatMessageDTO.fromEntity(saved);
    }

    @Transactional
    public ChatMessageDTO sendFileMessage(Integer matchId, MultipartFile file, String caption,
                                           String senderEmail) throws IOException {
        ApartmentMatchEntity match = findMatchOrThrow(matchId);
        UserEntity sender = userService.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        validateParticipant(match, sender);
        validateChatAllowed(match);

        if (file == null || file.isEmpty()) {
            throw new ConflictException("File cannot be empty");
        }

        Map<String, Object> uploadResult = cloudinaryService.uploadRaw(file, "chat/" + matchId);
        String fileUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        MessageType messageType = resolveMessageType(file.getContentType());

        ChatMessageEntity message = new ChatMessageEntity();
        message.setApartmentMatch(match);
        message.setSender(sender);
        message.setContent(caption);
        message.setMessageType(messageType);
        message.setFileUrl(fileUrl);
        message.setFilePublicId(publicId);
        message.setFileName(file.getOriginalFilename());

        ChatMessageEntity saved = chatMessageRepository.save(message);
        return ChatMessageDTO.fromEntity(saved);
    }

    private MessageType resolveMessageType(String contentType) {
        if (contentType == null) {
            return MessageType.FILE;
        }
        if (contentType.startsWith("image/")) {
            return MessageType.IMAGE;
        }
        if (contentType.startsWith("audio/")) {
            return MessageType.AUDIO;
        }
        return MessageType.FILE;
    }

    private ApartmentMatchEntity findMatchOrThrow(Integer matchId) {
        return apartmentMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    }

    private void validateParticipant(ApartmentMatchEntity match, UserEntity user) {
        boolean isCandidate = match.getCandidate().getId().equals(user.getId());
        boolean isLandlord = match.getApartment().getUser().getId().equals(user.getId());
        if (!isCandidate && !isLandlord) {
            throw new AccessDeniedException("You are not a participant of this match");
        }
    }

    private void validateChatAllowed(ApartmentMatchEntity match) {
        MatchStatus status = match.getMatchStatus();
        if (status != MatchStatus.MATCH
                && status != MatchStatus.INVITED
                && status != MatchStatus.SUCCESSFUL) {
            throw new ConflictException("Chat is only available for matches with status MATCH, INVITED, or SUCCESSFUL");
        }
    }

    @Transactional
    public List<ChatMessageDTO> markMessagesAsRead(Integer matchId, String readerEmail) {
        ApartmentMatchEntity match = findMatchOrThrow(matchId);
        UserEntity reader = userService.findByEmail(readerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        validateParticipant(match, reader);
        validateChatAllowed(match);

        List<ChatMessageEntity> messages = chatMessageRepository
                .findByApartmentMatchIdOrderBySentAtAsc(matchId);

        List<ChatMessageEntity> updated = messages.stream()
                .filter(m -> !m.getSender().getId().equals(reader.getId()))
                .filter(m -> m.getStatus() != MessageStatus.READ)
                .peek(m -> m.setStatus(MessageStatus.READ))
                .toList();

        chatMessageRepository.saveAll(updated);

        return ChatMessageDTO.fromEntityList(
                chatMessageRepository.findByApartmentMatchIdOrderBySentAtAsc(matchId));
    }
}
