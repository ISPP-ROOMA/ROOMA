package com.example.demo.Auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.Auth.DTOs.AuthResult;
import com.example.demo.Exceptions.InvalidHashException;
import com.example.demo.Exceptions.InvalidPasswordException;
import com.example.demo.Exceptions.InvalidRefreshTokenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Exceptions.UserExistsException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.User.RefreshTokenEntity;
import com.example.demo.User.RefreshTokenService;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;


@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UserEntity user;

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidHashException("Error hashing token");
        }
    }

    @BeforeEach
    void setUp() {

        user = new UserEntity();
        user.setEmail("test@email.com");
        user.setPassword("hashedPassword");
        user.setRole(Role.ADMIN);
    }

    // == Test register ==

    @Test
    @DisplayName("register should return AuthResult when user is new")
    void register_shouldReturnAuthResultWhenUserIsNew() {
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
        when(jwtService.generateAccessTokenFromEmail(user.getEmail())).thenReturn("accessToken");
        when(jwtService.generateRefreshTokenFromEmail(user.getEmail())).thenReturn("refreshToken");
        when(jwtService.getExpirarationFromToken("refreshToken")).thenReturn(new Date());

        AuthResult result = authService.register(
                user.getEmail(), "password", "device1", Role.LANDLORD
        );

        assertNotNull(result);
        assertEquals("accessToken", result.accessToken());
        assertEquals("refreshToken", result.refreshToken());
        assertEquals("LANDLORD", result.role());

        verify(userService).save(any(UserEntity.class));
        verify(refreshTokenService).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("register should throw UserExistsException when user already exists")
    void register_shouldThrowExceptionWhenUserExists() {
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserExistsException.class, () ->
                authService.register(user.getEmail(), "password", "device1", Role.ADMIN)
        );
    }

    // == Test login ==

    @Test
    @DisplayName("login should return AuthResult when password is correct")
    void login_shouldReturnAuthResultWhenPasswordIsCorrect() {
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessTokenFromEmail(user.getEmail())).thenReturn("accessToken");
        when(jwtService.generateRefreshTokenFromEmail(user.getEmail())).thenReturn("refreshToken");
        when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(new RefreshTokenEntity());
        when(jwtService.getExpirarationFromToken("refreshToken")).thenReturn(new Date());

        AuthResult result = authService.login(user.getEmail(), "password", "device1");

        assertNotNull(result);
        assertEquals("accessToken", result.accessToken());
        assertEquals("refreshToken", result.refreshToken());
    }

    @Test
    @DisplayName("login should throw InvalidPasswordException when password is incorrect")
    void login_shouldThrowExceptionWhenPasswordIncorrect() {
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);

        assertThrows(InvalidPasswordException.class, () ->
                authService.login(user.getEmail(), "wrongPassword", "device1")
        );
    }

    @Test
    @DisplayName("login should throw ResourceNotFoundException when user not found")
    void login_shouldThrowExceptionWhenUserNotFound() {
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                authService.login(user.getEmail(), "password", "device1")
        );
    }

    // == Test refreshToken ==

    @Test
    @DisplayName("refreshToken should return new tokens when refresh token is valid")
    void refreshToken_shouldReturnNewTokensWhenValid() {
        RefreshTokenEntity storedToken = new RefreshTokenEntity();
        storedToken.setToken(hashToken("oldRefreshToken"));
        storedToken.setExpiration(new Date(System.currentTimeMillis() + 10000)); // no expirado

        when(jwtService.verifyToken("oldRefreshToken")).thenReturn(true);
        when(jwtService.getEmailFromToken("oldRefreshToken")).thenReturn(user.getEmail());
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);
        when(jwtService.generateAccessTokenFromEmail(user.getEmail())).thenReturn("newAccessToken");
        when(jwtService.generateRefreshTokenFromEmail(user.getEmail())).thenReturn("newRefreshToken");
        when(jwtService.getExpirarationFromToken("newRefreshToken")).thenReturn(new Date(System.currentTimeMillis() + 10000));

        AuthResult result = authService.refreshToken("oldRefreshToken", "device1");

        assertEquals("newAccessToken", result.accessToken());
        assertEquals("newRefreshToken", result.refreshToken());
    }

    @Test
    @DisplayName("refreshToken should throw InvalidRefreshTokenException when token is invalid")
    void refreshToken_shouldThrowWhenTokenInvalid() {
        when(jwtService.verifyToken("invalidToken")).thenReturn(false);

        assertThrows(InvalidRefreshTokenException.class, () ->
                authService.refreshToken("invalidToken", "device1")
        );
    }

    @Test
    @DisplayName("refreshToken should throw InvalidRefreshTokenException when token does not match records")
    void refreshToken_shouldThrowWhenTokenMismatch() {
        RefreshTokenEntity storedToken = new RefreshTokenEntity();
        storedToken.setToken(hashToken("someOtherToken")); // hash diferente
        storedToken.setExpiration(new Date(System.currentTimeMillis() + 10000));

        when(jwtService.verifyToken("validToken")).thenReturn(true);
        when(jwtService.getEmailFromToken("validToken")).thenReturn(user.getEmail());
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);

        assertThrows(InvalidRefreshTokenException.class, () ->
                authService.refreshToken("validToken", "device1")
        );
    }

    @Test
    @DisplayName("refreshToken should throw InvalidRefreshTokenException when token is expired")
    void refreshToken_shouldThrowWhenTokenExpired() {
        RefreshTokenEntity storedToken = new RefreshTokenEntity();
        storedToken.setToken(hashToken("validToken"));
        storedToken.setExpiration(new Date(System.currentTimeMillis() - 1000)); // ya expirado

        when(jwtService.verifyToken("validToken")).thenReturn(true);
        when(jwtService.getEmailFromToken("validToken")).thenReturn(user.getEmail());
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);

        assertThrows(InvalidRefreshTokenException.class, () ->
                authService.refreshToken("validToken", "device1")
        );

        verify(refreshTokenService).delete(storedToken); // se elimina token expirado
    }

    // == Test logout ==

    @Test
    @DisplayName("logout should delete refresh token when valid")
    void logout_shouldDeleteRefreshTokenWhenValid() {
        RefreshTokenEntity storedToken = new RefreshTokenEntity();
        storedToken.setToken(hashToken("refreshToken"));

        when(jwtService.verifyToken("refreshToken")).thenReturn(true);
        when(jwtService.getEmailFromToken("refreshToken")).thenReturn(user.getEmail());
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);

        authService.logout("refreshToken", "device1");

        verify(refreshTokenService).delete(storedToken);
    }

    @Test
    @DisplayName("logout should throw InvalidRefreshTokenException when token is invalid")
    void logout_shouldThrowWhenTokenInvalid() {
        when(jwtService.verifyToken("invalidToken")).thenReturn(false);

        assertThrows(InvalidRefreshTokenException.class, () ->
                authService.logout("invalidToken", "device1")
        );

        verify(userService, never()).findByEmail(anyString());
        verify(refreshTokenService, never()).delete(any());
    }

    @Test
    @DisplayName("logout should throw InvalidRefreshTokenException when token does not match records")
    void logout_shouldThrowWhenTokenMismatch() {
        RefreshTokenEntity storedToken = new RefreshTokenEntity();
        storedToken.setToken(hashToken("otherToken")); // hash distinto

        when(jwtService.verifyToken("validToken")).thenReturn(true);
        when(jwtService.getEmailFromToken("validToken")).thenReturn(user.getEmail());
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);

        assertThrows(InvalidRefreshTokenException.class, () ->
                authService.logout("validToken", "device1")
        );

        verify(refreshTokenService, never()).delete(storedToken);
    }


}
