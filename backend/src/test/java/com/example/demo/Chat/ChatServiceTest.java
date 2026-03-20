package com.example.demo.Chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.ApartmentMatchRepository;
import com.example.demo.ApartmentMatch.MatchStatus;
import com.example.demo.Chat.DTOs.ChatMessageDTO;
import com.example.demo.Cloudinary.CloudinaryService;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ApartmentMatchRepository apartmentMatchRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ChatService chatService;

    private ApartmentMatchEntity match;
    private UserEntity user;
    private UserEntity otherUser;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1);
        user.setName("John");

        otherUser = new UserEntity();
        otherUser.setId(2);
        otherUser.setName("Alice");

        UserEntity landlord = new UserEntity();
        landlord.setId(3);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setUser(landlord);

        match = new ApartmentMatchEntity();
        match.setId(100);
        match.setCandidate(user);
        match.setApartment(apartment);
        match.setMatchStatus(MatchStatus.MATCH);
    }

    // == Test getMessageHistory ==

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"MATCH", "INVITED", "SUCCESSFUL"})
    @DisplayName("should return messages for valid match statuses")
    void getMessageHistory_shouldReturnMessages_forValidStatuses(MatchStatus status) {

        Integer matchId = match.getId();
        match.setMatchStatus(status);

        user.setName("John");

        ChatMessageEntity message = new ChatMessageEntity();
        message.setSender(user);
        message.setApartmentMatch(match);
        message.setContent("Hello");

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findCurrentUserEntity())
                .thenReturn(user);

        when(chatMessageRepository.findByApartmentMatchIdOrderBySentAtAsc(matchId))
                .thenReturn(List.of(message));

        List<ChatMessageDTO> result = chatService.getMessageHistory(matchId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }


    @Test
    @DisplayName("should return messages when user is landlord")
    void getMessageHistory_shouldWork_whenUserIsLandlord() {

        Integer matchId = match.getId();

        UserEntity landlord = match.getApartment().getUser();
        landlord.setName("Landlord");

        ChatMessageEntity message = new ChatMessageEntity();
        message.setSender(landlord);
        message.setApartmentMatch(match);
        message.setContent("Hello");

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findCurrentUserEntity())
                .thenReturn(landlord);

        when(chatMessageRepository.findByApartmentMatchIdOrderBySentAtAsc(matchId))
                .thenReturn(List.of(message));

        List<ChatMessageDTO> result = chatService.getMessageHistory(matchId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when match not found")
    void getMessageHistory_shouldThrow_whenMatchNotFound() {

        when(apartmentMatchRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatService.getMessageHistory(999)
        );

        verify(chatMessageRepository, never())
                .findByApartmentMatchIdOrderBySentAtAsc(any());
    }

    @Test
    @DisplayName("should throw AccessDeniedException when user is not participant")
    void getMessageHistory_shouldThrow_whenUserNotParticipant() {

        Integer matchId = match.getId();

        UserEntity otherUser = new UserEntity();
        otherUser.setId(999);

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findCurrentUserEntity())
                .thenReturn(otherUser);

        assertThrows(AccessDeniedException.class, () ->
                chatService.getMessageHistory(matchId)
        );

        verify(chatMessageRepository, never())
                .findByApartmentMatchIdOrderBySentAtAsc(any());
    }

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"REJECTED", "CANCELED", "ACTIVE"})
    @DisplayName("should throw ConflictException when match status is invalid")
    void getMessageHistory_shouldThrow_whenInvalidStatus(MatchStatus status) {

        Integer matchId = match.getId();

        match.setMatchStatus(status);

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findCurrentUserEntity())
                .thenReturn(user);

        assertThrows(ConflictException.class, () ->
                chatService.getMessageHistory(matchId)
        );

        verify(chatMessageRepository, never())
                .findByApartmentMatchIdOrderBySentAtAsc(any());
    }

    @Test
    @DisplayName("should return empty list when no messages exist")
    void getMessageHistory_shouldReturnEmptyList_whenNoMessages() {

        Integer matchId = match.getId();

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findCurrentUserEntity())
                .thenReturn(user);

        when(chatMessageRepository.findByApartmentMatchIdOrderBySentAtAsc(matchId))
                .thenReturn(List.of());

        List<ChatMessageDTO> result = chatService.getMessageHistory(matchId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // == Test sendMessage ==

    @Test
    @DisplayName("sendMessage should save and return message when valid")
    void sendMessage_shouldSaveMessage_whenValid() {

        Integer matchId = match.getId();
        String email = user.getEmail();
        user.setName("John");

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(chatMessageRepository.save(any(ChatMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // devuelve el mismo

        ChatMessageDTO result = chatService.sendMessage(matchId, " Hello ", email);

        assertNotNull(result);
        assertEquals("Hello", result.content()); // 🔥 trim
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when match not found")
    void sendMessage_shouldThrow_whenMatchNotFound() {

        when(apartmentMatchRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatService.sendMessage(999, "Hello", user.getEmail())
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void sendMessage_shouldThrow_whenUserNotFound() {

        Integer matchId = match.getId();

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail("test@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatService.sendMessage(matchId, "Hello", "test@email.com")
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw AccessDeniedException when user is not participant")
    void sendMessage_shouldThrow_whenUserNotParticipant() {

        Integer matchId = match.getId();

        UserEntity otherUser = new UserEntity();
        otherUser.setId(999);
        otherUser.setEmail("other@email.com");

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(otherUser.getEmail()))
                .thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () ->
                chatService.sendMessage(matchId, "Hello", otherUser.getEmail())
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"REJECTED", "CANCELED", "ACTIVE"})
    @DisplayName("should throw ConflictException when match status is invalid")
    void sendMessage_shouldThrow_whenInvalidStatus(MatchStatus status) {

        Integer matchId = match.getId();
        match.setMatchStatus(status);

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () ->
                chatService.sendMessage(matchId, "Hello", user.getEmail())
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("should throw ConflictException when content is empty")
    void sendMessage_shouldThrow_whenContentInvalid(String content) {

        Integer matchId = match.getId();

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () ->
                chatService.sendMessage(matchId, content, user.getEmail())
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw ConflictException when content is null")
    void sendMessage_shouldThrow_whenContentNull() {

        Integer matchId = match.getId();

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () ->
                chatService.sendMessage(matchId, null, user.getEmail())
        );
    }

    // == Test sendFileMessage ==

    @Test
    @DisplayName("sendFileMessage should upload file and save message when valid")
    void sendFileMessage_shouldSaveMessage_whenValid() throws IOException {

        Integer matchId = match.getId();
        String email = user.getEmail();
        user.setName("John");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "data".getBytes()
        );

        Map<String, Object> uploadResult = Map.of(
                "secure_url", "http://file.url",
                "public_id", "file123"
        );

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(cloudinaryService.uploadRaw(any(), anyString()))
                .thenReturn(uploadResult);

        when(chatMessageRepository.save(any(ChatMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessageDTO result = chatService.sendFileMessage(matchId, file, "caption", email);

        assertNotNull(result);
        assertEquals("caption", result.content());
    }

    @Test
    @DisplayName("resolveMessageType should return IMAGE for image content type")
    void resolveMessageType_shouldReturnImage() throws Exception {
        Method method = ChatService.class.getDeclaredMethod("resolveMessageType", String.class);
        method.setAccessible(true);

        MessageType type = (MessageType) method.invoke(chatService, "image/png");

        assertEquals(MessageType.IMAGE, type);
    }

    @Test
    @DisplayName("resolveMessageType should return AUDIO for audio content type")
    void resolveMessageType_shouldReturnAudio() throws Exception {
        Method method = ChatService.class.getDeclaredMethod("resolveMessageType", String.class);
        method.setAccessible(true);

        MessageType type = (MessageType) method.invoke(chatService, "audio/mpeg");

        assertEquals(MessageType.AUDIO, type);
    }

    @Test
    @DisplayName("resolveMessageType should return FILE for null content type")
    void resolveMessageType_shouldReturnFile_whenNull() throws Exception {
        Method method = ChatService.class.getDeclaredMethod("resolveMessageType", String.class);
        method.setAccessible(true);

        MessageType type = (MessageType) method.invoke(chatService, (Object) null);

        assertEquals(MessageType.FILE, type);
    }

    @Test
    @DisplayName("resolveMessageType should return FILE for other content types")
    void resolveMessageType_shouldReturnFile_forOtherTypes() throws Exception {
        Method method = ChatService.class.getDeclaredMethod("resolveMessageType", String.class);
        method.setAccessible(true);

        MessageType type = (MessageType) method.invoke(chatService, "application/pdf");

        assertEquals(MessageType.FILE, type);
    }

    @Test
    @DisplayName("should throw ConflictException when file is null")
    void sendFileMessage_shouldThrow_whenFileNull() {

        Integer matchId = match.getId();

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () ->
                chatService.sendFileMessage(matchId, null, "caption", user.getEmail())
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw ConflictException when file is empty")
    void sendFileMessage_shouldThrow_whenFileEmpty() {

        Integer matchId = match.getId();

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                new byte[0]
        );

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () ->
                chatService.sendFileMessage(matchId, emptyFile, "caption", user.getEmail())
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void sendFileMessage_shouldThrow_whenUserNotFound() {

        Integer matchId = match.getId();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "data".getBytes()
        );

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail("test@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatService.sendFileMessage(matchId, file, "caption", "test@email.com")
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw AccessDeniedException when user is not participant")
    void sendFileMessage_shouldThrow_whenUserNotParticipant() {

        Integer matchId = match.getId();

        UserEntity otherUser = new UserEntity();
        otherUser.setId(999);
        otherUser.setEmail("other@email.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "data".getBytes()
        );

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(otherUser.getEmail()))
                .thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () ->
                chatService.sendFileMessage(matchId, file, "caption", otherUser.getEmail())
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"REJECTED", "CANCELED", "ACTIVE"})
    @DisplayName("should throw ConflictException when match status is invalid")
    void sendFileMessage_shouldThrow_whenInvalidStatus(MatchStatus status) {

        Integer matchId = match.getId();
        match.setMatchStatus(status);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "data".getBytes()
        );

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () ->
                chatService.sendFileMessage(matchId, file, "caption", user.getEmail())
        );

        verify(chatMessageRepository, never()).save(any());
    }

    // == Test markMessagesAsRead ==

    @Test
    @DisplayName("markMessagesAsRead should update messages from other users to READ")
    void markMessagesAsRead_shouldMarkOtherUsersMessagesAsRead() {

        Integer matchId = match.getId();

        UserEntity otherUser = new UserEntity();
        otherUser.setId(2);
        otherUser.setEmail("other@email.com");

        ChatMessageEntity msg1 = new ChatMessageEntity();
        msg1.setId(1);
        msg1.setSender(otherUser);
        msg1.setApartmentMatch(match);
        msg1.setStatus(MessageStatus.SENT);

        ChatMessageEntity msg2 = new ChatMessageEntity();
        msg2.setId(2);
        msg2.setSender(user);
        msg2.setApartmentMatch(match);
        msg2.setStatus(MessageStatus.SENT);

        List<ChatMessageEntity> allMessages = List.of(msg1, msg2);

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(chatMessageRepository.findByApartmentMatchIdOrderBySentAtAsc(matchId))
                .thenReturn(allMessages)
                .thenReturn(allMessages);

        doAnswer(invocation -> invocation.getArgument(0))
                .when(chatMessageRepository).saveAll(anyList());

        List<ChatMessageDTO> result = chatService.markMessagesAsRead(matchId, user.getEmail());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(MessageStatus.READ, msg1.getStatus());
        assertEquals(MessageStatus.SENT, msg2.getStatus());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when match not found")
    void markMessagesAsRead_shouldThrow_whenMatchNotFound() {

        when(apartmentMatchRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatService.markMessagesAsRead(999, user.getEmail())
        );

        verify(chatMessageRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when user not found")
    void markMessagesAsRead_shouldThrow_whenUserNotFound() {

        when(apartmentMatchRepository.findById(match.getId()))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail("unknown@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatService.markMessagesAsRead(match.getId(), "unknown@email.com")
        );

        verify(chatMessageRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("should throw AccessDeniedException when user is not participant")
    void markMessagesAsRead_shouldThrow_whenUserNotParticipant() {

        UserEntity otherUser = new UserEntity();
        otherUser.setId(999);

        when(apartmentMatchRepository.findById(match.getId()))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(otherUser.getEmail()))
                .thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () ->
                chatService.markMessagesAsRead(match.getId(), otherUser.getEmail())
        );

        verify(chatMessageRepository, never()).saveAll(any());
    }

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"REJECTED", "CANCELED", "ACTIVE"})
    @DisplayName("should throw ConflictException when match status is invalid")
    void markMessagesAsRead_shouldThrow_whenInvalidStatus(MatchStatus status) {

        match.setMatchStatus(status);

        when(apartmentMatchRepository.findById(match.getId()))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () ->
                chatService.markMessagesAsRead(match.getId(), user.getEmail())
        );

        verify(chatMessageRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("should not change messages already marked as READ")
    void markMessagesAsRead_shouldNotChangeAlreadyReadMessages() {

        Integer matchId = match.getId();

        UserEntity otherUser = new UserEntity();
        otherUser.setId(2);

        ChatMessageEntity msg1 = new ChatMessageEntity();
        msg1.setId(1);
        msg1.setSender(otherUser);
        msg1.setApartmentMatch(match);
        msg1.setStatus(MessageStatus.READ);

        List<ChatMessageEntity> allMessages = List.of(msg1);

        when(apartmentMatchRepository.findById(matchId))
                .thenReturn(Optional.of(match));

        when(userService.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(chatMessageRepository.findByApartmentMatchIdOrderBySentAtAsc(matchId))
                .thenReturn(allMessages)
                .thenReturn(allMessages);

        List<ChatMessageDTO> result = chatService.markMessagesAsRead(matchId, user.getEmail());
        assertNotNull(result);
        assertFalse(result.isEmpty());


        assertEquals(MessageStatus.READ, msg1.getStatus());
        verify(chatMessageRepository).saveAll(Collections.emptyList());
    }
    
}
