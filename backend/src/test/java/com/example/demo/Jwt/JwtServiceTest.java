package com.example.demo.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Spy
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();


        String rawSecret = "12345678901234567890123456789012";
        String secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "expirationAt", 3600000L); // 1h
        ReflectionTestUtils.setField(jwtService, "expirationRt", 86400000L); // 1 dia
    }

    // == TESTS de generateAccessTokenFromEmail ==

    @Test
    @DisplayName("Should generate a valid access token")
    void shouldGenerateValidAccessToken() {

        String token = jwtService.generateAccessTokenFromEmail("test@email.com");

        assertNotNull(token);
        assertTrue(jwtService.verifyToken(token));
    }

    @Test
    @DisplayName("Should throw RuntimeException with correct message when token generation fails")
    void shouldThrowRuntimeExceptionWithCorrectMessage() {

        ReflectionTestUtils.setField(jwtService, "secretKey", "invalidBase64Key");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jwtService.generateAccessTokenFromEmail("test@email.com");
        });

        assertEquals("Error generating access token", ex.getMessage());
    }

    // == TESTS de getEmailFromToken ==

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {

        String token = jwtService.generateAccessTokenFromEmail("test@email.com");

        String email = jwtService.getEmailFromToken(token);

        assertEquals("test@email.com", email);
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void shouldThrowExceptionWhenTokenInvalid() {

        assertThrows(RuntimeException.class, () -> {
            jwtService.getEmailFromToken("NotAValidToken");
        });
    }

    // == TESTS de getExpirarationFromToken ==

    @Test
    @DisplayName("Should return expiration date from token")
    void shouldReturnExpirationDate() {

        String token = jwtService.generateAccessTokenFromEmail("test@email.com");

        assertNotNull(jwtService.getExpirarationFromToken(token));
    }

    @Test
    void shouldThrowExceptionWhenExpirationParsingFails() {

        assertThrows(RuntimeException.class, () -> {
            jwtService.getExpirarationFromToken("NotAValidToken");
        });
    }

    // == TESTS de generateRefreshTokenFromEmail ==

    @Test
    @DisplayName("Should generate a valid refresh token")
    void shouldGenerateValidRefreshToken() {

        String token = jwtService.generateRefreshTokenFromEmail("test@email.com");

        assertNotNull(token);
        assertTrue(jwtService.verifyToken(token));
    }

    @Test
    @DisplayName("Should throw RuntimeException when generating refresh token fails")
    void shouldThrowRuntimeExceptionWhenGeneratingRefreshTokenFails() {

        ReflectionTestUtils.setField(jwtService, "secretKey", "invalidBase64Key");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            jwtService.generateRefreshTokenFromEmail("test@email.com");
        });
        assertEquals("Error generating refresh token", ex.getMessage());
    }

    // == TESTS de verifyToken ==

    @Test
    @DisplayName("Should return false for malformed token")
    void shouldReturnFalseForMalformedToken() {

        boolean result = jwtService.verifyToken("NotAValidToken");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for expired token")
    void shouldReturnFalseForExpiredToken() throws InterruptedException {

        ReflectionTestUtils.setField(jwtService, "expirationAt", 1L);

        String token = jwtService.generateAccessTokenFromEmail("test@email.com");

        Thread.sleep(5);

        boolean result = jwtService.verifyToken(token);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for token signed with different key")
    void shouldReturnFalseForTokenSignedWithDifferentKey() {

        JwtService anotherService = new JwtService();

        String rawSecret = "DifferentSecretKey12345678901234567890";
        String secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

        ReflectionTestUtils.setField(anotherService, "secretKey", secret);
        ReflectionTestUtils.setField(anotherService, "expirationAt", 3600000L);

        String token = anotherService.generateAccessTokenFromEmail("test@email.com");

        boolean result = jwtService.verifyToken(token);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForNullToken() {

        boolean result = jwtService.verifyToken(null);

        assertFalse(result);
    }

    
}
