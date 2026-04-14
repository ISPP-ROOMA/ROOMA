package com.example.demo.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.UserDetailsImpl;
import com.example.demo.User.DTOs.UpdateProfileRequest;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;


    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = Mockito.spy(new UserService(userRepository, passwordEncoder, refreshTokenService));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // == Test updateCurrentUserProfile ==

    @Test
    void updateCurrentUserProfile_updatesFields() {
        UserEntity currentUser = new UserEntity();
        currentUser.setId(10);
        currentUser.setEmail("old@example.com");
        currentUser.setProfileImageUrl("old-pic");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Germán",
                "Moraga",
                "new@example.com",
                "newpass",
                "2003-06-13T00:00:00.000Z",
                "+49 1514461150",
                "new-pic",
                "Other",
                true,
                "hobbies",
                "schedule",
                "profession"
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("newpass")).thenReturn("encoded");
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        UserEntity updated = userService.updateCurrentUserProfile(request);

        assertEquals("new@example.com", updated.getEmail());
        assertEquals("encoded", updated.getPassword());
        assertEquals("new-pic", updated.getProfileImageUrl());
        assertEquals("Germán", updated.getName());
        assertEquals("Moraga", updated.getSurname());
        assertEquals("+49 1514461150", updated.getPhone());
        assertEquals("Other", updated.getGender());
        assertEquals(Boolean.TRUE, updated.getSmoker());
        assertEquals(2003, updated.getBirthDate().getYear());
        assertEquals("hobbies", updated.getHobbies());
        assertEquals("schedule", updated.getSchedule());
        assertEquals("profession", updated.getProfession());
    }

    @Test
    void updateCurrentUserProfile_emailConflict_throws() {
        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail("old@example.com");

        UserEntity other = new UserEntity();
        other.setId(2);
        other.setEmail("new@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,
                null,
                "new@example.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(other));

        assertThrows(ConflictException.class, () -> userService.updateCurrentUserProfile(request));
    }

    @Test
    void updateCurrentUserProfile_sameEmail_doesNotCheckRepository() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail("same@email.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null,
                "same@email.com",
                null,
                null,null,null,null,null,null,null,null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        userService.updateCurrentUserProfile(request);

        verify(userRepository, Mockito.never()).findByEmail(Mockito.any());
    }

    @Test
    void updateCurrentUserProfile_emailBelongsToSameUser_allowed() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail("old@example.com");

        UserEntity sameUser = new UserEntity();
        sameUser.setId(1);
        sameUser.setEmail("new@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,
                null,
                "new@example.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.of(sameUser));
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        UserEntity result = userService.updateCurrentUserProfile(request);

        assertEquals("new@example.com", result.getEmail());
    }

    @Test
    void updateCurrentUserProfile_newEmail_available_updatesEmail() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail("old@email.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,null,
                "new@email.com",
                null,
                null,null,null,null,null,null,null,null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        UserEntity result = userService.updateCurrentUserProfile(request);

        assertEquals("new@email.com", result.getEmail());
    }

    @Test
    void updateCurrentUserProfile_blankPassword_notUpdated() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setPassword("oldpass");

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,null,null,
                "   ", // blank
                null,null,null,null,null,null,null,null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        userService.updateCurrentUserProfile(request);

        assertEquals("oldpass", currentUser.getPassword());
        verify(passwordEncoder, Mockito.never()).encode(Mockito.any());
    }

    @Test
    void updateCurrentUserProfile_noFieldsProvided_onlySavesUser() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail("test@email.com");

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,null,null,
                null,
                null,null,null,null,null,null,null,null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        UserEntity result = userService.updateCurrentUserProfile(request);

        assertEquals("test@email.com", result.getEmail());
        verify(userRepository).save(currentUser);
    }

    @Test
    void updateCurrentUserProfile_birthDateLocalDateFormat() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,null,null,null,
                "2003-06-13",
                null,null,null,null,null,null,null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        UserEntity result = userService.updateCurrentUserProfile(request);

        assertEquals(2003, result.getBirthDate().getYear());
    }

    @Test
    void updateCurrentUserProfile_invalidBirthDate_throws() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,null,null,null,
                "not-a-date",
                null,null,null,null,null,null,null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();

        assertThrows(BadRequestException.class,
                () -> userService.updateCurrentUserProfile(request));
    }

    @Test
    void updateCurrentUserProfile_nullBirthDate_doesNotChange() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setBirthDate(LocalDate.of(2000,1,1));

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,null,null,null,
                null,
                null,null,null,null,null,null,null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        UserEntity result = userService.updateCurrentUserProfile(request);

        assertEquals(LocalDate.of(2000,1,1), result.getBirthDate());
    }

    @Test
    void updateCurrentUserProfile_blankBirthDate_returnsNull() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,null,null,null,
                "   ",
                null,null,null,null,null,null,null
        );

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        UserEntity result = userService.updateCurrentUserProfile(request);

        assertNull(result.getBirthDate());
    }

    // == Test deleteCurrentUserProfile ==

    @Test
    void deleteCurrentUserProfile_conflictWhenRelatedData() {
        UserEntity currentUser = new UserEntity();
        currentUser.setId(5);

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        doThrow(new DataIntegrityViolationException("fk")).when(userRepository).flush();

        assertThrows(ConflictException.class, () -> userService.deleteCurrentUserProfile());
        verify(refreshTokenService).deleteByUser(currentUser);
        verify(userRepository).delete(currentUser);
        verify(userRepository).flush();
    }

    @Test
    void deleteCurrentUserProfile_successfullyDeletesUser() {

        UserEntity currentUser = new UserEntity();
        currentUser.setId(5);

        doReturn(currentUser).when(userService).findCurrentUserEntity();

        userService.deleteCurrentUserProfile();

        verify(refreshTokenService).deleteByUser(currentUser);
        verify(userRepository).delete(currentUser);
        verify(userRepository).flush();
    }

    // == Test findCurrentUser ==

    @Test
    @DisplayName("findCurrentUser should return username when authenticated")
    void findCurrentUser_shouldReturnUsernameWhenAuthenticated() {
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getUsername()).thenReturn("test@email.com");
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        String username = userService.findCurrentUser();

        assertEquals("test@email.com", username);
    }

    @Test
    @DisplayName("findCurrentUser should throw when authentication is null")
    void findCurrentUser_shouldThrowWhenAuthenticationNull() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertThrows(ResourceNotFoundException.class, () -> userService.findCurrentUser());
    }

    @Test
    @DisplayName("findCurrentUser should throw when principal is not UserDetails")
    void findCurrentUser_shouldThrowWhenPrincipalNotUserDetails() {
        when(authentication.getPrincipal()).thenReturn("someString");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThrows(ResourceNotFoundException.class, () -> userService.findCurrentUser());
    }

    // == Test getUserProfile ==

    @Test
    @DisplayName("getUserProfile should return user when exists")
    void getUserProfile_shouldReturnUserWhenExists() {
        UserEntity user = new UserEntity();
        user.setEmail("test@email.com");

        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getUsername()).thenReturn("test@email.com");
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("test@email.com"))
                .thenReturn(Optional.of(user));

        UserEntity result = userService.getUserProfile();

        assertNotNull(result);
        assertEquals("test@email.com", result.getEmail());
    }

    @Test
    @DisplayName("getUserProfile should throw when user not found")
    void getUserProfile_shouldThrowWhenUserNotFound() {
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getUsername()).thenReturn("test@email.com");
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("test@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserProfile());
    }

    // == Test update ==
    @Test
    @DisplayName("update should update email, role and password")
    void update_shouldUpdateUser() {

        UserEntity existingUser = new UserEntity();
        existingUser.setId(1);
        existingUser.setEmail("old@email.com");
        existingUser.setPassword("oldpass");

        UserEntity updateData = new UserEntity();
        updateData.setEmail("new@email.com");
        updateData.setRole(Role.ADMIN);
        updateData.setPassword("newpass");

        doReturn(existingUser).when(userService).findById(1);
        when(passwordEncoder.encode("newpass")).thenReturn("encodedPass");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        UserEntity result = userService.update(1, updateData);

        assertEquals("new@email.com", result.getEmail());
        assertEquals(Role.ADMIN, result.getRole());
        assertEquals("encodedPass", result.getPassword());

        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("update should not change password when password is null")
    void update_shouldNotChangePasswordWhenNull() {

        UserEntity existingUser = new UserEntity();
        existingUser.setId(1);
        existingUser.setPassword("oldpass");

        UserEntity updateData = new UserEntity();
        updateData.setEmail("new@email.com");
        updateData.setRole(Role.ADMIN);
        updateData.setPassword(null);

        doReturn(existingUser).when(userService).findById(1);
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        UserEntity result = userService.update(1, updateData);

        assertEquals("oldpass", result.getPassword());
        verify(passwordEncoder, Mockito.never()).encode(Mockito.any());
    }

    @Test
    @DisplayName("update should not change password when password is empty")
    void update_shouldNotChangePasswordWhenEmpty() {

        UserEntity existingUser = new UserEntity();
        existingUser.setId(1);
        existingUser.setPassword("oldpass");

        UserEntity updateData = new UserEntity();
        updateData.setEmail("new@email.com");
        updateData.setRole(Role.ADMIN);
        updateData.setPassword("");

        doReturn(existingUser).when(userService).findById(1);
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        UserEntity result = userService.update(1, updateData);

        assertEquals("oldpass", result.getPassword());

        verify(passwordEncoder, Mockito.never()).encode(Mockito.any());
    }

    // == Test deleteById ==

    @Test
    @DisplayName("deleteById should delete user when exists")
    void deleteById_shouldDeleteUser() {

        when(userRepository.existsById(1)).thenReturn(true);

        userService.deleteById(1);

        verify(userRepository).deleteById(1);
    }

    @Test
    @DisplayName("deleteById should throw when user not found")
    void deleteById_shouldThrowWhenUserNotFound() {

        when(userRepository.existsById(1)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteById(1));

        verify(userRepository, Mockito.never()).deleteById(Mockito.any());
    }

    // == Test findCurrentUserEntity ==

    @Test
    @DisplayName("findCurrentUserEntity should return user when found")
    void findCurrentUserEntity_shouldReturnUser() {

        UserEntity user = new UserEntity();
        user.setEmail("test@email.com");

        doReturn("test@email.com").when(userService).findCurrentUser();
        doReturn(Optional.of(user)).when(userService).findByEmail("test@email.com");

        UserEntity result = userService.findCurrentUserEntity();

        assertNotNull(result);
        assertEquals("test@email.com", result.getEmail());
    }

    @Test
    @DisplayName("findCurrentUserEntity should throw when user not found")
    void findCurrentUserEntity_shouldThrowWhenUserNotFound() {

        doReturn("test@email.com").when(userService).findCurrentUser();
        doReturn(Optional.empty()).when(userService).findByEmail("test@email.com");

        assertThrows(ResourceNotFoundException.class,
                () -> userService.findCurrentUserEntity());
    }

    // == Test findById ==

    @Test
    void findById_userExists_returnsUser() {

        UserEntity user = new UserEntity();
        user.setId(1);
        user.setEmail("test@email.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserEntity result = userService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("test@email.com", result.getEmail());
    }

    @Test
    void findById_userNotFound_throws() {

        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.findById(1));
    }
    
}
