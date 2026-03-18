package com.example.demo.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationEntryPointTest {
private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

    @Test
    @DisplayName("Should return Unauthorized response")
    void shouldReturnUnauthorizedResponse() throws IOException, ServletException {

        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException exception =
                new AuthenticationException("Invalid token") {};

        entryPoint.commence(null, response, exception);

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());

        String body = response.getContentAsString();

        assertTrue(body.contains("Unauthorized: Invalid token"));
    }
}
