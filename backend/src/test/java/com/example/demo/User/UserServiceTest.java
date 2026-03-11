package com.example.demo.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.Exceptions.ConflictException;
import com.example.demo.User.DTOs.UpdateProfileRequest;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = Mockito.spy(new UserService(userRepository, passwordEncoder));
    }

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
    void deleteCurrentUserProfile_conflictWhenRelatedData() {
        UserEntity currentUser = new UserEntity();
        currentUser.setId(5);

        doReturn(currentUser).when(userService).findCurrentUserEntity();
        doThrow(new DataIntegrityViolationException("fk")).when(userRepository).flush();

        assertThrows(ConflictException.class, () -> userService.deleteCurrentUserProfile());
        verify(userRepository).delete(currentUser);
        verify(userRepository).flush();
    }
}
