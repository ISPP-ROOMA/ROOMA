package com.example.demo.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;


@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {

    private UserService userService;
    
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userDetailsService = new UserDetailsServiceImpl(userService);
    }

    @Test
    @DisplayName("Should return UserDetails when user exists")
    void shouldReturnUserDetailsWhenUserExists() {
        UserEntity user = new UserEntity();
        user.setEmail("test@email.com");
        user.setPassword("secret");

        user.setRole(Role.ADMIN);

        when(userService.findByEmail("test@email.com"))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@email.com");

        assertNotNull(userDetails);
        assertEquals("test@email.com", userDetails.getUsername());
        assertEquals("secret", userDetails.getPassword());

        verify(userService).findByEmail("test@email.com");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userService.findByEmail("notfound@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("notfound@email.com");
        });

        verify(userService).findByEmail("notfound@email.com");
    }
}
