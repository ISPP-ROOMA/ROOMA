package com.example.demo.Jwt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter jwtFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }
    
    // == TESTS de doFilterInternal ==

    @Test
    @DisplayName("Should authenticate user when token is valid")
    void shouldAuthenticateUserWhenTokenValid() throws Exception {

        String token = "valid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.verifyToken(token)).thenReturn(true);
        when(jwtService.getEmailFromToken(token)).thenReturn("test@email.com");
        when(userDetailsService.loadUserByUsername("test@email.com")).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter when no Authorization header is present")
    void shouldContinueFilterWhenNoAuthorizationHeader() throws Exception {

        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter when Authorization header does not start with Bearer")
    void shouldContinueFilterWhenAuthorizationHeaderDoesNotStartWithBearer() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Invalid " + "token");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate user when token is invalid")
    void shouldNotAuthenticateWhenTokenInvalid() throws Exception {

        String token = "invalid";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.verifyToken(token)).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).verifyToken(token);
        verify(userDetailsService, never()).loadUserByUsername(any());

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear security context when exception occurs during authentication")
    void shouldClearSecurityContextWhenExceptionOccurs() throws Exception {

        String token = "token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.verifyToken(token)).thenThrow(new RuntimeException());

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }


}
